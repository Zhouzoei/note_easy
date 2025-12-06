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
    private boolean hasVoice;
    private String voicePath;
    private long audioDuration;
    private long timestamp;
    private String date;

    public Note() {
    }

    // 原有的构造函数（保持向后兼容）
    public Note(String id, String content, String time, String mood, String tag, boolean hasPhoto, String imagePath) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.mood = mood;
        this.tag = tag;
        this.hasPhoto = hasPhoto;
        this.imagePath = imagePath;
        this.hasVoice = false;  // 默认没有语音
        this.voicePath = null;
        this.audioDuration = 0;
        this.timestamp = System.currentTimeMillis();

        // 自动设置日期
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.date = dateFormat.format(new Date());
    }

    // 新的构造函数（支持语音功能）
    public Note(String id, String content, String time, String mood, String tag,
                boolean hasPhoto, String imagePath, boolean hasVoice,
                String voicePath, long audioDuration) {
        this.id = id;
        this.content = content;
        this.time = time;
        this.mood = mood;
        this.tag = tag;
        this.hasPhoto = hasPhoto;
        this.imagePath = imagePath;
        this.hasVoice = hasVoice;
        this.voicePath = voicePath;
        this.audioDuration = audioDuration;
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

    public boolean hasVoice() { return hasVoice; }
    public void setHasVoice(boolean hasVoice) { this.hasVoice = hasVoice; }

    public String getVoicePath() { return voicePath; }
    public void setVoicePath(String voicePath) { this.voicePath = voicePath; }

    public long getAudioDuration() { return audioDuration; }
    public void setAudioDuration(long audioDuration) { this.audioDuration = audioDuration; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}