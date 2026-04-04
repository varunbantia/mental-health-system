package com.vanaksh.manomitra.safety;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * ChatGPTManager - Handles communication with OpenAI's ChatGPT API
 * 
 * ROLE IN THE GUIDE (MINUTE DETAILS):
 * [PRE-CONDITION] This class is *only* called if SafetyManager returns isSafe=true.
 * [1] JSON PREP: Safe message + history converted to OpenAI-compatible JSON.
 * [2] NETWORK CALL: Async HTTP request sent to https://api.openai.com/v1/chat/completions.
 * [3] STREAM/RESPONSE: OpenAI processes the request and returns a JSON response.
 * [4] PARSING: assistant_message extracted and returned to ChatController via callback.
 * 
 * Usage:
 * ChatGPTManager chatGPT = new ChatGPTManager(API_KEY);
 * chatGPT.sendMessage("Hello!", new ChatGPTCallback() {
 * 
 * @Override
 *           public void onSuccess(String response) { ... }
 * @Override
 *           public void onError(String error) { ... }
 *           });
 */
public class ChatGPTManager {
    private static final String TAG = "ChatGPTManager";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // Model configuration
    private static final String MODEL = "gpt-3.5-turbo";
    private static final int MAX_TOKENS = 500;
    private static final double TEMPERATURE = 0.7;

    private final OkHttpClient client;
    private final String apiKey;
    private final String systemPrompt;

    /**
     * Callback interface for ChatGPT responses
     */
    public interface ChatGPTCallback {
        void onSuccess(String response);

        void onError(String error);
    }

    /**
     * Constructor with API key
     * 
     * @param apiKey OpenAI API key
     */
    public ChatGPTManager(String apiKey) {
        this(apiKey, getDefaultSystemPrompt());
    }

    /**
     * Constructor with API key and custom system prompt
     * 
     * @param apiKey       OpenAI API key
     * @param systemPrompt Custom system prompt for the chatbot
     */
    public ChatGPTManager(String apiKey, String systemPrompt) {
        this.apiKey = apiKey;
        this.systemPrompt = systemPrompt;

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Get default system prompt for mental health support chatbot
     */
    private static String getDefaultSystemPrompt() {
        return "You are ManoMitra, a compassionate and supportive mental health companion. " +
                "You provide empathetic responses, helpful coping strategies, and emotional support. " +
                "You are NOT a replacement for professional mental health care. " +
                "If someone needs professional help, gently encourage them to speak with a counselor. " +
                "Keep responses warm, understanding, and concise. " +
                "Use simple language and be culturally sensitive to Indian users. " +
                "Include helpful tips and self-care suggestions when appropriate.";
    }

    /**
     * Send a single message to ChatGPT (OpenAI API)
     * 
     * THE FINAL STEP (MINUTE DETAILS):
     * [STEP 4.1.1] Create JSON payload with 'model', 'messages', and 'temperature'.
     * [STEP 4.1.2] Build Request with Authorization Header (Bearer API_KEY).
     * [STEP 4.2.1] Execute OkHttp Call asynchronously.
     * [STEP 4.3.1] Parse 'choices[0].message.content' from the response.
     */
    public void sendMessage(String message, ChatGPTCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not configured");
            return;
        }

        try {
            // Build request body
            JSONObject requestBody = buildRequestBody(message);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            Log.d(TAG, "Sending message to ChatGPT...");

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Request failed: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    e.printStackTrace();
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        callback.onError("API error: " + response.code());
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        String assistantMessage = parseResponse(responseBody);
                        Log.d(TAG, "Received response from ChatGPT");
                        callback.onSuccess(assistantMessage);

                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse response: " + e.getMessage());
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "Failed to build request: " + e.getMessage());
            callback.onError("Failed to build request");
        }
    }

    /**
     * Send a message with conversation history
     * 
     * @param messages Array of previous messages (role, content pairs)
     * @param callback Callback for response/error
     */
    public void sendMessageWithHistory(JSONArray messages, ChatGPTCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not configured");
            return;
        }

        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", MODEL);
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", MAX_TOKENS);
            requestBody.put("temperature", TEMPERATURE);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onError("API error: " + response.code());
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        String assistantMessage = parseResponse(responseBody);
                        callback.onSuccess(assistantMessage);
                    } catch (JSONException e) {
                        callback.onError("Failed to parse response");
                    }
                }
            });

        } catch (JSONException e) {
            callback.onError("Failed to build request");
        }
    }

    /**
     * Build the request body JSON
     */
    private JSONObject buildRequestBody(String userMessage) throws JSONException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", MODEL);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.put("temperature", TEMPERATURE);

        JSONArray messages = new JSONArray();

        // System message
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.put(systemMessage);

        // User message
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);

        requestBody.put("messages", messages);

        return requestBody;
    }

    /**
     * Parse the API response to extract the assistant's message
     */
    private String parseResponse(String responseBody) throws JSONException {
        JSONObject response = new JSONObject(responseBody);

        if (response.has("error")) {
            throw new JSONException(response.getJSONObject("error").getString("message"));
        }

        JSONArray choices = response.getJSONArray("choices");
        if (choices.length() == 0) {
            throw new JSONException("No response choices");
        }

        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");

        return message.getString("content").trim();
    }

    /**
     * Cancel all pending requests
     */
    public void cancelAll() {
        client.dispatcher().cancelAll();
    }

}
