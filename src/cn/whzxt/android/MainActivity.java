package cn.whzxt.android;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtInfoCoach, txtInfoStudent, txtStatus, txtLngLat;
	private TextView txtCoachName, txtCoachCard, txtStudentName, txtStudentCard;
	private TextView txtStartTime, txtTrainTime;
	private ImageView imgCoach, imgStudent;
	private LinearLayout layCoachTitle, layStudentTitle, layCoachInfo, layStudentInfo;
	private Button btnCoachLogout;
	private String deviceID, deviceName, schoolID, schoolName, session;
	private String server;
	private String uploadresult, cardresult;
	private String coachID = "", coachName, coachCard, studentID = "", studentCard, studentName;
	private int col = 0;
	private Date startTime;
	private double lng, lat;
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerFlicker, _timerSecond;
	private SharedPreferences settings;

	private static final String PATH = "/dev/s3c2410_serial1";
	private static final int BAUD = 9600;
	private int fd;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 1:
				txtStatus.setText("正在上传数据[" + getNowTime() + "]");
				getLngLat();
				upload();
				break;
			case 2:
				flicker();
				break;
			case 3:
				second();
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
		txtInfoStudent = (TextView) findViewById(R.id.txtInfoStudent);
		layCoachTitle = (LinearLayout) findViewById(R.id.layCoachTitle);
		layStudentTitle = (LinearLayout) findViewById(R.id.layStudentTitle);
		layCoachInfo = (LinearLayout) findViewById(R.id.layCoachInfo);
		layStudentInfo = (LinearLayout) findViewById(R.id.layStudentInfo);
		txtCoachName = (TextView) findViewById(R.id.txtCoachName);
		txtCoachCard = (TextView) findViewById(R.id.txtCoachCard);
		txtStudentName = (TextView) findViewById(R.id.txtStudentName);
		txtStudentCard = (TextView) findViewById(R.id.txtStudentCard);
		imgCoach = (ImageView) findViewById(R.id.imgCoach);
		imgStudent = (ImageView) findViewById(R.id.imgStudent);
		txtStartTime = (TextView) findViewById(R.id.txtStartTime);
		txtTrainTime = (TextView) findViewById(R.id.txtTrainTime);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtLngLat = (TextView) findViewById(R.id.txtLngLat);
		btnCoachLogout = (Button) findViewById(R.id.btnCoachLogout);
		btnCoachLogout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (txtInfoStudent.getVisibility() == View.VISIBLE) {
					coachID = "";
					coachName = "";
					coachCard = "";
					showCoach(false);
				} else {
					Toast.makeText(MainActivity.this, "请先取出学员卡", Toast.LENGTH_LONG).show();
				}
			}
		});
		Bundle bundle = this.getIntent().getExtras();
		deviceID = bundle.getString("deviceID");
		deviceName = bundle.getString("deviceName");
		schoolID = "11111111";
		schoolName = bundle.getString("schoolName");
		session = bundle.getString("session");
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm:ss");
		txtSystemTime.setText(dateFormat.format(new Date()));

		settings = getSharedPreferences("whzxt.net", 0);

		server = getString(R.string.server1);

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
		// 显示教练信息
		if (settings.getString("coachID", "") != "") {
			coachID = settings.getString("coachID", "");
			coachName = settings.getString("coachName", "");
			coachCard = settings.getString("coachCard", "");
			showCoach(true);
		}
	}

	// 文字闪烁
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

	// 每秒执行
	private void second() {
		txtSystemTime.setText(dateFormat.format(new Date()));
		if (fd > 0) {
			if (Native.chk_4442(fd) == 0) {
				if (txtInfoCoach.getVisibility() == View.VISIBLE) {
					// String data = Native.srd_4442(fd, 0x00, 0xff);
					coachCard = "00100005";
					new Thread() {
						public void run() {
							HttpPost httpRequest = new HttpPost(server + "/getcoachinfo.ashx");
							List<NameValuePair> params = new ArrayList<NameValuePair>(4);
							params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
							params.add(new BasicNameValuePair("session", session)); // 当前会话
							params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
							params.add(new BasicNameValuePair("card", coachCard)); // 卡号
							try {
								httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
								HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
								if (httpResponse.getStatusLine().getStatusCode() == 200) {
									cardresult = EntityUtils.toString(httpResponse.getEntity());
									handler.post(new Runnable() {
										public void run() {
											if (cardresult.startsWith("s|")) {
												String[] results = cardresult.split("\\|");
												coachID = results[1];
												coachName = results[2];
												showCoach(true);
											} else {
												Toast.makeText(MainActivity.this, cardresult.split("\\|")[1], Toast.LENGTH_LONG).show();
											}
										}
									});
								}
							} catch (Exception e) {
								e.printStackTrace();
								Toast.makeText(MainActivity.this, "网络错误,暂时无法获取教练信息,请尝试重新插卡", Toast.LENGTH_LONG).show();
								changeServer();
							}
						}
					}.start();
				}
			} else if (Native.chk_102(fd) == 0) {
				// 102卡
				studentCard = "00100491";
				if (txtInfoCoach.getVisibility() == View.GONE && txtInfoStudent.getVisibility() == View.VISIBLE) {
					new Thread() {
						public void run() {
							HttpPost httpRequest = new HttpPost(server + "/getstudentinfo.ashx");
							List<NameValuePair> params = new ArrayList<NameValuePair>(4);
							params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
							params.add(new BasicNameValuePair("session", session)); // 当前会话
							params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
							params.add(new BasicNameValuePair("card", studentCard)); // 学员卡号
							try {
								httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
								HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
								if (httpResponse.getStatusLine().getStatusCode() == 200) {
									cardresult = EntityUtils.toString(httpResponse.getEntity());
									handler.post(new Runnable() {
										public void run() {
											if (cardresult.startsWith("s|")) {
												String[] results = cardresult.split("\\|");
												studentID = results[1];
												studentName = results[2];
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
								Toast.makeText(MainActivity.this, "网络错误,暂时无法获取学员信息,请尝试重新插卡", Toast.LENGTH_LONG).show();
								changeServer();
							}
						}
					}.start();
				}
			} else {
				if (txtInfoStudent.getVisibility() == View.GONE) {
					studentID = "";
					studentName = "";
					studentCard = "";
					showStudent(false);
				}
			}
		}
	}

	// 上传数据
	private void upload() {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/upload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(6);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("lng", Double.toString(lng))); // 经度
				params.add(new BasicNameValuePair("lat", Double.toString(lat))); // 纬度
				params.add(new BasicNameValuePair("coach", coachID)); // 教练
				params.add(new BasicNameValuePair("student", studentID)); // 学员
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						uploadresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (uploadresult.equals("success")) {
									txtStatus.setText("数据上传成功[" + getNowTime() + "]");
								} else {
									txtStatus.setText(uploadresult + "[" + getNowTime() + "]");
								}
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					txtStatus.setText("网络异常[" + getNowTime() + "]");
					changeServer();
				}
			}
		}.start();
	}

	// 显示(隐藏)教练信息
	private void showCoach(Boolean b) {
		if (b) {
			layCoachTitle.setBackgroundResource(R.drawable.bg2);
			txtInfoStudent.setText("请插学员卡");
			txtCoachName.setText(coachName);
			txtCoachCard.setText(coachCard);
			layCoachInfo.setVisibility(View.VISIBLE);
			btnCoachLogout.setVisibility(View.VISIBLE);
			txtInfoCoach.setVisibility(View.GONE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", coachID);
			editor.putString("coachCard", coachCard);
			editor.putString("coachName", coachName);
			editor.commit();
			Toast.makeText(MainActivity.this, "教练登签成功,现在可以取出教练卡", Toast.LENGTH_LONG).show();
		} else {
			layCoachTitle.setBackgroundResource(R.drawable.bg1);
			txtInfoStudent.setText("请先插教练卡");
			layCoachInfo.setVisibility(View.GONE);
			btnCoachLogout.setVisibility(View.GONE);
			txtInfoCoach.setVisibility(View.VISIBLE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", "");
			editor.putString("coachCard", "");
			editor.putString("coachName", "");
			editor.commit();
		}
	}

	// 显示(隐藏)学员信息
	private void showStudent(Boolean b) {
		if (b) {
			layStudentTitle.setBackgroundResource(R.drawable.bg2);
			txtStudentName.setText(studentName);
			txtStudentCard.setText(studentCard);
			layStudentInfo.setVisibility(View.VISIBLE);
			txtInfoStudent.setVisibility(View.GONE);
			txtStartTime.setText("训练开始时间:" + timeFormat.format(startTime));
		} else {
			layStudentTitle.setBackgroundResource(R.drawable.bg1);
			layStudentInfo.setVisibility(View.GONE);
			txtInfoStudent.setVisibility(View.VISIBLE);
		}
	}

	private void getLngLat() {
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			lng = location.getLongitude() * 1E6;
			lat = location.getLatitude() * 1E6;
			txtLngLat.setText("GPS:" + lng + "," + lat);
		}
	}

	private String getNowTime() {
		return timeFormat.format(new Date());
	}

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
				lng = location.getLongitude() * 1E6;
				lat = location.getLatitude() * 1E6;
				txtLngLat.setText("GPS:" + lng + "," + lat);
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
		super.onDestroy();
	}
}