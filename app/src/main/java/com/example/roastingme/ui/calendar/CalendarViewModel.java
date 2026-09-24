package com.example.roastingme.ui.calendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.roastingme.data.local.entity.CleaningScheduleEntity;
import com.example.roastingme.data.repository.ScheduleRepository;
import com.example.roastingme.network.TokenManager;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CalendarViewModel extends AndroidViewModel {

    private final ScheduleRepository repository;
    private final TokenManager tokenManager;

    private final MutableLiveData<Long> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<String> currentUserId = new MutableLiveData<>();

    private final LiveData<List<CleaningScheduleEntity>> schedules;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ScheduleRepository(application);
        this.tokenManager = TokenManager.getInstance(application);

        // 1. 초기값 설정 (오늘 날짜 및 현재 로그인 유저 ID)
        currentUserId.setValue(tokenManager.getUserId());
        selectedDate.setValue(System.currentTimeMillis());

        // 2. selectedDate가 변경될 때마다 DB 쿼리 자동 재연결
        schedules = Transformations.switchMap(selectedDate, dateMillis -> {
            String userId = currentUserId.getValue();

            // userId가 null이거나 날짜가 없으면 빈 목록 반환 (NPE 방지)
            if (userId == null || userId.isEmpty() || dateMillis == null) {
                MutableLiveData<List<CleaningScheduleEntity>> emptyList = new MutableLiveData<>();
                emptyList.setValue(Collections.emptyList());
                return emptyList;
            }

            long startMillis = getStartOfDay(dateMillis);
            long endMillis = getEndOfDay(dateMillis);

            return repository.getSchedulesByDate(userId, startMillis, endMillis);
        });
    }

    // 날짜 선택 시 호출
    public void setSelectedDate(long dateMillis) {
        selectedDate.setValue(dateMillis);
    }

    // 계정 전환 또는 로그인/로그아웃 시 유저 세션 갱신
    public void refreshUserSession() {
        String newUserId = tokenManager.getUserId();
        if (!Objects.equals(currentUserId.getValue(), newUserId)) {
            currentUserId.setValue(newUserId);
            // 날짜 변경 이벤트를 재발행하여 switchMap 트리거
            Long currentDate = selectedDate.getValue();
            if (currentDate != null) {
                selectedDate.setValue(currentDate);
            }
        }
    }

    public LiveData<List<CleaningScheduleEntity>> getSchedules() {
        return schedules;
    }

    // 완료 상태 토글 (UI 체크박스 클릭 대응)
    public void toggleCompletion(CleaningScheduleEntity schedule) {
        if (schedule == null) return;
        repository.toggleCompletion(
                schedule.getUserId(),
                schedule.getId(),
                schedule.isCompleted(),
                null
        );
    }

    // 하루의 시작 시각 (00:00:00.000) 계산
    private long getStartOfDay(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // 다음 날의 시작 시각 (00:00:00.000) 계산
    private long getEndOfDay(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }
}