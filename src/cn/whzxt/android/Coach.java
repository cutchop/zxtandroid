package cn.whzxt.android;

public class Coach {
	/**
	 * 教练编号
	 */
	public String ID;
	/**
	 * 教练姓名
	 */
	public String Name;
	/**
	 * 教练卡号
	 */
	public String CardNo;
	/**
	 * 教练身份证
	 */
	public String IDCardNo;
	/**
	 * 教练证号
	 */
	public String Certificate;

	public Coach() {
		this.ID = "";
		this.Name = "";
		this.CardNo = "";
		this.IDCardNo = "";
		this.Certificate = "";
	}

	/**
	 * 重置所有属性
	 */
	public void reset() {
		this.ID = "";
		this.Name = "";
		this.CardNo = "";
		this.IDCardNo = "";
		this.Certificate = "";
	}
}
