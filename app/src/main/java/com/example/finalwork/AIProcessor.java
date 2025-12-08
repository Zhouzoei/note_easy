package com.example.finalwork;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AIProcessor {

    // 使用国内可访问的AI API（示例：百度文心一言）
    private static final String BAIDU_API_URL = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
    private static final String BAIDU_ACCESS_TOKEN = "your-baidu-access-token"; // 需要申请

    // 或者使用 OpenAI 兼容的国内代理
    private static final String OPENAI_COMPATIBLE_URL = "https://api.openai-proxy.com/v1/chat/completions";
    private static final String OPENAI_API_KEY = "your-openai-api-key";

    private Context context;
    private Gson gson;

    public interface AIProcessCallback {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public AIProcessor(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    /**
     * 处理笔记整合（本地模拟版本）
     */
    public void processNotes(List<Note> notes, AIProcessCallback callback) {
        if (notes == null || notes.isEmpty()) {
            callback.onFailure("没有可处理的笔记");
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(1500); // 模拟网络延迟

                // 生成模拟的AI整合结果
                StringBuilder result = new StringBuilder();
                result.append("【AI整合日记 - ").append(getCurrentDate()).append("】\n\n");

                // 按时间顺序整理
                List<Note> sortedNotes = new ArrayList<>(notes);
                sortedNotes.sort((n1, n2) -> n1.getTime().compareTo(n2.getTime()));

                // 生成整合内容
                String morning = "";
                String afternoon = "";
                String evening = "";

                for (Note note : sortedNotes) {
                    String time = note.getTime();
                    String content = note.getContent();

                    if (time.compareTo("12:00") < 0) {
                        // 上午
                        if (!morning.isEmpty()) morning += "，";
                        morning += content;
                    } else if (time.compareTo("18:00") < 0) {
                        // 下午
                        if (!afternoon.isEmpty()) afternoon += "，";
                        afternoon += content;
                    } else {
                        // 晚上
                        if (!evening.isEmpty()) evening += "，";
                        evening += content;
                    }
                }

                if (!morning.isEmpty()) {
                    result.append("🌅 上午：").append(morning).append("\n\n");
                }
                if (!afternoon.isEmpty()) {
                    result.append("🌞 下午：").append(afternoon).append("\n\n");
                }
                if (!evening.isEmpty()) {
                    result.append("🌙 晚上：").append(evening).append("\n\n");
                }

                // 添加总结
                result.append("📝 今日总结：今天过得非常充实，记录了生活的点点滴滴。");

                // 返回结果
                callback.onSuccess(result.toString());

            } catch (InterruptedException e) {
                callback.onFailure("处理中断");
            }
        }).start();
    }

    /**
     * 调用真实的AI API（需要网络权限和API密钥）
     */
    public void callRealAIAPI(List<Note> notes, AIProcessCallback callback) {
        new Thread(() -> {
            try {
                // 准备请求数据
                String prompt = buildPrompt(notes);

                // 使用HTTPURLConnection调用API（不需要OkHttp依赖）
                String result = YOUR_API_KEY_HERE(prompt);

                callback.onSuccess(result);

            } catch (Exception e) {
                Log.e("AIProcessor", "API调用失败", e);
                callback.onFailure("AI服务暂时不可用: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(List<Note> notes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请将以下碎片化笔记整合成一篇连贯的日记，要求：\n");
        prompt.append("1. 按照时间顺序整理\n");
        prompt.append("2. 保持自然的叙事风格\n");
        prompt.append("3. 适当添加连接词和过渡句\n");
        prompt.append("4. 总字数控制在300字左右\n\n");
        prompt.append("碎片笔记：\n");

        for (Note note : notes) {
            prompt.append("[").append(note.getTime()).append("] ");
            if (note.getMood() != null && !note.getMood().isEmpty()) {
                prompt.append("心情:").append(note.getMood()).append(" ");
            }
            if (note.getTag() != null && !note.getTag().isEmpty()) {
                prompt.append("标签:").append(note.getTag()).append(" ");
            }
            if (note.getContent() != null && !note.getContent().isEmpty()) {
                prompt.append(note.getContent());
            }
            prompt.append("\n");
        }

        return prompt.toString();
    }

    /**
     * 使用 HttpURLConnection 调用 API
     */
    private String YOUR_API_KEY_HERE(String prompt) throws Exception {
        // 这里需要替换为真实的API调用
        // 以下代码仅为示例，需要根据具体的API文档修改

        String apiUrl = OPENAI_COMPATIBLE_URL;
        String apiKey = OPENAI_API_KEY;

        // 构建请求体
        String requestBody = String.format(
                "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}],\"max_tokens\":500}",
                prompt.replace("\"", "\\\"")
        );

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);

        // 发送请求
        conn.getOutputStream().write(requestBody.getBytes("UTF-8"));

        // 读取响应
        InputStream responseStream = conn.getInputStream();
        Scanner scanner = new Scanner(responseStream, "UTF-8");
        String responseBody = scanner.useDelimiter("\\A").next();
        scanner.close();

        // 解析响应（简化版本，实际需要根据API返回结构解析）
        return parseAPIResponse(responseBody);
    }

    /**
     * 解析API响应
     */
    private String parseAPIResponse(String response) {
        try {
            // 简单的JSON解析
            int contentStart = response.indexOf("\"content\":\"") + 11;
            int contentEnd = response.indexOf("\"", contentStart);
            if (contentStart > 10 && contentEnd > contentStart) {
                return response.substring(contentStart, contentEnd)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
            }
        } catch (Exception e) {
            Log.e("AIProcessor", "解析响应失败", e);
        }
        return "抱歉，AI处理结果解析失败";
    }

    /**
     * 获取当前日期
     */
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy年MM月dd日");
        return sdf.format(new java.util.Date());
    }

    /**
     * Bitmap转Base64（如果需要上传图片）
     */
    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
        byte[] byteArray = outputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    /**
     * 加载图片
     */
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
}