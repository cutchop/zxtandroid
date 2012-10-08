package cn.whzxt.android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class Logger {
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static int MAX = 50;
	private static LinkedList<String> logs = new LinkedList<String>();

	/**
	 * 记录日志
	 * 
	 * @param 日志
	 */
	public static void Write(String log) {
		logs.add("[" + dateFormat.format(new Date()) + "]" + log + "\n");
		if (logs.size() > MAX) {
			logs.remove();
		}
	}

	public static void Write(int[] log) {
		String tmp = "";
		for (int i = 0; i < log.length; i++) {
			tmp += Integer.toHexString(log[i]) + " ";
		}
		logs.add("[" + dateFormat.format(new Date()) + "]" + tmp + "\n");
		if (logs.size() > MAX) {
			logs.remove();
		}
	}

	/**
	 * 读取日志
	 */
	public static String Read() {
		String ret = "";
		while (logs.size() > 0) {
			ret += logs.pop();
		}
		return ret;
	}
}
