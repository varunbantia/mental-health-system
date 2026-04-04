package com.vanaksh.manomitra.community;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.data.model.Post;
import com.vanaksh.manomitra.data.repository.PeerSupportRepository;
import com.vanaksh.manomitra.databinding.ActivityCreatePostBinding;
import com.vanaksh.manomitra.util.CrisisDetectionUtils;
import java.util.Objects;

public class CreatePostActivity extends AppCompatActivity {

    private ActivityCreatePostBinding binding;
    private PeerSupportRepository repository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new PeerSupportRepository();
        currentUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        setupListeners();
    }

    private void setupListeners() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        binding.btnSubmit.setOnClickListener(v -> {
            if (validateFields()) {
                submitPost();
            }
        });

        TextWatcher crisisWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkCrisis();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        binding.etTitle.addTextChangedListener(crisisWatcher);
        binding.etDesc.addTextChangedListener(crisisWatcher);
    }

    private void checkCrisis() {
        String title = binding.etTitle.getText().toString();
        String desc = binding.etDesc.getText().toString();
        
        if (CrisisDetectionUtils.detectCrisis(title) || CrisisDetectionUtils.detectCrisis(desc)) {
            binding.crisisBanner.setVisibility(View.VISIBLE);
        } else {
            binding.crisisBanner.setVisibility(View.GONE);
        }
    }

    private boolean validateFields() {
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDesc.getText().toString().trim();
        boolean isValid = true;

        if (title.isEmpty()) {
            binding.tilTitle.setError(getString(R.string.error_title_required));
            isValid = false;
        } else {
            binding.tilTitle.setError(null);
        }

        if (desc.isEmpty()) {
            binding.tilDesc.setError(getString(R.string.error_desc_required));
            isValid = false;
        } else {
            binding.tilDesc.setError(null);
        }

        return isValid;
    }

    private void submitPost() {
        String title = binding.etTitle.getText().toString().trim();
        String desc = binding.etDesc.getText().toString().trim();
        String category = "General Support";
        
        int selectedChipId = binding.cgCategories.getCheckedChipId();
        if (selectedChipId == R.id.chip_anxiety) category = "Anxiety";
        else if (selectedChipId == R.id.chip_depression) category = "Depression";
        else if (selectedChipId == R.id.chip_academic) category = "Academic Stress";
        else if (selectedChipId == R.id.chip_relationships) category = "Relationships";
        else if (selectedChipId == R.id.chip_family) category = "Family";

        boolean anonymous = binding.switchAnonymous.isChecked();
        
        Post post = new Post(null, currentUserId, title, desc, category, 
                System.currentTimeMillis(), "approved", anonymous);
        
        repository.createPost(post);
        Toast.makeText(this, getString(R.string.toast_post_success), Toast.LENGTH_SHORT).show();
        finish();
    }
}
