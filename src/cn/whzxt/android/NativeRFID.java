package cn.whzxt.android;

public class NativeRFID {
	public native static boolean open_fm1702();// 打开设备节点，使能模块；

	public native static void select_type(int type);// 选择读卡类型；

	public native static void close_fm1702();// 关闭模块，关闭设备节点；

	public native static String read_A();// 读A卡；

	public native static String read_B();// 读B卡；

	public native static int set_key(int[] key);// 修改密码；

	public native static String read_card(int[] address, int[] key, int kind);// 读区/块；

	public native static int write_card(int[] address, int[] key, int kind, int[] content);// 写区/块；
	// public native static boolean isActive();

	static {
		System.loadLibrary("rfid_zxt");
	}
}
