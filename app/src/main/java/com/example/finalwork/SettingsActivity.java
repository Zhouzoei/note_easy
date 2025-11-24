package com.example.finalwork;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends Activity {

    private EditText etNewUsername, etOldPassword, etNewPassword;
    private TextView tvBirthdayDisplay, tvError;
    private Button btnSelectBirthday, btnSave, btnBack;
    private UserManager userManager;
    private String currentUser;
    private Calendar selectedCalendar;
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        userManager = new UserManager(this);
        currentUser = userManager.getCurrentUser();
        selectedCalendar = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        initViews();
        setupClickListeners();
        loadCurrentData();
    }

    private void initViews() {
        etNewUsername = findViewById(R.id.etNewUsername);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        tvBirthdayDisplay = findViewById(R.id.tvBirthdayDisplay);
        btnSelectBirthday = findViewById(R.id.btnSelectBirthday);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        tvError = findViewById(R.id.tvError);
    }

    private void setupClickListeners() {
        btnSelectBirthday.setOnClickListener(v -> showDatePickerDialog());
        btnSave.setOnClickListener(v -> saveSettings());
        btnBack.setOnClickListener(v -> goBack());
    }

    private void loadCurrentData() {
        if (currentUser != null) {
            String birthday = userManager.getUserBirthday(currentUser);
            if (!"未设置".equals(birthday)) {
                tvBirthdayDisplay.setText(birthday);
                try {
                    // 解析已设置的生日日期
                    Calendar calendar = Calendar.getInstance();
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    calendar.setTime(parser.parse(birthday));
                    selectedCalendar = calendar;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showDatePickerDialog() {
        // 获取当前日期或已选日期
        int year = selectedCalendar.get(Calendar.YEAR);
        int month = selectedCalendar.get(Calendar.MONTH);
        int day = selectedCalendar.get(Calendar.DAY_OF_MONTH);

        // 创建日期选择对话框
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // 用户选择日期后的回调
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    String selectedDate = dateFormatter.format(selectedCalendar.getTime());
                    tvBirthdayDisplay.setText(selectedDate);
                },
                year, month, day
        );

        // 设置最大日期为今天（不能选择未来的生日）
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // 显示对话框
        datePickerDialog.show();
    }

    private void saveSettings() {
        String newUsername = etNewUsername.getText().toString().trim();
        String oldPassword = etOldPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String birthday = tvBirthdayDisplay.getText().toString().trim();

        boolean hasChanges = false;

        // 检查生日修改
        if (!"未选择".equals(birthday)) {
            userManager.setUserBirthday(currentUser, birthday);
            hasChanges = true;
        }

        // 检查用户名修改
        if (!TextUtils.isEmpty(newUsername)) {
            if (newUsername.length() < 3 || newUsername.length() > 20) {
                showError("用户名长度应为3-20位字符");
                return;
            }
            if (!TextUtils.isEmpty(oldPassword)) {
                if (userManager.changeUsername(currentUser, newUsername, oldPassword)) {
                    currentUser = newUsername; // 更新当前用户名
                    hasChanges = true;
                    showToast("用户名修改成功");
                } else {
                    showError("修改用户名失败，请检查密码或用户名是否已存在");
                    return;
                }
            } else {
                showError("修改用户名需要输入密码");
                return;
            }
        }

        // 检查密码修改
        if (!TextUtils.isEmpty(newPassword)) {
            if (newPassword.length() < 6 || newPassword.length() > 20) {
                showError("密码长度应为6-20位字符");
                return;
            }
            if (!TextUtils.isEmpty(oldPassword)) {
                if (userManager.changePassword(currentUser, oldPassword, newPassword)) {
                    hasChanges = true;
                    showToast("密码修改成功");
                } else {
                    showError("修改密码失败，请检查旧密码");
                    return;
                }
            } else {
                showError("修改密码需要输入旧密码");
                return;
            }
        }

        if (hasChanges) {
            // 设置成功，返回主界面
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            showToast("没有修改任何信息");
        }
    }

    private void goBack() {
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