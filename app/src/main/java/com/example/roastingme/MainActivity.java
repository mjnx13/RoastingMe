package com.example.roastingme;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.content.res.ColorStateList;
import android.graphics.Color;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView pageTitle = findViewById(R.id.tv_page_title);

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setItemRippleColor(null);
        bottomNavigation.setItemBackground(null);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                pageTitle.setText("RoastingMe 홈");
                return true;
            } else if (itemId == R.id.nav_calendar) {
                pageTitle.setText("청소 일지");
                return true;
            } else if (itemId == R.id.nav_newaction) {
                pageTitle.setText("청소 기록 생성");
                return true;
            } else if (itemId == R.id.nav_shopping) {
                pageTitle.setText("제품 추천");
                return true;
            } else if (itemId == R.id.nav_settinglist) {
                pageTitle.setText("설정");
                return true;
            }

            return false;
        });

// 처음에는 홈 선택, 재생성 시에는 이전 선택 복원
        int selectedTab = savedInstanceState == null
                ? R.id.nav_home
                : savedInstanceState.getInt("selected_tab", R.id.nav_home);

        bottomNavigation.setSelectedItemId(selectedTab);
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);

        outState.putInt(
                "selected_tab",
                bottomNavigation.getSelectedItemId()
        );

        super.onSaveInstanceState(outState);
    }
}