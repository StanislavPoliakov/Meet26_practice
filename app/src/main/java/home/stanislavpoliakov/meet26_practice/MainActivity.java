package home.stanislavpoliakov.meet26_practice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static home.stanislavpoliakov.meet26_practice.ConvertUtils.convertAlarmToValues;
import static home.stanislavpoliakov.meet26_practice.ConvertUtils.convertCursorToAlarmsSet;

public class MainActivity extends AppCompatActivity implements ICallback{
    private static final String TAG = "meet26_logs";
    private static final String AUTHORITY = "content_provider";
    private static final String ENTRIES_TABLE = "alarm_database";

    public static final int CREATE_REQUEST = 1;
    public static final int UPDATE_REQUEST = 2;

    private Alarm alarm;
    private TextView noAlarmsLabel;

    private ContentResolver contentResolver;
    private QueryHandler queryHandler;
    //private Alarm alarm0;
    //private Set<Alarm> alarmSet = new HashSet<>();

    private MutableLiveData<ArraySet<Alarm>> alarmSet = new MutableLiveData<>();

    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent createIntent = AlarmActivity.newIntent(this);
            createIntent.putExtra("requestCode", CREATE_REQUEST);
            startActivityForResult(AlarmActivity.newIntent(this), CREATE_REQUEST);
        });

        init();
    }

    private void updateRecycler(Set<Alarm> data) {
        mAdapter.setData(data);
    }

    private void init() {
        noAlarmsLabel = findViewById(R.id.noAlarmsLabel);

        contentResolver = getContentResolver();
        queryHandler = new QueryHandler(contentResolver);
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);
        //getContentResolver().query(CONTENT_URI, null, null, null, null);
        queryHandler.startQuery(1, null, CONTENT_URI, null,
                null, null, null);

        ArraySet<Alarm> initSet = new ArraySet<>();
        alarmSet.setValue(initSet);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mAdapter = new MyAdapter(this, alarmSet.getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(layoutManager);
        
        Observer<ArraySet<Alarm>> observer = this::updateRecycler;
        alarmSet.observe(this, observer);

        //Log.d(TAG, "init: ");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if ("Delete".equals(item.getTitle())) {
            /*int id = item.getItemId();
            alarm = alarmSet.getValue().stream()
                    .filter(e -> e.id == id)
                    .findFirst().orElse(null);*/
            int position = item.getItemId();
            //Log.d(TAG, "onContextItemSelected: pos = " + position);
            alarm = alarmSet.getValue().valueAt(position);
            Log.d(TAG, "onContextItemSelected: alarm.id = " + alarm.id);
            deleteAlarm(alarm);


        }

        return super.onContextItemSelected(item);
    }

    private void deleteAlarm(Alarm alarm) {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + alarm.id);
        queryHandler.startDelete(1, null, CONTENT_URI, null, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        //Log.d(TAG, "onActivityResult: ");
        if (data != null) {
            alarm = new Alarm(data.getLongExtra("start", 0), data.getStringExtra("repeatString"));
            if (requestCode == CREATE_REQUEST) {


                //alarm = new Alarm(data.getLongExtra("start", 0));

                //Log.d(TAG, "onActivityResult: size = " + alarmSet.size());
                if (!alarmSet.getValue().contains(alarm)) insertAlarm(alarm);

                // mAdapter.setData(alarmSet);

            } else if (resultCode == UPDATE_REQUEST) {
                alarm.id = data.getIntExtra("id", 0);
                alarm.setRepeatIn(data.getIntExtra("repeatIn", 0));
                alarm.setVibro(data.getBooleanExtra("vibro", false));
                alarm.setEnabled(data.getBooleanExtra("enabled", false));

                //PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L = 15 минут :)
                PeriodicWorkRequest alarmWorkRequest = new PeriodicWorkRequest.Builder(AlarmWorker.class, 1, TimeUnit.SECONDS)
                        .build();

                OneTimeWorkRequest alarmWorkRequestOneTime = new OneTimeWorkRequest.Builder(AlarmWorker.class)
                        //.setInitialDelay(5, TimeUnit.SECONDS)
                        .build();

                WorkManager.getInstance().cancelAllWork();
                //WorkManager.getInstance().enqueue(alarmWorkRequest);

                if (!alarmSet.getValue().contains(alarm)) updateAlarm(alarm);
                else {
                    Toast.makeText(this,
                            "Будильник с такими параметрами уже существует", Toast.LENGTH_SHORT).show();
                    alarm = null;
                }
            }
            /*NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "main")
                    .setContentTitle("New")
                    .setContentText("ALARM!")
                    .setSmallIcon(R.drawable.ic_launcher_background);
            Notification notification = mBuilder.build();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.notify(1, notification);*//*
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Title")
                            .setContentText("Notification text");

            Notification notification = builder.build();

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {


                String CHANNEL_ID = "my_channel_01";
                CharSequence name = "my_channel";
                String Description = "This is my channel";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mChannel.setDescription(Description);
                mChannel.enableLights(true);
                mChannel.setLightColor(Color.RED);
                mChannel.enableVibration(true);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                mChannel.setShowBadge(false);
                notificationManager.createNotificationChannel(mChannel);
            }


            notificationManager.notify(1, notification);*/
            //makeNotification();
        }
    }



    private void updateAlarm(Alarm alarm) {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + alarm.id);
        queryHandler.startUpdate(1, null, CONTENT_URI, convertAlarmToValues(alarm),
                null, null);
    }

    private void insertAlarm(Alarm alarm) {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/0");
        queryHandler.startInsert(1, null, CONTENT_URI, convertAlarmToValues(alarm));
    }

    private void checkCount() {
        noAlarmsLabel.setVisibility((alarmSet.getValue().size() == 0) ? View.VISIBLE : View.GONE);
    }

    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            //super.onQueryComplete(token, cookie, cursor);
            alarmSet.setValue(convertCursorToAlarmsSet(cursor));
            //if (alarmSet.getValue().size() > 0) noAlarmsLabel.setVisibility(View.GONE);
            checkCount();
        }

        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {
            //super.onInsertComplete(token, cookie, uri);
            String stringID = uri.getLastPathSegment();
            int id = Integer.parseInt(stringID);
            alarm.id = id;

           // Log.d(TAG, "onInsertComplete: id = " + alarm.id);

            ArraySet<Alarm> aSet = alarmSet.getValue();
            aSet.add(alarm);
            alarmSet.setValue(aSet);
            checkCount();
            alarm = null;
        }

        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {
            //super.onUpdateComplete(token, cookie, result);
            if (result > 0) {
                ArraySet<Alarm> aSet = alarmSet.getValue().stream()
                        .map(item -> item = (item.id == alarm.id) ? alarm : item)
                        .collect(Collectors.toCollection(ArraySet::new));
                alarmSet.setValue(aSet);
                alarm = null;
            }
        }

        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            //super.onDeleteComplete(token, cookie, result);
            if (result > 0) {
           // Log.d(TAG, "onDeleteComplete: result code = " + result);
                ArraySet<Alarm> aSet = alarmSet.getValue();
                aSet.remove(alarm);
                alarmSet.setValue(aSet);
                //if (alarmSet.getValue().size() == 0) noAlarmsLabel.setVisibility(View.VISIBLE);
                checkCount();
                alarm = null;
            }
        }
    }

    /*private void delete(Alarm alarm) {
        // Получаем id записи и формируем адрес записи в ContentProvider
        int id = entry.getId();
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + id);

        // Удаляем элемент базы данных. Реализация в ContentProvider
        getContentResolver().delete(CONTENT_URI, null, null);

        // Удаляем элемент в текущем слепке
        data.remove(entry);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void itemSelected(Alarm alarm) {
        Intent updateAlarm = AlarmActivity.newIntent(this);
        updateAlarm.putExtra("id", alarm.id)
                .putExtra("start", alarm.getStart().getTimeInMillis())
                .putExtra("repeatIn", alarm.getRepeatIn())
                .putExtra("repeatString", alarm.getRepeatString())
                .putExtra("vibro", alarm.isVibro())
                .putExtra("enabled", alarm.isEnabled())
                .putExtra("requestCode", UPDATE_REQUEST);
        startActivityForResult(updateAlarm, UPDATE_REQUEST);
    }

    @Override
    public void switchChange(int position) {
        alarm = alarmSet.getValue().valueAt(position);
        alarm.setEnabled(!alarm.isEnabled());
        updateAlarm(alarm);
        Log.d(TAG, "switchChange: ");
    }
}
