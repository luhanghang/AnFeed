package com.smartvision.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by IntelliJ IDEA.
 * User: luhang
 * Date: 11/21/11
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "db";
    public static final int VERSION = 3;

    public static final String[] tables = {Site.TABLE, Task.TABLE, TaskSite.TABLE, Media.TABLE};
    public static final String[] creates = {Site.CREATE, Task.CREATE, TaskSite.CREATE, Media.CREATE};

    DatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        for (String create : creates)
            db.execSQL(create);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String table : tables)
            db.execSQL("DROP table IF EXISTS " + table);
        onCreate(db);
    }
}
