package cn.whzxt.android;

import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.Queue;

import android.os.SystemClock;
import android.util.Log;

public class CardOper implements Runnable {

	public static interface OnChange {
		public abstract void onReadStart();

		public abstract void onFind(String msg);

		public abstract void onLose();
	}

	public static final String MSG_COACH_FIRST = "请先插教练卡";
	public static final String MSG_SCHOOL_ERROR = "此卡不属于本驾校";
	public static final String MSG_READ_FAILURE = "读卡失败,请尝试重新插卡";
	public static final int RFID_TYPE_A = 0x02;
	public static final int RFID_TYPE_B = 0x01;
	public static final int RFID_BLOCK = 0;
	public static final int RFID_EARA = 1;
	public static final int NO_CARD = 0;
	public static final int CARD_24C02 = 1;
	public static final int CARD_S50 = 2;
	public static final int CARD_S70 = 3;
	public int CardType = NO_CARD;

	private Boolean _stop = false;

	private OnChange onchang = null;
	private String schoolID = "";

	private static final int[] RFID_KEY = new int[] { 255, 255, 255, 255, 255, 255 };

	private static final String PATH = "/dev/s3c2410_serial1";
	private static final int BAUD = 9600;
	private int fd;
	private String _rfidUID = null;

	public static Object lock = new Object();

	private Thread _thread = null;
	private Queue<RfidOper> rfidOpers;

	public CardOper(String sc, OnChange oc) {
		rfidOpers = new LinkedList<RfidOper>();
		this.schoolID = sc;
		this.onchang = oc;
	}

	/**
	 * 打开IC卡读卡器
	 */
	public void openICCard() {
		fd = Native.auto_init(PATH, BAUD);
	}

	/**
	 * 打开RFID读卡器
	 */
	public void openRfidCard() {
		if (!NativeRFID.open_fm1702()) {
			NativeRFID.close_fm1702();
			NativeRFID.open_fm1702();
		}
	}

	/**
	 * RFID选择卡型
	 * 
	 * @param 卡型
	 */
	public void selectRfidType(int type) {
		NativeRFID.select_type(type);
	}

	public void start() {
		if (_thread == null) {
			_thread = new Thread(this);
			_stop = false;
			_thread.start();
		}
	}

	public void run() {
		SystemClock.sleep(5000);// 启动后5秒再开始读卡
		while (!_stop) {
			Log.i("gc", "checkcard");
			switch (CardType) {
			case CARD_24C02:
				Logger.Write("检查IC卡(1)");
				synchronized (lock) {
					if (Native.chk_24c02(fd) != 0) {
						SystemClock.sleep(50);
						Logger.Write("检查IC卡(2)");
						if (Native.chk_24c02(fd) != 0) {
							Logger.Write("没有发现IC卡");
							CardType = NO_CARD;
							onchang.onLose();
						}
					}
				}
				break;
			case CARD_S50:
					Logger.Write("检查RFID(1)");
					_rfidUID = NativeRFID.read_A();
					if (_rfidUID == null || _rfidUID.charAt(16) != '0') {
						Logger.Write("检查RFID(2)");
						_rfidUID = NativeRFID.read_A();
						if (_rfidUID == null || _rfidUID.charAt(16) != '0') {
							Logger.Write("检查RFID(3)");
							_rfidUID = NativeRFID.read_A();
							if (_rfidUID == null || _rfidUID.charAt(16) != '0') {
								Logger.Write("没有发现RFID卡");
								CardType = NO_CARD;
								onchang.onLose();
							}
						}
					}
				break;
			case CARD_S70:
					Logger.Write("检查RFID(1)");
					_rfidUID = NativeRFID.read_A();
					if (_rfidUID == null || _rfidUID.charAt(16) != '1') {
						Logger.Write("检查RFID(2)");
						_rfidUID = NativeRFID.read_A();
						if (_rfidUID == null || _rfidUID.charAt(16) != '1') {
							Logger.Write("检查RFID(3)");
							_rfidUID = NativeRFID.read_A();
							if (_rfidUID == null || _rfidUID.charAt(16) != '1') {
								Logger.Write("没有发现RFID卡");
								CardType = NO_CARD;
								onchang.onLose();
							}
						}
					}
				break;
			case NO_CARD:
					CardType = readCardType();
				if (CardType != NO_CARD) {
					onchang.onReadStart();
					String msg = null;
					synchronized (lock) {
						Logger.Write("开始读卡");
						msg = read();
						Logger.Write("读卡完毕");
					}
					onchang.onFind(msg);
				}
				break;
			default:
				break;
			}
			if (CardType == NO_CARD) {
				rfidOpers.clear();
				SystemClock.sleep(100);
			} else {
				if (CardType == CARD_S50 || CardType == CARD_S70) {
					RfidOper oper = rfidOpers.poll();
					while (oper != null) {
						oper.callback.onStart();
						if (oper.operType == RfidOper.TYPE_READ) {
							String data = NativeRFID.read_card(oper.address, RFID_KEY, oper.kind);
							oper.callback.onFinish(data);
						} else {
							int ret = NativeRFID.write_card(oper.address, RFID_KEY, oper.kind, oper.content);
							oper.callback.onFinish(ret);
						}
						oper = rfidOpers.poll();
					}
				}
				SystemClock.sleep(1000);
			}
		}
	}

	private int readCardType() {
		Logger.Write("检测IC卡");
		if (Native.chk_24c02(fd) == 0) {
			Logger.Write("发现IC卡");
			return CARD_24C02;
		}
		Logger.Write("检测RFID卡");
		_rfidUID = NativeRFID.read_A();
		if (null != _rfidUID) {
			if (_rfidUID.charAt(16) == '0') {
				Logger.Write("发现RFID卡");
				return CARD_S50;
			}
			if (_rfidUID.charAt(16) == '1') {
				Logger.Write("发现RFID卡");
				return CARD_S70;
			}
		}
		return NO_CARD;
	}

	private String read() {
		if (CardType == CARD_24C02) {
			Log.i("gc", "readcard 1");
			String tmpdata = Native.srd_24c02(fd, 0x08, 0x22);
			Logger.Write(tmpdata);
			String[] tmpchars = tmpdata.split(" ");
			if (tmpchars[0].equals("02") && Coach.CardNo.equals("")) {
				return MSG_COACH_FIRST;
			}
			Log.i("gc", "readcard 2");
			// 驾校ID
			String tmpschool = String.valueOf((char) Integer.parseInt(tmpchars[24], 16));
			for (int i = 25; i < 32; i++) {
				tmpschool += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
			}
			// 兼容老卡
			if (tmpschool.equals("11111111")) {
				tmpschool = "001001";
			}
			tmpschool = tmpschool.trim();
			if (!tmpschool.equals(schoolID)) {
				return MSG_SCHOOL_ERROR;
			}
			// 卡号
			String tmpCardNo = String.valueOf((char) Integer.parseInt(tmpchars[8], 16));
			for (int i = 9; i < 16; i++) {
				tmpCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
			}
			// 卡内剩余时长
			try {
				Student.RealBalance = Student.Balance = Integer.parseInt(tmpchars[1].toString() + tmpchars[2].toString(), 16);
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
			Log.i("gc", "readcard 3");
			if (tmpchars[0].equals("01")) {
				// 教练卡
				Coach.CardNo = Student.CardNo = tmpCardNo;
				Coach.ID = Student.ID = "";
				Coach.Name = Student.Name = tmpCardName;
				Coach.IDCardNo = Student.IDCardNo = "";
				Student.IsCoach = true;
			} else {
				// 学员卡
				Student.CardNo = tmpCardNo;
				Student.ID = "";
				Student.Name = tmpCardName;
				Student.IDCardNo = "";
				Student.IsCoach = false;
			}
			Student.HasFinger = tmpchars[32].equals("01");
			Student.NotNeedFinger = tmpchars[33].equals("01");
		} else if (CardType == CARD_S50 || CardType == CARD_S70) {
			Log.i("gc", "readcard 1");
			// 身份信息
			String tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
			Logger.Write(tmpdata);
			if (null == tmpdata || !tmpdata.endsWith("1")) {
				SystemClock.sleep(100);// 100毫秒后尝试重新读卡
				tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
			}
			if (null == tmpdata) {
				return MSG_READ_FAILURE;
			}
			if (!tmpdata.endsWith("1")) {
				return MSG_READ_FAILURE;
			}
			String[] tmpchars = tmpdata.split(" ");
			// 卡类型
			if (tmpchars[0].equals("02") && Coach.CardNo.equals("")) {
				return MSG_COACH_FIRST;
			}
			// 驾校ID
			String tmpschool = "";
			for (int i = 9; i < 18; i++) {
				tmpschool += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
			}
			tmpschool = tmpschool.trim();
			if (!tmpschool.equals(schoolID)) {
				return MSG_SCHOOL_ERROR;
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
				Coach.CardNo = Student.CardNo = tmpCardNo;
				Coach.ID = Student.ID = "";
				Coach.Name = Student.Name = tmpCardName;
				Coach.IDCardNo = Student.IDCardNo = "";
				Student.IsCoach = true;
			} else {
				// 学员卡
				Student.CardNo = tmpCardNo;
				Student.ID = "";
				Student.Name = tmpCardName;
				Student.IDCardNo = "";
				Student.IsCoach = false;
			}
			Log.i("gc", "readcard 2");
			// 余额.学时.里程
			tmpdata = NativeRFID.read_card(new int[] { 18 }, RFID_KEY, RFID_BLOCK);
			if (null == tmpdata || !tmpdata.endsWith("1")) {
				SystemClock.sleep(100);// 100毫秒后尝试重新读卡
				tmpdata = NativeRFID.read_card(new int[] { 18 }, RFID_KEY, RFID_BLOCK);
			}
			if (null != tmpdata && tmpdata.endsWith("1")) {
				tmpchars = tmpdata.split(" ");
				Student.RealBalance = Student.Balance = Integer.parseInt(tmpchars[0].toString() + tmpchars[1].toString(), 16);
				Student.TotalTime = Integer.parseInt(tmpchars[2].toString() + tmpchars[3].toString(), 16);
				Student.TotalMi = Integer.parseInt(tmpchars[4].toString() + tmpchars[5].toString() + tmpchars[6], 16);
			}
			// 判断是否有指纹
			Log.i("gc", "readcard 3");
			tmpdata = NativeRFID.read_card(new int[] { 20 }, RFID_KEY, RFID_BLOCK);
			if (null == tmpdata || !tmpdata.endsWith("1")) {
				SystemClock.sleep(100);// 100毫秒后尝试重新读卡
				tmpdata = NativeRFID.read_card(new int[] { 20 }, RFID_KEY, RFID_BLOCK);
			}
			if (null == tmpdata) {
				return MSG_READ_FAILURE;
			}
			if (!tmpdata.endsWith("1")) {
				return MSG_READ_FAILURE;
			}
			Student.HasFinger = tmpdata.startsWith("01");
			// 判断是否需要验证指纹
			Log.i("gc", "readcard 4");
			tmpdata = NativeRFID.read_card(new int[] { 21 }, RFID_KEY, RFID_BLOCK);
			if (null == tmpdata || !tmpdata.endsWith("1")) {
				SystemClock.sleep(100);// 100毫秒后尝试重新读卡
				tmpdata = NativeRFID.read_card(new int[] { 21 }, RFID_KEY, RFID_BLOCK);
			}
			if (null == tmpdata) {
				return MSG_READ_FAILURE;
			}
			if (!tmpdata.endsWith("1")) {
				return MSG_READ_FAILURE;
			}
			Student.NotNeedFinger = tmpdata.startsWith("01");
		}
		return null;
	}

	/**
	 * 初始化卡
	 * 
	 * @param 卡类型
	 *            ,教练卡:01,学员卡:02
	 * @param 卡号
	 * @param 驾校ID
	 * @return
	 */
	public String CardInit(String type, String cardno, String school) {
		Logger.Write("等待初始化卡");
		synchronized (lock) {
			Logger.Write("开始初始化卡");
			try {
				int i = 0;
				if (CardType == CARD_24C02) {
					int[] context = new int[32];
					for (i = 0; i < context.length; i++) {
						context[i] = ' ';
					}
					// 卡型
					context[0] = 2;
					if (type.equals("01")) {
						context[0] = 1;
					}
					// 余额
					context[1] = 0;
					context[2] = 0;
					context[3] = 0;
					context[4] = 0;
					// 卡号
					for (i = 0; i < cardno.length(); i++) {
						context[i + 8] = cardno.charAt(i);
					}
					for (i = 0; i < school.length(); i++) {
						context[i + 24] = school.charAt(i);
					}
					if (Native.swr_24c02(fd, 0x08, 0x20, context) != 0) {
						SystemClock.sleep(100);
						if (Native.swr_24c02(fd, 0x08, 0x20, context) != 0) {
							return "卡初始化失败,请重试";
						}
					}
				} else if (CardType == CARD_S50 || CardType == CARD_S70) {
					int[] context = new int[48];
					for (i = 0; i < context.length; i++) {
						context[i] = ' ';
					}
					context[0] = 2;
					if (type.equals("01")) {
						context[0] = 1;
					}
					for (i = 0; i < cardno.length(); i++) {
						context[i + 1] = cardno.charAt(i);
					}
					for (i = 0; i < school.length(); i++) {
						context[i + 9] = school.charAt(i);
					}
					if (NativeRFID.write_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA, context) != 1) {
						SystemClock.sleep(100);
						if (NativeRFID.write_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA, context) != 1) {
							return "卡初始化失败,请重试";
						}
					}
					/*
					 * if (NativeRFID.write_card(new int[] { 15 }, RFID_KEY,
					 * RFID_BLOCK, new int[] { 0 }) != 1) {
					 * SystemClock.sleep(100); if (NativeRFID.write_card(new
					 * int[] { 15 }, RFID_KEY, RFID_BLOCK, new int[] { 0 }) !=
					 * 1) { return "卡初始化失败,请重试"; } }
					 */
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			Logger.Write("初始化卡完毕");
		}
		return "卡初始化成功";
	}

	/**
	 * 写IC卡数据
	 * 
	 * @param 起始位置
	 * @param 长度
	 * @param 数据
	 */
	public void WriteIC(int offset, int len, int[] data) {
		Logger.Write("等待写IC卡");
		synchronized (lock) {
			Logger.Write("开始写IC卡");
			try {
				Native.swr_24c02(fd, offset, len, data);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Logger.Write("写IC卡完毕");
		}
	}

	/**
	 * 检查卡号是否与当前学员相符（防止不读卡的异常）
	 * 
	 */
	public Boolean checkCardNo() {
		Logger.Write("检查卡号");
		synchronized (lock) {
			if (CardType == CARD_24C02) {
				String tmpdata = Native.srd_24c02(fd, 16, 8);
				Logger.Write(tmpdata);
				if (tmpdata == null) {
					tmpdata = Native.srd_24c02(fd, 16, 8);
					Logger.Write(tmpdata);
				}
				if (tmpdata == null) {
					return false;
				}
				String[] tmpchars = tmpdata.split(" ");
				if (tmpchars.length != 8) {
					return false;
				}
				// 卡号
				String tmpCardNo = String.valueOf((char) Integer.parseInt(tmpchars[0], 16));
				for (int i = 1; i < 8; i++) {
					tmpCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
				}
				tmpCardNo = tmpCardNo.trim();
				return tmpCardNo.equals(Student.CardNo);
			} else if (CardType == CARD_S50 || CardType == CARD_S70) {
				/*
				String tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
				Logger.Write(tmpdata);
				if (null == tmpdata || !tmpdata.endsWith("1")) {
					SystemClock.sleep(100);// 100毫秒后尝试重新读卡
					tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
					if (null == tmpdata || !tmpdata.endsWith("1")) {
						SystemClock.sleep(100);// 100毫秒后尝试重新读卡
						tmpdata = NativeRFID.read_card(new int[] { 1, 1 }, RFID_KEY, RFID_EARA);
					}
				}
				if (null == tmpdata) {
					return false;
				}
				if (!tmpdata.endsWith("1")) {
					return false;
				}
				String[] tmpchars = tmpdata.split(" ");
				// 卡号
				String tmpCardNo = "";
				for (int i = 1; i < 9; i++) {
					tmpCardNo += String.valueOf((char) Integer.parseInt(tmpchars[i], 16));
				}
				tmpCardNo = tmpCardNo.trim();
				return tmpCardNo.equals(Student.CardNo);
				*/
			}
		}
		return true;
	}

	/**
	 * 重写卡内指纹标记
	 * 
	 * @param 1,0
	 * @return
	 */
	public Boolean WriteFingerFlag(int flag) {
		if (CardType == CARD_24C02) {
			WriteIC(0x28, 0x01, new int[] { flag });
		} else if (CardType == CARD_S50 || CardType == CARD_S70) {
			RfidOper rfidOper = new RfidOper();
			rfidOper.operType = RfidOper.TYPE_WRITE;
			rfidOper.address = new int[] { 20 };
			rfidOper.kind = RFID_BLOCK;
			rfidOper.content = new int[] { flag };
			rfidOper.callback = new RfidOper.Callback() {
				public void onFinish(int ret) {
					Logger.Write("写指纹标记结束,ret=" + ret);
				}

				public void onStart() {
					Logger.Write("开始写指纹标记");
				}

				public void onFinish(String data) {
				}
			};
			rfidOpers.offer(rfidOper);
		}
		return true;
	}

	/**
	 * 重写卡内是否需要验证指纹的标记
	 * 
	 * @param 1,0
	 * @return
	 */
	public Boolean WriteNotNeedFingerFlag(int flag) {
		if (CardType == CARD_24C02) {
			WriteIC(0x29, 0x01, new int[] { flag });
		} else if (CardType == CARD_S50 || CardType == CARD_S70) {
			RfidOper oper = new RfidOper();
			oper.operType = RfidOper.TYPE_WRITE;
			oper.address = new int[] { 21 };
			oper.kind = RFID_BLOCK;
			oper.content = new int[] { flag };
			oper.callback = new RfidOper.Callback() {
				public void onFinish(int ret) {
					Logger.Write("写是否验证指纹标记结束,ret=" + ret);
				}

				public void onStart() {
					Logger.Write("开始写是否验证指纹标记");
				}

				public void onFinish(String data) {
				}
			};
			rfidOpers.offer(oper);
		}
		return true;
	}

	/**
	 * 重写卡内余额、学时、里程
	 */
	public void WriteBalance(int balance) {
		String data = Integer.toHexString(balance);
		while (data.length() < 4) {
			data = "0" + data;
		}
		int[] buffYE = new int[4];
		buffYE[0] = Integer.parseInt(data.substring(0, 2), 16);
		buffYE[1] = Integer.parseInt(data.substring(2), 16);
		buffYE[2] = Integer.parseInt(data.substring(0, 2), 16);
		buffYE[3] = Integer.parseInt(data.substring(2), 16);
		if (CardType == CARD_24C02) {
			WriteIC(0x09, 0x04, buffYE);
		} else if (CardType == CARD_S50 || CardType == CARD_S70) {
			int[] buff = new int[7];
			// 余额
			buff[0] = buffYE[0];
			buff[1] = buffYE[1];
			// 学时
			data = Integer.toHexString(Student.RealTotalTime);
			while (data.length() < 4) {
				data = "0" + data;
			}
			buff[2] = Integer.parseInt(data.substring(0, 2), 16);
			buff[3] = Integer.parseInt(data.substring(2), 16);
			// 里程
			data = Integer.toHexString(Student.RealTotalMi);
			while (data.length() < 6) {
				data = "0" + data;
			}
			buff[4] = Integer.parseInt(data.substring(0, 2), 16);
			buff[5] = Integer.parseInt(data.substring(2, 4), 16);
			buff[6] = Integer.parseInt(data.substring(2), 16);

			RfidOper oper = new RfidOper();
			oper.operType = RfidOper.TYPE_WRITE;
			oper.address = new int[] { 18 };
			oper.kind = RFID_BLOCK;
			oper.content = buff;
			oper.callback = new RfidOper.Callback() {
				public void onFinish(int ret) {
					Logger.Write("写余额结束,ret=" + ret);
				}

				public void onStart() {
					Logger.Write("开始写余额");
				}

				public void onFinish(String data) {
				}
			};
			rfidOpers.offer(oper);
		}
	}

	/**
	 * 重写卡内姓名和身份证
	 */
	public Boolean WriteName() {
		if (CardType == CARD_24C02) {
			int[] context = new int[8];
			for (int i = 0; i < Student.Name.length(); i++) {
				try {
					int j = bytes2int(String.valueOf(Student.Name.charAt(i)).getBytes("GB2312"));
					String data = Integer.toHexString(j);
					while (data.length() < 4) {
						data = "0" + data;
					}
					context[i * 2] = Integer.parseInt(data.substring(0, 2), 16);
					context[1 + i * 2] = Integer.parseInt(data.substring(2), 16);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			Logger.Write(context);
			WriteIC(0x18, 0x08, context);
		} else if (CardType == CardOper.CARD_S50 || CardType == CardOper.CARD_S70) {
			int[] address = new int[2];
			address[0] = 1;
			address[1] = 1;
			int[] context = new int[48];
			for (int i = 0; i < context.length; i++) {
				context[i] = ' ';
			}
			// 类型：教练卡、学员卡
			if (Student.IsCoach) {
				context[0] = 1;
			} else {
				context[0] = 2;
			}
			// 卡号
			for (int i = 0; i < Student.CardNo.length(); i++) {
				context[i + 1] = Student.CardNo.charAt(i);
			}
			// 驾校ID
			for (int i = 0; i < schoolID.length(); i++) {
				context[i + 9] = schoolID.charAt(i);
			}
			// 身份证号
			for (int i = 0; i < Student.IDCardNo.length(); i++) {
				context[i + 18] = Student.IDCardNo.charAt(i);
			}
			// 姓名
			for (int i = 0; i < Student.Name.length(); i++) {
				try {
					int j = bytes2int(String.valueOf(Student.Name.charAt(i)).getBytes("GB2312"));
					String data = Integer.toHexString(j);
					while (data.length() < 4) {
						data = "0" + data;
					}
					context[36 + i * 2] = Integer.parseInt(data.substring(0, 2), 16);
					context[37 + i * 2] = Integer.parseInt(data.substring(2), 16);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			RfidOper oper = new RfidOper();
			oper.operType = RfidOper.TYPE_WRITE;
			oper.address = new int[] { 1, 1 };
			oper.kind = RFID_EARA;
			oper.content = context;
			oper.callback = new RfidOper.Callback() {
				public void onFinish(int ret) {
					Logger.Write("写RFID卡结束(姓名/身份证),ret=" + ret);
				}

				public void onStart() {
					Logger.Write("开始写RFID卡(姓名/身份证)");
				}

				public void onFinish(String data) {
				}
			};
			rfidOpers.offer(oper);
		}
		return true;
	}

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

	public void stop() {
		if (_thread != null) {
			_stop = true;
			// _thread.stop();
			_thread = null;
			// IC卡读卡器
			try {
				Native.ic_exit(fd);
				Log.i("zxt", "ic card closed");
			} catch (Exception e) {
				e.printStackTrace();
			}
			// RFID读卡器
			try {
				NativeRFID.close_fm1702();
				Log.i("zxt", "rfid card closed");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
