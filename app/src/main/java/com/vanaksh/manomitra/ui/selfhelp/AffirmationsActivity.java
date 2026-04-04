package com.vanaksh.manomitra.ui.selfhelp;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.vanaksh.manomitra.R;

import java.util.ArrayList;
import java.util.List;

public class AffirmationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_affirmations);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Setup RecyclerView
        RecyclerView recyclerAffirmations = findViewById(R.id.recycler_affirmations);

        // 10 Curated Positive Affirmations
        List<String> affirmations = new ArrayList<>();
        affirmations.add("I am capable of achieving great things.");
        affirmations.add("My mental health is a priority, and I give myself permission to rest and heal.");
        affirmations.add("I release the need to judge myself negatively, and I lean into self-love.");
        affirmations.add("I am surrounded by people who truly support me and love me.");
        affirmations.add("My worth is not defined by my productivity. I am enough just as I am.");
        affirmations.add("Every day brings new opportunities, and I welcome them with an open heart.");
        affirmations.add("I am resilient, strong, and entirely capable of overcoming any challenge I face.");
        affirmations.add("I forgive myself for my past mistakes and I unconditionally accept my flaws.");
        affirmations.add("I choose to focus peacefully on the things I can control and let go of the rest.");
        affirmations.add("I am deserving of happiness, peace, abundance, and inner calm.");

        AffirmationAdapter adapter = new AffirmationAdapter(affirmations);
        recyclerAffirmations.setAdapter(adapter);
    }
}