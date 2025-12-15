package com.example.finalwork;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private static final String NOTES_FILE = "notes.json";

    private TextView tvMonthlyRecords, tvTotalPhotos, tvPrimaryMood, tvCommonTags, tvHeatmapTitle;
    private LinearLayout activityChartContainer, chartLabelsContainer, moodHeatmapContainer;

    // 四个模块的布局引用
    private LinearLayout monthlyRecordsLayout, totalPhotosLayout, primaryMoodLayout, commonTagsLayout;

    private final List<Note> notesList = new ArrayList<>();
    private NoteStatistics statistics;
    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        initViews();
        loadNotes();
        setupBottomNavigation();
        calculateAndDisplayStatistics();

        // 设置四个模块的点击事件
        setupClickListeners();
    }

    private void initViews() {
        tvMonthlyRecords = findViewById(R.id.tvMonthlyRecords);
        tvTotalPhotos = findViewById(R.id.tvTotalPhotos);
        tvPrimaryMood = findViewById(R.id.tvPrimaryMood);
        tvCommonTags = findViewById(R.id.tvCommonTags);
        tvHeatmapTitle = findViewById(R.id.tvHeatmapTitle);
        activityChartContainer = findViewById(R.id.activityChartContainer);
        chartLabelsContainer = findViewById(R.id.chartLabelsContainer);
        moodHeatmapContainer = findViewById(R.id.moodHeatmapContainer);

        // 初始化四个模块的布局
        monthlyRecordsLayout = findViewById(R.id.monthlyRecordsLayout);
        totalPhotosLayout = findViewById(R.id.totalPhotosLayout);
        primaryMoodLayout = findViewById(R.id.primaryMoodLayout);
        commonTagsLayout = findViewById(R.id.commonTagsLayout);
    }

    private void setupClickListeners() {
        // 1. 月度记录 - 点击整个蓝色卡片
        LinearLayout monthlyCard = findViewById(R.id.monthlyRecordsLayout);
        if (monthlyCard != null) {
            monthlyCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMonthlyRecordsDialog();
                }
            });
            monthlyCard.setClickable(true);
        }

        // 2. 照片总数 - 点击整个绿色卡片
        LinearLayout photosCard = findViewById(R.id.totalPhotosLayout);
        if (photosCard != null) {
            photosCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTotalPhotosDialog();
                }
            });
            photosCard.setClickable(true);
        }

        // 3. 主要心情 - 点击整个橙色卡片
        LinearLayout moodCard = findViewById(R.id.primaryMoodLayout);
        if (moodCard != null) {
            moodCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPrimaryMoodDialog();
                }
            });
            moodCard.setClickable(true);
        }

        // 4. 常用标签 - 点击整个紫色卡片
        LinearLayout tagsCard = findViewById(R.id.commonTagsLayout);
        if (tagsCard != null) {
            tagsCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCommonTagsDialog();
                }
            });
            tagsCard.setClickable(true);
        }

        // 5. 同时给数字也添加点击事件（双保险）
        tvMonthlyRecords.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMonthlyRecordsDialog();
            }
        });

        tvTotalPhotos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTotalPhotosDialog();
            }
        });

        tvPrimaryMood.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrimaryMoodDialog();
            }
        });

        tvCommonTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCommonTagsDialog();
            }
        });
    }

    // 显示月度记录弹窗（带日期选择）
    private void showMonthlyRecordsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_monthly_records, null);

        // 初始化当前日期为今天
        final Calendar currentDate = Calendar.getInstance();
        final SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // 获取控件
        final TextView tvCurrentDate = dialogView.findViewById(R.id.tvCurrentDate);
        final TextView tvDayRecordsCount = dialogView.findViewById(R.id.tvDayRecordsCount);
        final TextView tvDayVoiceCount = dialogView.findViewById(R.id.tvDayVoiceCount);
        Button btnPrevDay = dialogView.findViewById(R.id.btnPrevDay);
        Button btnNextDay = dialogView.findViewById(R.id.btnNextDay);
        Button btnViewMonth = dialogView.findViewById(R.id.btnViewMonth);
        Button btnClose = dialogView.findViewById(R.id.btnClose);
        final LinearLayout recordsListContainer = dialogView.findViewById(R.id.recordsListContainer);
        final TextView tvNoRecords = dialogView.findViewById(R.id.tvNoRecords);

        // 更新日期显示和日记列表
        final Runnable updateDateDisplay = new Runnable() {
            @Override
            public void run() {
                // 更新日期显示
                tvCurrentDate.setText(displayFormat.format(currentDate.getTime()));

                // 获取当天日期字符串
                String currentDateStr = dataFormat.format(currentDate.getTime());

                // 统计当天的日记
                List<Note> dayNotes = new ArrayList<>();
                int voiceCount = 0;

                for (Note note : notesList) {
                    if (note.getDate().equals(currentDateStr)) {
                        dayNotes.add(note);
                        if (note.hasVoice()) {
                            voiceCount++;
                        }
                    }
                }

                // 更新统计数字
                tvDayRecordsCount.setText(String.valueOf(dayNotes.size()));
                tvDayVoiceCount.setText(String.valueOf(voiceCount));

                // 清空并重新添加日记列表
                recordsListContainer.removeAllViews();

                if (dayNotes.isEmpty()) {
                    tvNoRecords.setVisibility(View.VISIBLE);
                    recordsListContainer.setVisibility(View.GONE);
                } else {
                    tvNoRecords.setVisibility(View.GONE);
                    recordsListContainer.setVisibility(View.VISIBLE);

                    // 按时间倒序排列（最新的在前）
                    dayNotes.sort((n1, n2) -> n2.getTime().compareTo(n1.getTime()));

                    // 添加日记项
                    for (Note note : dayNotes) {
                        addNoteItemWithMood(recordsListContainer, note);
                    }
                }
            }
        };

        // 初始显示今天的数据
        updateDateDisplay.run();

        // 前一天按钮
        btnPrevDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDate.add(Calendar.DAY_OF_MONTH, -1);
                updateDateDisplay.run();
            }
        });

        // 后一天按钮
        btnNextDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentDate.add(Calendar.DAY_OF_MONTH, 1);
                updateDateDisplay.run();
            }
        });

        // 查看本月按钮
        btnViewMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMonthSummaryDialog(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH) + 1);
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // 显示本月汇总弹窗
    private void showMonthSummaryDialog(int year, int month) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_monthly_records, null);

        // 修改标题
        TextView title = dialogView.findViewById(R.id.tvCurrentDate);
        title.setText(year + "年" + month + "月汇总");

        // 隐藏日期选择按钮
        dialogView.findViewById(R.id.btnPrevDay).setVisibility(View.GONE);
        dialogView.findViewById(R.id.btnNextDay).setVisibility(View.GONE);
        dialogView.findViewById(R.id.tvCurrentDate).setVisibility(View.GONE);

        // 获取本月所有日记
        List<Note> monthlyNotes = new ArrayList<>();
        int voiceCount = 0;
        int photoCount = 0;

        for (Note note : notesList) {
            if (isNoteInMonth(note, year, month)) {
                monthlyNotes.add(note);
                if (note.hasVoice()) {
                    voiceCount++;
                }
                if (note.hasPhoto()) {
                    photoCount++;
                }
            }
        }

        // 设置数据
        TextView tvDayRecordsCount = dialogView.findViewById(R.id.tvDayRecordsCount);
        TextView tvDayVoiceCount = dialogView.findViewById(R.id.tvDayVoiceCount);
        LinearLayout recordsListContainer = dialogView.findViewById(R.id.recordsListContainer);
        TextView tvNoRecords = dialogView.findViewById(R.id.tvNoRecords);

        tvDayRecordsCount.setText(String.valueOf(monthlyNotes.size()));
        tvDayVoiceCount.setText(String.valueOf(voiceCount));

        if (monthlyNotes.isEmpty()) {
            tvNoRecords.setVisibility(View.VISIBLE);
            recordsListContainer.setVisibility(View.GONE);
        } else {
            tvNoRecords.setVisibility(View.GONE);
            recordsListContainer.setVisibility(View.VISIBLE);
            recordsListContainer.removeAllViews();

            // 按日期倒序排列
            monthlyNotes.sort((n1, n2) -> {
                // 先按日期倒序，再按时间倒序
                int dateCompare = n2.getDate().compareTo(n1.getDate());
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return n2.getTime().compareTo(n1.getTime());
            });

            // 添加日记项
            for (Note note : monthlyNotes) {
                addNoteItemWithMood(recordsListContainer, note);
            }
        }

        Button btnViewMonth = dialogView.findViewById(R.id.btnViewMonth);
        btnViewMonth.setText("返回每日视图");

        Button btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnViewMonth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showMonthlyRecordsDialog();
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // 添加带有心情显示的日记项
    private void addNoteItemWithMood(LinearLayout container, Note note) {
        LinearLayout noteItem = new LinearLayout(this);
        noteItem.setOrientation(LinearLayout.VERTICAL);
        noteItem.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        noteItem.setBackgroundResource(R.drawable.bg_note_item);

        // 第一行：时间、心情、标签
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // 时间
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

        // 心情（如果有）
        if (note.getMood() != null && !note.getMood().isEmpty()) {
            TextView moodView = new TextView(this);
            moodView.setText(" " + note.getMood() + " ");
            moodView.setTextSize(10);
            moodView.setTextColor(Color.WHITE);
            moodView.setBackgroundColor(Color.parseColor("#FF6B9C"));
            moodView.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            moodView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams moodParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            moodParams.setMargins(dpToPx(4), 0, 0, 0);
            moodView.setLayoutParams(moodParams);
            topRow.addView(moodView);
        }

        // 标签（如果有）
        if (note.getTag() != null && !note.getTag().isEmpty()) {
            TextView tagView = new TextView(this);
            tagView.setText(" " + note.getTag() + " ");
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

        noteItem.addView(topRow);

        // 内容
        if (note.getContent() != null && !note.getContent().isEmpty()) {
            TextView contentText = new TextView(this);
            contentText.setText(note.getContent());
            contentText.setTextSize(14);
            contentText.setTextColor(Color.parseColor("#333333"));
            LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            contentParams.setMargins(0, dpToPx(8), 0, 0);
            contentText.setLayoutParams(contentParams);
            noteItem.addView(contentText);
        }

        // 第二行：图标指示器（照片、语音）
        if (note.hasPhoto() || note.hasVoice()) {
            LinearLayout iconRow = new LinearLayout(this);
            iconRow.setOrientation(LinearLayout.HORIZONTAL);
            iconRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            iconRow.setPadding(0, dpToPx(4), 0, 0);

            // 日期（如果是在月视图中显示）
            TextView dateText = new TextView(this);
            dateText.setText(note.getDate());
            dateText.setTextSize(10);
            dateText.setTextColor(Color.parseColor("#999999"));
            dateText.setPadding(0, 0, dpToPx(8), 0);
            iconRow.addView(dateText);

            // 照片图标
            if (note.hasPhoto()) {
                TextView photoIcon = new TextView(this);
                photoIcon.setText("📷");
                photoIcon.setTextSize(12);
                photoIcon.setPadding(dpToPx(4), 0, dpToPx(8), 0);
                iconRow.addView(photoIcon);
            }

            // 语音图标
            if (note.hasVoice()) {
                TextView voiceIcon = new TextView(this);
                voiceIcon.setText("🎤");
                voiceIcon.setTextSize(12);
                voiceIcon.setPadding(dpToPx(4), 0, 0, 0);
                iconRow.addView(voiceIcon);
            }

            noteItem.addView(iconRow);
        }

        // 添加间距
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(8));
        container.addView(noteItem, itemParams);
    }

    // 根据心情文字获取颜色
    private int getMoodColorByText(String mood) {
        switch (mood) {
            case "开心":
            case "兴奋":
                return Color.parseColor("#4CAF50"); // 绿色
            case "平静":
            case "思考":
                return Color.parseColor("#2196F3"); // 蓝色
            case "疲惫":
            case "其他":
                return Color.parseColor("#9E9E9E"); // 灰色
            default:
                return Color.parseColor("#FF9800"); // 橙色
        }
    }

    // 显示照片总数弹窗
    private void showTotalPhotosDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_monthly_photos, null);

        // 获取本月照片数据
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        int photoCount = 0;
        int notesWithPhotos = 0;
        List<String> photoPaths = new ArrayList<>();

        for (Note note : notesList) {
            if (isNoteInMonth(note, currentYear, currentMonth)) {
                if (note.hasPhoto() && note.getImagePath() != null) {
                    photoCount++;
                    notesWithPhotos++;
                    photoPaths.add(note.getImagePath());
                }
            }
        }

        // 设置数据
        TextView tvTotalPhotosCount = dialogView.findViewById(R.id.tvTotalPhotosCount);
        TextView tvNotesWithPhotos = dialogView.findViewById(R.id.tvNotesWithPhotos);
        LinearLayout photosGridContainer = dialogView.findViewById(R.id.photosGridContainer);
        TextView tvNoPhotos = dialogView.findViewById(R.id.tvNoPhotos);

        tvTotalPhotosCount.setText(String.valueOf(photoCount));
        tvNotesWithPhotos.setText(String.valueOf(notesWithPhotos));

        if (photoCount == 0) {
            tvNoPhotos.setVisibility(View.VISIBLE);
            photosGridContainer.setVisibility(View.GONE);
        } else {
            tvNoPhotos.setVisibility(View.GONE);
            photosGridContainer.setVisibility(View.VISIBLE);
            photosGridContainer.removeAllViews();

            // 添加照片预览（每行3个，最多显示9张）
            int maxPhotosToShow = Math.min(photoCount, 9);
            LinearLayout currentRow = null;

            for (int i = 0; i < maxPhotosToShow; i++) {
                if (i % 3 == 0) {
                    // 创建新行
                    currentRow = new LinearLayout(this);
                    currentRow.setOrientation(LinearLayout.HORIZONTAL);
                    currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    photosGridContainer.addView(currentRow);
                }

                if (currentRow != null) {
                    addPhotoItem(currentRow, photoPaths.get(i));
                }
            }
        }

        Button btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // 显示主要心情弹窗
    private void showPrimaryMoodDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_primary_mood, null);

        // 获取本月心情数据
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        Map<String, Integer> moodCount = new HashMap<>();
        List<Note> monthlyNotes = new ArrayList<>();

        for (Note note : notesList) {
            if (isNoteInMonth(note, currentYear, currentMonth)) {
                monthlyNotes.add(note);
                if (note.getMood() != null && !note.getMood().isEmpty()) {
                    moodCount.put(note.getMood(), moodCount.getOrDefault(note.getMood(), 0) + 1);
                }
            }
        }

        // 设置主要心情
        TextView tvMoodEmoji = dialogView.findViewById(R.id.tvMoodEmoji);
        TextView tvMoodName = dialogView.findViewById(R.id.tvMoodName);
        TextView tvMoodDetail = dialogView.findViewById(R.id.tvMoodDetail);
        ProgressBar progressBarMood = dialogView.findViewById(R.id.progressBarMood);
        LinearLayout moodDistributionContainer = dialogView.findViewById(R.id.moodDistributionContainer);
        TextView tvNoMoodData = dialogView.findViewById(R.id.tvNoMoodData);

        if (moodCount.isEmpty()) {
            tvMoodName.setText("无记录");
            tvMoodDetail.setText("本月暂无心情记录");
            tvMoodEmoji.setText("😐");
            progressBarMood.setProgress(0);
            tvNoMoodData.setVisibility(View.VISIBLE);
            moodDistributionContainer.setVisibility(View.GONE);
        } else {
            tvNoMoodData.setVisibility(View.GONE);
            moodDistributionContainer.setVisibility(View.VISIBLE);
            moodDistributionContainer.removeAllViews();

            // 找到主要心情
            String primaryMood = "";
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    primaryMood = entry.getKey();
                }
            }

            // 计算百分比
            int percent = monthlyNotes.size() > 0 ? (maxCount * 100 / monthlyNotes.size()) : 0;

            // 设置主要心情显示
            tvMoodName.setText(primaryMood);
            tvMoodDetail.setText(String.format("出现%d次，占比%d%%", maxCount, percent));
            tvMoodEmoji.setText(getMoodEmoji(primaryMood));
            progressBarMood.setProgress(percent);
            progressBarMood.setProgressTintList(android.content.res.ColorStateList.valueOf(getMoodColorValue(primaryMood)));

            // 添加心情分布
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                addMoodDistributionItem(moodDistributionContainer,
                        entry.getKey(), entry.getValue(), monthlyNotes.size());
            }
        }

        Button btnClose = dialogView.findViewById(R.id.btnClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    // 显示常用标签弹窗
    private void showCommonTagsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_common_tags, null);

        // 获取本月标签数据
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        final Map<String, Integer> tagCount = new HashMap<>();

        for (Note note : notesList) {
            if (isNoteInMonth(note, currentYear, currentMonth)) {
                if (note.getTag() != null && !note.getTag().isEmpty()) {
                    tagCount.put(note.getTag(), tagCount.getOrDefault(note.getTag(), 0) + 1);
                }
            }
        }

        // 设置数据
        TextView tvCommonTagsCount = dialogView.findViewById(R.id.tvCommonTagsCount);
        TextView tvMostUsedTag = dialogView.findViewById(R.id.tvMostUsedTag);
        WordCloudView wordCloudView = dialogView.findViewById(R.id.wordCloudView);
        TextView tvNoTags = dialogView.findViewById(R.id.tvNoTags);
        Button btnRefreshCloud = dialogView.findViewById(R.id.btnRefreshCloud);

        int commonCount = 0;
        String mostUsedTag = "无";
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : tagCount.entrySet()) {
            if (entry.getValue() > 1) {
                commonCount++;
            }
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostUsedTag = entry.getKey();
            }
        }

        tvCommonTagsCount.setText(String.valueOf(commonCount));
        tvMostUsedTag.setText(mostUsedTag);

        if (tagCount.isEmpty()) {
            tvNoTags.setVisibility(View.VISIBLE);
            wordCloudView.setVisibility(View.GONE);
            btnRefreshCloud.setVisibility(View.GONE);
        } else {
            tvNoTags.setVisibility(View.GONE);
            wordCloudView.setVisibility(View.VISIBLE);
            btnRefreshCloud.setVisibility(View.VISIBLE);

            // 设置词云数据
            wordCloudView.setWords(tagCount);

            // 刷新按钮点击事件
            btnRefreshCloud.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    wordCloudView.startNewAnimation();
                }
            });
        }

        Button btnClose = dialogView.findViewById(R.id.btnClose);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        // 在showCommonTagsDialog()方法中，修改弹窗关闭时的处理
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                // 停止动画以节省资源
                if (wordCloudView != null) {
                    wordCloudView.stopAnimation();
                }
            }
        });


        // 监听弹窗显示/隐藏，重新开始动画
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (!tagCount.isEmpty() && wordCloudView != null) {
                    wordCloudView.startNewAnimation();
                }
            }
        });

        dialog.show();
    }

    // =============== 辅助方法 ===============

    // 判断笔记是否在本月
    private boolean isNoteInMonth(Note note, int year, int month) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date noteDate = sdf.parse(note.getDate());
            Calendar cal = Calendar.getInstance();
            cal.setTime(noteDate);

            int noteYear = cal.get(Calendar.YEAR);
            int noteMonth = cal.get(Calendar.MONTH) + 1;

            return noteYear == year && noteMonth == month;
        } catch (Exception e) {
            return false;
        }
    }

    // 添加照片项到容器
    private void addPhotoItem(LinearLayout rowContainer, String imagePath) {
        ImageView imageView = new ImageView(this);
        int size = dpToPx(80);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        params.weight = 1;
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundResource(R.drawable.bg_photo_item);

        // 加载图片
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                if (bitmap != null) {
                    // 缩放图片以适应ImageView
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
                    imageView.setImageBitmap(scaledBitmap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        rowContainer.addView(imageView);
    }

    // 添加心情分布项
    private void addMoodDistributionItem(LinearLayout container, String mood, int count, int totalNotes) {
        LinearLayout moodItem = new LinearLayout(this);
        moodItem.setOrientation(LinearLayout.VERTICAL);
        moodItem.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
        moodItem.setBackgroundResource(R.drawable.bg_note_item);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView emojiText = new TextView(this);
        emojiText.setText(getMoodEmoji(mood));
        emojiText.setTextSize(20);
        emojiText.setPadding(0, 0, dpToPx(12), 0);
        topRow.addView(emojiText);

        TextView moodText = new TextView(this);
        moodText.setText(mood);
        moodText.setTextSize(14);
        moodText.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams moodParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        moodParams.weight = 1;
        moodText.setLayoutParams(moodParams);
        topRow.addView(moodText);

        TextView countText = new TextView(this);
        countText.setText(count + "次");
        countText.setTextSize(14);
        countText.setTextColor(Color.parseColor("#666666"));
        countText.setPadding(0, 0, dpToPx(12), 0);
        topRow.addView(countText);

        TextView percentText = new TextView(this);
        int percent = totalNotes > 0 ? (count * 100 / totalNotes) : 0;
        percentText.setText(percent + "%");
        percentText.setTextSize(14);
        percentText.setTypeface(null, android.graphics.Typeface.BOLD);
        percentText.setTextColor(getMoodColorValue(mood));
        topRow.addView(percentText);

        moodItem.addView(topRow);

        // 进度条
        ProgressBar progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(4)
        );
        progressParams.setMargins(0, dpToPx(6), 0, 0);
        progressBar.setLayoutParams(progressParams);
        progressBar.setProgress(percent);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(getMoodColorValue(mood)));
        moodItem.addView(progressBar);

        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        itemParams.setMargins(0, 0, 0, dpToPx(6));
        container.addView(moodItem, itemParams);
    }

    // 添加标签项
    private void addTagItem(LinearLayout container, String tag, int count) {
        TextView tagView = new TextView(this);
        tagView.setText(tag + " (" + count + ")");
        tagView.setTextSize(13);
        tagView.setTextColor(Color.WHITE);
        tagView.setPadding(dpToPx(14), dpToPx(8), dpToPx(14), dpToPx(8));
        tagView.setGravity(Gravity.CENTER);

        // 根据标签类型设置不同颜色
        int bgResource = getTagBackground(tag);
        tagView.setBackgroundResource(bgResource);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
        tagView.setLayoutParams(params);

        container.addView(tagView);
    }

    // 获取心情表情
    private String getMoodEmoji(String mood) {
        switch (mood) {
            case "开心":
            case "兴奋":
                return "😊";
            case "平静":
            case "思考":
                return "😐";
            case "疲惫":
            case "其他":
                return "😔";
            default:
                return "😊";
        }
    }

    // 获取心情颜色值
    private int getMoodColorValue(String mood) {
        switch (mood) {
            case "开心":
            case "兴奋":
                return Color.parseColor("#4CAF50"); // 绿色
            case "平静":
            case "思考":
                return Color.parseColor("#2196F3"); // 蓝色
            case "疲惫":
            case "其他":
                return Color.parseColor("#9E9E9E"); // 灰色
            default:
                return Color.parseColor("#FF9800"); // 橙色
        }
    }

    // 获取标签背景资源
    private int getTagBackground(String tag) {
        // 默认使用主标签背景
        int defaultBg = R.drawable.bg_tag_primary;

        // 根据标签类型返回对应的背景
        if (tag == null) return defaultBg;

        switch (tag) {
            case "工作":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_work);
                    return R.drawable.bg_tag_work;
                } catch (Exception e) {
                    return defaultBg;
                }
            case "生活":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_life);
                    return R.drawable.bg_tag_life;
                } catch (Exception e) {
                    return defaultBg;
                }
            case "学习":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_study);
                    return R.drawable.bg_tag_study;
                } catch (Exception e) {
                    return defaultBg;
                }
            case "娱乐":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_entertainment);
                    return R.drawable.bg_tag_entertainment;
                } catch (Exception e) {
                    return defaultBg;
                }
            case "健康":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_health);
                    return R.drawable.bg_tag_health;
                } catch (Exception e) {
                    return defaultBg;
                }
            case "其他":
                try {
                    getResources().getDrawable(R.drawable.bg_tag_other);
                    return R.drawable.bg_tag_other;
                } catch (Exception e) {
                    return defaultBg;
                }
            default:
                return defaultBg;
        }
    }

    // =============== 原有方法（保持原样） ===============

    private void loadNotes() {
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

    private void calculateAndDisplayStatistics() {
        // 创建统计对象并计算
        statistics = new NoteStatistics();
        statistics.calculateStatistics(notesList);

        // 显示基本统计
        displayBasicStatistics();

        // 显示活动图表
        displayActivityChart();

        // 显示心情热力图
        displayMoodHeatmap();
    }

    private void displayBasicStatistics() {
        tvMonthlyRecords.setText(String.valueOf(statistics.getMonthlyRecords()));
        tvTotalPhotos.setText(String.valueOf(statistics.getTotalPhotos()));
        tvPrimaryMood.setText(statistics.getPrimaryMood());
        tvCommonTags.setText(String.valueOf(statistics.getCommonTags()));

        // 设置热力图标题
        String heatmapTitle = String.format(Locale.getDefault(), "%d年%d月",
                statistics.getMoodHeatmap().getYear(),
                statistics.getMoodHeatmap().getMonth());
        tvHeatmapTitle.setText(heatmapTitle);
    }

    private void displayActivityChart() {
        activityChartContainer.removeAllViews();
        chartLabelsContainer.removeAllViews();

        List<NoteStatistics.MonthActivity> chartData = statistics.getActivityChart();
        if (chartData.isEmpty()) return;

        // 找到最大值用于计算比例
        int maxValue = 0;
        for (NoteStatistics.MonthActivity activity : chartData) {
            if (activity.getValue() > maxValue) {
                maxValue = activity.getValue();
            }
        }

        // 创建柱状图
        for (int i = 0; i < chartData.size(); i++) {
            NoteStatistics.MonthActivity activity = chartData.get(i);

            // 创建柱子
            View bar = createBarView(activity.getValue(), maxValue);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            barParams.weight = 1;
            barParams.setMargins(4, 0, 4, 0);
            activityChartContainer.addView(bar, barParams);

            // 创建月份标签
            TextView label = createChartLabel(activity.getName());
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            labelParams.weight = 1;
            chartLabelsContainer.addView(label, labelParams);
        }
    }

    private View createBarView(int value, int maxValue) {
        View bar = new View(this);

        // 计算柱子的高度
        int containerHeight = activityChartContainer.getHeight();
        if (containerHeight == 0) containerHeight = 150;

        float heightRatio = maxValue > 0 ? (float) value / maxValue : 0;
        int barHeight = (int) (containerHeight * 0.7 * heightRatio);

        // 设置最小高度
        if (barHeight < 10 && value == 0) {
            barHeight = 4;
        } else if (barHeight < 20) {
            barHeight = 20;
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                barHeight
        );
        params.setMargins(6, 0, 6, 0);
        bar.setLayoutParams(params);

        // 设置颜色
        if (value > 0) {
            if (value == maxValue) {
                bar.setBackgroundColor(Color.parseColor("#1565C0"));
            } else if (value > maxValue * 0.7) {
                bar.setBackgroundColor(Color.parseColor("#1976D2"));
            } else if (value > maxValue * 0.4) {
                bar.setBackgroundColor(Color.parseColor("#2196F3"));
            } else {
                bar.setBackgroundColor(Color.parseColor("#64B5F6"));
            }
        } else {
            bar.setBackgroundColor(Color.parseColor("#E0E0E0"));
        }

        return bar;
    }

    private TextView createChartLabel(String monthName) {
        TextView label = new TextView(this);
        label.setText(monthName);
        label.setTextSize(10);
        label.setTextColor(Color.parseColor("#666666"));
        label.setGravity(Gravity.CENTER);
        return label;
    }

    private void displayMoodHeatmap() {
        moodHeatmapContainer.removeAllViews();

        List<NoteStatistics.MoodDay> moodDays = statistics.getMoodHeatmap().getDays();
        if (moodDays.isEmpty()) return;

        // 获取该月的第一天是星期几（Calendar中周日=1, 周一=2, ..., 周六=7）
        Calendar cal = Calendar.getInstance();
        cal.set(statistics.getMoodHeatmap().getYear(), statistics.getMoodHeatmap().getMonth() - 1, 1);
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // 创建心情日历行
        LinearLayout currentRow = createNewRow();
        moodHeatmapContainer.addView(currentRow);

        // 添加空白日期（在第一天之前）
        for (int i = 1; i < firstDayOfWeek; i++) {
            addEmptyDay(currentRow);
        }

        // 添加实际日期 - 确保每个日期都有框
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            // 如果当前行已满（7个元素），创建新行
            if (currentRow.getChildCount() >= 7) {
                currentRow = createNewRow();
                moodHeatmapContainer.addView(currentRow);
            }

            // 查找这一天的心情记录
            NoteStatistics.MoodDay moodDayForDate = findMoodDayForDate(moodDays, day);
            addMoodDay(currentRow, moodDayForDate, day);
        }
    }

    private NoteStatistics.MoodDay findMoodDayForDate(List<NoteStatistics.MoodDay> moodDays, int day) {
        for (NoteStatistics.MoodDay moodDay : moodDays) {
            String[] dateParts = moodDay.getDate().split("/");
            if (dateParts.length > 1) {
                try {
                    int moodDayNumber = Integer.parseInt(dateParts[1]);
                    if (moodDayNumber == day) {
                        return moodDay;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        // 如果没有找到记录，返回一个空的MoodDay对象
        return new NoteStatistics.MoodDay(statistics.getMoodHeatmap().getMonth() + "/" + day, 0, "无记录");
    }

    private void addMoodDay(LinearLayout row, NoteStatistics.MoodDay moodDay, int dayNumber) {
        TextView dayView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        dayView.setLayoutParams(params);

        // 设置日期文本 - 显示日期数字
        dayView.setText(String.valueOf(dayNumber));
        dayView.setTextSize(12);
        dayView.setGravity(Gravity.CENTER);

        // 设置背景色和文字颜色
        if (moodDay.getMood() > 0) {
            // 有记录的情况
            int backgroundColor = getMoodColor(moodDay.getMood());
            dayView.setBackgroundColor(backgroundColor);
            dayView.setTextColor(Color.WHITE);

            // 添加点击事件显示详细信息
            dayView.setOnClickListener(v -> {
                String message = String.format("%d月%d日: %s",
                        statistics.getMoodHeatmap().getMonth(), dayNumber, moodDay.getLabel());
                Toast.makeText(StatisticsActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        } else {
            // 无记录的情况 - 白色背景，灰色文字
            dayView.setBackgroundColor(Color.WHITE);
            dayView.setTextColor(Color.parseColor("#999999"));

            // 添加边框
            dayView.setBackground(getResources().getDrawable(R.drawable.bg_mood_day_empty));
        }

        // 设置圆角背景
        if (moodDay.getMood() > 0) {
            dayView.setBackground(getResources().getDrawable(R.drawable.bg_mood_day_filled));
        } else {
            dayView.setBackground(getResources().getDrawable(R.drawable.bg_mood_day_empty));
        }

        row.addView(dayView);
    }

    private LinearLayout createNewRow() {
        LinearLayout row = new LinearLayout(this);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private void addEmptyDay(LinearLayout row) {
        View emptyView = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        emptyView.setLayoutParams(params);
        row.addView(emptyView);
    }

    private void addMoodDay(LinearLayout row, NoteStatistics.MoodDay moodDay) {
        TextView dayView = new TextView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(40),
                dpToPx(40)
        );
        params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));
        dayView.setLayoutParams(params);

        // 设置日期文本
        String[] dateParts = moodDay.getDate().split("/");
        String dayText = dateParts.length > 1 ? dateParts[1] : moodDay.getDate();
        dayView.setText(dayText);
        dayView.setTextSize(12);
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextColor(Color.WHITE);

        // 设置背景色
        int backgroundColor = getMoodColor(moodDay.getMood());
        dayView.setBackgroundColor(backgroundColor);

        // 设置圆角
        dayView.setBackground(getResources().getDrawable(R.drawable.bg_mood_day));

        // 添加点击事件
        if (moodDay.getMood() > 0) {
            dayView.setOnClickListener(v -> {
                String message = String.format("%s: %s", moodDay.getDate(), moodDay.getLabel());
                Toast.makeText(StatisticsActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        }

        row.addView(dayView);
    }

    private int getMoodColor(int moodValue) {
        switch (moodValue) {
            case 4: // 兴奋
                return Color.parseColor("#FF6B9C");
            case 3: // 愉快
                return Color.parseColor("#4CAF50");
            case 2: // 平静
                return Color.parseColor("#2196F3");
            case 1: // 低落
                return Color.parseColor("#9E9E9E");
            default: // 无记录
                return Color.parseColor("#F5F5F5");
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

        setSelectedTab(navStats);

        // 记录页面
        navRecord.setOnClickListener(v -> {
            Intent recordIntent = new Intent(StatisticsActivity.this, FirstActivity.class);
            startActivity(recordIntent);
            finish(); // 关闭当前统计页面
        });

        // 整理页面
        navOrganize.setOnClickListener(v -> {
            Intent organizeIntent = new Intent(StatisticsActivity.this, OrganizeActivity.class);
            startActivity(organizeIntent);
            finish();
        });

        // 统计页面 - 不需要跳转
        navStats.setOnClickListener(v -> setSelectedTab(navStats));

        // 个人页面
        navProfile.setOnClickListener(v -> {
            Intent profileIntent = new Intent(StatisticsActivity.this, MainActivity.class);
            startActivity(profileIntent);
            finish();
        });
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