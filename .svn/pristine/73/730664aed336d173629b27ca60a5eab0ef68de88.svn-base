package com.task;

import java.util.Timer;
import java.util.TimerTask;

import com.DoorAddressServer;

/**
 * 定时任务
 * 
 * @author admin
 *
 */
public class TimeService {
	public static Timer timer = new Timer();
	public static Timer isRunTimer = new Timer();
	
	/**
	 * 清除定时任务
	 */
	public static void clearTime(){
		DoorAddressServer.cardThreadIsRun = false;
		timer.cancel();
		timer.purge();
		timer = new Timer();
	}
	

	
	/**
	 * 添加定时任务
	 * 
	 * @param task
	 * @param time
	 */
	public static void addTimer(TimerTask task,long time) {
		timer.schedule(task,0 ,time);
	}
}
