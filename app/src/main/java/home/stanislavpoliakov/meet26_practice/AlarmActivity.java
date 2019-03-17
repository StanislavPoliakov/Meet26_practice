package home.stanislavpoliakov.meet26_practice;

import android.app.DatePickerDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "meet26_logs";

    // Текущая временная точка (момент создания будильника)
    private Calendar timestamp;
    private TextView dateLabel;
    private boolean vibro, enabled;
    private String repeatString;
    //private int repeatIn;
    private MutableLiveData<Integer> repeatIn = new MutableLiveData<>();
    private DatePickerDialog datePickerDialog;

    private int defaultColor;
    private int pressedColor = Color.RED;
    private List<Button> buttons = new ArrayList<>();

    private String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

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
        repeatIn.setValue(0);

        // LiveData
        Observer<Calendar> observer = this::updateLabel;
        alarmDate.observe(this, observer);

        Observer<Integer> observerInt = this::setRepeat;
        repeatIn.observe(this, observerInt);

        // И еще одну временную точку - шаблон для будильника
        Calendar alarm = Calendar.getInstance();
        if (requestCode == MainActivity.UPDATE_REQUEST) {
            Intent intent = getIntent();

            alarm.setTimeInMillis(intent.getLongExtra("start", 0));
            //repeatIn = intent.getIntExtra("repeatIn", 0);
            repeatIn.setValue(intent.getIntExtra("repeatIn", 0));
            repeatString = intent.getStringExtra("repeatString");
            vibro = intent.getBooleanExtra("vibro", false);
            enabled = intent.getBooleanExtra("enabled", false);
        }
        alarmDate.setValue(alarm);



        dateLabel = findViewById(R.id.dateLabel);

        Switch vibroSwitch = findViewById(R.id.vibroSwitch);
        vibroSwitch.setChecked(vibro);
        vibroSwitch.setOnClickListener(v -> vibro = !vibro);

        initRepeatButtons();

        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(view -> {
            setResult(requestCode);
            finish();
        });

        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            Intent result = new Intent();
            result.putExtra("start", alarmDate.getValue().getTimeInMillis());
            result.putExtra("repeatIn", repeatIn.getValue());
            result.putExtra("vibro", vibro);
            result.putExtra("enabled", enabled);
            result.putExtra("repeatString", dateLabel.getText().toString());
            if (requestCode == MainActivity.UPDATE_REQUEST)
                result.putExtra("id", getIntent().getIntExtra("id", -1));
            setResult(requestCode, result);
            finish();
        });

        initPicker(requestCode);
    }

    private View.OnClickListener weekButtonListener = v -> {
        Button button = (Button) v;
        //Log.d(TAG, "default = " + defaultColor);
        int color = button.getTextColors().getDefaultColor();
        int sign = (color == pressedColor) ? 0 : 1;
        //Log.d(TAG, "pressed = " + pressedColor);

        button.setTextColor((color == pressedColor) ? defaultColor : pressedColor);
        String btnText = button.getText().toString();
        int offset = 6 - Arrays.asList(daysOfWeek).indexOf(btnText);
        Log.d(TAG, "offset = " + offset);

        int repeat = repeatIn.getValue();
        if (sign == 1) repeat |= (sign << offset);
        else {
            repeat = ~repeat;
            repeat |= (1 << offset);
            repeat = ~repeat;
        }
        repeatIn.setValue(repeat);
        //Log.d(TAG, "repeatIn = " + repeatIn);
    };

    private void initRepeatButtons() {
        Button mondayButton = findViewById(R.id.mondayButton);
        buttons.add(mondayButton);
        defaultColor = mondayButton.getTextColors().getDefaultColor();

        Button tuesdayButton = findViewById(R.id.tuesdayButton);
        buttons.add(tuesdayButton);
        Button wednesdayButton = findViewById(R.id.wednesdayButton);
        buttons.add(wednesdayButton);
        Button thursdayButton = findViewById(R.id.thursdayButton);
        buttons.add(thursdayButton);
        Button fridayButton = findViewById(R.id.fridayButton);
        buttons.add(fridayButton);
        Button saturdayButton = findViewById(R.id.saturdayButton);
        buttons.add(saturdayButton);
        Button sundayButton = findViewById(R.id.sundayButton);
        buttons.add(sundayButton);

        buttons.stream()
                .map(b -> {
                    if (repeatString.contains(b.getText().toString())) b.setTextColor(pressedColor);
                    return b;
                }).forEach(b -> b.setOnClickListener(weekButtonListener));

        /*mondayButton.setOnClickListener(weekButtonListener);
        tuesdayButton.setOnClickListener(weekButtonListener);
        wednesdayButton.setOnClickListener(weekButtonListener);
        thursdayButton.setOnClickListener(weekButtonListener);
        fridayButton.setOnClickListener(weekButtonListener);
        saturdayButton.setOnClickListener(weekButtonListener);
        sundayButton.setOnClickListener(weekButtonListener);*/
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
        if (repeatIn.getValue() == 0) dateLabel.setText(format.format(value.getTime()));
        /*else {
            String binary = Integer.toBinaryString(repeatIn.getValue());
            StringJoiner joiner = new StringJoiner(",");
            for (int i = 0; i < binary.length(); i++) {
                if (binary.charAt(i) == 1) joiner.add(daysOfWeek[i]);
            }
            dateLabel.setText(joiner.toString());
        }*/
    }

    private void setRepeat(int repeat) {
        if (repeat == 0) {
            updateLabel(alarmDate.getValue());
            return;
        }
        String binary = Integer.toBinaryString(repeat);
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < binary.length(); i++) {
            int offset = daysOfWeek.length - binary.length();
            if (binary.charAt(i) == '1') joiner.add(daysOfWeek[i + offset]);
            //Log.d(TAG, "setRepeat: day = " + daysOfWeek[i + offset]);
            //Log.d(TAG, "setRepeat: joiner = " + binary);
        }
        dateLabel.setText(joiner.toString());
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
