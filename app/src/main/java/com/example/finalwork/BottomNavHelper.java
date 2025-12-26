package com.example.finalwork;

import android.app.Activity;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BottomNavHelper {

    public static void setSelectedTab(Activity activity, int index) {
        resetTabStyles(activity);

        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};
        int[] iconIds = {R.id.icon_record, R.id.icon_organize, R.id.icon_stats, R.id.icon_profile};
        int[] textIds = {R.id.text_record, R.id.text_organize, R.id.text_stats, R.id.text_profile};
        // 预定义文字内容，确保显示正确
        String[] textStrings = {"记录", "整理", "统计", "个人"};

        if (index >= 0 && index < navIds.length) {
            LinearLayout selectedTab = activity.findViewById(navIds[index]);
            ImageView selectedIcon = activity.findViewById(iconIds[index]);
            TextView selectedText = activity.findViewById(textIds[index]);

            if (selectedTab != null) {
                selectedTab.setBackgroundColor(Color.parseColor("#E3F2FD"));
            }
            if (selectedIcon != null) {
                selectedIcon.setColorFilter(Color.parseColor("#2196F3"), android.graphics.PorterDuff.Mode.SRC_IN);
                setIconSize(selectedIcon, 24);
            }
            if (selectedText != null) {
                // 强制显示文字
                selectedText.setVisibility(View.VISIBLE);
                selectedText.setTextColor(Color.parseColor("#2196F3"));
                // 设置文字内容，防止XML里丢失
                selectedText.setText(textStrings[index]);
            }
        }
    }

    private static void resetTabStyles(Activity activity) {
        int[] navIds = {R.id.nav_record, R.id.nav_organize, R.id.nav_stats, R.id.nav_profile};
        int[] iconIds = {R.id.icon_record, R.id.icon_organize, R.id.icon_stats, R.id.icon_profile};
        int[] textIds = {R.id.text_record, R.id.text_organize, R.id.text_stats, R.id.text_profile};

        for (int i = 0; i < 4; i++) {
            LinearLayout tab = activity.findViewById(navIds[i]);
            ImageView icon = activity.findViewById(iconIds[i]);
            TextView text = activity.findViewById(textIds[i]);

            if (tab != null) {
                tab.setBackgroundColor(Color.TRANSPARENT);
            }
            if (icon != null) {
                icon.setColorFilter(Color.parseColor("#999999"), android.graphics.PorterDuff.Mode.SRC_IN);
                setIconSize(icon, 32);
            }
            if (text != null) {
                text.setVisibility(View.GONE);
                text.setTextColor(Color.parseColor("#999999"));
            }
        }
    }

    private static void setIconSize(ImageView icon, int sizeDp) {
        int sizePx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                sizeDp,
                icon.getResources().getDisplayMetrics()
        );

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizePx, sizePx);
        icon.setLayoutParams(params);
    }
}
