package com.example.roastingme.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.roastingme.data.local.dao.CleaningScheduleDao;
import com.example.roastingme.data.local.entity.CleaningScheduleEntity;

@Database(
        entities = {CleaningScheduleEntity.class},
        version = 1,
        exportSchema = true // (스키마 파일 관리 시 true 변경)
)
public abstract class AppDatabase extends RoomDatabase {

    // 멀티스레드 환경에서 메모리 동기화를 보장하기 위해 volatile 선언
    private static volatile AppDatabase INSTANCE;

    public abstract CleaningScheduleDao cleaningScheduleDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(), // Context 메모리 누수 방지
                            AppDatabase.class,
                            "roastingme.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}