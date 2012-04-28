package com.longcent.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点信息
 */
public class Site extends Base {
    /**站点名称*/
    public static final String NAME = "site_name";
    /**服务器*/
    public static final String HOST = "host";
    /**端口*/
    public static final String PORT = "port";
    /**ftp账号*/
    public static final String ACCOUNT = "account";
    /**ftp密码*/
    public static final String PASSWD = "passwd";
    /**元数据服务器用户名*/
    public static final String ACCOUNT1 = "account1";
    /**元数据服务器密码*/
    public static final String PASSWD1 = "passwd1";
    /**元数据服务器地址*/
    public static final String ADDR = "addr";
    /**是否被选中*/
    public static final String SELECTED = "flag";

    public static final String[] FIELDS = {ID, NAME, HOST, PORT, ACCOUNT, PASSWD, ACCOUNT1, PASSWD1, ADDR, SELECTED};
    public static final String[] FIELDS_FOR_LIST = {ID,NAME};
    public static final String TABLE = "sites";
    public static final String CREATE = "create table if not exists " + TABLE + " ("
                + ID + " INTEGER PRIMARY KEY  AUTOINCREMENT  NOT NULL,"
                + NAME + " VARCHAR NOT NULL , "
                + HOST + " VARCHAR NOT NULL , "
                + PORT + " INTEGER NOT NULL  DEFAULT 21, "
                + ACCOUNT + " VARCHAR, "
                + PASSWD + " VARCHAR, "
                + ACCOUNT1 + " VARCHAR, "
                + PASSWD1 + " VARCHAR NOT NULL , "
                + ADDR + " VARCHAR NOT NULL , "
                + SELECTED + " INTEGER DEFAULT 0)";

    public Site(Context ctx) {
        super(ctx);
        this.fields = FIELDS;
        this.table = TABLE;
        this.fieldsForList = FIELDS_FOR_LIST;
    }

    /**
     * 设置被选中状态
     * @param ids 被选中站点id集合
     * @return 操作是否成功
     */
    public boolean setSelected(List<String> ids) {
        open();
        ContentValues values = new ContentValues();
        values.put(SELECTED, 0);
        boolean result = db.update(table, values, null, null) > 0;
        if (ids == null || ids.size() == 0) {
            close();
            return result;
        }
        values.put(SELECTED, 1);
        result &= db.update(table, values, genInIdsCondition(ids), null) > 0;
        close();
        return result;
    }

    /**
     * 获取被选中站点
     * @return 被选中站点id列表
     */
    public List<String> getSelectedIds() {
        List<String> ids = new ArrayList<String>();
        open();
        Cursor cursor = db.query(true, table, new String[]{ID}, SELECTED + "= 1", null, null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ids.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        close();
        return ids;
    }
}