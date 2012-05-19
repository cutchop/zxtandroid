package cn.whzxt.android;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener, SurfaceHolder.Callback {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtInfoCoach, txtInfoStudent, txtStudentTitle, txtStatus, txtUploadUseDataStatus, txtLngLat;
	private TextView txtCoachName, txtCoachCard, txtCoachCertificate, txtStudentName, txtStudentCard;
	private TextView txtStartTime, txtTrainTime, txtBalance;
	private TextView txtNetworkStatus;
	private TextView txtJFMS, txtFJFMS, txtSubject2, txtSubject3;
	private TextView txtStudentID, txtStudentIDCard, txtStudentDriverType, txtStudentTotalTime, txtStudentTotalMi;
	private NetImageView imgCoach, imgStudent;
	private LinearLayout layCoachTitle, layStudentTitle, layCoachInfo, layStudentInfo;
	private LinearLayout btnJFMS, btnFJFMS, btnSubject2, btnSubject3;
	private LinearLayout layTts, layScrollDown, layTabSysInfo, layTabDevStatus, laySysInfo, layDevStatus;
	private TextView btnTabSysInfo, btnTabDevStatus, txtSysInfo;
	private TableLayout layTitle;
	private ScrollView mainView;
	private SurfaceView previewSurface;
	private SurfaceHolder previewSurfaceHolder;
	private String deviceID, deviceName, schoolID, schoolName, session;
	private String server;
	private String uploadresult, cardresult, usedataresult, ttsdataresult;
	private String coachID = "", coachName, coachCard = "", coachIDCard = "", coachCertificate = "", studentID = "", studentCard = "", studentIDCard = "", studentName = "", studentDriverType = "";
	private String card;
	private int cardBalance = 0, cardBalanceNow = 0;
	private int studentTotalTime = 0, studentTotalMi = 0;
	private Boolean _hascard = false;
	private int col = 0;
	private Date startTime, nowTime, endTime;
	private int startMi, nowMi = 0;
	private String _curUUID;
	private int mode = 0;// 训练模式0，自由模式1
	private int subject = 2;// 科目二，科目三
	private int retry = 0;
	private double lng, lat;
	private int speed; // 速度
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerFlicker, _timerSecond, _timerMinute, _timerCamera, _timerTakePhoto, _timerUploadPhoto;
	private SharedPreferences settings;
	private MySQLHelper sqlHelper;
	private SQLiteDatabase db;
	private Cursor _cursor;
	private Boolean blindSpotFinish = true;
	private HashMap<String, String> _hashTts;
	private String _ttsVer;
	private String _tcpMsg;

	private static final int DBVERSION = 6;
	private static final int PRICE = 2; // 设备单价
	// 读卡器
	private static final String PATH = "/dev/s3c2410_serial1"; // 读卡器参数
	private static final int BAUD = 9600; // 读卡器参数
	private int fd;
	// TTS
	private TextToSpeech mTts;
	private static final int REQ_TTS_STATUS_CHECK = 0;
	// 指纹
	private lytfingerprint finger;
	private static final int fingerAddress = 0xffffffff;
	private static final int CHAR_BUFFER_A = 0x01;
	final char PS_OK = 0x00;
	// 摄像头
	private Camera _camera;
	private static final int PHOTO_DEF_WIDTH = 720; // 默认照片宽度
	private static final int PHOTO_DEF_HEIGHT = 576; // 默认照片高度
	private static final int PHOTO_DEF_INTERVAL = 30; // 默认拍照间隔(秒)
	private static final String PHOTO_PATH = "/sdcard/zxtphoto";// 照片保存路径
	private int _curCamera = 1;// 当前摄像头
	private Boolean _previewing = false;
	private Boolean _takephotoing = false;
	private Boolean _photouploading = false;
	private String _phototime = "";

	private WakeLock wakeLock;

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
			case 5:
				takephoto();
				break;
			case 6:
				uploadPhoto();
				break;
			case 7:
				takephotoDelay();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			speak("欢迎使用,中信通智能驾培终端");
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_TTS_STATUS_CHECK) {
			if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
				mTts = new TextToSpeech(this, this);
			}
		}
	}

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
		layTts = (LinearLayout) findViewById(R.id.layTts);
		layScrollDown = (LinearLayout) findViewById(R.id.layScrollDown);
		layTabSysInfo = (LinearLayout) findViewById(R.id.layTabSysInfo);
		layTabDevStatus = (LinearLayout) findViewById(R.id.layTabDevStatus);
		btnTabSysInfo = (TextView) findViewById(R.id.btnTabSysInfo);
		btnTabDevStatus = (TextView) findViewById(R.id.btnTabDevStatus);
		txtSysInfo = (TextView) findViewById(R.id.txtSysInfo);
		laySysInfo = (LinearLayout) findViewById(R.id.laySysInfo);
		layDevStatus = (LinearLayout) findViewById(R.id.layDevStatus);
		layTitle = (TableLayout) findViewById(R.id.layTitle);
		mainView = (ScrollView) findViewById(R.id.mainView);
		previewSurface = (SurfaceView) findViewById(R.id.previewSurface);
		txtJFMS = (TextView) findViewById(R.id.txtJFMS);
		txtFJFMS = (TextView) findViewById(R.id.txtFJFMS);
		txtSubject2 = (TextView) findViewById(R.id.txtSubject2);
		txtSubject3 = (TextView) findViewById(R.id.txtSubject3);
		txtCoachName = (TextView) findViewById(R.id.txtCoachName);
		txtCoachCard = (TextView) findViewById(R.id.txtCoachCard);
		txtCoachCertificate = (TextView) findViewById(R.id.txtCoachCertificate);
		txtStudentName = (TextView) findViewById(R.id.txtStudentName);
		txtStudentCard = (TextView) findViewById(R.id.txtStudentCard);
		txtStudentID = (TextView) findViewById(R.id.txtStudentID);
		txtStudentIDCard = (TextView) findViewById(R.id.txtStudentIDCard);
		txtStudentDriverType = (TextView) findViewById(R.id.txtStudentDriverType);
		txtStudentTotalTime = (TextView) findViewById(R.id.txtStudentTotalTime);
		txtStudentTotalMi = (TextView) findViewById(R.id.txtStudentTotalMi);
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
		if (bundle.getString("offline") != null) {
			retry = 3;
			txtNetworkStatus.setText("网络异常");
		}
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm:ss");
		txtSystemTime.setText(dateFormat.format(new Date()));

		settings = getSharedPreferences("whzxt.net", 0);

		server = getString(R.string.server1);

		sqlHelper = new MySQLHelper(this, "zxt.db", null, DBVERSION);
		db = sqlHelper.getWritableDatabase();

		// 科目二
		btnSubject2.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (subject == 3) {
					subject = 2;
					btnSubject3.setBackgroundResource(R.drawable.button_bg);
					txtSubject3.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
					btnSubject2.setBackgroundResource(R.drawable.button_checked_bg);
					txtSubject2.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				}
			}
		});
		// 科目三
		btnSubject3.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (subject == 2) {
					subject = 3;
					btnSubject2.setBackgroundResource(R.drawable.button_bg);
					txtSubject2.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
					btnSubject3.setBackgroundResource(R.drawable.button_checked_bg);
					txtSubject3.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				}
			}
		});

		layTitle.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mainView.fullScroll(View.FOCUS_UP);
			}
		});
		//
		layScrollDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mainView.fullScroll(View.FOCUS_DOWN);
			}
		});
		// 系统消息Tab
		btnTabSysInfo.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				layTabDevStatus.setBackgroundResource(R.drawable.button_bg);
				btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
				layTabSysInfo.setBackgroundResource(R.drawable.button_checked_bg);
				btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				laySysInfo.setVisibility(View.VISIBLE);
				layDevStatus.setVisibility(View.GONE);
			}
		});
		// 设备状态Tab
		btnTabDevStatus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				layTabSysInfo.setBackgroundResource(R.drawable.button_bg);
				btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
				layTabDevStatus.setBackgroundResource(R.drawable.button_checked_bg);
				btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				laySysInfo.setVisibility(View.GONE);
				layDevStatus.setVisibility(View.VISIBLE);
			}
		});
		// 训练状态
		btnJFMS.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				changeMode(0);
			}
		});
		// 自由状态
		btnFJFMS.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				speak("请输入密码");
				final EditText txtpsd = new EditText(MainActivity.this);
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("请输入密码").setIcon(android.R.drawable.ic_menu_help).setView(txtpsd).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (txtpsd.getText().toString().toLowerCase().equals(settings.getString("offlinepassword", "").toLowerCase())) {
							changeMode(1);
						} else {
							toashShow("密码错误");
						}
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				}).create();
				alertDialog.show();
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
			// 打开GPS
			try {
				Intent intent = new Intent();
				intent.setComponent(new ComponentName("cn.whzxt.gps", "cn.whzxt.gps.ZxtOpenGPSActivity"));
				startActivity(intent);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		}, 3000, 200);
		// 每秒执行
		_timerSecond = new Timer();
		_timerSecond.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 3;
				handler.sendMessage(msg);
			}
		}, 2000, 1000);
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
		// 定时拍照
		_timerCamera = new Timer();
		_timerCamera.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 5;
				handler.sendMessage(msg);
			}
		}, 20000, settings.getInt("photo_interval", PHOTO_DEF_INTERVAL) * 1000);
		// 延时拍照
		_timerTakePhoto = new Timer();
		// 上传照片
		_timerUploadPhoto = new Timer();
		_timerUploadPhoto.schedule(new TimerTask() {
			@Override
			public void run() {
				Message msg = new Message();
				msg.what = 6;
				handler.sendMessage(msg);
			}
		}, 25000, 30000);

		//
		showStudent(false);
		// 显示教练信息
		if (settings.getString("coachCard", "") != "") {
			coachID = settings.getString("coachID", "");
			coachName = settings.getString("coachName", "");
			coachCard = settings.getString("coachCard", "");
			coachIDCard = settings.getString("coachIDCard", "");
			coachCertificate = settings.getString("coachCertificate", "");
			showCoach(true);
		} else {
			showCoach(false);
		}

		// TTS
		Intent checkIntent = new Intent();
		try {
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 指纹
		// finger = new lytfingerprint();
		// finger.PSOpenDevice(1, 0, 57600 / 9600, 2);

		// 摄像头
		File photoDir = new File(PHOTO_PATH);
		if (!photoDir.exists()) {
			photoDir.mkdirs();
		}
		previewSurfaceHolder = previewSurface.getHolder();
		previewSurfaceHolder.addCallback(this);
		previewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		ttsButtonInit(); // 语音提示按钮初始化

		socketListen();// 监听指令

		// 屏幕长亮
		PowerManager manager = ((PowerManager) getSystemService(POWER_SERVICE));
		wakeLock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ATAAW");
		wakeLock.acquire();
	}

	/**
	 * 保存照片
	 */
	PictureCallback jpegPictureCallback = new PictureCallback() {
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = 4;
			Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length, options);
			File file = new File(PHOTO_PATH + "/" + _phototime + "_" + _curCamera + "_" + speed + "_" + _curUUID + ".jpg");
			try {
				file.createNewFile();
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
				bitmapPicture.compress(Bitmap.CompressFormat.JPEG, 80, os);
				os.flush();
				os.close();

				if (_curCamera == 1) {
					_curCamera = 2;
					Camera.Parameters parameters = _camera.getParameters();
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
					_camera.setParameters(parameters);
					_timerTakePhoto.schedule(new TimerTask() {
						@Override
						public void run() {
							Message msg = new Message();
							msg.what = 7;
							handler.sendMessage(msg);
						}
					}, 1 * 1000);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			if (_curCamera == 2) {
				_takephotoing = false;
				try {
					if (_camera != null) {
						_camera.startPreview();
						_previewing = true;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	/**
	 * 拍照
	 */
	private void takephoto() {
		if (_camera != null && !_takephotoing && null != startTime) {
			_takephotoing = true;
			_phototime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			_curCamera = 1;
			Camera.Parameters parameters = _camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			_camera.setParameters(parameters);
			_timerTakePhoto.schedule(new TimerTask() {
				@Override
				public void run() {
					Message msg = new Message();
					msg.what = 7;
					handler.sendMessage(msg);
				}
			}, 1 * 1000);
		}
	}

	private void takephotoDelay() {
		_camera.takePicture(null, null, jpegPictureCallback);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (_previewing && _camera != null) {
			_camera.stopPreview();
			_previewing = false;
		}
		try {
			if (_camera != null) {
				_camera.setPreviewDisplay(holder);
				_camera.startPreview();
				_previewing = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			_camera = Camera.open();
			if (_camera != null) {
				Camera.Parameters parameters = _camera.getParameters();
				parameters.setPictureFormat(PixelFormat.JPEG);
				// parameters.setJpegQuality(20);
				// parameters.setJpegThumbnailQuality(1);
				// parameters.setJpegThumbnailSize(0, 0);
				parameters.setPictureSize(settings.getInt("photo_width", PHOTO_DEF_WIDTH), settings.getInt("photo_height", PHOTO_DEF_HEIGHT));
				_camera.setParameters(parameters);
			} else {
				Toast.makeText(MainActivity.this, "摄像头启动失败", Toast.LENGTH_SHORT);
			}
		} catch (Exception e) {
			Toast.makeText(MainActivity.this, "摄像头启动失败", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		if (_camera != null) {
			_camera.stopPreview();
			_camera.release();
			_camera = null;
			_previewing = false;
		}
	}

	/**
	 * 科目三模拟考试按钮初始化
	 */
	private void ttsButtonInit() {
		_hashTts = new HashMap<String, String>();
		_ttsVer = "0";
		Cursor cursor = db.query(MySQLHelper.T_ZXT_TTS, null, "id=0", null, null, null, null);
		if (cursor.getCount() > 0) {
			cursor.moveToFirst();
			_ttsVer = cursor.getString(cursor.getColumnIndex("tts"));
		}
		cursor.close();
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/gettts.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("ver", _ttsVer)); // 版本号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						ttsdataresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (ttsdataresult.startsWith("s|")) {
									String[] results = ttsdataresult.split("\\|");
									if (results.length > 1) {
										db.delete(MySQLHelper.T_ZXT_TTS, null, null);
										for (int i = 1; i < results.length; i++) {
											String[] items = results[i].split("#");
											ContentValues tcv = new ContentValues();
											tcv.put("id", items[0]);
											tcv.put("name", items[1]);
											tcv.put("tts", items[2]);
											db.insert(MySQLHelper.T_ZXT_TTS, null, tcv);
										}
									}
								}
							}
						});
					}
					handler.post(new Runnable() {
						public void run() {
							Cursor cursor = db.query(MySQLHelper.T_ZXT_TTS, null, "id!=0", null, null, null, "id");
							if (cursor.getCount() > 0) {
								cursor.moveToFirst();
								int i = 0;
								do {
									_hashTts.put(cursor.getString(cursor.getColumnIndex("name")), cursor.getString(cursor.getColumnIndex("tts")));
									if (i % 7 == 0) {
										LinearLayout lay = new LinearLayout(MainActivity.this);
										layTts.addView(lay);
									}
									LinearLayout lastChild = (LinearLayout) layTts.getChildAt(layTts.getChildCount() - 1);
									TextView textView = new TextView(MainActivity.this);
									textView.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
									textView.setBackgroundResource(R.drawable.card_bg);
									textView.setTextColor(MainActivity.this.getResources().getColor(R.color.card_text));
									textView.setPadding(3, 3, 3, 3);
									textView.setClickable(true);
									LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
									lp.setMargins(1, 1, 1, 1);
									textView.setLayoutParams(lp);
									textView.setText(cursor.getString(cursor.getColumnIndex("name")));
									textView.setOnClickListener(new OnClickListener() {
										public void onClick(View v) {
											speak(_hashTts.get(((TextView) v).getText().toString()));
										}
									});
									lastChild.addView(textView);
									i++;
								} while (cursor.moveToNext());
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 监听消息
	 */
	private void socketListen() {
		new Thread() {
			public void run() {
				try {
					ServerSocket socket = new ServerSocket(8888);
					while (true) {
						Socket client = socket.accept();
						try {
							BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "gb2312"));
							_tcpMsg = in.readLine();
							if (_tcpMsg != null) {
								handler.post(new Runnable() {
									public void run() {
										if (_tcpMsg.startsWith("cmd:")) {
											String cmd = _tcpMsg.substring(4);
											if (cmd.equals("open_oil")) {
												// 恢复油路
											} else if (cmd.equals("close_oil")) {
												// 切断油路
											} else if (cmd.equals("mode_train")) {
												changeMode(0);// 切换训练状态
											} else if (cmd.equals("mode_free")) {
												changeMode(1);// 切换自由状态
											} else if (cmd.equals("take_photo")) {
												takephoto();// 拍照
											} else if (cmd.startsWith("set_photo_size:")) {
												try {
													// 设置照片尺寸
													cmd = cmd.replace("set_photo_size:", "");
													int pwidth = Integer.valueOf(cmd.split(",")[0]);
													int pheight = Integer.valueOf(cmd.split(",")[1]);
													if (_camera != null) {
														Camera.Parameters parameters = _camera.getParameters();
														parameters.setPictureSize(pwidth, pheight);
														_camera.setParameters(parameters);
														SharedPreferences.Editor editor = settings.edit();
														editor.putInt("photo_width", pwidth);
														editor.putInt("photo_height", pheight);
														editor.commit();
													}
												} catch (Exception e) {
													e.printStackTrace();
												}
											} else if (cmd.startsWith("set_photo_interval:")) {
												/*
												 * try { // 设置拍照间隔 cmd =
												 * cmd.replace
												 * ("set_photo_interval:", "");
												 * int pinterval =
												 * Integer.valueOf(cmd);
												 * _timerCamera.cancel();
												 * _timerCamera.schedule(new
												 * TimerTask() {
												 * 
												 * @Override public void run() {
												 * Message msg = new Message();
												 * msg.what = 5;
												 * handler.sendMessage(msg); }
												 * }, 5000, pinterval * 1000);
												 * SharedPreferences.Editor
												 * editor = settings.edit();
												 * editor
												 * .putInt("photo_interval",
												 * pinterval); editor.commit();
												 * } catch (Exception e) {
												 * e.printStackTrace(); }
												 */
											}
										} else if (_tcpMsg.startsWith("sms:")) {
											txtSysInfo.setText(_tcpMsg.substring(4));
											layTabDevStatus.setBackgroundResource(R.drawable.button_bg);
											btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
											layTabSysInfo.setBackgroundResource(R.drawable.button_checked_bg);
											btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
											laySysInfo.setVisibility(View.VISIBLE);
											layDevStatus.setVisibility(View.GONE);
											toashShow("收到新短信:" + txtSysInfo.getText().toString());
										}
									}
								});
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
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
	 * @return 时间差字符串
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
				// 计算余额
				cardBalanceNow = cardBalance - (int) ((nowTime.getTime() - startTime.getTime()) / 60000);
				if (((nowTime.getTime() - startTime.getTime()) / 1000) % 60 > 0) {
					cardBalanceNow--;
				}
				// 显示余额
				if (coachCard.equals(studentCard)) {
					txtTrainTime.setText("已用车" + getTimeDiff(startTime, nowTime));
					txtBalance.setText("卡内剩余时长:" + cardBalanceNow + "分钟");
				} else {
					txtTrainTime.setText("已训练" + getTimeDiff(startTime, nowTime));
					txtBalance.setText("余额:" + cardBalanceNow * PRICE + "元,剩余" + cardBalanceNow + "分钟");
				}
				if (cardBalanceNow <= 0) {
					trainFinish();// 结束训练
				}
			} else {
				txtTrainTime.setText("自由模式");
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
						card = String.valueOf((char) Integer.parseInt(chars[8], 16));
						for (int i = 9; i < 16; i++) {
							card += String.valueOf((char) Integer.parseInt(chars[i], 16));
						}
						// 卡内剩余时长
						cardBalance = Integer.parseInt(chars[1].toString() + chars[2].toString(), 16);
						// Toast.makeText(MainActivity.this,
						// String.valueOf(cardBalance),
						// Toast.LENGTH_LONG).show();
						if (chars[0].equals("01")) {
							if (retry < 3) {
								getCoachInfo();
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
									toashShow("此卡不属于本驾校");
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
										trainBegin();
									} catch (UnsupportedEncodingException e) {
										e.printStackTrace();
									}
								}
							}
						} else {
							if (!coachCard.equals("")) {
								if (retry < 3) {
									getStudentInfo();
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
										toashShow("此卡不属于本驾校");
									} else {
										try {
											studentName = new String(int2bytes(Integer.parseInt(chars[16].toString() + chars[17].toString(), 16)), "GB2312").trim();
											for (int i = 18; i < 24; i++) {
												studentName += new String(int2bytes(Integer.parseInt(chars[i].toString() + chars[++i].toString(), 16)), "GB2312").trim();
											}
											studentCard = card;
											studentID = "";
											studentIDCard = "";
											trainBegin();
										} catch (UnsupportedEncodingException e) {
											e.printStackTrace();
										}
									}
								}
							} else {
								toashShow("请先插教练卡");
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
	 * 搜索指纹
	 * 
	 * @return
	 */
	private Boolean searchFinger() {
		Toast.makeText(MainActivity.this, "请按指纹", Toast.LENGTH_LONG).show();
		while (finger.PSGetImage(fingerAddress) == 2) {
			SystemClock.sleep(30);
		}
		SystemClock.sleep(10);
		if (finger.PSGenChar(fingerAddress, CHAR_BUFFER_A) != PS_OK) {
			return false;
		}
		SystemClock.sleep(1000);
		if (finger.PSSearch(fingerAddress, CHAR_BUFFER_A, 0, 50, 0) != PS_OK) {
			return false;
		}
		return true;
	}

	/**
	 * 开始训练
	 */
	private void trainBegin() {
		startTime = new Date();
		startMi = nowMi;
		showStudent(true);
		String _tmp = "早上好";
		if (startTime.getHours() >= 8 && startTime.getHours() < 12) {
			_tmp = "上午好";
		} else if (startTime.getHours() >= 12 && startTime.getHours() < 14) {
			_tmp = "中午好";
		} else if (startTime.getHours() >= 14 && startTime.getHours() < 20) {
			_tmp = "下午好";
		} else if (startTime.getHours() >= 20 || startTime.getHours() < 5) {
			_tmp = "晚上好";
		}
		if (mode == 0) {
			if (coachCard.equals(studentCard)) {
				speak(_tmp + "," + studentName + ",卡内剩余" + (cardBalance - 1) + "分钟,请谨慎驾驶");
			} else {
				speak(_tmp + "," + studentName + ",卡内余额:" + ((cardBalance - 1) * PRICE) + "元,剩余" + (cardBalance - 1) + "分钟,请谨慎驾驶");
			}
			writeBalance(cardBalance - 1);// 重写卡内余额
			takephoto();// 拍照
		}
	}

	/**
	 * 结束训练
	 */
	private void trainFinish() {
		_hascard = false;
		if (!studentCard.equals("") && null != startTime) {
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
			minute();// 上传数据
			speak(txtStartTime.getText().toString() + "," + txtTrainTime.getText().toString() + "," + txtBalance.getText().toString());
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
					tcv.put("balance", String.valueOf(cardBalanceNow));
					tcv.put("startmi", String.valueOf(startMi));
					tcv.put("endmi", String.valueOf(nowMi));
					tcv.put("subject", String.valueOf(subject));
					db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
				} else {
					endTime = new Date();
					ContentValues tcv = new ContentValues();
					tcv.put("endtime", dateFormat.format(endTime));
					tcv.put("balance", String.valueOf(cardBalanceNow));
					tcv.put("endmi", String.valueOf(nowMi));
					tcv.put("subject", String.valueOf(subject));
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
				tcv.put("balance", String.valueOf(cardBalanceNow));
				tcv.put("startmi", String.valueOf(startMi));
				tcv.put("endmi", String.valueOf(nowMi));
				tcv.put("subject", String.valueOf(subject));
				db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
			}

			writeBalance(cardBalanceNow);// 重写卡内余额

			// 余额不足提醒
			if (cardBalanceNow <= 4) {
				toashShow("卡内剩余时长不足" + cardBalanceNow + "分钟,请注意");
			}
		}

		if (retry < 3 && blindSpotFinish) {
			blindSpotFinish = !blindSpotFinish;
			uploadUseData();
		}
	}

	/**
	 * 重写卡内余额
	 */
	private void writeBalance(int balance) {
		if (fd > 0) {
			if (Native.chk_24c02(fd) == 0) {// AT24C02卡
				String data = Integer.toHexString(balance);
				while (data.length() < 4) {
					data = "0" + data;
				}
				int[] buff = new int[4];
				buff[0] = Integer.parseInt(data.substring(0, 2), 16);
				buff[1] = Integer.parseInt(data.substring(2), 16);
				buff[2] = Integer.parseInt(data.substring(0, 2), 16);
				buff[3] = Integer.parseInt(data.substring(2), 16);
				Native.swr_24c02(fd, 0x09, 0x04, buff);
			} else if (Native.chk_102(fd) == 0) {
			}
		}
	}

	/**
	 * 上传GPS盲点数据
	 */
	private void uploadBlindSpot() {
		try {
			_cursor = db.query(MySQLHelper.T_ZXT_GPS_DATA, null, null, null, null, null, null);
			if (_cursor.getCount() > 0) {
				new Thread() {
					public void run() {
						HttpPost httpRequest = new HttpPost(server + "/blindspot.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(7);
						_cursor.moveToFirst();
						params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
						params.add(new BasicNameValuePair("session", session)); // 当前会话
						params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
						params.add(new BasicNameValuePair("gpstime", _cursor.getString(_cursor.getColumnIndex("gpstime"))));
						params.add(new BasicNameValuePair("lng", _cursor.getString(_cursor.getColumnIndex("lng"))));
						params.add(new BasicNameValuePair("lat", _cursor.getString(_cursor.getColumnIndex("lat"))));
						params.add(new BasicNameValuePair("speed", _cursor.getString(_cursor.getColumnIndex("speed"))));
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
										} else {
											blindSpotFinish = true;
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
		} catch (Exception e) {
			e.printStackTrace();
			blindSpotFinish = true;
		}
	}

	/**
	 * 上传训练数据
	 */
	private void uploadUseData() {
		try {
			_cursor = db.query(MySQLHelper.T_ZXT_USE_DATA, null, "guid!='" + _curUUID + "'", null, null, null, null);
			if (_cursor.getCount() > 0) {
				txtUploadUseDataStatus.setText("正在上传训练数据");
				new Thread() {
					public void run() {
						HttpPost httpRequest = new HttpPost(server + "/usedata.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(12);
						_cursor.moveToFirst();
						params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
						params.add(new BasicNameValuePair("session", session)); // 当前会话
						params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
						params.add(new BasicNameValuePair("guid", _cursor.getString(_cursor.getColumnIndex("guid"))));
						params.add(new BasicNameValuePair("coach", _cursor.getString(_cursor.getColumnIndex("coach"))));
						params.add(new BasicNameValuePair("student", _cursor.getString(_cursor.getColumnIndex("student"))));
						params.add(new BasicNameValuePair("starttime", _cursor.getString(_cursor.getColumnIndex("starttime"))));
						params.add(new BasicNameValuePair("endtime", _cursor.getString(_cursor.getColumnIndex("endtime"))));
						params.add(new BasicNameValuePair("balance", _cursor.getString(_cursor.getColumnIndex("balance"))));
						params.add(new BasicNameValuePair("startmi", _cursor.getString(_cursor.getColumnIndex("startmi"))));
						params.add(new BasicNameValuePair("endmi", _cursor.getString(_cursor.getColumnIndex("endmi"))));
						params.add(new BasicNameValuePair("subject", _cursor.getString(_cursor.getColumnIndex("subject"))));
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
										} else {
											txtUploadUseDataStatus.setText(usedataresult + "[" + getNowTime() + "]");
											uploadBlindSpot();
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
		} catch (Exception e) {
			e.printStackTrace();
			uploadBlindSpot();// 上传GPS盲点
		}
	}

	/**
	 * 上传照片
	 */
	private void uploadPhoto() {
		if (retry < 3 && !_photouploading) {
			new Thread() {
				public void run() {
					_photouploading = true;
					try {
						Map<String, String> params = new HashMap<String, String>();
						params.put("deviceid", deviceID);// 设备号码
						params.put("session", session);// 当前会话
						File path = new File(PHOTO_PATH);
						while (path.listFiles().length > 0) {
							File file = path.listFiles()[0];
							params.put("filename", file.getName().substring(0, 16) + ".jpg");
							params.put("speed", file.getName().split("_")[2]);// 速度
							params.put("guid", file.getName().split("_")[3].replace(".jpg", ""));// 训练任务ID
							FormFile formfile = new FormFile(file.getName(), file, "image", "application/octet-stream");
							SocketHttpRequester.post(server + "/photoupload.ashx", params, formfile);
							file.delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					_photouploading = false;
				}
			}.start();
		}
	}

	/**
	 * 获取教练信息
	 * 
	 * @param 卡号
	 */
	private void getCoachInfo() {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/getcoachinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(5);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", card)); // 卡号
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (cardresult.startsWith("s|")) {
									String[] results = cardresult.split("\\|");
									if (results[1].equals(card)) {
										cardBalance = Integer.parseInt(results[2]);
										if (cardBalance > 0) {
											coachCard = results[1];
											coachID = results[3];
											coachName = results[4];
											coachIDCard = results[5];// 教练身份证
											coachCertificate = results[6];// 教练证号
											if (coachIDCard.equals("无")) {
												coachIDCard = "";
											}
											if (coachCertificate.equals("无")) {
												coachCertificate = "";
											}
											showCoach(true);
											studentCard = coachCard;
											studentID = coachID;
											studentName = coachName;
											studentIDCard = coachIDCard;
											trainBegin();// 开始训练
										} else {
											toashShow("卡内余额不足");
										}
									}
								} else if (cardresult.equals("version_error")) {
									versionUpdate();
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
							toashShow("网络错误,暂时无法获取教练信息,请尝试重新插卡");
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
	private void getStudentInfo() {
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/getstudentinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(5);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", card)); // 学员卡号
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (cardresult.startsWith("s|")) {
									String[] results = cardresult.split("\\|");
									if (results[1].equals(card)) {
										cardBalance = Integer.parseInt(results[2]);
										if (cardBalance > 0) {
											studentCard = results[1];
											studentID = results[3];
											studentName = results[4];
											studentIDCard = results[5];
											studentDriverType = results[6];
											if (studentIDCard.equals("无")) {
												studentIDCard = "";
											}
											if (studentDriverType.equals("无")) {
												studentDriverType = "";
											}
											trainBegin();// 开始训练
										} else {
											toashShow("卡内余额不足");
										}
									}
								} else if (cardresult.equals("version_error")) {
									versionUpdate();
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
							toashShow("网络错误,暂时无法获取学员信息,请尝试重新插卡");
						}
					});
					retry++;
					changeServer();
				}
			}
		}.start();
	}

	/**
	 * 获取累计学时和里程
	 */
	private void getTotalTimeAndMi() {
		txtStudentTotalTime.setText("累计学时:正在读取...");
		txtStudentTotalMi.setText("累计里程:正在读取...");
		new Thread() {
			public void run() {
				HttpPost httpRequest = new HttpPost(server + "/gettimeandmi.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("stuid", studentID)); // 学员编号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
						handler.post(new Runnable() {
							public void run() {
								if (cardresult.startsWith("s|")) {
									String[] results = cardresult.split("\\|");
									studentTotalTime = Integer.parseInt(results[1]);
									studentTotalMi = Integer.parseInt(results[2]);
									txtStudentTotalTime.setText("累计学时:" + (studentTotalTime / 60) + "小时" + (studentTotalTime % 60) + "分钟");
									txtStudentTotalMi.setText("累计里程:" + (studentTotalMi / 1000) + "KM");
								}
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
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
				List<NameValuePair> params = new ArrayList<NameValuePair>(7);
				if (studentCard.equals("") || startTime == null) {
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(speed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", coachCard)); // 教练
				} else {
					params = new ArrayList<NameValuePair>(11);
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(speed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", coachCard)); // 教练
					params.add(new BasicNameValuePair("student", studentCard)); // 学员
					params.add(new BasicNameValuePair("starttime", dateFormat.format(startTime))); // 开始训练时间
					params.add(new BasicNameValuePair("balance", String.valueOf(cardBalanceNow))); // 余额
					params.add(new BasicNameValuePair("subject", String.valueOf(subject))); // 训练科目
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
									tcv.put("speed", String.valueOf(speed));
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
			txtCoachCard.setText("卡号:" + coachCard);
			txtCoachCertificate.setText(coachCertificate);
			layCoachInfo.setVisibility(View.VISIBLE);
			txtInfoCoach.setVisibility(View.GONE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", coachID);
			editor.putString("coachCard", coachCard);
			editor.putString("coachName", coachName);
			editor.putString("coachIDCard", coachIDCard);
			editor.putString("coachCertificate", coachCertificate);
			editor.commit();
			imgCoach.setImageResource(R.drawable.photo);
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
			editor.putString("coachCertificate", "");
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
			txtStudentCard.setText("卡号:" + studentCard);
			layStudentInfo.setVisibility(View.VISIBLE);
			txtInfoStudent.setVisibility(View.GONE);
			if (coachCard.equals(studentCard)) {
				txtStudentTitle.setText("驾驶员");
				txtStartTime.setText("开始用车时间: " + timeFormat.format(startTime));
			} else {
				txtStudentTitle.setText("学员");
				txtStartTime.setText("训练开始时间: " + timeFormat.format(startTime));
				txtStudentDriverType.setText("准驾车型:" + studentDriverType);
				txtStudentID.setText("学员编号:" + studentID);
				txtStudentIDCard.setText("身份证号:" + studentIDCard);
				getTotalTimeAndMi();// 读取学时和里程
			}
			if (!studentIDCard.equals("")) {
				// 读取头像
				imgStudent.setImageUrl(server + "/" + studentIDCard + ".bmp");
			}
		} else {
			txtStudentTitle.setText("学员");
			layStudentTitle.setBackgroundResource(R.drawable.bg1);
			layStudentInfo.setVisibility(View.GONE);
			txtInfoStudent.setVisibility(View.VISIBLE);
			imgStudent.setImageResource(R.drawable.photo);
			txtStudentID.setText("");
			txtStudentIDCard.setText("");
			txtStudentDriverType.setText("");
			txtStudentTotalTime.setText("");
			txtStudentTotalMi.setText("");
		}
	}

	/**
	 * 切换状态
	 * 
	 * @param 训练状态0
	 *            ,自由状态1
	 */
	private void changeMode(int m) {
		if (m == 0) {
			if (mode == 1) {
				trainFinish();// 结束训练
				mode = 0;
				btnFJFMS.setBackgroundResource(R.drawable.button_bg);
				txtFJFMS.setTextColor(R.color.button_text);
				btnJFMS.setBackgroundResource(R.drawable.button_checked_bg);
				txtJFMS.setTextColor(Color.WHITE);
				toashShow("已经切换为训练状态");
			}
		} else {
			if (mode == 0) {
				trainFinish();// 结束训练
				mode = 1;
				btnJFMS.setBackgroundResource(R.drawable.button_bg);
				txtJFMS.setTextColor(R.color.button_text);
				btnFJFMS.setBackgroundResource(R.drawable.button_checked_bg);
				txtFJFMS.setTextColor(Color.WHITE);
				toashShow("已经切换为自由状态");
			}
		}
	}

	/**
	 * 程序更新
	 */
	private void versionUpdate() {
		speak("发现新版本,需要更新");
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("发现新版本,需要更新").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Uri uri = Uri.parse(server + "/zpad.apk");
				startActivity(new Intent(Intent.ACTION_VIEW, uri));
			}
		}).create();
		alertDialog.show();
	}

	/**
	 * 获取经纬度
	 */
	private void getLngLat() {
		Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		if (location != null) {
			lng = location.getLongitude();
			lat = location.getLatitude();
			speed = Math.round(location.getSpeed() * 60 / 1000);
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

	private void toashShow(String str) {
		Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
		speak(str);
	}

	private void speak(String str) {
		if (mTts != null) {
			try {
				mTts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
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
				speed = Math.round(location.getSpeed() * 60 / 1000);
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
		// 定时器
		_timerUpload.cancel();
		_timerFlicker.cancel();
		_timerSecond.cancel();
		_timerMinute.cancel();
		_timerCamera.cancel();
		_timerUploadPhoto.cancel();
		// 读卡器
		try {
			Native.ic_exit(fd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TTS语音
		if (mTts != null) {
			try {
				mTts.shutdown();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 数据库
		if (_cursor != null && !_cursor.isClosed()) {
			try {
				_cursor.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (db != null) {
			try {
				db.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 指纹
		if (finger != null) {
			try {
				finger.PSCloseDevice();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 摄像头
		if (_camera != null) {
			try {
				_camera.stopPreview();
				_camera.release();
				_camera = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (wakeLock != null) {
			wakeLock.release();
		}
		super.onDestroy();
	}
}