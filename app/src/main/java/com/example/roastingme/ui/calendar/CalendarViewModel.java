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

        currentUserId.setValue(getEffectiveUserId());

        schedules = Transformations.switchMap(selectedDate, dateMillis -> {
            String userId = currentUserId.getValue();

            if (userId == null || userId.trim().isEmpty() || dateMillis == null) {
                MutableLiveData<List<CleaningScheduleEntity>> emptyList = new MutableLiveData<>();
                emptyList.setValue(Collections.emptyList());
                return emptyList;
            }

            long startMillis = getStartOfDay(dateMillis);
            long endMillis = getEndOfDay(dateMillis);

            return repository.getSchedulesByDate(userId, startMillis, endMillis);
        });
    }

    public void setSelectedDate(long dateMillis) {
        selectedDate.setValue(dateMillis);
    }

    public void refreshUserSession() {
        String newUserId = getEffectiveUserId();
        currentUserId.setValue(newUserId);

        Long currentDate = selectedDate.getValue();
        if (currentDate != null) {
            selectedDate.setValue(currentDate);
        }
    }

    // userId가 null이어도 Token이 존재하면 안전한 fallback ID 반환
    private String getEffectiveUserId() {
        if (tokenManager == null) return null;

        String userId = tokenManager.getUserId();
        if (userId != null && !userId.trim().isEmpty()) {
            return userId;
        }

        String token = tokenManager.getAccessToken(); // TokenManager 내 토큰 가져오기 메서드
        if (token != null && !token.trim().isEmpty()) {
            return "default_user";
        }

        return null;
    }

    public LiveData<List<CleaningScheduleEntity>> getSchedules() {
        return schedules;
    }

    public void toggleCompletion(CleaningScheduleEntity schedule) {
        if (schedule == null) return;
        repository.toggleCompletion(
                schedule.getUserId(),
                schedule.getId(),
                schedule.isCompleted(),
                null
        );
    }

    private long getStartOfDay(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

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