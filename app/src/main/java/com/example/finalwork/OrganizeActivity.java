package com.example.finalwork;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrganizeActivity extends AppCompatActivity {

    private static final String NOTES_FILE = "notes.json";

    private LinearLayout timelineContainer;
    private TextView emptyStateText;
    private List<Note> notesList = new ArrayList<>();
    private List<Note> todayNotesList = new ArrayList<>();
    private Gson gson = new Gson();
    private MediaPlayer mediaPlayer;
    private Button aiProcessBtn;
    private LinearLayout aiResultSection;
    private TextView aiResultText;
    private Button btnSaveResult, btnClearResult;
    private AIProcessor aiProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize);

        initViews();
        setupBottomNavigation();
        setupAIProcessing();
        loadNotesFromFile();
        updateTodayNotes();
        refreshNotesDisplay();
    }
    // 添加初始化方法


    // 添加AI处理设置方法
    private void setupAIProcessing() {
        aiProcessor = new AIProcessor(this);

        aiProcessBtn.setOnClickListener(v -> {
            if (todayNotesList.isEmpty()) {
                Toast.makeText(this, "请先添加一些碎片再使用AI整合", Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示加载状态
            aiResultText.setText("🤖 AI正在整合碎片...");
            aiResultSection.setVisibility(View.VISIBLE);
            aiProcessBtn.setEnabled(false);
            aiProcessBtn.setText("处理中...");

            // 使用模拟处理（测试用）
            aiProcessor.processNotes(todayNotesList, new AIProcessor.AIProcessCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        aiResultText.setText(result);
                        aiProcessBtn.setEnabled(true);
                        aiProcessBtn.setText("🤖 AI整合碎片");
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        aiResultText.setText("❌ " + error);
                        aiProcessBtn.setEnabled(true);
                        aiProcessBtn.setText("🤖 AI整合碎片");
                    });
                }
            });
        });

        // 保存结果按钮
        btnSaveResult.setOnClickListener(v -> {
            String result = aiResultText.getText().toString();
            if (!result.isEmpty() && !result.contains("等待AI处理") && !result.contains("正在整合")) {
                saveAIResultToDiary(result);
            } else {
                Toast.makeText(this, "请先生成有效的结果", Toast.LENGTH_SHORT).show();
            }
        });

        // 清空结果按钮
        btnClearResult.setOnClickListener(v -> {
            aiResultSection.setVisibility(View.GONE);
            aiResultText.setText("等待AI处理...");
        });
    }

    // 保存AI结果到日记
    // 修改 saveAIResultToDiary 方法：
    private void saveAIResultToDiary(String result) {
        // 获取日记管理器
        DiaryManager diaryManager = DiaryManager.getInstance(this);

        // 保存到日记
        diaryManager.saveAIDiary(result);

        // 显示成功消息
        String currentDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        Toast.makeText(this, "已保存为" + currentDate + "的日记", Toast.LENGTH_SHORT).show();
        // 清空结果区域
        aiResultSection.setVisibility(View.GONE);
        aiResultText.setText("等待AI处理...");
        // 可选：跳转到日记查看页面

        // Intent intent = new Intent(this, DiaryListActivity.class);
        // startActivity(intent);
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
        // 释放 MediaPlayer 资源
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void initViews() {
        timelineContainer = findViewById(R.id.timelineContainer);
        emptyStateText = findViewById(R.id.emptyStateText);
        // AI相关视图
        aiProcessBtn = findViewById(R.id.ai_process_btn);
        aiResultSection = findViewById(R.id.ai_result_section);
        aiResultText = findViewById(R.id.ai_result_text);
        btnSaveResult = findViewById(R.id.btn_save_result);
        btnClearResult = findViewById(R.id.btn_clear_result);
    }

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
                View noteView = createNoteView(note, i);
                timelineContainer.addView(noteView);
            }
        }
    }

    private View createNoteView(Note note, int position) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        containerParams.setMargins(0, 0, 0, dpToPx(16));
        container.setLayoutParams(containerParams);

        LinearLayout noteContentLayout = createNoteLayout(note, position);
        container.addView(noteContentLayout);

        return container;
    }

    private LinearLayout createNoteLayout(Note note, int position) {
        LinearLayout noteLayout = new LinearLayout(this);
        noteLayout.setOrientation(LinearLayout.VERTICAL);
        noteLayout.setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12));
        noteLayout.setBackgroundResource(R.drawable.bg_rounded_border);
        noteLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // 顶部行：时间、心情、标签
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

        noteLayout.addView(topRow);

        // 内容
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

        // 显示图片
        if (note.hasPhoto() && note.getImagePath() != null) {
            ImageView photoView = createPhotoView(note);
            LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(120)
            );
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

        // 设置点击播放功能
        voiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playVoiceNote(note.getVoicePath());
            }
        });

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

        return voiceLayout;
    }

    private ImageView createPhotoView(Note note) {
        ImageView photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setBackgroundResource(R.drawable.bg_dashed_border);

        // 加载图片
        Bitmap bitmap = loadImageFromStorage(note.getImagePath());
        if (bitmap != null) {
            photoView.setImageBitmap(bitmap);
            photoView.setTag(bitmap); // 存储bitmap供大图查看使用

            // 设置点击查看大图功能
            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bitmap clickedBitmap = (Bitmap) v.getTag();
                    if (clickedBitmap != null) {
                        showImageDialog(clickedBitmap);
                    }
                }
            });
        } else {
            // 如果图片加载失败，显示占位符
            photoView.setImageResource(R.drawable.ic_photo_placeholder);
            photoView.setScaleType(ImageView.ScaleType.CENTER);
        }

        return photoView;
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

    private void showImageDialog(Bitmap bitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 使用原有的dialog_image_view布局
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_view, null);
        builder.setView(dialogView);

        ImageView dialogImageView = dialogView.findViewById(R.id.dialogImageView);
        Button closeDialogButton = dialogView.findViewById(R.id.closeDialogButton);

        // 移除保存按钮
        Button saveImageButton = dialogView.findViewById(R.id.saveImageButton);
        if (saveImageButton != null) {
            saveImageButton.setVisibility(View.GONE);
        }

        dialogImageView.setImageBitmap(bitmap);

        AlertDialog dialog = builder.create();

        closeDialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void playVoiceNote(String voicePath) {
        if (voicePath == null || voicePath.isEmpty()) return;

        try {
            // 如果正在播放，先停止
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(voicePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 播放完成后的处理
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    mediaPlayer = null;
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
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
            }
        } catch (Exception e) {
            notesList.clear();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void setupBottomNavigation() {
        LinearLayout navRecord = findViewById(R.id.nav_record);
        LinearLayout navOrganize = findViewById(R.id.nav_organize);
        LinearLayout navStats = findViewById(R.id.nav_stats);
        LinearLayout navProfile = findViewById(R.id.nav_profile);

        setSelectedTab(navOrganize);

        navRecord.setOnClickListener(v -> navigateToActivity(FirstActivity.class));
        navOrganize.setOnClickListener(v -> setSelectedTab(navOrganize));
        navStats.setOnClickListener(v -> navigateToActivity(StatisticsActivity.class));
        navProfile.setOnClickListener(v -> navigateToActivity(MainActivity.class));
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(this, targetActivity);
        startActivity(intent);
        finish();
    }

    private void setSelectedTab(LinearLayout selectedTab) {
        resetTabStyles();
        selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));

        TextView iconText = (TextView) selectedTab.getChildAt(0);
        TextView labelText = (TextView) selectedTab.getChildAt(1);

        iconText.setTextColor(Color.parseColor("#2196F3"));
        labelText.setTextColor(Color.parseColor("#2196F3"));
    }

    private void resetTabStyles() {
        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};
        for (int id : navIds) {
            LinearLayout tab = findViewById(id);
            if (tab != null) {
                tab.setBackgroundColor(Color.TRANSPARENT);
                TextView iconText = (TextView) tab.getChildAt(0);
                TextView labelText = (TextView) tab.getChildAt(1);
                if (iconText != null) iconText.setTextColor(Color.BLACK);
                if (labelText != null) labelText.setTextColor(Color.BLACK);
            }
        }
    }
}