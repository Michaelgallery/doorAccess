package com;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;






import javax.swing.JPanel;







import org.observer.DoorAccessThreadObserver;
import org.wiegand.TestZone.TestShort;

import com.task.SynchronizedMember;
import com.task.TimeService;
import com.util.LogManager;


/**
 * 上位机设置
 * 
 * @author admin
 *
 */
public class DoorAddressServer {		
	private static java.util.logging.Logger log = java.util.logging.Logger.getLogger("SetUpFirstPage");
	private static final long serialVersionUID = 1302545292782086457L;
	// 设置页面
	public static DoorAddressServer page = null;
	// 主div
	public static JPanel mainPanel = null;
	// 签到设置div
	public static JPanel loginJpanel;
	// 检测设置div
	public static JPanel testJpanel;
	
	// 签到设置div
	public static JPanel loginJpanelb;
	// 检测设置div
	public static JPanel testJpanelb;
	// 串口设置div
	public static JPanel portJpanel;
	// 帮助文档div
	public static JPanel helpJpanel;
	
	public static JPanel doorAccessJpanel;
	// 签到本地缓存
	public static Map<String, Object> loginMap = new HashMap<String, Object>();
	
	//存放线程是否退出变量
	public static ConcurrentHashMap<String,Boolean> currentMap = new ConcurrentHashMap<>();
	//存放读卡器机口和刷卡类型
	public static ConcurrentHashMap<String,String> typeMap = new ConcurrentHashMap<>();

	public static int width = 730;
	public static int height = 600;
	public static int contentheight = 570;
	public static int contentwidth = 600;
	public static boolean loginpanleisshow = false;
	public static boolean testpanleisshow = false;
	public static boolean portpanleisshow = true;
	public static boolean helppanleisshow = false;

	// 读卡器监听任务是否在执行
	public static boolean cardThreadIsRun = false;

	// 配置设备的时候防止没有串口卡死
	public static boolean isopenport = false;

	public static void main(String[] args) {
	
	}

	public DoorAddressServer() {
		try{			
			TimeService.addTimer(new SynchronizedMember(), 1000*60*60*4);
			//开启门禁线程
			TestShort doorAccess = new TestShort();
			DoorAccessThreadObserver ob = new DoorAccessThreadObserver();
			doorAccess.addObserver(ob);
			Thread th = new Thread(doorAccess,"doorAccessThread");
			th.start();
		}catch(Exception e){
			e.printStackTrace();
			log.info("门禁服务启动异常===========");
		}
	}
	




}
