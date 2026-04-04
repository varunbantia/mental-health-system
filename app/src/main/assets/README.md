# Placeholder TFLite Models

This directory should contain the TensorFlow Lite models for the safety layer.

## Required Files

1. **use_model.tflite** - Universal Sentence Encoder (text to 512-dim embeddings)
2. **safety_classifier.tflite** - FCNN classifier (embedding to risk probability)

## How to Generate

Run the Python training script:

```bash
cd training
python train_safety_model.py
```

Then copy the output files:

```bash
cp training/output_models/use_model.tflite app/src/main/assets/
cp training/output_models/safety_classifier.tflite app/src/main/assets/
```

## Fallback Behavior

If these files are not present, the safety layer will use a keyword-based fallback classifier that still provides protection.
