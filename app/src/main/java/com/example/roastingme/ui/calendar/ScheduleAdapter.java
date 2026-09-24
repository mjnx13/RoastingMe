package com.example.roastingme.ui.calendar;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roastingme.R;
import com.example.roastingme.data.local.entity.CleaningScheduleEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScheduleAdapter extends ListAdapter<CleaningScheduleEntity, ScheduleAdapter.ScheduleViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(CleaningScheduleEntity schedule);
        void onToggleCompletion(CleaningScheduleEntity schedule);
    }

    private final OnItemClickListener listener;
    private final SimpleDateFormat timeFormatter = new SimpleDateFormat("a hh:mm", Locale.KOREA);

    public ScheduleAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // DiffUtil을 통한 리스트 변경 최적화
    private static final DiffUtil.ItemCallback<CleaningScheduleEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CleaningScheduleEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull CleaningScheduleEntity oldItem, @NonNull CleaningScheduleEntity newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CleaningScheduleEntity oldItem, @NonNull CleaningScheduleEntity newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                            oldItem.getScheduledAt() == newItem.getScheduledAt() &&
                            oldItem.isCompleted() == newItem.isCompleted() &&
                            oldItem.isReminderEnabled() == newItem.isReminderEnabled();
                }
            };

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        CleaningScheduleEntity schedule = getItem(position);
        holder.bind(schedule);
    }

    class ScheduleViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvTime;
        private final CheckBox cbCompleted;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_schedule.xml 의 뷰 ID와 맞춥니다.
            tvTitle = itemView.findViewById(R.id.tv_schedule_title);
            tvTime = itemView.findViewById(R.id.tv_schedule_time);
            cbCompleted = itemView.findViewById(R.id.cb_schedule_completed);

            // 1. 아이템 클릭 시 수정 화면 이동 콜백
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(getItem(pos));
                }
            });

            // 2. 체크박스 '사용자 직접 클릭' 시에만 완료 토글 호출 (재사용 버그 방지)
            cbCompleted.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onToggleCompletion(getItem(pos));
                }
            });
        }

        public void bind(CleaningScheduleEntity schedule) {
            tvTitle.setText(schedule.getTitle());
            tvTime.setText(timeFormatter.format(new Date(schedule.getScheduledAt())));

            // 체크박스 상태 바인딩
            cbCompleted.setChecked(schedule.isCompleted());

            // 완료 상태에 따른 시각적 스타일 처리 (재사용 버그 방지를 위해 둘 다 명시)
            if (schedule.isCompleted()) {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.darker_gray));
            } else {
                tvTitle.setPaintFlags(tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.black));
            }
        }
    }
}