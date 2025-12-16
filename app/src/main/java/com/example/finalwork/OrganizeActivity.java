package com.example.finalwork;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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

public class OrganizeActivity extends BaseActivity {
    private static final String DIARY_CACHE_FILE = "today_diary_cache.json";
    private static final String DIARY_DATE_KEY = "diary_date";


    private static final String NOTES_FILE = "notes.json";

    // Tab相关
    private Button tabFragments, tabDiary;
    private ScrollView fragmentsScroll, diaryScroll;

    // 碎片相关
    private LinearLayout timelineContainer;
    private TextView emptyStateText;
    private List<Note> notesList = new ArrayList<>();
    private List<Note> todayNotesList = new ArrayList<>();

    // AI相关（保持原有）
    private Button aiProcessBtn;
    private LinearLayout aiResultSection;
    private TextView aiResultText;
    private Button btnSaveResult, btnClearResult, btnEditResult;
    private AIProcessor aiProcessor;
    private LinearLayout aiImagesContainer;
    private LinearLayout aiVoicesContainer;

    // 日记编辑相关
    private TextView diaryDate, diaryStatus;
    private LinearLayout diaryEditSection;
    private EditText diaryContentEdit;
    private Button btnCancelEdit, btnSaveEdit;
    private LinearLayout originalContentSection;
    private LinearLayout diaryImagesContainer, diaryVoicesContainer;

    private Gson gson = new Gson();
    private MediaPlayer mediaPlayer;

    // 当前日记状态
    private String currentDiaryContent = "";
    private List<String> currentImagePaths = new ArrayList<>();
    private List<String> currentVoicePaths = new ArrayList<>();

    private DiaryManager diaryManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize);

        initViews();
        setupTabSwitching();
        setupAIProcessing();
        setupDiaryActions();
        setupBottomNavigation();

        loadNotesFromFile();
        updateTodayNotes();
        refreshNotesDisplay();
        updateDiaryDate();

        // 加载今日日记缓存
        loadTodayDiaryCache();
    }
    private void saveTodayDiaryCache() {
        diaryManager.saveTodayDiary(currentDiaryContent, currentImagePaths, currentVoicePaths);
    }

    private void loadTodayDiaryCache() {
        DiaryManager.TodayDiaryCache cache = diaryManager.loadTodayDiary();
        if (cache != null) {
            // 恢复日记内容
            currentDiaryContent = cache.getContent();
            currentImagePaths = cache.getImagePaths();
            currentVoicePaths = cache.getVoicePaths();

            // 显示保存的内容
            displaySavedDiary();
        }
    }
    /**
     * 显示已保存的日记内容
     */
    private void displaySavedDiary() {
        if (currentDiaryContent.isEmpty()) {
            return;
        }

        // 显示AI结果区域
        aiResultSection.setVisibility(View.VISIBLE);

        // 显示保存的文字内容
        String currentDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        String formattedText = "📅 " + currentDate + "\n\n" + currentDiaryContent;
        aiResultText.setText(formattedText);

        // 显示图片
        if (currentImagePaths != null && !currentImagePaths.isEmpty()) {
            displayOriginalImages(currentImagePaths);
        }

        // 显示语音
        if (currentVoicePaths != null && !currentVoicePaths.isEmpty()) {
            displayOriginalVoices(currentVoicePaths);
        }

        // 显示操作按钮
        btnEditResult.setVisibility(View.VISIBLE);
        btnSaveResult.setVisibility(View.VISIBLE);
        btnClearResult.setVisibility(View.VISIBLE);

        // 更新状态
        diaryStatus.setText("已保存");
        diaryStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
        diaryStatus.setTextColor(Color.parseColor("#2196F3"));
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
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void initViews() {
        // Tab相关
        tabFragments = findViewById(R.id.tab_fragments);
        tabDiary = findViewById(R.id.tab_diary);
        fragmentsScroll = findViewById(R.id.fragments_scroll);
        diaryScroll = findViewById(R.id.diary_scroll);

        // 碎片相关
        timelineContainer = findViewById(R.id.timelineContainer);
        emptyStateText = findViewById(R.id.emptyStateText);

        // AI相关（保持原有）
        aiProcessBtn = findViewById(R.id.ai_process_btn);
        aiResultSection = findViewById(R.id.ai_result_section);
        aiResultText = findViewById(R.id.ai_result_text);
        btnSaveResult = findViewById(R.id.btn_save_result);
        btnClearResult = findViewById(R.id.btn_clear_result);
        btnEditResult = findViewById(R.id.btn_edit_result);

        // 日记编辑相关
        diaryDate = findViewById(R.id.diary_date);
        diaryStatus = findViewById(R.id.diary_status);
        diaryEditSection = findViewById(R.id.diary_edit_section);
        diaryContentEdit = findViewById(R.id.diary_content_edit);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnSaveEdit = findViewById(R.id.btn_save_edit);
        originalContentSection = findViewById(R.id.original_content_section);
        diaryImagesContainer = findViewById(R.id.diary_images_container);
        diaryVoicesContainer = findViewById(R.id.diary_voices_container);
        diaryManager = DiaryManager.getInstance(this);

        // 动态添加图片和语音容器（保持原有）
        aiImagesContainer = new LinearLayout(this);
        aiImagesContainer.setOrientation(LinearLayout.VERTICAL);
        aiImagesContainer.setVisibility(View.GONE);

        aiVoicesContainer = new LinearLayout(this);
        aiVoicesContainer.setOrientation(LinearLayout.VERTICAL);
        aiVoicesContainer.setVisibility(View.GONE);

        // 将这些容器添加到结果区域
        LinearLayout resultLayout = findViewById(R.id.result_content_layout);
        if (resultLayout != null) {
            resultLayout.addView(aiImagesContainer);
            resultLayout.addView(aiVoicesContainer);
        }

        aiProcessor = new AIProcessor(this);
    }

    private void setupTabSwitching() {
        tabFragments.setOnClickListener(v -> switchToFragmentsTab());
        tabDiary.setOnClickListener(v -> switchToDiaryTab());
    }

    private void switchToFragmentsTab() {
        // 更新Tab样式
        tabFragments.setTextColor(Color.parseColor("#2196F3"));
        tabFragments.setBackgroundResource(R.drawable.tab_selected_bg);
        tabDiary.setTextColor(Color.parseColor("#666666"));
        tabDiary.setBackgroundResource(R.drawable.tab_normal_bg);

        // 切换内容
        fragmentsScroll.setVisibility(View.VISIBLE);
        diaryScroll.setVisibility(View.GONE);
    }

    private void switchToDiaryTab() {
        // 更新Tab样式
        tabDiary.setTextColor(Color.parseColor("#2196F3"));
        tabDiary.setBackgroundResource(R.drawable.tab_selected_bg);
        tabFragments.setTextColor(Color.parseColor("#666666"));
        tabFragments.setBackgroundResource(R.drawable.tab_normal_bg);

        // 切换内容
        fragmentsScroll.setVisibility(View.GONE);
        diaryScroll.setVisibility(View.VISIBLE);
    }

    private void setupAIProcessing() {
        // 保持原有的AI处理逻辑
        aiProcessBtn.setOnClickListener(v -> {
            if (todayNotesList.isEmpty()) {
                Toast.makeText(this, "请先添加一些碎片再使用AI整合", Toast.LENGTH_SHORT).show();
                return;
            }

            // 清空之前的显示容器
            aiImagesContainer.removeAllViews();
            aiVoicesContainer.removeAllViews();
            aiImagesContainer.setVisibility(View.GONE);
            aiVoicesContainer.setVisibility(View.GONE);

            // 显示加载状态
            aiResultText.setText("🤖 智谱AI正在分析" + todayNotesList.size() + "个碎片...\n请耐心等待");
            aiResultSection.setVisibility(View.VISIBLE);
            aiProcessBtn.setEnabled(false);
            aiProcessBtn.setText("AI处理中...");

            // 调用AI处理
            aiProcessor.processNotes(todayNotesList, new AIProcessor.AIProcessCallback() {
                @Override
                public void onSuccess(String aiText, List<String> imagePaths, List<String> voicePaths) {
                    runOnUiThread(() -> {
                        // 1. 显示AI生成的文字（保持原有格式）
                        displayAIText(aiText);

                        // 2. 显示原图片（保持原有）
                        displayOriginalImages(imagePaths);

                        // 3. 显示原语音（保持原有）
                        displayOriginalVoices(voicePaths);

                        // 4. 恢复按钮状态
                        aiProcessBtn.setEnabled(true);
                        aiProcessBtn.setText("🤖 AI整合碎片");

                        // 5. 显示操作按钮
                        btnEditResult.setVisibility(View.VISIBLE);
                        btnSaveResult.setVisibility(View.VISIBLE);
                        btnClearResult.setVisibility(View.VISIBLE);

                        // 6. 保存当前结果
                        currentDiaryContent = aiText;
                        currentImagePaths = imagePaths;
                        currentVoicePaths = voicePaths;

                        // 7. 更新状态
                        diaryStatus.setText("已生成");
                        diaryStatus.setBackgroundColor(Color.parseColor("#E8F5E8"));
                        diaryStatus.setTextColor(Color.parseColor("#4CAF50"));

                        // 8. 自动切换到日记Tab
                        switchToDiaryTab();

                        Toast.makeText(OrganizeActivity.this,
                                "AI整合完成！已切换到日记页面进行编辑",
                                Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        aiResultText.setText("❌ AI整合失败\n\n错误原因：" + error);
                        aiProcessBtn.setEnabled(true);
                        aiProcessBtn.setText("🤖 重新尝试");
                    });
                }

                @Override
                public void onProgress(String progress) {
                    runOnUiThread(() -> {
                        aiResultText.setText("🤖 " + progress + "\n请稍候...");
                    });
                }
            });
        });

        // 编辑结果按钮
        btnEditResult.setOnClickListener(v -> {
            enterEditMode();
        });

        // 保存结果按钮
        btnSaveResult.setOnClickListener(v -> {
            saveCompleteResult();
        });

        // 清空结果按钮
        btnClearResult.setOnClickListener(v -> {
            clearAIResult();
        });
    }

    private void setupDiaryActions() {
        btnCancelEdit.setOnClickListener(v -> exitEditMode());
        btnSaveEdit.setOnClickListener(v -> saveEditedDiary());
    }

    private void updateDiaryDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        diaryDate.setText(sdf.format(new Date()));
    }

    // 保持原有的displayAIText方法
    private void displayAIText(String aiText) {
        String currentDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        String formattedText = "📅 " + currentDate + "\n\n" + aiText;
        aiResultText.setText(formattedText);
    }

    // 保持原有的displayOriginalImages方法
    private void displayOriginalImages(List<String> imagePaths) {
        if (imagePaths == null || imagePaths.isEmpty()) {
            return;
        }

        TextView imagesTitle = new TextView(this);
        imagesTitle.setText("\n📷 今日图片 (" + imagePaths.size() + "张)");
        imagesTitle.setTextSize(16);
        imagesTitle.setTypeface(imagesTitle.getTypeface(), android.graphics.Typeface.BOLD);
        imagesTitle.setTextColor(Color.parseColor("#333333"));
        imagesTitle.setPadding(0, dpToPx(16), 0, dpToPx(8));
        aiImagesContainer.addView(imagesTitle);

        LinearLayout imagesRow = new LinearLayout(this);
        imagesRow.setOrientation(LinearLayout.HORIZONTAL);
        imagesRow.setPadding(0, 0, 0, dpToPx(16));

        int maxImages = Math.min(imagePaths.size(), 3);
        for (int i = 0; i < maxImages; i++) {
            String imagePath = imagePaths.get(i);
            ImageView imageView = createThumbnailImageView(imagePath);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, dpToPx(80));
            params.weight = 1;
            params.setMargins(i > 0 ? dpToPx(4) : 0, 0, 0, 0);
            imageView.setLayoutParams(params);

            imagesRow.addView(imageView);
        }

        aiImagesContainer.addView(imagesRow);
        aiImagesContainer.setVisibility(View.VISIBLE);

        // 同时在日记Tab中显示
       // displayDiaryImages(imagePaths);
    }

    // 保持原有的displayOriginalVoices方法
    private void displayOriginalVoices(List<String> voicePaths) {
        if (voicePaths == null || voicePaths.isEmpty()) {
            return;
        }

        TextView voicesTitle = new TextView(this);
        voicesTitle.setText("\n🎤 今日语音 (" + voicePaths.size() + "条)");
        voicesTitle.setTextSize(16);
        voicesTitle.setTypeface(voicesTitle.getTypeface(), android.graphics.Typeface.BOLD);
        voicesTitle.setTextColor(Color.parseColor("#333333"));
        voicesTitle.setPadding(0, dpToPx(8), 0, dpToPx(8));
        aiVoicesContainer.addView(voicesTitle);

        for (String voicePath : voicePaths) {
            LinearLayout voiceItem = createVoiceItem(voicePath);
            aiVoicesContainer.addView(voiceItem);
        }

        aiVoicesContainer.setVisibility(View.VISIBLE);

        // 同时在日记Tab中显示
        //displayDiaryVoices(voicePaths);
    }



    // 进入编辑模式
    private void enterEditMode() {
        // 提取纯文本内容（移除日期标题）
        String content = extractContentFromAIText(aiResultText.getText().toString());
        diaryContentEdit.setText(content);

        aiResultSection.setVisibility(View.GONE);
        diaryEditSection.setVisibility(View.VISIBLE);
    }

    // 退出编辑模式
    private void exitEditMode() {
        diaryEditSection.setVisibility(View.GONE);
        aiResultSection.setVisibility(View.VISIBLE);

        // 确保显示最新的内容
        if (!currentDiaryContent.isEmpty()) {
            String displayDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
            String formattedText = "📅 " + displayDate + "\n\n" + currentDiaryContent;
            aiResultText.setText(formattedText);
        }
    }


    // 保存编辑后的日记
    private void saveEditedDiary() {
        String content = diaryContentEdit.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "日记内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        // 更新当前内容
        currentDiaryContent = content;

        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String diaryId = "diary_" + System.currentTimeMillis();
        String title = "日记 - " + currentDate;

        // 创建并保存日记
        Diary diary = new Diary(diaryId, title, content, currentDate);
        diary.setImagePaths(currentImagePaths);
        diary.setVoicePaths(currentVoicePaths);
        diaryManager.addDiary(diary);

        // 保存到今日缓存
        saveTodayDiaryCache();

        // 更新AI结果区域的显示内容
        String displayDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        String formattedText = "📅 " + displayDate + "\n\n" + content;
        aiResultText.setText(formattedText);

        Toast.makeText(this, "✅ 编辑后的日记已保存", Toast.LENGTH_SHORT).show();

        // 更新状态
        diaryStatus.setText("已保存");
        diaryStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
        diaryStatus.setTextColor(Color.parseColor("#2196F3"));

        // 退出编辑模式
        exitEditMode();
    }


    // 保持原有的saveCompleteResult方法
    private void saveCompleteResult() {
        String aiText = aiResultText.getText().toString();

        if (aiText == null || aiText.isEmpty() || aiText.contains("正在分析") || aiText.contains("AI整合失败")) {
            Toast.makeText(this, "请先生成有效的AI结果", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = extractContentFromAIText(aiText);
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        // 使用DiaryManager保存AI日记
        diaryManager.saveAIDiary(content, currentImagePaths, currentVoicePaths);

        // 保存到今日缓存
        currentDiaryContent = content;
        saveTodayDiaryCache();

        String displayDate = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
        Toast.makeText(this, "✅ 已保存为" + displayDate + "的日记", Toast.LENGTH_LONG).show();

        diaryStatus.setText("已保存");
        diaryStatus.setBackgroundColor(Color.parseColor("#E3F2FD"));
        diaryStatus.setTextColor(Color.parseColor("#2196F3"));
    }

    // 保持原有的extractContentFromAIText方法
    private String extractContentFromAIText(String aiText) {
        String[] lines = aiText.split("\n");
        StringBuilder content = new StringBuilder();

        for (String line : lines) {
            if (!line.startsWith("📅") && !line.trim().isEmpty()) {
                content.append(line).append("\n");
            }
        }

        return content.toString().trim();
    }

    // 保持原有的clearAIResult方法
    private void clearAIResult() {
        aiResultSection.setVisibility(View.GONE);
        aiResultText.setText("等待AI处理...");
        aiImagesContainer.removeAllViews();
        aiVoicesContainer.removeAllViews();
        aiImagesContainer.setVisibility(View.GONE);
        aiVoicesContainer.setVisibility(View.GONE);

        diaryEditSection.setVisibility(View.GONE);
        originalContentSection.setVisibility(View.GONE);

        currentDiaryContent = "";
        currentImagePaths.clear();
        currentVoicePaths.clear();

        diaryStatus.setText("未生成");
        diaryStatus.setBackgroundColor(Color.parseColor("#F0F0F0"));
        diaryStatus.setTextColor(Color.parseColor("#999999"));
    }

    // 保持原有的其他方法...
    private ImageView createThumbnailImageView(String imagePath) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_rounded_border);

        Bitmap bitmap = loadImageFromStorage(imagePath);
        if (bitmap != null) {
            int thumbnailSize = dpToPx(80);
            Bitmap thumbnail = Bitmap.createScaledBitmap(bitmap, thumbnailSize, thumbnailSize, true);
            imageView.setImageBitmap(thumbnail);
            imageView.setTag(bitmap);

            imageView.setOnClickListener(v -> {
                Bitmap originalBitmap = (Bitmap) v.getTag();
                if (originalBitmap != null) {
                    showImageDialog(originalBitmap);
                }
            });
        } else {
            imageView.setImageResource(R.drawable.ic_photo_placeholder);
        }

        return imageView;
    }

    private LinearLayout createVoiceItem(String voicePath) {
        LinearLayout voiceLayout = new LinearLayout(this);
        voiceLayout.setOrientation(LinearLayout.HORIZONTAL);
        voiceLayout.setBackgroundResource(R.drawable.bg_voice_border);
        voiceLayout.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        voiceLayout.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        voiceLayout.setLayoutParams(params);

        TextView voiceIcon = new TextView(this);
        voiceIcon.setText("🔊");
        voiceIcon.setTextSize(18);
        voiceIcon.setPadding(0, 0, dpToPx(8), 0);
        voiceLayout.addView(voiceIcon);

        TextView voiceInfo = new TextView(this);
        voiceInfo.setText("语音记录");
        voiceInfo.setTextSize(14);
        voiceInfo.setTextColor(Color.parseColor("#666666"));
        voiceLayout.addView(voiceInfo);

        TextView playButton = new TextView(this);
        playButton.setText(" ▶ 播放");
        playButton.setTextSize(12);
        playButton.setTextColor(Color.parseColor("#2196F3"));
        playButton.setPadding(dpToPx(12), 0, 0, 0);

        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        playParams.gravity = Gravity.END;
        playParams.weight = 1;
        playButton.setLayoutParams(playParams);

        playButton.setOnClickListener(v -> playVoice(voicePath));
        voiceLayout.addView(playButton);

        return voiceLayout;
    }

    private void playVoice(String voicePath) {
        if (voicePath == null || voicePath.isEmpty()) return;

        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(voicePath);
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "正在播放语音", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
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

        voiceLayout.setOnClickListener(v -> playVoiceNote(note.getVoicePath()));

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

    private void playVoiceNote(String voicePath) {
        if (voicePath == null || voicePath.isEmpty()) return;

        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(voicePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(this, "正在播放语音", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                Toast.makeText(OrganizeActivity.this, "播放完成", Toast.LENGTH_SHORT).show();
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
        }
    }

    private ImageView createPhotoView(Note note) {
        ImageView photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setBackgroundResource(R.drawable.bg_dashed_border);

        Bitmap bitmap = loadImageFromStorage(note.getImagePath());
        if (bitmap != null) {
            photoView.setImageBitmap(bitmap);
            photoView.setTag(bitmap);

            photoView.setOnClickListener(v -> {
                Bitmap clickedBitmap = (Bitmap) v.getTag();
                if (clickedBitmap != null) {
                    showImageDialog(clickedBitmap);
                }
            });
        } else {
            photoView.setImageResource(R.drawable.ic_photo_placeholder);
            photoView.setScaleType(ImageView.ScaleType.CENTER);
        }

        return photoView;
    }

    private void showImageDialog(Bitmap bitmap) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_image_view, null);
        builder.setView(dialogView);

        ImageView dialogImageView = dialogView.findViewById(R.id.dialogImageView);
        Button closeDialogButton = dialogView.findViewById(R.id.closeDialogButton);

        Button saveImageButton = dialogView.findViewById(R.id.saveImageButton);
        if (saveImageButton != null) {
            saveImageButton.setVisibility(View.GONE);
        }

        dialogImageView.setImageBitmap(bitmap);

        android.app.AlertDialog dialog = builder.create();

        closeDialogButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
