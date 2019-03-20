package home.stanislavpoliakov.meet26_practice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;


import android.support.v4.app.NotificationCompat;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.Duration;

import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmWorker extends Worker {
    private static final String TAG = "meet26_logs";
    private Context context;

    private NotificationManager notificationManager;
    private NotificationCompat.Builder notification;
    private int max = 100;
    private boolean isCanceled = false;
    private String tag;

    private final String CHANNEL_ID = "Channel_10";

    public AlarmWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;

        tag = getTag();

        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Удаляю канал перед созданием, но это все равно не помогает! Баг! Приходится менять
        // channel_id, чтобы применить новые настройки
        notificationManager.deleteNotificationChannel(CHANNEL_ID);
        notification = makeNotification();
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
     * Продлить звонок будильника, то есть повторить через 10 секунд
     */
    private void extendAlarm() {
        WorkManager workManager = WorkManager.getInstance();

        OneTimeWorkRequest nextTry = new OneTimeWorkRequest.Builder(AlarmWorker.class)
                .setInitialDelay(Duration.ofSeconds(10))
                .addTag(tag)
                .build();

        workManager.enqueue(nextTry);
    }

    /**
     * Получить tag задачи
     * @return
     */
    private String getTag() {
        return this.getTags().stream()
                .filter(t -> t.contains("Alarm"))
                .findFirst().get();
    }

    /**
     * Устанавливаем флаг конца работы задачи. Работает с isStopped() - через раз
     */
    @Override
    public void onStopped() {
        isCanceled = true;
    }

    /**
     * Метод запланированной работы
     * @return
     */
    private String alarmWork() {

        int i = 0;

        while (i < max && !isStopped()) {
            i += 10;
            notification.setProgress(100, i, false);
            notificationManager.notify(1, notification.build());
            //Log.d(TAG, "alarmWork: tag = " + tag);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        // Если в течение 20 секунд будильника он не был остановлен - автоматически продлеваем
        notificationManager.cancel(1);
        if (!isCanceled) extendAlarm();

        return "Alarm at work!";
    }

    /**
     * Делаем нотик в для отображения в момент работы будильника
     */
    private NotificationCompat.Builder makeNotification() {

        // API ≥ 26 - создаем Channel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            CharSequence name = "my_channel";

            boolean isVibro = getInputData().getBoolean("isVibro", false);

            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            mChannel.setDescription("description");
            mChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), new AudioAttributes.Builder().build());
            long[] vibrationPattern = {100, 100, 100, 100};
            if (isVibro) mChannel.setVibrationPattern(vibrationPattern);
            notificationManager.createNotificationChannel(mChannel);
        }

        Intent deleteIntent = new Intent(context, MyReceiver.class);
        deleteIntent.setAction("STOP");
        deleteIntent.putExtra("TAG", tag);
        //Log.d(TAG, "makeNotification: deleteIntent = " + deleteIntent);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Будильник")
                .setContentText("Вставайте, граф! Вас ждут великие дела!")

                // Показать заполняемый progressBar
                .setProgress(max, 0, false)

                // Автоматически закрыть через
                //.setTimeoutAfter(1000) НЕ РАБОТАЕТ, как и autoCancel
                .addAction(android.R.drawable.ic_menu_delete, "Остановить", deletePendingIntent);
    }
}
