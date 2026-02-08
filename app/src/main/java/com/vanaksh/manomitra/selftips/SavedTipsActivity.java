package com.vanaksh.manomitra.selftips;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.vanaksh.manomitra.R;
import java.util.ArrayList;
import java.util.List;

public class SavedTipsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private SelfCareAdapter adapter;
    private List<SelfCareTip> savedTips = new ArrayList<>();
    private FirebaseFirestore db;

    // UI Elements for Header
    private TextView tvCounter;
    private LinearLayout dotsContainer;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_tips);

        // Initialize Firebase and UI
        db = FirebaseFirestore.getInstance();
        viewPager = findViewById(R.id.viewPagerSaved);
        tvCounter = findViewById(R.id.tv_counter_saved);
        dotsContainer = findViewById(R.id.dotsContainerSaved);

        // Setup Adapter
        adapter = new SelfCareAdapter(savedTips);
        viewPager.setAdapter(adapter);

        fetchSavedTips();
    }

    private void fetchSavedTips() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(uid).collection("tips")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savedTips.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        SelfCareTip tip = doc.toObject(SelfCareTip.class);
                        savedTips.add(tip);
                    }
                    adapter.notifyDataSetChanged();

                    if (!savedTips.isEmpty()) {
                        setupPaginationUI(savedTips.size());
                    } else {
                        tvCounter.setText("0 of 0");
                        Toast.makeText(this, "You haven't saved any tips yet.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupPaginationUI(int size) {
        // 1. Create the dots
        dotsContainer.removeAllViews();
        dots = new ImageView[size];
        for (int i = 0; i < size; i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_inactive));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(20, 20);
            params.setMargins(8, 0, 8, 0);
            dotsContainer.addView(dots[i], params);
        }

        // 2. Set the first dot as active and update text
        if (size > 0) {
            dots[0].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.dot_active));
            tvCounter.setText("1 of " + size);
        }

        // 3. Sync ViewPager swipes with UI
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Update Text Counter
                tvCounter.setText((position + 1) + " of " + size);

                // Update Dots
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setImageDrawable(ContextCompat.getDrawable(SavedTipsActivity.this,
                            i == position ? R.drawable.dot_active : R.drawable.dot_inactive));
                }
            }
        });
    }
}