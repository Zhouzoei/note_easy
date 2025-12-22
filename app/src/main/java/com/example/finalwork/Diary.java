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
    private List<String> imagePaths; // 修复：移除static
    private List<String> voicePaths; // 修复：移除static
    private int moodCount;
    private int wordCount;
    private String style;

    public Diary(String id, String title, String content, String date) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
        this.imagePaths = new ArrayList<>(); // 每个日记实例都有自己的列表
        this.voicePaths = new ArrayList<>();  // 每个日记实例都有自己的列表
        this.wordCount = content.length();
    }

    // 修复：fromAIResult方法需要接收图片和语音路径
    public static Diary fromAIResult(String aiContent, List<String> imagePaths, List<String> voicePaths, AIProcessor.DiaryStyle style) {
        String diaryId = "diary_" + System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String title = "日记 - " + date;

        Diary diary = new Diary(diaryId, title, aiContent, date);
        diary.setImagePaths(imagePaths);
        diary.setVoicePaths(voicePaths);
        diary.setStyle(style != null ? style.getDisplayName() : "简洁风格");
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
        this.imagePaths = imagePaths != null ? new ArrayList<>(imagePaths) : new ArrayList<>();
    }

    public void setVoicePaths(List<String> voicePaths) {
        this.voicePaths = voicePaths != null ? new ArrayList<>(voicePaths) : new ArrayList<>();
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
    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}
