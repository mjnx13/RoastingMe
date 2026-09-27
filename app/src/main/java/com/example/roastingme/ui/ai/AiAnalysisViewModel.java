package com.example.roastingme.ui.ai;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.roastingme.ai.model.ImagePreprocessResult;
import com.example.roastingme.ai.model.ImageQualityResult;
import com.example.roastingme.ai.model.ImageQualityResult.IssueStatus;
import com.example.roastingme.ai.preprocess.ImagePreprocessUtils;
import com.example.roastingme.ai.quality.ImageQualityUtils;

import java.util.EnumSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AiAnalysisViewModel extends AndroidViewModel {

    // 단발성 이벤트 처리를 위한 Wrapper 클래스 (화면 회전 시 중복 팝업 방지)
    public static class Event<T> {
        private final T content;
        private boolean hasBeenHandled = false;

        public Event(T content) { this.content = content; }

        public T getContentIfNotHandled() {
            if (hasBeenHandled) return null;
            hasBeenHandled = true;
            return content;
        }

        public T peekContent() { return content; }
    }

    private final MutableLiveData<ImagePreprocessResult> preprocessResult = new MutableLiveData<>();
    private final MutableLiveData<Event<ImageQualityResult>> qualityResultEvent = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;

    public AiAnalysisViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ImagePreprocessResult> getPreprocessResult() { return preprocessResult; }
    public LiveData<Event<ImageQualityResult>> getQualityResultEvent() { return qualityResultEvent; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    /**
     * Context 파라미터 제거: getApplication()을 활용하여 MVVM 결합도 낮춤
     */
    public void processAndValidateImage(Uri imageUri) {
        if (imageUri == null) return;

        // 1. 이전 실행 중인 작업 취소 및 비트맵 메모리 정리
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        recyclePreviousBitmaps();

        // 2. 상태 초기화
        isLoading.setValue(true);
        preprocessResult.setValue(null);
        qualityResultEvent.setValue(null);

        // 3. 비동기 작업 제출
        currentTask = executorService.submit(() -> {
            try {
                // [1단계] 전처리 파이프라인
                ImagePreprocessResult pResult = ImagePreprocessUtils.processImage(getApplication(), imageUri);

                if (Thread.currentThread().isInterrupted()) return;
                preprocessResult.postValue(pResult);

                if (pResult != null && pResult.getAiInputBitmap() != null) {
                    // [2단계] 품질 검증
                    ImageQualityResult qResult = ImageQualityUtils.evaluateQuality(pResult.getAiInputBitmap());

                    if (Thread.currentThread().isInterrupted()) return;
                    qualityResultEvent.postValue(new Event<>(qResult));
                } else {
                    EnumSet<IssueStatus> issues = EnumSet.of(IssueStatus.PARTIAL_SPACE_ONLY);
                    ImageQualityResult failResult = new ImageQualityResult(
                            issues, 0, 0, 0, 1.0f, 1.0f, 1.0f, "이미지를 읽거나 변환하는 데 실패했습니다."
                    );
                    qualityResultEvent.postValue(new Event<>(failResult));
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 스레드가 취소되지 않고 정상 종료되었을 때만 로딩 플래그 해제
                if (!Thread.currentThread().isInterrupted()) {
                    isLoading.postValue(false);
                }
            }
        });
    }

    // 기존 비트맵 메모리 해제 메서드
    private void recyclePreviousBitmaps() {
        ImagePreprocessResult previous = preprocessResult.getValue();
        if (previous != null) {
            Bitmap orig = previous.getOriginalBitmap();
            Bitmap aiIn = previous.getAiInputBitmap();
            if (orig != null && !orig.isRecycled()) orig.recycle();
            if (aiIn != null && !aiIn.isRecycled()) aiIn.recycle();
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        recyclePreviousBitmaps();
        executorService.shutdown();
    }
}