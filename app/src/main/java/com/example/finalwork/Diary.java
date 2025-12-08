package com.example.finalwork;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Diary {
    private String id;
    private String title;
    private String content;
    private String date; // 格式: yyyy-MM-dd
    private long timestamp;
    private static List<String> imagePaths; // 引用的图片路径
    private static List<String> voicePaths; // 引用的语音路径
    private int moodCount; // 情绪统计
    private int wordCount; // 字数统计

    public Diary(String id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
        this.imagePaths = new ArrayList<>();
        this.voicePaths = new ArrayList<>();
        this.wordCount = content.length();
    }

    // 从AI结果创建日记
    public static Diary fromAIResult(String aiContent) {
        String diaryId = "diary_" + System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String title = "日记 - " + date;

        Diary diary = new Diary(diaryId, title, aiContent, date);
        diary.setImagePaths(imagePaths);
        diary.setVoicePaths(voicePaths);
        return diary;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }
    public List<String> getImagePaths() { return imagePaths; }
    public List<String> getVoicePaths() { return voicePaths; }
    public int getMoodCount() { return moodCount; }
    public int getWordCount() { return wordCount; }

    public void setTitle(String title) { this.title = title; }
    public void setImagePaths(List<String> imagePaths) {
        if (imagePaths != null) {
            this.imagePaths = imagePaths;
        }
    }
    public void setVoicePaths(List<String> voicePaths) {
        if (voicePaths != null) {
            this.voicePaths = voicePaths;
        }
    }

    // 添加图片路径
    public void addImagePath(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            this.imagePaths.add(imagePath);
        }
    }

    // 添加语音路径
    public void addVoicePath(String voicePath) {
        if (voicePath != null && !voicePath.isEmpty()) {
            this.voicePaths.add(voicePath);
        }
    }

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

    // 获取统计信息
    public String getStats() {
        return "字数: " + wordCount +
                " | 图片: " + imagePaths.size() + "张" +
                " | 语音: " + voicePaths.size() + "条";
    }
}