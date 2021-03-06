package home.stanislavpoliakov.meet26_practice;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Класс для конвертации форматов. Содержит статические методы. Ненаследуемый
 */
public final class ConvertUtils {
    private static final String START = "start";
    private static final String REPEAT_IN = "repeat_in";
    private static final String REPEAT_STRING = "repeat_string";
    private static final String ID = "id";
    private static final String VIBRO = "vibro";
    private static final String ENABLED = "enabled";

    /**
     * Метод преобразования форматов. Используется для создания (insert) и обновления (update)
     * записей в базе данных на участке взаимодействия ContentProvider -> Database
     * @param contentValues значения в формате ContentValues
     * @return объект записи Alarm
     */
    public static Alarm convertValuesToAlarm(ContentValues contentValues) {
        int id = contentValues.getAsInteger(ID);
        long startInMillis = contentValues.getAsLong(START);
        int repeatIn = contentValues.getAsInteger(REPEAT_IN);
        String repeatString = contentValues.getAsString(REPEAT_STRING);
        boolean vibro = contentValues.getAsBoolean(VIBRO);
        boolean enabled = contentValues.getAsBoolean(ENABLED);

        Alarm alarm = new Alarm(startInMillis, repeatString);
        alarm.id = id;
        alarm.setRepeatIn(repeatIn);
        alarm.setVibro(vibro);
        alarm.setEnabled(enabled);

        return alarm;
    }

    /**
     * Метод преобразования форматов. Используется для создания (insert) и обновления (update)
     * записей в базе данных на участке взаимодействия Activity -> ContentProvider
     * @param alarm объект записи Alarm
     * @return значения в формате ContentValues
     */
    public static ContentValues convertAlarmToValues(Alarm alarm) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(ID, alarm.id);
        contentValues.put(START, alarm.getStart().getTimeInMillis());
        contentValues.put(REPEAT_IN, alarm.getRepeatIn());
        contentValues.put(REPEAT_STRING, alarm.getRepeatString());
        contentValues.put(VIBRO, alarm.isVibro());
        contentValues.put(ENABLED, alarm.isEnabled());

        return contentValues;
    }

    /**
     * Метод преборазования форматов. Используется для преборазования полученных данных из
     * базы данных (SELECT * FROM entries) в список записей, который мы отправляем в Handler, а
     * затем в Activity при инициализации RecyclerView, а затем при отрисовке RecyclerView
     * @param cursor объект Cursor со списком найденных элементов базы данных
     * @return список записей в формате ArraySet<Alarm>
     */
    public static ArraySet<Alarm> convertCursorToAlarmSet(Cursor cursor) {
        ArraySet<Alarm> alarmSet = new ArraySet<>();

        // Перемещаем значение курсора в начало
        cursor.moveToFirst();

        // Пока курсор не указывает "за последний элемент", то есть пока есть элементы...
        while (!cursor.isAfterLast()) {

            // Получаем поля записи базы данных
            int id = cursor.getInt(cursor.getColumnIndex(ID));
            long start = cursor.getLong(cursor.getColumnIndex(START));
            int repeatIn = cursor.getInt(cursor.getColumnIndex(REPEAT_IN));
            String repeatString = cursor.getString(cursor.getColumnIndex(REPEAT_STRING));

            // Поскольку SQLite хранит Boolean в виде Integer (true/false = 1/0),
            // получаем int значения и преобразуем в соответствующий boolean
            int value = cursor.getInt(cursor.getColumnIndex(VIBRO));
            boolean vibro = (value == 1) ? Boolean.TRUE : Boolean.FALSE;

            value = cursor.getInt(cursor.getColumnIndex(ENABLED));
            boolean enabled = (value == 1) ? Boolean.TRUE : Boolean.FALSE;

            Alarm alarm = new Alarm(start, repeatString);
            alarm.id = id;
            alarm.setRepeatIn(repeatIn);
            alarm.setVibro(vibro);
            alarm.setEnabled(enabled);

            alarmSet.add(alarm);

            // Передвигаем курсор к следующей записи найденного списка
            cursor.moveToNext();
        }
        cursor.close();
        return alarmSet;
    }
}
