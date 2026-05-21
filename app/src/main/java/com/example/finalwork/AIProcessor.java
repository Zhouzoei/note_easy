package com.example.finalwork;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
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

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/chat/completions";
    private static final String MODEL_NAME = "deepseek-chat";

    // ==================== 重试策略 ====================
    private static final int MAX_RETRIES = 3;
    private static final long BASE_RETRY_DELAY_MS = 1000;
    private static final long MAX_RETRY_DELAY_MS = 8000;

    // ==================== 错误码定义 ====================
    public static final int ERROR_AUTH = 401;
    public static final int ERROR_RATE_LIMIT = 429;
    public static final int ERROR_SERVER = 500;
    public static final int ERROR_SERVER_UNAVAILABLE = 503;
    public static final int ERROR_NETWORK = -1;
    public static final int ERROR_TIMEOUT = -2;
    public static final int ERROR_PARSE = -3;
    public static final int ERROR_UNKNOWN = -99;

    private Context context;
    private Gson gson;

    public enum DiaryStyle {
        SIMPLE("简洁风格", "用简洁明了的语言记录，重点突出核心事件和感受"),
        LITERARY("文艺风格", "用优美的文字表达，注重情感渲染和意境营造"),
        HUMOROUS("轻松幽默风格", "用轻松幽默的语调，让日记读起来更有趣");

        private final String displayName;
        private final String description;

        DiaryStyle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    public interface AIProcessCallback {
        void onSuccess(String aiText, List<String> imagePaths, List<String> voicePaths);
        void onFailure(String error, int errorCode);
        void onProgress(String progress);
        void onStreamContent(String partialContent);
    }

    public AIProcessor(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    private String getApiKey() {
        try {
            Class<?> buildConfigClass = Class.forName("com.example.finalwork.BuildConfig");
            java.lang.reflect.Field field = buildConfigClass.getField("DEEPSEEK_API_KEY");
            String key = (String) field.get(null);
            if (key != null && !key.isEmpty() && !"YOUR_API_KEY_HERE".equals(key)) {
                return key;
            }
        } catch (Exception e) {
            Log.e("AIProcessor", "读取 BuildConfig.DEEPSEEK_API_KEY 失败", e);
        }
        return null;
    }

    public void processNotes(List<Note> notes, DiaryStyle style, AIProcessCallback callback) {
        if (notes == null || notes.isEmpty()) {
            callback.onFailure("没有可处理的笔记", ERROR_UNKNOWN);
            return;
        }

        String apiKey = getApiKey();
        if (apiKey == null) {
            callback.onFailure("API Key 未配置。请在 gradle.properties 中设置 DEEPSEEK_API_KEY", ERROR_AUTH);
            return;
        }

        callback.onProgress("正在分析笔记内容...");
        new DeepSeekAIRequestTask(notes, style, callback, apiKey).execute();
    }

    private static long computeRetryDelay(int attempt) {
        long delay = (long) (BASE_RETRY_DELAY_MS * Math.pow(2, attempt));
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }

    private static boolean isRetryable(int statusCode) {
        return statusCode == ERROR_RATE_LIMIT
            || statusCode == ERROR_SERVER
            || statusCode == ERROR_SERVER_UNAVAILABLE
            || statusCode == ERROR_NETWORK
            || statusCode == ERROR_TIMEOUT;
    }

    private static String classifyError(int statusCode) {
        switch (statusCode) {
            case ERROR_AUTH:
                return "认证失败（401）：API Key 无效或已过期，请在 gradle.properties 中检查 DEEPSEEK_API_KEY";
            case ERROR_RATE_LIMIT:
                return "请求过于频繁（429）：已触发限流，请稍后重试";
            case ERROR_SERVER:
                return "DeepSeek 服务内部错误（500）";
            case ERROR_SERVER_UNAVAILABLE:
                return "DeepSeek 服务暂不可用（503）";
            case ERROR_NETWORK:
                return "网络连接失败，请检查网络设置";
            case ERROR_TIMEOUT:
                return "请求超时，服务器响应过慢";
            default:
                return "未知错误（HTTP " + statusCode + "）";
        }
    }

    private static int extractStatusCode(Exception e) {
        if (e instanceof UnknownHostException) return ERROR_NETWORK;
        if (e instanceof SocketTimeoutException) return ERROR_TIMEOUT;
        if (e instanceof java.net.ConnectException) return ERROR_NETWORK;
        return ERROR_UNKNOWN;
    }

    private class DeepSeekAIRequestTask extends AsyncTask<Void, String, Map<String, Object>> {
        private List<Note> notes;
        private DiaryStyle style;
        private AIProcessCallback callback;
        private String apiKey;
        private Exception exception;

        DeepSeekAIRequestTask(List<Note> notes, DiaryStyle style, AIProcessCallback callback, String apiKey) {
            this.notes = notes;
            this.style = style != null ? style : DiaryStyle.SIMPLE;
            this.callback = callback;
            this.apiKey = apiKey;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            callback.onProgress("正在准备文本数据...");
        }

        @Override
        protected Map<String, Object> doInBackground(Void... voids) {
            try {
                publishProgress("progress", "正在收集文本内容...");

                List<String> imagePaths = new ArrayList<>();
                List<String> voicePaths = new ArrayList<>();
                StringBuilder allText = new StringBuilder();

                List<Note> sortedNotes = new ArrayList<>(notes);
                Collections.sort(sortedNotes, (n1, n2) -> n1.getTime().compareTo(n2.getTime()));

                int textCount = 0;
                for (Note note : sortedNotes) {
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
                    if (note.hasPhoto() && note.getImagePath() != null) {
                        imagePaths.add(note.getImagePath());
                    }
                    if (note.hasVoice() && note.getVoicePath() != null) {
                        voicePaths.add(note.getVoicePath());
                    }
                }

                if (textCount == 0) {
                    throw new AIProcessorException("没有找到有效的文本内容", ERROR_UNKNOWN);
                }

                publishProgress("progress", "正在调用 DeepSeek AI...");

                String aiText = callDeepSeekStreamAPI(allText.toString(), textCount, style);

                Map<String, Object> result = new HashMap<>();
                result.put("aiText", aiText);
                result.put("imagePaths", imagePaths);
                result.put("voicePaths", voicePaths);
                return result;

            } catch (AIProcessorException e) {
                exception = e;
                Log.e("AIProcessor", "处理失败: code=" + e.errorCode, e);
                return null;
            } catch (Exception e) {
                exception = new AIProcessorException(e.getMessage(), extractStatusCode(e));
                Log.e("AIProcessor", "处理失败", e);
                return null;
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            if (callback != null) {
                String tag = values[0];
                String payload = values.length > 1 ? values[1] : "";
                switch (tag) {
                    case "progress":
                        callback.onProgress(payload);
                        break;
                    case "stream":
                        callback.onStreamContent(payload);
                        break;
                }
            }
        }

        @Override
        protected void onPostExecute(Map<String, Object> result) {
            super.onPostExecute(result);

            if (exception != null) {
                int code = ERROR_UNKNOWN;
                String msg = exception.getMessage();
                if (exception instanceof AIProcessorException) {
                    code = ((AIProcessorException) exception).errorCode;
                }
                callback.onFailure("处理失败: " + msg, code);
            } else if (result == null) {
                callback.onFailure("AI 服务返回空结果", ERROR_PARSE);
            } else {
                String aiText = (String) result.get("aiText");
                @SuppressWarnings("unchecked")
                List<String> imagePaths = (List<String>) result.get("imagePaths");
                @SuppressWarnings("unchecked")
                List<String> voicePaths = (List<String>) result.get("voicePaths");
                callback.onSuccess(aiText, imagePaths, voicePaths);
            }
        }

        private String callDeepSeekStreamAPI(String allText, int textCount, DiaryStyle style) throws Exception {
            JsonArray messages = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", PromptTemplate.buildSystemPrompt(
                PromptTemplate.fromDiaryStyle(style)));
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", PromptTemplate.buildUserPrompt(allText, textCount,
                PromptTemplate.fromDiaryStyle(style)));
            messages.add(userMessage);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL_NAME);
            requestBody.add("messages", messages);

            requestBody.addProperty("max_tokens", PromptTemplate.getMaxTokens());
            requestBody.addProperty("temperature", (double) PromptTemplate.getTemperature());
            requestBody.addProperty("top_p", (double) PromptTemplate.getTopP());
            requestBody.addProperty("stream", true);

            Log.d("AIProcessor", "请求模型: " + MODEL_NAME
                + ", 文本片段数: " + textCount
                + ", prompt版本: " + PromptTemplate.TEMPLATE_VERSION);

            return sendStreamRequest(DEEPSEEK_API_URL, gson.toJson(requestBody));
        }

        private String sendStreamRequest(String urlString, String requestBody) throws Exception {
            Exception lastException = null;
            int totalRetries = 0;

            for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
                if (attempt > 0) {
                    long delay = computeRetryDelay(attempt - 1);
                    totalRetries = attempt;
                    publishProgress("progress",
                        "请求失败，正在重试 (" + attempt + "/" + MAX_RETRIES + ")...");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                }

                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(90000);
                    connection.setDoOutput(true);
                    connection.setDoInput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Accept", "text/event-stream");
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                    try (OutputStream os = connection.getOutputStream()) {
                        byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = connection.getResponseCode();

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        return parseStreamResponse(connection);
                    }

                    if (!isRetryable(responseCode)) {
                        String detail = readErrorStream(connection);
                        throw new AIProcessorException(classifyError(responseCode) + ": " + detail, responseCode);
                    }

                    lastException = new AIProcessorException(classifyError(responseCode), responseCode);
                    Log.w("AIProcessor", "请求失败 (attempt " + (attempt + 1) + "): HTTP " + responseCode);

                } catch (AIProcessorException e) {
                    throw e;
                } catch (Exception e) {
                    int code = extractStatusCode(e);
                    if (!isRetryable(code)) {
                        throw new AIProcessorException(classifyError(code), code);
                    }
                    lastException = e;
                    Log.w("AIProcessor", "请求异常 (attempt " + (attempt + 1) + ")", e);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }

            String summary = "请求重试 " + totalRetries + " 次后仍失败";
            if (lastException != null) {
                int code = extractStatusCode(lastException);
                throw new AIProcessorException(summary + ": " + lastException.getMessage(), code);
            }
            throw new AIProcessorException(summary, ERROR_UNKNOWN);
        }

        private String parseStreamResponse(HttpURLConnection connection) throws Exception {
            StringBuilder fullContent = new StringBuilder();
            boolean hasContent = false;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if ("[DONE]".equals(data)) {
                            break;
                        }
                        try {
                            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
                            if (json.has("choices")) {
                                JsonArray choices = json.getAsJsonArray("choices");
                                if (choices.size() > 0) {
                                    JsonObject choice = choices.get(0).getAsJsonObject();
                                    if (choice.has("delta")) {
                                        JsonObject delta = choice.getAsJsonObject("delta");
                                        if (delta.has("content")) {
                                            String chunk = delta.get("content").getAsString();
                                            fullContent.append(chunk);
                                            hasContent = true;
                                            publishProgress("stream", chunk);
                                        }
                                    }
                                    if (choice.has("finish_reason") && !choice.get("finish_reason").isJsonNull()) {
                                        String finishReason = choice.get("finish_reason").getAsString();
                                        if ("stop".equals(finishReason) || "length".equals(finishReason)) {
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.w("AIProcessor", "解析流式数据块失败: " + data, e);
                        }
                    }
                }
            }

            if (!hasContent) {
                throw new AIProcessorException("AI 返回了空内容", ERROR_PARSE);
            }

            String content = fullContent.toString().trim();
            content = cleanAIResponse(content);
            if (!content.contains("【") && !content.contains("[")) {
                content = formatDiaryContent(content);
            }
            return content;
        }

        private String readErrorStream(HttpURLConnection connection) {
            try {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                return sb.length() > 200 ? sb.substring(0, 200) + "..." : sb.toString();
            } catch (Exception e) {
                return "(无法读取错误详情)";
            }
        }
    }

    private static class AIProcessorException extends Exception {
        final int errorCode;
        AIProcessorException(String message, int errorCode) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    private String cleanAIResponse(String content) {
        content = content.replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\'", "'")
            .replace("\\t", "\t");

        String[] unwantedPrefixes = {
            "好的，", "根据您提供的", "以下是", "作为助手，",
            "根据今天的记录，", "我来帮您整理", "好的，根据"
        };
        for (String prefix : unwantedPrefixes) {
            if (content.startsWith(prefix)) {
                content = content.substring(prefix.length()).trim();
                break;
            }
        }
        return content.trim();
    }

    private String formatDiaryContent(String content) {
        StringBuilder formatted = new StringBuilder();
        String[] paragraphs = content.split("\n\n");
        if (paragraphs.length > 1) {
            for (String para : paragraphs) {
                if (!para.trim().isEmpty()) {
                    formatted.append(para.trim()).append("\n\n");
                }
            }
        } else {
            String[] sentences = content.split("。");
            StringBuilder currentParagraph = new StringBuilder();
            for (int i = 0; i < sentences.length; i++) {
                String sentence = sentences[i].trim();
                if (!sentence.isEmpty()) {
                    currentParagraph.append(sentence);
                    if (i < sentences.length - 1) currentParagraph.append("。");
                    if ((i + 1) % 4 == 0 || i == sentences.length - 1) {
                        formatted.append(currentParagraph).append("\n\n");
                        currentParagraph = new StringBuilder();
                    }
                }
            }
        }
        return formatted.toString().trim();
    }

    public String getCurrentDate() {
        return new SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault()).format(new Date());
    }
}
