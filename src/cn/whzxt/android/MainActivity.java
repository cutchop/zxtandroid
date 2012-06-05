package cn.whzxt.android;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.security.auth.PrivateCredentialPermission;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnInitListener, SurfaceHolder.Callback {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtInfoCoach, txtInfoStudent, txtStudentTitle, txtStatus, txtUploadUseDataStatus, txtLngLat, txtSensor;
	private TextView txtCoachName, txtCoachCard, txtCoachCertificate, txtStudentName, txtStudentCard;
	private TextView txtStartTime, txtTrainTime, txtBalance;
	private TextView txtNetworkStatus;
	private TextView txtJFMS, txtFJFMS, txtSubject2, txtSubject3;
	private TextView txtStudentID, txtStudentDriverType, txtStudentTotalTime, txtStudentTotalMi;
	private NetImageView imgCoach, imgStudent;
	private LinearLayout layCoachTitle, layStudentTitle, layCoachInfo, layStudentInfo;
	private LinearLayout btnJFMS, btnFJFMS, btnSubject2, btnSubject3;
	private LinearLayout layTts, layScrollDown, laySysInfo, layDevStatus;
	private TextView btnTabSysInfo, btnTabDevStatus, txtSysInfo;
	private TableLayout layTitle;
	private ScrollView mainView;
	private SurfaceView previewSurface;
	private SurfaceHolder previewSurfaceHolder;
	private String deviceID, deviceName, schoolID, schoolName, session;
	private String server;
	private String uploadresult, cardresult, usedataresult, ttsdataresult;
	private Student _student;
	private Coach _coach;
	private int _cardType;
	private int cardBalance = 0, cardBalanceNow = 0;
	private int[] _initcontext;
	private int col = 0;
	private Date startTime, nowTime, endTime;
	private int startMi = 0, nowMi = 0, startSenMi = 0, nowSenMi = 0;
	private String _curUUID;
	private static final int MODE_TRAIN = 0;
	private static final int MODE_FREE = 1;
	private int mode = MODE_TRAIN;// 训练模式0，自由模式1
	private int subject = 2;// 科目二，科目三
	private int retry = 0;
	private double lng, lat;
	private int speed, senspeed; // 速度
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerFlicker, _timerSecond, _timerMinute, _timerCamera, _timerTakePhoto, _timerUploadPhoto, _timerSensor;
	private SharedPreferences settings;
	private MySQLHelper sqlHelper;
	private SQLiteDatabase db;
	private Boolean _usedataUploading = false;
	private Boolean _blindspotUploading = false;
	private HashMap<String, String> _hashTts;
	private String _ttsVer;
	private String _tcpMsg;
	private Boolean _rfidProcsing = false;
	private String _rfidUID;
	private int _uploadblindSpotCount = 0;
	private Boolean _hasfingerdata = false;
	private static final int BS_EVERY_MAX = 10;// 每次上传10个盲点

	private ServerSocket socket = null;
	private Socket client = null;

	private static final int DBVERSION = 10;
	private static final int PRICE = 2; // 设备单价
	private static final float NMDIVIDED = 1.852f; // 海里换算成公里
	// 读卡器
	private static final String PATH = "/dev/s3c2410_serial1"; // 读卡器参数
	private static final int BAUD = 9600; // 读卡器参数
	private int fd;
	private static final int NO_CARD = 0;
	private static final int CARD_24C02 = 1;
	private static final int CARD_S50 = 2;
	private static final int CARD_S70 = 3;

	private static final int RFID_TYPE_A = 0x02;
	// private static final int RFID_TYPE_B = 0x01;

	private static final int RFID_BLOCK = 0;
	private static final int RFID_EARA = 1;

	private static final int[] RFID_KEY = new int[] { 255, 255, 255, 255, 255, 255 };
	// TTS
	private TextToSpeech mTts;
	private static final int REQ_TTS_STATUS_CHECK = 0;
	// 指纹
	private lytfingerprint _fingerprint;
	private static final int _fingerAddress = 0xffffffff;// 默认的地址
	private static final int CHAR_BUFFER_A = 0x01;
	private static final int CHAR_BUFFER_B = 0x02;
	private static final char PS_OK = 0x00;
	private static final char PS_NO_FINGER = 0x02;
	private String _fingertmppath = "/data/local/tmp/finger_";
	private int[] _address = new int[2];
	private int[] _context;
	private int _fingerCurId;
	private Boolean _fingerFinish = false;
	// 摄像头
	private Camera _camera;
	private static final int PHOTO_DEF_WIDTH = 720; // 默认照片宽度
	private static final int PHOTO_DEF_HEIGHT = 576; // 默认照片高度
	private static final int PHOTO_DEF_INTERVAL = 30; // 默认拍照间隔(秒)
	private static final String PHOTO_PATH = "/sdcard/zxtphoto";// 照片保存路径
	private int _inSampleSize = 4;// 照片压缩比,宽720/4,高576/4
	private int _curCamera = 1;// 当前摄像头
	private int _phototaked = 1;
	private Boolean _previewing = false;
	private Boolean _takephotoing = false;
	private Boolean _photouploading = false;
	private String _phototime = "";
	private String _photoUUID = "";
	// 控制屏幕长亮
	private WakeLock wakeLock;
	// 弹出框
	private static final int DL_CARD_READING = 0x00;
	private static final int DL_DOWN_FINGER = 0x01;
	private static final int DL_VALI_FINGER = 0x02;
	private static final int DL_CARD_INIT = 0x03;
	private static final int DL_FINGER1 = 0x04;
	private static final int DL_FINGER2 = 0x05;
	private static final int DL_FINGER3 = 0x06;
	private static final int DL_GET_COACH = 0x07;
	private static final int DL_GET_STUDENT = 0x08;
	// Handle_What
	private static final int H_W_UPLOAD = 0x01;
	private static final int H_W_FLICKER = 0x02;
	private static final int H_W_SECOND = 0x03;
	private static final int H_W_MINUTE = 0x04;
	private static final int H_W_TAKE_PHOTO = 0x05;
	private static final int H_W_UPLOAD_PHOTO = 0x06;
	private static final int H_W_TAKE_PHOTO_DELAY = 0x07;
	private static final int H_W_HIDE_TRAININFO = 0x08;
	private static final int H_W_SET_RELAY_FALSE = 0x09;
	private static final int H_W_SHOW_SENSOR = 0x0A;
	private static final int H_W_UPDATEDIALOG_MAX = 0x14;
	private static final int H_W_UPDATEDIALOG_NOW = 0x15;
	// 程序更新对话框
	private ProgressDialog _updateDialog;
	private File _downLoadFile;
	private int _fileLength, _downedFileLength = 0;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case H_W_UPLOAD:
				upload();
				break;
			case H_W_FLICKER:
				flicker();
				break;
			case H_W_SECOND:
				second();
				break;
			case H_W_MINUTE:
				minute();
				break;
			case H_W_TAKE_PHOTO:
				takephoto();
				break;
			case H_W_UPLOAD_PHOTO:
				uploadPhoto();
				break;
			case H_W_TAKE_PHOTO_DELAY:
				takephotoDelay();
				break;
			case H_W_HIDE_TRAININFO:
				hideTrainInfo();
				break;
			case H_W_SET_RELAY_FALSE:
				if (mode == MODE_TRAIN) {
					if (startTime == null) {
						NativeGPIO.setRelay(false);
						toashShow("油电已断");
					}
				}
				break;
			case H_W_SHOW_SENSOR:
				showSensor();
				break;
			case H_W_UPDATEDIALOG_MAX:
				_updateDialog.setMax(_fileLength);
				break;
			case H_W_UPDATEDIALOG_NOW:
				int x = _downedFileLength * 100 / _fileLength;
				_updateDialog.setMessage("正在下载，已完成" + x + "%");
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
		txtSensor = (TextView) findViewById(R.id.txtSensor);
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
			txtNetworkStatus.setTextColor(Color.RED);
		}
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm:ss");
		txtSystemTime.setText(dateFormat.format(new Date()));

		settings = getSharedPreferences("whzxt.net", 0);

		_inSampleSize = settings.getInt("inSampleSize", 4);

		server = getString(R.string.server1);

		sqlHelper = new MySQLHelper(this, "zxt.db", null, DBVERSION);
		db = sqlHelper.getWritableDatabase();

		// 初始化学员和教练
		_student = new Student();
		_coach = new Coach();
		// 初始化TTS
		Intent checkIntent = new Intent();
		try {
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// IC卡读卡器
		try {
			fd = Native.auto_init(PATH, BAUD);
		} catch (Exception e) {
			speak("IC卡读卡器打开失败");
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("IC卡读卡器打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			}).create();
			alertDialog.show();
			return;
		}
		// RFID读卡器
		try {
			if (!NativeRFID.open_fm1702()) {
				speak("RFID读卡器打开失败");
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("RFID读卡器打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.this.finish();
					}
				}).create();
				alertDialog.show();
				return;
			}
		} catch (ExceptionInInitializerError e) {
			speak("RFID读卡器打开失败");
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("RFID读卡器打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			}).create();
			alertDialog.show();
			e.printStackTrace();
			return;
		}
		NativeRFID.select_type(RFID_TYPE_A);
		// 指纹
		try {
			_fingerprint = new lytfingerprint();
			lytfingerprint.Open();
			if (_fingerprint.PSOpenDevice(1, 0, 57600 / 9600, 2) != 1) {
				speak("指纹模块打开失败");
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("指纹模块打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.this.finish();
					}
				}).create();
				alertDialog.show();
				return;
			}
		} catch (ExceptionInInitializerError e) {
			speak("指纹模块打开失败");
			AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("指纹模块打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			}).create();
			alertDialog.show();
			e.printStackTrace();
			return;
		}
		// 隐藏学员信息
		showStudent(false);
		// 显示教练信息
		if (settings.getString("coachCard", "") != "") {
			_coach.ID = settings.getString("coachID", "");
			_coach.Name = settings.getString("coachName", "");
			_coach.CardNo = settings.getString("coachCard", "");
			_coach.IDCardNo = settings.getString("coachIDCard", "");
			_coach.Certificate = settings.getString("coachCertificate", "");
			showCoach(true);
		} else {
			showCoach(false);
		}

		// 摄像头
		File photoDir = new File(PHOTO_PATH);
		if (!photoDir.exists()) {
			photoDir.mkdirs();
		}
		previewSurfaceHolder = previewSurface.getHolder();
		previewSurfaceHolder.addCallback(this);
		previewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		ttsButtonInit(); // 语音提示按钮初始化

		// 屏幕长亮
		PowerManager manager = ((PowerManager) getSystemService(POWER_SERVICE));
		wakeLock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ATAAW");
		wakeLock.acquire();

		// 初始化GPS
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
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
				btnTabDevStatus.setBackgroundResource(R.drawable.button_bg);
				btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
				btnTabSysInfo.setBackgroundResource(R.drawable.button_checked_bg);
				btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				laySysInfo.setVisibility(View.VISIBLE);
				layDevStatus.setVisibility(View.GONE);
			}
		});
		// 设备状态Tab
		btnTabDevStatus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				btnTabSysInfo.setBackgroundResource(R.drawable.button_bg);
				btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
				btnTabDevStatus.setBackgroundResource(R.drawable.button_checked_bg);
				btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
				laySysInfo.setVisibility(View.GONE);
				layDevStatus.setVisibility(View.VISIBLE);
			}
		});
		// 训练状态
		btnJFMS.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				changeMode(MODE_TRAIN);
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
							changeMode(MODE_FREE);
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
		// 切换摄像头
		previewSurface.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_takephotoing) {
					Toast.makeText(MainActivity.this, "正在拍照,请稍候再切换摄像头", Toast.LENGTH_SHORT);
				} else {
					changeCamera();
				}
			}
		});

		// 定时30秒上传
		_timerUpload = new Timer();
		_timerUpload.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_UPLOAD);
			}
		}, 10000, 30000);
		// 文字闪烁
		_timerFlicker = new Timer();
		_timerFlicker.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_FLICKER);
			}
		}, 3000, 200);
		// 每秒执行
		_timerSecond = new Timer();
		_timerSecond.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_SECOND);
			}
		}, 2000, 1000);
		// 每分钟执行
		_timerMinute = new Timer();
		_timerMinute.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_MINUTE);
			}
		}, 15000, 60000);
		// 定时拍照
		_timerCamera = new Timer();
		_timerCamera.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_TAKE_PHOTO);
			}
		}, 20000, settings.getInt("photo_interval", PHOTO_DEF_INTERVAL) * 1000);
		// 延时拍照
		_timerTakePhoto = new Timer();
		// 上传照片
		_timerUploadPhoto = new Timer();
		_timerUploadPhoto.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_UPLOAD_PHOTO);
			}
		}, 25000, 30000);
		// 计算传感器速度和里程
		_timerSensor = new Timer();
		_timerSensor.schedule(new TimerTask() {
			@Override
			public void run() {
				senspeed = Math.round((float) NativeGPIO.getRotateSpeed(1, 1) / 1.32f);
				nowSenMi += senspeed * 1000 / 3600;

				handler.sendEmptyMessage(H_W_SHOW_SENSOR);
			}
		}, 4000, 1000);

		try {
			socket = new ServerSocket(8888);
			socketListen();// 监听指令
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 保存照片
	 */
	PictureCallback jpegPictureCallback = new PictureCallback() {
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inSampleSize = _inSampleSize;
			Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length, options);
			Bitmap newPicture = Bitmap.createBitmap(bitmapPicture.getWidth(), bitmapPicture.getHeight(), bitmapPicture.getConfig());
			Canvas canvas = new Canvas(newPicture);
			canvas.drawBitmap(bitmapPicture, 0, 0, null);
			canvas.drawText(_phototime, bitmapPicture.getWidth() - 100, bitmapPicture.getHeight() - 30, null);
			canvas.save(Canvas.ALL_SAVE_FLAG);
			File file = new File(PHOTO_PATH + "/" + _phototime + "_" + _curCamera + "_" + speed + "_" + senspeed + "_" + _photoUUID + ".jpg");
			try {
				file.createNewFile();
				BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
				newPicture.compress(Bitmap.CompressFormat.JPEG, 80, os);
				os.flush();
				os.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

			changeCamera();// 切换摄像头
			if (_phototaked == 1) {
				_phototaked = 2;
				_timerTakePhoto.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.sendEmptyMessage(H_W_TAKE_PHOTO_DELAY);
					}
				}, 1000);
			} else {
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
		if (_camera != null && !_takephotoing && !_curUUID.equals("")) {
			try {
				_photoUUID = _curUUID;
				_takephotoing = true;
				_phototime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
				_phototaked = 1;
				_timerTakePhoto.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.sendEmptyMessage(H_W_TAKE_PHOTO_DELAY);
					}
				}, 1 * 1000);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 切换摄像头
	 * 
	 * @param 摄像头路数
	 */
	private void changeCamera() {
		if (_curCamera == 1) {
			_curCamera = 2;
			Camera.Parameters parameters = _camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
			_camera.setParameters(parameters);
		} else {
			_curCamera = 1;
			Camera.Parameters parameters = _camera.getParameters();
			parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			_camera.setParameters(parameters);
		}
	}

	private void takephotoDelay() {
		try {
			_camera.takePicture(null, null, jpegPictureCallback);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
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
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_TTS, null, "id=0", null, null, null, null);
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					_ttsVer = cursor.getString(cursor.getColumnIndex("tts"));
				}
				HttpPost httpRequest = new HttpPost(server + "/gettts.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("ver", _ttsVer)); // 版本号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				HttpResponse httpResponse = null;
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						ttsdataresult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						return 0;
					}
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
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_TTS, null, "id!=0", null, null, null, "id");
				if (cursor.moveToFirst()) {
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
		}.execute();
	}

	/**
	 * 监听消息
	 */
	private void socketListen() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				_tcpMsg = null;
				try {
					client = socket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					client.setSoTimeout(2000);
				} catch (SocketException e) {
					e.printStackTrace();
				}
				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(client.getInputStream(), "gb2312"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					_tcpMsg = in.readLine();
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				return 1;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (_tcpMsg != null) {
					if (_tcpMsg.startsWith("cmd:")) {
						String cmd = _tcpMsg.substring(4);
						if (cmd.equals("open_oil")) {
							// 恢复油电
							NativeGPIO.setRelay(true);
						} else if (cmd.equals("close_oil")) {
							// 切断油电
							toashShow("1分钟后将断油电");
							new Timer().schedule(new TimerTask() {
								@Override
								public void run() {
									handler.sendEmptyMessage(H_W_SET_RELAY_FALSE);
								}
							}, 60 * 1000);
						} else if (cmd.equals("mode_train")) {
							changeMode(MODE_TRAIN);// 切换训练状态
						} else if (cmd.equals("mode_free")) {
							changeMode(MODE_FREE);// 切换自由状态
						} else if (cmd.equals("take_photo")) {
							takephoto();// 拍照
						} else if (cmd.startsWith("set_photo_size:")) {
							try {
								// 设置照片压缩比
								cmd = cmd.replace("set_photo_size:", "");
								_inSampleSize = Integer.valueOf(cmd);
								SharedPreferences.Editor editor = settings.edit();
								editor.putInt("inSampleSize", _inSampleSize);
								editor.commit();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (cmd.startsWith("set_photo_interval:")) {
							/*
							 * try { // 设置拍照间隔 cmd = cmd.replace
							 * ("set_photo_interval:", ""); int pinterval =
							 * Integer.valueOf(cmd); _timerCamera.cancel();
							 * _timerCamera.schedule(new TimerTask() {
							 * 
							 * @Override public void run() { Message msg = new
							 * Message(); msg.what = 5;
							 * handler.sendMessage(msg); } }, 5000, pinterval *
							 * 1000); SharedPreferences.Editor editor =
							 * settings.edit(); editor .putInt("photo_interval",
							 * pinterval); editor.commit(); } catch (Exception
							 * e) { e.printStackTrace(); }
							 */
						} else if (cmd.startsWith("card_init:")) {
							// 初始化卡
							_fingerFinish = true;
							cmd = cmd.replace("card_init:", "");
							_initcontext = new int[48];
							for (int i = 0; i < _initcontext.length; i++) {
								_initcontext[i] = ' ';
							}
							_initcontext[0] = 2;
							if (cmd.split(",")[0].equals("01")) {
								_initcontext[0] = 1;
							}
							String initcardno = cmd.split(",")[1];
							for (int i = 0; i < initcardno.length(); i++) {
								_initcontext[i + 1] = initcardno.charAt(i);
							}
							String initschoolid = cmd.split(",")[2];
							for (int i = 0; i < initschoolid.length(); i++) {
								_initcontext[i + 9] = initschoolid.charAt(i);
							}
							showDialog(DL_CARD_INIT);
							_rfidProcsing = true;
							new AsyncTask<Void, Void, Integer>() {
								@Override
								protected Integer doInBackground(Void... args) {
									return NativeRFID.write_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA, _initcontext);
								}

								@Override
								protected void onPostExecute(Integer result) {
									dismissDialog(DL_CARD_INIT);
									_rfidProcsing = false;
									if (result == 1) {
										NativeRFID.write_card(new int[] { 10 }, RFID_KEY, RFID_BLOCK, new int[] { 0 });
										toashShow("卡初始化成功,请重新插卡");
									} else {
										toashShow("卡初始化失败,请重试");
									}
								}
							}.execute();
						}
					} else if (_tcpMsg.startsWith("sms:")) {
						txtSysInfo.setText(_tcpMsg.substring(4));
						btnTabDevStatus.setBackgroundResource(R.drawable.button_bg);
						btnTabDevStatus.setTextColor(MainActivity.this.getResources().getColor(R.color.button_text));
						btnTabSysInfo.setBackgroundResource(R.drawable.button_checked_bg);
						btnTabSysInfo.setTextColor(MainActivity.this.getResources().getColor(R.color.button_checked_text));
						laySysInfo.setVisibility(View.VISIBLE);
						layDevStatus.setVisibility(View.GONE);
						toashShow("收到新短信:" + txtSysInfo.getText().toString());
					}
				}
				socketListen();// 继续监听
			}
		}.execute();
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
	 * 读卡型
	 * 
	 * @return
	 */
	private int readCardType() {
		if (Native.chk_24c02(fd) == 0) {
			return CARD_24C02;
		}
		_rfidUID = NativeRFID.read_A();
		if (_rfidUID.charAt(16) == '0') {
			// _rfidUID = hex2string(_rfidUID.substring(0, 13));
			return CARD_S50;
		}
		if (_rfidUID.charAt(16) == '1') {
			// _rfidUID = hex2string(_rfidUID.substring(0, 13));
			return CARD_S70;
		}
		return NO_CARD;
	}

	/*
	 * private String hex2string(String hex) { String str = ""; String[] strs =
	 * str.split(" "); for (int i = 0; i < strs.length; i++) { if
	 * (!strs[i].equals("")) { str += String.valueOf((char)
	 * Integer.parseInt(strs[i], 16)); } } return str; }
	 */

	/**
	 * 读卡
	 */
	private void readCard() {
		if (_cardType == CARD_24C02) {
			String tmpdata = Native.srd_24c02(fd, 0x08, 0x20);
			String[] tmpchars = tmpdata.split(" ");
			if (tmpchars[0].equals("02") && _coach.CardNo.equals("")) {
				toashShow("请先插教练卡");
				return;
			}
			// 驾校ID
			String tmpschool = String.valueOf((char) Integer.parseInt(tmpchars[24], 16));
			for (int i = 25; i < 32; i++) {
				tmpschool += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
			}
			// 兼容老卡
			if (tmpschool.equals("11111111")) {
				tmpschool = "001001";
			}
			if (!tmpschool.equals(schoolID)) {
				toashShow("此卡不属于本驾校");
				return;
			}
			// 卡号
			String tmpCardNo = String.valueOf((char) Integer.parseInt(tmpchars[8], 16));
			for (int i = 9; i < 16; i++) {
				tmpCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
			}
			// 卡内剩余时长
			cardBalance = 0;
			try {
				cardBalance = Integer.parseInt(tmpchars[1].toString() + tmpchars[2].toString(), 16);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// 姓名
			String tmpCardName = "";
			try {
				tmpCardName = new String(int2bytes(Integer.parseInt(tmpchars[16].toString() + tmpchars[17].toString(), 16)), "GB2312").trim();
				for (int i = 18; i < 24; i++) {
					tmpCardName += new String(int2bytes(Integer.parseInt(tmpchars[i].toString() + tmpchars[++i].toString(), 16)), "GB2312").trim();
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			if (tmpchars[0].equals("01")) {
				// 教练卡
				_coach.CardNo = _student.CardNo = tmpCardNo;
				_coach.ID = _student.ID = "";
				_coach.Name = _student.Name = tmpCardName;
				_coach.IDCardNo = _student.IDCardNo = "";
				_student.IsCoach = true;
				showCoach(true);
				showStudent(true);
				if (retry < 3) {
					getCoachInfo();
				} else {
					trainBegin();// 开始离线训练
				}
			} else {
				// 学员卡
				_student.CardNo = tmpCardNo;
				_student.ID = "";
				_student.Name = tmpCardName;
				_student.IDCardNo = "";
				_student.IsCoach = false;
				showStudent(true);
				if (retry < 3) {
					getStudentInfo();
				} else {
					trainBegin();// 开始离线训练
				}
			}
		} else if (_cardType == CARD_S50 || _cardType == CARD_S70) {
			_rfidProcsing = true;
			showDialog(DL_CARD_READING);// 提示正在读卡
			new AsyncTask<Void, Void, Integer>() {
				@Override
				protected Integer doInBackground(Void... args) {
					Log.i("genzong", "read card 1");
					// 身份信息
					String tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
					if (!tmpdata.endsWith("1")) {
						SystemClock.sleep(200);// 200毫秒后尝试重新读卡
						tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
					}
					if (!tmpdata.endsWith("1")) {
						return 1;
					}
					String[] tmpchars = tmpdata.split(" ");
					// 卡类型
					if (tmpchars[0].equals("02") && _coach.CardNo.equals("")) {
						return 3;
					}
					// 驾校ID
					String tmpschool = "";
					for (int i = 9; i < 18; i++) {
						tmpschool += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
					}
					tmpschool = tmpschool.trim();
					if (!tmpschool.equals(schoolID)) {
						return 2;
					}
					// 卡号
					String tmpCardNo = "";
					for (int i = 1; i < 9; i++) {
						tmpCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
					}
					tmpCardNo = tmpCardNo.trim();
					// 身份证号
					String tmpIDCardNo = "";
					for (int i = 18; i < 36; i++) {
						tmpIDCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
					}
					tmpIDCardNo = tmpIDCardNo.trim();
					// 姓名
					String tmpCardName = "";
					try {
						for (int i = 36; i < 46; i++) {
							tmpCardName += new String(int2bytes(Integer.parseInt(tmpchars[i].toString() + tmpchars[++i].toString(), 16)), "GB2312").trim();
						}
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					if (tmpchars[0].equals("01")) {
						// 教练卡
						_coach.CardNo = _student.CardNo = tmpCardNo;
						_coach.ID = _student.ID = "";
						_coach.Name = _student.Name = tmpCardName;
						_coach.IDCardNo = _student.IDCardNo = "";
						_student.IsCoach = true;
					} else {
						// 学员卡
						_student.CardNo = tmpCardNo;
						_student.ID = "";
						_student.Name = tmpCardName;
						_student.IDCardNo = "";
						_student.IsCoach = false;
					}
					Log.i("genzong", "read card 2");
					// 余额.学时.里程
					tmpdata = NativeRFID.read_card(new int[] { 8 }, RFID_KEY, RFID_BLOCK);
					if (!tmpdata.endsWith("1")) {
						SystemClock.sleep(200);// 200毫秒后尝试重新读卡
						tmpdata = NativeRFID.read_card(new int[] { 8 }, RFID_KEY, RFID_BLOCK);
					}
					if (tmpdata.endsWith("1")) {
						tmpchars = tmpdata.split(" ");
						cardBalance = Integer.parseInt(tmpchars[0].toString() + tmpchars[1].toString(), 16);
						_student.TotalTime = Integer.parseInt(tmpchars[2].toString() + tmpchars[3].toString(), 16);
						_student.TotalMi = Integer.parseInt(tmpchars[4].toString() + tmpchars[5].toString() + tmpchars[6], 16);
					}
					// 判断是否有指纹
					if (_cardType == CARD_S70) {
						Log.i("genzong", "read card 3");
						tmpdata = NativeRFID.read_card(new int[] { 10 }, RFID_KEY, RFID_BLOCK);
						if (!tmpdata.endsWith("1")) {
							SystemClock.sleep(200);// 200毫秒后尝试重新读卡
							tmpdata = NativeRFID.read_card(new int[] { 10 }, RFID_KEY, RFID_BLOCK);
						}
						if (tmpdata.startsWith("01")) {
							_hasfingerdata = true;
						} else {
							_hasfingerdata = false;
						}
					}
					return 0;
				}

				@Override
				protected void onPostExecute(Integer result) {
					dismissDialog(DL_CARD_READING);
					if (result == 0) {
						if (_cardType == CARD_S70) {
							if (!_hasfingerdata) {
								Log.i("genzong", "record finger 1");
								writeFinger();// 第一次使用记录指纹
							} else {
								showDialog(DL_DOWN_FINGER);
								Log.i("genzong", "download finger 1");
								downFinger();
							}
						} else {
							_rfidProcsing = false;
							if (_student.IsCoach) {
								showCoach(true);
								showStudent(true);
								if (retry < 3) {
									getCoachInfo();
								} else {
									trainBegin();// 开始离线训练
								}
							} else {
								if (!_coach.CardNo.equals("")) {
									showStudent(true);
									if (retry < 3) {
										getStudentInfo();
									} else {
										trainBegin();// 开始离线训练
									}
								} else {
									toashShow("请先插教练卡");
								}
							}
						}
					} else {
						_rfidProcsing = false;
						if (result == 1) {
							toashShow("读卡失败,请尝试重新插卡");
						} else if (result == 2) {
							toashShow("此卡不属于本驾校");
						} else if (result == 3) {
							toashShow("请先插教练卡");
						}
					}
				}
			}.execute();
		}
	}

	/**
	 * 下载指纹
	 */
	private void downFinger() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_FINGER, new String[] { "id" }, "cardno=?", new String[] { _student.CardNo }, null, null, null);
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					_fingerCurId = cursor.getInt(0);
					ContentValues tcv = new ContentValues();
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.update(MySQLHelper.T_ZXT_FINGER, tcv, "id=?", new String[] { String.valueOf(_fingerCurId) });
					return 1;
				}
				Boolean isinsert = true;
				cursor = db.rawQuery("SELECT MAX(id) AS max_id FROM " + MySQLHelper.T_ZXT_FINGER, null);
				_fingerCurId = 1;
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					_fingerCurId = cursor.getInt(0);
					if (_fingerCurId < 253) {
						if (_fingerCurId == 0) {
							_fingerCurId = 1;
						} else {
							_fingerCurId = _fingerCurId + 2;
						}
					} else {
						cursor = db.rawQuery("SELECT id FROM " + MySQLHelper.T_ZXT_FINGER + " ORDER BY lasttime", null);
						cursor.moveToFirst();
						_fingerCurId = cursor.getInt(0);
						isinsert = false;
					}
				}
				Boolean done = false;
				if (retry < 3) {
					try {
						URL url = new URL(server + "/getfinger.ashx?cardno=" + _student.CardNo + "&n=1");
						URLConnection connection = url.openConnection();
						connection.connect();
						InputStream inputStream = connection.getInputStream();
						File fingerfile = new File(_fingertmppath);
						if (fingerfile.exists()) {
							fingerfile.delete();
						}
						fingerfile.createNewFile();
						OutputStream outputStream = new FileOutputStream(fingerfile);
						int len = connection.getContentLength();
						if (len >= 512) {
							byte[] buffer = new byte[512];
							inputStream.read(buffer);
							outputStream.write(buffer, 0, buffer.length);
							done = true;
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (!done) {
					Log.i("zhiwen", "(卡)指纹1开始:" + new Date().toString());
					String tmpdata = NativeRFID.read_card(new int[] { 3, 11 }, RFID_KEY, RFID_EARA);
					if (!tmpdata.endsWith("1")) {
						tmpdata = NativeRFID.read_card(new int[] { 3, 11 }, RFID_KEY, RFID_EARA);
					}
					Log.i("zhiwen", "(卡)指纹1结束:" + new Date().toString());
					if (!tmpdata.endsWith("1")) {
						return 0;
					}
					String[] tmpchars = tmpdata.split(" ");
					byte[] tmpbytes = new byte[512];
					for (int i = 0; i < tmpbytes.length; i++) {
						tmpbytes[i] = 0;
					}
					for (int i = 0; i < tmpbytes.length; i++) {
						tmpbytes[i] = (byte) Integer.parseInt(tmpchars[i], 16);
					}
					try {
						FileOutputStream fo = new FileOutputStream(_fingertmppath);
						fo.write(tmpbytes);
						fo.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						return 2;
					} catch (IOException e) {
						e.printStackTrace();
						return 2;
					}
				}
				Log.i("zhiwen", "(模块)指纹1开始:" + new Date().toString());
				_fingerprint.PSDownChar(_fingerAddress, CHAR_BUFFER_A, 0, 512, _fingertmppath);
				_fingerprint.PSStoreChar(_fingerAddress, CHAR_BUFFER_A, _fingerCurId);
				Log.i("zhiwen", "(模块)指纹1结束:" + new Date().toString());

				done = false;
				if (retry < 3) {
					try {
						URL url = new URL(server + "/getfinger.ashx?cardno=" + _student.CardNo + "&n=2");
						URLConnection connection = url.openConnection();
						connection.connect();
						InputStream inputStream = connection.getInputStream();
						File fingerfile = new File(_fingertmppath);
						if (fingerfile.exists()) {
							fingerfile.delete();
						}
						fingerfile.createNewFile();
						OutputStream outputStream = new FileOutputStream(fingerfile);
						int len = connection.getContentLength();
						if (len >= 512) {
							byte[] buffer = new byte[512];
							inputStream.read(buffer);
							outputStream.write(buffer, 0, buffer.length);
							done = true;
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (!done) {
					Log.i("zhiwen", "(卡)指纹2开始:" + new Date().toString());
					String tmpdata = NativeRFID.read_card(new int[] { 14, 11 }, RFID_KEY, RFID_EARA);
					if (!tmpdata.endsWith("1")) {
						tmpdata = NativeRFID.read_card(new int[] { 14, 11 }, RFID_KEY, RFID_EARA);
					}
					Log.i("zhiwen", "(卡)指纹2结束:" + new Date().toString());
					if (!tmpdata.endsWith("1")) {
						return 0;
					}
					String[] tmpchars = tmpdata.split(" ");
					byte[] tmpbytes = new byte[512];
					for (int i = 0; i < tmpbytes.length; i++) {
						tmpbytes[i] = (byte) Integer.parseInt(tmpchars[i], 16);
					}
					try {
						FileOutputStream fo = new FileOutputStream(_fingertmppath);
						fo.write(tmpbytes);
						fo.close();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						return 2;
					} catch (IOException e) {
						e.printStackTrace();
						return 2;
					}
				}
				Log.i("zhiwen", "(模块)指纹2开始:" + new Date().toString());
				_fingerprint.PSDownChar(_fingerAddress, CHAR_BUFFER_A, 0, 512, _fingertmppath);
				_fingerprint.PSStoreChar(_fingerAddress, CHAR_BUFFER_A, _fingerCurId + 1);
				Log.i("zhiwen", "(模块)指纹2结束:" + new Date().toString());

				if (isinsert) {
					ContentValues tcv = new ContentValues();
					tcv.put("id", _fingerCurId);
					tcv.put("cardno", _student.CardNo);
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.insert(MySQLHelper.T_ZXT_FINGER, null, tcv);
				} else {
					ContentValues tcv = new ContentValues();
					tcv.put("cardno", _student.CardNo);
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.update(MySQLHelper.T_ZXT_FINGER, tcv, "id=?", new String[] { String.valueOf(_fingerCurId) });
				}
				return 1;
			}

			@Override
			protected void onPostExecute(Integer result) {
				_rfidProcsing = false;
				dismissDialog(DL_DOWN_FINGER);
				if (result == 1) {
					if (_student.IsCoach) {
						showCoach(true);
						showStudent(true);
					} else {
						if (!_coach.CardNo.equals("")) {
							showStudent(true);
						} else {
							toashShow("请先插教练卡");
							return;
						}
					}
					_fingerFinish = false;
					showDialog(DL_VALI_FINGER);
					speak("需要验证指纹,请按手指");
					valiFinger();
				} else if (result == 2) {
					Toast.makeText(MainActivity.this, "写指纹文件时出现异常", Toast.LENGTH_SHORT).show();
				} else {
					toashShow("获取指纹信息失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	/**
	 * 验证指纹
	 */
	private void valiFinger() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				// 验证指纹
				while (_fingerprint.PSGetImage(_fingerAddress) == 2 && !_fingerFinish) {
					SystemClock.sleep(30);
				}
				SystemClock.sleep(30);
				while (_fingerprint.PSGetImage(_fingerAddress) == 2 && !_fingerFinish) {
					SystemClock.sleep(30);
				}
				SystemClock.sleep(30);
				_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A);
				SystemClock.sleep(30);
				if (_fingerprint.PSSearch(_fingerAddress, CHAR_BUFFER_A, _fingerCurId, 2, 0) == PS_OK) {
					return 1;
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_VALI_FINGER);
				if (result == 1) {
					toashShow("指纹验证成功");
					if (_student.IsCoach) {
						if (retry < 3) {
							getCoachInfo();
						} else {
							trainBegin();// 开始离线训练
						}
					} else {
						if (retry < 3) {
							getStudentInfo();
						} else {
							trainBegin();// 开始离线训练
						}
					}
				} else {
					if (!_fingerFinish) {
						toashShow("指纹验证失败,请重新按手指");
						showDialog(DL_VALI_FINGER);
						valiFinger();
					} else {
						toashShow("指纹验证失败,请尝试重新插卡");
					}
				}
			}
		}.execute();
	}

	/**
	 * 每秒执行
	 */
	private void second() {
		// 更新时间
		nowTime = new Date();
		txtSystemTime.setText(dateFormat.format(nowTime));
		if (null != startTime && mode == MODE_TRAIN) {
			// 计算余额
			cardBalanceNow = cardBalance - (int) ((nowTime.getTime() - startTime.getTime()) / 60000);
			if (((nowTime.getTime() - startTime.getTime()) / 1000) % 60 > 0) {
				cardBalanceNow--;
			}
			_student.RealTotalTime = _student.TotalTime + (cardBalance - cardBalanceNow);
			// 显示余额
			if (_student.IsCoach) {
				txtTrainTime.setText("已用车" + getTimeDiff(startTime, nowTime));
				txtBalance.setText("卡内剩余时长:" + cardBalanceNow + "分钟");
			} else {
				txtTrainTime.setText("已训练" + getTimeDiff(startTime, nowTime));
				txtBalance.setText("余额:" + cardBalanceNow * PRICE + "元,剩余" + cardBalanceNow + "分钟");
			}
			if (cardBalanceNow <= 0) {
				trainFinish();// 结束训练
			}
		}
		// 判断是否插卡
		switch (_cardType) {
		case CARD_24C02:
			if (Native.chk_24c02(fd) != 0) {
				trainFinish();
			}
			break;
		case CARD_S50:
			if (!_rfidProcsing) {
				if (NativeRFID.read_A().charAt(16) != '0') {
					SystemClock.sleep(100);// 100毫秒后重试一次
					Log.i("rfid", "re50");
					if (NativeRFID.read_A().charAt(16) != '0') {
						SystemClock.sleep(100);// 100毫秒后重试一次
						Log.i("rfid", "re50");
						if (NativeRFID.read_A().charAt(16) != '0') {
							trainFinish();
						}
					}
				}
			}
			break;
		case CARD_S70:
			if (!_rfidProcsing) {
				if (NativeRFID.read_A().charAt(16) != '1') {
					SystemClock.sleep(100);// 100毫秒后重试一次
					Log.i("rfid", "re70");
					if (NativeRFID.read_A().charAt(16) != '1') {
						SystemClock.sleep(100);// 100毫秒后重试一次
						Log.i("rfid", "re70");
						if (NativeRFID.read_A().charAt(16) != '1') {
							_fingerFinish = true;
							trainFinish();
						}
					}
				}
			}
			break;
		case NO_CARD:
			_cardType = readCardType();
			if (_cardType != NO_CARD) {
				mainView.fullScroll(View.FOCUS_UP);
				readCard();
			}
			break;
		default:
			break;
		}
	}

	/**
	 * 显示传感器速度
	 */
	private void showSensor() {
		txtSensor.setText("SEN:" + senspeed);
	}

	/**
	 * 开始训练
	 */
	private void trainBegin() {
		if (mode == MODE_TRAIN && cardBalance <= 0) {
			toashShow("卡内余额不足");
			return;
		}
		String _tmp = "早上好";
		if (nowTime.getHours() >= 8 && nowTime.getHours() < 12) {
			_tmp = "上午好";
		} else if (nowTime.getHours() >= 12 && nowTime.getHours() < 14) {
			_tmp = "中午好";
		} else if (nowTime.getHours() >= 14 && nowTime.getHours() < 20) {
			_tmp = "下午好";
		} else if (nowTime.getHours() >= 20 || nowTime.getHours() < 5) {
			_tmp = "晚上好";
		}
		if (mode == MODE_TRAIN) {
			startTime = new Date();
			startMi = nowMi;
			startSenMi = nowSenMi;
			if (_student.IsCoach) {
				txtStartTime.setText("开始用车时间: " + timeFormat.format(startTime));
				speak(_tmp + "," + _student.Name + ",卡内剩余" + (cardBalance - 1) + "分钟,请谨慎驾驶");
			} else {
				txtStartTime.setText("训练开始时间: " + timeFormat.format(startTime));
				speak(_tmp + "," + _student.Name + ",卡内余额:" + ((cardBalance - 1) * PRICE) + "元,剩余" + (cardBalance - 1) + "分钟,请谨慎驾驶");
			}
			writeBalance(cardBalance - 1);// 重写卡内余额
			takephoto();// 拍照
		} else {
			speak(_tmp + "," + _student.Name);
			txtStartTime.setText("尚未开始训练");
			txtTrainTime.setText("自由模式");
			if (_student.IsCoach) {
				txtBalance.setText("卡内剩余时长:" + cardBalanceNow + "分钟");
			} else {
				txtBalance.setText("余额:" + cardBalanceNow * PRICE + "元,剩余" + cardBalanceNow + "分钟");
			}
		}
		// 继电器
		NativeGPIO.setRelay(true);
	}

	/**
	 * 更新训练结束时间
	 */
	private void trainUpdate() {
		if (null != endTime) {
			if (startTime.after(endTime)) {
				endTime = new Date();
				_curUUID = UUID.randomUUID().toString();
				ContentValues tcv = new ContentValues();
				tcv.put("guid", _curUUID);
				tcv.put("coach", _coach.CardNo);
				tcv.put("student", _student.CardNo);
				tcv.put("starttime", dateFormat.format(startTime));
				tcv.put("endtime", dateFormat.format(endTime));
				tcv.put("balance", String.valueOf(cardBalanceNow));
				tcv.put("startmi", String.valueOf(startMi));
				tcv.put("endmi", String.valueOf(nowMi));
				tcv.put("startsenmi", String.valueOf(startSenMi));
				tcv.put("endsenmi", String.valueOf(nowSenMi));
				tcv.put("subject", String.valueOf(subject));
				db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
			} else {
				endTime = new Date();
				ContentValues tcv = new ContentValues();
				tcv.put("endtime", dateFormat.format(endTime));
				tcv.put("balance", String.valueOf(cardBalanceNow));
				tcv.put("endmi", String.valueOf(nowMi));
				tcv.put("endsenmi", String.valueOf(nowSenMi));
				tcv.put("subject", String.valueOf(subject));
				db.update(MySQLHelper.T_ZXT_USE_DATA, tcv, "guid=?", new String[] { _curUUID });
			}
		} else {
			endTime = new Date();
			_curUUID = UUID.randomUUID().toString();
			ContentValues tcv = new ContentValues();
			tcv.put("guid", _curUUID);
			tcv.put("coach", _coach.CardNo);
			tcv.put("student", _student.CardNo);
			tcv.put("starttime", dateFormat.format(startTime));
			tcv.put("endtime", dateFormat.format(endTime));
			tcv.put("balance", String.valueOf(cardBalanceNow));
			tcv.put("startmi", String.valueOf(startMi));
			tcv.put("endmi", String.valueOf(nowMi));
			tcv.put("startsenmi", String.valueOf(startSenMi));
			tcv.put("endsenmi", String.valueOf(nowSenMi));
			tcv.put("subject", String.valueOf(subject));
			db.insert(MySQLHelper.T_ZXT_USE_DATA, null, tcv);
		}
	}

	/**
	 * 结束训练
	 */
	private void trainFinish() {
		if (null != startTime && mode == MODE_TRAIN) {
			trainUpdate();
			if (_student.IsCoach) {
				txtStartTime.setText("本次用车已结束");
				txtTrainTime.setText("共使用" + getTimeDiff(startTime, nowTime));
			} else {
				txtStartTime.setText("本次训练已结束");
				txtTrainTime.setText("共训练" + getTimeDiff(startTime, nowTime));
			}
			takephoto();// 拍照
			// 20秒之后隐藏训练信息
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(H_W_HIDE_TRAININFO);
				}
			}, 20 * 1000);
			// 语音播报余额
			speak(txtStartTime.getText().toString() + "," + txtTrainTime.getText().toString() + "," + txtBalance.getText().toString() + ",1分钟后将断油电");
			// 上传训练数据
			if (retry < 3 && !_usedataUploading) {
				uploadUseData();
			}
			// 1分钟后断电熄火
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(H_W_SET_RELAY_FALSE);
				}
			}, 60 * 1000);
		}
		_student.reset();
		showStudent(false);
		startTime = null;
		endTime = null;
		_curUUID = "";
		_cardType = NO_CARD;
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

	private int bytes2int(byte[] b) {
		int mask = 0xff;
		int temp = 0;
		int res = 0;
		for (int i = 0; i < b.length; i++) {
			res <<= 8;
			temp = b[i] & mask;
			res |= temp;
		}
		return res;
	}

	/**
	 * 每分钟执行
	 */
	private void minute() {
		// 记录/更新当前训练结束时间、余额
		if (null != startTime && mode == MODE_TRAIN) {
			trainUpdate();

			writeBalance(cardBalanceNow);// 重写卡内余额

			// 余额不足提醒
			if (cardBalanceNow <= 3) {
				toashShow("卡内剩余时长不足" + cardBalanceNow + "分钟,请注意");
			}
		}
		// 上传训练数据
		if (retry < 3 && !_usedataUploading) {
			uploadUseData();
		}
		// 上传GPS盲点
		if (retry < 3 && !_blindspotUploading) {
			uploadBlindSpot();
		}
	}

	/**
	 * 重写卡内姓名和身份证
	 */
	private void writeName() {
		_rfidProcsing = true;
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				if (_cardType == CARD_S50 || _cardType == CARD_S70) {
					_address[0] = 1;
					_address[1] = 1;
					_context = new int[48];
					for (int i = 0; i < _context.length; i++) {
						_context[i] = ' ';
					}
					// 类型：教练卡、学员卡
					if (_student.IsCoach) {
						_context[0] = 1;
					} else {
						_context[0] = 2;
					}
					// 卡号
					for (int i = 0; i < _student.CardNo.length(); i++) {
						_context[i + 1] = _student.CardNo.charAt(i);
					}
					// 驾校ID
					for (int i = 0; i < schoolID.length(); i++) {
						_context[i + 9] = schoolID.charAt(i);
					}
					// 身份证号
					for (int i = 0; i < _student.IDCardNo.length(); i++) {
						_context[i + 18] = _student.IDCardNo.charAt(i);
					}
					// 姓名
					for (int i = 0; i < _student.Name.length(); i++) {
						try {
							int j = bytes2int(String.valueOf(_student.Name.charAt(i)).getBytes("GB2312"));
							String data = Integer.toHexString(j);
							while (data.length() < 4) {
								data = "0" + data;
							}
							_context[36 + i * 2] = Integer.parseInt(data.substring(0, 2), 16);
							_context[37 + i * 2] = Integer.parseInt(data.substring(2), 16);
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
					}
					return NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context);
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				_rfidProcsing = false;
			}
		}.execute();
	}

	/**
	 * 写指纹
	 */
	private void writeFinger() {
		speak("需要采集指纹,请按右手手指");
		showDialog(DL_FINGER1);
		recordfinger1();
	}

	private void recordfinger1() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				return fingertocontext();
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_FINGER1);
				if (result == PS_OK) {
					speak("指纹采集完毕,正在记录指纹,请稍候");
					showDialog(DL_FINGER3);
					recordfinger2();
				} else {
					_rfidProcsing = false;
					toashShow("采集指纹失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	private void recordfinger2() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				_address[0] = 3;
				_address[1] = 11;
				Log.i("zhiwen", new Date().toString());
				if (NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context) == 1) {
					return 1;
				}
				SystemClock.sleep(200);
				if (NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context) == 1) {
					return 1;
				}
				SystemClock.sleep(200);
				return NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context);
			}

			@Override
			protected void onPostExecute(Integer result) {
				Log.i("zhiwen", new Date().toString());
				dismissDialog(DL_FINGER3);
				if (result == 1) {
					speak("请按左手手指..");
					showDialog(DL_FINGER2);
					recordfinger3();
				} else {
					_rfidProcsing = false;
					toashShow("记录指纹失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	private void recordfinger3() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				return fingertocontext();
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_FINGER2);
				if (result == PS_OK) {
					speak("指纹采集完毕,正在记录指纹,请稍候");
					showDialog(DL_FINGER3);
					recordfinger4();
				} else {
					_rfidProcsing = false;
					toashShow("采集指纹失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	private void recordfinger4() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				_address[0] = 14;
				_address[1] = 11;
				if (NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context) == 1) {
					return 1;
				}
				SystemClock.sleep(200);
				if (NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context) == 1) {
					return 1;
				}
				SystemClock.sleep(200);
				return NativeRFID.write_card(_address, RFID_KEY, RFID_EARA, _context);
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_FINGER3);
				_rfidProcsing = false;
				if (result == 1) {
					if (NativeRFID.write_card(new int[] { 10 }, RFID_KEY, RFID_BLOCK, new int[] { 1 }) != 1) {
						SystemClock.sleep(200);
						NativeRFID.write_card(new int[] { 10 }, RFID_KEY, RFID_BLOCK, new int[] { 1 });
					}
					toashShow("记录指纹成功,请重新插卡");
				} else {
					toashShow("记录指纹失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	private int fingertocontext() {
		// 1，检测手指并录取图像
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			SystemClock.sleep(30);
		}
		SystemClock.sleep(30);
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			SystemClock.sleep(30);
		}
		SystemClock.sleep(10);
		// 2，根据原始图像生成指纹特征
		if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A) != PS_OK) {
			SystemClock.sleep(10);
			while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
				SystemClock.sleep(30);
			}
			if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A) != PS_OK) {
				return -1;
			}
		}
		SystemClock.sleep(100);
		// 3，再一次检测手指并录取图像
		// 4，根据原始图像生成指纹特征
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			SystemClock.sleep(30);
		}
		SystemClock.sleep(30);
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			SystemClock.sleep(30);
		}
		SystemClock.sleep(10);
		if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_B) != PS_OK) {
			SystemClock.sleep(10);
			while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
				SystemClock.sleep(30);
			}
			if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_B) != PS_OK) {
				return -1;
			}
		}
		SystemClock.sleep(100);
		// 5，合成模版
		if (_fingerprint.PSRegModule(_fingerAddress) != PS_OK) {
			return -1;
		}
		SystemClock.sleep(100);
		// 6，存储到固定的某个page(0~256)
		// if (_fingerprint.PSStoreChar(_fingerAddress, CHAR_BUFFER_A, 0) !=
		// PS_OK)// 覆盖
		// {
		// return -1;
		// }
		// 特征码写到文件
		// if (_fingerprint.PSLoadChar(_fingerAddress, CHAR_BUFFER_A, 0) !=
		// PS_OK) {
		// return -1;
		// }

		// String fileName = _fingertmppath + _student.CardNo + "_1";
		// if (_address[0] == 3) {
		// fileName = _fingertmppath + _student.CardNo + "_2";
		// }
		if (_fingerprint.PSUpChar(_fingerAddress, CHAR_BUFFER_A, null, 0, _fingertmppath) != PS_OK) {
			return -1;
		}

		byte[] bs = new byte[512];
		try {
			FileInputStream fi = new FileInputStream(_fingertmppath);
			fi.read(bs, 0, bs.length);
			fi.close();
		} catch (FileNotFoundException e) {
			return 0xff;
		} catch (IOException e) {
			return 0xff;
		}
		_context = new int[512];
		for (int i = 0; i < bs.length; i++) {
			_context[i] = bs[i];
		}
		uploadFinger();// 上传指纹
		return PS_OK;
	}

	/**
	 * 上传指纹
	 */
	private void uploadFinger() {
		if (retry < 3) {
			new AsyncTask<Void, Void, Integer>() {
				@Override
				protected Integer doInBackground(Void... args) {
					Map<String, String> params = new HashMap<String, String>();
					params.put("deviceid", deviceID);// 设备号码
					params.put("session", session);// 当前会话
					params.put("cardno", _student.CardNo);
					if (_address[0] == 3) {
						params.put("n", "2");
					} else {
						params.put("n", "1");
					}
					File file = new File(_fingertmppath);
					FormFile formfile = new FormFile(file.getName(), file, "finger", "application/octet-stream");
					try {
						SocketHttpRequester.post(server + "/fingerupload.ashx", params, formfile);
						file.delete();
					} catch (Exception e) {
						e.printStackTrace();
					}
					return 0;
				}
			}.execute();
		}
	}

	/**
	 * 重写卡内余额、学时、里程
	 */
	private void writeBalance(int balance) {
		String data = Integer.toHexString(balance);
		while (data.length() < 4) {
			data = "0" + data;
		}
		int[] buffYE = new int[4];
		buffYE[0] = Integer.parseInt(data.substring(0, 2), 16);
		buffYE[1] = Integer.parseInt(data.substring(2), 16);
		buffYE[2] = Integer.parseInt(data.substring(0, 2), 16);
		buffYE[3] = Integer.parseInt(data.substring(2), 16);
		if (_cardType == CARD_24C02) {
			if (Native.chk_24c02(fd) == 0) {// AT24C02卡
				try {
					Native.swr_24c02(fd, 0x09, 0x04, buffYE);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (_cardType == CARD_S50 || _cardType == CARD_S70) {
			int[] buff = new int[7];
			// 余额
			buff[0] = buffYE[0];
			buff[1] = buffYE[1];
			// 学时
			data = Integer.toHexString(_student.RealTotalTime);
			while (data.length() < 4) {
				data = "0" + data;
			}
			buff[2] = Integer.parseInt(data.substring(0, 2), 16);
			buff[3] = Integer.parseInt(data.substring(2), 16);
			// 里程
			data = Integer.toHexString(_student.RealTotalMi);
			while (data.length() < 6) {
				data = "0" + data;
			}
			buff[4] = Integer.parseInt(data.substring(0, 2), 16);
			buff[5] = Integer.parseInt(data.substring(2, 4), 16);
			buff[6] = Integer.parseInt(data.substring(2), 16);
			NativeRFID.write_card(new int[] { 8 }, RFID_KEY, RFID_BLOCK, buff);
		}
	}

	/**
	 * 上传GPS盲点数据
	 */
	private void uploadBlindSpot() {
		_blindspotUploading = true;
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_GPS_DATA, null, null, null, null, null, null);
				if (cursor.moveToFirst()) {
					do {
						_uploadblindSpotCount++;
						HttpPost httpRequest = new HttpPost(server + "/blindspot.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(8);
						params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
						params.add(new BasicNameValuePair("session", session)); // 当前会话
						params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
						params.add(new BasicNameValuePair("gpstime", cursor.getString(cursor.getColumnIndex("gpstime"))));
						params.add(new BasicNameValuePair("lng", cursor.getString(cursor.getColumnIndex("lng"))));
						params.add(new BasicNameValuePair("lat", cursor.getString(cursor.getColumnIndex("lat"))));
						params.add(new BasicNameValuePair("speed", cursor.getString(cursor.getColumnIndex("speed"))));
						params.add(new BasicNameValuePair("senspeed", cursor.getString(cursor.getColumnIndex("senspeed"))));
						try {
							httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
							return 0;
						}
						HttpResponse httpResponse = null;
						try {
							httpResponse = new DefaultHttpClient().execute(httpRequest);
						} catch (ClientProtocolException e) {
							e.printStackTrace();
							return 0;
						} catch (IOException e) {
							e.printStackTrace();
							return 0;
						}
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							try {
								usedataresult = EntityUtils.toString(httpResponse.getEntity());
							} catch (ParseException e) {
								e.printStackTrace();
								return 0;
							} catch (IOException e) {
								e.printStackTrace();
								return 0;
							}
							if (usedataresult.equals("s")) {
								db.delete(MySQLHelper.T_ZXT_GPS_DATA, "gpstime=?", new String[] { cursor.getString(cursor.getColumnIndex("gpstime")) });
							} else {
								return 0;
							}
						} else {
							return 0;
						}
					} while (cursor.moveToNext() && _uploadblindSpotCount <= BS_EVERY_MAX);
				}
				return 1;
			}

			@Override
			protected void onPostExecute(Integer result) {
				_uploadblindSpotCount = 0;
				_blindspotUploading = false;
			}
		}.execute();
	}

	/**
	 * 上传训练数据
	 */
	private void uploadUseData() {
		_usedataUploading = true;
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_USE_DATA, null, "guid!='" + _curUUID + "'", null, null, null, null);
				if (cursor.moveToFirst()) {
					do {
						HttpPost httpRequest = new HttpPost(server + "/usedata.ashx");
						List<NameValuePair> params = new ArrayList<NameValuePair>(14);
						params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
						params.add(new BasicNameValuePair("session", session)); // 当前会话
						params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
						params.add(new BasicNameValuePair("guid", cursor.getString(cursor.getColumnIndex("guid"))));
						params.add(new BasicNameValuePair("coach", cursor.getString(cursor.getColumnIndex("coach"))));
						params.add(new BasicNameValuePair("student", cursor.getString(cursor.getColumnIndex("student"))));
						params.add(new BasicNameValuePair("starttime", cursor.getString(cursor.getColumnIndex("starttime"))));
						params.add(new BasicNameValuePair("endtime", cursor.getString(cursor.getColumnIndex("endtime"))));
						params.add(new BasicNameValuePair("balance", cursor.getString(cursor.getColumnIndex("balance"))));
						params.add(new BasicNameValuePair("startmi", cursor.getString(cursor.getColumnIndex("startmi"))));
						params.add(new BasicNameValuePair("endmi", cursor.getString(cursor.getColumnIndex("endmi"))));
						params.add(new BasicNameValuePair("startsenmi", cursor.getString(cursor.getColumnIndex("startsenmi"))));
						params.add(new BasicNameValuePair("endsenmi", cursor.getString(cursor.getColumnIndex("endsenmi"))));
						params.add(new BasicNameValuePair("subject", cursor.getString(cursor.getColumnIndex("subject"))));
						try {
							httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
						} catch (UnsupportedEncodingException e1) {
							e1.printStackTrace();
							return 0;
						}
						HttpResponse httpResponse = null;
						try {
							httpResponse = new DefaultHttpClient().execute(httpRequest);
						} catch (ClientProtocolException e1) {
							e1.printStackTrace();
							return 0;
						} catch (IOException e1) {
							e1.printStackTrace();
							return 0;
						}
						if (httpResponse.getStatusLine().getStatusCode() == 200) {
							try {
								usedataresult = EntityUtils.toString(httpResponse.getEntity());
							} catch (ParseException e) {
								e.printStackTrace();
								return 0;
							} catch (IOException e) {
								e.printStackTrace();
								return 0;
							}
							if (usedataresult.equals("s")) {
								db.delete(MySQLHelper.T_ZXT_USE_DATA, "guid=?", new String[] { cursor.getString(cursor.getColumnIndex("guid")) });
							} else {
								return 2;
							}
						} else {
							return 0;
						}
					} while (cursor.moveToNext());
				}
				return 1;
			}

			@Override
			protected void onPostExecute(Integer result) {
				_usedataUploading = false;
				if (result == 0) {
					txtUploadUseDataStatus.setText("网络异常[" + getNowTime() + "]");
				} else if (result == 2) {
					txtUploadUseDataStatus.setText(usedataresult + "[" + getNowTime() + "]");
				} else {
					txtUploadUseDataStatus.setText("训练数据上传成功[" + getNowTime() + "]");
				}
			}
		}.execute();
	}

	/**
	 * 上传照片
	 */
	private void uploadPhoto() {
		if (retry < 3 && !_photouploading) {
			_photouploading = true;
			new AsyncTask<Void, Void, Integer>() {
				@Override
				protected Integer doInBackground(Void... args) {
					Map<String, String> params = new HashMap<String, String>();
					params.put("deviceid", deviceID);// 设备号码
					params.put("session", session);// 当前会话
					File path = new File(PHOTO_PATH);
					while (path.listFiles().length > 0) {
						File file = path.listFiles()[0];
						params.put("filename", file.getName().substring(0, 16) + ".jpg");
						params.put("speed", file.getName().split("_")[2]);// GPS速度
						params.put("senspeed", file.getName().split("_")[3]);// 传感器速度
						params.put("guid", file.getName().split("_")[4].replace(".jpg", ""));// 训练任务ID
						FormFile formfile = new FormFile(file.getName(), file, "image", "application/octet-stream");
						try {
							SocketHttpRequester.post(server + "/photoupload.ashx", params, formfile);
							file.delete();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return 0;
				}

				@Override
				protected void onPostExecute(Integer result) {
					_photouploading = false;
				}
			}.execute();
		}
	}

	/**
	 * 获取教练信息
	 * 
	 * @param 卡号
	 */
	private void getCoachInfo() {
		showDialog(DL_GET_COACH);
		// speak("正在获取教练信息,请稍候...");
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				HttpPost httpRequest = new HttpPost(server + "/getcoachinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(5);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", _coach.CardNo)); // 教练卡号
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				}
				HttpResponse httpResponse;
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				}
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					}
					if (cardresult.startsWith("s|")) {
						String[] results = cardresult.split("\\|");
						if (results[1].equals(_coach.CardNo)) {
							cardBalance = Integer.parseInt(results[2]);
							_coach.CardNo = _student.CardNo = results[1];
							_coach.ID = _student.ID = results[3];
							if (!_coach.Name.equals(results[4])) {
								writeName();// 重写姓名
							}
							_coach.Name = _student.Name = results[4];
							_coach.IDCardNo = _student.IDCardNo = results[5];
							_coach.Certificate = results[6];// 教练证号
							if (_coach.IDCardNo.equals("无")) {
								_coach.IDCardNo = "";
							}
							if (_coach.Certificate.equals("无")) {
								_coach.Certificate = "";
							}
							return 1;
						} else {
							return 9;
						}
					} else if (cardresult.equals("version_error")) {
						return 2;
					} else {
						return 3;
					}
				} else {
					retry++;
					changeServer();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_GET_COACH);
				if (result == 0) {
					trainBegin();
				} else if (result == 1) {
					showCoach(true);
					showStudent(true);
					trainBegin();
				} else if (result == 2) {
					versionUpdate();// 更新程序
				} else if (result == 3) {
					toashShow(cardresult.split("\\|")[1]);// 提示错误
				}
			}
		}.execute();
	}

	/**
	 * 获取学员信息
	 * 
	 * @param 卡号
	 */
	private void getStudentInfo() {
		showDialog(DL_GET_STUDENT);
		// speak("正在获取学员信息,请稍候...");
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				HttpPost httpRequest = new HttpPost(server + "/getstudentinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(5);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", _student.CardNo)); // 学员卡号
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				HttpResponse httpResponse;
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				}
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					}
					if (cardresult.startsWith("s|")) {
						String[] results = cardresult.split("\\|");
						if (results.length > 1 && results[1].equals(_student.CardNo)) {
							cardBalance = Integer.parseInt(results[2]);
							if (!_student.Name.equals(results[4])) {
								writeName();// 重写姓名和身份证
							}
							_student.ID = results[3];
							_student.Name = results[4];
							_student.IDCardNo = results[5];
							_student.DriverType = results[6];
							if (_student.IDCardNo.equals("无")) {
								_student.IDCardNo = "";
							}
							if (_student.DriverType.equals("无")) {
								_student.DriverType = "";
							}
							return 1;
						} else {
							return 5;
						}
					} else if (cardresult.equals("version_error")) {
						return 2;
					} else {
						return 3;
					}
				} else {
					retry++;
					changeServer();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_GET_STUDENT);
				if (result == 0) {
					trainBegin();
				} else if (result == 1) {
					showStudent(true);
					getTotalTimeAndMi();
					trainBegin();
				} else if (result == 2) {
					versionUpdate();// 更新程序
				} else if (result == 3) {
					toashShow(cardresult.split("\\|")[1]);// 提示错误
				}
			}
		}.execute();
	}

	/**
	 * 获取累计学时和里程
	 */
	private void getTotalTimeAndMi() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				HttpPost httpRequest = new HttpPost(server + "/gettimeandmi.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(4);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("stuid", _student.ID)); // 学员编号
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				HttpResponse httpResponse;
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						cardresult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						return 0;
					}
					if (cardresult.startsWith("s|")) {
						String[] results = cardresult.split("\\|");
						if (results.length > 1) {
							_student.TotalTime = Integer.parseInt(results[1]);
						}
						if (results.length > 2) {
							_student.TotalMi = Integer.parseInt(results[2]);
						}
						return 1;
					}
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					txtStudentTotalTime.setText("累计学时:" + (_student.TotalTime / 60) + "小时" + (_student.TotalTime % 60) + "分钟");
					txtStudentTotalMi.setText("累计里程:" + (_student.TotalMi / 1000) + "KM");
				}
			}
		}.execute();
	}

	/**
	 * 上传GPS数据
	 */
	private void upload() {
		txtStatus.setText("正在上传GPS数据[" + getNowTime() + "]");
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				HttpPost httpRequest = new HttpPost(server + "/upload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(8);
				if (_student.CardNo.equals("") || startTime == null) {
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(speed))); // 速度
					params.add(new BasicNameValuePair("senspeed", String.valueOf(senspeed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", _coach.CardNo)); // 教练
				} else {
					params = new ArrayList<NameValuePair>(12);
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("lng", String.format("%.6f", lng))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", lat))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(speed))); // 速度
					params.add(new BasicNameValuePair("senspeed", String.valueOf(senspeed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(mode))); // 模式
					params.add(new BasicNameValuePair("coach", _coach.CardNo)); // 教练
					params.add(new BasicNameValuePair("student", _student.CardNo)); // 学员
					params.add(new BasicNameValuePair("starttime", dateFormat.format(startTime))); // 开始训练时间
					params.add(new BasicNameValuePair("balance", String.valueOf(cardBalanceNow))); // 余额
					params.add(new BasicNameValuePair("subject", String.valueOf(subject))); // 训练科目
				}
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				}
				HttpResponse httpResponse = null;
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					retry++;
					changeServer();
					return 0;
				}
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						uploadresult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						retry++;
						changeServer();
						return 0;
					}
					if (uploadresult.equals("s")) {
						return 1;
					} else {
						return 2;
					}
				} else {
					retry++;
					changeServer();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					retry = 0;
					txtNetworkStatus.setText("网络正常");
					txtNetworkStatus.setTextColor(Color.BLACK);
					txtStatus.setText("GPS数据上传成功[" + getNowTime() + "]");
				} else {
					// 记录盲点
					ContentValues tcv = new ContentValues();
					tcv.put("gpstime", dateFormat.format(new Date()));
					tcv.put("lng", String.format("%.6f", lng));
					tcv.put("lat", String.format("%.6f", lat));
					tcv.put("speed", String.valueOf(speed));
					tcv.put("senspeed", String.valueOf(senspeed));
					db.insert(MySQLHelper.T_ZXT_GPS_DATA, null, tcv);
					if (result == 2) {
						txtStatus.setText(uploadresult + "[" + getNowTime() + "]");
					} else {
						txtStatus.setText("网络异常(" + retry + ")[" + getNowTime() + "]");
						if (retry >= 3) {
							txtNetworkStatus.setText("网络异常");
							txtNetworkStatus.setTextColor(Color.RED);
						}
					}
				}
			}
		}.execute();
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
			txtCoachName.setText(_coach.Name);
			txtCoachCard.setText("卡号:" + _coach.CardNo);
			txtCoachCertificate.setText(_coach.Certificate);
			layCoachInfo.setVisibility(View.VISIBLE);
			txtInfoCoach.setVisibility(View.GONE);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", _coach.ID);
			editor.putString("coachCard", _coach.CardNo);
			editor.putString("coachName", _coach.Name);
			editor.putString("coachIDCard", _coach.IDCardNo);
			editor.putString("coachCertificate", _coach.Certificate);
			editor.commit();
			imgCoach.setImageResource(R.drawable.photo);
			if (!_coach.IDCardNo.equals("")) {
				imgCoach.setImageUrl(server + "/" + _coach.IDCardNo + ".bmp");
			}
		} else {
			layCoachTitle.setBackgroundResource(R.drawable.bg1);
			txtInfoStudent.setText("请先插教练卡");
			txtCoachName.setText("");
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
			layStudentInfo.setVisibility(View.VISIBLE);
			txtInfoStudent.setVisibility(View.GONE);
			txtStudentName.setText(_student.Name);
			txtStudentCard.setText("卡号:" + _student.CardNo);
			if (_student.IsCoach) {
				txtStudentTitle.setText("教练:");
				txtStudentID.setText("教练编号:" + _student.ID);
			} else {
				txtStudentTitle.setText("学员:");
				txtStudentID.setText("学员编号:" + _student.ID);
				txtStudentDriverType.setText("准驾车型:" + _student.DriverType);
				txtStudentTotalTime.setText("累计学时:" + (_student.TotalTime / 60) + "小时" + (_student.TotalTime % 60) + "分钟");
				txtStudentTotalMi.setText("累计里程:" + (_student.TotalMi / 1000) + "KM");
			}
			if (!_student.IDCardNo.equals("")) {
				imgStudent.setImageUrl(server + "/" + _student.IDCardNo + ".bmp");
			}
		} else {
			txtStudentTitle.setText("学员");
			layStudentTitle.setBackgroundResource(R.drawable.bg1);
			layStudentInfo.setVisibility(View.GONE);
			txtInfoStudent.setVisibility(View.VISIBLE);
			imgStudent.setImageResource(R.drawable.photo);
			txtStudentName.setText("");
			txtStudentCard.setText("");
			txtStudentID.setText("");
			txtStudentDriverType.setText("");
			txtStudentTotalTime.setText("");
			txtStudentTotalMi.setText("");
		}
	}

	/**
	 * 隐藏训练提示信息(训练时长、余额)
	 */
	private void hideTrainInfo() {
		if (null == startTime) {
			txtStartTime.setText("尚未开始训练");
			txtTrainTime.setText("");
			txtBalance.setText("");
		}
	}

	/**
	 * 切换状态
	 * 
	 * @param 训练状态0
	 *            ,自由状态1
	 */
	private void changeMode(int m) {
		if (m == MODE_TRAIN) {
			if (mode == MODE_FREE) {
				mode = MODE_TRAIN;
				btnFJFMS.setBackgroundResource(R.drawable.button_bg);
				txtFJFMS.setTextColor(R.color.button_text);
				btnJFMS.setBackgroundResource(R.drawable.button_checked_bg);
				txtJFMS.setTextColor(Color.WHITE);
				if (_cardType == NO_CARD) {
					toashShow("已经切换为训练状态,请插卡,否则一分钟后将断油电");
				} else {
					toashShow("已经切换为训练状态");
				}
				// 继电器
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						handler.sendEmptyMessage(H_W_SET_RELAY_FALSE);
					}
				}, 60 * 1000);
				startTime = null;
				endTime = null;
				_curUUID = "";
				_cardType = NO_CARD;
			}
		} else {
			if (mode == MODE_TRAIN) {
				if (startTime != null) {
					trainFinish();// 结束训练
				}
				mode = MODE_FREE;
				btnJFMS.setBackgroundResource(R.drawable.button_bg);
				txtJFMS.setTextColor(R.color.button_text);
				btnFJFMS.setBackgroundResource(R.drawable.button_checked_bg);
				txtFJFMS.setTextColor(Color.WHITE);
				toashShow("已经切换为自由状态");
				NativeGPIO.setRelay(true);
			}
		}
	}

	/**
	 * 程序更新
	 */
	private void versionUpdate() {
		speak("发现新版本,需要更新程序");
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("发现新版本,需要更新").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				_updateDialog = new ProgressDialog(MainActivity.this);
				_updateDialog.setMessage("正在等待下载...");
				_updateDialog.setIndeterminate(true);
				_updateDialog.show();
				downloadAPK();
			}
		}).create();
		alertDialog.show();
	}

	private void downloadAPK() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				try {
					/*
					 * 连接到服务器
					 */
					URL url = new URL(server + "/zpad.apk");
					URLConnection connection = url.openConnection();
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					/*
					 * 文件的保存路径和和文件名其中ExsunAndroid.apk是在手机SD卡上要保存的路径，如果不存在则新建
					 */
					String savePath = Environment.getExternalStorageDirectory() + "/download";
					File file = new File(savePath);
					if (!file.exists()) {
						file.mkdir();
					}
					String savePathString = Environment.getExternalStorageDirectory() + "/download/" + "zpad.apk";
					_downLoadFile = new File(savePathString);
					if (_downLoadFile.exists()) {
						_downLoadFile.delete();
					}
					_downLoadFile.createNewFile();
					/*
					 * 向SD卡中写入文件,用Handle传递线程
					 */
					OutputStream outputStream = new FileOutputStream(_downLoadFile);
					_fileLength = connection.getContentLength();
					handler.sendEmptyMessage(H_W_UPDATEDIALOG_MAX);
					byte[] buffer = new byte[128];
					while (_downedFileLength < _fileLength) {
						int numRead = inputStream.read(buffer);
						_downedFileLength += numRead;
						outputStream.write(buffer, 0, numRead);
						handler.sendEmptyMessage(H_W_UPDATEDIALOG_NOW);
					}
					return 1;
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					_updateDialog.setMessage("下载完成！");
					_updateDialog.dismiss();
					installFile(_downLoadFile);
				}
			}
		}.execute();
	}

	/**
	 * 安装程序
	 * 
	 * @param f
	 */
	private void installFile(File f) {
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(android.content.Intent.ACTION_VIEW);
		// 获得下载好的文件类型
		String type = "application/vnd.android.package-archive";
		// 打开各种类型文件
		intent.setDataAndType(Uri.fromFile(f), type);
		// 安装
		startActivity(intent);
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

	/**
	 * 阻止返回键
	 */
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

	/**
	 * 阻止home键
	 */
	@Override
	public void onAttachedToWindow() {
		this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}

	@Override
	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case DL_CARD_READING:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("正在读卡,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_DOWN_FINGER:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("正在获取指纹信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_VALI_FINGER:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("请按手指...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_CARD_INIT:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("正在初始化卡,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_FINGER1:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("需要采集指纹,请按右手手指...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_FINGER2:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("请按左手手指...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_FINGER3:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("指纹采集完毕,正在记录指纹,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_GET_COACH:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("正在获取教练信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		case DL_GET_STUDENT:
			return new AlertDialog.Builder(MainActivity.this).setTitle("警告").setMessage("正在获取学员信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
		default:
			return null;
		}
	}

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// log it when the location changes
			if (location != null) {
				lng = location.getLongitude();
				lat = location.getLatitude();
				speed = Math.round(location.getSpeed() / NMDIVIDED * 60 * 60 / 1000);
				nowMi += location.getSpeed() / NMDIVIDED;
				txtLngLat.setText("GPS:" + String.format("%.2f", lng) + "," + String.format("%.2f", lat) + "," + speed + ";");
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
		// 关闭TCP监听
		try {
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 定时器
		_timerUpload.cancel();
		_timerFlicker.cancel();
		_timerSecond.cancel();
		_timerMinute.cancel();
		_timerCamera.cancel();
		_timerUploadPhoto.cancel();
		// IC卡读卡器
		try {
			Native.ic_exit(fd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// RFID读卡器
		try {
			NativeRFID.close_fm1702();
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
		if (db != null) {
			try {
				db.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 指纹
		if (_fingerprint != null) {
			try {
				_fingerprint.PSCloseDevice();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			lytfingerprint.Close();
		} catch (Exception e) {
			e.printStackTrace();
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