package org.observer;

import java.util.Observable;
import java.util.Observer;

import org.wiegand.TestZone.TestShort;
/**
 * 门禁线程观察者
 * 观察者模式，线程出现异常后自动通知启动
 * @author zhanpengcheng
 *
 */
public class DoorAccessThreadObserver implements Observer{

	@Override
	public void update(Observable o, Object arg) {
		// TODO Auto-generated method stub
		//重启线程，并添加观察者
		TestShort ts = new TestShort();
		Thread doorAccess = new Thread(ts,"doorAccessThread");
		ts.addObserver(this);
		System.out.println("======重启线程========");
		doorAccess.start();	
	}

}
