package cn.whzxt.android;

public class NativeGPIO {
	public static native boolean setRelay(boolean state);

	public static native int getAccState();

	public static native int getRotateSpeed(int flag, int term);

	public static native int readmile();

	public static native boolean disableRotateSpeed(int flag);

	static {
		System.loadLibrary("gpio_zxt_fixed");
	}
}
