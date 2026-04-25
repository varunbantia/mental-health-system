package com.vanaksh.manomitra.safety;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatController - Core Message Orchestrator
 * 
 * ROLE:
 * This acts as the traffic controller for all incoming user messages. 
 * Before any message reaches the AI, it MUST pass through the local safety shield.
 * 
 * PIPELINE FLOW:
 * 1. User Input -> Sent to ChatController
 * 2. Model Evaluation -> ChatController sends text to SafetyManager (offline ML)
 * 3. Routing:
 *    - SAFE: Message forwarded to ChatGPT; ChatGPT response returned to User.
 *    - HIGH-RISK: ChatGPT is Bypassed; Local Crisis Helplines returned to User.
 */
public class ChatController {
    private static final String TAG = "ChatController";

    private final SafetyManager safetyManager;
    private final ChatGPTManager chatGPTManager;
    private final VoiceTranscriptionManager voiceManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface ChatCallback {
        void onResponse(String response, boolean wasSafe);
        void onError(String error);
    }

    public interface VoiceChatCallback extends ChatCallback {
        void onTranscription(String transcribedText);
    }

    public interface DetailedChatCallback extends ChatCallback {
        void onResponseWithDetails(String response, SafetyManager.SafetyResult result);
    }

    public ChatController(Context context, String openAiApiKey) {
        this.safetyManager = new SafetyManager(context);
        this.chatGPTManager = new ChatGPTManager(openAiApiKey);
        this.voiceManager = new VoiceTranscriptionManager(openAiApiKey);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Core functionality linking the UI, the Safety Model, and ChatGPT.
     */
    // this method is called by chatbotactitvity
    public void processMessageWithHistoryDetailed(String message,
            java.util.List<com.vanaksh.manomitra.data.model.ChatMessage> history, DetailedChatCallback callback) {
        if (message == null || message.trim().isEmpty()) {
            postToMainThread(() -> callback.onError("Empty message"));
            return;
        }

        executor.execute(() -> {
            try {
                // 1. SAFETY GATE: Analyze the message using our offline ML model
                Log.d(TAG, "Sending message to ML Safety Model...");
                SafetyManager.SafetyResult safetyResult = safetyManager.analyzeMessage(message);

                if (safetyResult.isSafe) {
                    // 2. SAFE PATH: Model approved the message (Score < 0.5)
                    Log.d(TAG, "Message is SAFE. Forwarding to ChatGPT API...");
                    org.json.JSONArray messagesJson = convertHistoryToJSON(history, message);

                    chatGPTManager.sendMessageWithHistory(messagesJson, new ChatGPTManager.ChatGPTCallback() {
                        @Override
                        public void onSuccess(String response) {
                            Log.d(TAG, "ChatGPT responded successfully.");
                            postToMainThread(() -> {
                                callback.onResponse(response, true);
                                callback.onResponseWithDetails(response, safetyResult);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "ChatGPT API error: " + error);
                            postToMainThread(() -> callback.onError(error));
                        }
                    });
                } else {
                    // 3. BLOCKED PATH: Model flagged life-threatening intent (Score >= 0.5)
                    Log.d(TAG, "Message is HIGH-RISK. ChatGPT Bypassed.");
                    
                    // Fetch hardcoded crisis intervention helplines instantly
                    String safetyResponse = safetyManager.getSafetyResponseMessage();
                    
                    postToMainThread(() -> {
                        callback.onResponse(safetyResponse, false);
                        callback.onResponseWithDetails(safetyResponse, safetyResult);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in processMessageWithHistoryDetailed: " + e.getMessage());
                postToMainThread(() -> callback.onError("Error processing message: " + e.getMessage()));
            }
        });
    }

    /**
     * Convert ChatMessage list to OpenAI JSON format
     */
    private org.json.JSONArray convertHistoryToJSON(
            java.util.List<com.vanaksh.manomitra.data.model.ChatMessage> history, String currentMessage) {
        org.json.JSONArray messages = new org.json.JSONArray();
        try {
            // Add System Prompt
            org.json.JSONObject systemMsg = new org.json.JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are ManoMitra, a compassionate mental health companion. Provide empathetic, short, and helpful responses. You are NOT a replacement for professional mental health care.");
            messages.put(systemMsg);

            // Add history (limit to last 10 messages)
            if (history != null) {
                int maxHistory = 10;
                int limit = Math.min(history.size(), maxHistory);

                for (int i = limit - 1; i >= 0; i--) {
                    com.vanaksh.manomitra.data.model.ChatMessage cm = history.get(i);
                    org.json.JSONObject msg = new org.json.JSONObject();

                    String role = "user";
                    if (cm.getType() == com.vanaksh.manomitra.data.model.ChatMessage.TYPE_BOT) {
                        role = "assistant";
                    }

                    if (cm.getType() == com.vanaksh.manomitra.data.model.ChatMessage.TYPE_LOADING)
                        continue;

                    msg.put("role", role);
                    String content = cm.getMessage();
                    if (content != null && content.startsWith("🎤 ")) {
                        content = content.substring(2);
                    }
                    msg.put("content", content);
                    messages.put(msg);
                }
            }

            // Add current message
            org.json.JSONObject currentMsg = new org.json.JSONObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", currentMessage);
            messages.put(currentMsg);

        } catch (org.json.JSONException e) {
            Log.e(TAG, "Error converting history: " + e.getMessage());
        }
        return messages;
    }

    /**
     * Process a voice message: Whisper Transcription -> Safety Gate -> ChatGPT
     */
    public void processVoiceMessage(java.io.File audioFile, VoiceChatCallback callback) {
        if (audioFile == null || !audioFile.exists()) {
            postToMainThread(() -> callback.onError("Audio file not found"));
            return;
        }

        Log.d(TAG, "Sending audio to Whisper API for transcription...");
        voiceManager.transcribe(audioFile, new VoiceTranscriptionManager.TranscriptionCallback() {
            @Override
            public void onSuccess(String transcribedText) {
                Log.d(TAG, "Whisper transcription received: " + transcribedText);
                postToMainThread(() -> callback.onTranscription(transcribedText));

                executor.execute(() -> {
                    try {
                        // 1. SAFETY GATE: Analyze the transcribed text
                        SafetyManager.SafetyResult safetyResult = safetyManager.analyzeMessage(transcribedText);

                        if (safetyResult.isSafe) {
                            // 2. SAFE PATH: Forward transcription to ChatGPT
                            Log.d(TAG, "Voice message SAFE. Sending to ChatGPT...");
                            sendToChatGPT(transcribedText, callback);
                        } else {
                            // 3. BLOCKED PATH: Return crisis response
                            Log.d(TAG, "Voice message HIGH-RISK. ChatGPT Bypassed.");
                            String safetyResponse = safetyManager.getSafetyResponseMessage();
                            postToMainThread(() -> callback.onResponse(safetyResponse, false));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Voice processing error: " + e.getMessage());
                        postToMainThread(() -> callback.onError("Processing error: " + e.getMessage()));
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Whisper transcription failed: " + error);
                postToMainThread(() -> callback.onError("Could not transcribe voice: " + error));
            }
        });
    }

    /**
     * Internal method to send individual text to ChatGPT
     */
    private void sendToChatGPT(String message, ChatCallback callback) {
        chatGPTManager.sendMessage(message, new ChatGPTManager.ChatGPTCallback() {
            @Override
            public void onSuccess(String response) {
                postToMainThread(() -> callback.onResponse(response, true));
            }

            @Override
            public void onError(String error) {
                postToMainThread(() -> callback.onError(error));
            }
        });
    }

    private void postToMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }

    public void close() {
        safetyManager.close();
        chatGPTManager.cancelAll();
        executor.shutdown();
    }
}
