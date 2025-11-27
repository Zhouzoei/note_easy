package com.example.finalwork;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private static final String NOTES_FILE = "notes.json";

    private TextView tvMonthlyRecords, tvTotalPhotos, tvPrimaryMood, tvCommonTags, tvHeatmapTitle;
    private LinearLayout activityChartContainer, chartLabelsContainer, moodHeatmapContainer;

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
        YOUR_API_KEY_HERE();
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
    }

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

    private void YOUR_API_KEY_HERE() {
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
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
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
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
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
