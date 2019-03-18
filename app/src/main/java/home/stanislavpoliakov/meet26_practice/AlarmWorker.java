package home.stanislavpoliakov.meet26_practice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmWorker extends Worker {
    private static final String TAG = "meet26_logs";
    private Context context;

    public AlarmWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        String result = alarmWork();
        if (result == null) return Result.failure();
        else if (result.isEmpty()) return Result.retry();
        else return Result.success();
    }

    /**
     * Метод запланированной работы
     * @return
     */
    private String alarmWork() {
        //Log.d(TAG, "alarmWork: ALARM AT WORK!!!");
        makeNotification();
        return "Alarm at work!";
    }

    /**
     * Делаем нотик в для отображения в момент работы будильника
     */
    private void makeNotification() {
        int NOTIFICATION_ID = 1;

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        String CHANNEL_ID = "1";

        // API > 26 - создаем Channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            CHANNEL_ID = "my_channel_01";
            CharSequence name = "my_channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Будильник")
                .setContentText("Вставайте, граф! Вас ждут великие дела!");

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
