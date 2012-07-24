package cn.whzxt.android;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

public class Train {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static Date StartTime = null;
	public static Date EndTime = null;
	public static int StartMileage = 0;// GPS里程
	public static int StartSenMileage = 0;// 传感器里程
	public static Boolean IsTraining = false;
	public static String TrainID = "";
	public static final String VideoBasePath = "/mnt/sdcard2/zxtvideo/";
	/**
	 * 本次训练的视频路径
	 */
	public static String VideoPath = "";
	/**
	 * 视频图片计数器
	 */
	public static int VideoPhotoCount = 0;

	public static void Start(SQLiteDatabase db) {
		if (!IsTraining) {
			StartTime = new Date();
			EndTime = new Date();
			StartMileage = DeviceInfo.Mileage;
			StartSenMileage = DeviceInfo.SenMileage;
			TrainID = UUID.randomUUID().toString();
			ContentValues tcv = new ContentValues();
			tcv.put("guid", TrainID);
			tcv.put("coach", Coach.CardNo);
			tcv.put("student", Student.CardNo);
			tcv.put("starttime", dateFormat.format(StartTime));
			tcv.put("endtime", dateFormat.format(EndTime));
			tcv.put("balance", String.valueOf(Student.Balance));
			tcv.put("startmi", String.valueOf(StartMileage));
			tcv.put("endmi", String.valueOf(StartMileage));
			tcv.put("startsenmi", String.valueOf(StartSenMileage));
			tcv.put("endsenmi", String.valueOf(StartSenMileage));
			tcv.put("subject", String.valueOf(DeviceInfo.Subject));
			db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
			VideoPhotoCount = 0;
			File file = new File("/mnt/sdcard2");
			if (file.exists()) {
				VideoPath = VideoBasePath + TrainID;
				file = new File(VideoPath);
				if (!file.exists()) {
					file.mkdirs();
				}
			} else {
				VideoPath = "";
			}
			file = null;
			IsTraining = true;
		}
	}

	public static void Update(SQLiteDatabase db) {
		if (IsTraining) {
			EndTime = new Date();
			ContentValues tcv = new ContentValues();
			tcv.put("endtime", dateFormat.format(EndTime));
			tcv.put("balance", String.valueOf(Student.RealBalance));
			tcv.put("endmi", String.valueOf(DeviceInfo.Mileage));
			tcv.put("endsenmi", String.valueOf(DeviceInfo.SenMileage));
			tcv.put("subject", String.valueOf(DeviceInfo.Subject));
			db.update(MySQLHelper.T_ZXT_USE_DATA, tcv, "guid=?", new String[] { TrainID });
		}
	}

	public static void End(SQLiteDatabase db) {
		if (IsTraining) {
			IsTraining = false;
			Update(db);
			StartTime = null;
			EndTime = null;
			TrainID = "";
		}
	}
}
