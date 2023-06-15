package com.paradox.addictsclock;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class RoomDBHelper extends SQLiteOpenHelper {
    private static final String db_name = "roomId.db";
    private static final int db_version = 1;
    private static RoomDBHelper dbHelper = null;
    private SQLiteDatabase sqliteDB = null;
    private static final String table_name = "roomIds";

    public RoomDBHelper(@Nullable Context context) {
        super(context, db_name, null, db_version);
    }

    public RoomDBHelper(@Nullable Context context, int version) {
        super(context, db_name, null, version);
    }

    public static RoomDBHelper getInstance(Context context, int version) {
        if(version > 0 && dbHelper == null) {
            dbHelper = new RoomDBHelper(context, version);
        } else if(dbHelper == null) {
            dbHelper = new RoomDBHelper(context);
        }
        return dbHelper;
    }

    public SQLiteDatabase openReadLink() {
        if(sqliteDB == null || !sqliteDB.isOpen()) {
            sqliteDB = dbHelper.getReadableDatabase();
        }
        return sqliteDB;
    }

    public SQLiteDatabase openWriteLink() {
        if(sqliteDB == null || !sqliteDB.isOpen()) {
            sqliteDB = dbHelper.getWritableDatabase();
        }
        return sqliteDB;
    }

    public void closeLink() {
        if(sqliteDB != null && sqliteDB.isOpen()) {
            sqliteDB.close();
            sqliteDB = null;
        }
    }

    public String getDBName() {
        if(dbHelper != null) {
            return dbHelper.getDatabaseName();
        } else {
            return db_name;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql_create = String.format("create table if not exists %s(roomId int primary key)", table_name);
        db.execSQL(sql_create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int delete(int roomId) {
        String condition = String.format("roomId = %s", roomId);

        return sqliteDB.delete(table_name, condition, null);
    }

    public long insert(int roomId) {
        ContentValues cv = new ContentValues();
        cv.put("roomId", roomId);

        return sqliteDB.insert(table_name, "", cv);
    }

    public ArrayList<Integer> query() {
        String sql_query = String.format("select roomId from %s", table_name);

        ArrayList<Integer> roomIds = new ArrayList<>();

        @SuppressLint("Recycle") Cursor cursor = sqliteDB.rawQuery(sql_query, null);

        if(cursor.moveToFirst()) {
            for(;; cursor.moveToNext()) {
                Integer roomId = cursor.getInt(0);

                roomIds.add(roomId);

                if(cursor.isLast()) break;
            }
        }
        cursor.close();

        return roomIds;
    }
}
