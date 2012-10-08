package cn.whzxt.android;

public class RfidOper {

	public static interface Callback {
		public abstract void onStart();
		public abstract void onFinish(String data);
		public abstract void onFinish(int ret);
	}
	public static int TYPE_READ = 0;
	public static int TYPE_WRITE = 1;

	/**
	 * 操作类型
	 */
	public int operType;
	/**
	 * 地址
	 */
	public int[] address;
	/**
	 * 类型(块/区)
	 */
	public int kind;
	/**
	 * 数据
	 */
	public int[] content;
	/**
	 * 回调函数
	 */
	public Callback callback;
}
