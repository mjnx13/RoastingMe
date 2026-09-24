package com.example.roastingme.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.roastingme.data.local.entity.CleaningScheduleEntity;

import java.util.List;

@Dao
public interface CleaningScheduleDao {

    // 1. 일정 등록 (이미 존재하면 덮어쓰기)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CleaningScheduleEntity schedule);

    // 2. 선택한 날짜의 일정 목록 조회 (LiveData로 UI 자동 갱신)
    @Query("SELECT * FROM cleaning_schedules " +
            "WHERE userId = :userId AND scheduledAt >= :startMillis AND scheduledAt < :endMillis " +
            "ORDER BY scheduledAt ASC")
    LiveData<List<CleaningScheduleEntity>> getSchedulesByDate(String userId, long startMillis, long endMillis);

    // 3. 특정 일정 한 건 조회 (수정 화면용)
    @Query("SELECT * FROM cleaning_schedules WHERE userId = :userId AND id = :id LIMIT 1")
    LiveData<CleaningScheduleEntity> getScheduleById(String userId, String id);

    // 4. 일정 전체 수정
    @Update
    void update(CleaningScheduleEntity schedule);

    // 5. 일정 삭제 (Entity 객체 기준)
    @Delete
    void delete(CleaningScheduleEntity schedule);

    // 5-1. 일정 삭제 (ID 기준 단독 삭제)
    @Query("DELETE FROM cleaning_schedules WHERE id = :id AND userId = :userId")
    void deleteById(String userId, String id);

    // 6. 완료 상태만 변경 (부분 업데이트)
    @Query("UPDATE cleaning_schedules SET completed = :completed, updatedAt = :updatedAt WHERE id = :id AND userId = :userId")
    void updateCompletionStatus(String userId, String id, boolean completed, long updatedAt);

    // 7. 앞으로 울릴 알림 조회 (AlarmManager 예약 및 재부팅 수신용, 동기 실행)
    @Query("SELECT * FROM cleaning_schedules " +
            "WHERE userId = :userId AND reminderEnabled = 1 AND completed = 0 " +
            "AND reminderAt IS NOT NULL AND reminderAt > :currentTimeMillis " +
            "ORDER BY reminderAt ASC")
    List<CleaningScheduleEntity> getUpcomingReminders(String userId, long currentTimeMillis);
}