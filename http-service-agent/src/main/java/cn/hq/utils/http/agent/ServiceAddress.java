package cn.hq.utils.http.agent;

import java.util.Properties;

/**
 * 服务器设置；
 * 
 * @author haiq
 *
 */
public class ServiceAddress implements Cloneable, ServerConfiguration {
	
	private String host;

	private int port;

	private String contextPath;
	
	private boolean isHttps;

	private Properties headers = new Properties();
	
	public ServiceAddress(String host, int port, boolean isHttps) {
		this(host, port, isHttps, null);
	}
	
	public ServiceAddress(String host, int port, Boolean isHttps,String contextPath) {
		this.host = host;
		this.port = port;
		this.isHttps = isHttps;
		this.contextPath = contextPath;
	}

	
	
	/* (non-Javadoc)
	 * @see com.yyuap.sns.utils.http.agent.ServerConfiguration#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see com.yyuap.sns.utils.http.agent.ServerConfiguration#getPort()
	 */
	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	

	public boolean isHttps() {
		return isHttps;
	}

	public void setHttps(boolean isHttps) {
		this.isHttps = isHttps;
	}

	/* (non-Javadoc)
	 * @see com.yyuap.sns.utils.http.agent.ServerConfiguration#getContextPath()
	 */
	@Override
	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	/* (non-Javadoc)
	 * @see com.yyuap.sns.utils.http.agent.ServerConfiguration#getHeaders()
	 */
	@Override
	public Properties getHeaders() {
		return headers;
	}

	/**
	 * 设置头部；
	 * 
	 * @param name
	 * @param value
	 */
	public void setHeader(String name, String value) {
		headers.setProperty(name, value);
	}

	@Override
	public ServiceAddress clone() {
		try {
			return (ServiceAddress) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new UnsupportedOperationException(e.getMessage(), e);
		}
	}

//	@Override
//	public String getCharset() {
//		return charset;
//	}
//
//	public void setCharset(String charset) {
//		this.charset = charset;
//	}
}
