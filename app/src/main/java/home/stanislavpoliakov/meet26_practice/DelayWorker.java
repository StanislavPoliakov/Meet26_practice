package home.stanislavpoliakov.meet26_practice;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class DelayWorker extends Worker {
    private static final String TAG = "meet26_logs";


    public DelayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String result = makeAlarm();
        if (result == null) return Result.failure();
        else if (result.isEmpty()) return Result.retry();
        else return Result.success();
    }

    /**
     * Метод осовной работы "задачи ожидания"
     * Создаем повторяющуюся задачу и активируем ее
     * @return результат работы
     */
    private String makeAlarm() {
        WorkManager workManager = WorkManager.getInstance();
        WorkRequest alarmEvent;

        // Получаем установленный для текущей задачи тэг, чтобы его же установить в новую задачу
        String tag = getTag();

        // Создаем новую задачу
        alarmEvent = new PeriodicWorkRequest.Builder(AlarmWorker.class, 7, TimeUnit.DAYS)
                .addTag(tag)
                .build();

        // Активируем
        workManager.enqueue(alarmEvent);

        return "OK";
    }

    /**
     * Метод получения тэга будильника из множества тэгов.
     * Знаю, что тэг можно передавать в inputData, просто хотелось достать из сета
     *
     * @return тэг
     */
    private String getTag() {
        return this.getTags().stream()
                .filter(t -> t.contains("Alarm"))
                .findFirst().get();
    }
}
