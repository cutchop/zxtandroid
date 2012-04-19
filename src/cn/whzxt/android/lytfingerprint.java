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
	
	static
    {
    System.loadLibrary("finger");
    }
}
