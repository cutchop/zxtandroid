package cn.whzxt.android;

import java.util.ArrayList;
import java.util.List;

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.widget.TextView;
import android.widget.Toast;

public class WelcomeActivity extends Activity {
	TextView txtStatus;
	Handler handler = new Handler();
	HttpPost httpRequest;
	HttpResponse httpResponse;
	SharedPreferences settings;
	private String server;
	private String imei;
	private String strResult;
	private String[] results;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.welcome);
		txtStatus = (TextView) findViewById(R.id.txtStatus);
		settings = getSharedPreferences("whzxt.net", 0);
		TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		imei = tm.getDeviceId();
		server = getString(R.string.server1);

		new Thread() {
			public void run() {
				httpRequest = new HttpPost(server + "/test.ashx");
				try {
					httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() != 200) {
						server = getString(R.string.server2);
					}
				} catch (Exception e) {
					server = getString(R.string.server2);
				}
				httpRequest = new HttpPost(server + "/getdeviceinfo.ashx");
				List<NameValuePair> params = new ArrayList<NameValuePair>(2);
				params.add(new BasicNameValuePair("imei", imei));
				params.add(new BasicNameValuePair("ver", getString(R.string.version)));
				try {
					httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
					httpResponse = new DefaultHttpClient().execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						strResult = EntityUtils.toString(httpResponse.getEntity());
						if (strResult.startsWith("success|")) {
							handler.post(new Runnable() {
								public void run() {
									results = strResult.split("\\|");
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
								}
							});
						} else if (strResult.startsWith("failure|")) {
							results = strResult.split("\\|");
							if (results[1].equals("version_error")) {
								AlertDialog alertDialog = new AlertDialog.Builder(WelcomeActivity.this).setTitle("发现新版本,需要更新").setIcon(android.R.drawable.ic_menu_help).setPositiveButton("确定", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										Uri uri = Uri.parse(server + "/zpad.apk");
										startActivity(new Intent(Intent.ACTION_VIEW, uri));
									}
								}).create();
								alertDialog.show();
							} else {
								handler.post(new Runnable() {
									public void run() {
										txtStatus.setText(results[1]);
										Toast.makeText(WelcomeActivity.this, results[1], Toast.LENGTH_SHORT).show();
									}
								});
							}
						}
					} else {
						handler.post(new Runnable() {
							public void run() {
								exitConfirm();
							}
						});
					}
				} catch (Exception e) {
					e.printStackTrace();
					handler.post(new Runnable() {
						public void run() {
							exitConfirm();
						}
					});
				}
			}
		}.start();
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
}