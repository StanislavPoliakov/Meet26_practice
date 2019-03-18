package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;
import android.database.Cursor;

/**
 * Room DataAccessObject
 */
@Dao
public interface AlarmDAO {

    /**
     * RETRIEVE
     * @return список всех будильников в формате Cursor
     */
    @Query("SELECT * FROM alarms")
    Cursor getAlarms();

    /**
     * CREATE
     * @param alarm новый будильник
     * @return id созданной записи
     */
    @Insert (onConflict = OnConflictStrategy.REPLACE)
    long insert(Alarm alarm);

    /**
     * UPDATE
     * @param alarm будильник, который мы обновляем
     * @return количество обновленных записей
     */
    @Update (onConflict = OnConflictStrategy.REPLACE)
    int update(Alarm alarm);

    /**
     * DELETE
     * @param id, по которому будем удалять запись
     * @return количество удаленных записей
     */
    @Query("DELETE FROM alarms WHERE id = :id")
    int delete(int id);

    // Напомню, еще разок, что стандартная реалзиации @Delete не удаляет явно по id
}
