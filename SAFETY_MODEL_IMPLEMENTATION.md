# ManoMitra Safety Model: Complete Implementation Details

This document provides the full implementation pipeline for the **ManoMitra Safety Model**, covering training, optimization, and mobile integration.

---

## 1. Model Preparation (Python / TensorFlow)

The safety model is a **Hybrid Classifier** that combines a transformer-based encoder with a deep neural network.

### A. Architecture
- **Feature Extractor**: Universal Sentence Encoder (USE) Lite (512-dim fixed vector).
- **Downstream Classifier**: 3-layer Fully Connected Neural Network (FCNN).
    - `Dense(256, activation='relu')` + `Dropout(0.3)`
    - `Dense(128, activation='relu')` + `Dropout(0.3)`
    - `Dense(64, activation='relu')`
    - `Output(1, activation='sigmoid')`

### B. Training Script Snippet (`train_safety_model.py`)
```python
def build_fcnn_classifier():
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(512,), name='embedding_input'),
        tf.keras.layers.Dense(256, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dropout(0.3),
        tf.keras.layers.Dense(64, activation='relu'),
        tf.keras.layers.Dense(1, activation='sigmoid')
    ])
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])
    return model

# TFLite Conversion with Quantization
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT] # Dynamic Range Quantization
tflite_model = converter.convert()
```

---

## 2. Mobile Integration (Android / Java)

The implementation on Android uses **TensorFlow Lite Interpreter** with a multi-layer fallback system.

### A. The Safety Pipeline (`SafetyManager.java`)
The `SafetyManager` orchestrates the flow between the embedding engine and the classifier.

```java
public boolean isMessageSafe(String message) {
    // 1. Generate 512-dim embedding via USE
    float[] embedding = useEmbedding.embed(message);
    
    // 2. Run TFLite Classifier
    float probability = classifier.predict(embedding);
    
    // 3. Concurrent Keyword Scan (Conservative Fallback)
    float textScore = classifier.predictFromText(message);
    
    // 4. Decision Logic (Threshold = 0.5)
    float finalScore = Math.max(probability, textScore);
    return finalScore < 0.5f;
}
```

### B. Embedding Engine (`USEEmbedding.java`)
Handles the **use_model.tflite** to convert raw strings into semantic vectors.
- **Input**: String/Token IDs
- **Output**: `float[1][512]`
- **Threading**: Configured with `options.setNumThreads(4)` for low-latency on-device processing.

### C. Fallback Mechanism
If the TFLite interpreter fails to initialize (e.g., unsupported hardware), the system falls back to:
- **Character-level Statistical Features**: Analyzing char distributions.
- **Keyword Risk Indices**: Weighted scoring of specific crisis-related terms (e.g., "suicide", "harm", "hopeless").

---

## 3. Optimization Summary
| Feature | Technique | Result |
| :--- | :--- | :--- |
| **Model Size** | Dynamic Range Quantization | Reduced from 2.4MB to ~600KB |
| **Latency** | Multi-threading (4 Threads) | < 50ms inference time on mid-range devices |
| **Robustness** | Hybrid Analysis | Protects against both semantic intent and keyword evasion |
