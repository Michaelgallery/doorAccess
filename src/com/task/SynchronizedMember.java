package com.task;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;
import org.redis.RedisFactory;
import org.wiegand.TestZone.TestShort;

import redis.clients.jedis.Jedis;

import com.DoorAddressServer;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.util.LogManager;

/**
 * 同步云服务器中的会员数据到本地库，定时任务
 * @author zhenpengcheng
 *
 */
public class SynchronizedMember extends TimerTask{
	private static java.util.logging.Logger log = java.util.logging.Logger.getLogger("SynchronizedMember");
	@Override
	public void run() {
		// TODO Auto-generated method stub
		JSONArray arr = null;
		try{			
			JSONObject obj = TestShort.searchVailDateMemberList();
			String memberList = obj.getString("data");
			arr = (JSONArray)JSONArray.parse(memberList);
			log.info(arr.toString());
		}catch(Exception e) {
			log.info("==========远程同步云数据库失败=============");
			e.printStackTrace();
		}
		Jedis conn = null;
		try{	
		    conn = RedisFactory.getConnection();
			HashMap<String,String> map = new HashMap<>();
			if(arr!=null&&arr.size()>0) {
				for(int i=0;i<arr.size();i++) {
					map.clear();
					String temp = arr.get(i).toString();
					JSONObject member = JSONObject.parseObject(temp);
					String key = "memberCard:"+member.getString("member_card");
					String memberCard = member.getString("member_card");
					String name = member.getString("name");
					if(StringUtils.isEmpty(memberCard)) {	
						continue;
					}				
					map.put("memberCard", memberCard);
					map.put("name", name);	
					conn.hmset(key, map);
					log.info("============同步本地缓存成功==========");
				}
			}
		}catch(Exception e) {
			log.info("=======redis 本地连接失败！请检查安装============");
			e.printStackTrace();
		}
	}
	

	
	public static void main(String[] args) {
		JSONObject json = new JSONObject();
		String s = json.getString("member_card");
		System.out.println(s);
		/*Jedis conn = RedisFactory.getConnection();
		String key = "memberCard:20190725140124305025";
		Map<String,String> map = conn.hgetAll(key);
		System.out.println("============");*/
	}

}
