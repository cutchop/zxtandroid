package cn.whzxt.android;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.xml.datatype.DatatypeFactory;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import cn.whzxt.android.R.color;

import android.R.bool;
import android.R.integer;
import android.R.string;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.AvoidXfermode.Mode;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtInfoCoach, txtInfoStudent, txtStudentTitle, txtStatus, txtUploadUseDataStatus, txtLngLat;
	private TextView txtCoachName, txtCoachCard, txtStudentName, txtStudentCard;
	private TextView txtStartTime, txtTrainTime, txtBalance;
	private TextView txtNetworkStatus;
	private TextView txtJFMS, txtFJFMS, txtSubject2, txtSubject3;
	private NetImageView imgCoach, imgStudent;
	private LinearLayout layCoachTitle, layStudentTitle, layCoachInfo, layStudentInfo;
	private LinearLayout btnJFMS, btnFJFMS, btnSubject2, btnSubject3;
	private String deviceID, deviceName, schoolID, schoolName, session;
	private String server;
	private String uploadresult, cardresult, usedataresult;
	private String coachID = "", coachName, coachCard = "", coachIDCard = "", studentID = "", studentCard = "", studentIDCard = "", studentName;
	private int cardBalance = 0, cardBalanceNow = 0;
	private Boolean _hascard = false;
	private int col = 0;
	private Date startTime, nowTime, endTime;
	private String _curUUID;
	private int mode = 0;// 计费模式0，非计费模式1
	private int subject = 2;// 科目二，科目三
	private int retry = 0;
	private double lng, lat;
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerFlicker, _timerSecond, _timerMinute;
	private SharedPreferences settings;
	private MySQLHelper sqlHelper;
	private SQLiteDatabase db;
	private Cursor _cursor;
	private Boolean blindSpotFinish = true;

	private static final String PATH = "/dev/s3c2410_serial1";
	private static final int BAUD = 9600;
	private int fd;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				getLngLat();
				upload();
				break;
			case 2:
				flicker();
				break;
			case 3:
				second();
				break;
			case 4:
				minute();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		// 界面初始化
		txtSchoolName = (TextView) findViewById(R.id.txtSchoolName);
		txtDeviceName = (TextView) findViewById(R.id.txtDeviceName);
		txtSystemTime = (TextView) findViewById(R.id.txtSystemTime);
		txtInfoCoach = (TextView) findViewById(R.id.txtInfoCoach);
		txtStudentTitle = (TextView) findViewById(R.id.txtStudentTitle);
		txtInfoStudent = (TextView) findViewById(R.id.txtInfoStudent);
		layCoachTitle = (LinearLayout) findViewById(R.id.layCoachTitle);
		layStudentTitle = (LinearLayout) findViewById(R.id.layStudentTitle);
		layCoachInfo = (LinearLayout) findViewById(R.id.layCoachInfo);
		layStudentInfo = (LinearLayout) findViewById(R.id.layStudentInfo);
		btnJFMS = (LinearLayout) findViewById(R.id.btnJFMS);
		btnFJFMS = (LinearLayout) findViewById(R.id.btnFJFMS);
		btnSubject2 = (LinearLayout) findViewById(R.id.btnSubject2);
		btnSubject3 = (LinearLayout) findViewById(R.id.btnSubject3);
		txtJFMS = (TextView) findViewById(R.id.txtJFMS);
		txtFJFMS = (TextView) findViewById(R.id.txtFJFMS);
		txtSubject2 = (TextView) findViewById(R.id.txtSubject2);
		txtSubject3 = (TextView) findViewById(R.id.txtSubject3);
		txtCoachName = (TextView) findViewById(R.id.txtCoachName);
		txtCoachCard = (TextView) findViewById(R.id.txtCoachCard);
		txtStudentName = (TextView) findViewById(R.id.txtStudentName);
		txtStudentCard = (TextView) findViewById(R.id.txtStudentCard);
		imgCoach = (NetImageView) findViewById(R.id.imgCoach);
		imgStudent = (NetImageView) findViewById(R.id.imgStudent);
		txtStartTime = (TextView) findViewById(R.id.txtStartTime);
		txtTrainTime = (TextView) findViewById(R.id.txtTrainTime);
		txtBalance = (TextView) findViewById(R.id.txtBalance);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtUploadUseDataStatus = (TextView) findViewById(R.id.txtUploadUseDataStatus);
		txtLngLat = (TextView) findViewById(R.id.txtLngLat);
		txtNetworkStatus = (TextView) findViewById(R.id.txtNetworkStatus);
		Bundle bundle = this.getIntent().getExtras();
		deviceID = bundle.getString("deviceID");
		deviceName = bundle.getString("deviceName");
		schoolID = bundle.getString("schoolID");
		schoolName = bundle.getString("schoolName");
		session = bundle.getString("session");
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm:ss");
		txtSystemTime.setText(dateFormat.format(new Date()));

		settings = getSharedPreferences("whzxt.net", 0);

		server = getString(R.string.server1);

		sqlHelper = new MySQLHelper(this, "zxt.db", null, 3);
		db = sqlHelper.getWritableDatabase();

		// 科目二
		btnSubject2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (subject == 3) {
					subject = 2;
					btnSubject3.setBackgroundResource(R.drawable.button_bg);
					txtSubject3.setTextColor(R.color.button_text);
					btnSubject2.setBackgroundResource(R.drawable.button_checked_bg);
					txtSubject2.setTextColor(Color.WHITE);
				}
			}
		});
		// 科目三
		btnSubject3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (subject == 2) {
					subject = 3;
					btnSubject2.setBackgroundResource(R.drawable.button_bg);
					txtSubject2.setTextColor(R.color.button_text);
					btnSubject3.setBackgroundResource(R.drawable.button_checked_bg);
					txtSubject3.setTextColor(Color.WHITE);
				}
			}
		});
		// 计费模式
		btnJFMS.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mode == 1) {
					trainFinish();//结束训练重新开始
					mode = 0;
					btnFJFMS.setBackgroundResource(R.drawable.button_bg);
					txtFJFMS.setTextColor(R.color.button_text);
					btnJFMS.setBackgroundResource(R.drawable.button_checked_bg);
					txtJFMS.setTextColor(Color.WHITE);
				}
			}
		});
		// 非计费模式
		btnFJFMS.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (mode == 0) {
					final EditText txtpsd = new EditText(MainActivity.this);
					txtpsd.setText(settings.getString("offlinepassword", ""));
					AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("请输入密码").setIcon(android.R.drawable.ic_menu_help).setView(txtpsd).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (txtpsd.getText().toString().toLowerCase().equals(settings.getString("offlinepassword", "").toLowerCase())) {
								trainFinish();//结束训练重新开始
								mode = 1;
								btnJFMS.setBackgroundResource(R.drawable.button_bg);
								txtJFMS.setTextColor(R.color.button_text);
								btnFJFMS.setBackgroundResource(R.drawable.button_checked_bg);
								txtFJFMS.setTextColor(Color.WHITE);
							} else {
								Toast.makeText(MainActivity.this, "密码错误", Toast.LENGTH_LONG).show();
							}
						}
					}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					}).create();
					alertDialog.show();
				}
			}
		});

		// 初始化读卡器
		try {
			fd = Native.auto_init(PATH, BAUD);
		} catch (Exception e) {
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("读卡器初始化失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			}).create();
			alertDialog.show();
			return;
		}

		// 初始化GPS
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("请打开GPS").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent();
					intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
				}
			}).create();
			alertDialog.show();
		}
		// 定时30秒上传
		_timerUpload = new Timer();
		_timerUpload.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 1;
				handler.sendMessage(msg);
			}
		}, 10000, 30000);
		// 文字闪烁
		_timerFlicker = new Timer();
		_timerFlicker.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 2;
				handler.sendMessage(msg);
			}
		}, 1, 200);
		// 每秒执行
		_timerSecond = new Timer();
		_timerSecond.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 3;
				handler.sendMessage(msg);
			}
		}, 1000, 1000);
		// 每分钟执行
		_timerMinute = new Timer();
		_timerMinute.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 4;
				handler.sendMessage(msg);
			}
		}, 15000, 60000);
		// 显示教练信息
		if (settings.getString("coachCard", "") != "") {
			coachID = settings.getString("coachID", "");
			coachName = settings.getString("coachName", "");
			coachCard = settings.getString("coachCard", "");
			coachIDCard = settings.getString("coachIDCard", "");
			showCoach(true);
		} else {
			showCoach(false);
		}
	}

	/**
	 * 文字闪烁
	 */
	private void flicker() {
		switch (col) {
		case 0:
			if (txtInfoCoach.getVisibility() == View.VISIBLE)
				txtInfoCoach.setTextColor(Color.TRANSPARENT);
			if (txtInfoStudent.getVisibility() == View.VISIBLE)
				txtInfoStudent.setTextColor(Color.TRANSPARENT);
			break;
		case 1:
			if (txtInfoCoach.getVisibility() == View.VISIBLE)
				txtInfoCoach.setTextColor(Color.YELLOW);
			if (txtInfoStudent.getVisibility() == View.VISIBLE)
				txtInfoStudent.setTextColor(Color.YELLOW);
			break;
		case 2:
			if (txtInfoCoach.getVisibility() == View.VISIBLE)
				txtInfoCoach.setTextColor(Color.RED);
			if (txtInfoStudent.getVisibility() == View.VISIBLE)
				txtInfoStudent.setTextColor(Color.RED);
			break;
		case 3:
			if (txtInfoCoach.getVisibility() == View.VISIBLE)
				txtInfoCoach.setTextColor(Color.BLUE);
			if (txtInfoStudent.getVisibility() == View.VISIBLE)
				txtInfoStudent.setTextColor(Color.BLUE);
			break;
		default:
			break;
		}
		if (col++ > 3) {
			col = 0;
		}
	}

	/**
	 * 获取时间差
	 * 
	 * @param 开始时间
	 * @param 结束时间
	 * @return
	 */
	private String getTimeDiff(Date start, Date end) {
		long between = (end.getTime() - start.getTime()) / 1000;
		// long day=between/(24*3600);
		// long hour = between % (24 * 3600) / 3600;
		long hour = between / 3600;
		long minute = between % 3600 / 60;
		long second = between % 60;
		return hour + "小时" + minute + "分" + second + "秒";
	}

	/**
	 * 每秒执行
	 */
	private void second() {
		// 更新时间
		nowTime = new Date();
		txtSystemTime.setText(dateFormat.format(nowTime));
		if (null != startTime) {
			if (mode == 0) {
				if (coachCard.equals(studentCard)) {
					txtTrainTime.setText("已用车" + getTimeDiff(startTime, nowTime));
					cardBalanceNow = cardBalance - (int) ((nowTime.getTime() - startTime.getTime()) / 60000);
					if (((nowTime.getTime() - startTime.getTime()) / 1000) % 60 > 0) {
						cardBalanceNow--;
					}
					txtBalance.setText("卡内剩余时长:" + cardBalanceNow + "分钟");
				} else {
					txtTrainTime.setText("已训练" + getTimeDiff(startTime, nowTime));
					cardBalanceNow = cardBalance - (int) ((nowTime.getTime() - startTime.getTime()) / 60000) * 2;
					if (((nowTime.getTime() - startTime.getTime()) / 1000) % 60 > 0) {
						cardBalanceNow = cardBalanceNow - 2;
					}
					txtBalance.setText("卡内余额:" + cardBalanceNow + "元");
				}
			} else {
				txtTrainTime.setText("非计费模式");
				txtBalance.setText("");
			}
		}
		// 判断是否插卡
		if (fd > 0) {
			if (Native.chk_24c02(fd) == 0) {// AT24C02卡
				if (!_hascard) {
					_hascard = true;
					if (studentCard.equals("")) {
						String data = Native.srd_24c02(fd, 0x08, 0x20);
						String[] chars = data.split(" ");
						// 卡号
						String card = String.valueOf((char) Integer.parseInt(chars[8], 16));
						for (int i = 9; i < 16; i++) {
							card += String.valueOf((char) Integer.parseInt(chars[i], 16));
						}
						if (chars[0].equals("01")) {
							if (retry < 3) {
								getCoachInfo(card);
							} else {
								// 驾校ID
								String cardschool = String.valueOf((char) Integer.parseInt(chars[24], 16));
								for (int i = 25; i < 32; i++) {
									cardschool += String.valueOf((char) Integer.parseInt(chars[i], 16));
								}
								// 兼容老卡
								if (cardschool.equals("11111111")) {
									cardschool = "001001";
								}
								if (!cardschool.equals(schoolID)) {
									Toast.makeText(MainActivity.this, "此卡不属于本驾校", Toast.LENGTH_LONG).show();
								} else {
									try {
										coachName = new String(int2bytes(Integer.parseInt(chars[16].toString() + chars[17].toString(), 16)), "GB2312").trim();
										for (int i = 18; i < 24; i++) {
											coachName += new String(int2bytes(Integer.parseInt(chars[i].toString() + chars[++i].toString(), 16)), "GB2312").trim();
										}
										coachCard = card;
										coachID = "";
										coachIDCard = "";
										showCoach(true);
										studentCard = coachCard;
										studentID = coachID;
										studentName = coachName;
										studentIDCard = coachIDCard;
										startTime = new Date();
										showStudent(true);
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
							}
						} else {
							if (!coachCard.equals("")) {
								if (retry < 3) {
									getStudentInfo(card);
								} else {
									// 驾校ID
									String cardschool = String.valueOf((char) Integer.parseInt(chars[24], 16));
									for (int i = 25; i < 32; i++) {
										cardschool += String.valueOf((char) Integer.parseInt(chars[i], 16));
									}
									// 兼容老卡
									if (cardschool.equals("11111111")) {
										cardschool = "001001";
									}
									if (!cardschool.equals(schoolID)) {
										Toast.makeText(MainActivity.this, "此卡不属于本驾校", Toast.LENGTH_LONG).show();
									} else {
										try {
											studentName = new String(int2bytes(Integer.parseInt(chars[16].toString() + chars[17].toString(), 16)), "GB2312").trim();
											for (int i = 18; i < 24; i++) {
												studentName += new String(int2bytes(Integer.parseInt(chars[i].toString() + chars[++i].toString(), 16)), "GB2312").trim();
											}
											studentCard = card;
											studentID = "";
											studentIDCard = "";
											startTime = new Date();
											showStudent(true);
										} catch (UnsupportedEncodingException e) {
											e.printStackTrace();
										}
									}
								}
							} else {
								Toast.makeText(MainActivity.this, "请先插教练卡", Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
			} else if (Native.chk_102(fd) == 0) {
				// 102卡
			} else {
				trainFinish();
			}
		}
	}

	/**
	 * 结束训练
	 */
	private void trainFinish() {
		_hascard = false;
		if (!studentCard.equals("")) {
			if (coachCard.equals(studentCard)) {
				txtStartTime.setText("本次用车已结束");
				txtTrainTime.setText("共使用" + getTimeDiff(startTime, nowTime));
			} else {
				txtStartTime.setText("本次训练已结束");
				txtTrainTime.setText("共训练" + getTimeDiff(startTime, nowTime));
			}
			minute();// 更新结束时间
			studentID = "";
			studentName = "";
			studentCard = "";
			showStudent(false);
			startTime = null;
			endTime = null;
			_curUUID = "";
		}
	}
	/**
	 * int转换为byte[]
	 * 
	 * @param i
	 * @return
	 */
	private byte[] int2bytes(int i) {
		byte[] result = new byte[4];
		result[0] = (byte) ((i >> 24) & 0xFF);
		result[1] = (byte) ((i >> 16) & 0xFF);
		result[2] = (byte) ((i >> 8) & 0xFF);
		result[3] = (byte) (i & 0xFF);
		return result;
	}

	/**
	 * 每分钟执行
	 */
	private void minute() {
		// 记录/更新当前训练结束时间、余额
		if (null != startTime && !coachCard.equals("") && !studentCard.equals("") && mode == 0) {
			if (null != endTime) {
				if (startTime.after(endTime)) {
					endTime = new Date();
					_curUUID = UUID.randomUUID().toString();
					ContentValues tcv = new ContentValues();
					tcv.put("guid", _curUUID);
					tcv.put("coach", coachCard);
					tcv.put("student", studentCard);
					tcv.put("starttime", dateFormat.format(startTime));
					tcv.put("endtime", dateFormat.format(endTime));
					db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
				} else {
					endTime = new Date();
					ContentValues tcv = new ContentValues();
					tcv.put("endtime", dateFormat.format(endTime));
					db.update(MySQLHelper.T_ZXT_USE_DATA, tcv, "guid=?", new String[] { _curUUID });
				}
			} else {
				endTime = new Date();
				_curUUID = UUID.randomUUID().toString();
				ContentValues tcv = new ContentValues();
				tcv.put("guid", _curUUID);
				tcv.put("coach", coachCard);
				tcv.put("student", studentCard);
				tcv.put("starttime", dateFormat.format(startTime));
				tcv.put("endtime", dateFormat.format(endTime));
				db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
			}
		}

		if (retry < 3 && blindSpotFinish) {
			blindSpotFinish = !blindSpotFinish;
			uploadUseData();
		}
	}

	/**
	 * 上传GPS盲点数据
	 */
	private void uploadBlindSpot() {
		_cursor = db.query(MySQLHelper.T_ZXT_GPS_DATA, null, null, null, null, null, null);
		if (_cursor.getCount() > 0) {
			new Thread() {
				public void run() {
					HttpPost httpRequest = new HttpPost(server + "/blindspot.ashx");
					List<NameValuePair> params = new ArrayList<NameValuePair>(6);
					_cursor.moveToFirst();
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
					params.add(new BasicNameValuePair("gpstime", _cursor.getString(_cursor.getColumnIndex("gpstime"))));
					params.add(new BasicNameValuePair("lng", _cursor.getString(_cursor.getColumnIndex("lng"))));
					params.add(new BasicNameValuePair("lat", _cursor.getString(_cursor.getColumnIndex("lat"))));
					try {
						httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							usedataresult = EntityUtils.toString(httpResponse.getEntity());
							handler.post(new Runnable() {
								public void run() {
									if (usedataresult.equals("s")) {
										db.delete(MySQLHelper.T_ZXT_GPS_DATA, "gpstime=?", new String[] { _cursor.getString(_cursor.getColumnIndex("gpstime")) });
										uploadBlindSpot();
									}
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}.start();
		} else {
			blindSpotFinish = true;
		}
	}

	/**
	 * 上传训练数据
	 */
	private void uploadUseData() {
		_cursor = db.query(MySQLHelper.T_ZXT_USE_DATA, null, "guid!='" + _curUUID + "'", null, null, null, null);
		if (_cursor.getCount() > 0) {
			txtUploadUseDataStatus.setText("正在上传训练数据");
			new Thread() {
				public void run() {
					HttpPost httpRequest = new HttpPost(server + "/usedata.ashx");
					List<NameValuePair> params = new ArrayList<NameValuePair>(8);
					_cursor.moveToFirst();
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
					params.add(new BasicNameValuePair("guid", _cursor.getString(_cursor.getColumnIndex("guid"))));
					params.add(new BasicNameValuePair("coach", _cursor.getString(_cursor.getColumnIndex("coach"))));
					params.add(new BasicNameValuePair("student", _cursor.getString(_cursor.getColumnIndex("student"))));
					params.add(new BasicNameValuePair("starttime", _cursor.getString(_cursor.getColumnIndex("starttime"))));
					params.add(new BasicNameValuePair("endtime", _cursor.getString(_cursor.getColumnIndex("endtime"))));
					try {
						httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							usedataresult = EntityUtils.toString(httpResponse.getEntity());
							handler.post(new Runnable() {
								public void run() {
									if (usedataresult.equals("s")) {
										db.delete(MySQLHelper.T_ZXT_USE_DATA, "guid=?", new String[] { _cursor.getString(_cursor.getColumnIndex("guid")) });
										txtUploadUseDataStatus.setText("训练数据上传成功[" + getNowTime() + "]");
										uploadUseData();
									}
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
						handler.post(new Runnable() {
							public void run() {
								txtUploadUseDataStatus.setText("网络异常[" + getNowTime() + "]");
							}
						});
					}
				}
			}.start();
		} else {
			uploadBlindSpot();// 上传GPS盲点
		}
	}

	/**
	 * 获取教练信息
	 * 
	 * @param 卡号
	 */
	private void getCoachInfo(final String card) {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/getcoachinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", card)); // 卡号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (cardresult.startsWith("s|")) {
									String[] results = cardresult.split("\\|");
									coachCard = results[1];
									cardBalance = Integer.parseInt(results[2]);
									coachID = results[3];
									coachName = results[4];
									if (results.length > 5) {
										coachIDCard = results[5];
									} else {
										coachIDCard = "";
									}
									showCoach(true);
									studentCard = coachCard;
									studentID = coachID;
									studentName = coachName;
									studentIDCard = coachIDCard;
									startTime = new Date();
									showStudent(true);
								} else {
									Toast.makeText(MainActivity.this, cardresult.split("\\|")[1], Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, "网络错误,暂时无法获取教练信息,请尝试重新插卡", Toast.LENGTH_LONG).show();
						}
					});
					retry++;
					changeServer();
				}
			}
		}.start();
	}

	/**
	 * 获取学员信息
	 * 
	 * @param 卡号
	 */
	private void getStudentInfo(final String card) {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/getstudentinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", card)); // 学员卡号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (cardresult.startsWith("s|")) {
									String[] results = cardresult.split("\\|");
									studentCard = results[1];
									cardBalance = Integer.parseInt(results[2]);
									studentID = results[3];
									studentName = results[4];
									if (results.length > 5) {
										studentIDCard = results[5];
									}
									startTime = new Date();
									showStudent(true);
								} else {
									Toast.makeText(MainActivity.this, cardresult.split("\\|")[1], Toast.LENGTH_LONG).show();
								}
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.post(new Runnable() {
						public void run() {
							Toast.makeText(MainActivity.this, "网络错误,暂时无法获取学员信息,请尝试重新插卡", Toast.LENGTH_LONG).show();
						}
					});
					retry++;
					changeServer();
				}
			}
		}.start();
	}

	/**
	 * 上传GPS数据
	 */
	private void upload() {
		txtStatus.setText("正在上传GPS数据[" + getNowTime() + "]");
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/upload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(6);
				if (studentCard.equals("")) {
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", coachCard)); // 教练
				} else {
					params = new ArrayList<NameValuePair>(9);
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", coachCard)); // 教练
					params.add(new BasicNameValuePair("student", studentCard)); // 学员
					params.add(new BasicNameValuePair("starttime", dateFormat.format(startTime))); // 开始训练时间
					params.add(new BasicNameValuePair("balance", String.valueOf(cardBalanceNow))); // 余额
				}
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						uploadresult = EntityUtils.toString(httpResponse.getEntity());
						retry = 0;
						handler.post(new Runnable() {
							public void run() {
								txtNetworkStatus.setText("网络正常");
								txtNetworkStatus.setTextColor(Color.BLACK);
								if (uploadresult.equals("success")) {
									txtStatus.setText("GPS数据上传成功[" + getNowTime() + "]");
								} else {
									txtStatus.setText(uploadresult + "[" + getNowTime() + "]");
									ContentValues tcv = new ContentValues();
									tcv.put("gpstime", dateFormat.format(new Date()));
									tcv.put("lng", String.format("%.6f", lng));
									tcv.put("lat", String.format("%.6f", lat));
									db.insert(MySQLHelper.T_ZXT_GPS_DATA, null, tcv);
								}
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					retry++;
					handler.post(new Runnable() {
						public void run() {
							txtStatus.setText("网络异常(" + retry + ")[" + getNowTime() + "]");
							if (retry == 3) {
								txtNetworkStatus.setText("网络异常");
								txtNetworkStatus.setTextColor(Color.RED);
							}
							ContentValues tcv = new ContentValues();
							tcv.put("gpstime", dateFormat.format(new Date()));
							tcv.put("lng", String.format("%.6f", lng));
							tcv.put("lat", String.format("%.6f", lat));
							db.insert(MySQLHelper.T_ZXT_GPS_DATA, null, tcv);
						}
					});
					changeServer();
				}
			}
		}.start();
	}

	/**
	 * 显示(隐藏)教练信息
	 * 
	 * @param 显示True
	 *            ,隐藏False
	 */
	private void showCoach(Boolean b) {
		if (b) {
			layCoachTitle.setBackgroundResource(R.drawable.bg2);
			txtInfoStudent.setText("请插学员卡");
			txtCoachName.setText(coachName);
			txtCoachCard.setText(coachCard);
			layCoachInfo.setVisibility(View.VISIBLE);
			txtInfoCoach.setVisibility(View.GONE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", coachID);
			editor.putString("coachCard", coachCard);
			editor.putString("coachName", coachName);
			editor.putString("coachIDCard", coachIDCard);
			editor.commit();
			if (!coachIDCard.equals("")) {
				imgCoach.setImageUrl(server + "/" + coachIDCard + ".bmp");
			}
		} else {
			layCoachTitle.setBackgroundResource(R.drawable.bg1);
			txtInfoStudent.setText("请先插教练卡");
			layCoachInfo.setVisibility(View.GONE);
			txtInfoCoach.setVisibility(View.VISIBLE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", "");
			editor.putString("coachCard", "");
			editor.putString("coachName", "");
			editor.putString("coachIDCard", "");
			editor.commit();
			imgCoach.setImageResource(R.drawable.photo);
		}
	}

	/**
	 * 显示(隐藏)学员信息
	 * 
	 * @param 显示True
	 *            ,隐藏False
	 */
	private void showStudent(Boolean b) {
		if (b) {
			layStudentTitle.setBackgroundResource(R.drawable.bg2);
			txtStudentName.setText(studentName);
			txtStudentCard.setText(studentCard);
			layStudentInfo.setVisibility(View.VISIBLE);
			txtInfoStudent.setVisibility(View.GONE);
			if (coachCard.equals(studentCard)) {
				txtStudentTitle.setText("驾驶员");
				txtStartTime.setText("开始用车时间: " + timeFormat.format(startTime));
			} else {
				txtStudentTitle.setText("学员");
				txtStartTime.setText("训练开始时间: " + timeFormat.format(startTime));
			}
			if (!studentIDCard.equals("")) {
				imgStudent.setImageUrl(server + "/" + studentIDCard + ".bmp");
			}
		} else {
			txtStudentTitle.setText("学员");
			layStudentTitle.setBackgroundResource(R.drawable.bg1);
			layStudentInfo.setVisibility(View.GONE);
			txtInfoStudent.setVisibility(View.VISIBLE);
			imgStudent.setImageResource(R.drawable.photo);
		}
	}

	/**
	 * 获取经纬度
	 */
	private void getLngLat() {
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			lng = location.getLongitude();
			lat = location.getLatitude();
			txtLngLat.setText("GPS:" + String.format("%.6f", lng) + "," + String.format("%.6f", lat));
		}
	}

	/**
	 * 获取当前时间字符串
	 * 
	 * @return
	 */
	private String getNowTime() {
		return timeFormat.format(new Date());
	}

	/**
	 * 切换服务器
	 */
	private void changeServer() {
		if (server.equals(getString(R.string.server1))) {
			server = getString(R.string.server2);
		} else {
			server = getString(R.string.server1);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			exitConfirm();
		}
		return false;
	}

	private void exitConfirm() {
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("确定要退出程序？").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				MainActivity.this.finish();
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		}).create();
		alertDialog.show();
	}

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// log it when the location changes
			if (location != null) {
				lng = location.getLongitude();
				lat = location.getLatitude();
				txtLngLat.setText("GPS:" + String.format("%.6f", lng) + "," + String.format("%.6f", lat));
			}
		}

		public void onProviderDisabled(String provider) {
			// Provider被disable时触发此函数，比如GPS被关闭
		}

		public void onProviderEnabled(String provider) {
			// Provider被enable时触发此函数，比如GPS被打开
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
		}
	};

	@Override
	protected void onDestroy() {
		Native.ic_exit(fd);
		_timerUpload.cancel();
		_timerFlicker.cancel();
		_timerSecond.cancel();
		if (db != null)
			db.close();
		super.onDestroy();
	}
}