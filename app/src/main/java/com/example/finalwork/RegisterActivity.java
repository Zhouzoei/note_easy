package com.example.finalwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.finalwork.UserManager;

public class RegisterActivity extends BaseActivity {

    private EditText etUsername, etPassword, etConfirmPassword;
    private Button btnRegister, btnBackToLogin;
    private TextView tvError;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        userManager = new UserManager(this);
        initViews();
        setupClickListeners();
    }

    // 其他方法保持不变...
    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);
        tvError = findViewById(R.id.tvError);
    }

    private void setupClickListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        btnBackToLogin.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showError("请填写所有字段");
            return;
        }

        if (username.length() < 3 || username.length() > 20) {
            showError("用户名长度应为3-20位字符");
            return;
        }

        if (password.length() < 6 || password.length() > 20) {
            showError("密码长度应为6-20位字符");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("两次输入的密码不一致");
            return;
        }

        if (userManager.register(username, password)) {
            userManager.login(username, password);
            showToast("注册成功");
            DiaryManager.getInstance(this).reload();
            navigateToMain();
        } else {
            showError("用户名已存在");
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}