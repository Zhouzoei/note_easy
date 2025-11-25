package com.example.finalwork;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Note implements Serializable {
    private String id;
    private String content;
    private String time;
    private String mood;
    private String tag;
    private boolean hasPhoto;
    private String imagePath;
    private long timestamp;
    private String date; // 添加日期字段

    public Note() {
    }

    public Note(String id, String content, String time, String mood, String tag, boolean hasPhoto, String imagePath) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.mood = mood;
        this.tag = tag;
        this.hasPhoto = hasPhoto;
        this.imagePath = imagePath;
        this.timestamp = System.currentTimeMillis();

        // 自动设置日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.date = dateFormat.format(new Date());
    }

    // Getter and Setter methods
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public boolean hasPhoto() { return hasPhoto; }
    public void setHasPhoto(boolean hasPhoto) { this.hasPhoto = hasPhoto; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}