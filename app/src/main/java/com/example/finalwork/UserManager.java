package com.example.finalwork;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class UserManager {
    private static final String PREF_NAME = "user_data";
    private static final String KEY_USERS = "users";
    private static final String KEY_CURRENT_USER = "current_user";

    private SharedPreferences sharedPreferences;

    public UserManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // 注册用户
    public boolean register(String username, String password) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }

        Set<String> users = getUsers();

        if (users.contains(username)) {
            return false;
        }

        users.add(username);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_USERS, users);
        editor.putString(username + "_password", password);
        // 初始化用户信息
        editor.putString(username + "_birthday", "");
        editor.apply();

        return true;
    }

    // 用户登录
    public boolean login(String username, String password) {
        Set<String> users = getUsers();

        if (users.contains(username)) {
            String storedPassword = sharedPreferences.getString(username + "_password", "");
            if (password.equals(storedPassword)) {
                setCurrentUser(username);
                return true;
            }
        }
        return false;
    }

    // 获取当前登录用户
    public String getCurrentUser() {
        return sharedPreferences.getString(KEY_CURRENT_USER, null);
    }

    // 用户登出
    public void logout() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER).apply();
    }

    // 获取所有用户
    private Set<String> getUsers() {
        return sharedPreferences.getStringSet(KEY_USERS, new HashSet<String>());
    }

    // 设置当前用户
    private void setCurrentUser(String username) {
        sharedPreferences.edit().putString(KEY_CURRENT_USER, username).apply();
    }

    // 检查用户名是否存在
    public boolean isUsernameExists(String username) {
        return getUsers().contains(username);
    }

    // 获取用户生日
    public String getUserBirthday(String username) {
        return sharedPreferences.getString(username + "_birthday", "未设置");
    }

    // 设置用户生日
    public void setUserBirthday(String username, String birthday) {
        sharedPreferences.edit().putString(username + "_birthday", birthday).apply();
    }

    // 修改密码
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String storedPassword = sharedPreferences.getString(username + "_password", "");
        if (storedPassword.equals(oldPassword)) {
            sharedPreferences.edit().putString(username + "_password", newPassword).apply();
            return true;
        }
        return false;
    }

    // 修改用户名（需要重新登录）
    public boolean changeUsername(String oldUsername, String newUsername, String password) {
        if (isUsernameExists(newUsername)) {
            return false; // 新用户名已存在
        }

        String storedPassword = sharedPreferences.getString(oldUsername + "_password", "");
        if (!storedPassword.equals(password)) {
            return false; // 密码错误
        }

        // 迁移用户数据
        String birthday = getUserBirthday(oldUsername);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        // 删除旧用户数据
        Set<String> users = getUsers();
        users.remove(oldUsername);
        editor.remove(oldUsername + "_password");
        editor.remove(oldUsername + "_birthday");

        // 添加新用户数据
        users.add(newUsername);
        editor.putStringSet(KEY_USERS, users);
        editor.putString(newUsername + "_password", storedPassword);
        editor.putString(newUsername + "_birthday", birthday);

        // 更新当前登录用户
        if (getCurrentUser().equals(oldUsername)) {
            editor.putString(KEY_CURRENT_USER, newUsername);
        }

        editor.apply();
        return true;
    }
}