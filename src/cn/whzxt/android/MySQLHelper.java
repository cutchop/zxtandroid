package cn.whzxt.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class MySQLHelper extends SQLiteOpenHelper {
	public static final String T_ZXT_USE_DATA = "zxt_use_data";
	public static final String T_ZXT_MODE_CHANGE = "zxt_mode_change";
	public static final String T_ZXT_GPS_DATA = "zxt_gps_data";

	public MySQLHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ZXT_USE_DATA + " (guid VARCHAR(36),coach VARCHAR(10),student VARCHAR(10),starttime VARCHAR(20),endtime VARCHAR(20))");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ZXT_MODE_CHANGE + " (changetime VARCHAR(20),mode CHAR(1))");
		db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ZXT_GPS_DATA + " (gpstime VARCHAR(20),lng VARCHAR(10),lat VARCHAR(10))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + T_ZXT_USE_DATA);
		db.execSQL("DROP TABLE IF EXISTS " + T_ZXT_MODE_CHANGE);
		db.execSQL("DROP TABLE IF EXISTS " + T_ZXT_GPS_DATA);
		onCreate(db);
	}
}