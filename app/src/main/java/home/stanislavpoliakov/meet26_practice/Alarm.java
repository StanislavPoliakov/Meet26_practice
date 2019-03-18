package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.util.Calendar;
import java.util.Date;

/**
 * Класс Будильника
 */
@Entity (tableName = "alarms", indices = @Index("id"))
public class Alarm {
    @Ignore private static final String TAG = "meet26_logs";

    @PrimaryKey (autoGenerate = true)
    public int id;

    // Время начала работы будильника (Date + Time) = getTimeInMillis
    public long start;

    // Битовая маска для повтора будильника
    @ColumnInfo(name = "repeat_in")
    private int repeatIn;

    // Результат парсинга битовой маски - здесь просто для удобства
    @ColumnInfo(name = "repeat_string")
    private String repeatString;

    // Включить вибрацию?
    private boolean vibro;

    // Включен ли будильник?
    private boolean enabled;

    /**
     * Будильник создаем по времени начала и строкове "повторений"
     * @param start момент начала работы будильника
     * @param repeatString паттерн повторений работы будильника
     */
    public Alarm(long start, String repeatString) {
        this.start = start;
        this.repeatString = repeatString;
        this.repeatIn = 0;
        this.vibro = false;
        this.enabled = true;
    }

    public String getRepeatString() {
        return this.repeatString;
    }

    @Ignore
    public Calendar getStart() {
        Calendar alarm = Calendar.getInstance();
        alarm.setTime(new Date(start));
        return alarm;
    }

    public void setEnabled(boolean state) {
        this.enabled = state;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setVibro(boolean state) {
        this.vibro = state;
    }

    public boolean isVibro() {
        return this.vibro;
    }

    public void setRepeatIn(int repeatIn) {
        this.repeatIn = repeatIn;
    }

    public int getRepeatIn() {
        return this.repeatIn;
    }

    /**
     * Переопределяем HashCode для сравнений в ArraySet
     * В значении момента убираем милисекунды (х1000) и секунды (х60)
     * Значимые поля - start и repeatIn
     * @return hashCode
     */
    @Ignore
    @Override
    public int hashCode() {
        int result = Long.hashCode(start / 60000);
        result += 31 * repeatIn;
        return result;
    }

    /**
     * Переопределяем Equals для сравнений в ArraySet
     * Значимые поля - start и repeatIn
     * @param obj объект сравнения
     * @return результат сравнения
     */
    @Ignore
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) return false;
        Alarm alarm = (Alarm) obj;
        return ((this.start / 60000 == alarm.start / 60000) && (this.repeatIn == alarm.repeatIn));
    }
}
