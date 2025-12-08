package com.example.finalwork;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Diary {
    private String id;
    private String title;
    private String content;
    private String date; // 格式: yyyy-MM-dd
    private long timestamp;
    private String coverImage; // 封面图片路径（可选）
    private int moodCount; // 情绪统计
    private int wordCount; // 字数统计

    public Diary(String id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
        this.wordCount = content.length();
    }

    // 从AI结果创建日记
    public static Diary fromAIResult(String aiContent) {
        String diaryId = "diary_" + System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String title = "日记 - " + date;

        return new Diary(diaryId, title, aiContent, date);
    }

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }
    public String getCoverImage() { return coverImage; }
    public int getMoodCount() { return moodCount; }
    public int getWordCount() { return wordCount; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    // 格式化显示日期
    public String getDisplayDate() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (Exception e) {
            return date;
        }
    }

    // 获取简短预览
    public String getPreview() {
        if (content.length() > 100) {
            return content.substring(0, 100) + "...";
        }
        return content;
    }
}