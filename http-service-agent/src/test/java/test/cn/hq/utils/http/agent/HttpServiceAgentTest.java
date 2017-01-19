package test.cn.hq.utils.http.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.hq.utils.http.HttpMethod;
import cn.hq.utils.http.agent.Authorization;
import cn.hq.utils.http.agent.AuthorizationAlgs;
import cn.hq.utils.http.agent.HttpServiceAgent;
import cn.hq.utils.http.agent.ServiceAddress;
import test.cn.hq.utils.web.server.WebServer;

public class HttpServiceAgentTest {

	private static final String host = "127.0.0.1";

	private static final int port = 10189;
	
	private static final String SENDER_NAME = "upush_test";
	
	private static final String SECRET_KEY = "123abc";

	private WebServer server;

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	private void prepareEnvironment(String contextPath, HttpServlet servlet, String servletMapping) {
		int port = 10189;
		server = new WebServer(host, port);
		server.registServlet("test-servlet", servlet, servletMapping);
		server.setContextPath(contextPath);
		server.start();
	}

	@Test
	public void testRequestParam() {
		String contextPath = "/testserver";
		String servicePath = "/test/content";
		DataResponse expectedResponse = new DataResponse(true, null, "TestContent\r\nabckkk\r\n1");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);

		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);

		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);

		// 主要路径；
		String expectedId = "001";
		boolean expectedPermited = true;
		boolean origMale = false;
		String expectedMaleStr = (new CustomBooleanConverter()).toString(origMale);
		int expectedValue = 10;
		// 发起请求；
		String actualResponseText = testService.content(expectedId, expectedPermited, origMale, expectedValue);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);
		Iterator<HttpRequestInfo> reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		HttpRequestInfo reqRecord = reqRecords.next();
		assertEquals(HttpMethod.GET, reqRecord.getMethod());
		Map<String, String[]> params = reqRecord.getParameters();
		assertEquals(4, params.size());
		assertTrue(params.containsKey("id"));
		assertTrue(params.containsKey("permited"));
		assertTrue(params.containsKey("male"));
		assertTrue(params.containsKey("value"));
		assertEquals(expectedId, params.get("id")[0]);
		assertEquals(Boolean.toString(expectedPermited), params.get("permited")[0]);
		assertEquals(expectedMaleStr, params.get("male")[0]);
		assertEquals(expectedValue + "", params.get("value")[0]);
		// 清理数据；
		servlet.clearRequestRecords();

		// 在主要路径的基础上验证标注的请求路径名，忽略特定值的参数；
		expectedId = "001";
		expectedPermited = true;
		origMale = false;
		expectedMaleStr = (new CustomBooleanConverter()).toString(origMale);
		int expectedIgnoreValue = -1;
		// 发起请求；
		DataResponse actualResponse = testService.getContent(expectedId, expectedPermited, origMale,
				expectedIgnoreValue, "other values");
		// 验证结果；
		assertEquals(expectedResponse.getContent(), actualResponse.getContent());
		assertEquals(expectedResponse.getError(), actualResponse.getError());
		assertEquals(expectedResponse.isSuccess(), actualResponse.isSuccess());

		reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		reqRecord = reqRecords.next();
		assertEquals(HttpMethod.GET, reqRecord.getMethod());
		params = reqRecord.getParameters();
		assertEquals(3, params.size());
		assertTrue(params.containsKey("id"));
		assertTrue(params.containsKey("permited"));
		assertTrue(params.containsKey("male"));
		assertTrue(!params.containsKey("value"));// 不包含应被忽略的值；
		assertEquals(expectedId, params.get("id")[0]);
		assertEquals(Boolean.toString(expectedPermited), params.get("permited")[0]);
		assertEquals(expectedMaleStr, params.get("male")[0]);
		servlet.clearRequestRecords();

		// 在前面的测试路径的基础上验证 http post 方法发送请求，以及自定义的回复转换器；
		expectedId = "001";
		expectedPermited = true;
		boolean expectedMale = false;
		expectedValue = 188;
		// 发起请求；
		String actualResponseText2 = testService.fetchContent(expectedId, expectedPermited, expectedMale,
				expectedValue);
		// 验证结果；
		assertEquals(expectedResponse.getContent(), actualResponseText2);
		reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		reqRecord = reqRecords.next();
		assertEquals(HttpMethod.POST, reqRecord.getMethod());
		params = reqRecord.getParameters();
		assertEquals(4, params.size());
		assertTrue(params.containsKey("id"));
		assertTrue(params.containsKey("permited"));
		assertTrue(params.containsKey("male"));
		assertTrue(params.containsKey("value"));
		assertEquals(expectedId, params.get("id")[0]);
		assertEquals(Boolean.toString(expectedPermited), params.get("permited")[0]);
		assertEquals(expectedMaleStr, params.get("male")[0]);
		assertEquals(expectedValue + "", params.get("value")[0]);
		
	}

	/**
	 * 测试自定义的 ResponseConverter 以及服务方法声明异常的处理机制；
	 */
	public void testResponseConverterWithException_Success() {
		String contextPath = "/testserver";
		String servicePath = "/test/content";
		boolean success = true;
		String expectedError = "";
		DataResponse expectedResponse = new DataResponse(success, expectedError, "TestContent\r\nabckkk\r\n1");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);
		
		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);
		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);
		
		// 准备数据；
		String expectedId = "001";
		boolean expectedPermited = true;
		boolean origMale = false;
		int expectedValue = 10;

		// 主要路径：测试自定义的回复转换器返回正常时的回复结果；
		expectedId = "001";
		expectedPermited = true;
		boolean expectedMale = false;
		String expectedMaleStr = (new CustomBooleanConverter()).toString(origMale);
		expectedValue = 188;
		// 发起请求；
		GetContentException expectedException = null;
		String actualResponseText2 = null;
		try {
			actualResponseText2 = testService.fetchContentWithError(expectedId, expectedPermited, expectedMale,
					expectedValue);
		} catch (GetContentException e) {
			expectedException = e;
		}
		// 验证结果；
		assertTrue(expectedException == null);// 预期没有异常；
		
		assertEquals(expectedResponse.getContent(), actualResponseText2);
		Iterator<HttpRequestInfo> reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		HttpRequestInfo reqRecord = reqRecords.next();
		assertEquals(HttpMethod.POST, reqRecord.getMethod());
		Map<String, String[]> params = reqRecord.getParameters();
		assertEquals(4, params.size());
		assertTrue(params.containsKey("id"));
		assertTrue(params.containsKey("permited"));
		assertTrue(params.containsKey("male"));
		assertTrue(params.containsKey("value"));
		assertEquals(expectedId, params.get("id")[0]);
		assertEquals(Boolean.toString(expectedPermited), params.get("permited")[0]);
		assertEquals(expectedMaleStr, params.get("male")[0]);
		assertEquals(expectedValue + "", params.get("value")[0]);

	}
	
	/**
	 * 测试自定义的 ResponseConverter 以及服务方法声明异常的处理机制；
	 */
	public void testResponseConverterWithException_Error() {
		String contextPath = "/testserver";
		String servicePath = "/test/content";
		boolean success = false;
		String expectedError = "test error message";
		DataResponse expectedResponse = new DataResponse(success, expectedError, "TestContent\r\nabckkk\r\n1");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);
		
		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);
		
		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);
		
		// 准备数据；
		String expectedId = "001";
		boolean expectedPermited = true;
		int expectedValue = 10;
		
		// 主要路径：测试自定义的回复转换器返回正常时的回复结果；
		expectedId = "001";
		expectedPermited = true;
		boolean expectedMale = false;
		expectedValue = 188;
		// 发起请求；
		GetContentException expectedException = null;
		try {
			String actualResponseText2 = testService.fetchContentWithError(expectedId, expectedPermited, expectedMale,
					expectedValue);
		} catch (GetContentException e) {
			expectedException = e;
		}
		// 验证结果；
		assertTrue(expectedException != null);// 预期没有异常；
		assertEquals(expectedError, expectedException.getMessage());
	}
	
	@Test
	public void testRequestParamMap() {
		String contextPath = "/testserver";
		String servicePath = "/test/content";
		DataResponse expectedResponse = new DataResponse(true, null, "TestContent\r\nabckkk\r\n1");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);

		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);

		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);

		// 主要路径；
		String expectedId = "001";
		boolean expectedPermited = true;
		boolean origMale = false;
		String expectedMaleStr = (new CustomBooleanConverter()).toString(origMale);
		int expectedValue = 10;

		ContentRequestSetting expected = new ContentRequestSetting();
		expected.setId(expectedId);
		expected.setMale(origMale);
		expected.setPermited(expectedPermited);
		expected.setValue(expectedValue);

		// 发起请求；
		String actualResponseText = testService.content(expected);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);
		Iterator<HttpRequestInfo> reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		HttpRequestInfo reqRecord = reqRecords.next();
		assertEquals(HttpMethod.GET, reqRecord.getMethod());
		Map<String, String[]> params = reqRecord.getParameters();
		assertEquals(5, params.size());
		assertTrue(params.containsKey("id"));
		assertTrue(params.containsKey("permited"));
		assertTrue(params.containsKey("male"));
		assertTrue(params.containsKey("value"));
		assertEquals(expectedId, params.get("id")[0]);
		assertEquals(Boolean.toString(expectedPermited), params.get("permited")[0]);
		assertEquals(expectedMaleStr, params.get("male")[0]);
		assertEquals(expectedValue + "", params.get("value")[0]);
		
		assertTrue(params.containsKey("type"));//验证只读常量字段也可以映射为请求参数；
		assertEquals(expected.getType().toString(), params.get("type")[0]);
		// 清理数据；
		servlet.clearRequestRecords();
	}

	@Test
	public void testRequestParam_Map() {
		// TODO implemented;
	}

	@Test
	public void testMultiRequestParamMap() {
		// TODO implemented;
	}

	/**
	 * 测试多行文本的发送和获取；
	 */
	@Test
	public void testMultilineTextSendAndGet() {
		String contextPath = "/testserver";
		String servicePath = "/test/content/update";
		String expectedResponseText = "TestContent\r\nline2:testcontent.......\r\nline3::::";
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);

		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);

		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);

		// 发起请求；
		String actualResponseText = testService.updateAndGetContent(expectedResponseText);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);

		Iterator<HttpRequestInfo> reqRecords = servlet.getRequestRecords();
		assertTrue(reqRecords.hasNext());
		HttpRequestInfo reqRecord = reqRecords.next();
		assertEquals(HttpMethod.GET, reqRecord.getMethod());
		Map<String, String[]> params = reqRecord.getParameters();
		assertEquals(1, params.size());
		assertTrue(params.containsKey("content"));
		assertEquals(expectedResponseText, params.get("content")[0]);

		// 清理数据；
		servlet.clearRequestRecords();
	}


	@Test
	public void testRequestParam_Map_Body() {
		// TODO implemented;
	}

	/**
	 * 测试PathParam
	 */
	@Test
	public void testPathParam() {
		String contextPath = "/testserver";
		String servicePath = "/test/content/*";
		DataResponse expectedResponse = new DataResponse(true, null, "TestContent\r\nabckkk\r\n001");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);

		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);

		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);

		// 主要路径；
		String expectedId = "001";
		// 发起请求；
		String actualResponseText = testService.contentById(expectedId);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);
		assertEquals("/testserver/test/content/001", servlet.getRequestPath());
		assertEquals(AuthorizationAlgs.DEFAULT + " upush_test:123abc", servlet.getAuthorization());
		// 清理数据；
		servlet.clearRequestRecords();
		
		/**
		 * 
		 * 测试put请求
		 * 
		 */
		String expectedContent = "Hello World!";
		// 发起请求；
		actualResponseText = testService.contentByPut(expectedId,expectedContent);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);
		assertEquals("PUT", servlet.getRequestMethod());
		// 清理数据；
		servlet.clearRequestRecords();
	}
	
	@Test
	public void testRequestBody(){
		String contextPath = "/testserver";
		String servicePath = "/test/content/*";
		DataResponse expectedResponse = new DataResponse(true, null, "TestContent\r\nabckkk\r\n001");
		String expectedResponseText = JSON.toJSONString(expectedResponse);
		HttpRequestCollector servlet = new HttpRequestCollector(expectedResponseText);

		// 准备环境；
		prepareEnvironment(contextPath, servlet, servicePath);

		ServiceAddress setting = new ServiceAddress(host, port, false);
		Authorization authSetting = new Authorization(AuthorizationAlgs.DEFAULT, SENDER_NAME, SECRET_KEY);
		setting.setContextPath(contextPath);
		HttpTestService testService = HttpServiceAgent.createService(HttpTestService.class, setting, authSetting);

		// 主要路径；
		String expectedId = "001";
		String expectedName = "liuxrb";
		RequestContent expectedRequestContent = new RequestContent(100, "Hello, World!");
		// 发起请求；
		String actualResponseText = testService.contentBody(expectedId, expectedName, expectedRequestContent);
		// 验证结果；
		assertEquals(expectedResponseText, actualResponseText);
		assertEquals("/testserver/test/content/001", servlet.getRequestPath());
		assertEquals(AuthorizationAlgs.DEFAULT + " upush_test:123abc", servlet.getAuthorization());
		assertEquals(JSONObject.toJSONString(expectedRequestContent), servlet.getRequestBody());
		// 清理数据；
		servlet.clearRequestRecords();
		
	}
}
