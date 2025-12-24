package com.example.finalwork;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.finalwork.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity {

    private TextView tvUsername, tvBirthday;
    private ImageView ivAvatar;
    private Button btnSettings, btnLogout;
    private UserManager userManager;

    // 请求码常量
    private static final int REQUEST_CODE_CAMERA = 1001;
    private static final int REQUEST_CODE_ALBUM = 1002;
    private static final int PERMISSIONS_REQUEST_CODE = 1003;
    private static final int REQUEST_CODE_SETTINGS = 1; // 从设置页面返回的请求码

    private Uri imageUri; // 用于存储拍照后的图片URI

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

        // 修改头像点击事件，弹出选择对话框
        ivAvatar.setOnClickListener(v -> showImagePickerDialog());
    }

    // 新增：显示选择图片的对话框
    private void showImagePickerDialog() {
        String[] options = {"拍照", "从相册选择"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("修改头像");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) { // 拍照
                openCamera();
            } else if (which == 1) { // 从相册选择
                openAlbum();
            }
        });
        builder.show();
    }

    // 新增：检查权限
    private boolean hasPermissions() {
        int cameraPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        // 根据目标API级别选择合适的存储权限
        int storagePermission;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            storagePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            storagePermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        return cameraPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED;
    }

    // 新增：请求权限
    private void requestPermissions() {
        String[] permissions;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
    }

    // 处理权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，可以继续操作
                // 这里可以再次调用 showImagePickerDialog() 或者让用户再点一次
                Toast.makeText(this, "权限已授予，请再次点击头像", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝，给用户一个提示
                Toast.makeText(this, "您拒绝了权限，无法修改头像", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // 新增：打开相机
    private void openCamera() {
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        // 权限都通过，开始拍照
        startCameraIntent();
    }
    private void startCameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                // 使用 FileProvider 获取 URI
                imageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

                // 添加URI权限（重要！）
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA);
            }
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    // 新增：与 FirstActivity 一样的 createImageFile 方法
    private File createImageFile() throws IOException {
        // 创建一个唯一的文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // 将图片存储在应用的外部私有目录
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // 创建临时文件
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    // 新增：打开相册
    private void openAlbum() {
        if (!hasPermissions()) {
            requestPermissions();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_CODE_ALBUM);
    }

    private void loadUserInfo() {
        String currentUser = userManager.getCurrentUser();
        if (currentUser != null) {
            tvUsername.setText(currentUser);
            String birthday = userManager.getUserBirthday(currentUser);
            tvBirthday.setText("未设置".equals(birthday) ? "未设置" : birthday);

            // 加载用户头像
            String avatarPath = userManager.getUserAvatarPath(currentUser);
            if (avatarPath != null && new File(avatarPath).exists()) {
                ivAvatar.setImageBitmap(BitmapFactory.decodeFile(avatarPath));
            } else {
                // 否则显示默认头像
                ivAvatar.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }
    }

    private void navigateToSettings() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SETTINGS);
    }

    private void logout() {
        userManager.logout();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        // 清除任务栈并创建新的任务
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理从设置页面返回的逻辑（保留原有功能）
        if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            loadUserInfo();
        }

        // 处理图片选择结果
        if (resultCode == RESULT_OK) {
            Uri selectedImageUri = null;
            if (requestCode == REQUEST_CODE_CAMERA) {
                selectedImageUri = imageUri; // 拍照的URI
            } else if (requestCode == REQUEST_CODE_ALBUM) {
                if (data != null && data.getData() != null) {
                    selectedImageUri = data.getData(); // 相册选择的URI
                }
            }

            // 如果成功获取了图片URI，则保存并加载
            if (selectedImageUri != null) {
                String currentUser = userManager.getCurrentUser();
                if (currentUser != null) {
                    // 调用UserManager的方法保存图片
                    boolean success = userManager.saveUserAvatar(this, currentUser, selectedImageUri);
                    if (success) {
                        // 保存成功后，重新加载用户信息以更新头像
                        loadUserInfo();
                    } else {
                        Toast.makeText(this, "头像保存失败", Toast.LENGTH_SHORT).show();
                    }
                }
            }
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 整理页面
        navOrganize.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OrganizeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 统计页面
        navStats.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish(); // 结束当前页面
        });

        // 个人页面 - 已经在当前页面，只需更新样式
        navProfile.setOnClickListener(v -> setSelectedTab(navProfile));
    }

    private void setSelectedTab(LinearLayout selectedTab) {
        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));

        // 根据新的XML结构，直接修改ImageView的颜色
        if (selectedTab.getChildCount() > 0) {
            View child = selectedTab.getChildAt(0);
            if (child instanceof ImageView) {
                ImageView imageView = (ImageView) child;
                // 选中状态：深蓝色
                imageView.setColorFilter(Color.parseColor("#2196F3"), android.graphics.PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private void resetTabStyles() {
        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};

        for (int id : navIds) {
            LinearLayout tab = findViewById(id);
            if (tab != null) {
                tab.setBackgroundColor(Color.TRANSPARENT);

                // 重置图标颜色为未选中状态
                if (tab.getChildCount() > 0) {
                    View child = tab.getChildAt(0);
                    if (child instanceof ImageView) {
                        ImageView imageView = (ImageView) child;
                        // 未选中状态：浅蓝色
                        imageView.setColorFilter(Color.parseColor("#90CAF9"), android.graphics.PorterDuff.Mode.SRC_IN);
                    }
                }
            }
        }
    }
}
