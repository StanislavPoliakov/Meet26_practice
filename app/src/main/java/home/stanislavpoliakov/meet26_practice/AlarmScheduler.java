package home.stanislavpoliakov.meet26_practice;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * Класс-помощник для планирования задач будильника
 */
public final class AlarmScheduler {
    private static WorkManager workManager = WorkManager.getInstance();

    /**
     * Метод для создания запланированных звонков
     * Задачу на срабатываение звонка создает "задача ожидания"
     * @param date - дата, на которую запланирован звонок
     * @param tag - тэг звонка (один тэг на все звонки будильника)
     * @param isPeriodic - флаг периодичности
     */
    public static void makeWorkChain(Calendar date, String tag, boolean isPeriodic) {

        // Получаем "задачу ожидания"
        OneTimeWorkRequest delay = getDelayWork(date, tag, isPeriodic);

        // Запускаем в работу
        workManager.enqueue(delay);
    }

    /**
     * Метод создания "задачи ожидания"
     * @param date - дата, на которую запланирован звонок
     * @param tag - тэг звонка
     * @param isPeriodic - флаг периодичности
     * @return задача ожидания (OneTimeRequest)
     */
    private static OneTimeWorkRequest getDelayWork(Calendar date, String tag, boolean isPeriodic) {
        Calendar currentMoment = Calendar.getInstance();

        // Временная точка звонка (Unix-Time)
        long dateTime = date.getTimeInMillis();

        // Текущая временная точка (Unix-Time)
        long currentTime = currentMoment.getTimeInMillis();

        // Поскольку работу для звонка создает сама "задача ожидания" - передаем ей необходимые
        // параметры (флаг периодичности)
        Data periodicMarker = new Data.Builder()
                .putBoolean("isPeriodic", isPeriodic)
                .build();

        // Поясню логику создания задачи срабатывания звонка в зависимости от флага периодичности.
        // Если звонок повторяется (например, каждый вторник и четверг), значит мы должны создать
        // задачу ожидания (OneTimeRequest + DelayWorker), которая создаст повторяющийся Event
        // (PeriodicTimeRequest + AlarmWorker) через определенное время.
        // Если звонок единичный, тогда мы создаем одиночную задачу звонка (OneTimeRequest + AlarmWorker)
        // и смещаем его срабатывание на initialDelay
        // В зависимости от флага выбираем класс задачи (которая наследуется от ListenableWorker)
        Class<? extends ListenableWorker> workerClass = (isPeriodic) ? DelayWorker.class : AlarmWorker.class;

        return new OneTimeWorkRequest.Builder(workerClass)
                .setInitialDelay(Duration.ofMillis(dateTime - currentTime))
                .addTag(tag)
                .setInputData(periodicMarker)
                .build();
    }

    /**
     * Метод для создания множества дат для звонков из паттерна повторений
     * @param alarm будильник
     * @return множество дат
     */
    public static Set<Calendar> makeRepeatDateSet(Alarm alarm) {

        // Получаем значение паттерна повторений
        int repeatIn = alarm.getRepeatIn();

        // Получаем строку паттерна (включая незначащие нули)
        String repeatPattern = getRepeatPattern(repeatIn);

        // Получаем время, на которое установлен будильник
        Calendar initial = alarm.getStart();
        int hour = initial.get(Calendar.HOUR_OF_DAY);
        int minute = initial.get(Calendar.MINUTE);

        // Смещение до временной точки срабатывания (относительно текущего момента). В днях!
        int offset;

        Set<Calendar> dateSet = new HashSet<>();

        // Для каждого бита, установленного в "1", получаем смещение относительно текущего времени
        for (int i = 0; i < repeatPattern.length(); i++) {
            if (repeatPattern.charAt(i) == '1') {
                offset = calculateOffset(hour, minute, i);

                // Получаем текущую временную точку...
                Calendar startDate = Calendar.getInstance();
                int date = startDate.get(Calendar.DAY_OF_MONTH);

                // и смещаем дату на расчитанное значение
                startDate.set(Calendar.DAY_OF_MONTH, date + offset);

                // Остальные поля те же
                startDate.set(Calendar.HOUR_OF_DAY, hour);
                startDate.set(Calendar.MINUTE, minute);
                startDate.set(Calendar.SECOND, 0);

                // Добавляем дату во множество
                dateSet.add(startDate);
            }
        }

        // Если сет пуст (то есть паттерн пуст), тогда добавляем во множество единственное значение даты
        if (dateSet.isEmpty()) dateSet.add(alarm.getStart());

        return dateSet;
    }

    /**
     * Метод расчета смещения относительно текущей временной точки до момента срабатывания звонка
     * @param startHour значение "часа" для момента звонка
     * @param startMinute значение "минуты" для момента звонка
     * @param dayOfWeek значение дня недели из паттерна повторений
     * @return
     */
    private static int calculateOffset(int startHour, int startMinute, int dayOfWeek) {

        // Получаем значение времени в момент включения
        Calendar currentDate = Calendar.getInstance();

        // Текущий день недели
        int currentDay = currentDate.get(Calendar.DAY_OF_WEEK); // Sunday = 1

        // Смещаем передаваемый "день недели паттерна" на +2
        // Массив начинается с 0 - это +1, и день недели с воскресенья - еще +1
        dayOfWeek = dayOfWeek + 2;

        // Если значение больше 7, например для "нашего воскресенья" - dayOfWeek = 6 + 2 = 8
        // Вычитаем из dayOfWeek количество дней в неделе и получаем первый день недели 8 - 7 = 1
        dayOfWeek = (dayOfWeek > 7) ? dayOfWeek - 7 : dayOfWeek;

        // Получаем смещение
        int offset = dayOfWeek - currentDay;

        // Если разница отрицательна, значит смещение должно быть на следующую неделю (например,
        // если сегодня среда, а будильник стоит на вторник, то смещение должно быть = 6 дней, то
        // есть до следующего вторника)
        offset = (offset < 0) ? 7 + offset : offset;

        // Если паттерн указывает на сегодняшний день (то есть смещение равно "0"), то считаем время
        // будильника. Если оно, относительно текущей временной точки, в прошлом (например, сейчас
        // вторник, 21:58, а звонок мы ставим на 14:00, который будет повторяться по вторникам (среди прочих)),
        // то смещение установить в неделю (7)
        if (offset == 0) {
            int currentHour = currentDate.get(Calendar.HOUR_OF_DAY);
            int currentMinute = currentDate.get(Calendar.MINUTE);
            if ((startHour < currentHour) ||
                    ((startHour == currentHour) && (startMinute <= currentMinute))) offset = 7;
        }

        return offset;
    }

    /**
     * Метод преобразования значения паттерна в строку паттерна, включая незначащие нули
     * @param repeatIn числовое значение паттерна
     * @return строковое представление
     */
    private static String getRepeatPattern(int repeatIn) {

        // Получаем бинарную строку
        String repeatString = Integer.toBinaryString(repeatIn);

        // Создаем StringBuilder, чтобы не плодить строки в результате конкатенации
        StringBuilder repeatPattern = new StringBuilder();

        // Добавляем незначащие нули
        for (int i = 0; i < 7 - repeatString.length(); i++) repeatPattern.append(0);

        // Соединяем с бинарной строкой и возвращаем результат
        return repeatPattern.append(repeatString).toString();
    }

    /**
     * Метод получения информации об оставшемся времени до ближайшего будильника
     * @param startTime время будильника
     * @return информация в формате String
     */
    public static String getTextForToast(long startTime) {

        // Получаем текущую временную точку
        Calendar moment = Calendar.getInstance();

        // Интервал в милисекундах
        long delay = (startTime - moment.getTimeInMillis()) / 1000;

        // Осталось секунд (меньше минуты)
        long seconds = delay % 60;

        // Интервал в минутах
        delay /= 60;

        // Осталось минут (меньше часа)
        long minutes = delay % 60;

        // Интервал в часах
        delay /= 60;

        // Остаток часов (меньше суток)
        long hours = delay % 24;

        // Интервал в днях
        delay /= 24;

        // Осталось дней (меньше года)
        long days = delay % 365;

        StringBuilder toastBuilder = new StringBuilder("Будильник сработает через: ");

        // Если осталось меньше минуты - выводим остаток секунд
        if (days == 0 && hours == 0 && minutes == 0)
            toastBuilder.append(seconds).append(" секунд");

        // Если осталось меньше часа - выводим остаток минут и секунд
        else if (days == 0 && hours == 0)
            toastBuilder.append(minutes).append(" минут и ").append(seconds).append(" секунд");

        // Если осталось меньше дня - выводим остаток в часах и минутах
        else if (days == 0)
            toastBuilder.append(hours).append(" часов и ").append(minutes).append(" минут");

        // В противном случае - выводим остаток в днях, часах и минутах
        else
            toastBuilder.append(days).append(" дней, ").append(hours).append(" часов и ").append(minutes).append(" минут");

        return toastBuilder.toString();
    }
}
