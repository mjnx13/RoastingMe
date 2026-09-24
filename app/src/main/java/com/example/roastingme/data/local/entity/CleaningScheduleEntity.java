package com.example.roastingme.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(
        tableName = "cleaning_schedules",
        indices = {@Index(value = {"userId", "scheduledAt"})} // 복합 인덱스 추가
)
public class CleaningScheduleEntity {

    @PrimaryKey
    @NonNull
    private final String id;

    @NonNull
    private final String userId;

    private String title;
    private String memo;
    private long scheduledAt;
    private boolean reminderEnabled;

    @Nullable
    private Long reminderAt; // null 허용을 위해 객체형 Long 사용

    private boolean completed;
    private long createdAt;
    private long updatedAt;

    // Room 생성자
    public CleaningScheduleEntity(
            @NonNull String id,
            @NonNull String userId,
            String title,
            String memo,
            long scheduledAt,
            boolean reminderEnabled,
            @Nullable Long reminderAt,
            boolean completed,
            long createdAt,
            long updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.memo = memo;
        this.scheduledAt = scheduledAt;
        this.reminderEnabled = reminderEnabled;
        this.reminderAt = reminderAt;
        this.completed = completed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // 신규 생성용 팩토리 메서드
    public static CleaningScheduleEntity createNew(
            @NonNull String userId,
            String title,
            String memo,
            long scheduledAt,
            boolean reminderEnabled,
            @Nullable Long reminderAt) {
        long now = System.currentTimeMillis();
        return new CleaningScheduleEntity(
                UUID.randomUUID().toString(),
                userId,
                title,
                memo,
                scheduledAt,
                reminderEnabled,
                reminderEnabled ? reminderAt : null, // 알림 미사용 시 null 보장
                false,
                now,
                now
        );
    }

    // Getters
    @NonNull
    public String getId() { return id; }

    @NonNull
    public String getUserId() { return userId; }

    public String getTitle() { return title; }
    public String getMemo() { return memo; }
    public long getScheduledAt() { return scheduledAt; }
    public boolean isReminderEnabled() { return reminderEnabled; }

    @Nullable
    public Long getReminderAt() { return reminderAt; }

    public boolean isCompleted() { return completed; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
}