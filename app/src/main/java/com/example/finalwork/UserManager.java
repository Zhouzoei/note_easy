package com.example.finalwork;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;

public class UserManager {

    private static final String PREF_NAME = "user_data_encrypted";
    private static final String KEY_USERS = "users";
    private static final String KEY_CURRENT_USER = "current_user";

    // 非敏感数据仍可共用，但统一放入加密存储中更简洁
    private SharedPreferences encryptedPrefs;
    private Context context;

    public UserManager(Context context) {
        this.context = context;
        this.encryptedPrefs = createEncryptedPrefs(context);
        migrateIfNeeded(context);
    }

    // ==================== 加密 SharedPreferences 初始化 ====================
    private static SharedPreferences createEncryptedPrefs(Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            return EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("加密存储初始化失败", e);
        }
    }

    // ==================== 旧数据迁移 ====================
    private void migrateIfNeeded(Context context) {
        SharedPreferences oldPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
        if (oldPrefs.contains(KEY_USERS) || oldPrefs.contains(KEY_CURRENT_USER)) {
            SharedPreferences.Editor editor = encryptedPrefs.edit();

            // 迁移 users set
            Set<String> users = oldPrefs.getStringSet(KEY_USERS, null);
            if (users != null && !users.isEmpty()) {
                editor.putStringSet(KEY_USERS, users);
                // 迁移每个用户的密码和生日
                for (String username : users) {
                    String password = oldPrefs.getString(username + "_password", null);
                    if (password != null) {
                        editor.putString(username + "_password", password);
                    }
                    String birthday = oldPrefs.getString(username + "_birthday", null);
                    if (birthday != null) {
                        editor.putString(username + "_birthday", birthday);
                    }
                    String avatarPath = oldPrefs.getString(username + "_avatar_path", null);
                    if (avatarPath != null) {
                        editor.putString(username + "_avatar_path", avatarPath);
                    }
                }
            }

            // 迁移当前用户
            String currentUser = oldPrefs.getString(KEY_CURRENT_USER, null);
            if (currentUser != null) {
                editor.putString(KEY_CURRENT_USER, currentUser);
            }

            editor.apply();

            // 清除旧数据
            oldPrefs.edit().clear().apply();
        }
    }

    // ==================== 头像管理 ====================
    public String getUserAvatarPath(String username) {
        return encryptedPrefs.getString(username + "_avatar_path", null);
    }

    public void setUserAvatarPath(String username, String avatarPath) {
        encryptedPrefs.edit().putString(username + "_avatar_path", avatarPath).apply();
    }

    public boolean saveUserAvatar(Context context, String username, Uri imageUri) {
        if (imageUri == null) return false;

        try {
            File avatarFile = new File(context.getFilesDir(), "avatars");
            if (!avatarFile.exists()) {
                avatarFile.mkdirs();
            }
            String fileName = "avatar_" + username + ".jpg";
            File destFile = new File(avatarFile, fileName);

            try (InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                 OutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
            }

            setUserAvatarPath(username, destFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== 用户注册 ====================
    public boolean register(String username, String password) {
        if (username == null || username.length() < 3 || username.length() > 20) return false;
        if (password == null || password.length() < 6 || password.length() > 20) return false;

        Set<String> users = getUsers();
        if (users.contains(username)) return false;

        Set<String> newUsers = new HashSet<>(users);
        newUsers.add(username);

        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.putStringSet(KEY_USERS, newUsers);
        editor.putString(username + "_password", password);
        editor.putString(username + "_birthday", "");
        editor.apply();

        return true;
    }

    // ==================== 用户登录 ====================
    public boolean login(String username, String password) {
        Set<String> users = getUsers();
        if (users.contains(username)) {
            String storedPassword = encryptedPrefs.getString(username + "_password", "");
            if (password.equals(storedPassword)) {
                setCurrentUser(username);
                return true;
            }
        }
        return false;
    }

    // ==================== 当前用户管理 ====================
    public String getCurrentUser() {
        return encryptedPrefs.getString(KEY_CURRENT_USER, null);
    }

    public void logout() {
        encryptedPrefs.edit().remove(KEY_CURRENT_USER).apply();
    }

    private void setCurrentUser(String username) {
        encryptedPrefs.edit().putString(KEY_CURRENT_USER, username).apply();
    }

    // ==================== 用户查询 ====================
    private Set<String> getUsers() {
        Set<String> storedUsers = encryptedPrefs.getStringSet(KEY_USERS, new HashSet<>());
        return new HashSet<>(storedUsers);
    }

    public boolean isUsernameExists(String username) {
        return getUsers().contains(username);
    }

    // ==================== 生日管理 ====================
    public String getUserBirthday(String username) {
        return encryptedPrefs.getString(username + "_birthday", "未设置");
    }

    public void setUserBirthday(String username, String birthday) {
        encryptedPrefs.edit().putString(username + "_birthday", birthday).apply();
    }

    // ==================== 密码修改 ====================
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        String storedPassword = encryptedPrefs.getString(username + "_password", "");
        if (storedPassword.equals(oldPassword)) {
            encryptedPrefs.edit().putString(username + "_password", newPassword).apply();
            return true;
        }
        return false;
    }

    // ==================== 用户名修改 ====================
    public boolean changeUsername(String oldUsername, String newUsername, String password) {
        if (isUsernameExists(newUsername)) return false;

        String storedPassword = encryptedPrefs.getString(oldUsername + "_password", "");
        if (!storedPassword.equals(password)) return false;

        String birthday = getUserBirthday(oldUsername);
        String avatarPath = getUserAvatarPath(oldUsername);

        Set<String> users = getUsers();
        Set<String> newUsers = new HashSet<>(users);
        newUsers.remove(oldUsername);
        newUsers.add(newUsername);

        SharedPreferences.Editor editor = encryptedPrefs.edit();
        editor.remove(oldUsername + "_password");
        editor.remove(oldUsername + "_birthday");
        editor.remove(oldUsername + "_avatar_path");
        editor.putStringSet(KEY_USERS, newUsers);
        editor.putString(newUsername + "_password", storedPassword);
        editor.putString(newUsername + "_birthday", birthday);
        if (avatarPath != null) {
            editor.putString(newUsername + "_avatar_path", avatarPath);
        }

        if (getCurrentUser() != null && getCurrentUser().equals(oldUsername)) {
            editor.putString(KEY_CURRENT_USER, newUsername);
        }

        editor.apply();
        return true;
    }
}
