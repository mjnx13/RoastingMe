package com.example.roastingme.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roastingme.R;
import com.example.roastingme.data.local.entity.CleaningScheduleEntity;
import com.example.roastingme.network.TokenManager;
import com.example.roastingme.ui.schedule.ScheduleEditActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private static final String KEY_SELECTED_DATE = "selected_date";

    private CalendarViewModel viewModel;
    private ScheduleAdapter adapter;
    private TokenManager tokenManager;

    private View btnAddSchedule;
    private TextView tvSchedulePlaceholder;
    private RecyclerView rvSchedules;
    private TextView tvSelectedDate;

    private long selectedDateMillis;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        selectedDateMillis = savedInstanceState == null
                ? System.currentTimeMillis()
                : savedInstanceState.getLong(KEY_SELECTED_DATE, System.currentTimeMillis());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = TokenManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        CalendarView calendarView = view.findViewById(R.id.calendar_view);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvSchedulePlaceholder = view.findViewById(R.id.tv_schedule_placeholder);
        rvSchedules = view.findViewById(R.id.rv_schedules);
        btnAddSchedule = view.findViewById(R.id.btn_add_schedule);

        updateTodayText(view);
        updateSelectedDateText(tvSelectedDate);
        calendarView.setDate(selectedDateMillis, false, true);

        setupRecyclerView();
        setupAddButton();

        // 날짜 선택 시 해당 날짜의 Room 일정 자동 조회
        calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.clear();
            selected.set(year, month, dayOfMonth);

            selectedDateMillis = selected.getTimeInMillis();
            updateSelectedDateText(tvSelectedDate);
            viewModel.setSelectedDate(selectedDateMillis);
        });

        observeViewModel();
        viewModel.setSelectedDate(selectedDateMillis);
    }

    @Override
    public void onResume() {
        super.onResume();
        View view = getView();
        if (view != null) {
            updateTodayText(view);
        }
        viewModel.refreshUserSession();
        updateAddButtonState();
    }

    private void observeViewModel() {
        viewModel.getSchedules().observe(getViewLifecycleOwner(), schedules -> {
            if (schedules == null || schedules.isEmpty()) {
                tvSchedulePlaceholder.setVisibility(View.VISIBLE);
                rvSchedules.setVisibility(View.GONE);
            } else {
                tvSchedulePlaceholder.setVisibility(View.GONE);
                rvSchedules.setVisibility(View.VISIBLE);
                adapter.submitList(schedules);
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(new ScheduleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CleaningScheduleEntity schedule) {
                // 일정 항목 클릭 시 수정 화면으로 이동
                Intent intent = new Intent(requireContext(), ScheduleEditActivity.class);
                intent.putExtra("EXTRA_SCHEDULE_ID", schedule.getId());
                startActivity(intent);
            }

            @Override
            public void onToggleCompletion(CleaningScheduleEntity schedule) {
                viewModel.toggleCompletion(schedule);
            }
        });

        rvSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSchedules.setAdapter(adapter);
    }

    private void setupAddButton() {
        if (btnAddSchedule != null) {
            btnAddSchedule.setOnClickListener(v -> {
                if (!isUserLoggedIn()) {
                    Toast.makeText(requireContext(), "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 일정 등록 화면으로 이동
                Intent intent = new Intent(requireContext(), ScheduleEditActivity.class);
                intent.putExtra("EXTRA_SELECTED_DATE", selectedDateMillis);
                startActivity(intent);
            });
        }
    }

    private void updateAddButtonState() {
        if (btnAddSchedule != null) {
            boolean loggedIn = isUserLoggedIn();
            btnAddSchedule.setEnabled(true);
            btnAddSchedule.setAlpha(loggedIn ? 1.0f : 0.5f);
        }
    }

    // 방어적 로그인 확인 로직 (userId 외에 AccessToken/Token 존재 여부도 함께 판단)
    private boolean isUserLoggedIn() {
        if (tokenManager == null) return false;
        String userId = tokenManager.getUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            return true;
        }
        // getUserId가 null이더라도 AccessToken(또는 getToken)이 있다면 로그인된 상태로 판단
        String token = tokenManager.getAccessToken(); // project의 TokenManager 메서드명 사용
        return token != null && !token.trim().isEmpty();
    }

    private void updateTodayText(View view) {
        TextView todayText = view.findViewById(R.id.tv_today);
        if (todayText != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN);
            todayText.setText("오늘은 " + format.format(new Date()));
        }
    }

    private void updateSelectedDateText(TextView textView) {
        if (textView != null) {
            SimpleDateFormat format = new SimpleDateFormat("M월 d일", Locale.KOREAN);
            String selectedDate = format.format(new Date(selectedDateMillis));
            textView.setText(selectedDate + "의 일정");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(KEY_SELECTED_DATE, selectedDateMillis);
        super.onSaveInstanceState(outState);
    }
}