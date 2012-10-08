package com.lyt.watchdog;

public class Native {
	//打开设备节点
	public native static int init();
	
	//关闭设备节点，终止喂狗
	public native static int exit(int fd);
	
	//设置超时时间
	public native static int settimeout(int timeout);
	
	//获得当前超时时间
	public native static int gettimeout();
	
	//喂狗
	public native static int keepalive();

		
    static {
   	 System.loadLibrary("watchdog");
   	}
}
