package com.vanaksh.manomitra.safety;

import android.content.Context;
import android.util.Log;

/**
 * SafetyManager - The Safety Gate
 * 
 * ROLE:
 * This class applies the critical safety threshold based on the offline ML
 * model.
 * 
 * FLOW:
 * 1. Takes raw user text from ChatController.
 * 2. Asks the CustomSafetyClassifier for a risk prediction score (0.0 to 1.0).
 * 3. Compares the score against the strict 0.5 threshold:
 * - Score < 0.5: SAFE (Proceed to ChatGPT)
 * - Score >= 0.5: HIGH-RISK (Blocked)
 */
public class SafetyManager {
    private static final String TAG = "SafetyManager";
    private static final float SAFETY_THRESHOLD = 0.5f;

    private CustomSafetyClassifier customClassifier;
    private Context context;
    private boolean isInitialized = false;

    public SafetyManager(Context context) {
        this.context = context.getApplicationContext();
        initialize();
    }

    private void initialize() {
        try {
            Log.d(TAG, "Initializing SafetyManager with Custom Model...");
            customClassifier = new CustomSafetyClassifier(context);
            isInitialized = true;
            Log.d(TAG, "Custom safety model loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load custom safety model: " + e.getMessage());
            isInitialized = false;
        }
    }

    /**
     * Determines whether a user message is safe to process.
     */
    public SafetyResult analyzeMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new SafetyResult(true, 0.0f, "Empty message");
        }

        // [Safety Patch] Whitelist common short greetings to avoid ML false positives
        if (isCommonGreeting(message)) {
            Log.d(TAG, "Message is a common greeting. Whitelisted as SAFE.");
            return new SafetyResult(true, 0.01f, "Whitelisted greeting");
        }

        long startTime = System.currentTimeMillis();
        float mlScore = 0.0f;

        // 1. Machine Learning Inference
        if (customClassifier != null) {
            try {
                // The message is physically handed over to the ML model here:
                mlScore = customClassifier.predict(message);
                Log.d(TAG, "Custom ML Model score: " + mlScore);
            } catch (Exception e) {
                Log.e(TAG, "Custom ML inference failed: " + e.getMessage());
                mlScore = 0.0f;
            }
        }

        long inferenceTime = System.currentTimeMillis() - startTime;

        // 2. Decision Logic (Threshold Check)
        if (mlScore >= SAFETY_THRESHOLD) {
            // [HIGH-RISK DETECTED] -> Block Message
            String details = String.format("BLOCKED by ML model. Score: %.3f, Time: %dms", mlScore, inferenceTime);
            Log.w(TAG, "Message BLOCKED: " + details);
            return new SafetyResult(false, mlScore, details);
        }

        // [SAFE] -> Allow Message
        String details = String.format("SAFE ✅ ML: %.3f, Time: %dms", mlScore, inferenceTime);
        Log.d(TAG, details);
        return new SafetyResult(true, mlScore, details);
    }

    /**
     * Returns the hardcoded crisis intervention message used when a message is
     * blocked.
     */
    public String getSafetyResponseMessage() {
        return "I noticed you might be going through a difficult time. " +
                "You're not alone, and support is available.\n\n" +
                "🆘 Crisis Helplines:\n" +
                "• iCall: 9152987821\n" +
                "• Vandrevala Foundation: 1860-2662-345 (24/7)\n" +
                "• NIMHANS: 080-46110007\n\n" +
                "Would you like to talk about something else, or would you prefer " +
                "some self-care activities?";
    }

    public void close() {
        if (customClassifier != null) {
            customClassifier.close();
            customClassifier = null;
        }
        isInitialized = false;
    }

    /**
     * Helper to identify extremely common, low-risk greetings that often trigger
     * false positives in ML models trained on noisy clinical data.
     */
    private boolean isCommonGreeting(String message) {
        if (message == null) return false;
        
        // Normalize for comparison
        String clean = message.toLowerCase().trim().replaceAll("[^a-z\\s]", "");
        String[] words = clean.split("\\s+");
        
        // Only whitelist very short messages (1-3 words) to ensure we don't
        // accidentally whitelist a longer risky sentence starting with a greeting.
        if (words.length > 3) return false;
        
        for (String word : words) {
            if (word.matches("h+i+") || 
                word.equals("hello") || 
                word.equals("hey") || 
                word.equals("hlo") ||
                word.equals("heyo") ||
                word.equals("hola")) {
                return true;
            }
        }
        return false;
    }

    public static class SafetyResult {
        public final boolean isSafe;
        public final float riskScore;
        public final String details;

        public SafetyResult(boolean isSafe, float riskScore, String details) {
            this.isSafe = isSafe;
            this.riskScore = riskScore;
            this.details = details;
        }
    }
}
