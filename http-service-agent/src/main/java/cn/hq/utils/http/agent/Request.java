package cn.hq.utils.http.agent;

import java.net.URI;
import java.nio.ByteBuffer;

import cn.hq.utils.http.HttpMethod;

public interface Request {

	HttpMethod getMethod();

	URI getUri();

	ByteBuffer getBody();
	
	void setHeader(String name, String value);

}