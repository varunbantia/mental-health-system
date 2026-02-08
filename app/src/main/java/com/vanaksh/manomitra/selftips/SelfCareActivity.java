package com.vanaksh.manomitra.selftips;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.vanaksh.manomitra.R;
import com.yuyakaido.android.cardstackview.*;
import java.util.ArrayList;
import java.util.List;

public class SelfCareActivity extends AppCompatActivity implements CardStackListener {

    private CardStackView cardStackView;
    private CardStackLayoutManager manager;
    private LinearLayout dotsContainer;
    private TextView tvCounter;
    private ImageView[] dots;
    private List<SelfCareTip> tipList = new ArrayList<>();
    private SelfCareAdapter adapter;
    private FirebaseFirestore db;
    private List<String> savedTipIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_care);

        db = FirebaseFirestore.getInstance();
        cardStackView = findViewById(R.id.card_stack_view);
        dotsContainer = findViewById(R.id.dotsContainer);
        tvCounter = findViewById(R.id.tv_counter);

        // 1. Initialize LayoutManager and Adapter
        setupCardStack();

        // 2. Load Local Data
        loadLocalTips();

        // 3. Initialize UI
        setupDots(tipList.size());
        updateUI(0);

        findViewById(R.id.btn_view_saved).setOnClickListener(v -> {
            startActivity(new Intent(this, SavedTipsActivity.class));
        });
    }

    private void loadLocalTips() {
        tipList.clear();
        tipList.add(new SelfCareTip("Take 5 deep breaths slowly, focusing on the air filling your lungs.", "#D1F2E5", "MINDFULNESS", "tip_1", R.drawable.ic_coffee));
        tipList.add(new SelfCareTip("Drink a full glass of water. Your brain is 75% water!", "#E3F2FD", "HYDRATION", "tip_2", R.drawable.ic_coffee));
        tipList.add(new SelfCareTip("Stretch your shoulders and neck for 30 seconds to release tension.", "#FFF9C4", "MOVEMENT", "tip_3", R.drawable.ic_coffee));
        tipList.add(new SelfCareTip("Write down one thing you are truly grateful for today.", "#F8D7DA", "MINDSET", "tip_4", R.drawable.ic_face));
        tipList.add(new SelfCareTip("Step outside for 2 minutes and feel the fresh air on your face.", "#E2E3E5", "NATURE", "tip_5", R.drawable.ic_tree));
        tipList.add(new SelfCareTip("Close your eyes and let your mind drift for 120 seconds. No screens!", "#E9D8FD", "REST", "tip_6", R.drawable.ic_face));
        tipList.add(new SelfCareTip("Put your phone on 'Do Not Disturb' for the next 20 minutes.", "#FEEBC8", "DIGITAL DETOX", "tip_7", R.drawable.ic_learn));
        tipList.add(new SelfCareTip("Send a quick 'thinking of you' text to a friend or family member.", "#C6F6D5", "CONNECTION", "tip_8", R.drawable.ic_learn));
        tipList.add(new SelfCareTip("Clear just one small section of your desk or workspace.", "#BEE3F8", "CLARITY", "tip_9", R.drawable.ic_learn));
        tipList.add(new SelfCareTip("Look in the mirror and give yourself one genuine compliment.", "#FED7E2", "SELF-LOVE", "tip_10", R.drawable.ic_learn));

        adapter.notifyDataSetChanged();
    }

    private void setupCardStack() {
        manager = new CardStackLayoutManager(this, this);
        manager.setStackFrom(StackFrom.None);
        manager.setVisibleCount(1);
        manager.setCanScrollVertical(false); // Only horizontal swipe

        adapter = new SelfCareAdapter(tipList);
        cardStackView.setLayoutManager(manager);
        cardStackView.setAdapter(adapter);
    }

    private void saveTipToFirebase(SelfCareTip tip) {
        String uid = FirebaseAuth.getInstance().getUid();

        if (uid == null) {
            Toast.makeText(this, "Please log in to save tips", Toast.LENGTH_SHORT).show();
            return;
        }

        // LOCAL CHECK: Don't even call Firebase if we swiped this in the current session
        if (savedTipIds.contains(tip.getTipId())) {
            Log.d("DEBUG_SAVE", "SKIP: Tip " + tip.getTipId() + " already saved this session.");
            return;
        }

        Log.d("DEBUG_SAVE", "--- SAVE ATTEMPT START ---");
        Log.d("DEBUG_SAVE", "Full Path: users/" + uid + "/tips/" + tip.getTipId());

        db.collection("users").document(uid)
                .collection("tips")
                .document(tip.getTipId())
                .set(tip)
                .addOnSuccessListener(aVoid -> {
                    Log.d("DEBUG_SAVE", "SUCCESS: Tip saved!");
                    savedTipIds.add(tip.getTipId()); // Add to local list to prevent duplicate Toast
                    Toast.makeText(this, "Saved to your tips!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("DEBUG_SAVE", "FAILURE: " + e.getMessage());
                    Toast.makeText(this, "Save Failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onCardSwiped(Direction direction) {
        int swipedPosition = manager.getTopPosition() - 1;

        // Save logic
        if (direction == Direction.Right && swipedPosition >= 0) {
            saveTipToFirebase(tipList.get(swipedPosition));
        }

        // Restart Logic
        if (manager.getTopPosition() == tipList.size()) {
            Toast.makeText(this, "Starting over!", Toast.LENGTH_SHORT).show();
            cardStackView.post(() -> {
                cardStackView.scrollToPosition(0);
                updateUI(0);
            });
        } else {
            updateUI(manager.getTopPosition());
        }
    }

    private void setupDots(int size) {
        dotsContainer.removeAllViews();
        dots = new ImageView[size];
        for (int i = 0; i < size; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(25, 25);
            params.setMargins(10, 0, 10, 0);
            dotsContainer.addView(dots[i], params);
        }
        if (dots.length > 0) dots[0].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_active));
    }

    private void updateUI(int position) {
        if (position < tipList.size()) {
            tvCounter.setText((position + 1) + " of " + tipList.size());
            if (dots != null) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(this,
                            i == position ? R.drawable.dot_active : R.drawable.dot_inactive));
                }
            }
        }
    }

    @Override public void onCardAppeared(View view, int position) {
        updateUI(position);
    }

    @Override public void onCardDragging(Direction direction, float ratio) {}
    @Override public void onCardDisappeared(View view, int position) {}
    @Override public void onCardRewound() {}
    @Override public void onCardCanceled() {}
}