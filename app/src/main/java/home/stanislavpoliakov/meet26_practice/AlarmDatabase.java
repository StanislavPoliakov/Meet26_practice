package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = Alarm.class, version = 1)
public abstract class AlarmDatabase extends RoomDatabase{
    public abstract AlarmDAO getAlarmDAO();
}
