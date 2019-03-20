package home.stanislavpoliakov.meet26_practice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.strictmode.WebViewMethodCalledOnWrongThreadViolation;
import android.util.Log;

import androidx.work.WorkManager;

public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "meet26_logs";

    @Override
    public void onReceive(Context context, Intent intent) {
        WorkManager workManager = WorkManager.getInstance();

        if ("STOP".equals(intent.getAction())) {
            String tag = intent.getStringExtra("TAG");
            workManager.cancelAllWork(); // Отправляю один String, а получаю внезапно другой!!!
            workManager.cancelAllWorkByTag(tag);
        }
    }
}
