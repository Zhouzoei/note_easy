package com.example.finalwork;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class MoodHeatmapAdapter extends BaseAdapter {
    private Context context;
    private List<NoteStatistics.MoodDay> moodDays;

    public MoodHeatmapAdapter(Context context, List<NoteStatistics.MoodDay> moodDays) {
        this.context = context;
        this.moodDays = moodDays;
    }

    @Override
    public int getCount() {
        return moodDays.size();
    }

    @Override
    public Object getItem(int position) {
        return moodDays.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_mood_day, parent, false);
        }

        TextView tvDay = convertView.findViewById(R.id.tvDay);
        NoteStatistics.MoodDay moodDay = moodDays.get(position);

        // 设置日期文本（只显示天数）
        String[] dateParts = moodDay.getDate().split("/");
        String dayText = dateParts.length > 1 ? dateParts[1] : moodDay.getDate();
        tvDay.setText(dayText);

        // 根据心情值设置背景色
        int backgroundColor = getMoodColor(moodDay.getMood());
        tvDay.setBackgroundColor(backgroundColor);

        // 设置文字颜色
        if (moodDay.getMood() == 0) {
            tvDay.setTextColor(Color.parseColor("#CCCCCC")); // 无记录
        } else {
            tvDay.setTextColor(Color.WHITE);
        }

        // 添加点击效果（可选）
        tvDay.setOnClickListener(v -> {
            // 可以添加点击显示详细信息的逻辑
            if (moodDay.getMood() > 0) {
                String message = String.format("%s: %s", moodDay.getDate(), moodDay.getLabel());
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    private int getMoodColor(int moodValue) {
        switch (moodValue) {
            case 4: // 兴奋
                return Color.parseColor("#FF6B9C");
            case 3: // 愉快
                return Color.parseColor("#4CAF50");
            case 2: // 平静
                return Color.parseColor("#2196F3");
            case 1: // 低落
                return Color.parseColor("#9E9E9E");
            default: // 无记录
                return Color.parseColor("#F5F5F5");
        }
    }

    public void updateData(List<NoteStatistics.MoodDay> newMoodDays) {
        this.moodDays = newMoodDays;
        notifyDataSetChanged();
    }
}