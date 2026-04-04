# ManoMitra: Core Algorithm & Logic Details

This document provides structured algorithm details for the **ManoMitra** project, specifically designed for PowerPoint (PPT) slides and academic project evaluation (Algorithm Details: 5/5, Source Code Details: 5/5).

---

## 1. AI Safety & Crisis Detection Algorithm
**Slide Title**: Multi-Layer AI Safety Pipeline (On-Device)

### Flow/Logic:
1.  **Preprocessing**: Message string is normalized and tokenized.
2.  **Vectorization**: The **Universal Sentence Encoder (USE)** converts text into a **512-dimensional semantic embedding**.
3.  **Neural Classification**: The high-dimensional vector is passed through a **Deep Fully Connected Neural Network (FCNN)**:
    - **Hidden Layers**: 256, 128, 64 Units with **ReLU** activation.
    - **Regularization**: 30% **Dropout** at each layer to prevent overfitting.
4.  **Probability Output**: A **Sigmoid** activation produces a risk score $P \in [0, 1]$.
5.  **Intervention Decision**:
    - $\text{If } P > 0.5 \implies$ **Bypass AI Processing** $\to$ Redirect to **Crisis Resources**.
    - $\text{If } P \leq 0.5 \implies$ **Allow LLM Interaction**.

---

## 2. Multi-Modal Interaction & LLM Integration
**Slide Title**: Asynchronous Voice-to-Response Pipeline

### Logic (The "ManoMitra Whisper" Pipeline):
1.  **Input capture**: Audio is recorded via `MediaRecorder` in `.3gp/aac` format.
2.  **Transcription (STT)**: Audio file is sent to **OpenAI Whisper-1** via API for high-accuracy Speech-to-Text.
3.  **Safety Validation**: Transcribed text is recursively checked by the **TFLite Safety Model** (Algorithm 1).
4.  **Generative Response (LLM)**:
    - Safe text is sent to **GPT-3.5-Turbo**.
    - **System Prompting**: Enforces the "ManoMitra" persona (Compassionate, non-clinical companion).
    - **Context Management**: The last **10 messages** are sent to maintain conversational continuity.
5.  **Output (TTS)**: The response is streamed to the Android **Text-to-Speech Engine** for auditory feedback.

---

## 3. Role-Based Access Control (RBAC) Dispatcher
**Slide Title**: Secure Firebase Role Enforcement Logic

### Algorithm:
1.  **Authentication**: User signs in via `FirebaseAuth` (Email/Google/Anonymous).
2.  **Adjudication**:
    - **Step A**: Check `SharedPreferences` (Fast-Path Cache).
    - **Step B**: If cache miss, query `Firestore` collection `users/{uid}`.
3.  **Mapping**: Role string is mapped to an Activity Class using a **Role Map**:
    - `student` $\to$ `DashboardActivity`
    - `counsellor` $\to$ `CounsellorActivity`
    - `admin` $\to$ `AdminActivity`
4.  **Dispatch**: The `Intent` is configured with `FLAG_ACTIVITY_CLEAR_TASK` for state-safe transition.

---

## 4. Multi-State Booking & Slot Management
**Slide Title**: Counselor-Student Appointment State Machine

### Logic:
1.  **Filtering**: Query Firestore for Specialists with `profileCompleted == true` AND `isTrusted == true`.
2.  **Slot Checking**: Atomic checks for availability in the `DateTimeSelectionActivity`.
3.  **Transaction**: 
    - Create document in `bookings` collection.
    - Set initial state: `PENDING`.
4.  **State Transitions**:
    - **Counselor Role**: Can trigger `CONFIRMED` or `COMPLETED`.
    - **Conflict Avoidance**: Uses Firestore `whereEqualTo` filters on `counsellorId` and `dateTime` to prevent double-booking.

---

## 5. Security & Optimization Summary
- **Optimization**: Dynamic Range Quantization in TFLite reduces model size by **4x** (from ~2.4MB to ~600KB).
- **Security**: SHA-256 fingerprinting for Google Sign-In and field-level validation for all inputs.
- **Privacy**: Anonymous login handles session state without storing PII (Personally Identifiable Information).
