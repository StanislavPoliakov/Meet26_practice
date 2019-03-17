package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;

import java.io.Closeable;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

@Entity (tableName = "alarms", indices = @Index("id"))
public class Alarm {
    @Ignore private static final String TAG = "meet26_logs";

    @PrimaryKey (autoGenerate = true)
    public int id;

    public long start;

    @ColumnInfo(name = "repeat_in")
    private int repeatIn;

    @ColumnInfo(name = "repeat_string")
    private String repeatString;

    private boolean vibro;
    private boolean enabled;

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

    public void setRepeatString(String repeatString) {
        this.repeatString = repeatString;
    }

    public void update(Calendar alarm) {
        this.start = alarm.getTimeInMillis();
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

    @Ignore
    @Override
    public int hashCode() {
        int result = Long.hashCode(start / 60000);
        result += 32 * repeatIn;
        return result;
    }

    @Ignore
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj.getClass() != this.getClass()) return false;
        Alarm alarm = (Alarm) obj;
        return ((this.start / 60000 == alarm.start / 60000) && (this.repeatIn == alarm.repeatIn));
    }
}
