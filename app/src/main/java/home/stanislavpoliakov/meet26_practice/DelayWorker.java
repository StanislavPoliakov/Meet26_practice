package home.stanislavpoliakov.meet26_practice;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class DelayWorker extends Worker {
    private static final String TAG = "meet26_logs";
    private Context context;


    public DelayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String result = makeAlarm();
        if (result == null) return Result.failure();
        else if (result.isEmpty()) return Result.retry();
        else return Result.success();
    }

    private String makeAlarm() {
        WorkManager workManager = WorkManager.getInstance();
        WorkRequest alarmEvent;

        String tag = getTag();
        Data data = getInputData();

        if (data.getBoolean("isPeriodic", false)) {
            alarmEvent = new PeriodicWorkRequest.Builder(AlarmWorker.class, 7, TimeUnit.DAYS)
                    .addTag(tag)
                    .build();
        } else {
            alarmEvent = new OneTimeWorkRequest.Builder(AlarmWorker.class)
                    .addTag(tag)
                    .build();
        }
        workManager.enqueue(alarmEvent);

        return "OK";
    }

    private String getTag() {
        return this.getTags().stream()
                .filter(t -> t.contains("Alarm"))
                .findFirst().get();
    }
}
