package cn.whzxt.android;

public class ICOper {

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
	public int offset;
	/**
	 * 长度
	 */
	public int len;	
	/**
	 * 数据
	 */
	public int[] content;
	/**
	 * 回调函数
	 */
	public Callback callback;
}
