package com.example.finalwork;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NoteStatistics implements Serializable {
    private String date;
    private int monthlyRecords;
    private int totalPhotos;
    private String primaryMood;
    private int commonTags;
    private List<MonthActivity> activityChart;
    private MoodHeatmap moodHeatmap;

    public NoteStatistics() {
        this.date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        this.activityChart = new ArrayList<>();
        this.moodHeatmap = new MoodHeatmap();
    }

    // 统计方法
    public void calculateStatistics(List<Note> allNotes) {
        if (allNotes == null || allNotes.isEmpty()) {
            initializeEmptyStatistics();
            return;
        }

        // 获取当前日期信息
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH) + 1;

        // 1. 统计本月记录数
        this.monthlyRecords = countMonthlyRecords(allNotes, currentYear, currentMonth);

        // 2. 统计总照片数
        this.totalPhotos = countTotalPhotos(allNotes);

        // 3. 统计主要心情
        this.primaryMood = calculatePrimaryMood(allNotes);

        // 4. 统计常用标签数
        this.commonTags = countCommonTags(allNotes);

        // 5. 生成活动图表
        this.activityChart = generateActivityChart(allNotes, currentYear);

        // 6. 生成心情热力图
        this.moodHeatmap = generateMoodHeatmap(allNotes, currentYear, currentMonth);
    }

    private void initializeEmptyStatistics() {
        this.monthlyRecords = 0;
        this.totalPhotos = 0;
        this.primaryMood = "无数据";
        this.commonTags = 0;
        this.activityChart = generateEmptyActivityChart();
        this.moodHeatmap = generateEmptyMoodHeatmap();
    }

    private int countMonthlyRecords(List<Note> notes, int year, int month) {
        int count = 0;
        for (Note note : notes) {
            if (isNoteInMonth(note, year, month)) {
                count++;
            }
        }
        return count;
    }

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

    private int countTotalPhotos(List<Note> notes) {
        int count = 0;
        for (Note note : notes) {
            if (note.hasPhoto()) {
                count++;
            }
        }
        return count;
    }

    private String calculatePrimaryMood(List<Note> notes) {
        Map<String, Integer> moodCount = new HashMap<>();

        for (Note note : notes) {
            if (note.getMood() != null && !note.getMood().isEmpty()) {
                moodCount.put(note.getMood(), moodCount.getOrDefault(note.getMood(), 0) + 1);
            }
        }

        if (moodCount.isEmpty()) {
            return "无记录";
        }

        // 找到出现次数最多的心情
        return Collections.max(moodCount.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private int countCommonTags(List<Note> notes) {
        Map<String, Integer> tagCount = new HashMap<>();

        for (Note note : notes) {
            if (note.getTag() != null && !note.getTag().isEmpty()) {
                tagCount.put(note.getTag(), tagCount.getOrDefault(note.getTag(), 0) + 1);
            }
        }

        // 统计出现超过1次的标签数量
        int commonCount = 0;
        for (int count : tagCount.values()) {
            if (count > 1) {
                commonCount++;
            }
        }

        return commonCount;
    }

    private List<MonthActivity> generateActivityChart(List<Note> notes, int currentYear) {
        List<MonthActivity> chart = new ArrayList<>();

        // 生成最近6个月的数据
        Calendar cal = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1;
            String monthName = getMonthName(month);

            int count = countMonthlyRecords(notes, year, month);
            chart.add(new MonthActivity(monthName, count));
        }

        return chart;
    }

    private List<MonthActivity> generateEmptyActivityChart() {
        List<MonthActivity> chart = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (int i = 5; i >= 0; i--) {
            cal.setTime(new Date());
            cal.add(Calendar.MONTH, -i);
            String monthName = getMonthName(cal.get(Calendar.MONTH) + 1);
            chart.add(new MonthActivity(monthName, 0));
        }
        return chart;
    }

    private MoodHeatmap generateMoodHeatmap(List<Note> notes, int year, int month) {
        MoodHeatmap heatmap = new MoodHeatmap();
        heatmap.setYear(year);
        heatmap.setMonth(month);

        // 获取该月的天数
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // 为每一天创建MoodDay对象，包括没有记录的日期
        for (int day = 1; day <= daysInMonth; day++) {
            String dateStr = String.format(Locale.getDefault(), "%d/%d", month, day);

            // 查找这一天是否有心情记录
            boolean hasRecord = false;
            for (Note note : notes) {
                if (isNoteOnDate(note, year, month, day)) {
                    // 转换心情为数值
                    int moodValue = convertMoodToValue(note.getMood());
                    String moodLabel = getMoodLabel(moodValue);

                    heatmap.addDay(new MoodDay(dateStr, moodValue, moodLabel));
                    hasRecord = true;
                    break;
                }
            }

            // 如果没有记录，添加一个无记录的MoodDay
            if (!hasRecord) {
                heatmap.addDay(new MoodDay(dateStr, 0, "无记录"));
            }
        }

        return heatmap;
    }

    private boolean isNoteOnDate(Note note, int year, int month, int day) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date noteDate = sdf.parse(note.getDate());
            Calendar noteCal = Calendar.getInstance();
            noteCal.setTime(noteDate);

            int noteYear = noteCal.get(Calendar.YEAR);
            int noteMonth = noteCal.get(Calendar.MONTH) + 1;
            int noteDay = noteCal.get(Calendar.DAY_OF_MONTH);

            return noteYear == year && noteMonth == month && noteDay == day;
        } catch (Exception e) {
            return false;
        }
    }

    private MoodHeatmap generateEmptyMoodHeatmap() {
        MoodHeatmap heatmap = new MoodHeatmap();
        Calendar cal = Calendar.getInstance();
        heatmap.setYear(cal.get(Calendar.YEAR));
        heatmap.setMonth(cal.get(Calendar.MONTH) + 1);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int day = 1; day <= daysInMonth; day++) {
            String dateStr = String.format(Locale.getDefault(), "%d/%d", heatmap.getMonth(), day);
            heatmap.addDay(new MoodDay(dateStr, 0, "无记录"));
        }

        return heatmap;
    }

    private int convertMoodToValue(String mood) {
        if (mood == null) return 0;

        switch (mood) {
            case "开心":
            case "兴奋":
                return 4;
            case "平静":
            case "思考":
                return 2;
            case "疲惫":
            case "其他":
                return 1;
            default:
                return 3; // 默认愉快
        }
    }

    private String getMoodLabel(int moodValue) {
        switch (moodValue) {
            case 4: return "兴奋";
            case 3: return "愉快";
            case 2: return "平静";
            case 1: return "低落";
            default: return "无记录";
        }
    }

    private String getMonthName(int month) {
        String[] monthNames = {"1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月"};
        return monthNames[month - 1];
    }

    // Getter and Setter methods
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public int getMonthlyRecords() { return monthlyRecords; }
    public void setMonthlyRecords(int monthlyRecords) { this.monthlyRecords = monthlyRecords; }

    public int getTotalPhotos() { return totalPhotos; }
    public void setTotalPhotos(int totalPhotos) { this.totalPhotos = totalPhotos; }

    public String getPrimaryMood() { return primaryMood; }
    public void setPrimaryMood(String primaryMood) { this.primaryMood = primaryMood; }

    public int getCommonTags() { return commonTags; }
    public void setCommonTags(int commonTags) { this.commonTags = commonTags; }

    public List<MonthActivity> getActivityChart() { return activityChart; }
    public void setActivityChart(List<MonthActivity> activityChart) { this.activityChart = activityChart; }

    public MoodHeatmap getMoodHeatmap() { return moodHeatmap; }
    public void setMoodHeatmap(MoodHeatmap moodHeatmap) { this.moodHeatmap = moodHeatmap; }

    // 内部类 - 月度活动
    public static class MonthActivity implements Serializable {
        private String name;
        private int value;

        public MonthActivity(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }

    // 内部类 - 心情热力图
    public static class MoodHeatmap implements Serializable {
        private int year;
        private int month;
        private List<MoodDay> days;

        public MoodHeatmap() {
            this.days = new ArrayList<>();
        }

        public void addDay(MoodDay day) {
            days.add(day);
        }

        public void updateDay(String date, int mood, String label) {
            for (MoodDay day : days) {
                if (day.getDate().equals(date)) {
                    day.setMood(mood);
                    day.setLabel(label);
                    break;
                }
            }
        }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public int getMonth() { return month; }
        public void setMonth(int month) { this.month = month; }

        public List<MoodDay> getDays() { return days; }
        public void setDays(List<MoodDay> days) { this.days = days; }
    }

    // 内部类 - 心情日期
    public static class MoodDay implements Serializable {
        private String date;
        private int mood;
        private String label;

        public MoodDay(String date, int mood, String label) {
            this.date = date;
            this.mood = mood;
            this.label = label;
        }
        public boolean hasRecord() {
            return mood > 0;
        }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public int getMood() { return mood; }
        public void setMood(int mood) { this.mood = mood; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }
}