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
import com.example.roastingme.ui.schedule.ScheduleEditActivity;
import com.example.roastingme.network.TokenManager;

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

    private long selectedDateMillis;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 처음에는 오늘, 재생성(화면 회전 등) 시에는 이전에 선택한 날짜 복구
        selectedDateMillis = savedInstanceState == null
                ? System.currentTimeMillis()
                : savedInstanceState.getLong(KEY_SELECTED_DATE, System.currentTimeMillis());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = TokenManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(CalendarViewModel.class);

        // 뷰 객체 바인딩
        CalendarView calendarView = view.findViewById(R.id.calendar_view);
        TextView tvSelectedDate = view.findViewById(R.id.tv_selected_date);
        tvSchedulePlaceholder = view.findViewById(R.id.tv_schedule_placeholder);
        rvSchedules = view.findViewById(R.id.rv_schedules);
        btnAddSchedule = view.findViewById(R.id.btn_add_schedule);

        // 1. 오늘 날짜 및 선택된 날짜 텍스트 초기화
        updateTodayText(view);
        updateSelectedDateText(tvSelectedDate);

        // 2. 캘린더 위치 동기화
        calendarView.setDate(selectedDateMillis, false, true);

        // 3. 리사이클러뷰 및 등록 버튼 설정
        setupRecyclerView();
        setupAddButton();

        // 4. CalendarView 날짜 변경 리스너
        calendarView.setOnDateChangeListener((calendar, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.clear();
            selected.set(year, month, dayOfMonth);

            selectedDateMillis = selected.getTimeInMillis();

            // 상단 텍스트 및 ViewModel DB 조회 요청 갱신
            updateSelectedDateText(tvSelectedDate);
            viewModel.setSelectedDate(selectedDateMillis);
        });

        // 5. LiveData 관찰
        observeViewModel();

        // 초기 선택 날짜로 ViewModel 데이터 요청
        viewModel.setSelectedDate(selectedDateMillis);
    }

    @Override
    public void onResume() {
        super.onResume();

        // 화면 복귀 시 오늘 날짜, 계정 세션, 등록 버튼 활성화 상태 갱신
        View view = getView();
        if (view != null) {
            updateTodayText(view);
        }
        viewModel.refreshUserSession();
        updateAddButtonState();
    }

    // 오늘 날짜 텍스트 갱신 ("오늘은 2026년 9월 24일 목요일")
    private void updateTodayText(View view) {
        TextView todayText = view.findViewById(R.id.tv_today);
        if (todayText != null) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy년 M월 d일 EEEE", Locale.KOREAN);
            todayText.setText("오늘은 " + format.format(new Date()));
        }
    }

    // 선택 날짜 텍스트 갱신 ("9월 24일의 일정")
    private void updateSelectedDateText(TextView textView) {
        if (textView != null) {
            SimpleDateFormat format = new SimpleDateFormat("M월 d일", Locale.KOREAN);
            String selectedDate = format.format(new Date(selectedDateMillis));
            textView.setText(selectedDate + "의 일정");
        }
    }

    // 리사이클러뷰 설정
    private void setupRecyclerView() {
        adapter = new ScheduleAdapter(new ScheduleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(CleaningScheduleEntity schedule) {
                // 수정 화면으로 이동 (scheduleId 전달)
                Intent intent = new Intent(requireContext(), ScheduleEditActivity.class);
                intent.putExtra("EXTRA_SCHEDULE_ID", schedule.getId());
                startActivity(intent);
            }

            @Override
            public void onToggleCompletion(CleaningScheduleEntity schedule) {
                // 완료 상태 토글
                viewModel.toggleCompletion(schedule);
            }
        });

        rvSchedules.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvSchedules.setAdapter(adapter);
    }

    // 일정 등록 버튼 설정
    private void setupAddButton() {
        if (btnAddSchedule != null) {
            btnAddSchedule.setOnClickListener(v -> {
                if (tokenManager.getUserId() == null) {
                    Toast.makeText(requireContext(), "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 등록 화면으로 이동 (선택 날짜 전달)
                Intent intent = new Intent(requireContext(), ScheduleEditActivity.class);
                intent.putExtra("EXTRA_SELECTED_DATE", selectedDateMillis);
                startActivity(intent);
            });
        }
    }

    // 로그인 여부에 따른 버튼 활성화
    private void updateAddButtonState() {
        if (btnAddSchedule != null) {
            boolean isLoggedIn = tokenManager.getUserId() != null;
            btnAddSchedule.setEnabled(isLoggedIn);
            btnAddSchedule.setAlpha(isLoggedIn ? 1.0f : 0.4f);
        }
    }

    // ViewModel LiveData 관찰
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(KEY_SELECTED_DATE, selectedDateMillis);
        super.onSaveInstanceState(outState);
    }
}