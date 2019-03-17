package home.stanislavpoliakov.meet26_practice;

import android.arch.persistence.room.Room;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class MyContentProvider extends ContentProvider {
    private static final String TAG = "meet26_logs";
    //private AlarmDatabase mDatabase;
    private AlarmDAO dao;
    private static final String AUTHORITY = "content_provider";
    private static final String ENTRIES_TABLE = "alarm_database";
    private static final Uri CONTENT_URI =
            Uri.parse("content://" + AUTHORITY + "/" + ENTRIES_TABLE);

    private static final int ENTRIES = 100;
    private static final int ENTRY_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, ENTRIES_TABLE, ENTRIES);
        uriMatcher.addURI(AUTHORITY, ENTRIES_TABLE + "/#", ENTRY_ID);
    }

    @Override
    public boolean onCreate() {
        AlarmDatabase mDatabase = Room.databaseBuilder(getContext(), AlarmDatabase.class, "alarms")
                .fallbackToDestructiveMigration()
                .build();
        dao = mDatabase.getAlarmDAO();
        return mDatabase != null;
    }

    @Override
    public Cursor query( Uri uri, String[] projection, String selection,
                         String[] selectionArgs, String sortOrder) {
        int uriType = uriMatcher.match(uri);
        Cursor cursor;
        if (uriType == ENTRIES) {
            cursor = dao.getAlarms();
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = uriMatcher.match(uri);
        long id;
        if (uriType == ENTRY_ID) {
            id = dao.insert(ConvertUtils.convertValuesToAlarm(values));
            //Log.d(TAG, "insert: id = " + id);
        } else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        getContext().getContentResolver().notifyChange(uri, null);

        return Uri.parse(CONTENT_URI + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsDeleted;
        if (uriType == ENTRY_ID) {
            String stringID = uri.getLastPathSegment();
            int id = Integer.parseInt(stringID);
            //Log.d(TAG, "delete: ID = " + id);
            rowsDeleted = dao.delete(id);
            //Log.d(TAG, "delete: Rows Deleted = " + rowsDeleted);
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        //Log.d(TAG, "delete: URI = " + uri);
        getContext().getContentResolver().notifyChange(uri, null);

        Log.d(TAG, "delete: rowsdeleted = " + rowsDeleted);
        return rowsDeleted;
        //return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int uriType = uriMatcher.match(uri);
        int rowsUpdated;
        if (uriType == ENTRY_ID) {
            rowsUpdated = dao.update(ConvertUtils.convertValuesToAlarm(values));
        }
        else throw new UnsupportedOperationException("Illegal URI(" + uri + ")");

        getContext().getContentResolver().notifyChange(uri, null);

        return rowsUpdated;
        //return 0;
    }
}
