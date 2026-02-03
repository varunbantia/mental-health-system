package com.vanaksh.manomitra.ui.chatbot;

// --- Android & System Imports ---

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

// --- AndroidX Imports ---
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

// --- Firebase Imports ---

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.ChatMessage;
import com.vanaksh.manomitra.wellness.LearmActivity;

// --- Network Imports ---
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;




public class ChatbotActivity extends AppCompatActivity implements
        ChatAdapter.OnPlayButtonClickListener {
    private static final String TAG = "ChatbotActivity";

    private ActivityResultLauncher<Intent> chatHistoryLauncher;
    // --- 🌎 Constants ---
    private static final int MIC_PERMISSION_REQUEST_CODE = 200;
    private static final int NOTIFICATION_PERMISSION_CODE = 101;
    private static final String PREF_KEY_PROFILE_PROMPT_SHOWN = "first_message_profile_prompt_shown";

    // --- 🎨 Views ---
    private RecyclerView recyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnMic, btnMenu;
    private TextView tvWelcome;
    private LinearLayout textInputLayout, voiceRecordingLayout, reviewVoiceLayout;
    private TextView tvRecordingTime, tvSlideToCancel, tvVoiceDuration;
    private ImageButton btnDeleteVoice, btnPlayPause, btnSendVoice, btnStopRecording;
    private ImageView ivRecordingDot ,ivLock, ivLockArrow;
    private SeekBar voiceSeekBar;
    private FrameLayout fragmentContainer;
    private ViewGroup inputArea;
    private ViewGroup lockViewContainer;

    // --- ✨ Animation ---
    private Animation pulseAnimation;
    private Animation bounceAnimation;
    private Animation slideToCancelAnimation;

    // --- 🧠 Data & Logic ---
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private TextToSpeech tts;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());

    // --- 🎤 Audio Recording & Playback ---
    private MediaRecorder recorder;
    private MediaPlayer reviewPlayer;
    private File audioFile;
    private long startTime = 0;

    // --- 🎧 Chat Playback ---
    private MediaPlayer chatPlayer;
    private ImageButton currentlyPlayingButton = null;
    private SeekBar currentlyPlayingSeekBar = null;
    private String currentlyPlayingFilePath = null;
    private final Handler chatSeekBarHandler = new Handler(Looper.getMainLooper());

    // --- ⚙️ State Management ---
    private boolean isActive = false;
    private boolean isRecording = false;
    private boolean isLocked = false;
    private String currentlySpeakingText = null;
    private String currentChatId = null;
    private boolean isProfileVerifiedIncomplete = false;
    private volatile boolean isBotResponding = false;

    private String selectedLanguageCode = "en-IN";
    private float initialX = 0f;
    private float initialY = 0f;
    private Vibrator vibrator;


    // ---------------------------------------------------------------------------------------------
    // 1. 🚀 ACTIVITY LIFECYCLE
    // ---------------------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);
        chatHistoryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // This is the callback that runs when ChatHistoryActivity closes
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        ArrayList<String> deletedIds = result.getData().getStringArrayListExtra("DELETED_CHAT_IDS");

                        if (deletedIds != null && currentChatId != null && deletedIds.contains(currentChatId)) {
                            // The chat we are currently viewing was deleted!
                            // We must reset the activity.
                            Log.d(TAG, "Current chat was deleted. Clearing state.");
                            clearChatState();
                        }
                    }
                }
        );
        // Run all setup methods
        initializeViews();
        setupKeyboardScrollListener();
        setupFirebase();
        setupRecyclerView();
        setupTts();

        setupInputListeners();


    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true; // App is foreground/visible
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActive = false; // App is background/hidden
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the activity's intent
         // Handle the prompt
    }

    @Override
    public void onBackPressed() {
        // Close fragment if one is open
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return; // Consume the back press
        }

        // Handle custom back navigation from Chat History
        boolean fromHistory = getIntent().getBooleanExtra("FROM_HISTORY", false);
        if (fromHistory) {
            Intent intent = new Intent(ChatbotActivity.this, LearmActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (reviewPlayer != null) {
            reviewPlayer.release();
            reviewPlayer = null;
        }
        if (chatPlayer != null) {
            chatPlayer.release();
            chatPlayer = null;
        }
        // Cancel any lingering network requests

        // Stop all handlers
        timerHandler.removeCallbacksAndMessages(null);
        chatSeekBarHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    // ---------------------------------------------------------------------------------------------
    // 2. 🛠️ INITIALIZATION
    // ---------------------------------------------------------------------------------------------

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.btnMic);
        tvWelcome = findViewById(R.id.tvWelcome);
        textInputLayout = findViewById(R.id.textInputLayout);
        voiceRecordingLayout = findViewById(R.id.voiceRecordingLayout);
        tvRecordingTime = findViewById(R.id.tvRecordingTime);
        tvSlideToCancel = findViewById(R.id.tvSlideToCancel);
        ivRecordingDot = findViewById(R.id.ivRecordingDot);

        // --- MODIFIED: Get all lock components ---
        ivLock = findViewById(R.id.ivLock);
        ivLockArrow = findViewById(R.id.ivLockArrow);
        lockViewContainer = findViewById(R.id.lock_view_container);
        // --- END MODIFIED ---

        reviewVoiceLayout = findViewById(R.id.reviewVoiceLayout);
        btnDeleteVoice = findViewById(R.id.btnDeleteVoice);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnSendVoice = findViewById(R.id.btnSendVoice);
        btnStopRecording = findViewById(R.id.btnStopRecording);
        voiceSeekBar = findViewById(R.id.voiceSeekBar);
        tvVoiceDuration = findViewById(R.id.tvVoiceDuration);
        fragmentContainer = findViewById(R.id.fragmentContainer);
        inputArea = findViewById(R.id.inputArea);

        // Load animations
        pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulsing_red_dot);
        bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_up);
        slideToCancelAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_to_cancel_animation); // --- ADDED ---

        // Get Vibrator service
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Set window preferences
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        etMessage.requestFocus();
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, this); // 'this' is the OnPlayButtonClickListener

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
    }
    /**
     * Resets the chat activity to a fresh, new chat.
     * Called when the current chat is deleted from ChatHistoryActivity.
     */
    private void clearChatState() {
        // 1. Stop any Firestore listeners by detaching
        //    (This assumes your listener is a class variable, which it should be)
        //    If not, you'll need to refactor loadChatHistory to support detaching.
        //    For now, we will just clear the local data.

        // 2. Clear the local data
        currentChatId = null;
        chatMessages.clear();

        // 3. Add the "Hello" message back
        chatMessages.add(new ChatMessage("Hello! How can I assist you today?", ChatMessage.TYPE_BOT));
        chatAdapter.notifyDataSetChanged();
        recyclerView.scrollToPosition(0);

        // 4. Clear the intent extra so it doesn't try to reload the old chat
        getIntent().removeExtra("CHAT_ID");
    }
    /**
     * Manually detects when the keyboard opens and forces the RecyclerView to scroll to the bottom.
     */
    private void setupKeyboardScrollListener() {
        final CoordinatorLayout rootLayout = findViewById(R.id.chatRootLayout);
        rootLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private final Rect r = new Rect();
            private boolean wasOpen = false;

            @Override
            public void onGlobalLayout() {
                rootLayout.getWindowVisibleDisplayFrame(r);
                int screenHeight = rootLayout.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;
                boolean isOpen = keypadHeight > screenHeight * 0.15;

                if (isOpen == wasOpen) return; // No change
                wasOpen = isOpen;

                if (isOpen) {
                    // Keyboard opened, scroll to bottom (position 0)
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
                            recyclerView.scrollToPosition(0);
                        }
                    }, 100); // 100ms delay for animation
                }
            }
        });
    }

    /**
     * Checks the intent for extras, like loading a specific chat history or a mock interview prompt.
     */


    private void setupFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        loadUserNameAndCheckProfile();
    }

    private void setupTts() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("en", "IN")); // Set specific locale
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        runOnUiThread(() -> currentlySpeakingText = utteranceId);
                    }
                    @Override
                    public void onDone(String utteranceId) {
                        runOnUiThread(() -> currentlySpeakingText = null);
                    }
                    @Override
                    public void onError(String utteranceId) {
                        runOnUiThread(() -> currentlySpeakingText = null);
                    }
                });
            } else {
                Log.e(TAG, "TTS Initialization failed.");
            }
        });
    }
    private void sendMessageToBot(String userMessage, boolean addUiMessage) {
        if (addUiMessage) {
            // Add User Message to list locally
            ChatMessage userMsg = new ChatMessage(userMessage, ChatMessage.TYPE_USER_TEXT);
            chatMessages.add(0, userMsg);
            chatAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);
        }

        // 1. Show Loading Indicator
        isBotResponding = true;
        setRespondingState();
        addLoadingMessage();

        // 2. Dummy Delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            // 3. Remove Loading Indicator
            removeLoadingMessage();

            // 4. Get and Display Bot Response
            String reply = getDummyBotReply(userMessage);
            ChatMessage botMsg = new ChatMessage(reply, ChatMessage.TYPE_BOT);

            chatMessages.add(0, botMsg);
            chatAdapter.notifyItemInserted(0);
            recyclerView.scrollToPosition(0);

            // 5. Reset UI State
            isBotResponding = false;
            setIdleState();

        }, 1200); // 1.2 second delay for realism
    }

    private void sendAudioToBackend(File file) {
        // 1. Set UI to "Bot is thinking" state
        isBotResponding = true;
        setRespondingState();
        addLoadingMessage();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 2. Remove the loading dots
            removeLoadingMessage();

            // 3. Create the dummy transcription message
            String fakeTranscription = "This is a dummy transcription of your voice.";
            ChatMessage transcriptionMsg = new ChatMessage(fakeTranscription, ChatMessage.TYPE_BOT);

            // 4. Update the UI list locally
            chatMessages.add(0, transcriptionMsg);
            chatAdapter.notifyItemInserted(0);

            // 5. Reset UI to idle
            isBotResponding = false;
            setIdleState();

            // 6. Scroll to the new message
            recyclerView.scrollToPosition(0);

        }, 2000); // 2-second delay to simulate processing
    }
    private String getDummyBotReply(String userMessage) {

        userMessage = userMessage.toLowerCase();

        if (userMessage.contains("hello") || userMessage.contains("hi")) {
            return "Hello! 👋 I'm your offline RozgarAI assistant.";
        }
        if (userMessage.contains("job")) {
            return "You can explore jobs in IT, Data Science, UI/UX, and Cybersecurity.";
        }
        if (userMessage.contains("resume")) {
            return "Tip: Keep your resume to 1 page and focus on measurable achievements.";
        }
        if (userMessage.contains("interview")) {
            return "Prepare STAR method answers: Situation, Task, Action, Result.";
        }
        if (userMessage.contains("bye")) {
            return "Goodbye! 👋 Come back anytime.";
        }

        return "I'm currently running offline. Your message was: \"" + userMessage + "\"";
    }

    private void addUserMessage(String message, String audioPath) {
        // Just create the object and add it to the list locally
        ChatMessage chatMessage;
        if (audioPath != null) {
            chatMessage = new ChatMessage(message, audioPath, ChatMessage.TYPE_USER_VOICE);
        } else {
            chatMessage = new ChatMessage(message, ChatMessage.TYPE_USER_TEXT);
        }
        chatMessages.add(0, chatMessage); // Add to the start of the list (since it's reversed)
        chatAdapter.notifyItemInserted(0); // Tell the adapter to animate the new item
        recyclerView.scrollToPosition(0);
    }
    private void addBotMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, ChatMessage.TYPE_BOT);
        saveMessage(chatMessage); // Save to Firestore
    }
    private void addLoadingMessage() {
        if (!chatMessages.isEmpty() && chatMessages.get(0).getType() == ChatMessage.TYPE_LOADING) {
            return; // Already loading
        }
        chatMessages.add(0, new ChatMessage(ChatMessage.TYPE_LOADING));
        chatAdapter.notifyItemInserted(0);
        recyclerView.scrollToPosition(0);
    }
    private void removeLoadingMessage() {
        if (chatMessages.isEmpty()) return;
        if (chatMessages.get(0).getType() == ChatMessage.TYPE_LOADING) {
            chatMessages.remove(0);
            chatAdapter.notifyItemRemoved(0);
        }
    }


    // ---------------------------------------------------------------------------------------------
    // 4. 🎹 INPUT & BUTTONS (TEXT, SEND, STOP)
    // ---------------------------------------------------------------------------------------------

    private void setupInputListeners() {
        // Text watcher for send/mic button
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isBotResponding) {
                    if (s.toString().trim().isEmpty()) {
                        btnSend.setVisibility(View.GONE);
                        btnMic.setVisibility(View.VISIBLE);
                    } else {
                        btnSend.setVisibility(View.VISIBLE);
                        btnMic.setVisibility(View.GONE);
                    }
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Send/Stop button click
        btnSend.setOnClickListener(v -> handleSendStopClick());

        // ---
        // --- 🚀 MODIFIED: "WHATSAPP-STYLE" MIC LISTENER 🚀 ---
        // ---

        final float LOCK_THRESHOLD_Y = 250f; // Pixels to swipe up to lock
        final float CANCEL_THRESHOLD_X = 200f; // Pixels to swipe left to cancel

        btnMic.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = event.getRawX();
                    initialY = event.getRawY();
                    if (checkPermissions()) {
                        startRecording(); // This will show ivLock and tvSlideToCancel
                        v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); // Haptic feedback
                    } else {
                        requestMicPermission();
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (isRecording && !isLocked) {
                        float currentX = event.getRawX();
                        float currentY = event.getRawY();

                        float deltaX = currentX - initialX;
                        float deltaY = currentY - initialY;

                        // --- Swipe Left (Cancel) Logic ---
                        if (deltaX < -50) { // Start animating left
                            // We only use the gesture to change the text
                            // The animation XML handles the movement

                            // Fade out lock on cancel
                            float alpha = 1.0f - (Math.abs(deltaX) / CANCEL_THRESHOLD_X);
                            if (lockViewContainer != null) {
                                lockViewContainer.setAlpha(alpha);
                            }

                            if (Math.abs(deltaX) > CANCEL_THRESHOLD_X) {
                                tvSlideToCancel.setText("Release to cancel");
                            } else {
                                tvSlideToCancel.setText("< Slide to cancel");
                            }

                            // Reset lock icon translation
                            if (lockViewContainer != null) {
                                lockViewContainer.setTranslationY(0f);
                            }
                        }
                        // --- Swipe Up (Lock) Logic ---
                        else if (deltaY < -50) { // Start animating up

                            // --- Stop the slide-to-cancel animation
                            tvSlideToCancel.clearAnimation();
                            tvSlideToCancel.setAlpha(1.0f); // Make sure it's visible

                            float translationY = Math.max(-LOCK_THRESHOLD_Y, deltaY);

                            if (lockViewContainer != null) {
                                // Move the lock icon up
                                lockViewContainer.setTranslationY(translationY);
                                lockViewContainer.setAlpha(1.0f); // Keep it visible
                            }

                            // Check if threshold is passed
                            if (Math.abs(deltaY) >= LOCK_THRESHOLD_Y) {
                                lockRecording(); // This will set isLocked = true

                                // Vibrate on lock
                                if (vibrator != null) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                                    } else {
                                        vibrator.vibrate(50);
                                    }
                                }
                            }

                            // Reset cancel text if user swipes up
                            tvSlideToCancel.setText("< Slide to cancel");
                        }
                        // --- User is just holding, reset ---
                        else {
                            tvSlideToCancel.setText("< Slide to cancel");

                            // Ensure slide animation is running
                            if (tvSlideToCancel.getAnimation() == null) {
                                tvSlideToCancel.startAnimation(slideToCancelAnimation);
                            }

                            if (lockViewContainer != null) {
                                lockViewContainer.setTranslationY(0f);
                                lockViewContainer.setAlpha(0.7f); // Dim it
                            }
                        }
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    v.performClick();
                    if (isRecording && !isLocked) {
                        float finalX = event.getRawX();
                        if (initialX - finalX > CANCEL_THRESHOLD_X) { // Check cancel threshold
                            cancelRecording();
                        } else {
                            stopAndSendRecording();
                        }
                    }
                    // If it *was* locked, stopAndSend/cancel is handled by the dedicated buttons
                    return true;
            }
            return false;
        });

        // --- END OF NEW MIC LISTENER ---

        // --- Review Layout Button Listeners (Unchanged) ---
        btnStopRecording.setOnClickListener(v -> stopRecordingAndShowReview());
        btnDeleteVoice.setOnClickListener(v -> deleteRecording());
        btnPlayPause.setOnClickListener(v -> playOrPauseReview());
        btnSendVoice.setOnClickListener(v -> sendRecording());
    }

    private void handleSendStopClick() {
        if (isBotResponding) {
            // --- STOP click ---
            Log.d(TAG, "Stop button clicked.");

        } else {
            // --- SEND click ---
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                Log.d(TAG, "Send button clicked.");
                etMessage.setText("");
                // ⬇️ MODIFIED: Pass 'true'
                sendMessageToBot(message, true);
            }
        }
    }

    private void setRespondingState() {
        isBotResponding = true;
        btnSend.setImageResource(R.drawable.ic_pause); // Change to STOP icon
        btnSend.setVisibility(View.VISIBLE);
        btnMic.setVisibility(View.GONE);
    }

    private void setIdleState() {
        isBotResponding = false;
        btnSend.setImageResource(R.drawable.ic_send); // Change back to SEND icon

        // Restore correct button based on text input
        if (etMessage.getText().toString().trim().isEmpty()) {
            btnSend.setVisibility(View.GONE);
            btnMic.setVisibility(View.VISIBLE);
        } else {
            btnSend.setVisibility(View.VISIBLE);
            btnMic.setVisibility(View.GONE);
        }
    }
    private void resetInputUI() {
        // 🚀 Animate the layout change
        TransitionManager.beginDelayedTransition(inputArea);

        textInputLayout.setVisibility(View.VISIBLE);
        voiceRecordingLayout.setVisibility(View.GONE);
        reviewVoiceLayout.setVisibility(View.GONE);

        // Reset recording layout
        btnStopRecording.setVisibility(View.GONE);
        ivRecordingDot.setVisibility(View.VISIBLE);
        ivRecordingDot.clearAnimation();
        tvSlideToCancel.setVisibility(View.VISIBLE);
        tvSlideToCancel.setText("< Slide to cancel");
        tvSlideToCancel.clearAnimation();
        tvRecordingTime.setText("0:00");

        // --- LOCK LOGIC FIX ---
        tvSlideToCancel.setTranslationX(0f);
        tvSlideToCancel.setAlpha(1.0f);
        if (lockViewContainer != null) {
            lockViewContainer.setVisibility(View.GONE);
            lockViewContainer.setTranslationY(0f);
            lockViewContainer.setAlpha(1.0f);
        }
        if (ivLockArrow != null) {
            ivLockArrow.clearAnimation();
        }
        // --- END FIX ---

        // Cleanup
        isRecording = false;
        isLocked = false;
        timerHandler.removeCallbacks(updateTimer);
        timerHandler.removeCallbacks(updateReviewSeekBar);
        if (reviewPlayer != null) {
            reviewPlayer.release();
            reviewPlayer = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 5. 🎤 VOICE RECORDING & REVIEW
    // ---------------------------------------------------------------------------------------------

    private void startRecording() {
        try {
            audioFile = File.createTempFile("voice_input", ".3gp", getCacheDir());
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(audioFile.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            isRecording = true;

            // 🚀 Animate the UI change
            TransitionManager.beginDelayedTransition(inputArea);
            textInputLayout.setVisibility(View.GONE);
            voiceRecordingLayout.setVisibility(View.VISIBLE);

            // --- LOCK LOGIC: MODIFIED ---
            if (lockViewContainer != null) {
                lockViewContainer.setVisibility(View.VISIBLE);
                lockViewContainer.setTranslationY(0f);
                lockViewContainer.setScaleX(1.0f);
                lockViewContainer.setScaleY(1.0f);
                lockViewContainer.setAlpha(0.7f); // Start dim
            }
            if (ivLockArrow != null) {
                ivLockArrow.setVisibility(View.VISIBLE);
                ivLockArrow.clearAnimation();
                ivLockArrow.startAnimation(bounceAnimation); // Start bouncing
            }
            tvSlideToCancel.setTranslationX(0f); // Reset position
            tvSlideToCancel.setAlpha(1.0f);
            tvSlideToCancel.startAnimation(slideToCancelAnimation); // --- START SLIDE ANIMATION ---
            // --- END LOCK LOGIC ---

            // Start animations
            ivRecordingDot.startAnimation(pulseAnimation);

            startTime = System.currentTimeMillis();
            timerHandler.post(updateTimer);
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
        }
    }

    private void lockRecording() {
        isLocked = true;
        // 🚀 Animate the lock UI
        TransitionManager.beginDelayedTransition(voiceRecordingLayout);
        tvSlideToCancel.setVisibility(View.GONE);
        tvSlideToCancel.clearAnimation(); // --- STOP SLIDE ANIMATION ---
        ivRecordingDot.setVisibility(View.GONE);
        ivRecordingDot.clearAnimation();
        btnStopRecording.setVisibility(View.VISIBLE);

        // --- LOCK LOGIC FIX ---
        if (ivLockArrow != null) {
            ivLockArrow.clearAnimation(); // Stop bouncing
            ivLockArrow.setVisibility(View.GONE);
        }
        if (lockViewContainer != null) {
            // Animate lock to its final state
            lockViewContainer.animate()
                    .translationY(0f)
                    .scaleX(1.2f) // Make it pop
                    .scaleY(1.2f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        // Reset to normal size after the "pop"
                        lockViewContainer.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    })
                    .start();
            lockViewContainer.setAlpha(1.0f); // Make it fully opaque
        }
        // --- END FIX ---
    }

    private void stopAndSendRecording() {
        if (!isRecording) return;
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            addUserMessage("🎤 Voice Message", audioFile.getAbsolutePath());
            sendAudioToBackend(audioFile);
        } catch (Exception e) {
            Log.e(TAG, "stopAndSendRecording failed", e);
        } finally {
            // --- THIS IS THE FIX ---
            if (lockViewContainer != null) {
                lockViewContainer.setVisibility(View.GONE);
            }
            if (ivLockArrow != null) {
                ivLockArrow.clearAnimation();
            }
            tvSlideToCancel.clearAnimation(); // --- STOP SLIDE ANIMATION ---
            // --- END FIX ---
            resetInputUI();
        }
    }

    private void cancelRecording() {
        if (!isRecording) return;
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            if (audioFile != null) audioFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "cancelRecording failed", e);
        } finally {
            resetInputUI();
        }
    }

    private void stopRecordingAndShowReview() {
        if (!isRecording) return;
        try {
            recorder.stop();
            recorder.release();
            recorder = null;
            isRecording = false;
            timerHandler.removeCallbacks(updateTimer);
            ivRecordingDot.clearAnimation();

            // --- LOCK LOGIC FIX ---
            // Hide the lock icon before showing the review layout
            if (lockViewContainer != null) {
                lockViewContainer.setVisibility(View.GONE);
            }
            if (ivLockArrow != null) {
                ivLockArrow.clearAnimation();
            }
            tvSlideToCancel.clearAnimation(); // --- STOP SLIDE ANIMATION ---
            // --- END FIX ---

            // 🚀 Animate to review layout
            TransitionManager.beginDelayedTransition(inputArea);
            voiceRecordingLayout.setVisibility(View.GONE);
            reviewVoiceLayout.setVisibility(View.VISIBLE);

            prepareMediaPlayer();
        } catch (Exception e) {
            Log.e(TAG, "stopRecordingAndShowReview failed", e);
            resetInputUI();
        }
    }

    private void prepareMediaPlayer() {
        try {
            reviewPlayer = new MediaPlayer();
            reviewPlayer.setDataSource(audioFile.getAbsolutePath());
            reviewPlayer.prepare();
            int duration = reviewPlayer.getDuration();
            voiceSeekBar.setMax(duration);
            tvVoiceDuration.setText(String.format(Locale.getDefault(), "%d:%02d", (duration / 1000) / 60, (duration / 1000) % 60));

            reviewPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
                voiceSeekBar.setProgress(0);
            });

            voiceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) reviewPlayer.seekTo(progress);
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        } catch (IOException e) {
            Log.e(TAG, "prepareMediaPlayer failed", e);
        }
    }

    private void playOrPauseReview() {
        if (reviewPlayer == null) return;
        if (reviewPlayer.isPlaying()) {
            reviewPlayer.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play_arrow);
            timerHandler.removeCallbacks(updateReviewSeekBar);
        } else {
            reviewPlayer.start();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            timerHandler.post(updateReviewSeekBar);
        }
    }

    private void deleteRecording() {
        if (reviewPlayer != null) {
            reviewPlayer.release();
            reviewPlayer = null;
        }
        if (audioFile != null && audioFile.exists()) {
            audioFile.delete();
        }
        resetInputUI(); // 🚀 This will animate back to text input
    }

    private void sendRecording() {
        if (audioFile != null) {
            addUserMessage("🎤 Voice Message", audioFile.getAbsolutePath());
            sendAudioToBackend(audioFile);
        }
        resetInputUI(); // 🚀 This will animate back to text input
    }

    // ---------------------------------------------------------------------------------------------
    // 6. 🎧 IN-CHAT PLAYBACK & TTS (ADAPTER CALLBACKS)
    // ---------------------------------------------------------------------------------------------

    /**
     * Callback from ChatAdapter when a play button in the RecyclerView is clicked.
     */
    @Override
    public void onPlayButtonClick(String filePath, ImageButton playButton, SeekBar seekBar, TextView durationView) {
        // Stop any currently playing chat message
        if (chatPlayer != null && chatPlayer.isPlaying()) {
            chatPlayer.stop();
            chatPlayer.release();
            if (currentlyPlayingButton != null) {
                currentlyPlayingButton.setImageResource(R.drawable.ic_play_arrow);
            }
            if (currentlyPlayingSeekBar != null) {
                currentlyPlayingSeekBar.setProgress(0);
            }
            // If it's the same button, just stop and return
            if (filePath.equals(currentlyPlayingFilePath)) {
                chatPlayer = null;
                currentlyPlayingFilePath = null;
                return;
            }
        }

        // Play the new file
        try {
            currentlyPlayingFilePath = filePath;
            currentlyPlayingButton = playButton;
            currentlyPlayingSeekBar = seekBar;

            chatPlayer = new MediaPlayer();
            chatPlayer.setDataSource(filePath);
            chatPlayer.prepare();
            chatPlayer.start();

            playButton.setImageResource(R.drawable.ic_pause);
            seekBar.setMax(chatPlayer.getDuration());

            chatPlayer.setOnCompletionListener(mp -> {
                playButton.setImageResource(R.drawable.ic_play_arrow);
                seekBar.setProgress(0);
                if(chatPlayer != null) chatPlayer.release();
                chatPlayer = null;
                currentlyPlayingFilePath = null;
            });

            // Start a runnable to update the seekbar
            Runnable updateChatSeekBarRunnable = new Runnable() {
                @Override
                public void run() {
                    if (chatPlayer != null && chatPlayer.isPlaying() && seekBar.equals(currentlyPlayingSeekBar)) {
                        seekBar.setProgress(chatPlayer.getCurrentPosition());
                        chatSeekBarHandler.postDelayed(this, 500);
                    } else {
                        chatSeekBarHandler.removeCallbacks(this);
                    }
                }
            };
            chatSeekBarHandler.post(updateChatSeekBarRunnable);

        } catch (IOException e) {
            Log.e(TAG, "onPlayButtonClick failed", e);
        }
    }

    /**
     * Callback from ChatAdapter when a speaker icon is clicked.
     */
    @Override
    public void onSpeakerIconClick(String textToSpeak) {
        final boolean isStoppingThisMessage = textToSpeak.equals(currentlySpeakingText);
        tts.stop();
        if (isStoppingThisMessage) {
            currentlySpeakingText = null; // User clicked on the message that was speaking
        } else {
            speakText(textToSpeak); // User clicked on a new message
        }
    }

    public void speakText(String text) {
        if (tts != null && text != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text); // Use text as utteranceId
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 7. 🔥 FIRESTORE & PROFILE LOGIC
    // ---------------------------------------------------------------------------------------------

    private void saveMessage(ChatMessage message) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "Cannot save message, user is not logged in.");
            return;
        }
        String uid = user.getUid();

        // --- 💡 MODIFIED (FIX) 💡 ---
        if (currentChatId == null) {
            // This is a NEW chat. Create the chat ID.
            currentChatId = db.collection("users").document(uid).collection("chats").document().getId();

            // This is the first message. We must ALSO save the "Hello!"
            // message so it becomes part of the permanent history.
            ChatMessage welcomeMessage = new ChatMessage("Hello! How can I assist you today?", ChatMessage.TYPE_BOT);

            // Add the "Hello" message to Firestore.
            db.collection("users").document(uid).collection("chats").document(currentChatId)
                    .collection("messages").add(welcomeMessage);

            // NOW attach the listener. It will pick up the "Hello" message.
            loadChatHistory(currentChatId);
        }
        // --- END MODIFIED ---

        // Add the message to the "messages" subcollection
        db.collection("users").document(uid).collection("chats").document(currentChatId)
                .collection("messages").add(message);

        // Update the "chats" document with the last message and timestamp
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("lastMessage", message.getMessage());
        sessionData.put("timestamp", new Date());
        db.collection("users").document(uid).collection("chats").document(currentChatId)
                .set(sessionData, SetOptions.merge());
    }

    private void loadChatHistory(String chatId) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        db.collection("users").document(uid).collection("chats").document(chatId)
                .collection("messages").orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots == null) return;

                    List<ChatMessage> newMessages = new ArrayList<>();

                    // 1. If we are currently waiting for a bot, keep the loading state at the top
                    if (isBotResponding) {
                        newMessages.add(new ChatMessage(ChatMessage.TYPE_LOADING));
                    }

                    // 2. Add all messages from Firestore
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatMessage msg = doc.toObject(ChatMessage.class);
                        if (msg != null) {
                            newMessages.add(msg);
                        }
                    }

                    // 3. Update the adapter with the full new list
                    chatMessages.clear();
                    chatMessages.addAll(newMessages);
                    chatAdapter.notifyDataSetChanged();
                    recyclerView.scrollToPosition(0);
                });
    }
    private void loadUserNameAndCheckProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvWelcome.setText("👋 Welcome!");
            isProfileVerifiedIncomplete = true;

            return;
        }

        db.collection("users").document(user.getUid())
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);

                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        tvWelcome.setText("👋 Welcome, " + (name != null && !name.isEmpty() ? name : "") + "!");
                        String skills = documentSnapshot.getString("skills");
                        isProfileVerifiedIncomplete = (skills == null || skills.trim().isEmpty());
                    } else {
                        tvWelcome.setText("👋 Welcome!");
                        isProfileVerifiedIncomplete = true;
                    }

                });
    }
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("To record your voice queries, RozgarAI needs access to your microphone. Please grant the permission.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create()
                    .show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void showSettingsRedirectDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Permanently Denied")
                .setMessage("You have permanently denied microphone permission. To use the voice feature, you must enable it in your device's settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); // !! Don't forget super !!

        if (requestCode == MIC_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Microphone permission granted. Tap mic again.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show();
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                    showSettingsRedirectDialog(); // Permanently denied
                }
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // 10. ⏳ HELPERS & TIMERS
    // ---------------------------------------------------------------------------------------------

    private final Runnable updateTimer = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long millis = System.currentTimeMillis() - startTime;
                int seconds = (int) (millis / 1000);
                int minutes = seconds / 60;
                seconds %= 60;
                tvRecordingTime.setText(String.format(Locale.getDefault(), "%d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        }
    };

    private final Runnable updateReviewSeekBar = new Runnable() {
        @Override
        public void run() {
            if (reviewPlayer != null && reviewPlayer.isPlaying()) {
                voiceSeekBar.setProgress(reviewPlayer.getCurrentPosition());
                timerHandler.postDelayed(this, 500);
            }
        }
    };
}