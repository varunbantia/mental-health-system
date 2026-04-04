package com.vanaksh.manomitra.safety;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/**
 * CustomSafetyClassifier - Core ML Inference Engine
 * 
 * ROLE:
 * Executes the offline neural network to detect life-threatening intent.
 * 
 * FLOW:
 * 1. Loads the TFLite model and vocabulary into RAM on startup.
 * 2. Tokenizes incoming text into an integer array (length 200).
 * 3. Runs the array through the Neural Network.
 * 4. Outputs a precise float representing the risk probability (0.0 to 1.0).
 */
public class CustomSafetyClassifier {
    private static final String TAG = "CustomSafetyClassifier";
    private static final String MODEL_FILE = "custom_safety_model.tflite";
    private static final String VOCAB_FILE = "vocab.txt";
    private static final int MAX_LEN = 200;
    private int vocabSize = 0;

    private Interpreter interpreter;
    private Map<String, Integer> wordIndex;
    private boolean isInitialized = false;
    private Context context;

    public CustomSafetyClassifier(Context context) {
        this.context = context.getApplicationContext();
        initialize();
    }

    private void initialize() {
        try {
            // [1] Load Model
            MappedByteBuffer modelBuffer = loadModelFile(MODEL_FILE);
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(4);
            interpreter = new Interpreter(modelBuffer, options);

            // [2] Load Vocabulary
            loadVocabulary();
            vocabSize = wordIndex.size();
            
            isInitialized = true;
            Log.d(TAG, "Custom safety model and vocab loaded successfully. Size: " + vocabSize);
        } catch (IOException e) {
            Log.e(TAG, "Failed to initialize custom classifier: " + e.getMessage());
        }
    }

    private void loadVocabulary() throws IOException {
        wordIndex = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(VOCAB_FILE)))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                wordIndex.put(line.trim(), index++);
            }
        }
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * The Main Inference Step
     */
    public float predict(String text) {
        if (!isInitialized || text == null || vocabSize == 0) return 0.0f;

        // =========================================================
        // STEP 1 & 2: Hacking Words into Numbers (Tokenization)
        // =========================================================
        // Neural networks cannot read English letters, only math.
        // We convert "i want to die" -> [2, 47, 3, 340, 0, 0, ...]
        int[] sequence = tokenizeAndPad(text);

        // =========================================================
        // STEP 3: Preparing the Memory for the Model
        // =========================================================
        // The model expects a "Batch" of size 1, so we put our 200 numbers inside a 2D array.
        int[][] input = new int[1][MAX_LEN];
        for (int i = 0; i < MAX_LEN; i++) {
            input[0][i] = sequence[i];
        }

        // We also prepare an empty catcher for the answer.
        float[][] output = new float[1][1];
        
        try {
            // =========================================================
            // STEP 4: Physically "Talking" to the Model
            // =========================================================
            // Java physically hands the `input` memory block to the TFLite C++ engine.
            // It shoots the numbers through the Embedding Layer, Self-Attention layer, 
            // and Dense layers, calculating the probability. 
            // Finally, it writes the decimal answer directly into your `output` catcher.
            interpreter.run(input, output);
        } catch (Exception e) {
            Log.e(TAG, "Inference error: " + e.getMessage());
            return 0.0f;
        }

        // =========================================================
        // STEP 5: Returning the Result
        // =========================================================
        // The float (e.g., 0.8512f) is now sitting inside output[0][0].
        return output[0][0];
    }

    private int[] tokenizeAndPad(String text) {
        // [Action 1] Lowercase everything and strip punctuation.
        // "I want to die." -> "i want to die"
        String cleanText = text.toLowerCase().replaceAll("[^a-z0-9\\s]", "");
        
        // [Action 2] Split by spaces into a list of words.
        String[] words = cleanText.split("\\s+");
        
        // [Action 3] Create a blank array of exactly 200 slots.
        int[] sequence = new int[MAX_LEN];

        // [Action 4] Loop through the words and look up their integer ID in your vocab.txt.
        for (int i = 0; i < Math.min(words.length, MAX_LEN); i++) {
            String word = words[i].trim();
            
            // [Safety Patch] Normalize common variations of 'hi' (e.g., hii, hiii) 
            // the model was sometimes sensitive to these variations.
            if (word.matches("h+i+")) {
                word = "hi";
            }
            
            Integer idx = wordIndex.get(word);
            // If the word exists, put its ID. If it's a completely unknown word, put a '1' (OOV).
            sequence[i] = (idx != null) ? idx : 1; 
        }
        
        // Any remaining slots after the sentence finishes are left as 0 (Padding).
        return sequence;
    }

    public void close() {
        if (interpreter != null) interpreter.close();
        isInitialized = false;
    }
}
