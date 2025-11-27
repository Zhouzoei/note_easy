package com.example.finalwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends Activity {

    private TextView tvUsername, tvBirthday;
    private ImageView ivAvatar;
    private Button btnSettings, btnLogout;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userManager = new UserManager(this);
        initViews();
        setupClickListeners();
        loadUserInfo();
        setupBottomNavigation(); // 添加底部导航栏
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvBirthday = findViewById(R.id.tvBirthday);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupClickListeners() {
        btnSettings.setOnClickListener(v -> navigateToSettings());
        btnLogout.setOnClickListener(v -> logout());

        // 头像点击也可以进入设置
        ivAvatar.setOnClickListener(v -> navigateToSettings());
    }

    private void loadUserInfo() {
        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            tvUsername.setText(currentUser);
            String birthday = userManager.getUserBirthday(currentUser);
            // 如果生日是"未设置"，显示为"未设置"，否则显示日期
            tvBirthday.setText("未设置".equals(birthday) ? "未设置" : birthday);
        }
    }

    private void navigateToSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, 1);
    }

    private void logout() {
        userManager.logout();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            // 从设置页面返回，刷新用户信息
            loadUserInfo();
        }
    }

    // 添加底部导航栏功能
    private void setupBottomNavigation() {
        LinearLayout navRecord = findViewById(R.id.nav_record);
        LinearLayout navOrganize = findViewById(R.id.nav_organize);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        // 设置当前页面为选中状态
        setSelectedTab(navProfile);

        // 记录页面
        navRecord.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FirstActivity.class);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 整理页面
        navOrganize.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizeActivity.class);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 统计页面
        navStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 个人页面 - 已经在当前页面，只需更新样式
        navProfile.setOnClickListener(v -> setSelectedTab(navProfile));
    }

    private void setSelectedTab(LinearLayout selectedTab) {
        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));

        // 安全地获取子View
        if (selectedTab.getChildCount() > 0) {
            View firstChild = selectedTab.getChildAt(0);
            if (firstChild instanceof LinearLayout) {
                // 如果是嵌套的LinearLayout
                LinearLayout innerLayout = (LinearLayout) firstChild;
                if (innerLayout.getChildCount() >= 2) {
                    TextView iconText = (TextView) innerLayout.getChildAt(0);
                    TextView labelText = (TextView) innerLayout.getChildAt(1);

                    if (iconText != null) iconText.setTextColor(Color.parseColor("#2196F3"));
                    if (labelText != null) labelText.setTextColor(Color.parseColor("#2196F3"));
                }
            } else {
                // 如果是直接包含两个TextView
                if (selectedTab.getChildCount() >= 2) {
                    TextView iconText = (TextView) selectedTab.getChildAt(0);
                    TextView labelText = (TextView) selectedTab.getChildAt(1);

                    if (iconText != null) iconText.setTextColor(Color.parseColor("#2196F3"));
                    if (labelText != null) labelText.setTextColor(Color.parseColor("#2196F3"));
                }
            }
        }
    }

    private void resetTabStyles() {
        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};

        for (int id : navIds) {
            LinearLayout tab = findViewById(id);
            if (tab != null) {
                tab.setBackgroundColor(Color.TRANSPARENT);

                if (tab.getChildCount() > 0) {
                    View firstChild = tab.getChildAt(0);
                    if (firstChild instanceof LinearLayout) {
                        // 嵌套布局的情况
                        LinearLayout innerLayout = (LinearLayout) firstChild;
                        if (innerLayout.getChildCount() >= 2) {
                            TextView iconText = (TextView) innerLayout.getChildAt(0);
                            TextView labelText = (TextView) innerLayout.getChildAt(1);

                            if (iconText != null) iconText.setTextColor(Color.BLACK);
                            if (labelText != null) labelText.setTextColor(Color.BLACK);
                        }
                    } else {
                        // 直接包含的情况
                        if (tab.getChildCount() >= 2) {
                            TextView iconText = (TextView) tab.getChildAt(0);
                            TextView labelText = (TextView) tab.getChildAt(1);

                            if (iconText != null) iconText.setTextColor(Color.BLACK);
                            if (labelText != null) labelText.setTextColor(Color.BLACK);
                        }
                    }
                }
            }
        }
    }
}