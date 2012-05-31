package cn.whzxt.android;

public class Student {
	/**
	 * 学员编号
	 */
	public String ID;
	/**
	 * 学员姓名
	 */
	public String Name;
	/**
	 * 学员卡号
	 */
	public String CardNo;
	/**
	 * 学员身份证号
	 */
	public String IDCardNo;
	/**
	 * 学员驾驶类型
	 */
	public String DriverType;
	/**
	 * 学员累计训练里程（米）
	 */
	public int TotalMi;
	/**
	 * 学员实时累计里程（米）
	 */
	public int RealTotalMi;
	/**
	 * 学员累计训练时长（分钟）
	 */
	public int TotalTime;
	/**
	 * 学员实时累计训练时长（分钟）
	 */
	public int RealTotalTime;
	/**
	 * 是否是教练
	 */
	public Boolean IsCoach;

	public Student() {
		this.ID = "";
		this.Name = "";
		this.CardNo = "";
		this.IDCardNo = "";
		this.DriverType = "";
		this.TotalMi = 0;
		this.RealTotalMi = 0;
		this.TotalTime = 0;
		this.RealTotalTime = 0;
		this.IsCoach = false;
	}

	/**
	 * 充值所有属性
	 */
	public void reset() {
		this.ID = "";
		this.Name = "";
		this.CardNo = "";
		this.IDCardNo = "";
		this.DriverType = "";
		this.TotalMi = 0;
		this.RealTotalMi = 0;
		this.TotalTime = 0;
		this.RealTotalTime = 0;
		this.IsCoach = false;
	}
}
