package home.stanislavpoliakov.meet26_practice;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.time.Duration;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public final class AlarmScheduler {
    private static WorkManager workManager = WorkManager.getInstance();

    public static void makeWorkChain(Calendar date, String tag, boolean isPeriodic) {

        OneTimeWorkRequest delay = getDelayWork(date, tag, isPeriodic);
        workManager.enqueue(delay);
    }

    /*public static void makeSingleWork(Calendar date, String tag) {
        Calendar currentMoment = Calendar.getInstance();
        long dateTime = date.getTimeInMillis();
        long currentTime = currentMoment.getTimeInMillis();

        Data periodicMarker = new Data.Builder()
                .putBoolean("isPeriodic", false)
                .build();

        new OneTimeWorkRequest.Builder(AlarmWorker.class)
                //.setInitialDelay(Duration.ofMillis(dateTime - currentTime))
                .addTag(tag)
                .setInputData(periodicMarker)
                .build();
    }*/

    private static OneTimeWorkRequest getDelayWork(Calendar date, String tag, boolean isPeriodic) {
        Calendar currentMoment = Calendar.getInstance();
        long dateTime = date.getTimeInMillis();
        long currentTime = currentMoment.getTimeInMillis();

        Data periodicMarker = new Data.Builder()
                .putBoolean("isPeriodic", isPeriodic)
                .build();

        Class<? extends ListenableWorker> workerClass = (isPeriodic) ? DelayWorker.class : AlarmWorker.class;

        return new OneTimeWorkRequest.Builder(workerClass)
                //.setInitialDelay(Duration.ofMillis(dateTime - currentTime))
                .addTag(tag)
                .setInputData(periodicMarker)
                .build();
    }

    public static Set<Calendar> makeRepeatDateSet(Alarm alarm) {
        int repeatIn = alarm.getRepeatIn();
        String repeatPattern = getRepeatPattern(repeatIn);

        Calendar initial = alarm.getStart();
        int hour = initial.get(Calendar.HOUR_OF_DAY);
        int minute = initial.get(Calendar.MINUTE);

        int offset;
        //int dayOfWeek;

        Set<Calendar> dateSet = new HashSet<>();

        for (int i = 0; i < repeatPattern.length(); i++) {
            if (repeatPattern.charAt(i) == '1') {
                offset = calculateOffset(hour, minute, i);
                //Log.d(TAG, "makeRepeatDateSet: offset = " + offset);

                Calendar startDate = Calendar.getInstance();
                int date = startDate.get(Calendar.DAY_OF_MONTH);

                startDate.set(Calendar.DAY_OF_MONTH, date + offset);
                startDate.set(Calendar.HOUR_OF_DAY, hour);
                startDate.set(Calendar.MINUTE, minute);
                startDate.set(Calendar.SECOND, 0);

                dateSet.add(startDate);
            }
        }

        //dateSet.stream().forEach(date -> Log.d(TAG, "makeRepeatDateSet: date = " + date.getTime()));

        if (dateSet.isEmpty()) dateSet.add(alarm.getStart());

        return dateSet;
    }

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


        int offset = dayOfWeek - currentDay;
        offset = (offset < 0) ? 7 + offset : offset;
        if (offset == 0) {
            int currentHour = currentDate.get(Calendar.HOUR_OF_DAY);
            int currentMinute = currentDate.get(Calendar.MINUTE);
            if ((startHour < currentHour) ||
                    ((startHour == currentHour) && (startMinute <= currentMinute))) offset = 7;
        }

        return offset;
    }

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
}
