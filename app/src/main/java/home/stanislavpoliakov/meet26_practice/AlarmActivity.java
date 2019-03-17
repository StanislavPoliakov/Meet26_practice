package home.stanislavpoliakov.meet26_practice;

import android.app.DatePickerDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "meet26_logs";

    // Текущая временная точка (момент создания будильника)
    private Calendar timestamp;
    private TextView dateLabel;
    private boolean vibro, enabled;
    private int repeatIn;
    private DatePickerDialog datePickerDialog;

    // Время будильника, устанавливаемое в Activity, будем отслеживать и сразу отображать
    private MutableLiveData<Calendar> alarmDate = new MutableLiveData<>();

    //private DatePickerDialog datePickerDialog;

    /**
     * Обрабатываем выбор даты будильника во всплывающем окне
     */
    private DatePickerDialog.OnDateSetListener dateSetListener = ((datePicker, year, month, day) -> {
        Calendar calendar = alarmDate.getValue();
        calendar.set(year, month, day);
        alarmDate.setValue(calendar);
    });

    public static Intent newIntent(Context context) {
        return new Intent(context, AlarmActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        Intent intent = getIntent();
        /*switch (intent.getIntExtra("requestCode", 0)) {
            case MainActivity.CREATE_REQUEST :
                defaultInit();
                break;
            case MainActivity.UPDATE_REQUEST :
                loadAlarm();
                break;
        }*/
        initialize(intent.getIntExtra("requestCode", 0));
    }

    private void initialize(int requestCode) {
        //Intent intent = getIntent();

        // Получаем текущую временную точку
        timestamp = Calendar.getInstance();

        // И еще одну временную точку - шаблон для будильника
        Calendar alarm = Calendar.getInstance();
        if (requestCode == MainActivity.UPDATE_REQUEST) {
            Intent intent = getIntent();

            alarm.setTimeInMillis(intent.getLongExtra("start", 0));
            repeatIn = intent.getIntExtra("repeatIn", 0);
            vibro = intent.getBooleanExtra("vibro", false);
            enabled = intent.getBooleanExtra("enabled", false);
        }
        alarmDate.setValue(alarm);

        // LiveData
        Observer<Calendar> observer = this::updateLabel;
        alarmDate.observe(this, observer);

        dateLabel = findViewById(R.id.dateLabel);

        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(view -> {
            setResult(requestCode);
            finish();
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            Intent result = new Intent();
            result.putExtra("start", alarmDate.getValue().getTimeInMillis());
            result.putExtra("repeatIn", repeatIn);
            result.putExtra("vibro", vibro);
            result.putExtra("enabled", enabled);
            if (requestCode == MainActivity.UPDATE_REQUEST)
                result.putExtra("id", getIntent().getIntExtra("id", -1));
            setResult(requestCode, result);
            finish();
        });

        initPicker(requestCode);
    }

    /**
     * Обновляем Label настройками будильника
     * @param value настройки будильника в формате Calendar
     */
    private void updateLabel(Calendar value) {

        // Получаем русскую локализацию, чтобы видеть названия месяцев на Русском языке
        Locale ruLocale = new Locale.Builder()
                .setLanguage("RU")
                .build();

        // Форматируем дату в соответствии с нужным паттерном
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM в HH:mm", ruLocale);

        dateLabel.setText(format.format(value.getTime()));
    }

    /**
     * Метод инициализации выбора времени. SpinnerMode установлен в XML
     */
    private void initPicker(int requestCode) {
        TimePicker timePicker = findViewById(R.id.timePicker);

        // Формат времени устанавливаем на 24h
        timePicker.setIs24HourView(true);

        // Обрабатываем выбор времени
        timePicker.setOnTimeChangedListener((picker, hour, minute) -> {
            Calendar calendar = alarmDate.getValue();

            // Calendar.HOUR_OF_DAY = 24h, Calendar.HOUR = 12h
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            alarmDate.setValue(calendar);
        });

        // Устанавливаем начальное значение на 06:00
        setTimePicker(timePicker, requestCode);
        //timePicker.setHour((requestCode == MainActivity.UPDATE_REQUEST) ? );
        //timePicker.setMinute(0);

        initDatePicker(requestCode);

        Button dateButton = findViewById(R.id.dateButton);

        // Обрабатываем нажатие на кнопку выбора даты
        dateButton.setOnClickListener(view -> datePickerDialog.show());
    }

    private void setTimePicker(TimePicker timePicker, int requestCode) {
        int hour = 6;
        int minute = 0;

        if (requestCode == MainActivity.UPDATE_REQUEST) {
            long timeInMillis = getIntent().getLongExtra("start", 0);
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(timeInMillis);

            hour = moment.get(Calendar.HOUR_OF_DAY);
            minute = moment.get(Calendar.MINUTE);
        }

        timePicker.setHour(hour);
        timePicker.setMinute(minute);
    }

    private void initDatePicker(int requestCode) {
        // Получаем значения года, месяца и дня месяца для текущего временного момента...
        int year = timestamp.get(Calendar.YEAR);
        int month = timestamp.get(Calendar.MONTH);

        // Calendar.DAY_OF_MONTH == Calendar.DATE
        int day = timestamp.get(Calendar.DAY_OF_MONTH);

        if (requestCode == MainActivity.UPDATE_REQUEST) {
            long timeInMillis = getIntent().getLongExtra("start", 0);
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(timeInMillis);

            year = moment.get(Calendar.YEAR);
            month = moment.get(Calendar.MONTH);
            day = moment.get(Calendar.DAY_OF_MONTH);
        }

        // И устанавливаем дату в календаре (из которого производится выбор даты) в текущий момент
        datePickerDialog = new DatePickerDialog(this, dateSetListener,
                year, month, day);

        // Текущий момент времени - это минимальная доступная точка
        datePickerDialog.getDatePicker().setMinDate(timestamp.getTimeInMillis());
    }

}
