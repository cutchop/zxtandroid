package cn.whzxt.android;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
	TextView txtStatus;
	HttpPost httpRequest;
	HttpResponse httpResponse;
	SharedPreferences settings;
	private String server;
	private String imei;
	private String strResult;
	private String[] results;
	private static final int H_W_UPDATEDIALOG_MAX = 0x14;
	private static final int H_W_UPDATEDIALOG_NOW = 0x15;
	private static final int H_W_SECOND = 0x01;
	private ProgressDialog _updateDialog;
	private File _downLoadFile;
	private int _fileLength, _downedFileLength = 0;
	private int _retry = 0;
	private int _countdown = 0;
	private static final int RetryCount = 3; // 重试3次
	private static final int RetryTime = 10; // 10秒后重试
	private Timer _timer;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case H_W_UPDATEDIALOG_MAX:
				_updateDialog.setMax(_fileLength);
				break;
			case H_W_UPDATEDIALOG_NOW:
				int x = _downedFileLength * 100 / _fileLength;
				_updateDialog.setMessage("正在下载新版本，已完成" + x + "%");
				break;
			case H_W_SECOND:
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
		setContentView(R.layout.welcome);
		
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		settings = getSharedPreferences("whzxt.net", 0);
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		imei = tm.getDeviceId();
		_retry = 0;
		_countdown = 0;
		_timer = new Timer();
		_timer.schedule(new TimerTask() {
			@Override
			public void run() {
				handler.sendEmptyMessage(H_W_SECOND);
			}
		}, 1000, 1000);
		checkNetWork();
        execCommand("chmod 777 /dev/watchdog");
	}

	private void checkNetWork() {
		new AsyncTask<Void, Void, Integer>() {
			@Override
			protected Integer doInBackground(Void... args) {
				server = getString(R.string.server1);
				httpRequest = new HttpPost(server + "/test.ashx");
				try {
					Log.i("welcome", "test");
					httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() != 200) {
						Log.i("welcome", "test fail,change server");
						server = getString(R.string.server2);
					}
				} catch (ClientProtocolException e) {
					server = getString(R.string.server2);
					e.printStackTrace();
				} catch (IOException e) {
					server = getString(R.string.server2);
					e.printStackTrace();
				}
				httpRequest = new HttpPost(server + "/getdeviceinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				params.add(new BasicNameValuePair("imei", imei));
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					return 0;
				}
				try {
					Log.i("welcome", "getdeviceinfo");
					httpResponse = new DefaultHttpClient().execute(httpRequest);
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					return 0;
				} catch (IOException e) {
					e.printStackTrace();
					return 0;
				}
				Log.i("welcome", "getdeviceinfo:" + httpResponse.getStatusLine().getStatusCode());
				if (httpResponse.getStatusLine().getStatusCode() == 200) {
					try {
						strResult = EntityUtils.toString(httpResponse.getEntity());
					} catch (ParseException e) {
						e.printStackTrace();
						return 0;
					} catch (IOException e) {
						e.printStackTrace();
						return 0;
					}
					if (strResult.startsWith("success|")) {
						return 1;
					} else if (strResult.startsWith("failure|")) {
						return 2;
					}
				}
				return 0;
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == 1) {
					txtStatus.setText("网络连接成功");
					results = strResult.split("\\|");
					if (results.length < 5) {
						txtStatus.setText("没有获取到驾校信息,请联系系统管理员");
						return;
					}
					// 保存密码到配置文件
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("offlinepassword", results[1].substring(results[1].length() - 6));
					editor.putString("session", results[1]);
					editor.putString("deviceID", results[2]);
					editor.putString("deviceName", results[3]);
					editor.putString("schoolID", results[4]);
					editor.putString("schoolName", results[5]);
					editor.commit();
					// 初始化并跳转到主界面
					Intent intent = new Intent();
					Bundle bundle = new Bundle();
					bundle.putString("session", results[1]);
					bundle.putString("deviceID", results[2]);
					bundle.putString("deviceName", results[3]);
					bundle.putString("schoolID", results[4]);
					bundle.putString("schoolName", results[5]);
					intent.putExtras(bundle);
					intent.setClass(WelcomeActivity.this, MainActivity.class);
					startActivity(intent);
					finish();
				} else if (result == 2) {
					results = strResult.split("\\|");
					if (results[1].equals("version_error")) {
						_updateDialog = new ProgressDialog(WelcomeActivity.this);
						_updateDialog.setMessage("正在等待下载新版本...");
						_updateDialog.setIndeterminate(true);
						_updateDialog.show();
						downloadAPK();
					} else {
						txtStatus.setText(results[1]);
						Toast.makeText(WelcomeActivity.this, results[1], Toast.LENGTH_SHORT).show();
					}
				} else {
					if (_retry <= RetryCount) {
						_retry++;
						_countdown = RetryTime;
					} else {
						exitConfirm();
					}
				}
			}
		}.execute();
	}

	private void second() {
		if (_countdown > 0) {
			txtStatus.setText("网络连接失败," + _countdown + "秒后重试...");
			if (--_countdown == 0) {
				txtStatus.setText("正在连接网络...");
				checkNetWork();
			}
		}
	}

	private void exitConfirm() {
		AlertDialog alertDialog = new AlertDialog.Builder(WelcomeActivity.this).setTitle("网络连接失败,请选择").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("关闭程序", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				WelcomeActivity.this.finish();
			}
		}).setNegativeButton("离线模式", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (settings.getString("offlinepassword", "") != "") {
					offline();
				} else {
					Toast.makeText(WelcomeActivity.this, "设备尚未初始化,不能使用离线模式", Toast.LENGTH_LONG).show();
				}
			}
		}).create();
		alertDialog.show();
	}

	private void offline() {
		// 初始化并跳转到主界面
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putString("session", settings.getString("session", ""));
		bundle.putString("deviceID", settings.getString("deviceID", ""));
		bundle.putString("deviceName", settings.getString("deviceName", ""));
		bundle.putString("schoolID", settings.getString("schoolID", ""));
		bundle.putString("schoolName", settings.getString("schoolName", ""));
		bundle.putString("offline", "true");
		intent.putExtras(bundle);
		intent.setClass(WelcomeActivity.this, MainActivity.class);
		startActivity(intent);
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			 WelcomeActivity.this.finish();
		}
		return false;
	}

	/**
	 * 下载新版本
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
					String savePathString = Environment.getExternalStorageDirectory() + "/download/" + "zpad.apk";
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

	public void execCommand(String command) {
		Process process = null;
		DataOutputStream os = null;
		try {
			process = Runtime.getRuntime().exec("su");
			//

			InputStream inputstream = process.getInputStream();
			InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
			BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

			//
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(command + "\n");
			os.writeBytes("exit\n");
			os.flush();
			//
			// read the ls output
			String line = "";

			StringBuilder sb = new StringBuilder(line);
			while ((line = bufferedreader.readLine()) != null) {
				sb.append(line);
				sb.append('\n');
			}

			// ////////////
			process.waitFor();
		} catch (Exception e) {
			Log.d("*** DEBUG ***", "Unexpected error - Here is what I know: " + e.getMessage());
		}
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
	protected void onDestroy() {
		_timer.cancel();
		_timer.purge();
		super.onDestroy();
	}
}