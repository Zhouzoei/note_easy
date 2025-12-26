package com.example.finalwork;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DiaryManager {

    // 移除固定的文件名常量，改为动态生成
    // private static final String DIARY_FILE = "diaries.json";

    private static final String TODAY_DIARY_FILE_PREFIX = "today_diary_cache_"; // 今日缓存也需要隔离
    private static DiaryManager instance;
    private Context context;
    private Gson gson;
    private List<Diary> diaries;

    private DiaryManager(Context context) {
        this.context = context.getApplicationContext(); // 使用Application Context防止内存泄漏
        this.gson = new Gson();
        this.diaries = new ArrayList<>();
        loadDiaries();
    }

    public static synchronized DiaryManager getInstance(Context context) {
        if (instance == null) {
            instance = new DiaryManager(context);
        }
        return instance;
    }

    /**
     * 获取当前用户的专属文件名
     */
    private String getDiaryFileName() {
        UserManager userManager = new UserManager(context);
        String currentUser = userManager.getCurrentUser();
        if (currentUser == null) {
            currentUser = "guest"; // 防止空指针，未登录时暂存为guest
        }
        return "diaries_" + currentUser + ".json";
    }

    /**
     * 获取今日缓存文件名
     */
    private String getTodayCacheFileName() {
        UserManager userManager = new UserManager(context);
        String currentUser = userManager.getCurrentUser();
        if (currentUser == null) currentUser = "guest";
        return TODAY_DIARY_FILE_PREFIX + currentUser + ".json";
    }

    /**
     * 添加新日记
     */
    public void addDiary(Diary diary) {
        // 检查是否已存在同一天的日记
        for (int i = 0; i < diaries.size(); i++) {
            if (diaries.get(i).getDate().equals(diary.getDate())) {
                // 替换现有日记
                diaries.set(i, diary);
                saveDiaries();
                return;
            }
        }

        // 添加新日记
        diaries.add(0, diary); // 最新的在最前面
        saveDiaries();
    }

    /**
     * 保存AI生成的日记（修复版本）
     */
    public void saveAIDiary(String aiContent, List<String> imagePaths, List<String> voicePaths, AIProcessor.DiaryStyle style) {
        Diary diary = Diary.fromAIResult(aiContent, imagePaths, voicePaths, style);
        addDiary(diary);
    }

    /**
     * 保存今日日记到缓存 (修改为动态文件名)
     */
    public void saveTodayDiary(String content, List<String> imagePaths, List<String> voicePaths) {
        try {
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            TodayDiaryCache cache = new TodayDiaryCache(currentDate, content, imagePaths, voicePaths);

            String json = gson.toJson(cache);
            // 使用动态文件名
            FileOutputStream fos = context.openFileOutput(getTodayCacheFileName(), Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载今日日记缓存 (修改为动态文件名)
     */
    public TodayDiaryCache loadTodayDiary() {
        try {
            // 使用动态文件名
            FileInputStream fis = context.openFileInput(getTodayCacheFileName());
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            String json = new String(buffer);
            TodayDiaryCache cache = gson.fromJson(json, TodayDiaryCache.class);

            // 检查是否是今天的日记
            if (cache != null) {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                if (currentDate.equals(cache.getDate())) {
                    return cache;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 清除今日日记缓存 (修改为动态文件名)
     */
    public void clearTodayDiary() {
        try {
            java.io.File cacheFile = new java.io.File(context.getFilesDir(), getTodayCacheFileName());
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据日期获取日记
     */
    public Diary getDiaryByDate(String date) {
        for (Diary diary : diaries) {
            if (diary.getDate().equals(date)) {
                return diary;
            }
        }
        return null;
    }

    /**
     * 获取所有日记（按日期倒序）
     */
    public List<Diary> getAllDiaries() {
        Collections.sort(diaries, new Comparator<Diary>() {
            @Override
            public int compare(Diary d1, Diary d2) {
                return Long.compare(d2.getTimestamp(), d1.getTimestamp());
            }
        });
        return new ArrayList<>(diaries);
    }

    /**
     * 按月份分组日记
     */
    public Map<String, List<Diary>> getDiariesByMonth() {
        Map<String, List<Diary>> monthlyDiaries = new HashMap<>();

        for (Diary diary : diaries) {
            String month = diary.getDate().substring(0, 7); // yyyy-MM
            if (!monthlyDiaries.containsKey(month)) {
                monthlyDiaries.put(month, new ArrayList<>());
            }
            monthlyDiaries.get(month).add(diary);
        }

        return monthlyDiaries;
    }

    /**
     * 删除日记
     */
    public boolean deleteDiary(String diaryId) {
        for (int i = 0; i < diaries.size(); i++) {
            if (diaries.get(i).getId().equals(diaryId)) {
                diaries.remove(i);
                saveDiaries();
                return true;
            }
        }
        return false;
    }

    /**
     * 保存日记到文件 (修改为动态文件名)
     */
    private void saveDiaries() {
        try {
            String json = gson.toJson(diaries);
            // 使用动态文件名：getDiaryFileName()
            FileOutputStream fos = context.openFileOutput(getDiaryFileName(), Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载日记 (修改为动态文件名)
     */
    private void loadDiaries() {
        try {
            // 使用动态文件名：getDiaryFileName()
            FileInputStream fis = context.openFileInput(getDiaryFileName());
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            String json = new String(buffer);
            Type listType = new TypeToken<ArrayList<Diary>>(){}.getType();
            List<Diary> loadedDiaries = gson.fromJson(json, listType);

            if (loadedDiaries != null) {
                diaries.clear();
                diaries.addAll(loadedDiaries);
            }
        } catch (Exception e) {
            // 如果文件不存在（新用户），清空列表即可
            diaries.clear();
        }
    }

    /**
     * 获取日记统计
     */
    public Map<String, Object> getDiaryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDiaries", diaries.size());
        stats.put("totalWords", calculateTotalWords());
        stats.put("firstDiaryDate", getFirstDiaryDate());
        stats.put("lastDiaryDate", getLastDiaryDate());
        return stats;
    }

    private int calculateTotalWords() {
        int total = 0;
        for (Diary diary : diaries) {
            total += diary.getWordCount();
        }
        return total;
    }

    private String getFirstDiaryDate() {
        if (diaries.isEmpty()) return "";
        Diary oldest = Collections.min(diaries, Comparator.comparing(Diary::getTimestamp));
        return oldest.getDisplayDate();
    }

    private String getLastDiaryDate() {
        if (diaries.isEmpty()) return "";
        Diary newest = Collections.max(diaries, Comparator.comparing(Diary::getTimestamp));
        return newest.getDisplayDate();
    }

    /**
     * 今日日记缓存数据类
     */
    public static class TodayDiaryCache {
        private String date;
        private String content;
        private List<String> imagePaths;
        private List<String> voicePaths;

        public TodayDiaryCache() {}

        public TodayDiaryCache(String date, String content, List<String> imagePaths, List<String> voicePaths) {
            this.date = date;
            this.content = content;
            this.imagePaths = imagePaths != null ? new ArrayList<>(imagePaths) : new ArrayList<>();
            this.voicePaths = voicePaths != null ? new ArrayList<>(voicePaths) : new ArrayList<>();
        }

        // Getter方法
        public String getDate() { return date; }
        public String getContent() { return content; }
        public List<String> getImagePaths() { return imagePaths; }
        public List<String> getVoicePaths() { return voicePaths; }

        // Setter方法
        public void setDate(String date) { this.date = date; }
        public void setContent(String content) { this.content = content; }
        public void setImagePaths(List<String> imagePaths) {
            this.imagePaths = imagePaths != null ? new ArrayList<>(imagePaths) : new ArrayList<>();
        }
        public void setVoicePaths(List<String> voicePaths) {
            this.voicePaths = voicePaths != null ? new ArrayList<>(voicePaths) : new ArrayList<>();
        }
    }
    // 在 DiaryManager.java 中添加
    public void reload() {
        diaries.clear();
        loadDiaries();
    }

}
