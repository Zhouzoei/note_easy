package com.example.finalwork;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends BaseActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin, btnRegister;
    private TextView tvError;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        // 初始化用户管理器
        userManager = new UserManager(this);

        // 检查是否已登录
        checkAutoLogin();

        // 初始化视图
        initViews();
        setupClickListeners();
    }

    // 其他方法保持不变...
    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        tvError = findViewById(R.id.tvError);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
        btnRegister.setOnClickListener(v -> navigateToRegister());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showError("请输入用户名和密码");
            return;
        }

        if (userManager.login(username, password)) {
            showToast("登录成功");
            navigateToMain();
        } else {
            showError("用户名或密码错误");
        }
    }

    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, FirstActivity.class);
        // 清除任务栈并创建新的任务
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    private void checkAutoLogin() {
        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            navigateToMain();
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(TextView.VISIBLE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}