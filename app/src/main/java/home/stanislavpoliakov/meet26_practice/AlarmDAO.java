package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

@Dao
public interface AlarmDAO {

    @Query("SELECT * FROM alarms")
    Cursor getAlarms();

    @Insert (onConflict = OnConflictStrategy.REPLACE)
    long insert(Alarm alarm);

    @Update (onConflict = OnConflictStrategy.REPLACE)
    int update(Alarm alarm);

    @Query("DELETE FROM alarms WHERE id = :id")
    int delete(int id);
}
