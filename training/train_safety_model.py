"""
Professional Safety Model - Self-Attention + BiLSTM
====================================================
Detects life-threatening intent using semantic understanding.
Trained on the full 232k-row Suicide Detection dataset.
No keywords. No augmentation. Pure neural intelligence.
"""
import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')


import os
import re
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, confusion_matrix

# ============================================================
# Configuration
# ============================================================
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
DATASET_PATH = os.path.join(SCRIPT_DIR, "Suicide_Detection.csv")
OUTPUT_DIR = os.path.join(SCRIPT_DIR, "model_output")
ASSETS_DIR = os.path.join(SCRIPT_DIR, "..", "app", "src", "main", "assets")

VOCAB_SIZE = 20000
MAX_LEN = 200
EMBEDDING_DIM = 128
LSTM_UNITS = 64
EPOCHS = 15
BATCH_SIZE = 256

os.makedirs(OUTPUT_DIR, exist_ok=True)


# ============================================================
# Step 1: Text Preprocessing
# ============================================================
def clean_text(text):
    """Clean raw text: lowercase, remove URLs, HTML entities, special chars."""
    if not isinstance(text, str):
        return ""
    text = text.lower()
    text = re.sub(r'http\S+|www\.\S+', '', text)           # URLs
    text = re.sub(r'&amp;|&lt;|&gt;|&nbsp;|&#x200b;', ' ', text)  # HTML entities
    text = re.sub(r'\[.*?\]|\(.*?\)', '', text)             # Markdown links
    text = re.sub(r'[^a-z0-9\s]', ' ', text)                # Keep only alphanumeric
    text = re.sub(r'\s+', ' ', text).strip()                 # Collapse whitespace
    return text


# ============================================================
# Step 3: Self-Attention Layer
# ============================================================
class SelfAttention(tf.keras.layers.Layer):
    """
    Self-Attention mechanism that learns WHICH words in a sequence
    are most important for the classification decision.
    
    This is the key to understanding context:
    - "hang" + "myself" + "fan" → HIGH attention on all three
    - "fan" + "nice" + "cool" → LOW attention on "fan"
    """
    def __init__(self, units=64, **kwargs):
        super(SelfAttention, self).__init__(**kwargs)
        self.units = units

    def build(self, input_shape):
        self.W = self.add_weight(
            name="att_weight",
            shape=(int(input_shape[-1]), self.units),
            initializer="glorot_uniform",
            trainable=True
        )
        self.b = self.add_weight(
            name="att_bias",
            shape=(self.units,),
            initializer="zeros",
            trainable=True
        )
        self.u = self.add_weight(
            name="att_context",
            shape=(self.units,),
            initializer="glorot_uniform",
            trainable=True
        )
        super().build(input_shape)

    def call(self, x):
        # x shape: (batch, timesteps, features)
        # Score each timestep
        score = tf.nn.tanh(tf.tensordot(x, self.W, axes=[[-1], [0]]) + self.b)
        # Attention weights
        attention_weights = tf.nn.softmax(
            tf.tensordot(score, self.u, axes=[[-1], [0]]), axis=1
        )
        # Weighted sum
        context = tf.reduce_sum(x * tf.expand_dims(attention_weights, -1), axis=1)
        return context

    def get_config(self):
        config = super().get_config()
        config.update({"units": self.units})
        return config


# ============================================================
# Main Training Function
# ============================================================
def train():
    print("=" * 60)
    print("  Professional Safety Model — Training Pipeline")
    print("=" * 60)

    # ----------------------------------------------------------
    # Load Dataset
    # ----------------------------------------------------------
    print(f"\n[1/6] Loading dataset from {DATASET_PATH}...")
    df = pd.read_csv(DATASET_PATH)
    print(f"  Total rows: {len(df)}")
    print(f"  Class distribution:\n{df['class'].value_counts().to_string()}")

    # ----------------------------------------------------------
    # Preprocess Text
    # ----------------------------------------------------------
    print("\n[2/6] Preprocessing text...")
    df['clean_text'] = df['text'].apply(clean_text)
    # Drop any empty rows after cleaning
    df = df[df['clean_text'].str.len() > 0].reset_index(drop=True)
    print(f"  Rows after cleaning: {len(df)}")

    texts = df['clean_text'].values
    labels = (df['class'] == 'suicide').astype(int).values

    # ----------------------------------------------------------
    # Train/Test Split (Stratified)
    # ----------------------------------------------------------
    X_train_text, X_test_text, y_train, y_test = train_test_split(
        texts, labels, test_size=0.2, random_state=42, stratify=labels
    )
    print(f"  Train: {len(X_train_text)} | Test: {len(X_test_text)}")

    # ----------------------------------------------------------
    # TextVectorization
    # ----------------------------------------------------------
    print("\n[3/6] Building vocabulary (TextVectorization)...")
    vectorize_layer = tf.keras.layers.TextVectorization(
        max_tokens=VOCAB_SIZE,
        output_mode='int',
        output_sequence_length=MAX_LEN
    )
    vectorize_layer.adapt(X_train_text)

    vocab = vectorize_layer.get_vocabulary()
    print(f"  Vocabulary size: {len(vocab)}")

    # Save vocabulary
    vocab_path = os.path.join(OUTPUT_DIR, "vocab.txt")
    with open(vocab_path, "w", encoding="utf-8") as f:
        for word in vocab:
            f.write(word + "\n")
    print(f"  Vocabulary saved to {vocab_path}")

    # Vectorize
    X_train = vectorize_layer(X_train_text)
    X_test = vectorize_layer(X_test_text)

    # ----------------------------------------------------------
    # Build Model: Self-Attention + BiLSTM
    # ----------------------------------------------------------
    print("\n[4/6] Building Self-Attention + BiLSTM model...")

    inputs = tf.keras.Input(shape=(MAX_LEN,), dtype=tf.int32)

    # Embedding
    x = tf.keras.layers.Embedding(
        input_dim=VOCAB_SIZE,
        output_dim=EMBEDDING_DIM,
        mask_zero=False
    )(inputs)
    x = tf.keras.layers.SpatialDropout1D(0.3)(x)

    # Bidirectional LSTM (return_sequences for attention)
    x = tf.keras.layers.Bidirectional(
        tf.keras.layers.LSTM(LSTM_UNITS, return_sequences=True)
    )(x)

    # Self-Attention (learns which words matter)
    x = SelfAttention(units=64)(x)

    # Classification Head
    x = tf.keras.layers.Dense(64, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.5)(x)
    x = tf.keras.layers.Dense(32, activation='relu')(x)
    x = tf.keras.layers.Dropout(0.3)(x)
    outputs = tf.keras.layers.Dense(1, activation='sigmoid')(x)

    model = tf.keras.Model(inputs, outputs)

    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='binary_crossentropy',
        metrics=['accuracy',
                 tf.keras.metrics.Precision(name='precision'),
                 tf.keras.metrics.Recall(name='recall')]
    )

    model.summary()

    # ----------------------------------------------------------
    # Train
    # ----------------------------------------------------------
    print("\n[5/6] Training model...")
    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor='val_loss', patience=3, restore_best_weights=True
    )
    reduce_lr = tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss', factor=0.5, patience=2, min_lr=1e-6
    )

    model.fit(
        X_train, y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_data=(X_test, y_test),
        callbacks=[early_stop, reduce_lr]
    )

    # ----------------------------------------------------------
    # Evaluate
    # ----------------------------------------------------------
    print("\n[6/6] Evaluation...")
    y_pred_proba = model.predict(X_test, verbose=0).flatten()
    y_pred = (y_pred_proba >= 0.5).astype(int)

    print("\n--- Classification Report ---")
    print(classification_report(y_test, y_pred, target_names=['SAFE', 'HIGH-RISK']))

    print("\n--- Confusion Matrix ---")
    cm = confusion_matrix(y_test, y_pred)
    print(f"  True Negatives  (Safe kept Safe):     {cm[0][0]}")
    print(f"  False Positives (Safe wrongly Blocked): {cm[0][1]}")
    print(f"  False Negatives (Risk MISSED):          {cm[1][0]}  << MINIMIZE THIS")
    print(f"  True Positives  (Risk Blocked):         {cm[1][1]}")

    # ----------------------------------------------------------
    # Verification Phrases
    # ----------------------------------------------------------
    print("\n--- Verification on Specific Phrases ---")
    test_phrases = [
        "I had a great day!",
        "Hello",
        "Hiii",
        "Pizza is nice",
        "How are you?",
        "I want to die",
        "I want to hang myself on the fan",
        "I will use a knife to end it",
        "I want to take 10 sleeping pills",
        "I feel like ending my life",
        "I want to do sucide",
        "kill myself",
        "rope around my neck",
        "jump off the building",
        "overdose on pills",
        "I want to eat pizza",
        "The weather is nice today",
    ]
    for phrase in test_phrases:
        clean = clean_text(phrase)
        vec = vectorize_layer([clean])
        score = model.predict(vec, verbose=0)[0][0]
        label = "HIGH-RISK" if score >= 0.5 else "SAFE"
        print(f"  [{label:>9}] Score: {score:.4f} | '{phrase}'")

    # ----------------------------------------------------------
    # Export to TFLite
    # ----------------------------------------------------------
    print("\n--- Exporting to TFLite ---")

    # In TensorFlow 2.16+ (Keras 3), use model.export() to save a SavedModel format for TFLite
    saved_model_path = os.path.join(OUTPUT_DIR, "saved_model")
    model.export(saved_model_path)

    # Convert to TFLite
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_model_path)
    # Important for Custom/SelfAttention layers:
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS, # enable TensorFlow Lite ops.
        tf.lite.OpsSet.SELECT_TF_OPS # enable TensorFlow ops.
    ]
    tflite_model = converter.convert()

    tflite_path = os.path.join(OUTPUT_DIR, "custom_safety_model.tflite")
    with open(tflite_path, "wb") as f:
        f.write(tflite_model)
    print(f"  TFLite model saved: {tflite_path}")
    print(f"  Model size: {os.path.getsize(tflite_path) / (1024*1024):.2f} MB")

    # ----------------------------------------------------------
    # Copy to Android assets
    # ----------------------------------------------------------
    print("\n--- Copying to Android assets ---")
    os.makedirs(ASSETS_DIR, exist_ok=True)

    import shutil
    shutil.copy2(tflite_path, os.path.join(ASSETS_DIR, "custom_safety_model.tflite"))
    shutil.copy2(vocab_path, os.path.join(ASSETS_DIR, "vocab.txt"))
    print(f"  Copied to: {ASSETS_DIR}")

    print("\n" + "=" * 60)
    print("  Training Complete!")
    print("=" * 60)


if __name__ == "__main__":
    train()
