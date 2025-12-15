package com.example.finalwork;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.*;

/**
 * 静态词云：词少随机撒点，词多螺旋外扩，矩形加 4dp 间隙防止视觉重叠
 */
public class WordCloudView extends View {

    /* ---------------- 数据 ---------------- */
    private final List<WordItem> items = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<String, Integer> colorMap = new HashMap<>();

    private int centerX, centerY;
    private float density;

    /* 字号区间（dp） */
    private static final int MIN_SP_DP = 20;
    private static final int MAX_SP_DP = 36;

    /* ---------------- 构造 ---------------- */
    public WordCloudView(Context c) { super(c); init(c); }
    public WordCloudView(Context c, AttributeSet a) { super(c, a); init(c); }
    public WordCloudView(Context c, AttributeSet a, int defStyleAttr) { super(c, a, defStyleAttr); init(c); }

    private void init(Context c) {
        density = c.getResources().getDisplayMetrics().density;
        paint.setTextAlign(Paint.Align.CENTER);
        initColorMap();
    }

    private void initColorMap() {
        colorMap.put("工作", Color.parseColor("#3F51B5"));
        colorMap.put("生活", Color.parseColor("#009688"));
        colorMap.put("学习", Color.parseColor("#FF5722"));
        colorMap.put("娱乐", Color.parseColor("#9C27B0"));
        colorMap.put("健康", Color.parseColor("#4CAF50"));
        colorMap.put("其他", Color.parseColor("#795548"));
    }

    /* ---------------- 对外 API ---------------- */
    public void setWords(Map<String, Integer> freq) {
        items.clear();
        if (freq == null || freq.isEmpty()) { invalidate(); return; }

        int max = Collections.max(freq.values());
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            float size = calcTextSize(e.getValue(), max);
            int color = colorMap.containsKey(e.getKey()) ?
                    colorMap.get(e.getKey()) : getDefaultColor(e.getKey());
            items.add(new WordItem(e.getKey(), e.getValue(), size, color));
        }
        items.sort((a, b) -> Float.compare(b.textSize, a.textSize));
        requestLayout();
    }

    /* ---------------- 尺寸变化 -> 布局 ---------------- */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        doLayout();
    }

    private void doLayout() {
        if (items.isEmpty() || getWidth() == 0) return;

        List<Rect> occupied = new ArrayList<>(items.size());

        // 对于中等数量词汇（3-10个）使用紧凑圆形布局
        if (items.size() >= 3 && items.size() <= 10) {
            compactCircleLayout(occupied);
        }
        // 词很少直接随机撒点
        else if (items.size() < 3) {
            randomScatter(items, occupied);
        }
        // 词多走螺旋
        else {
            spiralLayout(occupied);
        }

        invalidate();
    }

    /**
     * 紧凑圆形布局：六个词更紧凑地排列
     */
    private void compactCircleLayout(List<Rect> occupied) {
        int count = items.size();
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // 根据词汇数量决定圆形的半径 - 更小的半径使布局更紧凑
        float circleRadius;
        if (count == 6) {
            // 六个词特殊处理：更紧凑
            circleRadius = Math.min(viewWidth, viewHeight) * 0.25f; // 25% 更紧凑
        } else {
            circleRadius = Math.min(viewWidth, viewHeight) * 0.3f; // 正常紧凑
        }

        // 角度偏移，使第一个词从正上方开始
        float startAngle = (float) (Math.PI / 2);

        for (int i = 0; i < count; i++) {
            WordItem word = items.get(i);

            paint.setTextSize(word.textSize);
            int textWidth = (int) paint.measureText(word.text);
            int textHeight = (int) (word.textSize * 1.2f);

            // 添加间隙 - 减小间隙使更紧凑
            int gap = (int) (4 * density);
            int rectWidth = textWidth + gap;
            int rectHeight = textHeight + gap;

            // 计算角度（均匀分布在圆上）
            double angle = startAngle + (2 * Math.PI * i) / count;

            // 根据词的大小微调半径（大词稍远，小词稍近）
            float adjustedRadius = circleRadius;
            if (count == 6) {
                // 六个词的特殊布局：词云经典紧凑布局
                switch (i % 3) {
                    case 0: // 最大的几个词
                        adjustedRadius *= 1.0f;
                        break;
                    case 1: // 中等词
                        adjustedRadius *= 0.8f;
                        angle += Math.PI / 12; // 稍微偏移角度
                        break;
                    case 2: // 最小的词
                        adjustedRadius *= 0.6f;
                        angle -= Math.PI / 12; // 稍微偏移角度
                        break;
                }
            }

            // 计算位置
            int centerXPos = (int) (centerX + adjustedRadius * Math.cos(angle));
            int centerYPos = (int) (centerY + adjustedRadius * Math.sin(angle));

            Rect rect = new Rect(
                    centerXPos - rectWidth / 2,
                    centerYPos - rectHeight / 2,
                    centerXPos + rectWidth / 2,
                    centerYPos + rectHeight / 2
            );

            // 如果需要，微调位置避免重叠（但尽量保持紧凑）
            rect = adjustForOverlap(word, rect, occupied, i, 0);

            word.x = rect.centerX();
            word.y = rect.centerY() - (paint.ascent() + paint.descent()) / 2f;
            occupied.add(rect);
        }
    }

    /**
     * 调整矩形位置避免重叠（更宽松的检查，允许一定重叠）
     */
    private Rect adjustForOverlap(WordItem word, Rect originalRect, List<Rect> occupied,
                                  int wordIndex, int maxAttempts) {
        if (maxAttempts == 0) maxAttempts = 30; // 减少尝试次数，保持紧凑

        // 检查是否有显著重叠（不是轻微重叠）
        if (!hasSignificantOverlap(originalRect, occupied)) {
            return originalRect;
        }

        // 尝试轻微移动位置
        Random rnd = new Random(word.text.hashCode() + wordIndex);
        for (int i = 0; i < maxAttempts; i++) {
            int offsetX = rnd.nextInt(40) - 20; // 减小移动范围
            int offsetY = rnd.nextInt(40) - 20;

            Rect adjustedRect = new Rect(
                    originalRect.left + offsetX,
                    originalRect.top + offsetY,
                    originalRect.right + offsetX,
                    originalRect.bottom + offsetY
            );

            // 检查是否在边界内且没有显著重叠
            if (adjustedRect.left >= 0 && adjustedRect.top >= 0 &&
                    adjustedRect.right <= getWidth() && adjustedRect.bottom <= getHeight() &&
                    !hasSignificantOverlap(adjustedRect, occupied)) {
                return adjustedRect;
            }
        }

        // 如果找不到合适位置，返回原位置（允许轻微重叠）
        return originalRect;
    }

    /**
     * 检查是否有显著重叠（更宽松的重叠检测）
     */
    private boolean hasSignificantOverlap(Rect r, List<Rect> list) {
        if (list.isEmpty()) return false;

        // 只检查50%以上的重叠才认为是显著重叠
        for (Rect o : list) {
            if (Rect.intersects(r, o)) {
                // 计算重叠区域
                Rect intersection = new Rect();
                intersection.setIntersect(r, o);
                int overlapArea = intersection.width() * intersection.height();
                int rArea = r.width() * r.height();
                int oArea = o.width() * o.height();

                // 如果重叠面积超过较小矩形的30%，认为是显著重叠
                int minArea = Math.min(rArea, oArea);
                if (overlapArea > minArea * 0.3) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 螺旋布局
     */
    private void spiralLayout(List<Rect> occupied) {
        for (WordItem word : items) {
            Rect bound = findNonOverlapping(word, occupied);
            word.x = bound.centerX();
            word.y = bound.centerY() - (paint.ascent() + paint.descent()) / 2f;
            occupied.add(bound);
        }
    }

    /**
     * 螺旋外扩 + 间隙
     */
    private Rect findNonOverlapping(WordItem word, List<Rect> occupied) {
        paint.setTextSize(word.textSize);
        int textWidth = (int) paint.measureText(word.text);
        int textHeight = (int) (word.textSize * 1.3f);

        int gap = (int) (6 * density);
        int rectWidth = textWidth + gap;
        int rectHeight = textHeight + gap;

        double angle = 0;
        double radius = Math.max(rectWidth, rectHeight) * 0.5; // 更小的起始半径

        Random rnd = new Random(word.text.hashCode());

        for (int retry = 0; retry < 300; retry++) {
            int centerXPos = (int) (centerX + radius * Math.cos(angle));
            int centerYPos = (int) (centerY + radius * Math.sin(angle));

            Rect r = new Rect(
                    centerXPos - rectWidth / 2,
                    centerXPos - rectHeight / 2,
                    centerXPos + rectWidth / 2,
                    centerYPos + rectHeight / 2
            );

            // 检查是否出界
            if (r.left < 0 || r.top < 0 || r.right > getWidth() || r.bottom > getHeight()) {
                // 出界则继续螺旋
            } else if (!hasSignificantOverlap(r, occupied)) {
                // 成功找到位置，返回无间隙的矩形
                return new Rect(
                        centerXPos - (rectWidth - gap) / 2,
                        centerYPos - (rectHeight - gap) / 2,
                        centerXPos + (rectWidth - gap) / 2,
                        centerYPos + (rectHeight - gap) / 2
                );
            }

            angle += 0.2;
            if (angle > 2 * Math.PI) {
                angle = 0;
                radius += 8;
            }
        }

        // 兜底：放在中心
        return new Rect(
                centerX - (rectWidth - gap) / 2,
                centerY - (rectHeight - gap) / 2,
                centerX + (rectWidth - gap) / 2,
                centerY + (rectHeight - gap) / 2
        );
    }

    /* 随机撒点（词少时） */
    private void randomScatter(List<WordItem> list, List<Rect> occupied) {
        Random rnd = new Random();
        int w = getWidth();
        int h = getHeight();

        for (WordItem word : list) {
            paint.setTextSize(word.textSize);
            int textWidth = (int) paint.measureText(word.text);
            int textHeight = (int) (word.textSize * 1.2f);

            Rect rect;
            int retry = 0;
            do {
                int cx = rnd.nextInt(Math.max(1, w - textWidth * 2)) + textWidth;
                int cy = rnd.nextInt(Math.max(1, h - textHeight * 2)) + textHeight;
                rect = new Rect(cx - textWidth / 2, cy - textHeight / 2,
                        cx + textWidth / 2, cy + textHeight / 2);
                retry++;
            } while (hasSignificantOverlap(rect, occupied) && retry < 50);

            word.x = rect.centerX();
            word.y = rect.centerY() - (paint.ascent() + paint.descent()) / 2f;
            occupied.add(rect);
        }
    }

    /* ---------------- 绘制 ---------------- */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) {
            drawEmpty(canvas);
            return;
        }

        // 绘制背景
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        for (WordItem word : items) {
            paint.setTextSize(word.textSize);

            // 白色描边 - 更细的描边使文字更紧凑
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setStrokeWidth(3 * density);
            paint.setColor(Color.WHITE);
            canvas.drawText(word.text, word.x, word.y, paint);

            // 实心字
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0);
            paint.setColor(word.color);
            canvas.drawText(word.text, word.x, word.y, paint);
        }
    }

    private void drawEmpty(Canvas canvas) {
        paint.setColor(Color.parseColor("#999999"));
        paint.setTextSize(28 * density);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawText("暂无标签数据", centerX, centerY, paint);
    }

    /* ---------------- 工具 ---------------- */
    private float calcTextSize(int count, int max) {
        float ratio = (float) Math.sqrt((float) count / max);
        return (MIN_SP_DP + ratio * (MAX_SP_DP - MIN_SP_DP)) * density;
    }

    private int getDefaultColor(String word) {
        int[] pool = {
                Color.parseColor("#2196F3"), Color.parseColor("#FF9800"),
                Color.parseColor("#E91E63"), Color.parseColor("#673AB7"),
                Color.parseColor("#00BCD4"), Color.parseColor("#8BC34A"),
                Color.parseColor("#FFC107"), Color.parseColor("#607D8B")
        };
        return pool[Math.abs(word.hashCode()) % pool.length];
    }

    /* ---------------- 内部数据类 ---------------- */
    private static class WordItem {
        final String text;
        final int count;
        final float textSize;
        final int color;
        float x, y;

        WordItem(String t, int c, float s, int color) {
            this.text = t;
            this.count = c;
            this.textSize = s;
            this.color = color;
        }
    }

    /* ---------------- 动画空实现 ---------------- */
    public void startNewAnimation() { }
    public void stopAnimation() { }
}