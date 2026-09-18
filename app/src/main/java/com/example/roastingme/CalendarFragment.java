package com.example.roastingme;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarFragment extends Fragment {

    private static final String KEY_SELECTED_DATE = "selected_date";

    private long selectedDateMillis;

    public CalendarFragment() {
        super(R.layout.fragment_calendar);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 처음에는 오늘, 재생성 시에는 이전에 선택한 날짜
        selectedDateMillis = savedInstanceState == null
                ? System.currentTimeMillis()
                : savedInstanceState.getLong(
                KEY_SELECTED_DATE,
                System.currentTimeMillis()
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        CalendarView calendarView =
                view.findViewById(R.id.calendar_view);

        TextView selectedDateText =
                view.findViewById(R.id.tv_selected_date);

        updateTodayText(view);

        calendarView.setDate(selectedDateMillis, false, true);
        updateSelectedDateText(selectedDateText);

        calendarView.setOnDateChangeListener(
                (calendar, year, month, dayOfMonth) -> {

                    Calendar selected = Calendar.getInstance();
                    selected.clear();

                    // month는 0부터 시작하며 Calendar도 동일
                    selected.set(year, month, dayOfMonth);

                    selectedDateMillis = selected.getTimeInMillis();

                    updateSelectedDateText(selectedDateText);

                    // 이후 이 날짜의 일정을 Room에서 조회
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        // 다시 화면으로 돌아오면 오늘 날짜를 갱신.
        // 사용자가 선택한 날짜는 그대로 유지.
        View view = getView();

        if (view != null) {
            updateTodayText(view);
        }
    }

    private void updateTodayText(View view) {
        TextView todayText = view.findViewById(R.id.tv_today);

        SimpleDateFormat format = new SimpleDateFormat(
                "yyyy년 M월 d일 EEEE",
                Locale.KOREAN
        );

        todayText.setText("오늘은 " + format.format(new Date()));
    }

    private void updateSelectedDateText(TextView textView) {
        SimpleDateFormat format = new SimpleDateFormat(
                "M월 d일",
                Locale.KOREAN
        );

        String selectedDate = format.format(
                new Date(selectedDateMillis)
        );

        textView.setText(selectedDate + "의 일정");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(KEY_SELECTED_DATE, selectedDateMillis);
        super.onSaveInstanceState(outState);
    }
}