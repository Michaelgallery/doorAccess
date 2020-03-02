package com.task;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 签到推送线程池
 * 
 * @author admin
 *
 */
public class SignNotifyThreadPoolExecutor {
	public static ThreadPoolExecutor executor = null;

	public static ThreadPoolExecutor getThreadPoolExecutor() {
		if (executor == null) {
			executor = new ThreadPoolExecutor(1, 1000, 300, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(3),
					new ThreadPoolExecutor.CallerRunsPolicy());
		}
		return executor;
	}
}

