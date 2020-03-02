package com.task;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.redis.RedisFactory;



import com.baidu.speech.restapi.ttsdemo.BaiDuSpeak;
import com.baidu.speech.restapi.ttsdemo.BaseVociceConfig;
import com.google.gson.JsonObject;
import com.util.HttpUtil;
import com.util.PropertiesUtil;




/**
 * 查询用户信息
 * 
 * @author admin
 *
 */
public class QueryPersonThread extends Thread {
	private static Logger log = Logger.getLogger("QueryPersonThread");
	private String search;
	private String port;
	private String type;
	private static String age = "";
	private static String sex = "";
	private static String height = "";
	private static String suffix = ".mp3";
	public static Map<String, Object> loginMap = new HashMap<String, Object>();
	public BaiDuSpeak baiduSpeak = new BaiDuSpeak();
	
	public static void main(String [] arge) {
		QueryPersonThread q = new QueryPersonThread("", "", "");
		q.getUserInfo("1427138903", "");
	}
	
	public QueryPersonThread(String search, String port,String typeName) {
		this.search = search;
		this.port = port;
		type = typeName;
	}

	public void run() {
		log.info("------------------------查询用户线程开始--------------------------");
		getUserInfo(search, port);
	}
	
	/**
	 * 获取用户信息（卡号、或者id）
	 * 
	 */
	public void getUserInfo(String search, String port) {
		try {
			
			String url = PropertiesUtil.getValue("config_url");
			if(StringUtils.isEmpty(url)) {
				url = RedisFactory.get("config_url");
				
			}
			String appKey = PropertiesUtil.getValue("appKey");
			if(StringUtils.isEmpty(appKey)) {
				appKey = RedisFactory.get("appKey");
			
			}
			String systemId = PropertiesUtil.getValue("systemId");
			if(StringUtils.isEmpty(systemId)) {
				systemId = RedisFactory.get("systemId");
				
			}
			url += "member/getData?appKey="+appKey+"&systemId="+systemId;
			
			String orgId = PropertiesUtil.getValue("orgId");
			if(StringUtils.isEmpty(orgId)) {
				orgId = RedisFactory.get("orgId");
				
			}
			JsonObject json = new JsonObject();
			json.addProperty("keyNo", "10001");
			JsonObject param = new JsonObject();
			param.addProperty("orgId", orgId);
			param.addProperty("systemId", systemId);
			param.addProperty("search", search);
			param.addProperty("pageNo", "1");
			param.addProperty("pageSize", "10");
			String [] orgs = new String[1];
			orgs[0] =orgId;
			param.addProperty("childrenOrgId",orgs.toString());
			json.add("param", param);
			String timeOut = PropertiesUtil.getValue("http_time_out");
			if(StringUtils.isEmpty(timeOut)) {
				timeOut = RedisFactory.get("http_time_out");
			
			}
			int time =3000;
			if(timeOut != null && !"".equals(timeOut)) {
				try{
					time = Integer.parseInt(timeOut);
				}catch(Exception e) {
				}
			}
			log.info("【查询用户信息url地址：】"+url);
			String result = HttpUtil.httpPostWithJSON(url, json.toString(),time);
			log.info("获取用户信息：" + result);
			com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSONObject.parseObject(result);
			com.alibaba.fastjson.JSONArray arr = obj.getJSONArray("data");
			if (arr != null && arr.size() > 0) {
				com.alibaba.fastjson.JSONObject bean = (com.alibaba.fastjson.JSONObject) arr.get(0);
				String name = bean.getString("name");
				String personId = bean.getString("personId");
				height = bean.getString("height");
				sex =  bean.getString("sex");
				age = bean.getString("age");
				
				if("login".equals(type)) {
					//签到使用
					login(personId, name);
				} 
			} else {
				log.info("未绑定的卡----------------------"+search);
				try{
					baiduSpeak.speak("未绑定的卡");					
				}catch(Exception e2) {
					e2.printStackTrace();
					log.info("百度语言播报异常");
					baiduSpeak.playBase(BaseVociceConfig.NOTBINGCARD);
				}
			}
		} catch (Exception e) {
			try{				
				baiduSpeak.speak("获取信息失败，请重新刷卡");
			}catch(Exception e1){
				e1.printStackTrace();
				log.info("百度语言播报异常");
				baiduSpeak.playBase(BaseVociceConfig.CANOTGETMESSAGE);
			}
			log.info("异常："+e.getMessage());
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 签到
	 * 
	 * @param userId
	 * @param name
	 */
	private void login(String userId,String name) {
		boolean flag = true;
		log.info("获取登录信息" + loginMap.get(userId));
		if (loginMap.get(userId) != null) {
			long time = (long) loginMap.get(userId);
			long now = new Date().getTime();
			String login_outtime = PropertiesUtil.getValue("login_outtime");
			if(StringUtils.isEmpty(login_outtime)) {
				login_outtime = RedisFactory.get("login_outtime");
			
			}
			long outtime = 10 * 60 * 1000;
			if (login_outtime != null && !"".equals(login_outtime)) {
				outtime = Long.parseLong(login_outtime);
			}
			if (now - time > outtime) {
				flag = true;
			} else {
				//重复签到提醒
				try{					
					baiduSpeak.speak("不能重复签到");
				}catch(Exception e1) {
					e1.printStackTrace();
					log.info("百度语音播报失败");
					baiduSpeak.playBase(BaseVociceConfig.CANOTREPEAT);
				}
				log.info(userId + "此用户在已经签到不能重复签到，不用重复登录");
				flag = false;
			}
		}
		if (flag) {
			String http_time_out = PropertiesUtil.getValue("http_time_out");
			if(StringUtils.isEmpty(http_time_out)) {
				http_time_out = RedisFactory.get("http_time_out");
			
			}
			int timeout =3000;
			if (http_time_out != null && !"".equals(http_time_out)) {
				try {
					timeout = Integer.valueOf(http_time_out);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		
			String login_url = PropertiesUtil.getValue("login_url");
			if(StringUtils.isEmpty(login_url)) {
				login_url = RedisFactory.get("login_url");
			
			}
			String systemId = PropertiesUtil.getValue("systemId");
			if(StringUtils.isEmpty(systemId)) {
				systemId = RedisFactory.get("systemId");
			
			}
			String orgId = PropertiesUtil.getValue("orgId");
			if(StringUtils.isEmpty(orgId)) {
				orgId = RedisFactory.get("orgId");
			
			}
			Map<String,String> map = new HashMap<>();
			map.put("personId", userId);
			map.put("systemId",systemId);
			map.put("orgId",orgId);
			log.info("【签到url地址：】"+login_url);
			String loginResutl = HttpUtil.httpPostRaw(login_url, map,timeout);
			if ("{\"success\":true,\"message\":\"ok!\"}".equals(loginResutl)) {
				loginMap.put(userId, new Date().getTime());
				// 签到成功文字转语音播放
				try{
					baiduSpeak.speak("刷卡签到成功," + name);					
				}catch(Exception e1) {
					e1.printStackTrace();
					log.info("百度语音播报失败");
					baiduSpeak.playBase(BaseVociceConfig.LOGINSUCCESS);
				} 
			} else {
				try{					
					baiduSpeak.speak("签到失败，请重新刷卡");
				}catch(Exception e1) {
					e1.printStackTrace();
					log.info("百度语音播报失败");
					baiduSpeak.playBase(BaseVociceConfig.LOGINFAIL);
				}
			}
		}
	}
	




	/**
	 * 将16进制字符串转换为byte[]
	 *
	 * @param str
	 * @return
	 */
	public static byte[] toBytes(String str) {
		if (str == null || str.trim().equals("")) {
			return new byte[0];
		}

		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < str.length() / 2; i++) {
			String subStr = str.substring(i * 2, i * 2 + 2);
			bytes[i] = (byte) Integer.parseInt(subStr, 16);
		}

		return bytes;
	}


	/**
	 * 字节流转成十六进制表示
	 */
	public static String encode(byte[] src) {
		String strHex = "";
		StringBuilder sb = new StringBuilder("");
		for (int n = 0; n < src.length; n++) {
			strHex = Integer.toHexString(src[n] & 0xFF);
			String value = (strHex.length() == 1) ? "0" + strHex : strHex;
			sb.append(value); // 每个字节由两个字符表示，位数不够，高位补0x
		}
		return sb.toString().trim();
	}

	/**
	 * 获取bcc值
	 *
	 * @param p
	 * @return
	 */
	private static String bbc(String p) {
		int total = 0;
		int len = p.length();
		int num = 0;
		while (num < len) {
			String s = p.substring(num, num + 2);
			total += Integer.parseInt(s, 16);
			num = num + 2;
		}

		int mod = total % 256;
		String hex = Integer.toHexString(mod);
		len = hex.length();
		// 如果不够校验位的长度，补0,这里用的是两位校验
		if (len < 2) {
			hex = "0" + hex;
		}
		hex = hex.toUpperCase();
		return hex;
	}
}
