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

    private String alarmWork() {
        //Toast.makeText(context, "БУДИЛЬНИК!!!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "alarmWork: ALARM AT WORK!!!");
        makeNotification();
        return "Alarm at work!";
    }

    private void makeNotification() {
        int NOTIFICATION_ID = 234;

        Context ctx = getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        String CHANNEL_ID = "1";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            CHANNEL_ID = "my_channel_01";
            CharSequence name = "my_channel";
            //String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            //mChannel.setDescription(Description);
            /*mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);*/
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("title")
                .setContentText("message");

        /*Intent resultIntent = new Intent(ctx, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(ctx);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);*/

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
