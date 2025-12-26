package com.example.finalwork;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class UserManager {
    private static final String PREF_NAME = "user_data";
    private static final String KEY_USERS = "users";
    private static final String KEY_CURRENT_USER = "current_user";

    private SharedPreferences sharedPreferences;

    public UserManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    public String getUserAvatarPath(String username) {
        return sharedPreferences.getString(username + "_avatar_path", null);
    }

    // 新增：设置用户头像路径
    public void setUserAvatarPath(String username, String avatarPath) {
        sharedPreferences.edit().putString(username + "_avatar_path", avatarPath).apply();
    }
    public boolean saveUserAvatar(Context context, String username, Uri imageUri) {
        if (imageUri == null) return false;

        try {
            // 1. 创建应用私有目录下的头像文件
            File avatarFile = new File(context.getFilesDir(), "avatars");
            if (!avatarFile.exists()) {
                avatarFile.mkdirs();
            }
            String fileName = "avatar_" + username + ".jpg"; // 以用户名命名，避免冲突
            File destFile = new File(avatarFile, fileName);

            // 2. 复制文件
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            OutputStream outputStream = new FileOutputStream(destFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();

            // 3. 保存新路径到SharedPreferences
            String newPath = destFile.getAbsolutePath();
            setUserAvatarPath(username, newPath);
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 注册用户
    // 注册用户
    public boolean register(String username, String password) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        if (password == null || password.length() < 6 || password.length() > 20) {
            return false;
        }

        // 获取现有用户列表
        Set<String> users = getUsers();

        if (users.contains(username)) {
            return false;
        }

        Set<String> newUsers = new HashSet<>(users);
        newUsers.add(username);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_USERS, newUsers);
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
    // 获取所有用户
    private Set<String> getUsers() {
        Set<String> storedUsers = sharedPreferences.getStringSet(KEY_USERS, new HashSet<String>());
        return new HashSet<>(storedUsers);
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

        // 获取现有用户（副本）
        Set<String> users = getUsers();

        SharedPreferences.Editor editor = sharedPreferences.edit();

        // 删除旧用户数据
        Set<String> newUsers = new HashSet<>(users);
        newUsers.remove(oldUsername);

        editor.remove(oldUsername + "_password");
        editor.remove(oldUsername + "_birthday");
        editor.remove(oldUsername + "_avatar_path"); // 记得也要清理头像路径

        // 添加新用户数据
        newUsers.add(newUsername);
        editor.putStringSet(KEY_USERS, newUsers);
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