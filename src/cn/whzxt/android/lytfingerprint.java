package cn.whzxt.android;

public class lytfingerprint {
	//打开串口
	public native int PSOpenDevice(int nDeviceType,int nPortNum,int nPortPara,int nPackageSize);
	//关闭串口
	public native int PSCloseDevice();
	/*
	 * 录入指纹步骤
	 * 1，检测手指并录取图像
	 * 2，根据原始图像生成指纹特征
	 * 3，再一次检测手指并录取图像
	 * 4，根据原始图像生成指纹特征
	 * 5，合成模版
	 * 6，存储到固定的某个page(0~256)
	 */
	public native int PSGetImage(int nAddr);
	public native int PSGenChar(int nAddr,int iBufferID);
	public native int PSRegModule(int nAddr);
	public native int PSStoreChar(int nAddr,int iBufferID, int iPageID);
	
	/*
	 * 搜索指纹
	 * 1，检测手指并录取图像
	 * 2，根据原始图像生成指纹特征
	 * 3，//以CharBufferA或CharBufferB中的特征文件搜索整个或部分指纹库 iMbAddress地址
	 */
	public native int PSSearch(int nAddr,int iBufferID, int iStartPage, int iPageNum, int iMbAddress);
	/*
	 * 获取图像
	 * 1，检测手指并录取图像
	 * 2，上传图像
	 */
	public native int PSUpImage(int nAddr,byte[] pImageData,int iImageLength);
	public native int PSImgData2BMP(byte[] pImgData,String pImageFile);
	
	/*
	 * 删除flash 特征文件
	 * 删除flash指纹库中的一个特征文件
	 * 清空flash指纹库
	 */

	public native int  PSDelChar(int nAddr,int iStartPageID,int nDelPageNum);
	public native int  PSEmpty(int nAddr);
	
	/*
	 * 自定义读取匹配ID ---frank
	 */
	public native int  getnFinger();
	
	/*
	 * 20120420
	 * 增加上传指纹特征值的接口
	 * 20120514--Frank
	 * 为了方便客户开发对于PSUpChar以及PSDownChar 我门加上一个path通道接口用于存取或获取数据路径
	 * path用于上传存放指纹数据存放的路径譬如/data/local/tmp/fingerprintdata 建议客户放在/data/local/tmp/目录
	 * 因为权限问题
	 */
	public native int  PSLoadChar(int nAddr,int iBufferID,int iPageID);
	public native int  PSUpChar(int nAddr,int iBufferID, byte[] pTemplet, int iTempletLength,String path);
	
	/*
	 * 20120420
	 * 从上位机下载一个特征文件到特征缓冲区
	 * 增加获取的指纹特征值（上位机保存）与模块中的指纹库对比
	 * 20120514--Frank
	 * 为了方便客户开发对于PSUpChar以及PSDownChar 我门加上一个path通道接口用于存取或获取数据路径
	 * path用于下载某个指纹数据存放的路径譬如/data/local/tmp/fingerprintdata这个指纹数据， 建议客户放在/data/local/tmp/目录
	 * 因为权限问题
	 */
	public native int PSDownChar(int nAddr,int iBufferID, int pTemplet, int iTempletLength,String path);
	
	/*
	 * 指纹电源 电源打开关闭
	 */
	public static native int Open();
	public static native int Close();
	
	
}
