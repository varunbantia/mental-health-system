package com.vanaksh.manomitra.safety;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * VoiceTranscriptionManager - Speech-to-Text using OpenAI Whisper API
 * 
 * Transcribes audio files to text for processing through the safety layer
 * and ChatGPT.
 */
public class VoiceTranscriptionManager {
    private static final String TAG = "VoiceTranscription";
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final OkHttpClient client;
    private final String apiKey;

    /**
     * Callback for transcription results
     */
    public interface TranscriptionCallback {
        void onSuccess(String transcribedText);

        void onError(String error);
    }

    /**
     * Constructor
     * 
     * @param apiKey OpenAI API key (same as ChatGPT)
     */
    public VoiceTranscriptionManager(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Transcribe an audio file to text
     * 
     * @param audioFile The audio file to transcribe (mp3, wav, m4a, etc.)
     * @param callback  Callback for the result
     */
    public void transcribe(File audioFile, TranscriptionCallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("API key not configured");
            return;
        }

        if (audioFile == null || !audioFile.exists()) {
            callback.onError("Audio file not found");
            return;
        }

        Log.d(TAG, "Transcribing audio file: " + audioFile.getName() +
                " (" + audioFile.length() / 1024 + " KB)");

        try {
            // Determine media type based on file extension
            String fileName = audioFile.getName().toLowerCase();
            MediaType mediaType;
            if (fileName.endsWith(".mp3")) {
                mediaType = MediaType.parse("audio/mpeg");
            } else if (fileName.endsWith(".wav")) {
                mediaType = MediaType.parse("audio/wav");
            } else if (fileName.endsWith(".m4a")) {
                mediaType = MediaType.parse("audio/m4a");
            } else if (fileName.endsWith(".ogg")) {
                mediaType = MediaType.parse("audio/ogg");
            } else {
                // Default for Android recordings (often .3gp or no extension)
                mediaType = MediaType.parse("audio/mpeg");
            }

            // Build multipart request
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("model", "whisper-1")
                    .addFormDataPart("language", "en") // Can be "hi" for Hindi
                    .addFormDataPart("file", audioFile.getName(),
                            RequestBody.create(audioFile, mediaType))
                    .build();

            Request request = new Request.Builder()
                    .url(WHISPER_API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "Transcription failed: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response)
                        throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "API error: " + response.code() + " - " + errorBody);
                        callback.onError("Transcription error: " + response.code());
                        return;
                    }

                    try {
                        String responseBody = response.body().string();
                        JSONObject json = new JSONObject(responseBody);
                        String text = json.getString("text");

                        Log.d(TAG, "Transcription successful: " + text);
                        callback.onSuccess(text.trim());

                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to parse response: " + e.getMessage());
                        callback.onError("Failed to parse transcription");
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error creating request: " + e.getMessage());
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Cancel all pending requests
     */
    public void cancelAll() {
        client.dispatcher().cancelAll();
    }
}
