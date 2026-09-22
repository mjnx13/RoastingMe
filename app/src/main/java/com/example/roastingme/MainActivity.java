package com.example.roastingme;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import com.example.roastingme.network.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private OnBackPressedCallback drawerBackCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupSettingsDrawer();

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottom_navigation);
        bottomNavigation.setItemActiveIndicatorEnabled(false);
        bottomNavigation.setItemRippleColor(null);
        bottomNavigation.setItemBackground(null);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return showMainTab("tab_home", HomeFragment.class);
            } else if (itemId == R.id.nav_calendar) {
                return showMainTab("tab_calendar", CalendarFragment.class);
            } else if (itemId == R.id.nav_newaction) {
                Toast.makeText(
                        MainActivity.this,
                        "출시 예정 기능!",
                        Toast.LENGTH_SHORT
                ).show();
                return false;
            } else if (itemId == R.id.nav_shopping) {
                return showMainTab("tab_shop", ShopFragment.class);
            } else if (itemId == R.id.nav_settinglist) {
                return showMainTab("tab_profile", ProfileFragment.class);
            }

            return false;
        });

        // 앱 최초 실행 시에는 nav_home 선택, 화면 재창조(화면 회전 등) 시에는 이전 선택 복원 (중복 제거)
        int selectedTab = (savedInstanceState == null)
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

    @Override
    protected void onPostResume() {
        super.onPostResume();

        if (drawerLayout != null && drawerBackCallback != null) {
            drawerBackCallback.setEnabled(
                    drawerLayout.isDrawerVisible(GravityCompat.END)
            );
        }
    }

    private void setupSettingsDrawer() {
        drawerLayout = findViewById(R.id.main);

        NavigationView settingsNavigation =
                findViewById(R.id.settings_navigation);

        // 다른 탭에서 가장자리 스와이프로 열리지 않도록 설정.
        // openDrawer()를 통한 버튼 열기는 가능.
        drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
        );

        drawerBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                drawerLayout.closeDrawer(GravityCompat.END);
            }
        };

        getOnBackPressedDispatcher().addCallback(
                this,
                drawerBackCallback
        );

        drawerLayout.addDrawerListener(
                new DrawerLayout.SimpleDrawerListener() {
                    @Override
                    public void onDrawerSlide(
                            @NonNull View drawerView,
                            float slideOffset
                    ) {
                        drawerBackCallback.setEnabled(slideOffset > 0f);
                    }

                    @Override
                    public void onDrawerOpened(
                            @NonNull View drawerView
                    ) {
                        drawerBackCallback.setEnabled(true);
                    }

                    @Override
                    public void onDrawerClosed(
                            @NonNull View drawerView
                    ) {
                        drawerBackCallback.setEnabled(false);
                    }
                }
        );

        settingsNavigation.setNavigationItemSelectedListener(item -> {
            drawerLayout.closeDrawer(GravityCompat.END);

            // 현재는 클릭 확인용. 이후 상세 Fragment로 연결.
            Toast.makeText(
                    this,
                    item.getTitle(),
                    Toast.LENGTH_SHORT
            ).show();

            return true;
        });
    }

    private boolean showMainTab(
            String tag,
            Class<? extends Fragment> fragmentClass
    ) {
        FragmentManager manager = getSupportFragmentManager();

        if (manager.isStateSaved()) {
            return false;
        }

        Fragment target = manager.findFragmentByTag(tag);

        FragmentTransaction transaction =
                manager.beginTransaction()
                        .setReorderingAllowed(true);

        // 현재 본문에 있는 기본 탭들을 숨김
        for (Fragment fragment : manager.getFragments()) {
            if (fragment.getId() == R.id.content_container
                    && fragment.isAdded()) {
                transaction.hide(fragment);
                transaction.setMaxLifecycle(
                        fragment,
                        Lifecycle.State.STARTED
                );
            }
        }

        if (target == null) {
            target = manager.getFragmentFactory().instantiate(
                    getClassLoader(),
                    fragmentClass.getName()
            );

            transaction.add(
                    R.id.content_container,
                    target,
                    tag
            );
        } else {
            transaction.show(target);
        }

        transaction.setMaxLifecycle(
                target,
                Lifecycle.State.RESUMED
        );

        transaction.setPrimaryNavigationFragment(target);

        // 기본 탭 이동은 뒤로가기 기록에 쌓지 않음
        transaction.commitNow();

        return true;
    }


    public void openSettingsDrawer() {
        drawerBackCallback.setEnabled(true);
        drawerLayout.openDrawer(GravityCompat.END);
    }
}