package cn.whzxt.android;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
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
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

public class MainActivity extends Activity implements OnInitListener, SurfaceHolder.Callback, OnGestureListener {
	private TextView txtSchoolName, txtDeviceName, txtSystemTime;
	private TextView txtStudentTitle, txtStatus, txtGPSSpeed, txtSensor;
	private TextView txtCoachName, txtCoachCard, txtStudentName, txtStudentCard;
	private TextView txtTrainTime, txtBalance;
	private TextView txtNetworkStatus, txtGPSStatus;
	private TextView txtLogger, txtDebug;
	private TextView txtMode, txtModeInfo;
	private TextView txtSubject, txtSubjectInfo;
	private TextView txtDetailStudentCard, txtDetailStudentStatus;
	private TextView txtDetailStudentLjxs, txtDetailStudentLjlc, txtDetailStudentBrxs, txtDetailStudentBrlc;
	private TextView txtDetailStudentBrsx;
	private TextView txtDetailCoachNo, txtDetailCoachCard, txtDetailCoachLevel, txtDetailCoachBrpx, txtDetailCoachBypx;
	private LinearLayout btnSubject, btnRestart, btnShutdown, btnReturnToMain;
	private NetImageView imgCoach, imgStudent;
	private ImageView imgLast01, imgLast02, imgLast03, imgLast04;
	private LinearLayout layCoachTitle, layStudentTitle, layStudentPhoto, layStudentDetail, layCoachPhoto, layCoachDetail;
	private LinearLayout btnMode, btnLogger, btnLoggerReturn;
	private LinearLayout layTts, layScrollDown;
	private ViewFlipper flipper, flipperStudent, flipperCoach;
	private GestureDetector detector;
	private SurfaceView previewSurface;
	private SurfaceHolder previewSurfaceHolder;
	private String deviceID, deviceName, schoolID, schoolName, session;
	private String server;
	private String uploadresult, cardresult, usedataresult, ttsdataresult;
	private Date nowTime;
	private int retry = 0;
	private SimpleDateFormat dateFormat, timeFormat;
	private LocationManager locationManager;
	private Timer _timerUpload, _timerSecond, _timerMinute, _timerCamera, _timerUploadPhoto, _timerSensor;
	private Timer _timerTakePhoto2;
	private SharedPreferences settings;
	private MySQLHelper sqlHelper;
	private SQLiteDatabase db;
	private Boolean _usedataUploading = false;
	private Boolean _blindspotUploading = false;
	private HashMap<String, String> _hashTts;
	private String _ttsVer;
	private String _tcpMsg;
	private Paint _markPaint;
	private int _uploadblindSpotCount = 0;
	private static final int BS_EVERY_MAX = 10;// 每次上传10个盲点
	private int _relayoff = 0;// 继电器
	private Boolean _jdq_ck = false;
	private long _gpstime_now = 0, _gpstime_pre = 0;
	private float _sts;
	private Boolean _needFingerImage = false;
	// private int dogfd = -1;

	private ServerSocket socket = null;
	private Socket client = null;

	private static final int MODE_TRAIN = 0;
	private static final int MODE_FREE = 1;

	private int s2_min_time = 5;// 科目二每次训练最短时间
	private int s2_max_time = 30;// 科目二每次训练最长时间
	private int s2_day_time = 120;// 科目二每日训练最长时间
	private int s3_min_time = 10;// 科目三每次训练最短时间
	private int s3_max_time = 30;// 科目三每次训练最长时间
	private int s3_day_time = 120;// 科目三每日训练最长时间

	private static final int DBVERSION = 10;// 数据库版本
	private static final int PRICE = 2; // 设备单价
	private float NMDIVIDED = 1.852f; // 海里换算成公里
	// 读卡器
	private CardOper cardOper = null;
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
	private int _fingerCurId;
	private Boolean _fingerFinish = false;
	private int _fingerRetry = 0;
	private byte[] _pImageData = new byte[256 * 288];
	private int _iImageLength;
	// 摄像头
	private Camera _camera;
	private static final String PHOTO_PATH = "/sdcard/zxtphoto";// 照片保存路径
	private static final int PHOTO_DEF_INTERVAL = 60; // 默认拍照间隔(秒)
	private int _curCamera = 1;// 当前摄像头
	private Boolean _previewing = false;
	private Boolean _photouploading = false;
	private Boolean _takephotoing = false;
	private String _phototime = "";
	private String _photoUUID = "";
	private int _photoSpeed = 0;
	private int _photoSenSpeed = 0;
	private Bitmap[] _lastBmps;
	private StreamIt streamIt = null;
	private PhotoServer photoServer;
	private int cameracount;
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
	private static final int DL_LOAD_TTSBUTTON = 0x09;
	// Handle_What
	private static final int H_W_UPLOAD = 0x01;
	private static final int H_W_SECOND = 0x03;
	private static final int H_W_MINUTE = 0x04;
	private static final int H_W_TAKE_PHOTO = 0x05;
	private static final int H_W_TAKE_PHOTO1 = 0x24;
	private static final int H_W_TAKE_PHOTO2 = 0x25;
	private static final int H_W_UPLOAD_PHOTO = 0x06;
	private static final int H_W_HIDE_TRAININFO = 0x08;
	private static final int H_W_SHOW_SENSOR = 0x0A;
	private static final int H_W_UPDATEDIALOG_MAX = 0x14;
	private static final int H_W_UPDATEDIALOG_NOW = 0x15;
	private static final int H_W_TRAIN_START = 0x17;
	private static final int H_W_TRAIN_END = 0x18;
	private static final int H_W_GET_COACHINFO = 0x31;
	private static final int H_W_GET_STUDENTINFO = 0x32;
	private static final int H_W_DOWN_FINGER = 0x33;
	private static final int H_W_WRITE_FINGER = 0x34;
	private static final int H_W_TAKE_FINGER_PHOTO = 0x3A;
	private static final int H_W_SHOW_STUDENT = 0x3B;
	private static final int H_W_SHOW_COACH = 0x3C;
	// 程序更新对话框
	private ProgressDialog _updateDialog;
	private File _downLoadFile;
	private int _fileLength, _downedFileLength = 0;

	private Boolean _isclosed = false;
	private Boolean _sensorTimerFlag = false;
	private Boolean _secondTimerFlag = false;
	private Boolean _ttsButtonLoaded = false;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case H_W_UPLOAD:
				upload();
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
			case H_W_TAKE_PHOTO1:
				takephoto1();
				break;
			case H_W_TAKE_PHOTO2:
				takephoto2();
				break;
			case H_W_UPLOAD_PHOTO:
				uploadPhoto();
				break;
			case H_W_HIDE_TRAININFO:
				hideTrainInfo();
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
			case H_W_TRAIN_START:
				trainBegin();
				break;
			case H_W_TRAIN_END:
				trainFinish();
				break;
			case H_W_GET_COACHINFO:
				getCoachInfo();
				break;
			case H_W_GET_STUDENTINFO:
				getStudentInfo();
				break;
			case H_W_DOWN_FINGER:
				_fingerRetry = 0;
				downFinger();
				break;
			case H_W_WRITE_FINGER:
				writeFinger();
				break;
			case H_W_TAKE_FINGER_PHOTO:
				takeFingerPhotoDelay();
				break;
			case H_W_SHOW_STUDENT:
				showStudent(true);
				break;
			case H_W_SHOW_COACH:
				showCoach(true);
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			if (NativeGPIO.getAccState() == 0) {
				speak("欢迎使用,车载智能驾培终端");
			} else {
				if (DeviceInfo.Mode == MODE_TRAIN) {
					speak("欢迎使用,车载智能驾培终端,请插卡并验证指纹,否则一分钟后将断油电");
				} else {
					speak("欢迎使用,车载智能驾培终端");
				}
			}
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
		detector = new GestureDetector(this);
		try {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName("cn.whzxt.gps", "cn.whzxt.gps.ZxtService"));
			startService(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		/*
		 * if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
		 * { // 打开GPS try { Intent intent = new Intent();
		 * intent.setComponent(new ComponentName("cn.whzxt.gps",
		 * "cn.whzxt.gps.ZxtOpenGPSActivity")); startActivity(intent); } catch
		 * (Exception e) { e.printStackTrace(); } }
		 */
		streamIt = new StreamIt();
		_lastBmps = new Bitmap[4];
		server = getString(R.string.server1);
		settings = getSharedPreferences("whzxt.net", 0);
		nowTime = new Date();
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		timeFormat = new SimpleDateFormat("HH:mm:ss");
		// 照片压缩比
		streamIt.options.inSampleSize = settings.getInt("inSampleSize", 4);
		// 训练科目
		DeviceInfo.Subject = settings.getInt("subject", 2);
		// 状态
		DeviceInfo.Mode = settings.getInt("mode", MODE_TRAIN);
		// 继电器是否长开
		_jdq_ck = settings.getBoolean("jdq_ck", false);
		// 摄像头数量
		cameracount = settings.getInt("camera", 1);
		// 脉冲数 / sts = 速度
		_sts = settings.getFloat("sts", 1.32f);
		// GPS速度单位,1:公里,2:海里
		if (settings.getInt("gst", 1) == 1) {
			NMDIVIDED = 1.0f;
		}
		// 是否需要上传指纹图片
		_needFingerImage = settings.getBoolean("needfingerimage", false);
		/*
		 * if (_needFingerImage) { // 获取指纹图片需要用到 for (int i = 0; i < 256 * 288;
		 * i++) { _pImageData[i] = 5; } }
		 */
		// 训练时长限制
		s2_min_time = settings.getInt("s2_min_time", 5);
		s2_max_time = settings.getInt("s2_max_time", 30);
		s2_day_time = settings.getInt("s2_day_time", 120);
		s3_min_time = settings.getInt("s3_min_time", 10);
		s3_max_time = settings.getInt("s3_max_time", 30);
		s3_day_time = settings.getInt("s3_day_time", 120);
		// 数据库
		sqlHelper = new MySQLHelper(this, "zxt.db", null, DBVERSION);
		db = sqlHelper.getWritableDatabase();
		// 照片水印
		_markPaint = new Paint();
		_markPaint.setColor(Color.RED);
		_markPaint.setTypeface(Typeface.create("宋体", Typeface.NORMAL));
		_markPaint.setTextSize(14);
		// 控制屏幕长亮
		PowerManager manager = ((PowerManager) getSystemService(POWER_SERVICE));
		wakeLock = manager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "ATAAW");
		wakeLock.acquire();

		// 设备和驾校信息
		deviceID = settings.getString("deviceID", "");
		deviceName = settings.getString("deviceName", "");
		schoolID = settings.getString("schoolID", "");
		schoolName = settings.getString("schoolName", "");
		session = settings.getString("session", "");
		retry = 0;
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null && bundle.getString("offline") != null) {
			retry = 3;
		}

		// 教练信息
		Coach.Init();
		Student.Init();
		if (settings.getString("coachCard", "") != "") {
			Coach.ID = settings.getString("coachID", "");
			Coach.Name = settings.getString("coachName", "");
			Coach.CardNo = settings.getString("coachCard", "");
			Coach.IDCardNo = settings.getString("coachIDCard", "");
			Coach.Certificate = settings.getString("coachCertificate", "");
		}

		// 界面初始化
		initView();

		// 模块初始化
		initMoudle();

		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		txtDebug.setText("IMEI:" + tm.getDeviceId() + " 设备号:" + deviceID + "\n摄像头个数:" + cameracount + " 继电器:" + (_jdq_ck ? "常开" : "常闭") + "\n程序启动:" + dateFormat.format(nowTime) + " 版本:" + getString(R.string.version));

		// GPS
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

		// TCP监听
		try {
			socket = new ServerSocket(8888);
			socketListen();// 监听指令
		} catch (IOException e) {
			e.printStackTrace();
		}

		cardOper.selectRfidType(CardOper.RFID_TYPE_A);

		// 定时器
		initTimer();

		if (DeviceInfo.Mode == MODE_TRAIN) {
			_relayoff = 60;
		}
		// 看门狗
		// dogfd = com.lyt.watchdog.Native.init();
		// com.lyt.watchdog.Native.settimeout(6);

		// 启动视频监控服务
		try {
			photoServer = new PhotoServer(9999, streamIt);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 开始读卡
		cardOper.start();
	}

	/**
	 * 初始化界面
	 */
	private void initView() {
		flipper = (ViewFlipper) findViewById(R.id.flipper);
		flipperStudent = (ViewFlipper) findViewById(R.id.flipperStudent);
		flipperCoach = (ViewFlipper) findViewById(R.id.flipperCoach);
		txtSchoolName = (TextView) findViewById(R.id.txtSchoolName);
		txtDeviceName = (TextView) findViewById(R.id.txtDeviceName);
		txtSystemTime = (TextView) findViewById(R.id.txtSystemTime);
		txtStudentTitle = (TextView) findViewById(R.id.txtStudentTitle);
		layCoachTitle = (LinearLayout) findViewById(R.id.layCoachTitle);
		layStudentTitle = (LinearLayout) findViewById(R.id.layStudentTitle);
		layStudentDetail = (LinearLayout) findViewById(R.id.layStudentDetail);
		layStudentPhoto = (LinearLayout) findViewById(R.id.layStudentPhoto);
		layCoachDetail = (LinearLayout) findViewById(R.id.layCoachDetail);
		layCoachPhoto = (LinearLayout) findViewById(R.id.layCoachPhoto);
		btnMode = (LinearLayout) findViewById(R.id.btnMode);
		btnSubject = (LinearLayout) findViewById(R.id.btnSubject);
		btnRestart = (LinearLayout) findViewById(R.id.btnRestart);
		btnShutdown = (LinearLayout) findViewById(R.id.btnShutdown);
		btnReturnToMain = (LinearLayout) findViewById(R.id.btnReturnToMain);
		btnLogger = (LinearLayout) findViewById(R.id.btnLogger);
		btnLoggerReturn = (LinearLayout) findViewById(R.id.btnLoggerReturn);
		txtLogger = (TextView) findViewById(R.id.txtLogger);
		txtDebug = (TextView) findViewById(R.id.txtDebug);
		txtSubject = (TextView) findViewById(R.id.txtSubject);
		txtSubjectInfo = (TextView) findViewById(R.id.txtSubjectInfo);
		layTts = (LinearLayout) findViewById(R.id.layTts);
		layScrollDown = (LinearLayout) findViewById(R.id.layScrollDown);
		previewSurface = (SurfaceView) findViewById(R.id.previewSurface);
		imgLast01 = (ImageView) findViewById(R.id.imgLast01);
		imgLast02 = (ImageView) findViewById(R.id.imgLast02);
		imgLast03 = (ImageView) findViewById(R.id.imgLast03);
		imgLast04 = (ImageView) findViewById(R.id.imgLast04);
		txtMode = (TextView) findViewById(R.id.txtMode);
		txtModeInfo = (TextView) findViewById(R.id.txtModeInfo);
		txtCoachName = (TextView) findViewById(R.id.txtCoachName);
		txtCoachCard = (TextView) findViewById(R.id.txtCoachCard);
		txtStudentName = (TextView) findViewById(R.id.txtStudentName);
		txtStudentCard = (TextView) findViewById(R.id.txtStudentCard);
		txtDetailStudentCard = (TextView) findViewById(R.id.txtDetailStudentCard);
		txtDetailStudentStatus = (TextView) findViewById(R.id.txtDetailStudentStatus);
		txtDetailStudentLjxs = (TextView) findViewById(R.id.txtDetailStudentLjxs);
		txtDetailStudentLjlc = (TextView) findViewById(R.id.txtDetailStudentLjlc);
		txtDetailStudentBrxs = (TextView) findViewById(R.id.txtDetailStudentBrxs);
		txtDetailStudentBrlc = (TextView) findViewById(R.id.txtDetailStudentBrlc);
		txtDetailStudentBrsx = (TextView) findViewById(R.id.txtDetailStudentBrsx);
		txtDetailCoachCard = (TextView) findViewById(R.id.txtDetailCoachCard);
		txtDetailCoachNo = (TextView) findViewById(R.id.txtDetailCoachNo);
		txtDetailCoachLevel = (TextView) findViewById(R.id.txtDetailCoachLevel);
		txtDetailCoachBrpx = (TextView) findViewById(R.id.txtDetailCoachBrpx);
		txtDetailCoachBypx = (TextView) findViewById(R.id.txtDetailCoachBypx);
		imgCoach = (NetImageView) findViewById(R.id.imgCoach);
		imgStudent = (NetImageView) findViewById(R.id.imgStudent);
		txtTrainTime = (TextView) findViewById(R.id.txtTrainTime);
		txtBalance = (TextView) findViewById(R.id.txtBalance);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		txtGPSSpeed = (TextView) findViewById(R.id.txtGPSSpeed);
		txtSensor = (TextView) findViewById(R.id.txtSensor);
		txtNetworkStatus = (TextView) findViewById(R.id.txtNetworkStatus);
		txtGPSStatus = (TextView) findViewById(R.id.txtGPSStatus);
		if (retry >= 3) {
			txtNetworkStatus.setText("网络异常");
			txtNetworkStatus.setTextColor(Color.RED);
		}
		txtSchoolName.setText(schoolName);
		txtDeviceName.setText(deviceName);
		txtSystemTime.setText(dateFormat.format(nowTime));
		if (DeviceInfo.Subject == 3) {
			txtSubject.setText("科目三");
			txtSubjectInfo.setText("点击切换为科目二");
		}
		if (DeviceInfo.Mode == MODE_FREE) {
			txtMode.setText("自由状态");
			txtModeInfo.setText("点击切换为训练状态");
			NativeGPIO.setRelay(_jdq_ck);
		}
		showStudent(false);
		showCoach(!Coach.CardNo.equals(""));
		// 科目切换
		btnSubject.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (Student.Subject < 1) {
					if (DeviceInfo.Subject == 3) {
						DeviceInfo.Subject = 2;
						txtSubject.setText("科目二");
						txtSubjectInfo.setText("点击切换为科目三");
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt("subject", DeviceInfo.Subject);
						editor.commit();
						speak("已经切换为科目二");
					} else {
						DeviceInfo.Subject = 3;
						txtSubject.setText("科目三");
						txtSubjectInfo.setText("点击切换为科目二");
						SharedPreferences.Editor editor = settings.edit();
						editor.putInt("subject", DeviceInfo.Subject);
						editor.commit();
						speak("已经切换为科目三");
					}
				} else {
					if (DeviceInfo.Subject == 2) {
						toastShow("当前学员不能训练科目三");
					} else {
						toastShow("当前学员不能训练科目二");
					}
				}
			}
		});

		layScrollDown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (!_ttsButtonLoaded) {
					ttsButtonInit();
				}
				flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_out));
				flipper.showNext();
			}
		});

		// 状态切换
		btnMode.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (DeviceInfo.Mode == MODE_FREE) {
					speak("确定要切换为训练状态吗？");
					AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("确定要切换为训练状态吗？").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							changeMode(MODE_TRAIN);
						}
					}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					}).create();
					alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_HOME)
								return true;
							return false;
						}
					});
					alertDialog.show();
					alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
				} else {
					speak("请输入密码");
					final EditText txtpsd = new EditText(MainActivity.this);
					AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("请输入密码").setIcon(android.R.drawable.ic_menu_help).setView(txtpsd).setPositiveButton("确定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (txtpsd.getText().toString().toLowerCase().equals(settings.getString("offlinepassword", "").toLowerCase())) {
								changeMode(MODE_FREE);
							} else {
								toastShow("密码错误");
							}
						}
					}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					}).create();
					alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
						public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
							if (keyCode == KeyEvent.KEYCODE_HOME)
								return true;
							return false;
						}
					});
					alertDialog.show();
					alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
				}
			}
		});

		txtStatus.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (flipper.getDisplayedChild() != 2) {
					flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_in));
					flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_out));
					flipper.setDisplayedChild(2);
				}
			}
		});

		btnRestart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				exitConfirm();
			}
		});

		btnShutdown.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("确定要复位？").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						com.lyt.watchdog.Native.init();
						com.lyt.watchdog.Native.settimeout(1);
					}
				}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				}).create();
				alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (keyCode == KeyEvent.KEYCODE_HOME)
							return true;
						return false;
					}
				});
				alertDialog.show();
				alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
			}
		});

		btnReturnToMain.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_out));
				flipper.setDisplayedChild(0);
			}
		});
		btnLoggerReturn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipper.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_out));
				flipper.setDisplayedChild(0);
			}
		});

		txtLogger.setMovementMethod(ScrollingMovementMethod.getInstance());
		btnLogger.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				txtLogger.setText(Logger.Read());
			}
		});

		imgLast01.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_lastBmps[0] != null) {
					// PhotoViewDialog pvd = new
					// PhotoViewDialog(MainActivity.this);
					// pvd.setImage(_lastBmps[0]);
					// pvd.show();
					// camView.setImageBitmap(_lastBmps[0]);
				}
			}
		});

		imgLast02.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_lastBmps[1] != null) {
					// camView.setImageBitmap(_lastBmps[1]);
				}
			}
		});

		imgLast03.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_lastBmps[2] != null) {
					// camView.setImageBitmap(_lastBmps[2]);
				}
			}
		});

		imgLast04.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (_lastBmps[3] != null) {
					// camView.setImageBitmap(_lastBmps[3]);
				}
			}
		});

		// 点击学员头像显示详细信息
		layStudentPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipperStudent.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_in));
				flipperStudent.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_out));
				flipperStudent.setDisplayedChild(1);
			}
		});

		// 点击学员详细信息返回学员头像
		layStudentDetail.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipperStudent.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_in));
				flipperStudent.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_out));
				flipperStudent.setDisplayedChild(0);
			}
		});

		// 点击教练头像显示详细信息
		layCoachPhoto.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipperCoach.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_in));
				flipperCoach.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_right_out));
				flipperCoach.setDisplayedChild(1);
			}
		});

		// 点击教练详细信息返回学员头像
		layCoachDetail.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				flipperCoach.setInAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_in));
				flipperCoach.setOutAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.layout.push_left_out));
				flipperCoach.setDisplayedChild(0);
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

		// 音量调节
		AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		VerticalSeekBar vSeekBar = (VerticalSeekBar) findViewById(R.id.SeekBarVolume);
		vSeekBar.setMax(am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
		vSeekBar.setProgress(am.getStreamVolume(AudioManager.STREAM_MUSIC));
		vSeekBar.setOnSeekBarChangeListener(new VerticalSeekBar.OnSeekBarChangeListener() {

			public void onStopTrackingTouch(VerticalSeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStartTrackingTouch(VerticalSeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onProgressChanged(VerticalSeekBar seekBar, int progress, boolean fromUser) {
				((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setStreamVolume(AudioManager.STREAM_MUSIC, seekBar.getProgress(), AudioManager.FLAG_PLAY_SOUND);
			}
		});
	}

	/**
	 * 初始化模块
	 */
	private void initMoudle() {
		// 初始化TTS
		Intent checkIntent = new Intent();
		try {
			checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
			startActivityForResult(checkIntent, REQ_TTS_STATUS_CHECK);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 打开读卡器
		cardOper = new CardOper(schoolID, cardOnChange);
		cardOper.openICCard();
		cardOper.openRfidCard();
		// 指纹模块
		try {
			_fingerprint = new lytfingerprint();
			lytfingerprint.Open();
			if (_fingerprint.PSOpenDevice(1, 0, 57600 / 9600, 2) != 1) {
				speak("指纹模块打开失败");
				AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("指纹模块打开失败").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.this.finish();
						System.exit(0);
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
					System.exit(0);
				}
			}).create();
			alertDialog.show();
			e.printStackTrace();
			return;
		}
		// 初始化摄像头
		File file = new File(PHOTO_PATH);
		if (!file.exists()) {
			file.mkdirs();
		}
		file = new File(PHOTO_PATH + "_finger");
		if (!file.exists()) {
			file.mkdirs();
		}
		previewSurfaceHolder = previewSurface.getHolder();
		previewSurfaceHolder.addCallback(this);
		previewSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	/**
	 * 初始化定时器
	 */
	private void initTimer() {
		// 定时30秒上传
		_timerUpload = new Timer();
		_timerUpload.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_UPLOAD);
			}
		}, 10000, 30000);
		// 每秒执行
		_secondTimerFlag = true;
		_timerSecond = new Timer();
		_timerSecond.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_SECOND);
			}
		}, 4000, 1000);
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
		}, 30000, settings.getInt("photo_interval", PHOTO_DEF_INTERVAL) * 1000);
		_timerTakePhoto2 = new Timer();
		// 上传照片
		_timerUploadPhoto = new Timer();
		_timerUploadPhoto.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_UPLOAD_PHOTO);
			}
		}, 25000, 30000);
		// 计算传感器速度和里程
		_sensorTimerFlag = true;
		_timerSensor = new Timer();
		_timerSensor.schedule(new TimerTask() {
			@Override
			public void run() {
				if (_sensorTimerFlag) {
					_sensorTimerFlag = false;
					try {
						DeviceInfo.SenSpeed = Math.round((float) NativeGPIO.getRotateSpeed(1, 1) / _sts);
						DeviceInfo.SenMileage += DeviceInfo.SenSpeed * 1000 / 3600;
						handler.sendEmptyMessage(H_W_SHOW_SENSOR);
					} catch (Exception e) {
						e.printStackTrace();
					}
					_sensorTimerFlag = true;
				}
			}
		}, 6000, 1000);
	}

	/**
	 * 初始化科目三模拟考试按钮
	 */
	private void ttsButtonInit() {
		showDialog(DL_LOAD_TTSBUTTON);
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
				dismissDialog(DL_LOAD_TTSBUTTON);
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
						textView.setGravity(Gravity.CENTER);
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
					_ttsButtonLoaded = true;
				}
			}
		}.execute();
	}

	/**
	 * 拍照
	 */
	private void takephoto() {
		if (_camera != null && !_takephotoing && Train.IsTraining) {
			_takephotoing = true;
			streamIt.Screenshot = true;
			_photoUUID = Train.TrainID;
			_phototime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			_photoSpeed = DeviceInfo.Speed;
			_photoSenSpeed = DeviceInfo.SenSpeed;
			_timerTakePhoto2.schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(H_W_TAKE_PHOTO1);
				}
			}, 1000);
		}
	}

	private void takephoto1() {
		if (null != streamIt.yuv420sp) {
			lastBitmapJoin(streamIt.yuv420sp);
			savePhoto(_curCamera);
			if (cameracount > 1) {
				changeCamera();
				_timerTakePhoto2.schedule(new TimerTask() {
					@Override
					public void run() {
						handler.sendEmptyMessage(H_W_TAKE_PHOTO2);
					}
				}, 1000);
			} else {
				_takephotoing = false;
				streamIt.Screenshot = false;
			}
		}
	}

	private void takephoto2() {
		lastBitmapJoin(streamIt.yuv420sp);
		savePhoto(_curCamera);
		changeCamera();
		_takephotoing = false;
		streamIt.Screenshot = false;
	}

	/**
	 * 采集指纹时拍照上传
	 */
	private void takeFingerPhoto() {
		if (_camera != null && !_takephotoing) {
			_takephotoing = true;
			streamIt.Screenshot = true;
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(H_W_TAKE_FINGER_PHOTO);
				}
			}, 1000);
		}
	}

	private void takeFingerPhotoDelay() {
		Bitmap bmp = BitmapFactory.decodeByteArray(streamIt.yuv420sp, 0, streamIt.yuv420sp.length);
		File file = new File(PHOTO_PATH + "_finger/" + Student.CardNo + ".jpg");
		try {
			file.createNewFile();
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			bmp.compress(Bitmap.CompressFormat.JPEG, 80, os);
			os.flush();
			os.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		_takephotoing = false;
		streamIt.Screenshot = false;
		// 上传照片
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Map<String, String> params = new HashMap<String, String>();
				params.put("deviceid", deviceID);// 设备号码
				params.put("session", session);// 当前会话
				params.put("f", "t");
				params.put("card", Student.CardNo);
				params.put("t", Student.IsCoach ? "1" : "2");
				File file = new File(PHOTO_PATH + "_finger/" + Student.CardNo + ".jpg");
				params.put("filename", Student.CardNo + ".jpg");
				FormFile formfile = new FormFile(file.getName(), file, "image", "application/octet-stream");
				try {
					SocketHttpRequester.post(server + "/photoupload.ashx", params, formfile);
					file.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
			}
		}.execute();
	}

	/**
	 * 切换摄像头
	 * 
	 * @param 摄像头路数
	 */
	private void changeCamera() {
		if (_camera != null && cameracount > 1) {
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
	}

	private void lastBitmapJoin(byte[] yuv420sp) {
		if (_lastBmps[0] == null) {
			_lastBmps[0] = BitmapFactory.decodeByteArray(yuv420sp, 0, yuv420sp.length);
			imgLast01.setImageBitmap(_lastBmps[0]);
			return;
		}
		_lastBmps[3] = _lastBmps[2];
		imgLast04.setImageBitmap(_lastBmps[3]);
		_lastBmps[2] = _lastBmps[1];
		imgLast03.setImageBitmap(_lastBmps[2]);
		_lastBmps[1] = _lastBmps[0];
		imgLast02.setImageBitmap(_lastBmps[1]);
		_lastBmps[0] = BitmapFactory.decodeByteArray(yuv420sp, 0, yuv420sp.length);
		imgLast01.setImageBitmap(_lastBmps[0]);
	}

	private void savePhoto(int i) {
		Bitmap newPicture = Bitmap.createBitmap(_lastBmps[0].getWidth(), _lastBmps[0].getHeight(), _lastBmps[0].getConfig());
		Canvas canvas = new Canvas(newPicture);
		canvas.drawBitmap(_lastBmps[0], 0, 0, null);
		canvas.drawText(_phototime, _lastBmps[0].getWidth() - 140, _lastBmps[0].getHeight() - 6, _markPaint);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		File file = new File(PHOTO_PATH + "/" + _phototime + "_" + i + "_" + _photoSpeed + "_" + _photoSenSpeed + "_" + _photoUUID + ".jpg");
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
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (_camera != null) {
			if (_previewing) {
				_camera.stopPreview();
			}
			Camera.Parameters p = _camera.getParameters();
			p.setPreviewSize(720, 576);
			p.setPreviewFormat(PixelFormat.YCbCr_420_SP);
			_camera.setPreviewCallback(streamIt);
			_camera.setParameters(p);
			try {
				_camera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			_camera.startPreview();
			_previewing = true;
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			_camera = Camera.open();
			if (_camera == null) {
				Toast.makeText(MainActivity.this, "摄像头启动失败", Toast.LENGTH_SHORT);
			}
		} catch (Exception e) {
			Toast.makeText(MainActivity.this, "摄像头启动失败", Toast.LENGTH_SHORT);
			e.printStackTrace();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.i("whzxt", "surfaceDestroyed");
		if (_camera != null) {
			_camera.setPreviewCallback(null);
			_camera.stopPreview();
			_previewing = false;
			_camera.release();
			_camera = null;
			Log.i("whzxt", "camera release");
		}
		previewSurfaceHolder = null;
		Log.i("whzxt", "previewSurfaceHolder release");
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
					SystemClock.sleep(5000);
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
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
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
							NativeGPIO.setRelay(_jdq_ck);
						} else if (cmd.equals("close_oil")) {
							// 切断油电
							toastHandleShow("1分钟后将断油电");
							_relayoff = 60;
						} else if (cmd.startsWith("call:")) {
							// 拨号
							cmd = cmd.replace("call:", "");
							if (cmd.length() == 11) {
								Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + cmd.replace("call:", "")));
								intent.putExtra("lytmode", true);
								MainActivity.this.startActivity(intent);
							}
						} else if (cmd.equals("subject_2")) {
							// 切换到科目二
							if (DeviceInfo.Subject == 3) {
								DeviceInfo.Subject = 2;
								txtSubject.setText("科目二");
								txtSubjectInfo.setText("点击切换为科目三");
								SharedPreferences.Editor editor = settings.edit();
								editor.putInt("subject", DeviceInfo.Subject);
								editor.commit();
							}
						} else if (cmd.equals("subject_3")) {
							// 切换到科目三
							if (DeviceInfo.Subject == 2) {
								DeviceInfo.Subject = 3;
								txtSubject.setText("科目三");
								txtSubjectInfo.setText("点击切换为科目二");
								SharedPreferences.Editor editor = settings.edit();
								editor.putInt("subject", DeviceInfo.Subject);
								editor.commit();
							}
						} else if (cmd.equals("mode_train")) {
							changeMode(MODE_TRAIN);// 切换训练状态
						} else if (cmd.equals("mode_free")) {
							changeMode(MODE_FREE);// 切换自由状态
						} else if (cmd.equals("change_camera")) {
							changeCamera();// 切换摄像头
						} else if (cmd.equals("take_photo")) {
							takephoto();// 拍照
						} else if (cmd.startsWith("set_photo_size:")) {
							try {
								// 设置照片压缩比
								cmd = cmd.replace("set_photo_size:", "");
								streamIt.options.inSampleSize = Integer.valueOf(cmd);
								SharedPreferences.Editor editor = settings.edit();
								editor.putInt("inSampleSize", streamIt.options.inSampleSize);
								editor.commit();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (cmd.startsWith("set_photo_interval:")) {
							try { // 设置拍照间隔
								cmd = cmd.replace("set_photo_interval:", "");
								int pinterval = Integer.valueOf(cmd);
								_timerCamera.cancel();
								_timerCamera.purge();
								_timerCamera = new Timer();
								_timerCamera.schedule(new TimerTask() {
									@Override
									public void run() {
										handler.sendEmptyMessage(H_W_TAKE_PHOTO);
									}
								}, 5000, pinterval * 1000);
								SharedPreferences.Editor editor = settings.edit();
								editor.putInt("photo_interval", pinterval);
								editor.commit();
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else if (cmd.startsWith("remove_finger:")) {
							// 删除某个卡号的指纹
							cmd = cmd.replace("remove_finger:", "");
							db.delete(MySQLHelper.T_ZXT_FINGER, "cardno=?", new String[] { cmd });
						} else if (cmd.startsWith("clear_finger")) {
							// 删除所有指纹
							db.delete(MySQLHelper.T_ZXT_FINGER, null, null);
						} else if (cmd.startsWith("card_init:")) {
							// 初始化卡
							_fingerFinish = true;
							cmd = cmd.replace("card_init:", "");
							toastShow(cardOper.CardInit(cmd.split(",")[0], cmd.split(",")[1], cmd.split(",")[2]));
						} else if (cmd.startsWith("clear_cache")) {
							// 清除缓存
							File file = new File("/sdcard/ZxtCache");
							if (file.exists() && file.isDirectory()) {
								String[] tempList = file.list();
								File tmpFile = null;
								for (int i = 0; i < tempList.length; i++) {
									tmpFile = new File("/sdcard/ZxtCache/" + tempList[i]);
									if (tmpFile.isFile()) {
										tmpFile.delete();
									}
								}
							}
						} else if (cmd.startsWith("remove_cache:")) {
							// 清除单个缓存文件
							cmd = cmd.replace("remove_cache:", "");
							File file = new File("/sdcard/ZxtCache/" + server.replace("://", "__") + "_" + cmd + ".bmp");
							if (file.exists()) {
								file.delete();
							}
						} else if (cmd.startsWith("upload_log")) {
							// 上传日志
							logupload();
						}
					} else if (_tcpMsg.startsWith("sms:")) {
						txtStatus.setText(_tcpMsg.substring(4));
						toastShow("收到新短信:" + _tcpMsg.substring(4));
						Logger.Write("收到新短信:" + _tcpMsg.substring(4));
					}
				}
				if (!_isclosed) {
					socketListen();// 继续监听
				}
			}
		}.execute();
	}

	CardOper.OnChange cardOnChange = new CardOper.OnChange() {
		public void onLose() {
			Log.i("gc", "onlose");
			handler.sendEmptyMessage(H_W_TRAIN_END);
		}

		public void onFind(String msg) {
			Log.i("gc", "onfind");
			if (msg != null) {
				toastHandleShow(msg);
				handler.post(new Runnable() {
					public void run() {
						dismissDialog(DL_CARD_READING);
					}
				});
			} else {
				// 判断是否需要验证指纹
				int ncf = checkFinger(Student.IsCoach, Student.CardNo);
				if (ncf == 0) {// 不需要验证指纹
					if (!Student.NotNeedFinger) {
						Student.NotNeedFinger = !Student.NotNeedFinger;
						cardOper.WriteNotNeedFingerFlag(1);
					}
				} else if (ncf == 1) {// 需要验证指纹
					if (Student.NotNeedFinger) {
						Student.NotNeedFinger = !Student.NotNeedFinger;
						cardOper.WriteNotNeedFingerFlag(0);
					}
					if (!Student.HasFinger) {
						Student.HasFinger = true;
						cardOper.WriteFingerFlag(1);
					}
				} else if (ncf == 2) {// 需要验证并采集指纹
					if (Student.NotNeedFinger) {
						Student.NotNeedFinger = !Student.NotNeedFinger;
						cardOper.WriteNotNeedFingerFlag(0);
					}
					Student.HasFinger = false;
				}
				handler.post(new Runnable() {
					public void run() {
						dismissDialog(DL_CARD_READING);
					}
				});
				if (Student.NotNeedFinger) {
					if (Student.IsCoach) {
						if (retry < 3) {
							handler.sendEmptyMessage(H_W_GET_COACHINFO);
						} else {
							handler.sendEmptyMessage(H_W_SHOW_COACH);
							handler.sendEmptyMessage(H_W_TRAIN_START);
						}
					} else {
						if (retry < 3) {
							handler.sendEmptyMessage(H_W_GET_STUDENTINFO);
						} else {
							handler.sendEmptyMessage(H_W_SHOW_STUDENT);
							handler.sendEmptyMessage(H_W_TRAIN_START);
						}
					}
				} else {
					if (Student.HasFinger) {
						handler.sendEmptyMessage(H_W_DOWN_FINGER);
					} else {
						handler.sendEmptyMessage(H_W_WRITE_FINGER);
					}
				}
			}
		}

		public void onReadStart() {
			handler.post(new Runnable() {
				public void run() {
					showDialog(DL_CARD_READING);
				}
			});
		}
	};

	/**
	 * 获取时间差
	 * 
	 * @param 开始时间
	 * @param 结束时间
	 * @return 时间差字符串
	 */
	private String getTimeDiff(Date start, Date end) {
		long between = (end.getTime() - start.getTime()) / 1000;
		long hour = between / 3600;
		long minute = between % 3600 / 60;
		long second = between % 60;
		return hour + "小时" + minute + "分" + second + "秒";
	}

	private String getTimeDiff2(Date start, Date end) {
		int between = (int) (end.getTime() - start.getTime()) / 1000;
		int hour = between / 3600;
		int minute = between % 3600 / 60;
		int second = between % 60;
		return String.format("%02d:%02d:%02d", hour, minute, second);
	}

	/**
	 * 下载指纹
	 */
	private void downFinger() {
		showDialog(DL_DOWN_FINGER);
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				Cursor cursor = db.query(MySQLHelper.T_ZXT_FINGER, new String[] { "id" }, "cardno=?", new String[] { (Student.IsCoach ? "01" : "02") + Student.CardNo }, null, null, null);
				if (cursor.moveToFirst()) {
					_fingerCurId = cursor.getInt(0);
					ContentValues tcv = new ContentValues();
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.update(MySQLHelper.T_ZXT_FINGER, tcv, "id=?", new String[] { String.valueOf(_fingerCurId) });
					return 1;
				}
				if (retry > 3) {
					return 0;// 网络异常
				}
				Boolean isinsert = true;
				cursor = db.rawQuery("SELECT MAX(id) AS max_id FROM " + MySQLHelper.T_ZXT_FINGER, null);
				_fingerCurId = 1;
				if (cursor.moveToFirst()) {
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
				try {
					URL url = new URL(server + "/getfinger.ashx?cardno=" + Student.CardNo + "&n=1&t=" + (Student.IsCoach ? "1" : "2"));
					URLConnection connection = url.openConnection();
					connection.setConnectTimeout(10000);
					connection.setReadTimeout(10000);
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					File fingerfile = new File(_fingertmppath + "1");
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
					} else {
						return 0;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					Logger.Write("将第一个指纹写入模块");
					_fingerprint.PSDownChar(_fingerAddress, CHAR_BUFFER_A, 0, 512, _fingertmppath + "1");
					SystemClock.sleep(200);
					_fingerprint.PSStoreChar(_fingerAddress, CHAR_BUFFER_A, _fingerCurId);
					SystemClock.sleep(200);
					Logger.Write("第一个指纹写入模块完成");
				} catch (Exception e) {
					e.printStackTrace();
					return 2;
				}

				try {
					URL url = new URL(server + "/getfinger.ashx?cardno=" + Student.CardNo + "&n=2&t=" + (Student.IsCoach ? "1" : "2"));
					URLConnection connection = url.openConnection();
					connection.setConnectTimeout(10000);
					connection.setReadTimeout(10000);
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					File fingerfile = new File(_fingertmppath + "2");
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
					} else {
						return 0;
					}
				} catch (MalformedURLException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					Logger.Write("将第二个指纹写入模块");
					_fingerprint.PSDownChar(_fingerAddress, CHAR_BUFFER_B, 0, 512, _fingertmppath + "2");
					SystemClock.sleep(200);
					_fingerprint.PSStoreChar(_fingerAddress, CHAR_BUFFER_B, _fingerCurId + 1);
					SystemClock.sleep(200);
					Logger.Write("第二个指纹写入模块完成");
				} catch (Exception e) {
					e.printStackTrace();
					return 2;
				}
				if (isinsert) {
					Logger.Write("记录指纹编号:" + _fingerCurId + ",卡号:" + Student.CardNo);
					ContentValues tcv = new ContentValues();
					tcv.put("id", _fingerCurId);
					tcv.put("cardno", (Student.IsCoach ? "01" : "02") + Student.CardNo);
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.insert(MySQLHelper.T_ZXT_FINGER, null, tcv);
				} else {
					Logger.Write("更新指纹LastTime,编号:" + _fingerCurId + ",卡号:" + Student.CardNo);
					ContentValues tcv = new ContentValues();
					tcv.put("cardno", (Student.IsCoach ? "01" : "02") + Student.CardNo);
					tcv.put("lasttime", dateFormat.format(nowTime));
					db.update(MySQLHelper.T_ZXT_FINGER, tcv, "id=?", new String[] { String.valueOf(_fingerCurId) });
				}
				return 1;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_DOWN_FINGER);
				if (result == 0) {
					toastShow("网络异常,无法下载指纹,请检查网络,然后尝试重新插卡");
				} else if (result == 1) {
					showStudent(true);
					speak("需要验证指纹,请按手指");
					_fingerFinish = false;
					valiFinger();
				} else if (result == 2) {
					Toast.makeText(MainActivity.this, "写指纹文件时出现异常", Toast.LENGTH_SHORT).show();
				} else {
					toastShow("获取指纹信息失败,请尝试重新插卡");
				}
			}
		}.execute();
	}

	/**
	 * 验证指纹
	 */
	private void valiFinger() {
		showDialog(DL_VALI_FINGER);
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				// 验证指纹
				while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
					if (_fingerFinish) {
						return 0;
					}
					SystemClock.sleep(100);
				}
				SystemClock.sleep(100);
				while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
					if (_fingerFinish) {
						return 0;
					}
					SystemClock.sleep(100);
				}
				SystemClock.sleep(10);
				_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A);
				SystemClock.sleep(200);
				if (_fingerprint.PSSearch(_fingerAddress, CHAR_BUFFER_A, _fingerCurId, 2, 0) == PS_OK) {
					return 1;
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_VALI_FINGER);
				if (result == 1) {
					toastShow("指纹验证成功");
					if (Student.IsCoach) {
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
						_fingerRetry++;
						if (_fingerRetry != 3 && _fingerRetry != 6) {
							toastShow("指纹验证失败,请重新按手指");
							valiFinger();
						} else {
							if (_fingerRetry == 3) {
								speak("指纹验证失败,请稍候,正在重新获取指纹");
								db.delete(MySQLHelper.T_ZXT_FINGER, "cardno=?", new String[] { (Student.IsCoach ? "01" : "02") + Student.CardNo });
								downFinger();
							} else {
								speak("指纹验证失败,请稍候,正在检查指纹模块");
								try {
									Logger.Write("关闭读卡器");
									cardOper.stop();
									Logger.Write("关闭指纹串口");
									_fingerprint.PSCloseDevice();
									Logger.Write("关闭指纹电源");
									lytfingerprint.Close();
									SystemClock.sleep(2000);
									Logger.Write("打开读卡器");
									cardOper.openICCard();
									cardOper.openRfidCard();
									Logger.Write("打开指纹电源");
									lytfingerprint.Open();
									Logger.Write("打开指纹串口");
									_fingerprint.PSOpenDevice(1, 0, 57600 / 9600, 2);
									cardOper.selectRfidType(CardOper.RFID_TYPE_A);
									cardOper.start();
									SystemClock.sleep(3000);
									speak("需要验证指纹,请按手指");
									valiFinger();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					} else {
						toastShow("指纹验证失败,请尝试重新插卡");
						showStudent(false);
					}
				}
			}
		}.execute();
	}

	/**
	 * 每秒执行
	 */
	private void second() {
		// 喂狗
		// com.lyt.watchdog.Native.keepalive();
		if (_secondTimerFlag) {
			_secondTimerFlag = false;
			// 更新时间
			nowTime = new Date();
			txtSystemTime.setText(dateFormat.format(nowTime));
			if (Train.IsTraining) {
				// 计算余额
				Student.RealBalance = Student.Balance - (int) ((nowTime.getTime() - Train.StartTime.getTime()) / 60000);
				if (((nowTime.getTime() - Train.StartTime.getTime()) / 1000) % 60 > 0) {
					Student.RealBalance--;
				}
				// Student.RealTotalTime = Student.TotalTime + (Student.Balance
				// - Student.RealBalance);
				// 显示余额
				// if (Student.IsCoach) {
				txtTrainTime.setText(getTimeDiff2(Train.StartTime, nowTime));
				if (Student.IsCharging) {
					txtBalance.setText("剩余:" + Student.RealBalance + "分钟");
				} else {
					txtBalance.setText("");
				}
				// } else {
				// txtTrainTime.setText(getTimeDiff2(Train.StartTime, nowTime));
				// txtBalance.setText("余额:" + Student.RealBalance * PRICE +
				// "元");
				// }
				if (Student.IsCharging) {
					if (Student.RealBalance <= 0) {
						// 结束训练
						trainFinish();
					}
				}
			}

			if (_relayoff > 0) {
				_relayoff--;
				if (_relayoff == 0) {
					NativeGPIO.setRelay(!_jdq_ck);
					toastShow("油电已断");
				}
			}

			if (photoServer.ConCount == photoServer.PreConCount) {
				if (!_takephotoing) {
					streamIt.Screenshot = false;
				}
			} else {
				photoServer.PreConCount = photoServer.ConCount;
			}

			_secondTimerFlag = true;
		}
	}

	/**
	 * 显示传感器速度
	 */
	private void showSensor() {
		txtSensor.setText(String.format("%3d", DeviceInfo.SenSpeed));
	}

	/**
	 * 开始训练
	 */
	private void trainBegin() {
		if (DeviceInfo.Mode == MODE_TRAIN && Student.Balance <= 0 && Student.IsCharging) {
			toastShow("卡内时长不足");
			return;
		}
		if (Student.Subject == -1) {
			toastShow("当前学员" + Student.Name + "已经毕业,不能再训练");
			return;
		}
		if (Student.Subject == 1) {
			toastShow("当前学员还不能训练" + txtSubject.getText());
			return;
		}
		if (Student.Subject > 1) {
			// 切换科目
			if (DeviceInfo.Subject != Student.Subject) {
				DeviceInfo.Subject = Student.Subject;
				if (Student.Subject == 2) {
					txtSubject.setText("科目二");
					txtSubjectInfo.setText("点击切换为科目三");
					SharedPreferences.Editor editor = settings.edit();
					editor.putInt("subject", DeviceInfo.Subject);
					editor.commit();
				} else {
					txtSubject.setText("科目三");
					txtSubjectInfo.setText("点击切换为科目二");
					SharedPreferences.Editor editor = settings.edit();
					editor.putInt("subject", DeviceInfo.Subject);
					editor.commit();
				}
			}
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
		if (DeviceInfo.Mode == MODE_TRAIN) {
			Train.Start(db);
			if (Student.IsCharging) {
				speak(_tmp + "," + Student.Name + ",卡内剩余" + (Student.Balance - 1) + "分钟,请谨慎驾驶");
				// 重写卡内余额
				new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... args) {
						cardOper.WriteBalance(Student.Balance - 1);
						return 0;
					}
				}.execute();
			} else {
				speak(_tmp + "," + Student.Name);
			}
			takephoto();// 拍照
		} else {
			speak(_tmp + "," + Student.Name);
			txtTrainTime.setText("自由状态");
			// if (Student.IsCoach) {
			if (Student.IsCharging) {
				txtBalance.setText("剩余:" + Student.RealBalance + "分钟");
			} else {
				txtBalance.setText("");
			}
			// } else {
			// txtBalance.setText("余额:" + Student.RealBalance * PRICE + "元");
			// }
		}
		// 继电器
		NativeGPIO.setRelay(_jdq_ck);
		_relayoff = 0;
	}

	/**
	 * 结束训练
	 */
	private void trainFinish() {
		Log.i("train", "train finish");
		_fingerFinish = true;
		if (Train.IsTraining) {
			if (Student.IsCoach) {
				if (Student.IsCharging) {
					speak("本次用车已结束,共使用" + getTimeDiff(Train.StartTime, nowTime) + "," + txtBalance.getText().toString() + ",1分钟后将断油电");
				} else {
					speak("本次用车已结束,共使用" + getTimeDiff(Train.StartTime, nowTime) + ",1分钟后将断油电");
				}
				txtTrainTime.setText(getTimeDiff2(Train.StartTime, nowTime));
			} else {
				if (Student.IsCharging) {
					speak("本次训练已结束,共训练" + getTimeDiff(Train.StartTime, nowTime) + "," + txtBalance.getText().toString() + ",1分钟后将断油电");
				} else {
					speak("本次训练已结束,共训练" + getTimeDiff(Train.StartTime, nowTime) + ",1分钟后将断油电");
				}
				txtTrainTime.setText(getTimeDiff2(Train.StartTime, nowTime));
			}
			takephoto();// 拍照
			Train.End(db);
			// 20秒之后隐藏训练信息
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					handler.sendEmptyMessage(H_W_HIDE_TRAININFO);
				}
			}, 20 * 1000);
			// 上传训练数据
			if (retry < 3 && !_usedataUploading) {
				uploadUseData();
				upload();
			}
			// 1分钟后断电熄火
			_relayoff = 60;
		}
		Student.Init();
		showStudent(false);
	}

	/**
	 * 每分钟执行
	 */
	private void minute() {
		// 记录/更新当前训练结束时间、余额
		if (Train.IsTraining) {
			Train.Update(db);

			if (Student.IsCharging) {
				// 重写卡内余额
				new AsyncTask<Void, Void, Integer>() {
					@Override
					protected Integer doInBackground(Void... args) {
						cardOper.WriteBalance(Student.RealBalance);
						return 0;
					}
				}.execute();

				// 余额不足提醒
				if (Student.RealBalance <= 3) {
					toastShow("卡内剩余时长不足" + Student.RealBalance + "分钟,请注意");
				}
			}

			// 超时提醒
			if (!Student.IsCoach) {
				if (DeviceInfo.Subject == 2) {
					if (Student.TodayTrainTime + ((nowTime.getTime() - Train.StartTime.getTime()) / 60000) > s2_day_time) {
						if (Train.Totalert < Train.TIMEOUTALERT) {
							toastShow("今天训练已超过" + s2_day_time + "分钟,请注意");
							Train.Totalert++;
						}
					} else {
						if ((nowTime.getTime() - Train.StartTime.getTime()) / 1000 > s2_max_time * 60) {
							if (Train.Tocalert < Train.TIMEOUTALERT) {
								toastShow("本次训练已超过" + s2_max_time + "分钟,请注意");
								Train.Tocalert++;
							}
						}
					}
				} else {
					if (Student.TodayTrainTime + ((nowTime.getTime() - Train.StartTime.getTime()) / 60000) > s3_day_time) {
						if (Train.Totalert < Train.TIMEOUTALERT) {
							toastShow("今天训练已超过" + s3_day_time + "分钟,请注意");
							Train.Totalert++;
						}
					} else {
						if ((nowTime.getTime() - Train.StartTime.getTime()) / 1000 > s3_max_time * 60) {
							if (Train.Tocalert < Train.TIMEOUTALERT) {
								toastShow("本次训练已超过" + s3_max_time + "分钟,请注意");
								Train.Tocalert++;
							}
						}
					}
				}
			}

			/*
			 * // 检查卡号 new AsyncTask<Void, Void, Integer>() {
			 * 
			 * @Override protected Integer doInBackground(Void... args) { return
			 * cardOper.checkCardNo() ? 1 : 0; }
			 * 
			 * @Override protected void onPostExecute(Integer result) { if
			 * (result == 0) { trainFinish(); } } }.execute();
			 */
		}
		// 上传训练数据
		if (retry < 3 && !_usedataUploading) {
			uploadUseData();
		}
		// 上传GPS盲点
		if (retry < 3 && !_blindspotUploading) {
			uploadBlindSpot();
		}
		if (_gpstime_now != _gpstime_pre) {
			_gpstime_pre = _gpstime_now;
			txtGPSStatus.setText("GPS正常");
			txtGPSStatus.setTextColor(Color.WHITE);
		} else {
			txtGPSStatus.setText("GPS异常");
			txtGPSStatus.setTextColor(Color.RED);
		}
	}

	/**
	 * 写指纹
	 */
	private void writeFinger() {
		showStudent(true);
		speak("需要采集指纹,请按右手拇指");
		showDialog(DL_FINGER1);
		_fingerFinish = false;
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
					takeFingerPhoto(); // 拍照上传
					recordfinger2();
				} else {
					toastShow("采集指纹失败,请尝试重新插卡");
					showStudent(false);
				}
			}
		}.execute();
	}

	private void recordfinger2() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				if (retry < 3) {
					if (_fingerprint.PSUpChar(_fingerAddress, CHAR_BUFFER_A, null, 0, _fingertmppath) != PS_OK) {
						SystemClock.sleep(200);
						if (_fingerprint.PSUpChar(_fingerAddress, CHAR_BUFFER_A, null, 0, _fingertmppath) != PS_OK) {
							return -1;
						}
					}
					// 上传指纹特征码
					Map<String, String> params = new HashMap<String, String>();
					params.put("deviceid", deviceID);// 设备号码
					params.put("session", session);// 当前会话
					params.put("cardno", Student.CardNo);
					params.put("n", "1");
					params.put("t", Student.IsCoach ? "1" : "2");// card_type,1:coach,2:student
					File file = new File(_fingertmppath);
					FormFile formfile = new FormFile(file.getName(), file, "finger", "application/octet-stream");
					try {
						SocketHttpRequester.post(server + "/fingerupload.ashx", params, formfile);
					} catch (Exception e) {
						e.printStackTrace();
						return 0;
					}
					// 上传指纹图片
					if (_needFingerImage) {
						file = new File("/data/local/tmp/finger.bmp");
						formfile = new FormFile(file.getName(), file, "finger", "application/octet-stream");
						try {
							SocketHttpRequester.post(server + "/fingerimageupload.ashx", params, formfile);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					return 1;
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				Log.i("zhiwen", new Date().toString());
				dismissDialog(DL_FINGER3);
				if (result == 1) {
					speak("请按右手食指..");
					showDialog(DL_FINGER2);
					recordfinger3();
				} else if (result == 0) {
					toastShow("记录指纹失败,请检查网络");
				} else {
					toastShow("记录指纹失败,请尝试重新插卡");
					showStudent(false);
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
					toastShow("采集指纹失败,请尝试重新插卡");
					showStudent(false);
				}
			}
		}.execute();
	}

	private void recordfinger4() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				if (retry < 3) {
					if (_fingerprint.PSUpChar(_fingerAddress, CHAR_BUFFER_A, null, 0, _fingertmppath) != PS_OK) {
						SystemClock.sleep(200);
						if (_fingerprint.PSUpChar(_fingerAddress, CHAR_BUFFER_A, null, 0, _fingertmppath) != PS_OK) {
							return -1;
						}
					}
					SystemClock.sleep(200);
					Map<String, String> params = new HashMap<String, String>();
					params.put("deviceid", deviceID);// 设备号码
					params.put("session", session);// 当前会话
					params.put("cardno", Student.CardNo);
					params.put("n", "2");
					params.put("t", Student.IsCoach ? "1" : "2");// card_type,1:coach,2:student
					File file = new File(_fingertmppath);
					FormFile formfile = new FormFile(file.getName(), file, "finger", "application/octet-stream");
					try {
						SocketHttpRequester.post(server + "/fingerupload.ashx", params, formfile);
					} catch (Exception e) {
						e.printStackTrace();
						return 0;
					}
					// 上传指纹图片
					if (_needFingerImage) {
						file = new File("/data/local/tmp/finger.bmp");
						formfile = new FormFile(file.getName(), file, "finger", "application/octet-stream");
						try {
							SocketHttpRequester.post(server + "/fingerimageupload.ashx", params, formfile);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					cardOper.WriteFingerFlag(1);
					return 1;
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				dismissDialog(DL_FINGER3);
				if (result == 1) {
					toastShow("记录指纹成功,请重新插卡");
				} else if (result == 0) {
					toastShow("记录指纹失败,请检查网络");
					showStudent(false);
				} else {
					toastShow("记录指纹失败,请尝试重新插卡");
					showStudent(false);
				}
			}
		}.execute();
	}

	private int fingertocontext() {
		// 1，检测手指并录取图像
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			if (_fingerFinish) {
				return -1;
			}
			SystemClock.sleep(100);
		}
		SystemClock.sleep(50);
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			if (_fingerFinish) {
				return -1;
			}
			SystemClock.sleep(100);
		}
		SystemClock.sleep(50);
		// 2，根据原始图像生成指纹特征
		if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A) != PS_OK) {
			SystemClock.sleep(50);
			while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
				if (_fingerFinish) {
					return -1;
				}
				SystemClock.sleep(100);
			}
			if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_A) != PS_OK) {
				return -1;
			}
		}
		SystemClock.sleep(100);
		// 3，再一次检测手指并录取图像
		// 4，根据原始图像生成指纹特征
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			if (_fingerFinish) {
				return -1;
			}
			SystemClock.sleep(100);
		}
		SystemClock.sleep(50);
		while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
			if (_fingerFinish) {
				return -1;
			}
			SystemClock.sleep(100);
		}
		SystemClock.sleep(100);
		if (_fingerprint.PSGenChar(_fingerAddress, CHAR_BUFFER_B) != PS_OK) {
			SystemClock.sleep(100);
			while (_fingerprint.PSGetImage(_fingerAddress) == PS_NO_FINGER) {
				if (_fingerFinish) {
					return -1;
				}
				SystemClock.sleep(100);
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
		if (_needFingerImage) {
			// 获取图像
			for (int i = 0; i < 256 * 288; i++) {
				_pImageData[i] = 5;
			}
			_fingerprint.PSUpImage(_fingerAddress, _pImageData, _iImageLength);
		}
		return PS_OK;
	}

	private int checkFinger(Boolean isCoach, String card) {
		if (retry >= 3) {
			return -1;
		}
		HttpPost httpRequest = new HttpPost(server + "/getcoachinfo.ashx");
		if (!isCoach) {
			httpRequest = new HttpPost(server + "/getstudentinfo.ashx");
		}
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("action", "checkfinger"));
		params.add(new BasicNameValuePair("card", card));
		try {
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			return -1;
		}
		HttpResponse httpResponse;
		try {
			httpResponse = new DefaultHttpClient().execute(httpRequest);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		if (httpResponse.getStatusLine().getStatusCode() == 200) {
			try {
				return Integer.parseInt(EntityUtils.toString(httpResponse.getEntity()));
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		return -1;
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
				Cursor cursor = db.query(MySQLHelper.T_ZXT_USE_DATA, null, "guid!='" + Train.TrainID + "'", null, null, null, null);
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
					txtStatus.setText("网络异常[" + getNowTime() + "]");
					Logger.Write("上传训练数据时网络出现异常");
				} else if (result == 2) {
					txtStatus.setText(usedataresult + "[" + getNowTime() + "]");
					Logger.Write(usedataresult);
				} else {
					txtStatus.setText("训练数据上传成功[" + getNowTime() + "]");
					Logger.Write("训练数据上传成功");
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
				params.add(new BasicNameValuePair("card", Coach.CardNo)); // 教练卡号
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
						if (results[1].equals(Coach.CardNo)) {
							Student.RealBalance = Student.Balance = Integer.parseInt(results[2]);
							Coach.CardNo = Student.CardNo = results[1];
							Coach.ID = Student.ID = results[3];
							Coach.IDCardNo = Student.IDCardNo = results[5];
							if (!Coach.Name.equals(results[4])) {
								Coach.Name = Student.Name = results[4];
								cardOper.WriteName();// 重写姓名
							}
							if (results.length > 6) {
								Coach.Certificate = results[6];// 教练证号
							}
							if (results.length > 7) {
								// 是否计费
								if (results[7].equals("1")) {
									if (!Student.IsCharging) {
										Student.IsCharging = true;
										cardOper.WriteBillingFlag(0);
									}
								} else {
									if (Student.IsCharging) {
										Student.IsCharging = false;
										cardOper.WriteBillingFlag(1);//不计费标记
									}
								}
							}
							if (results.length > 8) {
								try {
									Coach.Level = Integer.parseInt(results[8]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
							if (results.length > 9) {
								try {
									Coach.MonthTotal = Integer.parseInt(results[9].split(",")[0]);
									Coach.DayTotal = Integer.parseInt(results[9].split(",")[1]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
							if (Coach.IDCardNo.equals("无")) {
								Coach.IDCardNo = "";
							}
							if (Coach.Certificate.equals("无")) {
								Coach.Certificate = "";
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
					toastShow(cardresult.split("\\|")[1]);// 提示错误
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
				List<NameValuePair> params = new ArrayList<NameValuePair>(7);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("school", schoolID)); // 驾校ID
				params.add(new BasicNameValuePair("card", Student.CardNo)); // 学员卡号
				params.add(new BasicNameValuePair("balance", String.valueOf(Student.Balance))); // 卡内余额
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				params.add(new BasicNameValuePair("coach", Coach.CardNo)); // 教练卡号
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
						if (results.length > 1 && results[1].equals(Student.CardNo)) {
							Student.RealBalance = Student.Balance = Integer.parseInt(results[2]);
							Student.ID = results[3];
							Student.IDCardNo = results[5];
							if (!Student.Name.equals(results[4])) {
								Student.Name = results[4];
								cardOper.WriteName();// 重写姓名和身份证
							}
							Student.DriverType = results[6];
							if (results.length > 7) {
								try {
									Student.TodayTrainTime = Integer.parseInt(results[7]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
							if (results.length > 8) {
								try {
									Student.Subject = Integer.parseInt(results[8]);
								} catch (NumberFormatException e) {
									Student.Subject = 0;
									e.printStackTrace();
								}
							}
							if (results.length > 9) {
								// 是否计费
								if (results[9].equals("1")) {
									if (!Student.IsCharging) {
										Student.IsCharging = true;
										cardOper.WriteBillingFlag(0);
									}
								} else {
									if (Student.IsCharging) {
										Student.IsCharging = false;
										cardOper.WriteBillingFlag(1);//不计费标记
									}
								}
							}
							if (results.length > 10) {
								try {
									Student.TotalTime = Integer.parseInt(results[10].split(",")[0]);
									Student.TotalMi = Integer.parseInt(results[10].split(",")[1]);
									Student.TodayTrainTime = Integer.parseInt(results[10].split(",")[2]);
									Student.TodayTrainMi = Integer.parseInt(results[10].split(",")[3]);
								} catch (NumberFormatException e) {
									e.printStackTrace();
								}
							}
							if (results.length > 11) {
								Student.Status = results[11];
							}
							if (Student.IDCardNo.equals("无")) {
								Student.IDCardNo = "";
							}
							if (Student.DriverType.equals("无")) {
								Student.DriverType = "";
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
					trainBegin();
				} else if (result == 2) {
					versionUpdate();// 更新程序
				} else if (result == 3) {
					toastShow(cardresult.split("\\|")[1]);// 提示错误
				}
			}
		}.execute();
	}

	/**
	 * 上传GPS数据
	 */
	private void upload() {
		txtStatus.setText("正在上传GPS数据[" + getNowTime() + "]");
		Logger.Write("正在上传GPS数据");
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				HttpPost httpRequest = new HttpPost(server + "/upload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(10);
				if (!Train.IsTraining) {
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("logintime", dateFormat.format(DeviceInfo.GPSTime))); // GPS时间
					params.add(new BasicNameValuePair("lng", String.format("%.6f", DeviceInfo.Longitude))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", DeviceInfo.Latitude))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(DeviceInfo.Speed))); // 速度
					params.add(new BasicNameValuePair("senspeed", String.valueOf(DeviceInfo.SenSpeed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(DeviceInfo.Mode))); // 模式
					params.add(new BasicNameValuePair("coach", Coach.CardNo)); // 教练
					params.add(new BasicNameValuePair("acc", String.valueOf(NativeGPIO.getAccState()))); // ACC
				} else {
					params = new ArrayList<NameValuePair>(15);
					params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
					params.add(new BasicNameValuePair("session", session)); // 当前会话
					params.add(new BasicNameValuePair("logintime", dateFormat.format(DeviceInfo.GPSTime))); // GPS时间
					params.add(new BasicNameValuePair("lng", String.format("%.6f", DeviceInfo.Longitude))); // 经度
					params.add(new BasicNameValuePair("lat", String.format("%.6f", DeviceInfo.Latitude))); // 纬度
					params.add(new BasicNameValuePair("speed", String.valueOf(DeviceInfo.Speed))); // 速度
					params.add(new BasicNameValuePair("senspeed", String.valueOf(DeviceInfo.SenSpeed))); // 速度
					params.add(new BasicNameValuePair("mode", String.valueOf(DeviceInfo.Mode))); // 模式
					params.add(new BasicNameValuePair("coach", Coach.CardNo)); // 教练
					params.add(new BasicNameValuePair("student", Student.CardNo)); // 学员
					params.add(new BasicNameValuePair("starttime", dateFormat.format(Train.StartTime))); // 开始训练时间
					params.add(new BasicNameValuePair("balance", String.valueOf(Student.RealBalance))); // 余额
					params.add(new BasicNameValuePair("subject", String.valueOf(DeviceInfo.Subject))); // 训练科目
					params.add(new BasicNameValuePair("taskid", Train.TrainID)); // 训练ID
					params.add(new BasicNameValuePair("acc", String.valueOf(NativeGPIO.getAccState()))); // ACC
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
					txtNetworkStatus.setTextColor(Color.WHITE);
					txtStatus.setText("GPS数据上传成功[" + getNowTime() + "]");
					Logger.Write("GPS数据上传成功");
				} else {
					// 记录盲点
					ContentValues tcv = new ContentValues();
					tcv.put("gpstime", dateFormat.format(DeviceInfo.GPSTime));
					tcv.put("lng", String.format("%.6f", DeviceInfo.Longitude));
					tcv.put("lat", String.format("%.6f", DeviceInfo.Latitude));
					tcv.put("speed", String.valueOf(DeviceInfo.Speed));
					tcv.put("senspeed", String.valueOf(DeviceInfo.SenSpeed));
					db.insert(MySQLHelper.T_ZXT_GPS_DATA, null, tcv);
					if (result == 2) {
						txtStatus.setText(uploadresult + "[" + getNowTime() + "]");
						Logger.Write(uploadresult);
					} else {
						txtStatus.setText("网络异常(" + retry + ")[" + getNowTime() + "]");
						Logger.Write("网络异常(" + retry + ")");
						if (retry >= 3) {
							txtNetworkStatus.setText("网络异常");
							txtNetworkStatus.setTextColor(Color.RED);
						}
					}
				}
			}
		}.execute();
	}

	private void logupload() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				//String systeminfo = "[系统信息:电量";
				HttpPost httpRequest = new HttpPost(server + "/logupload.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(3);
				params.add(new BasicNameValuePair("deviceid", deviceID)); // 设备号码
				params.add(new BasicNameValuePair("session", session)); // 当前会话
				params.add(new BasicNameValuePair("log", Logger.Read()));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {

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
			txtCoachName.setText(Coach.Name);
			txtCoachCard.setText("卡号:" + Coach.CardNo);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("coachID", Coach.ID);
			editor.putString("coachCard", Coach.CardNo);
			editor.putString("coachName", Coach.Name);
			editor.putString("coachIDCard", Coach.IDCardNo);
			editor.putString("coachCertificate", Coach.Certificate);
			editor.commit();
			imgCoach.setImageResource(R.drawable.photo);
			if (!Coach.IDCardNo.equals("")) {
				imgCoach.setImageUrl(server + "/" + Coach.IDCardNo + ".bmp");
			}
			// 详细信息
			txtDetailCoachCard.setText("卡号:" + Coach.CardNo);
			txtDetailCoachNo.setText("教练证号:" + Coach.Certificate);
			txtDetailCoachLevel.setText("教练等级:" + Coach.Level);
			txtDetailCoachBrpx.setText("本日培训:" + Coach.DayTotal + "分钟");
			txtDetailCoachBypx.setText("本月培训:" + Coach.MonthTotal + "分钟");
		} else {
			layCoachTitle.setBackgroundResource(R.drawable.bg1);
			txtCoachName.setText("");
			txtCoachCard.setText("请插卡");
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
			txtStudentName.setText(Student.Name);
			txtStudentCard.setText("卡号:" + Student.CardNo);
			if (Student.IsCoach) {
				txtStudentTitle.setText("教练:");
			} else {
				txtStudentTitle.setText("学员:");
			}
			if (!Student.IDCardNo.equals("")) {
				imgStudent.setImageUrl(server + "/" + Student.IDCardNo + ".bmp");
			}
			// 详细信息
			txtDetailStudentCard.setText("卡号:" + Student.CardNo);
			txtDetailStudentStatus.setText("状态:" + Student.Status);
			txtDetailStudentLjlc.setText("累计里程:" + Student.TotalMi + "米");
			txtDetailStudentLjxs.setText("累计学时:" + Student.TotalTime + "分钟");
			txtDetailStudentBrxs.setText("本日学时:" + Student.TodayTrainTime + "分钟");
			txtDetailStudentBrlc.setText("本日里程:" + Student.TodayTrainMi + "米");
			if (DeviceInfo.Subject == 2) {
				txtDetailStudentBrsx.setText("每日上限:" + s2_day_time + "分钟");
			} else {
				txtDetailStudentBrsx.setText("每日上限:" + s3_day_time + "分钟");
			}
		} else {
			txtStudentTitle.setText("学员");
			layStudentTitle.setBackgroundResource(R.drawable.bg1);
			imgStudent.setImageResource(R.drawable.photo);
			txtStudentName.setText("");
			txtStudentCard.setText("请插卡");
			txtBalance.setText("");
		}
	}

	/**
	 * 隐藏训练提示信息(训练时长、余额)
	 */
	private void hideTrainInfo() {
		if (!Train.IsTraining) {
			txtTrainTime.setText("训练停止");
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
			if (DeviceInfo.Mode == MODE_FREE) {
				DeviceInfo.Mode = MODE_TRAIN;
				txtTrainTime.setText("训练停止");
				txtMode.setText("训练状态");
				txtModeInfo.setText("点击切换为自由状态");
				if (cardOper.CardType == CardOper.NO_CARD) {
					toastShow("已经切换为训练状态,请插卡,否则一分钟后将断油电");
				} else {
					toastShow("已经切换为训练状态");
				}
				// 继电器
				_relayoff = 60;
				Train.End(db);
				cardOper.CardType = CardOper.NO_CARD;

				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("mode", DeviceInfo.Mode);
				editor.commit();
			}
		} else {
			if (DeviceInfo.Mode == MODE_TRAIN) {
				txtTrainTime.setText("训练停止");
				// if (Student.IsCoach) {
				if (Student.IsCharging) {
					txtBalance.setText("剩余:" + Student.RealBalance + "分钟");
				}
				// } else {
				// txtBalance.setText("余额:" + Student.RealBalance * PRICE +
				// "元");
				// }
				Train.End(db);
				DeviceInfo.Mode = MODE_FREE;
				txtMode.setText("自由状态");
				txtModeInfo.setText("点击切换为训练状态");
				toastShow("已经切换为自由状态");
				NativeGPIO.setRelay(_jdq_ck);
				_relayoff = 0;

				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("mode", DeviceInfo.Mode);
				editor.commit();
			}
		}
	}

	/**
	 * 程序更新
	 */
	private void versionUpdate() {
		speak("发现新版本,需要耕新、程序");
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

	/**
	 * 下载程序
	 */
	private void downloadAPK() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				try {
					URL url = new URL(server + "/zpad.apk");
					URLConnection connection = url.openConnection();
					connection.connect();
					InputStream inputStream = connection.getInputStream();

					String savePath = Environment.getExternalStorageDirectory() + "/download";
					File file = new File(savePath);
					if (!file.exists()) {
						file.mkdir();
					}
					String savePathString = Environment.getExternalStorageDirectory() + "/download/zpad.apk";
					_downLoadFile = new File(savePathString);
					if (_downLoadFile.exists()) {
						_downLoadFile.delete();
					}
					_downLoadFile.createNewFile();

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
		String type = "application/vnd.android.package-archive";
		intent.setDataAndType(Uri.fromFile(f), type);
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

	private void toastShow(String str) {
		Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
		speak(str);
	}

	private void toastHandleShow(final String str) {
		handler.post(new Runnable() {
			public void run() {
				Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
				speak(str);
			}
		});
	}

	private void speak(String str) {
		if (mTts != null) {
			try {
				if (str.indexOf('[') > 0 && str.indexOf(']', str.indexOf('[') + 1) > 0) {
					mTts.speak(str.substring(0, str.indexOf('[')), TextToSpeech.QUEUE_FLUSH, null);
					str = str.substring(str.indexOf('[') + 1);
					try {
						final String nextStr = str.substring(str.indexOf(']') + 1);
						new Timer().schedule(new TimerTask() {
							@Override
							public void run() {
								speak(nextStr);
							}
						}, 1000 * Integer.parseInt(str.substring(0, str.indexOf(']'))));
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (IndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				} else {
					mTts.speak(str, TextToSpeech.QUEUE_FLUSH, null);
				}
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
		final EditText txtpsd = new EditText(MainActivity.this);
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).setTitle("确定要重启程序？").setIcon(android.R.drawable.ic_menu_help).setView(txtpsd).setPositiveButton("确定", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				int psd = 0;
				try {
					psd = Integer.parseInt(txtpsd.getText().toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
				// trainFinish();
				if (psd == nowTime.getDate() * nowTime.getDay()) {
					// com.lyt.watchdog.Native.exit(dogfd);
					try {
						Intent intent = new Intent();
						intent.setComponent(new ComponentName("cn.whzxt.gps", "cn.whzxt.gps.ZxtService"));
						stopService(intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				MainActivity.this.finish();
				System.exit(0);
			}
		}).setNegativeButton("取消", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				return;
			}
		}).create();
		alertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_HOME)
					return true;
				return false;
			}
		});
		alertDialog.show();
		alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
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
		AlertDialog dialog = null;
		switch (id) {
		case DL_CARD_READING:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在读卡,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_DOWN_FINGER:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在获取指纹信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_VALI_FINGER:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("请按手指...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_CARD_INIT:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在初始化卡,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_FINGER1:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("需要采集指纹,请按右手姆指...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_FINGER2:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("请按右手食指...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_FINGER3:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("指纹采集完毕,正在记录指纹,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_GET_COACH:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在获取教练信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_GET_STUDENT:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在获取学员信息,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		case DL_LOAD_TTSBUTTON:
			dialog = new AlertDialog.Builder(MainActivity.this).setCancelable(false).setTitle("警告").setMessage("正在加载,请稍候...").setIcon(android.R.drawable.ic_dialog_info).create();
			break;
		default:
			return null;
		}
		if (dialog == null)
			return null;
		/*
		 * dialog.setOnKeyListener(new DialogInterface.OnKeyListener() { public
		 * boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		 * if (keyCode == KeyEvent.KEYCODE_HOME) return true; return false; }
		 * }); dialog.show();
		 * dialog.getWindow().setType(WindowManager.LayoutParams
		 * .TYPE_KEYGUARD_DIALOG);
		 */
		return dialog;
	}

	private final LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// log it when the location changes
			if (location != null) {
				_gpstime_now = location.getTime();
				DeviceInfo.GPSTime = new Date(location.getTime());
				DeviceInfo.Longitude = location.getLongitude();
				DeviceInfo.Latitude = location.getLatitude();
				DeviceInfo.Speed = Math.round(location.getSpeed() / NMDIVIDED * 60 * 60 / 1000);
				DeviceInfo.Mileage += location.getSpeed() / NMDIVIDED;
				txtGPSSpeed.setText(String.format("%3d", DeviceInfo.Speed));
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

	public boolean onDown(MotionEvent arg0) {
		return false;
	}

	public void onLongPress(MotionEvent e) {
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	public void onShowPress(MotionEvent e) {
	}

	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	public boolean onTouchEvent(MotionEvent event) {
		return this.detector.onTouchEvent(event);
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		if (e1.getX() - e2.getX() > 80) {
			if (flipper.getDisplayedChild() == 0) {
				flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.layout.push_left_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.layout.push_left_out));
				flipper.setDisplayedChild(2);
			}
			return true;
		} else if (e1.getX() - e2.getX() < -80) {
			if (flipper.getDisplayedChild() == 0) {
				flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.layout.push_right_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.layout.push_right_out));
				flipper.setDisplayedChild(1);
			} else if (flipper.getDisplayedChild() == 2) {
				flipper.setInAnimation(AnimationUtils.loadAnimation(this, R.layout.push_right_in));
				flipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.layout.push_right_out));
				flipper.setDisplayedChild(0);
			}
			return true;
		}
		return true;
	}

	@Override
	protected void onDestroy() {
		_isclosed = true;
		// 关闭TCP监听
		try {
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 关闭定时器
		_timerUpload.cancel();
		_timerUpload.purge();
		_timerSecond.cancel();
		_timerSecond.purge();
		_timerMinute.cancel();
		_timerMinute.purge();
		_timerCamera.cancel();
		_timerCamera.purge();
		_timerUploadPhoto.cancel();
		_timerUploadPhoto.purge();
		_timerSensor.cancel();
		_timerSensor.purge();
		Log.i("zxt", "timer closed");
		// 读卡器
		cardOper.stop();
		// TTS语音
		if (mTts != null) {
			try {
				mTts.shutdown();
				Log.i("zxt", "tts closed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 数据库
		if (db != null) {
			try {
				db.close();
				Log.i("zxt", "database closed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 指纹
		if (_fingerprint != null) {
			try {
				_fingerprint.PSCloseDevice();
				Log.i("zxt", "finger closed 1");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			lytfingerprint.Close();
			Log.i("zxt", "finger closed 2");
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 摄像头
		if (wakeLock != null) {
			wakeLock.release();
		}
		super.onDestroy();
	}
}