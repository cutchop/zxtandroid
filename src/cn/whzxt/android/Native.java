package cn.whzxt.android;

public class Native {
	/**************    commual subroutin  ***********/
	public native static int auto_init(String filename,int baud);//打开串口
	public native static int ic_exit(int icdev);//关闭串口
	
	//***********************    operate sle 4442    **************************
	public native static String srd_4442(int icdev,int offset,int len);//读卡
	public native static int swr_4442(int icdev,int offset,int len, int[] data_buffer);//写卡
	public native static int csc_4442(int icdev,int len, int[] key_now);//核对密码
	public native static int wsc_4442(int icdev,int len, int[] key_new);//修改密码
	public native static int chk_4442(int icdev);//测卡类型
	
	//***********************    operate at88sc102    ************************
	public native static String srd_102(int icdev,int zone,int offset,int len);//读卡
	public native static int swr_102(int icdev,int zone,int offset,int len,  int[] data_buffer);//写卡
	public native static int csc_102(int icdev,int len,  int[] key_now);//核对密码
	public native static int wsc_102(int icdev,int len,  int[] key_new);//修改密码
	public native static int chk_102(int icdev);//测卡类型
	public native static int cesc_102(int icdev,int zone,int len,int[] key_erase);//核对擦卡密码
	public native static int wesc_102(int icdev,int zone,int len,int[] key_erase);//修改擦卡密码
	public native static int ser_102(int icdev,int zone,int offset,int len);//擦卡
	
	//***********************    operate at24c02    **************************
	public native static String srd_24c02(int icdev,int offset,int len);//读卡
	public native static int swr_24c02(int icdev,int offset,int len, int[] data_buffer);//写卡
	public native static int chk_24c02(int icdev);//测卡类型

	static {
		System.loadLibrary("IC_demo_zxt"); 
	}
}
