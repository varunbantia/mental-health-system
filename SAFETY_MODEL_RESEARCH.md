# Technical Specification: ManoMitra Safety Classification System

This document provides in-depth technical details regarding the machine learning architecture used in the **ManoMitra** mental health platform for safe chatbot interactions. This information is intended for use in academic research and technical publications.

---

## 1. System Architecture Overview
The ManoMitra Safety Layer follows a **Modular Hybrid Architecture** designed for high-precision, low-latency inference on edge devices (Android). It combines transformer-based feature extraction with a specialized downstream classification head.

### Architectural Pipeline:
1.  **Pre-processing**: Text normalization and tokenization.
2.  **Embedding Generation**: Universal Sentence Encoder (USE) mapping.
3.  **Classification Inference**: Fully Connected Neural Network (FCNN).
4.  **Heuristic Hybridization**: Dual-path analysis (model-based + keyword-based).

---

## 2. Feature Extraction: Universal Sentence Encoder (USE)
The system leverages a Lite version of the **Universal Sentence Encoder** for generating semantic embeddings.

*   **Input Representation**: Raw UTF-8 encoded text sequences.
*   **Sequence Length**: Optimized for $L = 128$ tokens.
*   **Output Dimension**: $D = 512$ (fixed-size vector).
*   **Methodology**: Semantic intent preservation. Unlike simple Word2Vec or GloVe, USE accounts for sentence-level context, allowing the model to distinguish between "I want to kill the process" (safe) and "I want to kill myself" (unsafe) by analyzing the holistic embedding space.

---

## 3. Classification Head: Deep FCNN Architecture
The core safety classifier is a **Fully Connected Neural Network (FCNN)** implemented in TensorFlow Lite.

### Model Topology:
| Layer | Type | Configuration | Activation | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Input** | Input | 512 Units | N/A | Receives USE Embedding vector |
| **Hidden 1** | Dense | 256 Units | ReLU | High-level pattern recognition |
| **Reg 1** | Dropout | $p = 0.3$ | N/A | Prevents overfitting to training set |
| **Hidden 2** | Dense | 128 Units | ReLU | Feature abstraction |
| **Reg 2** | Dropout | $p = 0.3$ | N/A | Generalization improvement |
| **Hidden 3** | Dense | 64 Units | ReLU | Final feature refinement |
| **Output** | Dense | 1 Unit | **Sigmoid** | Binary probability distribution |

### Mathematical Objective:
The model is trained to minimize **Binary Cross-Entropy Loss**:
$$L(y, \hat{y}) = -(y \log(\hat{y}) + (1-y) \log(1-\hat{y}))$$
Where $\hat{y} \in [0, 1]$ represents the probability of the message being "Concerning" (Class 1).

---

## 4. Model Training and Hyperparameters
The model was trained using the following empirical configuration to ensure high sensitivity and generalization:

### Training Configuration:
*   **Optimizer**: Adam (Adaptive Moment Estimation)
*   **Loss Function**: Binary Cross-Entropy
*   **Epochs**: 10 (with early stopping)
*   **Batch Size**: 32
*   **Validation Split**: 20% (with an additional 20% internal validation for early stopping)
*   **Metrics**: Accuracy, Precision, and Recall (Precision/Recall balance is critical for safety).

### Optimization Strategies:
*   **Early Stopping**: Monitoring `val_loss` with a patience of 3 epochs and `restore_best_weights=True`.
*   **Regularization**: Dropout layers ($p=0.3$) implemented after each dense layer in the FCNN to prevent co-adaptation of features.

---

## 5. Model Conversion and Deployment
The transition from a high-level Keras/TensorFlow model to a mobile-optimized format involved several critical steps:

### TensorFlow Lite Conversion:
*   **USE Model**: Converted using `TFLiteConverter.from_concrete_functions` with `OpsSet.SELECT_TF_OPS` enabled to support complex transformer operations on Android.
*   **Safety Classifier**: Converted with `tf.lite.Optimize.DEFAULT` (Dynamic Range Quantization) to reduce model footprint while maintaining accuracy.

### Runtime Specifications:
*   **Threads**: Configured for 4-thread parallel inference on mobile CPUs.
*   **Model Size**:
    *   Classification Head: Optimized quantized TFLite format (~600KB).
    *   Embedding Engine: Simplified transformer-lite implementation.

---

## 6. Dataset and Validation
*   **Source**: The model was trained on a specialized suicide detection dataset (Binary: `safe` vs `suicide`).
*   **Fallback Logic**: A recursive fallback to Character-level statistical features (128 dims) and Keyword-Indicator vectors (252+ dims) ensures that safety protocols remain active even if the TFLite interpreter fails.

---

> [!NOTE]
> This architecture was specifically designed for the **ManoMitra** project to balance the need for sensitive mental health monitoring with the performance constraints of mobile hardware.
