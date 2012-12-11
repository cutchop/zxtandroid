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
	 * 学员当日训练总时长（分钟）
	 */
	public static int TodayTrainTime;
	/**
	 * 学员当日训练总里程（米）
	 */
	public static int TodayTrainMi;
	/**
	 * 学员当前状态
	 */
	public static String Status;
	/**
	 * 是否是教练
	 */
	public static Boolean IsCoach;
	/**
	 * 不需要验证指纹
	 */
	public static Boolean NotNeedFinger;
	/**
	 * 是否有指纹
	 */
	public static Boolean HasFinger;
	
	/**
	 * 学员当前科目
	 */
	public static int Subject;

	/**
	 * 是否计费
	 */
	public static Boolean IsCharging;
	
	/**
	 * 重置所有属性
	 */
	public static void Init() {
		ID = Name = CardNo = IDCardNo = DriverType = Status = "";
		Balance = RealBalance = TotalMi = TotalTime = TodayTrainMi = TodayTrainTime = 0;
		RealTotalMi = RealTotalTime = 0;
		HasFinger = NotNeedFinger = IsCoach = false;
		IsCharging = true;
		Subject = 0;
	}
}
