package com.example.roastingme.ui.calendar;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roastingme.R;
import com.example.roastingme.data.local.entity.CleaningScheduleEntity;
import com.example.roastingme.data.repository.ScheduleRepository;
import com.example.roastingme.network.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ScheduleEditActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHeaderTitle;
    private TextView tvSelectedDateInfo;
    private TextInputEditText etTitle;
    private MaterialButton btnSelectTime;
    private SwitchMaterial switchReminder;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;

    private ScheduleRepository repository;
    private TokenManager tokenManager;

    private String scheduleId;
    private boolean isEditMode = false;
    private boolean isDataLoaded = false;

    // 삭제 및 수정을 위해 기존 로드된 Entity 보관
    private CleaningScheduleEntity currentSchedule;
    private boolean existingIsCompleted = false;
    private long existingCreatedAt = System.currentTimeMillis();

    private final Calendar calendar = Calendar.getInstance();
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("a hh:mm", Locale.KOREA);
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("선택한 날짜: yyyy년 M월 d일 (E)", Locale.KOREA);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_edit);

        repository = new ScheduleRepository(getApplication());
        tokenManager = TokenManager.getInstance(this);

        initViews();
        parseIntentData();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        tvHeaderTitle = findViewById(R.id.tv_header_title);
        tvSelectedDateInfo = findViewById(R.id.tv_selected_date_info);
        etTitle = findViewById(R.id.et_title);
        btnSelectTime = findViewById(R.id.btn_select_time);
        switchReminder = findViewById(R.id.switch_reminder);
        btnSave = findViewById(R.id.btn_save);
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void parseIntentData() {
        scheduleId = getIntent().getStringExtra("EXTRA_SCHEDULE_ID");

        if (scheduleId != null && !scheduleId.trim().isEmpty()) {
            // [수정 모드]
            isEditMode = true;
            if (tvHeaderTitle != null) tvHeaderTitle.setText("일정 수정");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            loadScheduleData(scheduleId);
        } else {
            // [신규 등록 모드]
            isEditMode = false;
            if (tvHeaderTitle != null) tvHeaderTitle.setText("일정 등록");
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);

            long selectedDateMillis = getIntent().getLongExtra("EXTRA_SELECTED_DATE", System.currentTimeMillis());
            calendar.setTimeInMillis(selectedDateMillis);

            updateDateAndDisplay();
        }
    }

    // DB에서 기존 일정 데이터 로드 (Repository의 userId + scheduleId 방식에 맞춤)
    private void loadScheduleData(String id) {
        String userId = getUserId();
        repository.getScheduleById(userId, id).observe(this, schedule -> {
            if (schedule != null && !isDataLoaded) {
                isDataLoaded = true;
                currentSchedule = schedule; // 삭제/수정 시 활용할 엔티티 저장

                existingIsCompleted = schedule.isCompleted();
                existingCreatedAt = schedule.getCreatedAt();

                if (etTitle != null) etTitle.setText(schedule.getTitle());
                calendar.setTimeInMillis(schedule.getScheduledAt());
                if (switchReminder != null) switchReminder.setChecked(schedule.isReminderEnabled());

                updateDateAndDisplay();
            }
        });
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnSelectTime != null) {
            btnSelectTime.setOnClickListener(v -> {
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);

                TimePickerDialog dialog = new TimePickerDialog(this,
                        (view, hourOfDay, minuteOfHour) -> {
                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                            calendar.set(Calendar.MINUTE, minuteOfHour);
                            calendar.set(Calendar.SECOND, 0);
                            updateDateAndDisplay();
                        }, hour, minute, false);
                dialog.show();
            });
        }

        if (btnSave != null) {
            btnSave.setOnClickListener(v -> saveSchedule());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDeleteSchedule());
        }
    }

    private void updateDateAndDisplay() {
        if (btnSelectTime != null) {
            btnSelectTime.setText("시간 선택: " + timeFormatter.format(calendar.getTime()));
        }
        if (tvSelectedDateInfo != null) {
            tvSelectedDateInfo.setText(dateFormatter.format(calendar.getTime()));
        }
    }

    private String getUserId() {
        String userId = tokenManager.getUserId();
        return (userId != null && !userId.trim().isEmpty()) ? userId : "default_user";
    }

    // 일정 저장 / 수정 처리
    private void saveSchedule() {
        String title = etTitle != null && etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, "일정 제목을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonsEnabled(false);

        String userId = getUserId();
        long scheduledAt = calendar.getTimeInMillis();
        boolean isReminderEnabled = switchReminder != null && switchReminder.isChecked();
        Long reminderAt = isReminderEnabled ? scheduledAt : null;

        if (isEditMode) {
            // [수정] - 10개 파라미터 규격에 맞춰 기존 memo, completed, createdAt 전달
            String memo = (currentSchedule != null) ? currentSchedule.getMemo() : "";

            CleaningScheduleEntity entity = new CleaningScheduleEntity(
                    scheduleId,
                    userId,
                    title,
                    memo,                  // 1. memo 추가
                    scheduledAt,
                    isReminderEnabled,
                    reminderAt,            // 2. reminderAt 추가
                    existingIsCompleted,
                    existingCreatedAt,
                    System.currentTimeMillis()
            );

            repository.update(entity, new ScheduleRepository.RepositoryCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(ScheduleEditActivity.this, "일정이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        setButtonsEnabled(true);
                        Toast.makeText(ScheduleEditActivity.this, "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            // [신규 등록] - CleaningScheduleEntity의 createNew() 팩토리 메서드 활용
            CleaningScheduleEntity entity = CleaningScheduleEntity.createNew(
                    userId,
                    title,
                    "",                   // memo 기본값 (빈 문자열)
                    scheduledAt,
                    isReminderEnabled,
                    reminderAt
            );

            repository.insert(entity, new ScheduleRepository.RepositoryCallback() {
                @Override
                public void onSuccess() {
                    runOnUiThread(() -> {
                        Toast.makeText(ScheduleEditActivity.this, "일정이 등록되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        setButtonsEnabled(true);
                        Toast.makeText(ScheduleEditActivity.this, "등록 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    // 삭제 전 다이얼로그 확인
    private void confirmDeleteSchedule() {
        if (!isEditMode || currentSchedule == null) return;

        new AlertDialog.Builder(ScheduleEditActivity.this)
                .setTitle("일정 삭제")
                .setMessage("정말 이 일정을 삭제하시겠습니까?")
                .setPositiveButton("삭제", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        deleteSchedule();
                    }
                })
                .setNegativeButton("취소", null)
                .show();
    }

    // 일정 삭제 실행
    private void deleteSchedule() {
        if (currentSchedule == null) return;
        setButtonsEnabled(false);

        repository.delete(currentSchedule, new ScheduleRepository.RepositoryCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    Toast.makeText(ScheduleEditActivity.this, "일정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(ScheduleEditActivity.this, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnSave != null) btnSave.setEnabled(enabled);
        if (btnDelete != null) btnDelete.setEnabled(enabled);
    }
}