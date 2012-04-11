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

import android.R.bool;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtInfoCoach, txtInfoStudent, txtCard, txtStatus, txtLngLat;
	private TextView txtCoachName;
	private ImageView imgCoach;
	private LinearLayout layCoachTitle;
	private Button btnCoachLogout;
	private String deviceID, deviceName, schoolName, session;
	private String server;
	private String uploadresult;
	private int col = 0;
	private double lng, lat;
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerFlicker, _timerSecond;

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
		txtCoachName = (TextView) findViewById(R.id.txtCoachName);
		imgCoach = (ImageView) findViewById(R.id.imgCoach);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtLngLat = (TextView) findViewById(R.id.txtLngLat);
		btnCoachLogout = (Button) findViewById(R.id.btnCoachLogout);
		btnCoachLogout.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showCoach(false);
			}
		});
		Bundle bundle = this.getIntent().getExtras();
		deviceID = bundle.getString("deviceID");
		deviceName = bundle.getString("deviceName");
		schoolName = bundle.getString("schoolName");
		session = bundle.getString("session");
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm");
		txtSystemTime.setText(dateFormat.format(new Date()));

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
	}

	// 文字闪烁
	private void flicker() {
		switch (col) {
		case 0:
			txtInfoCoach.setTextColor(Color.TRANSPARENT);
			txtInfoStudent.setTextColor(Color.TRANSPARENT);
			break;
		case 1:
			txtInfoCoach.setTextColor(Color.YELLOW);
			txtInfoStudent.setTextColor(Color.YELLOW);
			break;
		case 2:
			txtInfoCoach.setTextColor(Color.RED);
			txtInfoStudent.setTextColor(Color.RED);
			break;
		case 3:
			txtInfoCoach.setTextColor(Color.BLUE);
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
				if (txtCoachName.getVisibility() == View.GONE) {
					showCoach(true);
				}
			} else if (Native.chk_102(fd) == 0) {
				// 102卡
			}
		}
	}

	// 上传数据
	private void upload() {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/upload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID));
				params.add(new BasicNameValuePair("session", session));
				params.add(new BasicNameValuePair("lng", Double.toString(lng)));
				params.add(new BasicNameValuePair("lat", Double.toString(lat)));
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
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private void showCoach(Boolean b) {
		if (b) {
			layCoachTitle.setBackgroundResource(R.drawable.bg2);
			txtInfoStudent.setText("请插学员卡");
			txtCoachName.setText("黄灿");
			txtCoachName.setVisibility(View.VISIBLE);
			imgCoach.setVisibility(View.VISIBLE);
			btnCoachLogout.setVisibility(View.VISIBLE);
			txtInfoCoach.setVisibility(View.GONE);
		} else {
			layCoachTitle.setBackgroundResource(R.drawable.bg1);
			txtInfoStudent.setText("请先插教练卡");
			txtCoachName.setVisibility(View.GONE);
			imgCoach.setVisibility(View.GONE);
			btnCoachLogout.setVisibility(View.GONE);
			txtInfoCoach.setVisibility(View.VISIBLE);
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