package org.wiegand.TestZone;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.Observable;
import java.util.Queue;





import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.observer.DoorAccessThreadObserver;
import org.redis.RedisFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wiegand.at8000.WgUdpCommShort;

import redis.clients.jedis.Jedis;
import sun.security.action.GetLongAction;

import com.DoorAddressServer;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.SettableFuture;
import com.task.QueryPersonThread;
import com.task.SignNotifyThreadPoolExecutor;
import com.util.HttpUtil;
import com.util.PropertiesUtil;


public class TestShort extends Observable implements Runnable{
	private static java.util.logging.Logger log = java.util.logging.Logger.getLogger("TestShort");
	private static long controllerSN = 0;
	private static String controllerIP = "";
	private static String watchServerIP = ""; 
	private static int watchServerPort = 0;
	private static NioDatagramAcceptor acceptor = null;


	public TestShort() {
		
	}
	
	
	public static long getControllerSN() {
		return controllerSN;
	}

	public static String getControllerIP() {
		return controllerIP;
	}

	public static String getWatchServerIP() {
		return watchServerIP;
	}


	public static int getWatchServerPort() {
		return watchServerPort;
	}

	/**
	* AT8000_Java zhanpengcheng二次开发 $
	*
	* 门禁控制器 短报文协议 测试案例
	* V2.1 版本  2013-11-09
	*            主要使用 MINA完成
	*            基本功能:  查询控制器状态
	*                       读取日期时间
	*                       设置日期时间
	*                       获取指定索引号的记录
	*                       设置已读取过的记录索引号
	*                       获取已读取过的记录索引号
	*                       远程开门
	*                       权限添加或修改
	*                       权限删除(单个删除)
	*                       权限清空(全部清掉)
	*                       权限总数读取
	*                       权限查询
	*                       设置门控制参数(在线/延时)
	*                       读取门控制参数(在线/延时)

	*                       设置接收服务器的IP和端口
	*                       读取接收服务器的IP和端口
	*
	*
	*                       接收服务器的实现 (在61005端口接收数据) -- 此项功能 一定要注意防火墙设置 必须是允许接收数据的.
	* V2.5 版本  2015-04-30   采用 V6.56驱动版本 型号由0x19改为0x17
	* V2.6 版本  2017-11-01   在接收数据时加入延时
	        long times = 100; 
		    	try {
					 Thread.sleep(times);
				  } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				  }  //2017-11-01 14:45:57 增加延时
	*/
	
	
/*	public TestShort() {
		init();
	}*/
	
	public  void restart(){
		if(true) {
			super.setChanged();
		}
		notifyObservers();
	}
	
	/**
	 * 初始化方法
	 */
	public static void init(){
		String localIp = "";
		 try {
			localIp = HttpUtil.getLocalIP();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 Jedis conn = RedisFactory.getConnection();
		 watchServerIP = PropertiesUtil.getValue("watchServerIP")==null?localIp:PropertiesUtil.getValue("watchServerIP");
		 watchServerPort = Integer.valueOf(PropertiesUtil.getValue("watchServerPort")==null?"61005":PropertiesUtil.getValue("watchServerPort"));
		 controllerSN = Long.valueOf(PropertiesUtil.getValue("controllerSN")==null?"0":PropertiesUtil.getValue("controllerSN"));
		 controllerIP = PropertiesUtil.getValue("controllerIP")==null?"0.0.0.0":PropertiesUtil.getValue("controllerIP");
		 	 if(watchServerIP.equals(localIp)) {		 		 
		 		 watchServerIP = RedisFactory.get("watchServerIP")==null?localIp:conn.get("watchServerIP");
		 	 }
		 	 if("0".equals(controllerSN+"")) {
		 		 controllerSN  = RedisFactory.get("controllerSN")==null?0:Integer.valueOf(conn.get("controllerSN"));
		 		
		 	 }
		 	 if("0.0.0.0".equals(controllerIP)) {		 		 
		 		 controllerIP = RedisFactory.get("controllerIP")==null?"0.0.0.0":conn.get("controllerIP");
		 		
		 	 }
		 	
		 
	}
	
	public static void stopThread() {
		Map<Thread, StackTraceElement[]> allThread = Thread.getAllStackTraces();
		List<Thread> ss = allThread.keySet().stream().collect(Collectors.toList());
		for(Thread th:ss) {
			if("doorAccessThread".equals(th.getName())) {
				DoorAddressServer.currentMap.put(th.getName(), true);
			}
		}
	}
	
	
	public static int changeIp() {
		stopThread();
		if(acceptor!=null) {
			acceptor.unbind();
			acceptor.dispose();
		}
		log("=====================服务器重启中==========================");
	/*	if(fail) {			
			accessObtain = true;
		}*/	
			TestShort doorAccess = new TestShort();
			DoorAccessThreadObserver ob = new DoorAccessThreadObserver();
			doorAccess.addObserver(ob);
			Thread th = new Thread(doorAccess);
			th.start();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(DoorAddressServer.currentMap.get("doorAccessThread")) {
				System.out.println("================服务器启动失败！==========");
				return 0;
			}else{
				System.out.println("============服务器启动成功！===========");
				return 1;
			}
	}
	
	/**
	 * @param args
	 */
	static ThreadPoolExecutor tpe = SignNotifyThreadPoolExecutor.getThreadPoolExecutor();
	public static void main(String[] args) throws IOException {
		
		//本案例 未作 搜索控制器  及 设置IP的工作  (直接由IP设置工具来完成)
		//本案例中测试说明
		//控制器SN  = 229999901
		//控制器IP  = 192.168.168.123
		//电脑  IP  = 192.168.168.101
		//用于作为接收服务器的IP (本电脑IP 192.168.168.101), 接收服务器端口 (61005)
	
	//	private static long controllerSN = 213104291;
	//	private static String controllerIP = "192.168.1.99";
	/*	init();
		int ret =0;
		int NO = 1;
		log("测试开始...");
		System.out.println(controllerIP.equals("192.168.1.199"));
		System.out.println(controllerIP+"========"+controllerSN);
		testBasicFunction("192.168.1.99",controllerSN,NO);//远程开门
*/	/*	if (ret ==0 )
		{
			log("基本功能测试 失败...");
			log("测试结束...");
			return;
		}*/

//		ret = testWatchingServer(controllerIP,controllerSN,watchServerIP, watchServerPort); //接收服务器设置
	/*	init();
		acceptor = new TestShort().WatchingServerRuning(watchServerIP, watchServerPort); //服务器运行....
		test();*/
	/*	TestShort ts = new TestShort();
		DoorAccessThreadObserver ob = new DoorAccessThreadObserver();
		ts.addObserver(ob);
		Thread th = new Thread(ts);
		th.start();*/
	/*	try{
			System.out.println("--------------");
		//	throw new ConnectException("----");
			new TestShort().searchVaildateMember("20190725140124305025");
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("================");
			Map<String,String> result = new TestShort().searchValidateMemberLocal("20190725140124305025");
			System.out.println("=================");
		}*/
	//	new TestShort().searchVailDateMemberList();
		
/*		try{
	//	System.setProperty("https.protocols", "TLSv1");
		QueryPersonThread thread = new QueryPersonThread("1427428647","","login");
		tpe.execute(thread);
		}catch(Exception e) {
			e.printStackTrace();
			tpe.shutdown();
		}*/
		TestShort ts = new TestShort();
		ts.searchVailDateMemberList();
	}




	@Override
	public void run() {
		// TODO Auto-generated method stub
	//	SetUpFirstPage.currentMap.put("change",false);
		init();
		try{			
			DoorAddressServer.currentMap.put("doorAccessThread", false);
			testWatchingServer(controllerIP,controllerSN,watchServerIP,watchServerPort);
			acceptor = WatchingServerRuning(watchServerIP, watchServerPort); //服务器运行....
		}catch(Exception e) {
			DoorAddressServer.currentMap.put("doorAccessThread", true);
			e.printStackTrace();
		}
		
	}
	
	
	public static void log(String info) //日志信息
	{
		System.out.println(info);
	}

	public static byte GetHex(int val) //获取Hex值, 主要用于日期时间格式
    {
	    return (byte)((val % 10) + (((val -(val % 10)) / 10)%10) *16);
    }
	
	
    /// <summary>
    /// 显示记录信息
    /// </summary>
    /// <param name="recvBuff"></param>
	public static Map<String,String> displayRecordInformation(byte[] recvBuff)
    {

      //8-11	最后一条记录的索引号
		//(=0表示没有记录)	4	0x00000000
		long recordIndex = WgUdpCommShort.getLongByByte(recvBuff, 8, 4);


		//12	记录类型
		//0=无记录
		//1=刷卡记录
		//2=门磁,按钮, 设备启动, 远程开门记录
		//3=报警记录	1	
		int recordType =WgUdpCommShort.getIntByByte(recvBuff[12]);

		//13	有效性(0 表示不通过, 1表示通过)	1	
		int recordValid = WgUdpCommShort.getIntByByte(recvBuff[13]);

		//14	门号(1,2,3,4)	1	
		int recordDoorNO = WgUdpCommShort.getIntByByte(recvBuff[14]);

		//15	进门/出门(1表示进门, 2表示出门)	1	0x01
		int recordInOrOut = WgUdpCommShort.getIntByByte(recvBuff[15]);

		//16-19	卡号(类型是刷卡记录时)
		//或编号(其他类型记录)	4	
		long  recordCardNO =WgUdpCommShort.getLongByByte(recvBuff, 16, 4);

		
		//20-26	刷卡时间:
		//年月日时分秒 (采用BCD码)见设置时间部分的说明
		String recordTime=  String.format("%02X%02X-%02X-%02X %02X:%02X:%02X", 
			WgUdpCommShort.getIntByByte(recvBuff[20]),
			WgUdpCommShort.getIntByByte(recvBuff[21]),
			WgUdpCommShort.getIntByByte(recvBuff[22]),
			WgUdpCommShort.getIntByByte(recvBuff[23]),
			WgUdpCommShort.getIntByByte(recvBuff[24]),
			WgUdpCommShort.getIntByByte(recvBuff[25]),
			WgUdpCommShort.getIntByByte(recvBuff[26]));

		//2012.12.11 10:49:59	7	
		//27	记录原因代码(可以查 "刷卡记录说明.xls"文件的ReasonNO)
		//处理复杂信息才用	1	
		int reason = WgUdpCommShort.getIntByByte(recvBuff[27]);
		
        //0=无记录
        //1=刷卡记录
        //2=门磁,按钮, 设备启动, 远程开门记录
        //3=报警记录	1	
        //0xFF=表示指定索引位的记录已被覆盖掉了.  请使用索引0, 取回最早一条记录的索引值
		Map<String,String> map = new HashMap<String,String>();
        if (recordType == 0)
        {
            log(String.format("索引位=%u  无记录", recordIndex));
        }
        else if (recordType == 0xff)
        {
            log(" 指定索引位的记录已被覆盖掉了,请使用索引0, 取回最早一条记录的索引值");
        }
        else if (recordType == 1) //2015-06-10 08:49:31 显示记录类型为卡号的数据
        {
            //卡号
            log.info(String.format("索引位=%d  ", recordIndex));
            log.info(String.format("  卡号 = %d", recordCardNO));
            log.info(String.format("  门号 = %d", recordDoorNO));
            log.info(String.format("  进出 = %s", recordInOrOut == 1 ? "进门" : "出门"));
            log.info(String.format("  有效 = %s", recordValid == 1 ? "通过" : "禁止"));
            log.info(String.format("  时间 = %s", recordTime));
            log.info(String.format("  描述 = %s", getReasonDetailChinese(reason)));
            map.put("recordCardNO", String.valueOf(recordCardNO));
            map.put("recordDoorNO",recordDoorNO+"");
            map.put("recordInOrOut", recordInOrOut+"");
    		map.put("recordTime", recordTime);
        }
        else if (recordType == 2)
        {
            //其他处理
            //门磁,按钮, 设备启动, 远程开门记录
            log.info(String.format("索引位=%d  非刷卡记录", recordIndex));
            log.info(String.format("  编号 = %d", recordCardNO));
            log.info(String.format("  门号 = %d", recordDoorNO));
            log.info(String.format("  时间 = %s", recordTime));
            log.info(String.format("  描述 = %s", getReasonDetailChinese(reason)));
        }
        else if (recordType == 3)
        {
            //其他处理
            //报警记录
            log.info(String.format("索引位=%d  报警记录", recordIndex));
            log.info(String.format("  编号 = %d", recordCardNO));
            log.info(String.format("  门号 = %d", recordDoorNO));
            log.info(String.format("  时间 = %s", recordTime));
            log.info(String.format("  描述 = %s", getReasonDetailChinese(reason)));
        }
        
        
        
    	int[] doorStatus=new int[4];
		//28	1号门门磁(0表示关上, 1表示打开)	1	0x00
		doorStatus[1-1] = WgUdpCommShort.getIntByByte(recvBuff[28]);
		//29	2号门门磁(0表示关上, 1表示打开)	1	0x00
		doorStatus[2-1] = WgUdpCommShort.getIntByByte(recvBuff[29]);
		//30	3号门门磁(0表示关上, 1表示打开)	1	0x00
		doorStatus[3-1] = WgUdpCommShort.getIntByByte(recvBuff[30]);
		//31	4号门门磁(0表示关上, 1表示打开)	1	0x00
		doorStatus[4-1] = WgUdpCommShort.getIntByByte(recvBuff[31]);

		int[] pbStatus= new int[4];
		//32	1号门按钮(0表示松开, 1表示按下)	1	0x00
		pbStatus[1-1] = WgUdpCommShort.getIntByByte(recvBuff[32]);
		//33	2号门按钮(0表示松开, 1表示按下)	1	0x00
		pbStatus[2-1] = WgUdpCommShort.getIntByByte(recvBuff[33]);
		//34	3号门按钮(0表示松开, 1表示按下)	1	0x00
		pbStatus[3-1] = WgUdpCommShort.getIntByByte(recvBuff[34]);
		//35	4号门按钮(0表示松开, 1表示按下)	1	0x00
		pbStatus[4-1] = WgUdpCommShort.getIntByByte(recvBuff[35]);
		//36	故障号
		//等于0 无故障
		//不等于0, 有故障(先重设时间, 如果还有问题, 则要返厂家维护)	1	
		int errCode = WgUdpCommShort.getIntByByte(recvBuff[36]);
		//37	控制器当前时间
		//时	1	0x21
		//38	分	1	0x30
		//39	秒	1	0x58

		//40-43	流水号	4	
		long   sequenceId= WgUdpCommShort.getLongByByte(recvBuff, 40, 4);

		//48
		//特殊信息1(依据实际使用中返回)
		//键盘按键信息	1	
		//49	继电器状态	1	
		int relayStatus = WgUdpCommShort.getIntByByte(recvBuff[49]);
		//50	门磁状态的8-15bit位[火警/强制锁门]
		//Bit0  强制锁门
		//Bit1  火警		
		int otherInputStatus = WgUdpCommShort.getIntByByte(recvBuff[50]);
		if ((otherInputStatus & 0x1) > 0)
		{
			//强制锁门
		}
		if ((otherInputStatus & 0x2) > 0)
		{
			//火警
		}

		//51	V5.46版本支持 控制器当前年	1	0x13
		//52	V5.46版本支持 月	1	0x06
		//53	V5.46版本支持 日	1	0x22

		String controllerTime; //控制器当前时间
		controllerTime= String.format("20%02X-%02X-%02X %02X:%02X:%02X", 
			WgUdpCommShort.getIntByByte(recvBuff[51]),
			WgUdpCommShort.getIntByByte(recvBuff[52]),
			WgUdpCommShort.getIntByByte(recvBuff[53]),
			WgUdpCommShort.getIntByByte(recvBuff[37]),
			WgUdpCommShort.getIntByByte(recvBuff[38]),
			WgUdpCommShort.getIntByByte(recvBuff[39]));
		
		
		return map;
		
		
        
    }

	public static  String RecordDetails[] =
    {
//记录原因 (类型中 SwipePass 表示通过; SwipeNOPass表示禁止通过; ValidEvent 有效事件(如按钮 门磁 超级密码开门); Warn 报警事件)
//代码  类型   英文描述  中文描述
"1","SwipePass","Swipe","刷卡开门",
"2","SwipePass","Swipe Close","刷卡关",
"3","SwipePass","Swipe Open","刷卡开",
"4","SwipePass","Swipe Limited Times","刷卡开门(带限次)",
"5","SwipeNOPass","Denied Access: PC Control","刷卡禁止通过: 电脑控制",
"6","SwipeNOPass","Denied Access: No PRIVILEGE","刷卡禁止通过: 没有权限",
"7","SwipeNOPass","Denied Access: Wrong PASSWORD","刷卡禁止通过: 密码不对",
"8","SwipeNOPass","Denied Access: AntiBack","刷卡禁止通过: 反潜回",
"9","SwipeNOPass","Denied Access: More Cards","刷卡禁止通过: 多卡",
"10","SwipeNOPass","Denied Access: First Card Open","刷卡禁止通过: 首卡",
"11","SwipeNOPass","Denied Access: Door Set NC","刷卡禁止通过: 门为常闭",
"12","SwipeNOPass","Denied Access: InterLock","刷卡禁止通过: 互锁",
"13","SwipeNOPass","Denied Access: Limited Times","刷卡禁止通过: 受刷卡次数限制",
"14","SwipeNOPass","Denied Access: Limited Person Indoor","刷卡禁止通过: 门内人数限制",
"15","SwipeNOPass","Denied Access: Invalid Timezone","刷卡禁止通过: 卡过期或不在有效时段",
"16","SwipeNOPass","Denied Access: In Order","刷卡禁止通过: 按顺序进出限制",
"17","SwipeNOPass","Denied Access: SWIPE GAP LIMIT","刷卡禁止通过: 刷卡间隔约束",
"18","SwipeNOPass","Denied Access","刷卡禁止通过: 原因不明",
"19","SwipeNOPass","Denied Access: Limited Times","刷卡禁止通过: 刷卡次数限制",
"20","ValidEvent","Push Button","按钮开门",
"21","ValidEvent","Push Button Open","按钮开",
"22","ValidEvent","Push Button Close","按钮关",
"23","ValidEvent","Door Open","门打开[门磁信号]",
"24","ValidEvent","Door Closed","门关闭[门磁信号]",
"25","ValidEvent","Super Password Open Door","超级密码开门",
"26","ValidEvent","Super Password Open","超级密码开",
"27","ValidEvent","Super Password Close","超级密码关",
"28","Warn","Controller Power On","控制器上电",
"29","Warn","Controller Reset","控制器复位",
"30","Warn","Push Button Invalid: Disable","按钮不开门: 按钮禁用",
"31","Warn","Push Button Invalid: Forced Lock","按钮不开门: 强制关门",
"32","Warn","Push Button Invalid: Not On Line","按钮不开门: 门不在线",
"33","Warn","Push Button Invalid: InterLock","按钮不开门: 互锁",
"34","Warn","Threat","胁迫报警",
"35","Warn","Threat Open","胁迫报警开",
"36","Warn","Threat Close","胁迫报警关",
"37","Warn","Open too long","门长时间未关报警[合法开门后]",
"38","Warn","Forced Open","强行闯入报警",
"39","Warn","Fire","火警",
"40","Warn","Forced Close","强制关门",
"41","Warn","Guard Against Theft","防盗报警",
"42","Warn","7*24Hour Zone","烟雾煤气温度报警",
"43","Warn","Emergency Call","紧急呼救报警",
"44","RemoteOpen","Remote Open Door","操作员远程开门",
"45","RemoteOpen","Remote Open Door By USB Reader","发卡器确定发出的远程开门"
    };

    public static   String getReasonDetailChinese(int Reason) //中文
    {
        if (Reason > 45)
        {
            return "";
        }
        if (Reason <= 0)
        {
            return "";
        }
        return RecordDetails[(Reason - 1) * 4 + 3]; //中文信息
    }

    public static String getReasonDetailEnglish(int Reason) //英文描述
    {
        if (Reason > 45)
        {
            return "";
        }
        if (Reason <= 0)
        {
            return "";
        }
        return RecordDetails[(Reason - 1) * 4 + 2]; //英文信息
    }
    
	@SuppressWarnings("unused")
	public static int 	 testBasicFunction(String controllerIP, long controllerSN,int NO)  //基本功能测试
	{
		byte[] recvBuff;
		int success =0;
		WgUdpCommShort pkt = new WgUdpCommShort();
		pkt.iDevSn = controllerSN;
		
		
		log(String.format("控制器SN = %d \r\n", controllerSN));
		
		//打开udp连接
		pkt.CommOpen(controllerIP);
		
		//1.4	查询控制器状态[功能号: 0x20](实时监控用) **********************************************************************************
	/*	pkt.Reset();
		pkt.functionID = (byte) 0x20;
		pkt.iDevSn = controllerSN; 
		recvBuff = pkt.run();

		success =0;
		if (recvBuff != null)
		{
			//读取信息成功...
			success =1;
			log("1.4 查询控制器状态 成功...");

			//	  	最后一条记录的信息		
			displayRecordInformation(recvBuff); 

			//	其他信息		
			int[] doorStatus=new int[4];
			//28	1号门门磁(0表示关上, 1表示打开)	1	0x00
			doorStatus[1-1] = WgUdpCommShort.getIntByByte(recvBuff[28]);
			//29	2号门门磁(0表示关上, 1表示打开)	1	0x00
			doorStatus[2-1] = WgUdpCommShort.getIntByByte(recvBuff[29]);
			//30	3号门门磁(0表示关上, 1表示打开)	1	0x00
			doorStatus[3-1] = WgUdpCommShort.getIntByByte(recvBuff[30]);
			//31	4号门门磁(0表示关上, 1表示打开)	1	0x00
			doorStatus[4-1] = WgUdpCommShort.getIntByByte(recvBuff[31]);

			int[] pbStatus= new int[4];
			//32	1号门按钮(0表示松开, 1表示按下)	1	0x00
			pbStatus[1-1] = WgUdpCommShort.getIntByByte(recvBuff[32]);
			//33	2号门按钮(0表示松开, 1表示按下)	1	0x00
			pbStatus[2-1] = WgUdpCommShort.getIntByByte(recvBuff[33]);
			//34	3号门按钮(0表示松开, 1表示按下)	1	0x00
			pbStatus[3-1] = WgUdpCommShort.getIntByByte(recvBuff[34]);
			//35	4号门按钮(0表示松开, 1表示按下)	1	0x00
			pbStatus[4-1] = WgUdpCommShort.getIntByByte(recvBuff[35]);
			//36	故障号
			//等于0 无故障
			//不等于0, 有故障(先重设时间, 如果还有问题, 则要返厂家维护)	1	
			int errCode = WgUdpCommShort.getIntByByte(recvBuff[36]);
			//37	控制器当前时间
			//时	1	0x21
			//38	分	1	0x30
			//39	秒	1	0x58

			//40-43	流水号	4	
			long   sequenceId= WgUdpCommShort.getLongByByte(recvBuff, 40, 4);

			//48
			//特殊信息1(依据实际使用中返回)
			//键盘按键信息	1	
			//49	继电器状态	1	
			int relayStatus = WgUdpCommShort.getIntByByte(recvBuff[49]);
			//50	门磁状态的8-15bit位[火警/强制锁门]
			//Bit0  强制锁门
			//Bit1  火警		
			int otherInputStatus = WgUdpCommShort.getIntByByte(recvBuff[50]);
			if ((otherInputStatus & 0x1) > 0)
			{
				//强制锁门
			}
			if ((otherInputStatus & 0x2) > 0)
			{
				//火警
			}

			//51	V5.46版本支持 控制器当前年	1	0x13
			//52	V5.46版本支持 月	1	0x06
			//53	V5.46版本支持 日	1	0x22

			String controllerTime; //控制器当前时间
			controllerTime= String.format("20%02X-%02X-%02X %02X:%02X:%02X", 
				WgUdpCommShort.getIntByByte(recvBuff[51]),
				WgUdpCommShort.getIntByByte(recvBuff[52]),
				WgUdpCommShort.getIntByByte(recvBuff[53]),
				WgUdpCommShort.getIntByByte(recvBuff[37]),
				WgUdpCommShort.getIntByByte(recvBuff[38]),
				WgUdpCommShort.getIntByByte(recvBuff[39]));
		}
		else
		{
			log("1.4 查询控制器状态 失败...");
			return 0;
		}*/



		//1.5	读取日期时间(功能号: 0x32) **********************************************************************************
	/*	pkt.Reset();
		pkt.functionID = (byte) 0x32;
		pkt.iDevSn = controllerSN; 
		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			String controllerTime; //控制器当前时间
			controllerTime= String.format("%02X%02X-%02X-%02X %02X:%02X:%02X", 
				WgUdpCommShort.getIntByByte(recvBuff[8]),WgUdpCommShort.getIntByByte(recvBuff[9]),WgUdpCommShort.getIntByByte(recvBuff[10]),WgUdpCommShort.getIntByByte(recvBuff[11]),WgUdpCommShort.getIntByByte(recvBuff[12]),WgUdpCommShort.getIntByByte(recvBuff[13]),WgUdpCommShort.getIntByByte(recvBuff[14]));

			log("1.5 读取日期时间 成功...");
			//log(controllerTime);
			success =1;
		}

		//1.6	设置日期时间[功能号: 0x30] **********************************************************************************
		//按电脑当前时间校准控制器.....
		pkt.Reset();
		pkt.functionID = (byte) 0x30;
		pkt.iDevSn = controllerSN; 

		Calendar cal = (Calendar.getInstance());
   
		pkt.data[0] =GetHex((int)(( cal.get(Calendar.YEAR) -(cal.get(Calendar.YEAR)%100))/100)); 
		pkt.data[1] =GetHex((int)(( cal.get(Calendar.YEAR))%100)); //st.GetMonth()); 
		pkt.data[2] =GetHex( cal.get(Calendar.MONTH) + 1); 
		pkt.data[3] =GetHex(cal.get(Calendar.DAY_OF_MONTH)); 
		pkt.data[4] =GetHex(cal.get(Calendar.HOUR_OF_DAY)); 
		pkt.data[5] =GetHex(cal.get(Calendar.MINUTE)); 
		pkt.data[6] = GetHex(cal.get(Calendar.SECOND)); 
		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			success =1;
			for(int i=0; i<7; i++)
			{
				if(pkt.data[i] != recvBuff[8+i])
				{
					success = 0;
					break;
				}	
			}
			if (success >0)
			{
				log("1.6 设置日期时间 成功...");
			}
		}*/

		//1.7	获取指定索引号的记录[功能号: 0xB0] **********************************************************************************
		//(取索引号 0x00000001的记录)
		/*int  recordIndexToGet =0;
		pkt.Reset();
		pkt.functionID =(byte) 0xB0;
		pkt.iDevSn = controllerSN; 

		//	(特殊
		//如果=0, 则取回最早一条记录信息
		//如果=0xffffffff则取回最后一条记录的信息)
		//记录索引号正常情况下是顺序递增的, 最大可达0xffffff = 16,777,215 (超过1千万) . 由于存储空间有限, 控制器上只会保留最近的20万个记录. 当索引号超过20万后, 旧的索引号位的记录就会被覆盖, 所以这时查询这些索引号的记录, 返回的记录类型将是0xff, 表示不存在了.
		recordIndexToGet =1;
 	    System.arraycopy(WgUdpCommShort.longToByte(recordIndexToGet) , 0, pkt.data, 0, 4);
		

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			log("1.7 获取索引为1号记录的信息	 成功...");
			//	  	索引为1号记录的信息		
			displayRecordInformation(recvBuff); 

			success =1;
		}

		//. 发出报文 (取最早的一条记录 通过索引号 0x00000000) [此指令适合于 刷卡记录超过20万时环境下使用]
		pkt.Reset();
		pkt.functionID = (byte) 0xB0;
		pkt.iDevSn = controllerSN; 
		recordIndexToGet =0;
 	    System.arraycopy(WgUdpCommShort.longToByte(recordIndexToGet) , 0, pkt.data, 0, 4);

 	    recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			log("1.7 获取最早一条记录的信息	 成功...");
			//	  	最早一条记录的信息		
			displayRecordInformation(recvBuff); 
			success =1;
		}

		//发出报文 (取最新的一条记录 通过索引 0xffffffff)
		pkt.Reset();
		pkt.functionID = (byte) 0xB0;
		pkt.iDevSn = controllerSN; 
		recordIndexToGet =0xffffffff;
 	    System.arraycopy(WgUdpCommShort.longToByte(recordIndexToGet) , 0, pkt.data, 0, 4);

 	    recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			log("1.7 获取最新记录的信息	 成功...");
			//	  	最新记录的信息		
			displayRecordInformation(recvBuff); 

			success =1;
		}*/

//		//1.8	设置已读取过的记录索引号[功能号: 0xB2] **********************************************************************************
//		pkt.Reset();
//		pkt.functionID = (byte) 0xB2;
//		pkt.iDevSn = controllerSN; 
//		// (设为已读取过的记录索引号为5)
//		long recordIndexGotToSet = 0x5;
// 	    System.arraycopy(WgUdpCommShort.longToByte(recordIndexGotToSet) , 0, pkt.data, 0, 4);
//
//		//12	标识(防止误设置)	1	0x55 [固定]
// 	    System.arraycopy(WgUdpCommShort.longToByte(WgUdpCommShort.SpecialFlag) , 0, pkt.data, 4, 4);
//
//		recvBuff = pkt.run();
//		success =0;
//		if (recvBuff != null)
//		{
//			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
//			{
//				log("1.8 设置已读取过的记录索引号	 成功...");
//				success =1;
//			}
//		}
//
//		//1.9	获取已读取过的记录索引号[功能号: 0xB4] **********************************************************************************
//		pkt.Reset();
//		pkt.functionID = (byte) 0xB4;
//		pkt.iDevSn = controllerSN; 
//		long recordIndexGotToRead =0x0;
//		recvBuff = pkt.run();
//		success =0;
//		if (recvBuff != null)
//		{
//			recordIndexGotToRead = WgUdpCommShort.getLongByByte(recvBuff, 8, 4);
//			log("1.9 获取已读取过的记录索引号	 成功...");
//			success =1;
//		}

		//1.9	提取记录操作
		//1. 通过 0xB4指令 获取已读取过的记录索引号 recordIndex
		//2. 通过 0xB0指令 获取指定索引号的记录  从recordIndex + 1开始提取记录， 直到记录为空为止
		//3. 通过 0xB2指令 设置已读取过的记录索引号  设置的值为最后读取到的刷卡记录索引号
		//经过上面三个步骤， 整个提取记录的操作完成
/*	    log("1.9 提取记录操作	 开始...");
		pkt.Reset();
		pkt.functionID = (byte) 0xB4;
		pkt.iDevSn = controllerSN; 
		long recordIndexGot4GetSwipe =0x0;
		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			recordIndexGot4GetSwipe= WgUdpCommShort.getLongByByte(recvBuff, 8, 4);
			pkt.Reset();
			pkt.functionID = (byte) 0xB0;
			pkt.iDevSn = controllerSN; 
			long recordIndexToGetStart = recordIndexGot4GetSwipe + 1;
			long recordIndexValidGet = 0;
			int cnt=0;
			do
			{
				System.arraycopy(WgUdpCommShort.longToByte(recordIndexToGetStart) , 0, pkt.data, 0, 4);
				recvBuff = pkt.run();
				success =0;
				if (recvBuff != null)
				{
					success =1;

					//12	记录类型
					//0=无记录
					//1=刷卡记录
					//2=门磁,按钮, 设备启动, 远程开门记录
					//3=报警记录	1	
					//0xFF=表示指定索引位的记录已被覆盖掉了.  请使用索引0, 取回最早一条记录的索引值
					int recordType = WgUdpCommShort.getIntByByte(recvBuff[12]);
					if (recordType == 0)
					{
						break; //没有更多记录
					}
					
					if (recordType == 0xff)
					{
						success = 0;   //此索引号无效  重新设置索引值
						//取最早一条记录的索引位
						recordIndexToGet =0;
				 	    System.arraycopy(WgUdpCommShort.longToByte(recordIndexToGet) , 0, pkt.data, 0, 4);

				 	    recvBuff = pkt.run();
						success =0;
						if (recvBuff != null)
						{
							log("1.7 获取最早一条记录的信息	 成功...");
							//	  	最早一条记录的信息		
							
							success =1;
							long recordIndex =0;
			                recordIndex  = WgUdpCommShort.getLongByByte(recvBuff,8, 4);
	                        recordIndexToGetStart = recordIndex;
	                        continue;
						}
						
						
	                    success = 0;  
						break; 
					}
					recordIndexValidGet = recordIndexToGetStart;
					//.......对收到的记录作存储处理
					 displayRecordInformation(recvBuff);
					//*****
					//###############
				}
				else
				{
					//提取失败
					break;
				}
				recordIndexToGetStart++;
			}while(cnt++ < 200000);
			if (success >0)
			{
				//通过 0xB2指令 设置已读取过的记录索引号  设置的值为最后读取到的刷卡记录索引号
				pkt.Reset();
				pkt.functionID = (byte) 0xB2;
				pkt.iDevSn = controllerSN; 
				System.arraycopy(WgUdpCommShort.longToByte(recordIndexValidGet) , 0, pkt.data, 0, 4);
				
				//12	标识(防止误设置)	1	0x55 [固定]
		 	    System.arraycopy(WgUdpCommShort.longToByte(WgUdpCommShort.SpecialFlag) , 0, pkt.data, 4, 4);

				recvBuff = pkt.run();
				success =0;
				if (recvBuff != null)
				{
					if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
					{
						//完全提取成功....
						log("1.9 完全提取成功	 成功...");
						success =1;
					}
				}

			}
		}*/

		//1.10	远程开门[功能号: 0x40] **********************************************************************************
		int doorNO =NO;
		pkt.Reset();
		pkt.functionID = (byte) 0x40;
		pkt.iDevSn = controllerSN; 
		pkt.data[0] =(byte) (doorNO & 0xff); //2013-11-03 20:56:33
		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
			{
				//有效开门.....
				log("1.10 远程开门	 成功...");
				success =1;
			}
		}

		//1.11	权限添加或修改[功能号: 0x50] **********************************************************************************
		//增加卡号0D D7 37 00, 通过当前控制器的所有门
	/*	pkt.Reset();
		pkt.functionID = (byte) 0x50;
		pkt.iDevSn = controllerSN; 
		//0D D7 37 00 要添加或修改的权限中的卡号 = 0x0037D70D = 3659533 (十进制)
		long cardNOOfPrivilege =0x0037D70D;
		//memcpy(&(pkt.data[0]), &cardNOOfPrivilege, 4);
		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilege) , 0, pkt.data, 0, 4);
		//20 10 01 01 起始日期:  2010年01月01日   (必须大于2001年)
		pkt.data[4] = 0x20;
		pkt.data[5] = 0x10;
		pkt.data[6] = 0x01;
		pkt.data[7] = 0x01;
		//20 29 12 31 截止日期:  2029年12月31日
		pkt.data[8] = 0x20;
		pkt.data[9] = 0x29;
		pkt.data[10] = 0x12;
		pkt.data[11] = 0x31;
		//01 允许通过 一号门 [对单门, 双门, 四门控制器有效] 
		pkt.data[12] = 0x01;
		//01 允许通过 二号门 [对双门, 四门控制器有效]
		pkt.data[13] = 0x01;  //如果禁止2号门, 则只要设为 0x00
		//01 允许通过 三号门 [对四门控制器有效]
		pkt.data[14] = 0x01;
		//01 允许通过 四号门 [对四门控制器有效]
		pkt.data[15] = 0x01;

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
			{
				//这时 刷卡号为= 0x0037D70D = 3659533 (十进制)的卡, 1号门继电器动作.
				log("1.11 权限添加或修改	 成功...");
				success =1;
			}
		}

		//1.12	权限删除(单个删除)[功能号: 0x52] **********************************************************************************
		pkt.Reset();
		pkt.functionID = (byte) 0x52;
		pkt.iDevSn = controllerSN; 
		//要删除的权限卡号0D D7 37 00  = 0x0037D70D = 3659533 (十进制)
		long cardNOOfPrivilegeToDelete =0x0037D70D;
		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilegeToDelete) , 0, pkt.data, 0, 4);

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
			{
				//这时 刷卡号为= 0x0037D70D = 3659533 (十进制)的卡, 1号门继电器不会动作.
				log("1.12 权限删除(单个删除)	 成功...");
				success =1;
			}
		}

		//1.13	权限清空(全部清掉)[功能号: 0x54] **********************************************************************************
		pkt.Reset();
		pkt.functionID = (byte) 0x54;
		pkt.iDevSn = controllerSN; 
		//12	标识(防止误设置)	1	0x55 [固定]
 	    System.arraycopy(WgUdpCommShort.longToByte(WgUdpCommShort.SpecialFlag) , 0, pkt.data, 0, 4);

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
			{
				//这时清空成功
				log("1.13 权限清空(全部清掉)	 成功...");
				success =1;
			}
		}*/

		//1.14	权限总数读取[功能号: 0x58] **********************************************************************************
/*		pkt.Reset();
		pkt.functionID = (byte) 0x58;
		pkt.iDevSn = controllerSN; 
		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			long privilegeCount  = WgUdpCommShort.getLongByByte(recvBuff,8, 4);
			log("1.14 权限总数读取	 成功...");

			success =1;
		}

		
		//再次添加为查询操作 1.11	权限添加或修改[功能号: 0x50] **********************************************************************************
		//增加卡号0D D7 37 00, 通过当前控制器的所有门
		pkt.Reset();
		pkt.functionID = (byte) 0x50;
		pkt.iDevSn = controllerSN; 
		//0D D7 37 00 要添加或修改的权限中的卡号 = 0x0037D70D = 3659533 (十进制)
		//long 
		cardNOOfPrivilege =0x0037D70D;
		
		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilege) , 0, pkt.data, 0, 4);
		//20 10 01 01 起始日期:  2010年01月01日   (必须大于2001年)
		pkt.data[4] = 0x20;
		pkt.data[5] = 0x10;
		pkt.data[6] = 0x01;
		pkt.data[7] = 0x01;
		//20 29 12 31 截止日期:  2029年12月31日
		pkt.data[8] = 0x20;
		pkt.data[9] = 0x29;
		pkt.data[10] = 0x12;
		pkt.data[11] = 0x31;
		//01 允许通过 一号门 [对单门, 双门, 四门控制器有效] 
		pkt.data[12] = 0x01;
		//01 允许通过 二号门 [对双门, 四门控制器有效]
		pkt.data[13] = 0x01;  //如果禁止2号门, 则只要设为 0x00
		//01 允许通过 三号门 [对四门控制器有效]
		pkt.data[14] = 0x01;
		//01 允许通过 四号门 [对四门控制器有效]
		pkt.data[15] = 0x01;

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
			{
				//这时 刷卡号为= 0x0037D70D = 3659533 (十进制)的卡, 1号门继电器动作.
				log("1.11 权限添加或修改	 成功...");
				success =1;
			}
		}*/
		
		//1.15	权限查询[功能号: 0x5A] **********************************************************************************
/*		pkt.Reset();
		pkt.functionID = (byte) 0x5A;
		pkt.iDevSn = controllerSN; 
		// (查卡号为 0D D7 37 00的权限)
		long cardNOOfPrivilegeToQuery =0x0037D70D;
		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilegeToQuery) , 0, pkt.data, 0, 4);

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{

			long cardNOOfPrivilegeToGet = WgUdpCommShort.getLongByByte(recvBuff,8, 4);
			if (cardNOOfPrivilegeToGet == 0)
			{
				//没有权限时: (卡号部分为0)
				log ("1.15      没有权限信息: (卡号部分为0)");
			}
			else
			{
				//具体权限信息...
				log ("1.15     有权限信息...");

			}
			log("1.15 权限查询	 成功...");
			success =1;
		}
		
		//1.16  获取指定索引号的权限[功能号: 0x5C] **********************************************************************************
		pkt.Reset();
		pkt.functionID = (byte) 0x5C;
		pkt.iDevSn = controllerSN; 
		
		cardNOOfPrivilegeToQuery =1;  //索引号(从1开始)
		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilegeToQuery) , 0, pkt.data, 0, 4);

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{

			long cardNOOfPrivilegeToGet = WgUdpCommShort.getLongByByte(recvBuff,8, 4);
			if (cardNOOfPrivilegeToGet == 4294967295l) //'FFFFFFFF对应于4294967295
			{
				//没有权限时: (卡号部分为0)
				log ("1.16      没有权限信息: (权限已删除)");
			}
			else if (cardNOOfPrivilegeToGet == 0)
			{
				//没有权限时: (卡号部分为0)
				log ("1.16       没有权限信息: (卡号部分为0)--此索引号之后没有权限了");
			}
			else
			{
				//具体权限信息...
				log ("1.16     有权限信息...");

			}
			log("1.15 权限查询	 成功...");
			success =1;
		}*/
		


		//1.17	设置门控制参数(在线/延时) [功能号: 0x80] **********************************************************************************
/*		pkt.Reset();
		pkt.functionID = (byte) 0x80;
		pkt.iDevSn = controllerSN; 
		//(设置2号门 在线  开门延时 3秒)
		pkt.data[0] = 0x02; //2号门
		pkt.data[1] = 0x03; //在线
		pkt.data[2] = 0x03; //开门延时

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			success =1;		
			for(int i=0; i<3; i++)
			{
				if (pkt.data[i] != recvBuff[8+i])
				{
				  success = 0;
				   break;
				}
			}
			if (success > 0)
			{
				//成功时, 返回值与设置一致
				log("1.17 设置门控制参数	 成功...");
				success =1;			
			}
		}
		*/
		
		//1.21	权限按从小到大顺序添加[功能号: 0x56] 适用于权限数过1000, 少于8万 **********************************************************************************
        //此功能实现 完全更新全部权限, 用户不用清空之前的权限. 只是将上传的权限顺序从第1个依次到最后一个上传完成. 如果中途中断的话, 仍以原权限为主
        //建议权限数更新超过50个, 即可使用此指令

       /* log("1.21	权限按从小到大顺序添加[功能号: 0x56]	开始...");
        log("       1万条权限...");*/

        //以10000个卡号为例, 此处简化的排序, 直接是以50001开始的10000个卡. 用户按照需要将要上传的卡号排序存放
      /*  int cardCount = 10000;  //2015-06-09 20:20:20 卡总数量
        long cardArray[]= new long[10000];
        for (int i = 0; i < cardCount; i++)
        {
            cardArray[i] = 50001+i;
        }

        for (int i = 0; i < cardCount; i++)
        {
        	pkt.Reset();
    		pkt.functionID = (byte) 0x56;
    		pkt.iDevSn = controllerSN; 
    		
    		cardNOOfPrivilege =cardArray[i];
    		
    		System.arraycopy(WgUdpCommShort.longToByte(cardNOOfPrivilege) , 0, pkt.data, 0, 4);
    		//20 10 01 01 起始日期:  2010年01月01日   (必须大于2001年)
    		pkt.data[4] = 0x20;
    		pkt.data[5] = 0x10;
    		pkt.data[6] = 0x01;
    		pkt.data[7] = 0x01;
    		//20 29 12 31 截止日期:  2029年12月31日
    		pkt.data[8] = 0x20;
    		pkt.data[9] = 0x29;
    		pkt.data[10] = 0x12;
    		pkt.data[11] = 0x31;
    		//01 允许通过 一号门 [对单门, 双门, 四门控制器有效] 
    		pkt.data[12] = 0x01;
    		//01 允许通过 二号门 [对双门, 四门控制器有效]
    		pkt.data[13] = 0x01;  //如果禁止2号门, 则只要设为 0x00
    		//01 允许通过 三号门 [对四门控制器有效]
    		pkt.data[14] = 0x01;
    		//01 允许通过 四号门 [对四门控制器有效]
    		pkt.data[15] = 0x01;

    		
    		System.arraycopy(WgUdpCommShort.longToByte(cardCount) , 0, pkt.data, 32-8, 4);//总的权限数
			int i2=i+1;
			System.arraycopy(WgUdpCommShort.longToByte(i2) , 0, pkt.data, 35-8, 4);//当前权限的索引位(从1开始)

    		recvBuff = pkt.run();
    		success =0;
    		if (recvBuff != null)
    		{
    			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 1)
    			{
    				success =1;
    			}
    			if (WgUdpCommShort.getIntByByte(recvBuff[8]) == 0xE1)
                {
                    log("1.21	权限按从小到大顺序添加[功能号: 0x56]	 =0xE1 表示卡号没有从小到大排序...???");
                    success = 0;
                    break;
                }
    		}
            else
            {
                break;
            }
        }
        if (success == 1)
        {
            log("1.21	权限按从小到大顺序添加[功能号: 0x56]	 成功...");
        }
        else
        {
            log("1.21	权限按从小到大顺序添加[功能号: 0x56]	 失败...????");
        }*/

		//其他指令  **********************************************************************************

		
		//结束  **********************************************************************************

		//关闭udp连接
		pkt.CommClose();
		
		return success;
	}

	
	//controllerIP 被设置的控制器IP地址
	//controllerSN 被设置的控制器序列号
	//watchServerIP   要设置的服务器IP
	//watchServerPort 要设置的端口
	@SuppressWarnings("unused")
	public  int testWatchingServer(String controllerIP, long controllerSN, String watchServerIP,int watchServerPort)  //接收服务器测试 -- 设置
	{
		System.out.println("==========进入控制器===========");
		byte[] recvBuff;
		int success =0;  //0 失败, 1表示成功
		WgUdpCommShort pkt = new WgUdpCommShort();
		pkt.iDevSn = controllerSN;
		
		//打开udp连接
		pkt.CommOpen(controllerIP);
		
		//1.18	设置接收服务器的IP和端口 [功能号: 0x90] **********************************************************************************
		//	接收服务器的IP: 192.168.168.101  [当前电脑IP]
		//(如果不想让控制器发出数据, 只要将接收服务器的IP设为0.0.0.0 就行了)
		//接收服务器的端口: 61005
		//每隔5秒发送一次: 05
		pkt.Reset();
		pkt.functionID = (byte)0x90;
		pkt.iDevSn = controllerSN;


			
		//服务器IP: 192.168.168.101
		//pkt.data[0] = 192; 
		//pkt.data[1] = 168; 
		//pkt.data[2] = 168; 
		//pkt.data[3] = 101; 
		String[] ip;
        ip= watchServerIP.split("\\.");
        if (ip.length == 4)
        {
			pkt.data[0] =  (byte)Integer.parseInt(ip[0]);   
			pkt.data[1] =  (byte)Integer.parseInt(ip[1]);   
			pkt.data[2] =  (byte)Integer.parseInt(ip[2]);  
			pkt.data[3] =  (byte)Integer.parseInt(ip[3]); 
        }

		//接收服务器的端口: 61005
		pkt.data[4] =(byte)(watchServerPort & 0xff);
		pkt.data[5] =(byte)((watchServerPort >>8) & 0xff);

		//每隔5秒发送一次: 05 (定时上传信息的周期为5秒 [正常运行时每隔5秒发送一次  有刷卡时立即发送])
		pkt.data[6] = 5;

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			if (recvBuff[8] == 1)
			{
				log.info("1.18 设置接收服务器的IP和端口 	 成功...");
				success =1;
			}
		}


		//1.19	读取接收服务器的IP和端口 [功能号: 0x92] **********************************************************************************
		pkt.Reset();
		pkt.functionID = (byte) 0x92;
		pkt.iDevSn = controllerSN; 

		recvBuff = pkt.run();
		success =0;
		if (recvBuff != null)
		{
			log.info("1.19 读取接收服务器的IP和端口 	 成功...");
			success =1;
		}
		
		//关闭udp连接
		pkt.CommClose();
		return 1;
	}
	
	
	
	//public static Queue<byte[]> queue = new LinkedList<byte[]>();
	public  NioDatagramAcceptor WatchingServerRuning(String watchServerIP,int watchServerPort) throws Exception // 进入服务器监控状态
	{
		Queue<byte[]> queue = new LinkedList<byte[]>();
		
		 // 创建UDP数据包NIO
        NioDatagramAcceptor acceptor = new NioDatagramAcceptor();
        // NIO设置底层IOHandler
        acceptor.setHandler(new WatchingShortHandler(queue));

        // 设置是否重用地址？ 也就是每个发过来的udp信息都是一个地址？
        DatagramSessionConfig dcfg = acceptor.getSessionConfig();
        dcfg.setReuseAddress(true);

        // 绑定端口地址
       acceptor.bind(new InetSocketAddress(watchServerIP,watchServerPort));
      log.info("进入接收服务器监控状态....[如果在win7下使用 一定要注意防火墙设置]");       
      long recordIndex = 0;
      System.out.println(queue.isEmpty());
      SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     long expirationTime = System.currentTimeMillis();
/*        while(true) {
    	  long now = System.currentTimeMillis();
    	  if(now-expirationTime>1000*60*3) {
    		  break;
    	  }
    	  try{
    		  Thread.sleep(1000);}catch(Exception e){}
    	  if(!queue.isEmpty()) {
    		  break;
    	  }
      }
      System.out.println(queue.isEmpty());
	  if (!queue.isEmpty())
	    {
	         byte[] recvBuff1;
	         synchronized (queue)
	         {
	        	 recvBuff1= queue.poll();
	         }
	         if (recvBuff1[1]== 0x20)
					{
						long sn = WgUdpCommShort.getLongByByte(recvBuff1, 4, 4);
						long recordIndexGet = WgUdpCommShort.getLongByByte(recvBuff1, 8, 4);
						if (recordIndex < recordIndexGet)
						{
							recordIndex = recordIndexGet;
						}
					}
	    }*/
      ThreadPoolExecutor tpe = SignNotifyThreadPoolExecutor.getThreadPoolExecutor();
	  while(true)
	  {
	
		  if(DoorAddressServer.currentMap.get("doorAccessThread")) {
			 // accessObtain = false;
			  log.info("【while 循环被终止了】");
			  break;
		  }
	   try{
		   	/*  long curr = System.currentTimeMillis();
	    	  if(curr-expirationTime>1000*15) {
	    		  throw new Exception("=====内存溢出=====");
	    	  }*/
		  if (!queue.isEmpty())
		    {
		         byte[] recvBuff;
		         synchronized (queue)
		         {
		        	 recvBuff= queue.poll();
		         }
		         if (recvBuff[1]== 0x20)
						{
							long sn = WgUdpCommShort.getLongByByte(recvBuff, 4, 4);
							long recordIndexGet = WgUdpCommShort.getLongByByte(recvBuff, 8, 4);

							if (recordIndex < recordIndexGet)
							{	
								recordIndex = recordIndexGet;
								Map<String,String> map = displayRecordInformation(recvBuff);
								if(map.size()>0) {									
									String recordTime = map.get("recordTime");
									String recordCard = map.get("recordCardNO");
									String recordCardNO = ""+recordCard;
									while (true) {
										if (recordCardNO.length() >= 10) {
											break;
										}
										recordCardNO = "0" + recordCardNO;
									}
									log.info("【门禁刷卡卡号为："+recordCardNO+"】========");
									int NO = Integer.valueOf(map.get("recordDoorNO"));
									int recordInOrOut = Integer.valueOf(map.get("recordInOrOut"));
									long now = new Date().getTime();
									long recent  = 0; 
									try{
										recent = df.parse(recordTime).getTime();
									}catch(Exception e) {	
										e.printStackTrace();
										log.info("【日期转换】");
									}
									double between = new BigDecimal(now - recent).divide(new BigDecimal(1000)).doubleValue();
									if(between<180) {
										if(StringUtils.isNotEmpty(recordCardNO)) {
											JSONObject obj = new JSONObject();
											try{
												obj = searchVaildateMember(recordCardNO);
											}catch(Exception e) {
												log.info("================连接不上远程云服务器================");
												Map<String,String> result = searchValidateMemberLocal(recordCardNO);
												obj.put("member_card", result.get("memberCard"));
												obj.put("name", result.get("name"));
												log.info("=============查询本地服务器数据============="+obj);
												e.printStackTrace();
											}
											String memberCard = "";
											if(obj!=null&&obj.containsKey("member_card")) {												
												memberCard = obj.getString("member_card");
											}
											log.info("门禁读卡器刷卡卡号："+recordCardNO+"======数据查询会员卡号："+memberCard);
											if(recordCardNO.equals(memberCard)){//如果是有效会员		
												log.info("--------远程开门---------------"+controllerIP+"-----"+controllerSN);
												testBasicFunction(controllerIP,controllerSN,NO);//远程开门
												if(recordInOrOut == 1) { //进门操作，进行签到
													QueryPersonThread thread = new QueryPersonThread(recordCardNO,"","login");
													tpe.execute(thread);
													log.info("进门操作，进行签到 卡号："+recordCardNO);
												}
											}
										}
									}
								}
							}
						}
		    }
		    else
		    {
		    		long times = 100; 
					 Thread.sleep(times);
			 //2017-11-01 14:45:57 增加延时
		    }
		  }catch(Exception e) {
			e.printStackTrace();
			tpe.shutdown();
			this.acceptor = acceptor;
			restart();
			return this.acceptor;
		}	     
	  }
// 如果 不是while(true), 则打开下面的注释...	  
	 // acceptor.unbind();
	  //acceptor.dispose();
	  //return 0;
	  return acceptor;
	}
	
	/**
	 * 查询有效会员根据卡号
	 * @return
	 * @throws IOException 
	 */
	public JSONObject searchVaildateMember(String recordCardNO) throws IOException{
		
		String url = PropertiesUtil.getValue("config_url");
		String appKey = PropertiesUtil.getValue("appKey");
		String systemId = PropertiesUtil.getValue("systemId");
		String orgId = PropertiesUtil.getValue("orgId");
		if(StringUtils.isEmpty(url) || StringUtils.isEmpty(appKey) || StringUtils.isEmpty(systemId) || StringUtils.isEmpty(orgId)) {
			url = RedisFactory.get("config_url");
			appKey = RedisFactory.get("appKey");
			systemId = RedisFactory.get("systemId");
			orgId = RedisFactory.get("orgId");
		}
		url += "queryEffectiveMember?appKey="+appKey+"&systemId="+systemId;
		Map<String, String> map = new HashMap<String, String>();
		map.put("memberCard", recordCardNO);
		map.put("orgId", orgId);
		int timeout = 8000;
		String result = HttpUtil.doPostMethod(url, map,timeout);
	    JSONObject obj = (JSONObject)JSONObject.parse(result);
	    System.out.println(obj);
	    String memberList = obj.getString("data");
	    System.out.println(memberList);
	    JSONArray arr = (JSONArray)JSONArray.parse(memberList);
	    String objResult = "";
	    if(arr.size()>0) {	    	
	    	objResult = arr.get(0).toString();
	    }
	    obj = JSONObject.parseObject(objResult);
	    return obj;	
	}
	

	
	/**
	 * 查询所有有效会员
	 * @return
	 * @throws IOException 
	 */
	public synchronized static JSONObject searchVailDateMemberList() throws IOException{
		String url = PropertiesUtil.getValue("config_url");
		String appKey = PropertiesUtil.getValue("appKey");
		String systemId = PropertiesUtil.getValue("systemId");
		String orgId = PropertiesUtil.getValue("orgId");
		if(StringUtils.isEmpty(url) || StringUtils.isEmpty(appKey) || StringUtils.isEmpty(systemId) || StringUtils.isEmpty(orgId)) {
			url = RedisFactory.get("config_url");
			appKey = RedisFactory.get("appKey");
			systemId = RedisFactory.get("systemId");
			orgId = RedisFactory.get("orgId");
		}
		System.out.println(url+":"+appKey+":"+systemId+":"+orgId+":");
		url += "queryEffectiveMember?appKey="+appKey+"&systemId="+systemId;
		Map<String, String> map = new HashMap<String, String>();
		map.put("orgId", orgId);
		int timeout = 8000;
		String result = HttpUtil.doPostMethod(url, map,timeout);
	    JSONObject obj = (JSONObject)JSONObject.parse(result);
	    return obj;
	}
	
	/**
	 * 根据本地缓存查询有效会员
	 * @return
	 */
	public Map<String,String> searchValidateMemberLocal(String memberCard) {
		Jedis conn = RedisFactory.getConnection();
		String key = "memberCard:"+memberCard;
		Map<String,String> map = conn.hgetAll(key);
		return map;
	}
	
	
	
}
    


    

