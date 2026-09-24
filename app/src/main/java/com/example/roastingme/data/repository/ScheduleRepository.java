package com.example.roastingme.data.repository;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.example.roastingme.data.local.AppDatabase;
import com.example.roastingme.data.local.dao.CleaningScheduleDao;
import com.example.roastingme.data.local.entity.CleaningScheduleEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScheduleRepository {

    private final CleaningScheduleDao dao;
    // 공유 스레드풀 (스레드 중복 생성 방지)
    private static final ExecutorService executorService = Executors.newFixedThreadPool(4);

    // 작업 결과 콜백 인터페이스
    public interface RepositoryCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public ScheduleRepository(@NonNull Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.cleaningScheduleDao();
    }

    // 1. 날짜 범위 일정 조회 (LiveData)
    public LiveData<List<CleaningScheduleEntity>> getSchedulesByDate(String userId, long startMillis, long endMillis) {
        return dao.getSchedulesByDate(userId, startMillis, endMillis);
    }

    // 2. 단일 일정 조회 (수정 화면용)
    public LiveData<CleaningScheduleEntity> getScheduleById(String userId, String id) {
        return dao.getScheduleById(userId, id);
    }

    // 3. 일정 등록 (성공/실패 콜백 포함)
    public void insert(CleaningScheduleEntity schedule, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                dao.insert(schedule);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    // 4. 일정 수정
    public void update(CleaningScheduleEntity schedule, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                dao.update(schedule);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    // 5. 일정 삭제
    public void delete(CleaningScheduleEntity schedule, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                dao.delete(schedule);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    // 6. 완료 상태 변경 (토글)
    public void toggleCompletion(String userId, String id, boolean currentStatus, RepositoryCallback callback) {
        executorService.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                dao.updateCompletionStatus(userId, id, !currentStatus, now);
                if (callback != null) callback.onSuccess();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    // 7. 예약 알림 복구/등록용 (동기 조회)
    public void getUpcomingReminders(String userId, long currentTimeMillis, OnUpcomingRemindersLoadedListener listener) {
        executorService.execute(() -> {
            List<CleaningScheduleEntity> list = dao.getUpcomingReminders(userId, currentTimeMillis);
            if (listener != null) listener.onLoaded(list);
        });
    }

    public interface OnUpcomingRemindersLoadedListener {
        void onLoaded(List<CleaningScheduleEntity> schedules);
    }
}