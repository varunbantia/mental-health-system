package com.vanaksh.manomitra.ui.roles;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.vanaksh.manomitra.R;
import com.vanaksh.manomitra.ui.auth.LoginActivity;
import com.vanaksh.manomitra.utils.RoleManager;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

        private FirebaseFirestore db;
        private ListenerRegistration analyticsListener;

        // KPI Views
        private TextView tvTotalUsers, tvActiveUsers, tvTotalAppointments;
        private TextView tvAcceptedSessions, tvCrisisAlerts, tvPeerPosts;

        // Peer Support Views
        private TextView tvPeerTotalPosts, tvPeerTotalReplies, tvMostActiveCategory;

        // Charts
        private LineChart lineChartAppointments;
        private BarChart barChartCrisis;

        // Filter
        private Spinner spinnerDateFilter;
        private String currentFilter = "last_30_days";

        private final String[] filterOptions = { "Last 7 Days", "Last 30 Days", "Last 6 Months", "All Time" };
        private final String[] filterKeys = { "last_7_days", "last_30_days", "last_6_months", "all_time" };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                EdgeToEdge.enable(this);
                setContentView(R.layout.activity_admin);

                MaterialToolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);

                db = FirebaseFirestore.getInstance();

                initViews();
                setupDateFilter();
                loadAnalytics();
        }

        private void initViews() {
                tvTotalUsers = findViewById(R.id.tvTotalUsers);
                tvActiveUsers = findViewById(R.id.tvActiveUsers);
                tvTotalAppointments = findViewById(R.id.tvTotalAppointments);
                tvAcceptedSessions = findViewById(R.id.tvAcceptedSessions);
                tvCrisisAlerts = findViewById(R.id.tvCrisisAlerts);
                tvPeerPosts = findViewById(R.id.tvPeerPosts);

                tvPeerTotalPosts = findViewById(R.id.tvPeerTotalPosts);
                tvPeerTotalReplies = findViewById(R.id.tvPeerTotalReplies);
                tvMostActiveCategory = findViewById(R.id.tvMostActiveCategory);

                lineChartAppointments = findViewById(R.id.lineChartAppointments);
                barChartCrisis = findViewById(R.id.barChartCrisis);
                spinnerDateFilter = findViewById(R.id.spinnerDateFilter);
        }

        private void setupDateFilter() {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_spinner_item, filterOptions);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDateFilter.setAdapter(adapter);
                spinnerDateFilter.setSelection(1); // Default: Last 30 Days

                spinnerDateFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                currentFilter = filterKeys[position];
                                loadAnalytics();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                });
        }

        private void loadAnalytics() {
                // Remove previous listener
                if (analyticsListener != null) {
                        analyticsListener.remove();
                }

                // Listen to analytics_summary document in real-time
                analyticsListener = db.collection("analytics_summary").document(currentFilter)
                                .addSnapshotListener((snapshot, error) -> {
                                        if (error != null || snapshot == null || !snapshot.exists()) {
                                                setDefaultValues();
                                                return;
                                        }
                                        populateKPIs(snapshot);
                                        populateLineChart(snapshot);
                                        populateBarChart(snapshot);
                                        populatePeerSupport(snapshot);
                                });
        }

        private void setDefaultValues() {
                tvTotalUsers.setText("0");
                tvActiveUsers.setText("0");
                tvTotalAppointments.setText("0");
                tvAcceptedSessions.setText("0");
                tvCrisisAlerts.setText("0");
                tvPeerPosts.setText("0");
                tvPeerTotalPosts.setText("0");
                tvPeerTotalReplies.setText("0");
                tvMostActiveCategory.setText("N/A");
        }

        private void populateKPIs(DocumentSnapshot doc) {
                animateValue(tvTotalUsers, getLongSafe(doc, "totalUsers"));
                animateValue(tvActiveUsers, getLongSafe(doc, "activeUsers"));
                animateValue(tvTotalAppointments, getLongSafe(doc, "totalAppointments"));
                animateValue(tvAcceptedSessions, getLongSafe(doc, "acceptedSessions"));
                animateValue(tvCrisisAlerts, getLongSafe(doc, "crisisCount"));
                animateValue(tvPeerPosts, getLongSafe(doc, "peerPosts"));
        }

        private void animateValue(TextView tv, long value) {
                tv.setText(String.valueOf(value));
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(500);
                tv.startAnimation(fadeIn);
        }

        private long getLongSafe(DocumentSnapshot doc, String field) {
                Long val = doc.getLong(field);
                return val != null ? val : 0;
        }

        private void populateLineChart(DocumentSnapshot doc) {
                int primaryColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary,
                                R.color.md_theme_light_primary);
                int textColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface,
                                R.color.md_theme_light_onSurface);

                List<Entry> entries = new ArrayList<>();

                // Read trend data from Firestore array field
                List<Long> trendData = (List<Long>) doc.get("appointmentTrend");
                if (trendData != null) {
                        for (int i = 0; i < trendData.size(); i++) {
                                entries.add(new Entry(i, trendData.get(i)));
                        }
                } else {
                        // Fallback with empty data
                        for (int i = 0; i < 30; i++) {
                                entries.add(new Entry(i, 0));
                        }
                }

                LineDataSet dataSet = new LineDataSet(entries, "Appointments");
                dataSet.setColor(primaryColor);
                dataSet.setCircleColor(primaryColor);
                dataSet.setLineWidth(2f);
                dataSet.setCircleRadius(3f);
                dataSet.setDrawValues(false);
                dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                dataSet.setDrawFilled(true);
                dataSet.setFillColor(primaryColor);
                dataSet.setFillAlpha(30);

                LineData lineData = new LineData(dataSet);
                lineChartAppointments.setData(lineData);
                lineChartAppointments.getDescription().setEnabled(false);
                lineChartAppointments.getLegend().setTextColor(textColor);
                lineChartAppointments.getXAxis().setTextColor(textColor);
                lineChartAppointments.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                lineChartAppointments.getAxisLeft().setTextColor(textColor);
                lineChartAppointments.getAxisRight().setEnabled(false);
                lineChartAppointments.setTouchEnabled(true);
                lineChartAppointments.setDragEnabled(true);
                lineChartAppointments.animateX(800);
                lineChartAppointments.invalidate();
        }

        private void populateBarChart(DocumentSnapshot doc) {
                int primaryColor = resolveThemeColor(com.google.android.material.R.attr.colorOnPrimary,
                                R.color.md_theme_light_primary);
                int secondaryColor = resolveThemeColor(com.google.android.material.R.attr.colorSecondary,
                                R.color.md_theme_light_secondary);
                int textColor = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface,
                                R.color.md_theme_light_onSurface);

                String[] categories = { "Anxiety", "Depression", "Academic\nStress", "Relationship", "Other" };
                String[] fields = { "crisisAnxiety", "crisisDepression", "crisisAcademic", "crisisRelationship",
                                "crisisOther" };

                List<BarEntry> entries = new ArrayList<>();
                for (int i = 0; i < fields.length; i++) {
                        entries.add(new BarEntry(i, getLongSafe(doc, fields[i])));
                }

                BarDataSet dataSet = new BarDataSet(entries, "Crisis Cases");
                dataSet.setColors(primaryColor, secondaryColor, primaryColor, secondaryColor, primaryColor);
                dataSet.setValueTextColor(textColor);
                dataSet.setValueTextSize(10f);

                BarData barData = new BarData(dataSet);
                barData.setBarWidth(0.6f);

                barChartCrisis.setData(barData);
                barChartCrisis.getDescription().setEnabled(false);
                barChartCrisis.getLegend().setTextColor(textColor);
                barChartCrisis.getXAxis().setValueFormatter(new IndexAxisValueFormatter(categories));
                barChartCrisis.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                barChartCrisis.getXAxis().setGranularity(1f);
                barChartCrisis.getXAxis().setTextColor(textColor);
                barChartCrisis.getAxisLeft().setTextColor(textColor);
                barChartCrisis.getAxisRight().setEnabled(false);
                barChartCrisis.setFitBars(true);
                barChartCrisis.animateY(800);
                barChartCrisis.invalidate();
        }

        private void populatePeerSupport(DocumentSnapshot doc) {
                tvPeerTotalPosts.setText(String.valueOf(getLongSafe(doc, "peerPosts")));
                tvPeerTotalReplies.setText(String.valueOf(getLongSafe(doc, "peerReplies")));

                String activeCategory = doc.getString("mostActiveCategory");
                tvMostActiveCategory.setText(activeCategory != null ? activeCategory : "N/A");
        }

        private int resolveThemeColor(int attr, int defaultColorRes) {
                return MaterialColors.getColor(this, attr, ContextCompat.getColor(this, defaultColorRes));
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

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (analyticsListener != null) {
                        analyticsListener.remove();
                }
        }
}
