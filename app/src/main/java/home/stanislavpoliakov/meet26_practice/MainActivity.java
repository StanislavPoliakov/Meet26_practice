package home.stanislavpoliakov.meet26_practice;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static home.stanislavpoliakov.meet26_practice.ConvertUtils.convertAlarmToValues;
import static home.stanislavpoliakov.meet26_practice.ConvertUtils.convertCursorToAlarmSet;

public class MainActivity extends AppCompatActivity implements ICallback{
    private static final String TAG = "meet26_logs";
    private static final String AUTHORITY = "content_provider";
    private static final String ENTRIES_TABLE = "alarm_database";

    public static final int CREATE_REQUEST = 1;
    public static final int UPDATE_REQUEST = 2;

    // Это объект изменения. Вынесенен на уровень глобальной переменной, потому что используется
    // из разных методов (нужен перенос состояния из одного метода в другой).
    // После окончания работы обнулять!
    private Alarm alarm;

    private TextView noAlarmsLabel;
    private ContentResolver contentResolver;
    private QueryHandler queryHandler;
    private MutableLiveData<ArraySet<Alarm>> alarmSet = new MutableLiveData<>();
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /**
         * Fab запускает новую Activity с запросом на создание нового будильника
         */
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent createIntent = AlarmActivity.newIntent(this);

            // Поскольку вызываемая Activity ничего не знает о том, какой код запроса был с вызывающей
            // стороны - передаем этот код через Intent
            createIntent.putExtra("requestCode", CREATE_REQUEST);
            startActivityForResult(AlarmActivity.newIntent(this), CREATE_REQUEST);
        });

        init();
    }

    /**
     * Метод обновления RecyclerView по данным LiveData
     * @param data множество (в смысле, математическое множество) будильников
     */
    private void updateRecycler(ArraySet<Alarm> data) {
        mAdapter.setData(data);
    }

    /**
     * Метод общей инициализации
     */
    private void init() {
        noAlarmsLabel = findViewById(R.id.noAlarmsLabel);

        contentResolver = getContentResolver();
        queryHandler = new QueryHandler(contentResolver);

        // Получаем список всех сохраненных будильников
        // Цепочка: (AsyncQueryHelper) startQuery -> onQueryComplete -> обновляем LiveData -> обновляем RecyclerView
        getAll();

        // Инициализируем список пустой (ненулевой!) ссылкой
        ArraySet<Alarm> initSet = new ArraySet<>();
        alarmSet.setValue(initSet);

        // Инициализируем RecyclerView
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        mAdapter = new MyAdapter(this, alarmSet.getValue());
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(layoutManager);

        // LiveData ArraySet<Alarm> нумерованное множество будильников
        Observer<ArraySet<Alarm>> observer = this::updateRecycler;
        alarmSet.observe(this, observer);
    }

    /**
     * Метод получение списка созданных (и сохраненных в базе) будильников. Если быть
     * точнее, то не получения, а инициирования получения данных. Получим результат мы
     * в onQueryComplete
     */
    private void getAll() {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);
        queryHandler.startQuery(1, null, CONTENT_URI, null,
                null, null, null);
    }

    /**
     * Метод создания новой записи в базе данных
     * @param alarm объект будильника, который мы будем сохранять
     */
    private void insertAlarm(Alarm alarm) {

        // /0 - это id = 0. Еще раз - здесь это любой ID, актуальное значение - в onInsertComplete
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/0");
        queryHandler.startInsert(1, null, CONTENT_URI, convertAlarmToValues(alarm));
    }

    /**
     * Метод обновления записи базы данных
     * @param alarm данные для обновления
     */
    private void updateAlarm(Alarm alarm) {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + alarm.id);
        queryHandler.startUpdate(1, null, CONTENT_URI, convertAlarmToValues(alarm),
                null, null);
    }

    /**
     * Метод удаления записи из базы данных
     * @param alarm данные, которые мы будем удалять
     */
    private void deleteAlarm(Alarm alarm) {
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE + "/" + alarm.id);
        queryHandler.startDelete(1, null, CONTENT_URI, null, null);
    }

    /**
     * Удаляем элемент списка через долгое нажатие -> выбор "Delete" в выпадающем меню
     * @param item элемент выпадающего меню
     * @return обработано здесь?
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if ("Delete".equals(item.getTitle())) {

            // Получаем позицию элемента, у которого вызвано выпадающее меню (подробности в MyViewHolder)
            int position = item.getItemId();

            // Вот зачем нам нумерованное множество - получаем будильник по позиции
            alarm = alarmSet.getValue().valueAt(position);

            // Инициируем процедуру удаления
            deleteAlarm(alarm);
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Метод обработки нажатия на элемент списка RecyclerView
     * @param position позиция элемента в списке
     */
    @Override
    public void itemSelected(int position) {

        // Специально оставляю Hide-полей, хоть это и не совсем корректно
        Alarm alarm = alarmSet.getValue().valueAt(position);

        // "Набиваем" Intent данными, которыми будем инциализировать Activity для изменения свойств будильника
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

    /**
     * Метод обработки нажатия на "рубильник" (включено / выключено)
     * @param position позиция элемента
     */
    @Override
    public void switchChange(int position) {
        alarm = alarmSet.getValue().valueAt(position);
        alarm.setEnabled(!alarm.isEnabled());

        // Инициируем обновление
        updateAlarm(alarm);
    }

    /**
     * Метод обработки результата запуска Activity для создания / изменения будильника
     * @param requestCode код запроса
     * @param resultCode код результата
     * @param data данные результата
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        // Если была нажата кнопка "Сохранить" (то есть если данные присутствуют)
        // Фактически эквивалентно обработке значений resultCode. Просто resultCode - правильнее
        if (data != null) {

            // Создаем новый объект будильника
            alarm = new Alarm(data.getLongExtra("start", 0), data.getStringExtra("repeatString"));

            // Устанавливаем паттерн повторов
            alarm.setRepeatIn(data.getIntExtra("repeatIn", 0));
            if (requestCode == CREATE_REQUEST) {

                // Если множество содержит такой будильник (здравствуй, Equals и HashCode), то добавить в базу
                if (!alarmSet.getValue().contains(alarm)) insertAlarm(alarm);
                else {
                    Toast.makeText(this,
                            "Будильник с такими параметрами уже существует", Toast.LENGTH_SHORT).show();
                    alarm = null;
                }

            } else if (requestCode == UPDATE_REQUEST) {
                alarm.id = data.getIntExtra("id", 0);
                alarm.setRepeatIn(data.getIntExtra("repeatIn", 0));
                alarm.setVibro(data.getBooleanExtra("vibro", false));
                alarm.setEnabled(data.getBooleanExtra("enabled", false));

                //PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS = 15 * 60 * 1000L = 15 минут :)
                /*PeriodicWorkRequest alarmWorkRequest = new PeriodicWorkRequest.Builder(AlarmWorker.class, 1, TimeUnit.SECONDS)
                        .build();

                OneTimeWorkRequest alarmWorkRequestOneTime = new OneTimeWorkRequest.Builder(AlarmWorker.class)
                        //.setInitialDelay(5, TimeUnit.SECONDS)
                        .build();

                WorkManager.getInstance().cancelAllWork();
                //WorkManager.getInstance().enqueue(alarmWorkRequest);*/

                // Если изменный будильник уникален - обновить запись
                if (!alarmSet.getValue().contains(alarm)) updateAlarm(alarm);
                else {
                    Toast.makeText(this,
                            "Будильник с такими параметрами уже существует", Toast.LENGTH_SHORT).show();
                    alarm = null;
                }
            }
        }
    }

    /**
     * Метод проверки количества элементов в списке
     * Список вывожу не во фрагменте, а в основной Activity, надпись показываю / прячу в зависимости
     * от количества элементов в списке
     */
    private void checkCount() {
        noAlarmsLabel.setVisibility((alarmSet.getValue().size() == 0) ? View.VISIBLE : View.GONE);
    }

    /**
     * Класс для работы с ContentResolver -> ContentProvider
     */
    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /**
         * После получения результата выборки
         */
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {

            // Обновить LiveData конвертированными значениями
            alarmSet.setValue(convertCursorToAlarmSet(cursor));

            // Проверить количество элементов в списке
            checkCount();
        }

        /**
         * После добавления элемента
         */
        @Override
        protected void onInsertComplete(int token, Object cookie, Uri uri) {

            // В этот момент alarm = объект, который мы добавляли. И в этом объекте нужно обновить
            // ID и добавить в ArraySet
            // Получаем ID
            String stringID = uri.getLastPathSegment();

            // Парсим
            int id = Integer.parseInt(stringID);

            // Обновляем
            alarm.id = id;

            ArraySet<Alarm> aSet = alarmSet.getValue();
            aSet.add(alarm);

            // Добавляем во множество
            alarmSet.setValue(aSet);

            // Проверяем количество элементов в списке
            checkCount();

            // Обнуляем глобальную переменную
            alarm = null;
        }

        /**
         * После обновления элемента
         */
        @Override
        protected void onUpdateComplete(int token, Object cookie, int result) {

            // Что-то добавили?
            if (result > 0) {
                // Изначально здесь был HashSet :)
                // Проходим по множеству. Если id совпадает - меняем элемент множества,
                // все собираем в новое множество и устанавливаем
                ArraySet<Alarm> aSet = alarmSet.getValue().stream()
                        .map(item -> item = (item.id == alarm.id) ? alarm : item)
                        .collect(Collectors.toCollection(ArraySet::new));
                alarmSet.setValue(aSet);

                // Обновляем глобальную переменную
                alarm = null;
            }
        }

        /**
         * После удаления элемента
         */
        @Override
        protected void onDeleteComplete(int token, Object cookie, int result) {
            // Что-то удалили?
            if (result > 0) {

                ArraySet<Alarm> aSet = alarmSet.getValue();

                // Удаляем элемент из множества
                aSet.remove(alarm);
                alarmSet.setValue(aSet);

                // Проверяем количество элементов
                checkCount();

                // Обнуляем глобальную переменную
                alarm = null;
            }
        }
    }

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
}
