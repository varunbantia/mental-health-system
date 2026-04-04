package com.vanaksh.manomitra.ui.roles;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.auth.LoginActivity;
import com.vanaksh.manomitra.utils.RoleManager;

public class ModeratorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_moderator);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupCards();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_role_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        RoleManager.performLogout(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    private void setupCards() {
        findViewById(R.id.cardApprovePosts).setOnClickListener(
                v -> Toast.makeText(this, "Approve Posts — Coming Soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardViewReports)
                .setOnClickListener(v -> Toast.makeText(this, "View Reports — Coming Soon", Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardEscalateCases).setOnClickListener(
                v -> Toast.makeText(this, "Escalate Cases — Coming Soon", Toast.LENGTH_SHORT).show());
    }
}
