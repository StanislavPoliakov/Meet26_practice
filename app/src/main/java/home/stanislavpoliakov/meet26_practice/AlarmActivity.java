package home.stanislavpoliakov.meet26_practice;

import android.app.DatePickerDialog;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "meet26_logs";

    // Текущая временная точка (момент создания будильника)
    private Calendar timestamp;

    // Метка отображения информации об установленной дате будильника или схеме повторений
    private TextView dateLabel;

    private boolean vibro, enabled;
    private String repeatString;

    // Паттер повторений, устанавливаемый в Activity, будем отслеживать и сразу отображать
    private MutableLiveData<Integer> repeatIn = new MutableLiveData<>();

    // Время будильника, устанавливаемое в Activity, будем отслеживать и сразу отображать
    // Приоритет отображения у паттерна повторений. Если паттерн пуст - отображаем время (и дату) будильника
    private MutableLiveData<Calendar> alarmDate = new MutableLiveData<>();

    private DatePickerDialog datePickerDialog;
    private List<Button> buttons = new ArrayList<>();

    //TODO значения в этом массиве и тексты кнопок должны быть значениями R.Strings для интернационализации
    private String[] daysOfWeek = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    // Текущий цвет кнопки (ненажатой, инициализируется один раз)
    private int defaultColor;

    // Цвет нажатой кнопки
    private int pressedColor = Color.RED;

    /**
     * Определяем Listener для кнопок паттерна повторений
     */
    private View.OnClickListener weekButtonListener = v -> {

        // Мы знаем, что view = button, поэтому кастим
        Button button = (Button) v;

        // Получаем текущий цвет кнопки, то есть тот цвет, в который окрашен текст кнопки в момент
        // нажатия на нее. Если кнопка включена - красный, выключена - цвет по умолчанию
        int color = button.getTextColors().getDefaultColor();

        // Поскольку мы используем битовую маску для установки значений паттерна, значит мы должны
        // знать что делать при нажатии на кнопку. Например, если кнопка была включена (color == pressedColor),
        // то мы при нажатии будем снимать значение (то есть устанавливать в значащем бите "0"), а если
        // была выключена - значит, нажав на выключенную кнопку, мы включим ее :) (КЭП здесь) и будем
        // устанавливать "1" в значащий бит
        int sign = (color == pressedColor) ? 0 : 1;

        // Устанавливаем цвет текста кнопки (фактически, инвертируем в рамках двух значений)
        button.setTextColor((color == pressedColor) ? defaultColor : pressedColor);

        // Получаем значение текста кнопки
        String btnText = button.getText().toString();

        // Объясню откуда здесь цифра "6". Значений у нас в массиве - 7, по одному для каждого дня
        // недели. Но дело в том, что единицу ("1") никуда сдвигать не надо - она уже в первом разряде,
        // а сдвиг единицы на один разряд (1 << 1) = 10, то есть необходимых свдигов нужно сделать
        // всего daysOfWeek.length - 1 = 6
        // Поскольку понедельник "Пн" в паттерне соответсвует старшему разряду, а в массиве значений -
        // первому элементу (index = 0), значит сдвиг нужно сделать на 6 - 0 = 6 разрядов
        int offset = 6 - Arrays.asList(daysOfWeek).indexOf(btnText);

        // Получаем текущее значение паттерна повторений
        int repeat = repeatIn.getValue();

        // Если нужно "включить" бит, то есть установить значащий бит в "1", то делаем логическое "ИЛИ"
        // между текущим паттерном и результатом сдвига (например, 100 | 010 = 110). Результат присваем паттерну.
        if (sign == 1) repeat |= (sign << offset);

        // Если же необходимо "сбросить" бит, то есть устанвовить значащий бит в "0", то необходимо
        // инвертировать паттерн (потому что установка значений в битовых операциях проходит по "1",
        // а не по "0", значит все единицы нужно сделать нулями, а нули - единицами, то есть инвертировать),
        // затем установить значащий бит в "инвертированный ноль" (то есть в "1"), и инвертировать
        // результат обратно
        else {
            repeat = ~repeat;
            repeat |= (1 << offset);
            repeat = ~repeat;
        }
        repeatIn.setValue(repeat);
    };

    /**
     * Обрабатываем выбор даты будильника во всплывающем окне
     */
    private DatePickerDialog.OnDateSetListener dateSetListener = ((datePicker, year, month, day) -> {

        // Получаем объект из Mutable
        Calendar calendar = alarmDate.getValue();

        // Устанавилваем значения
        calendar.set(year, month, day);

        // Возвращаем объект
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

        // Получаем код, с которым открым вызывается эта Activity
        // Если это CREATE_REQUEST (= 1) - значит мы создаем новый будильник и инициализировать
        // компоненты нужно дефолтными значениями.
        // Если это UPDATE_REQUEST (= 2) - значит мы меняем уже созданный будильник, параметры
        // которого передаем в вызывающем Intent, и инциализировать должны этими значениями
        initialize(intent.getIntExtra("requestCode", 0)); // Значение "0" не обрабатываю
    }

    /**
     * Метод инициализации состояния Activity в зависимости от намерения (простите за каламбур) вызывающей
     * стороны
     * @param requestCode код запроса вызывающий стороны (CREATE или UPDATE)
     */
    private void initialize(int requestCode) {
        //Intent intent = getIntent();

        // Получаем текущую временную точку
        timestamp = Calendar.getInstance();

        // Инициализируем паттерн нулем
        repeatIn.setValue(0);

        // LiveData Calendar alarmDate (момент включения будильника)
        Observer<Calendar> observer = this::updateLabel;
        alarmDate.observe(this, observer);

        // LiveData Integer repeatIn (паттерн повторений)
        Observer<Integer> observerInt = this::setRepeat;
        repeatIn.observe(this, observerInt);

        // Задаем локальную (!) переменную для инициализации начальных значений.
        // Знаю, что есть глобальная, но не хочу ее использовать для инициализации.
        Calendar alarm = Calendar.getInstance();

        /**
         * Если вызов был с целью обновления будильника
         * default-значения в get..Extra - просто для компиляции. Мы и так знаем, что вызывающий
         * Intent содержит необходимые значения
         */
        if (requestCode == MainActivity.UPDATE_REQUEST) {

            // Получаем вызывающий Intent и достаем из его тела значения:
            Intent intent = getIntent();

            // Установленная ранее точка начала работы будильника
            alarm.setTimeInMillis(intent.getLongExtra("start", 0));

            // Определенный ранее паттерн повторений
            repeatIn.setValue(intent.getIntExtra("repeatIn", 0));

            // Сформированная ранее строка из паттерна повторений
            repeatString = intent.getStringExtra("repeatString");

            // Значение вибро-режима
            vibro = intent.getBooleanExtra("vibro", false);

            // И включен ли вообще этот будильник
            enabled = intent.getBooleanExtra("enabled", false);
        }
        alarmDate.setValue(alarm);

        dateLabel = findViewById(R.id.dateLabel);

        // Определяем переключаетель виброрежима
        Switch vibroSwitch = findViewById(R.id.vibroSwitch);

        // Устанавливаем значения
        vibroSwitch.setChecked(vibro);

        // Используем обработчик нажатий.
        // Важное замечание: в арсенале Listener'-ов для Switch'-а есть OnCheckedChange, НО!
        // он будет срабатывать каждый раз, когда мы меняем значения не только по нажатию, но и
        // программно (setChecked), что несколько увеличит количество к нему обращений. Поскольку
        // мы отслеживаем поведения пользователя в UI, более корректно будет переопределить нажатие,
        // то есть OnClick, а уж визуальная составляющая OnChecked изменится сама собой.
        vibroSwitch.setOnClickListener(v -> vibro = !vibro);

        // Определяем состояние и поведение кнопки "Отменить"
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(view -> {
            // Результат работы Activity (то есть resultCode) устанавливаем в код запуска, просто
            // для удобства. То есть ответ Activity всегда будет с вызывающим кодом, а успешная работа
            // или нет будем определять по наличию данных для передачи.
            // Это неверное использование. Здесь нужно поставить resultCode для Cancel и для Save разный,
            // и работать с кодами результата, но в данной программе я позволю себе такое использование,
            // хотя, конечно, это надо переделывать.
            setResult(requestCode);
            finish();
        });

        // Определяем состояние и поведение кнопки "Сохранить"
        Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> {
            // Поскольку передать объект напрямую мы не можем (хотя, можем реализвать Parcelable или
            // Serializable), Bundle собирать я не хочу (чтобы не упаковывать данные, которые можно
            // передать в Intent, еще и в Bundle, а потом в Intent), "набиваем" Intent напрямую данными,
            // которые хотим передать в вызвающую сторону в качестве результата работы Activity
            Intent result = new Intent();
            result.putExtra("start", alarmDate.getValue().getTimeInMillis());
            result.putExtra("repeatIn", repeatIn.getValue());
            result.putExtra("vibro", vibro);
            result.putExtra("enabled", enabled);
            result.putExtra("repeatString", dateLabel.getText().toString());

            // Если мы обновляем будильник, то, среди прочего, передаем и id
            if (requestCode == MainActivity.UPDATE_REQUEST)
                result.putExtra("id", getIntent().getIntExtra("id", -1));

            // Помещаем Intent в результат
            setResult(requestCode, result);
            finish();
        });

        // Иницилаизируем кнопки паттерна повторений
        initRepeatButtons();

        // Инициализируем пикеры (TimePicker и DatePicker) для установки момента включения
        initPickers(requestCode);
    }

    /**
     * Метод инициализации кнопок паттерна повторений
     * //TODO Перенести в XML DataBinding
     * Все кнопки добавляем в List<Button>. А можно б и создание этих кнопок определить
     * программно, раз уж в коде все это делается. Но DataBinding нагляднее
     */
    private void initRepeatButtons() {
        Button mondayButton = findViewById(R.id.mondayButton);
        buttons.add(mondayButton);

        // В момент инициализации кнопок, еще до того, как мы будем присваивать кнопкам цвет их
        // текста, в зависимости от паттерна повторений, получаем и сохраняем цвет ненажатой кнопки.
        // Я даже не знаю - это такая попытка не лезть в R.style :)))
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

        // Для каждой кнопки в списке - сравниваем значение ее текста с паттерном, и если текст
        // присутсвует в паттерне - устанавливаем цвет кнопки в "цвет нажатия", а потом прикручиваем
        // OnClickListener
        buttons.stream()
                .peek(b -> {
                    if (repeatString != null && repeatString.contains(b.getText().toString())) b.setTextColor(pressedColor);
                }).forEach(b -> b.setOnClickListener(weekButtonListener));
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

        // Если паттерн пуст - устанавливаем значение момента срабатывания будильника
        if (repeatIn.getValue() == 0) dateLabel.setText(format.format(value.getTime()));
    }

    /**
     * Обновляем Label значением паттерна повторений
     * @param repeat значение паттерна
     */
    private void setRepeat(int repeat) {

        // Если паттерн пуст - устанавливаем значения времени и даты
        if (repeat == 0) {
            updateLabel(alarmDate.getValue());
            return;
        }

        // Преобразуем значение паттерна в бинарную строку
        String binary = Integer.toBinaryString(repeat);

        // По каждому значащему биту будем собирать результирующую строку из значений через запятую
        StringJoiner joiner = new StringJoiner(", ");

        // Проходим по всей бинарной строке паттерна
        for (int i = 0; i < binary.length(); i++) {

            // Рассчитываем смещение. Это необходимо потому, что незначащие нули (старшие нулевые биты)
            // отбрасываются, то есть 0000100 = 100, а значит для первая (слева направо) единица в
            // бинарной строке - это daysOfWeek.length - binary.length (= 7 - 3 = 4, то есть
            // 5-ый элемент (начиная с "0") для пример выше).
            int offset = daysOfWeek.length - binary.length();

            // Если бит равен "1" - добавляем соответсвующий элемент в результирующую строку
            if (binary.charAt(i) == '1') joiner.add(daysOfWeek[i + offset]);
        }
        dateLabel.setText(joiner.toString());
    }

    /**
     * Метод инициализации выбора времени. SpinnerMode установлен в XML
     */
    private void initPickers(int requestCode) {
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

        // Инициализируем TimePicker
        initTimePicker(timePicker, requestCode);

        // Инициализируем DatePicker
        initDatePicker(requestCode);

        Button dateButton = findViewById(R.id.dateButton);

        // Обрабатываем нажатие на кнопку выбора даты
        dateButton.setOnClickListener(view -> datePickerDialog.show());
    }

    /**
     * Метод инициализации TimePicker
     * @param timePicker, который будем инициализировать
     * @param requestCode - код запроса, от которого зависят инициализируемые значения
     */
    private void initTimePicker(TimePicker timePicker, int requestCode) {

        // По умолчанию = 06:00
        int hour = 6;
        int minute = 0;

        // Если мы обновляем будильник, значит значения мы должны поставить те, которые уже
        // были установлены
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

    /**
     * Метод инициализации DatePicker
     * @param requestCode - код запроса, от которого зависят инициализируемые значения
     */
    private void initDatePicker(int requestCode) {
        // Получаем значения года, месяца и дня месяца для текущего временного момента...
        int year = timestamp.get(Calendar.YEAR);
        int month = timestamp.get(Calendar.MONTH);

        // Calendar.DAY_OF_MONTH == Calendar.DATE
        int day = timestamp.get(Calendar.DAY_OF_MONTH);

        // Если мы обновляем будильник, значит значения мы должны поставить те, которые уже
        // были установлены
        if (requestCode == MainActivity.UPDATE_REQUEST) {
            long timeInMillis = getIntent().getLongExtra("start", 0);
            Calendar moment = Calendar.getInstance();
            moment.setTimeInMillis(timeInMillis);

            year = moment.get(Calendar.YEAR);
            month = moment.get(Calendar.MONTH);
            day = moment.get(Calendar.DAY_OF_MONTH);
        }

        // И устанавливаем дату в календаре (из которого производится выбор даты)
        datePickerDialog = new DatePickerDialog(this, dateSetListener,
                year, month, day);

        // Текущий момент времени - это минимальная доступная точка
        datePickerDialog.getDatePicker().setMinDate(timestamp.getTimeInMillis());
    }

}
