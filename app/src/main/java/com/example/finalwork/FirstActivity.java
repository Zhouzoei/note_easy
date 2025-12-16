package com.example.finalwork;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirstActivity extends BaseActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 4;
    private static final int REQUEST_CAMERA_PERMISSION = 5; // 添加相机权限请求码

    private static final String NOTES_FILE = "notes.json";

    private EditText inputArea;
    private Button confirmButton;
    private LinearLayout timelineContainer;
    private LinearLayout imagePreviewLayout;
    private LinearLayout photoLayout, voiceLayout, moodLayout, tagLayout;
    private ImageView imagePreview;
    private Button removeImageButton;
    private TextView moodText, tagText, emptyStateText, todayRecordsTitle;

    // 语音预览相关
    private LinearLayout voicePreviewLayout;
    private TextView voicePreviewIcon, voicePreviewDuration, voicePreviewStatus;
    private Button removeVoiceButton;

    private String selectedMood = "";
    private String selectedTag = "";
    private boolean hasPhoto = false;
    private boolean hasVoice = false;
    private String currentAudioPath = null;
    private long audioDuration = 0;
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    private List<Note> notesList = new ArrayList<>();
    private List<Note> todayNotesList = new ArrayList<>();
    private Gson gson = new Gson();
    private Button statisticsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);

        initViews();
        setupListeners();
        setupBottomNavigation();
        loadNotesFromFile();
        updateTodayNotes();
        refreshNotesDisplay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTodayNotes();
        refreshNotesDisplay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveNotesToFile();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveNotesToFile();
    }

    private void initViews() {
        inputArea = findViewById(R.id.inputArea);
        confirmButton = findViewById(R.id.confirmButton);
        timelineContainer = findViewById(R.id.timelineContainer);
        imagePreviewLayout = findViewById(R.id.imagePreviewLayout);
        imagePreview = findViewById(R.id.imagePreview);
        removeImageButton = findViewById(R.id.removeImageButton);
        photoLayout = findViewById(R.id.photoLayout);
        voiceLayout = findViewById(R.id.voiceLayout);
        moodLayout = findViewById(R.id.moodLayout);
        tagLayout = findViewById(R.id.tagLayout);
        moodText = findViewById(R.id.moodText);
        tagText = findViewById(R.id.tagText);
        emptyStateText = findViewById(R.id.emptyStateText);
        todayRecordsTitle = findViewById(R.id.todayRecordsTitle);

        // 初始化语音预览组件
        voicePreviewLayout = findViewById(R.id.voicePreviewLayout);
        voicePreviewIcon = findViewById(R.id.voicePreviewIcon);
        voicePreviewDuration = findViewById(R.id.voicePreviewDuration);
        voicePreviewStatus = findViewById(R.id.voicePreviewStatus);
        removeVoiceButton = findViewById(R.id.removeVoiceButton);

        updateTitleWithDate();
    }

    private void updateTitleWithDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        todayRecordsTitle.setText("今日记录 - " + currentDate);
    }

    private void setupListeners() {
        inputArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateConfirmButtonVisibility();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewNote();
            }
        });

        photoLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImageSourceDialog();
            }
        });

        removeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSelectedImage();
            }
        });

        moodLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMoodSelection();
            }
        });

        tagLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagSelection();
            }
        });

        voiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showVoiceRecordingDialog();
            }
        });
    }

    private void updateConfirmButtonVisibility() {
        if (inputArea.getText().toString().trim().length() > 0 || hasPhoto || hasVoice) {
            confirmButton.setVisibility(View.VISIBLE);
        } else {
            confirmButton.setVisibility(View.GONE);
        }
    }

    private void showVoiceRecordingDialog() {
        VoiceRecordDialog dialog = new VoiceRecordDialog(this, new VoiceRecordDialog.OnAudioRecordedListener() {
            @Override
            public void onAudioRecorded(String audioPath, long duration) {
                // 处理录音完成
                currentAudioPath = audioPath;
                audioDuration = duration;
                hasVoice = true;
                updateConfirmButtonVisibility();

                // 显示语音预览
                showVoiceAddedToast(duration);
            }

            @Override
            public void onRecordingCancelled() {
                // 录音取消，清空语音数据
                resetVoiceRecording();
            }
        });

        dialog.show();
    }

    private void showVoiceAddedToast(long duration) {
        String durationText = formatDuration(duration);

        // 显示语音预览
        voicePreviewDuration.setText(durationText);
        voicePreviewStatus.setText("已录制");
        voicePreviewLayout.setVisibility(View.VISIBLE);

        // 设置语音预览点击播放
        voicePreviewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviewVoice();
            }
        });

        // 设置删除按钮点击事件
        removeVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeVoiceRecording();
            }
        });

        Toast.makeText(this, "语音已添加 (" + durationText + ")", Toast.LENGTH_SHORT).show();
    }

    // 播放预览中的语音
    private void playPreviewVoice() {
        if (currentAudioPath == null || currentAudioPath.isEmpty()) {
            Toast.makeText(this, "没有可用的语音", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(currentAudioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 更新预览状态
            voicePreviewIcon.setText("⏸️");
            voicePreviewStatus.setText("正在播放...");

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    voicePreviewIcon.setText("🔊");
                    voicePreviewStatus.setText("已录制");
                }
            });

            Toast.makeText(this, "正在播放语音", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeVoiceRecording() {
        // 删除语音文件
        if (currentAudioPath != null) {
            File voiceFile = new File(currentAudioPath);
            if (voiceFile.exists()) {
                voiceFile.delete();
            }
        }

        // 重置语音相关状态
        resetVoiceRecording();
    }


    private String formatDuration(long duration) {
        long seconds = duration / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("00:%02d", seconds);
        }
    }

    private void resetVoiceRecording() {
        hasVoice = false;
        currentAudioPath = null;
        audioDuration = 0;
        voicePreviewLayout.setVisibility(View.GONE); // 隐藏预览
        updateConfirmButtonVisibility();
        Toast.makeText(this, "语音已删除", Toast.LENGTH_SHORT).show();
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择图片来源");

        String[] options = {"拍照", "从相册选择", "取消"};
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        dispatchTakePictureIntent();
                        break;
                    case 1:
                        openGallery();
                        break;
                    case 2:
                        dialog.dismiss();
                        break;
                }
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求相机权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
            return;
        }

        // 检查存储权限（Android 10以下需要）
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // 请求存储权限
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
                return;
            }
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
                Toast.makeText(this, "创建文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                selectedImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);

                // 添加URI权限（重要！）
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "没有可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                loadSelectedImage();
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                selectedImageUri = data.getData();
                loadSelectedImage();
            }
        }
    }

    private void loadSelectedImage() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            selectedImageBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            imagePreview.setImageBitmap(selectedImageBitmap);
            imagePreviewLayout.setVisibility(View.VISIBLE);
            hasPhoto = true;
            updateConfirmButtonVisibility();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void removeSelectedImage() {
        imagePreviewLayout.setVisibility(View.GONE);
        hasPhoto = false;
        selectedImageUri = null;
        selectedImageBitmap = null;
        updateConfirmButtonVisibility();
    }

    private void addNewNote() {
        String content = inputArea.getText().toString().trim();
        if (content.isEmpty() && !hasPhoto && !hasVoice) {
            Toast.makeText(this, "请输入内容、添加图片或录音", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String currentTime = timeFormat.format(new Date());

        // 保存图片到本地
        String imagePath = null;
        if (hasPhoto && selectedImageBitmap != null) {
            imagePath = saveImageToInternalStorage(selectedImageBitmap);
        }

        // 语音文件路径（已保存在指定位置）
        String voicePath = currentAudioPath;

        // 创建笔记对象
        String noteId = String.valueOf(System.currentTimeMillis());
        Note newNote = new Note(noteId, content, currentTime, selectedMood, selectedTag,
                hasPhoto, imagePath, hasVoice, voicePath, audioDuration);
        notesList.add(0, newNote);

        // 立即保存到文件
        saveNotesToFile();

        // 更新当天笔记列表并刷新显示
        updateTodayNotes();
        refreshNotesDisplay();

        resetInputState();
        Toast.makeText(this, "笔记已添加", Toast.LENGTH_SHORT).show();
    }

    // 更新当天笔记列表
    private void updateTodayNotes() {
        todayNotesList.clear();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());

        for (Note note : notesList) {
            if (currentDate.equals(note.getDate())) {
                todayNotesList.add(note);
            }
        }

        Collections.sort(todayNotesList, new Comparator<Note>() {
            @Override
            public int compare(Note n1, Note n2) {
                return Long.compare(n2.getTimestamp(), n1.getTimestamp());
            }
        });
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "note_image_" + timeStamp + ".jpg";
            File file = new File(getFilesDir(), fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap loadImageFromStorage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                return BitmapFactory.decodeStream(new FileInputStream(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void refreshNotesDisplay() {
        timelineContainer.removeAllViews();

        if (todayNotesList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            timelineContainer.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            timelineContainer.setVisibility(View.VISIBLE);

            for (int i = 0; i < todayNotesList.size(); i++) {
                Note note = todayNotesList.get(i);
                View noteView = createNoteViewWithDeleteButton(note, i);
                timelineContainer.addView(noteView);
            }
        }

        showNotesStatistics();
    }

    private void showNotesStatistics() {
        int totalNotes = notesList.size();
        int todayNotes = todayNotesList.size();
    }

    private View createNoteViewWithDeleteButton(Note note, int position) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, dpToPx(16));
        container.setLayoutParams(containerParams);

        LinearLayout noteContentLayout = createNoteLayoutWithDelete(note, position);
        container.addView(noteContentLayout);

        return container;
    }

    private LinearLayout createNoteLayoutWithDelete(Note note, int position) {
        LinearLayout noteLayout = new LinearLayout(this);
        noteLayout.setOrientation(LinearLayout.VERTICAL);
        noteLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        noteLayout.setBackgroundResource(R.drawable.bg_rounded_border);
        noteLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView timeText = new TextView(this);
        timeText.setText(note.getTime());
        timeText.setTextSize(12);
        timeText.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        timeParams.weight = 1;
        timeText.setLayoutParams(timeParams);
        topRow.addView(timeText);

        if (note.getMood() != null && !note.getMood().isEmpty()) {
            TextView moodView = new TextView(this);
            moodView.setText(" " + note.getMood());
            moodView.setTextSize(10);
            moodView.setTextColor(Color.WHITE);
            moodView.setBackgroundColor(Color.parseColor("#FF6B9C"));
            moodView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            moodView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams moodParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            moodParams.setMargins(dpToPx(4), 0, 0, 0);
            moodView.setLayoutParams(moodParams);
            topRow.addView(moodView);
        }

        if (note.getTag() != null && !note.getTag().isEmpty()) {
            TextView tagView = new TextView(this);
            tagView.setText(" " + note.getTag());
            tagView.setTextSize(10);
            tagView.setTextColor(Color.WHITE);
            tagView.setBackgroundColor(Color.parseColor("#4CAF50"));
            tagView.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            tagView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tagParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            tagParams.setMargins(dpToPx(4), 0, 0, 0);
            tagView.setLayoutParams(tagParams);
            topRow.addView(tagView);
        }

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                dpToPx(24),
                dpToPx(24)
        );
        deleteParams.setMargins(dpToPx(8), 0, 0, 0);
        deleteButton.setLayoutParams(deleteParams);

        final int notePosition = findNoteInAllList(note);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation(notePosition);
            }
        });

        topRow.addView(deleteButton);
        noteLayout.addView(topRow);

        if (note.getContent() != null && !note.getContent().isEmpty()) {
            TextView contentText = new TextView(this);
            contentText.setText(note.getContent());
            contentText.setTextSize(14);
            contentText.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            LinearLayout.LayoutParams contentParams = (LinearLayout.LayoutParams) contentText.getLayoutParams();
            contentParams.setMargins(0, dpToPx(8), 0, 0);
            contentText.setLayoutParams(contentParams);
            noteLayout.addView(contentText);
        }

        // 显示语音
        if (note.hasVoice() && note.getVoicePath() != null) {
            LinearLayout voiceLayout = createVoiceView(note);
            LinearLayout.LayoutParams voiceParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            voiceParams.setMargins(0, dpToPx(8), 0, 0);
            voiceLayout.setLayoutParams(voiceParams);
            noteLayout.addView(voiceLayout);
        }

        if (note.hasPhoto() && note.getImagePath() != null) {
            ImageView photoView = new ImageView(this);
            photoView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(120)
            ));
            photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoView.setBackgroundResource(R.drawable.bg_dashed_border);

            Bitmap bitmap = loadImageFromStorage(note.getImagePath());
            if (bitmap != null) {
                photoView.setImageBitmap(bitmap);
                photoView.setTag(bitmap);

                photoView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap clickedBitmap = (Bitmap) v.getTag();
                        if (clickedBitmap != null) {
                            showImageDialog(clickedBitmap);
                        }
                    }
                });
            }

            LinearLayout.LayoutParams photoParams = (LinearLayout.LayoutParams) photoView.getLayoutParams();
            photoParams.setMargins(0, dpToPx(8), 0, 0);
            photoView.setLayoutParams(photoParams);
            noteLayout.addView(photoView);
        }

        return noteLayout;
    }

    private LinearLayout createVoiceView(Note note) {
        LinearLayout voiceLayout = new LinearLayout(this);
        voiceLayout.setOrientation(LinearLayout.HORIZONTAL);
        voiceLayout.setBackgroundResource(R.drawable.bg_voice_border);
        voiceLayout.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        voiceLayout.setGravity(Gravity.CENTER_VERTICAL);

        TextView voiceIcon = new TextView(this);
        voiceIcon.setText("🔊");
        voiceIcon.setTextSize(18);
        voiceIcon.setPadding(0, 0, dpToPx(8), 0);
        voiceLayout.addView(voiceIcon);

        TextView voiceDuration = new TextView(this);
        if (note.getAudioDuration() > 0) {
            voiceDuration.setText(formatDuration(note.getAudioDuration()));
        } else {
            voiceDuration.setText("语音");
        }
        voiceDuration.setTextSize(14);
        voiceDuration.setTextColor(Color.parseColor("#666666"));
        voiceLayout.addView(voiceDuration);

        // 添加点击播放功能
        voiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVoiceNote(note.getVoicePath());
            }
        });

        return voiceLayout;
    }

    private void playVoiceNote(String voicePath) {
        if (voicePath == null || voicePath.isEmpty()) return;

        try {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(voicePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "正在播放语音", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private int findNoteInAllList(Note note) {
        for (int i = 0; i < notesList.size(); i++) {
            if (notesList.get(i).getId().equals(note.getId())) {
                return i;
            }
        }
        return -1;
    }

    private void showDeleteConfirmation(int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这条笔记吗？")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteNote(position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteNote(int position) {
        if (position >= 0 && position < notesList.size()) {
            Note note = notesList.get(position);

            if (note.hasPhoto() && note.getImagePath() != null) {
                File imageFile = new File(note.getImagePath());
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }

            if (note.hasVoice() && note.getVoicePath() != null) {
                File voiceFile = new File(note.getVoicePath());
                if (voiceFile.exists()) {
                    voiceFile.delete();
                }
            }

            notesList.remove(position);
            saveNotesToFile();
            updateTodayNotes();
            refreshNotesDisplay();
            Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageDialog(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_view, null);
        builder.setView(dialogView);

        ImageView dialogImageView = dialogView.findViewById(R.id.dialogImageView);
        Button saveImageButton = dialogView.findViewById(R.id.saveImageButton);
        Button closeDialogButton = dialogView.findViewById(R.id.closeDialogButton);

        dialogImageView.setImageBitmap(bitmap);

        AlertDialog dialog = builder.create();

        saveImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermissionAndSaveImage(bitmap);
                dialog.dismiss();
            }
        });

        closeDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void checkPermissionAndSaveImage(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveImageToGallery(bitmap);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                saveImageToGallery(bitmap);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (selectedImageBitmap != null) {
                    saveImageToGallery(selectedImageBitmap);
                }
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 相机权限被授予，重新尝试拍照
                startCameraIntent();
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Note_Image_" + timeStamp + ".jpg";

            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    fileName,
                    "Saved from Note App"
            );

            if (savedImageURL != null) {
                Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(savedImageURL)));
            } else {
                Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存图片时出错", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNotesToFile() {
        try {
            String json = gson.toJson(notesList);
            FileOutputStream fos = openFileOutput(NOTES_FILE, MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "保存笔记失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotesFromFile() {
        try {
            FileInputStream fis = openFileInput(NOTES_FILE);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            String json = new String(buffer);
            Type listType = new TypeToken<ArrayList<Note>>(){}.getType();
            List<Note> loadedNotes = gson.fromJson(json, listType);

            if (loadedNotes != null) {
                notesList.clear();
                notesList.addAll(loadedNotes);
                //Toast.makeText(this, "加载了 " + notesList.size() + " 条笔记", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            notesList.clear();
        }
    }

    private void resetInputState() {
        inputArea.setText("");
        confirmButton.setVisibility(View.GONE);
        imagePreviewLayout.setVisibility(View.GONE);
        voicePreviewLayout.setVisibility(View.GONE); // 隐藏语音预览
        selectedMood = "";
        selectedTag = "";
        hasPhoto = false;
        hasVoice = false;
        currentAudioPath = null;
        audioDuration = 0;
        selectedImageUri = null;
        selectedImageBitmap = null;
        moodText.setText("心情");
        tagText.setText("标签");
    }

    private void showMoodSelection() {
        final String[] moods = {"开心", "平静", "兴奋", "思考", "疲惫", "其他"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择心情");
        builder.setItems(moods, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedMood = moods[which];
                moodText.setText(selectedMood);
            }
        });
        builder.show();
    }

    private void showTagSelection() {
        final String[] tags = {"工作", "生活", "学习", "娱乐", "健康", "其他"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择标签");
        builder.setItems(tags, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedTag = tags[which];
                tagText.setText(selectedTag);
            }
        });
        builder.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupBottomNavigation() {
        LinearLayout navRecord = findViewById(R.id.nav_record);
        LinearLayout navOrganize = findViewById(R.id.nav_organize);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        setSelectedTab(navRecord);

        navRecord.setOnClickListener(v -> setSelectedTab(navRecord));

        navOrganize.setOnClickListener(v -> {
            Intent organizeIntent = new Intent(FirstActivity.this, OrganizeActivity.class);
            startActivity(organizeIntent);
        });

        navStats.setOnClickListener(v -> {
            Intent statsIntent = new Intent(FirstActivity.this, StatisticsActivity.class);
            startActivity(statsIntent);
        });

        navProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(FirstActivity.this, MainActivity.class);
            startActivity(profileIntent);
        });
    }

    private void setSelectedTab(LinearLayout selectedTab) {
        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));

        if (selectedTab.getChildCount() > 0) {
            View firstChild = selectedTab.getChildAt(0);
            if (firstChild instanceof LinearLayout) {
                LinearLayout innerLayout = (LinearLayout) firstChild;
                if (innerLayout.getChildCount() >= 2) {
                    TextView iconText = (TextView) innerLayout.getChildAt(0);
                    TextView labelText = (TextView) innerLayout.getChildAt(1);

                    iconText.setTextColor(Color.parseColor("#2196F3"));
                    labelText.setTextColor(Color.parseColor("#2196F3"));
                }
            } else {
                if (selectedTab.getChildCount() >= 2) {
                    TextView iconText = (TextView) selectedTab.getChildAt(0);
                    TextView labelText = (TextView) selectedTab.getChildAt(1);

                    iconText.setTextColor(Color.parseColor("#2196F3"));
                    labelText.setTextColor(Color.parseColor("#2196F3"));
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
                        LinearLayout innerLayout = (LinearLayout) firstChild;
                        if (innerLayout.getChildCount() >= 2) {
                            TextView iconText = (TextView) innerLayout.getChildAt(0);
                            TextView labelText = (TextView) innerLayout.getChildAt(1);

                            if (iconText != null) iconText.setTextColor(Color.BLACK);
                            if (labelText != null) labelText.setTextColor(Color.BLACK);
                        }
                    } else {
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