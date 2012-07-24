package cn.whzxt.android;

public class Student {
	/**
	 * 学员编号
	 */
	public static String ID;
	/**
	 * 学员姓名
	 */
	public static String Name;
	/**
	 * 学员卡号
	 */
	public static String CardNo;
	/**
	 * 学员身份证号
	 */
	public static String IDCardNo;
	/**
	 * 学员驾驶类型
	 */
	public static String DriverType;
	/**
	 * 余额
	 */
	public static int Balance;
	/**
	 * 实时余额
	 */
	public static int RealBalance;
	/**
	 * 学员累计训练里程（米）
	 */
	public static int TotalMi;
	/**
	 * 学员实时累计里程（米）
	 */
	public static int RealTotalMi;
	/**
	 * 学员累计训练时长（分钟）
	 */
	public static int TotalTime;
	/**
	 * 学员实时累计训练时长（分钟）
	 */
	public static int RealTotalTime;
	/**
	 * 是否是教练
	 */
	public static Boolean IsCoach;

	/**
	 * 重置所有属性
	 */
	public static void Init() {
		ID = Name = CardNo = IDCardNo = DriverType = "";
		Balance = RealBalance = TotalMi = RealTotalMi = TotalTime = RealTotalTime = 0;
		IsCoach = false;
	}
}