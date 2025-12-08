package com.example.finalwork;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AIProcessor {

    // ==================== 智谱AI配置 ====================
    private static final String ZHIPU_API_KEY = "9fc5e1d6ea014e2abc98107dced82075.tDhbqOiu5gCHiVgO"; // 替换为你的智谱API Key
    private static final String ZHIPU_API_URL = "https://open.bigmodel.cn/api/paas/v4/chat/completions";

    // 使用GLM-4.5-Flash模型（推荐用于文本处理）
    private static final String MODEL_NAME = "glm-4.5-flash";

    private Context context;
    private Gson gson;

    public interface AIProcessCallback {
        void onSuccess(String aiText, List<String> imagePaths, List<String> voicePaths);
        void onFailure(String error);
        void onProgress(String progress);
    }

    public AIProcessor(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    /**
     * 主处理方法 - 仅文本处理
     */
    public void processNotes(List<Note> notes, AIProcessCallback callback) {
        if (notes == null || notes.isEmpty()) {
            callback.onFailure("没有可处理的笔记");
            return;
        }

        if (ZHIPU_API_KEY == null || ZHIPU_API_KEY.isEmpty() || ZHIPU_API_KEY.equals("")) {
            callback.onFailure("请先在AIProcessor.java中配置智谱AI的API Key");
            return;
        }

        // 开始处理
        callback.onProgress("正在分析笔记内容...");
        new ZhipuAIRequestTask(notes, callback).execute();
    }

    /**
     * 异步任务类 - 只处理文本
     */
    private class ZhipuAIRequestTask extends AsyncTask<Void, String, Map<String, Object>> {
        private List<Note> notes;
        private AIProcessCallback callback;
        private Exception exception;

        public ZhipuAIRequestTask(List<Note> notes, AIProcessCallback callback) {
            this.notes = notes;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            callback.onProgress("正在准备文本数据...");
        }

        @Override
        protected Map<String, Object> doInBackground(Void... voids) {
            try {
                publishProgress("正在收集文本内容...");

                // 1. 收集所有信息（为保持接口兼容）
                List<String> imagePaths = new ArrayList<>();
                List<String> voicePaths = new ArrayList<>();
                StringBuilder allText = new StringBuilder();

                // 按时间排序
                List<Note> sortedNotes = new ArrayList<>(notes);
                Collections.sort(sortedNotes, (n1, n2) -> n1.getTime().compareTo(n2.getTime()));

                int textCount = 0;
                for (Note note : sortedNotes) {
                    // 收集图片路径（仅为记录）
                    if (note.hasPhoto() && note.getImagePath() != null) {
                        imagePaths.add(note.getImagePath());
                    }

                    // 收集语音路径（仅为记录）
                    if (note.hasVoice() && note.getVoicePath() != null) {
                        voicePaths.add(note.getVoicePath());
                    }

                    // 收集文本内容
                    if (note.getContent() != null && !note.getContent().isEmpty()) {
                        textCount++;
                        allText.append("【").append(note.getTime()).append("】");

                        if (note.getMood() != null && !note.getMood().isEmpty()) {
                            allText.append("[").append(note.getMood()).append("] ");
                        }

                        if (note.getTag() != null && !note.getTag().isEmpty()) {
                            allText.append("#").append(note.getTag()).append(" ");
                        }

                        allText.append(note.getContent()).append("\n");
                    }
                }

                if (textCount == 0) {
                    throw new Exception("没有找到有效的文本内容");
                }

                publishProgress("正在调用智谱AI-GLM-4.5-Flash...");

                // 2. 调用AI API进行文本处理
                String aiText = callZhipuTextAPI(allText.toString(), textCount);

                // 3. 返回结果
                Map<String, Object> result = new HashMap<>();
                result.put("aiText", aiText);
                result.put("imagePaths", imagePaths);
                result.put("voicePaths", voicePaths);

                return result;

            } catch (Exception e) {
                exception = e;
                Log.e("AIProcessor", "处理失败", e);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (callback != null && values.length > 0) {
                callback.onProgress(values[0]);
            }
        }

        @Override
        protected void onPostExecute(Map<String, Object> result) {
            super.onPostExecute(result);

            if (exception != null) {
                callback.onFailure("处理失败: " + exception.getMessage());
            } else if (result == null) {
                callback.onFailure("AI服务返回空结果");
            } else {
                String aiText = (String) result.get("aiText");
                @SuppressWarnings("unchecked")
                List<String> imagePaths = (List<String>) result.get("imagePaths");
                @SuppressWarnings("unchecked")
                List<String> voicePaths = (List<String>) result.get("voicePaths");

                callback.onSuccess(aiText, imagePaths, voicePaths);
            }
        }
    }

    /**
     * 调用智谱AI API（仅文本处理）
     */
    private String callZhipuTextAPI(String allText, int textCount) throws Exception {
        // 构建消息数组
        JsonArray messages = new JsonArray();

        // 构建用户消息
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");

        // 构建提示词
        String prompt = buildTextPrompt(allText, textCount);
        userMessage.addProperty("content", prompt);
        messages.add(userMessage);

        // 构建完整请求
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", MODEL_NAME);
        requestBody.add("messages", messages);

        // 设置生成参数（优化文本生成）
        JsonObject parameters = new JsonObject();
        parameters.addProperty("max_tokens", 800); // 适当增加输出长度
        parameters.addProperty("temperature", 0.7); // 中等随机性，使文本更自然
        parameters.addProperty("top_p", 0.8);
        parameters.addProperty("stream", false);

        requestBody.add("parameters", parameters);

        Log.d("AIProcessor", "请求模型: " + MODEL_NAME);
        Log.d("AIProcessor", "文本片段数: " + textCount);

        // 发送请求
        return sendZhipuRequest(ZHIPU_API_URL, gson.toJson(requestBody));
    }

    /**
     * 构建文本处理的提示词
     */
    private String buildTextPrompt(String allText, int textCount) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一位日记整理助手，擅长将碎片化的日常记录整合成流畅的日记，不要过多扩展。\n\n");

        prompt.append("以下是我今天记录的").append(textCount).append("条碎片化内容（按时间顺序）：\n");
        prompt.append("```\n");
        prompt.append(allText);
        prompt.append("```\n\n");

        prompt.append("请帮我完成以下任务：\n");
        prompt.append("1. 理解每条记录的情境、情绪和事件\n");
        prompt.append("2. 按时间顺序将这些碎片串联成连贯的叙事\n");
        prompt.append("3. 保持原文的核心信息和情绪基调\n");
        prompt.append("4. 使用自然的连接词使段落流畅\n");
        prompt.append("5. 适当少的润色语言，使日记更可读\n");

        prompt.append("输出要求：\n");
        prompt.append("- 不要添加额外标题\n");
        prompt.append("- 不要使用" + "“根据记录”" + "、" + "“作为助手”" + "等开场白\n");
        prompt.append("- 直接输出整理后的日记正文\n");
        prompt.append("- 字数约100-500字\n");
        prompt.append("- 用第一人称“我”来叙述\n");
        prompt.append("- 保持温暖、自然、简单的写作风格\n\n");

        prompt.append("请开始整理：");

        return prompt.toString();
    }

    /**
     * 发送HTTP请求到智谱AI
     */
    private String sendZhipuRequest(String urlString, String requestBody) throws Exception {
        HttpURLConnection connection = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000); // 30秒连接超时
            connection.setReadTimeout(60000);    // 60秒读取超时
            connection.setDoOutput(true);
            connection.setDoInput(true);

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + ZHIPU_API_KEY);

            // 发送请求
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                StringBuilder response = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;

                while ((bytesRead = reader.read(buffer)) != -1) {
                    response.append(buffer, 0, bytesRead);
                }
                reader.close();

                return parseZhipuResponse(response.toString());
            } else {
                // 读取错误信息
                InputStreamReader errorReader = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
                StringBuilder errorResponse = new StringBuilder();
                char[] buffer = new char[1024];
                int bytesRead;

                while ((bytesRead = errorReader.read(buffer)) != -1) {
                    errorResponse.append(buffer, 0, bytesRead);
                }
                errorReader.close();

                String errorMsg = "HTTP错误 " + responseCode + ": " + errorResponse.toString();
                Log.e("AIProcessor", errorMsg);
                throw new Exception("AI服务调用失败，请检查API Key和网络连接");
            }

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 解析智谱AI响应
     */
    private String parseZhipuResponse(String response) {
        try {
            Log.d("AIProcessor", "原始响应: " + response.substring(0, Math.min(response.length(), 200)));

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

            if (jsonResponse.has("choices") && jsonResponse.getAsJsonArray("choices").size() > 0) {
                JsonObject choice = jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject();

                if (choice.has("message")) {
                    JsonObject message = choice.getAsJsonObject("message");
                    if (message.has("content")) {
                        String content = message.get("content").getAsString().trim();

                        // 清理内容
                        content = cleanAIResponse(content);

                        // 添加基本格式
                        if (!content.contains("【") && !content.contains("[")) {
                            content = formatDiaryContent(content);
                        }

                        return content;
                    }
                }
            }

            Log.w("AIProcessor", "响应解析失败");
            return "AI返回了无法解析的响应，请稍后重试。";

        } catch (Exception e) {
            Log.e("AIProcessor", "解析响应失败", e);
            return "解析AI响应时出错：" + e.getMessage();
        }
    }

    /**
     * 清理AI响应
     */
    private String cleanAIResponse(String content) {
        // 移除可能的JSON转义
        content = content.replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\'", "'")
                .replace("\\t", "\t");

        // 移除常见的问题开头
        String[] unwantedPrefixes = {
                "好的，",
                "根据您提供的",
                "以下是",
                "作为助手，",
                "根据今天的记录，",
                "我来帮您整理"
        };

        for (String prefix : unwantedPrefixes) {
            if (content.startsWith(prefix)) {
                content = content.substring(prefix.length()).trim();
                break;
            }
        }

        // 移除开头和结尾的多余空格和换行
        content = content.trim();

        return content;
    }

    /**
     * 格式化日记内容
     */
    private String formatDiaryContent(String content) {
        StringBuilder formatted = new StringBuilder();

        // 确保有适当的段落分隔
        String[] paragraphs = content.split("\n\n");

        if (paragraphs.length > 1) {
            // 如果已经有段落，保持原样
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    formatted.append(para.trim()).append("\n\n");
                }
            }
        } else {
            // 如果没有段落，尝试按句号分割
            String[] sentences = content.split("。");
            StringBuilder currentParagraph = new StringBuilder();

            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i].trim();
                if (!sentence.isEmpty()) {
                    currentParagraph.append(sentence);

                    if (i < sentences.length - 1) {
                        currentParagraph.append("。");
                    }

                    // 每3-4个句子一个段落
                    if ((i + 1) % 4 == 0 || i == sentences.length - 1) {
                        formatted.append(currentParagraph.toString()).append("\n\n");
                        currentParagraph = new StringBuilder();
                    }
                }
            }
        }

        return formatted.toString().trim();
    }

    /**
     * 获取当前日期
     */
    public String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault());
        return sdf.format(new Date());
    }
}