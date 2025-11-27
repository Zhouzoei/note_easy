package com.example.finalwork;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class OrganizeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize);
        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        LinearLayout navRecord = findViewById(R.id.nav_record);
        LinearLayout navOrganize = findViewById(R.id.nav_organize);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        setSelectedTab(navOrganize);

        navRecord.setOnClickListener(v -> navigateToActivity(FirstActivity.class));
        navOrganize.setOnClickListener(v -> setSelectedTab(navOrganize));
        navStats.setOnClickListener(v -> navigateToActivity(StatisticsActivity.class));
        navProfile.setOnClickListener(v -> navigateToActivity(MainActivity.class));
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }

    private void setSelectedTab(LinearLayout selectedTab) {
        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));

        TextView iconText = (TextView) selectedTab.getChildAt(0);
        TextView labelText = (TextView) selectedTab.getChildAt(1);

        iconText.setTextColor(Color.parseColor("#2196F3"));
        labelText.setTextColor(Color.parseColor("#2196F3"));
    }

    private void resetTabStyles() {
        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};
        for (int id : navIds) {
            LinearLayout tab = findViewById(id);
            if (tab != null) {
                tab.setBackgroundColor(Color.TRANSPARENT);
                TextView iconText = (TextView) tab.getChildAt(0);
                TextView labelText = (TextView) tab.getChildAt(1);
                if (iconText != null) iconText.setTextColor(Color.BLACK);
                if (labelText != null) labelText.setTextColor(Color.BLACK);
            }
        }
    }
}