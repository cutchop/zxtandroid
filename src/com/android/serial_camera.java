package com.android;

public class serial_camera {

	static {
		System.loadLibrary("serialcamerajni");
	}

	// 打开串口默认115200
	public native int uart_init0(int uart_bps);// 115200

	// 拍照初始化函数
	public native int photo_init();

	// 拍照命令
	public native int take_photo_cmd();

	// 拍照 自定义保存的路径文件名称
	public native int take_photo(String path);

	// 关闭串口
	public native int uart_close();
}
