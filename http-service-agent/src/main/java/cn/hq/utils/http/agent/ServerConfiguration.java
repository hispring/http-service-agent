package cn.hq.utils.http.agent;

import java.util.Properties;

public interface ServerConfiguration {
	
	public static final String DEFAULT_CHARSET = "utf-8";

	/**
	 * 获取主机地址；
	 * 
	 * @return
	 */
	String getHost();

	/**
	 * 获取端口；
	 * 
	 * @return
	 */
	int getPort();

	/**
	 * 应用上下文路径；
	 * 
	 * 默认为 / ；
	 * 
	 * @return
	 */
	String getContextPath();

	/**
	 * 获取所有的头部；
	 * 
	 * @return
	 */
	Properties getHeaders();
	
	boolean isHttps();
}