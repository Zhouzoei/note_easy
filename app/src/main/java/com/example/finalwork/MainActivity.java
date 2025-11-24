package com.example.finalwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
}