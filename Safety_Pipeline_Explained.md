# ManoMitra Safety Architecture: End-to-End Pipeline
=============================================================
This document explains exactly how the 100% custom, offline machine learning safety shield works in your ManoMitra mental health app. 

It covers the journey from the Python training phase all the way through the Java Android app, with exact code snippets pulled from your project files.

---

## 🛑 STAGE 1: Offline Training (Python)
Before the app is even installed on a phone, the "brain" is built on your computer. We fed 232,074 rows of suicidal and non-suicidal text into a custom Neural Network.

**Where it happens:** `training/train_custom_safety_model.py`

This script builds a custom dictionary (`vocab.txt`) of 20,000 words. It then builds a massive neural network containing Semantic Embeddings, LSTMs, and Self-Attention layers:

```python
# 1. We build our Architecture (Math logic)
model = tf.keras.Sequential([
    # Expect sentences of exactly 120 (or 200) length
    tf.keras.layers.Input(shape=(MAX_LEN,), dtype=tf.int32),
    # Meaning Extractor
    tf.keras.layers.Embedding(VOCAB_SIZE, EMBEDDING_DIM),
    # Dense reasoning layers
    tf.keras.layers.GlobalAveragePooling1D(),
    tf.keras.layers.Dense(32, activation='relu'),
    # Final Probability (Sigmoid squashes the math into a float between 0.0 and 1.0)
    tf.keras.layers.Dense(1, activation='sigmoid')
])

# 2. We convert the trained brain into a tiny, 10MB mobile file
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# 3. Save it for Android!
with open("custom_safety_model.tflite", "wb") as f:
    f.write(tflite_model)
```
*(The `.tflite` model and the `vocab.txt` file are then copied perfectly into your Android app's `app/src/main/assets/` folder, ready for deployment.)*

---

## 🛑 STAGE 2: Intercepting the Message (Java)
A user downloads the app, opens the chat, and types: `"I want to die"`. 
Before ChatGPT is even allowed to know what was typed, the `ChatController` intercepts the raw text.

**Where it happens:** `ChatController.java` (Line ~143)

```java
public void processMessageWithHistoryDetailed(String message, ...) {
    
    // 1. SAFETY GATE: Send the raw text to our offline ML model BEFORE OpenAI
    SafetyManager.SafetyResult safetyResult = safetyManager.analyzeMessage(message);

    if (safetyResult.isSafe) {
        // 2. SAFE PATH: The model approved it. Now we can send to ChatGPT API.
        chatGPTManager.sendMessageWithHistory(...);
    } else {
        // 3. BLOCKED PATH: The model flagged it! 
        // We completely bypass ChatGPT and send hardcoded helplines to the user.
        String safetyResponse = safetyManager.getSafetyResponseMessage();
        callback.onResponse(safetyResponse, false);
    }
}
```

---

## 🛑 STAGE 3: The Safety Gate (Java)
The `SafetyManager` is the policy enforcer. It receives the text from the `ChatController`, passes it to the AI for a math score, and then enforces the `0.5` strict policy rule.

**Where it happens:** `SafetyManager.java` (Line ~76)

```java
public SafetyResult analyzeMessage(String message) {
    // 1. Send the text physically down into the AI Classifier memory
    float mlScore = customClassifier.predict(message); 

    // 2. Enforce the strict 50% Threshold Rule
    if (mlScore >= 0.5f) {
        // HIGH RISK DETECTED
        return new SafetyResult(false, mlScore, "BLOCKED");
    }

    // SAFE
    return new SafetyResult(true, mlScore, "SAFE");
}
```

---

## 🛑 STAGE 4: The Neural Inference (Java / C++)
This is the core "AI magic". The `CustomSafetyClassifier` grabs the text string, rips it apart, turns the English words into numbers (using `vocab.txt`), and forces the numbers through the `.tflite` model math.

**Where it happens:** `CustomSafetyClassifier.java` (Line ~90)

```java
public float predict(String text) {
    // 1. Tokenization: 
    // Takes "I want to die" and looks up every word in vocab.txt.
    // Returns an array like: [2, 47, 3, 340, 0, 0, ...] filled out to 200 numbers.
    int[] sequence = tokenizeAndPad(text);

    // 2. Prep Memory: Create the C++ bridge holding our 200 numbers
    int[][] input = new int[1][200];
    for (int i = 0; i < 200; i++) { input[0][i] = sequence[i]; }
    
    // Create an empty catcher for the answer
    float[][] output = new float[1][1];
    
    // 3. Inference:
    // The numbers are pushed into the C++ TFLite engine. 
    // The Embeddings, LSTMs, and Dense layers calculate the exact probability of danger.
    interpreter.run(input, output);

    // 4. Return: The float (e.g., 0.85f) is caught inside output[0][0].
    return output[0][0]; // Sends score back up to SafetyManager!
}
```

---

### SUMMARY OF THE FLOW:
1. **User Input:** User hits 'send'.
2. **Controller Trap:** `ChatController` intercepts and hands it to `SafetyManager`.
3. **Model Prediction:** `SafetyManager` asks `CustomSafetyClassifier` for the mathematical probability of risk.
4. **Tokenization:** `CustomSafetyClassifier` turns the words into numbers using `vocab.txt`.
5. **Inference:** The numbers run through the fully offline `custom_safety_model.tflite` on the phone's RAM.
6. **Decision:** Score (`0.85`) comes back. `SafetyManager` sees it is `>= 0.5` and yells **BLOCKED**.
7. **Action:** `ChatController` cancels the OpenAI call and instantly puts Local Helplines on the user's screen.
