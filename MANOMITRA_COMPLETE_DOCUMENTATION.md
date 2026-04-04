# ManoMitra Central Documentation

ManoMitra is a comprehensive, multi-role mental health support system designed to provide accessible, safe, and empathetic care through AI-driven interactions, professional counseling, and community support.

---

## 1. Project Overview
ManoMitra is designed to bridge the gap between students and mental health resources. It provides a 24/7 AI companion, a streamlined scheduling system for on-campus and helpline specialists, and a suite of wellness tools for daily self-care.

### Core Objectives
- **Accessibility**: Instant mental health support via AI and voice interaction.
- **Safety**: Robust, on-device safety checks to identify and intervene in crisis situations.
- **Professionalism**: Secure booking and communication portals for qualified counselors.
- **Privacy**: Support for anonymous interactions while maintaining high-quality support.

---

## 2. Detailed Application Features

### A. Unified Dashboard (Student/User)
The central hub for mental health navigation:
- **Real-time Mood Tracker**: Interactive 4-state mood selection (Good, Okay, Low, Very Low) with instant empathetic feedback.
- **Quick-Action Shortcuts**: High-priority access to the **ManoMitra Chatbot**, **Wellness Hub**, and **Professional Booking**.
- **Emergency Support**: A dedicated high-visibility button for immediate crisis intervention and helpline access.
- **Smart Notification Badges**: Real-time unread message counts for counselor-student communications.

### B. Professional Counseling & Booking
- **Specialist Categorization**: Choice between **On-Campus Specialists** and **Mental Health Helplines**.
- **Verification System**: Visual "Verified" badges for trusted professionals.
- **Comprehensive Profile View**: See a counselor's specialization, experience, and real-time availability.
- **Booking Management**: Step-by-step DateTime selection and multi-state status tracking (Pending, Confirmed, Completed).

### C. Role-Based Ecosystems (Admin, Counselor, Volunteer)
- **Counselor Dashboard**: Real-time counters for Pending Bookings, Active Sessions, and Crisis Alerts. Includes secure chat with confirmed students.
- **Administrative Analytics**: KPI tracking (Total/Active Users, Appointments) and trend visualization for specific crisis types (Anxiety, Depression, etc.).
- **Volunteer Portal**: Platform for moderated community contribution and support tasks.

### D. Wellness Hub & Self-Care
- **Focus Timer**: Pomodoro-style productivity tool with customizable countdowns and auditory signals.
- **Activity Categories**: Dedicated modules for **Relaxation**, **Learning**, and **Sleep Improvement**.
- **Self-Care Library**: Curated tips for Affirmations, Breathing exercises, and Focused meditation.
- **Multilingual Support**: Core resources available in 6 regional languages (English, Hindi, Bengali, Marathi, Tamil, Telugu).

---

## 3. AI Chatbot Technical In-Depth

The ManoMitra AI Chatbot is a sophisticated assistant built with a "Safety-First" architecture.

### A. Multi-Layer Safety Architecture
1. **Local Safety Layer (TFLite)**: 
   - Uses **Universal Sentence Encoder (USE)** to convert messages into 512-dimensional vector embeddings locally.
   - A **Safety Classifier** analyzes the embeddings to detect high-risk topics.
   - **Keyword Fallback** acts as a fail-safe if models fail to load.
2. **Safety Interventions**: 
   - If User Risk Score > 0.5, AI processing is bypassed.
   - Users are immediately provided with verified crisis helpline numbers (e.g., iCall, NIMHANS).

### B. LLM Integration (OpenAI GPT)
- **Model**: GPT-3.5-Turbo via OpenAI API.
- **System Persona**: "ManoMitra," a compassionate, culturally sensitive companion.
- **Guardrails**: Strictly non-clinical; encourages seeking professional help and does not replace human care.

### C. Advanced Interaction Modes
- **Voice Interaction (Whisper AI)**: High-accuracy Speech-to-Text transcription via OpenAI Whisper-1, integrated into the safety pipeline.
- **Multi-Modal Feedback**: Built-in Android **Text-to-Speech (TTS)** for auditory interaction.
- **Contextual Memory**: Maintains the last 10 messages for relevant conversational continuity.

---

## 4. Authentication & Security In-Depth

### A. Authentication Providers
1. **Email & Password**: Standard registration with strong password enforcement (Upper, Lower, Special, Digit, 6+ chars).
2. **Google Sign-In**: OAuth integration for seamless entry and auto-profile provisioning.
3. **Anonymous Login (Student only)**: Privacy-first session management using temporary Firebase Auth states.

### B. Role Enforcement Logic
- **Firestore-Enforced Roles**: Upon login, the app fetches the authoritative role from Firestore.
- **Fast-Path Caching**: Roles are cached in `SharedPreferences` for instant UI updates on app restart.
- **Dynamic Portals**: The app dynamically launches role-specific "Home" Activities (e.g., `CounsellorActivity` or `DashboardActivity`) based on the validated profile.

### C. Technical Security Features
- **FCM Token Sync**: Real-time synchronization of Firebase Cloud Messaging tokens for reliable session notifications.
- **Data Persistence**: Secure, real-time synchronization of chat history and bookings via Firebase Firestore.

---

## 5. Upcoming & Backend Features
ManoMitra is actively expanding its infrastructure:
- **Peer Community**: moderated group support spaces with backend safety logic for member interaction.
- **multilingual Library**: Expansion to 8+ regional languages with dynamic, Firebase-driven content delivery.
