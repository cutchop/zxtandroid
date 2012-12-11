package cn.whzxt.android;

public class Coach {
	/**
	 * 教练编号
	 */
	public static String ID;
	/**
	 * 教练姓名
	 */
	public static String Name;
	/**
	 * 教练卡号
	 */
	public static String CardNo;
	/**
	 * 教练身份证
	 */
	public static String IDCardNo;
	/**
	 * 教练证号
	 */
	public static String Certificate;
	/**
	 * 教练等级
	 */
	public static int Level;
	/**
	 * 本日累计培训小时
	 */
	public static int DayTotal;
	/**
	 * 本月累计培训小时
	 */
	public static int MonthTotal;

	/**
	 * 初始化
	 */
	public static void Init() {
		ID = Name = CardNo = IDCardNo = Certificate = "";
		Level = 1;
		DayTotal = MonthTotal = 0;
	}
}
