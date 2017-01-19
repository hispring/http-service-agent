package cn.hq.utils.http.agent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import cn.hq.utils.http.HttpAction;
import cn.hq.utils.http.HttpMethod;
import cn.hq.utils.http.HttpService;
import cn.hq.utils.http.HttpServiceException;
import cn.hq.utils.http.HttpStatusException;
import cn.hq.utils.http.PathParam;
import cn.hq.utils.http.RequestBody;
import cn.hq.utils.http.RequestBodyConverter;
import cn.hq.utils.http.RequestParam;
import cn.hq.utils.http.RequestParamMap;
import cn.hq.utils.http.ResponseConverter;
import cn.hq.utils.http.ResponseConverterFactory;
import cn.hq.utils.http.StringConverter;
import cn.hq.utils.http.converters.NullResponseConverter;
import cn.hq.utils.http.converters.StringResponseConverter;
import cn.hq.utils.io.BytesUtils;

/**
 * http 服务代理；
 * 
 * @author haiq
 *
 */
public class HttpServiceAgent implements InvocationHandler {

	private Class<?> serviceClass;

	private ServiceAddress setting;

//	private Authorization authSetting;

	private ResponseConverter defaultResponseConverter;

	private ResponseConverterFactory responseConverterFactory;

	private AuthorizationResovler authorizationResolver;

	private Map<Method, ServiceActionContext> actions = new HashMap<Method, ServiceActionContext>();

	private HttpServiceAgent(Class<?> serviceClass, ServiceAddress setting, final Authorization authSetting) {
		this.serviceClass = serviceClass;
		this.setting = setting.clone();
		if (authSetting != null) {
			this.authorizationResolver = new AuthorizationResovler() {
				@Override
				public Authorization authorize(Request request) {
					return authSetting;
				}
			};
		}
		resolveService();
	}
	private HttpServiceAgent(Class<?> serviceClass, ServiceAddress setting){
		this.serviceClass = serviceClass;
		this.setting = setting.clone();
		resolveService();
	}
	private HttpServiceAgent(Class<?> serviceClass, ServiceAddress setting, AuthorizationResovler authResolver) {
		this.serviceClass = serviceClass;
		this.setting = setting.clone();
		this.authorizationResolver = authResolver;
		resolveService();
	}

	/**
	 * 创建映射指定服务接口的 HTTP 服务代理；
	 * 
	 * @param serviceClass
	 *            定义了服务的接口类型；
	 * @param setting
	 *            连接到服务提供者服务器的相关设置；
	 * @return
	 */
	public static <T> T createService(Class<T> serviceClass, ServiceAddress setting, Authorization authSetting) {
		HttpServiceAgent agent = new HttpServiceAgent(serviceClass, setting, authSetting);
		@SuppressWarnings("unchecked")
		T serviceProxy = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[] { serviceClass },
				agent);
		return serviceProxy;
	}
	public static <T> T createService(Class<T> serviceClass, ServiceAddress setting) {
		HttpServiceAgent agent = new HttpServiceAgent(serviceClass, setting);
		@SuppressWarnings("unchecked")
		T serviceProxy = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[] { serviceClass },
				agent);
		return serviceProxy;
	}
	/**
	 * 创建映射指定服务接口的 HTTP 服务代理；
	 * 
	 * @param serviceClass
	 *            定义了服务的接口类型；
	 * @param setting
	 *            连接到服务提供者服务器的相关设置；
	 * @return
	 */
	public static <T> T createService(Class<T> serviceClass, ServiceAddress setting, AuthorizationResovler authResolver) {
		HttpServiceAgent agent = new HttpServiceAgent(serviceClass, setting, authResolver);
		@SuppressWarnings("unchecked")
		T serviceProxy = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[] { serviceClass },
				agent);
		return serviceProxy;
	}

	private void resolveService() {
		// 处理上下文路径;
		String contextPath = setting.getContextPath();
		contextPath = StringUtils.cleanPath(contextPath);
		if (StringUtils.isEmpty(contextPath)) {
			contextPath = "/";
		}
		setting.setContextPath(contextPath);

		// 处理服务路径；
		HttpService serviceAnno = serviceClass.getAnnotation(HttpService.class);
		if (serviceAnno == null) {
			throw new IllegalHttpServiceDefinitionException(
					"The specific service was not tag with HttpService annotation!");
		}
		String servicePath = serviceAnno.path();
		servicePath = StringUtils.cleanPath(servicePath);
		if (StringUtils.isEmpty(servicePath)) {
			throw new IllegalHttpServiceDefinitionException(
					"Illegal path or no path was specified for the HttpService!-- path=" + serviceAnno.path());
		}
		// 创建服务的默认回复转换器；
		Class<?> defaultResponseConverterClazz = serviceAnno.defaultResponseConverter();
		if (defaultResponseConverterClazz != null && defaultResponseConverterClazz != ResponseConverter.class) {
			if (ResponseConverter.class.isAssignableFrom(defaultResponseConverterClazz)) {
				defaultResponseConverter = (ResponseConverter) BeanUtils.instantiate(defaultResponseConverterClazz);
			} else {
				throw new IllegalHttpServiceDefinitionException(
						"The specified service level default response converter doesn't implement the interface "
								+ ResponseConverter.class.getName() + "!");
			}
		}
		Class<?> responseConverterFactoryClazz = serviceAnno.responseConverterFactory();
		if (responseConverterFactoryClazz != null && responseConverterFactoryClazz != ResponseConverterFactory.class) {
			if (ResponseConverterFactory.class.isAssignableFrom(responseConverterFactoryClazz)) {
				this.responseConverterFactory = (ResponseConverterFactory) BeanUtils
						.instantiate(responseConverterFactoryClazz);
			} else {
				throw new IllegalHttpServiceDefinitionException(
						"The specified service level response converter factory doesn't implement the interface "
								+ ResponseConverterFactory.class.getName() + "!");
			}

		}

		// 解析服务操作；
		Method[] mths = ReflectionUtils.getAllDeclaredMethods(serviceClass);
		for (Method mth : mths) {
			ServiceActionContext actionContext = resolveAction(setting, mth, servicePath);
			actions.put(mth, actionContext);
		}
	}

	private ServiceActionContext resolveAction(ServerConfiguration serverConfig, Method mth, String servicePath) {
		// 生成路径模板；
		HttpAction actionAnno = mth.getAnnotation(HttpAction.class);
		String actionPath = StringUtils.cleanPath(actionAnno.path());
		if (StringUtils.isEmpty(actionPath)) {
			actionPath = mth.getName();
		}
		RequestPathTemplate pathTemplate = new RequestPathTemplate(serverConfig, servicePath, actionPath);

		// 校验请求你方法；
		if (actionAnno.method() == null) {
			throw new IllegalHttpServiceDefinitionException("The http method of action was not specified!");
		}

		ResponseConverter responseConverter = createResponseConverter(actionAnno, mth);

		// 获取参数定义；
		// 参数列表中， RequestBody 最多只能定义一个；
		RequestBodyResolver bodyResolver = null;
		// Parameter[] params = mth.getParameters();
		Class<?>[] paramTypes = mth.getParameterTypes();
		Annotation[][] paramAnnos = mth.getParameterAnnotations();

		List<ArgDefEntry<RequestParam>> reqParamAnnos = new LinkedList<ArgDefEntry<RequestParam>>();
		List<ArgDefEntry<RequestParamMap>> reqParamMapAnnos = new LinkedList<ArgDefEntry<RequestParamMap>>();
		List<ArgDefEntry<PathParam>> pathParamAnnos = new LinkedList<ArgDefEntry<PathParam>>();
		for (int i = 0; i < paramTypes.length; i++) {
			RequestBody reqBodyAnno = findAnnotation(RequestBody.class, paramAnnos[i]);
			RequestParam reqParamAnno = findAnnotation(RequestParam.class, paramAnnos[i]);
			RequestParamMap reqParamsAnno = findAnnotation(RequestParamMap.class, paramAnnos[i]);
			PathParam pathParamAnno = findAnnotation(PathParam.class, paramAnnos[i]);
			if (hasConflictiveAnnotation(reqBodyAnno, reqParamAnno, reqParamsAnno, pathParamAnno)) {
				// 存在冲突的定义；
				throw new IllegalHttpServiceDefinitionException(
						"The argument[" + i + "] of action has conflictive definition!");
			}
			if (bodyResolver != null && reqBodyAnno != null) {
				throw new IllegalHttpServiceDefinitionException("Define more than one request body for the action!");
			}
			if (reqBodyAnno != null) {
				bodyResolver = createBodyResolver(new ArgDefEntry<RequestBody>(i, paramTypes[i], reqBodyAnno));
			}
			if (reqParamAnno != null) {
				reqParamAnnos.add(new ArgDefEntry<RequestParam>(i, paramTypes[i], reqParamAnno));
			}
			if (reqParamsAnno != null) {
				reqParamMapAnnos.add(new ArgDefEntry<RequestParamMap>(i, paramTypes[i], reqParamsAnno));
			}
			if (pathParamAnno != null) {
				pathParamAnnos.add(new ArgDefEntry<PathParam>(i, paramTypes[i], pathParamAnno));
			}
		}
		RequestParamResolver reqParamResolver = createRequestParamResolver(reqParamAnnos, reqParamMapAnnos);
		PathParamResolver pathParamResolver = createPathParamResolver(pathParamAnnos);
		if (bodyResolver == null) {
			bodyResolver = RequestBodyResolvers.NULL_BODY_RESOLVER;
		}

		// 获取声明的异常列表；
		Class<?>[] thrownExceptionTypes = mth.getExceptionTypes();

		ServiceActionContext actionContext = new ServiceActionContext(mth, actionAnno.method(), pathTemplate,
				pathParamResolver, reqParamResolver, bodyResolver, responseConverter, thrownExceptionTypes,
				actionAnno.resolveContentOnHttpError());
		return actionContext;
	}

	@SuppressWarnings("unchecked")
	private static <T> T findAnnotation(Class<T> clazz, Annotation[] annos) {
		for (Annotation annotation : annos) {
			if (clazz.isAssignableFrom(annotation.getClass())) {
				return (T) annotation;
			}
		}
		return null;
	}

	/**
	 * 创建回复结果转换器；
	 * 
	 * @param actionDef
	 * @param retnClazz
	 * @return
	 */
	private ResponseConverter createResponseConverter(HttpAction actionDef, Method mth) {
		Class<?> retnClazz = mth.getReturnType();
		if (Void.class.equals(retnClazz)) {
			return NullResponseConverter.INSTANCE;
		}
		Class<?> respConverterClass = actionDef.responseConverter();
		if (respConverterClass == null || respConverterClass == ResponseConverter.class) {
			// 未设置方法级别的回复转换器；
			if (defaultResponseConverter != null) {
				// 如果未设置方法级别的回复转换器，且设置了服务级别的默认回复转换器，则应用服务级别的默认回复转换器；
				return defaultResponseConverter;
			}
			if (responseConverterFactory != null) {
				return responseConverterFactory.createResponseConverter(actionDef, mth);
			}
		}
		if (respConverterClass != null && respConverterClass != ResponseConverter.class) {
			if (ResponseConverter.class.isAssignableFrom(respConverterClass)) {
				return (ResponseConverter) BeanUtils.instantiate(respConverterClass);
			} else {
				throw new IllegalHttpServiceDefinitionException(
						"The specified response converter doesn't implement the interface "
								+ ResponseConverter.class.getName() + "!");
			}
		}
		// create default response converter;
		return DefaultResponseConverterFactory.INSTANCE.createResponseConverter(actionDef, mth);

		// if (byte[].class == retnClazz) {
		// return ByteArrayResponseConverter.INSTANCE;
		// }
		// if (String.class == retnClazz) {
		// return StringResponseConverter.INSTANCE;
		// }
		// // TODO:未处理 基本类型、输入输出流；
		// return new JsonResponseConverter(retnClazz);
	}

	/**
	 * 创建路径参数解析器；
	 * 
	 * @param pathParamAnnos
	 * @return
	 */
	private PathParamResolver createPathParamResolver(List<ArgDefEntry<PathParam>> pathParamAnnos) {
		if (pathParamAnnos.size() == 0) {
			return PathParamResolvers.NONE_PATH_PARAM_RESOLVER;
		}
		List<ArgDefEntry<PathParamDefinition>> pathParamDefs = new LinkedList<ArgDefEntry<PathParamDefinition>>();
		for (ArgDefEntry<PathParam> entry : pathParamAnnos) {
			if (StringUtils.isEmpty(entry.getDefinition().name())) {
				throw new IllegalHttpServiceDefinitionException("The name of path parameter is empty!");
			}

			Class<?> converterClazz = entry.getDefinition().converter();
			StringConverter converter = StringConverterFactory.instantiateStringConverter(converterClazz);
			ArgDefEntry<PathParamDefinition> argDefEntry = new ArgDefEntry<PathParamDefinition>(entry.getIndex(),
					entry.getArgType(), new PathParamDefinition(entry.getDefinition().name(), converter));
			pathParamDefs.add(argDefEntry);
		}

		return PathParamResolvers.createResolver(pathParamDefs);
	}

	/**
	 * 创建请求参数解析器；
	 * 
	 * @param reqParamAnnos
	 * @return
	 */
	private RequestParamResolver createRequestParamResolver(List<ArgDefEntry<RequestParam>> reqParamAnnos,
			List<ArgDefEntry<RequestParamMap>> reqParamsAnnos) {
		List<ArgDefEntry<RequestParamDefinition>> reqDefs = RequestParamDefinition
				.resolveSingleParamDefinitions(reqParamAnnos);
		List<ArgDefEntry<RequestParamMapDefinition>> reqMapDefs = RequestParamMapDefinition
				.resolveParamMapDefinitions(reqParamsAnnos);

		return RequestParamResolvers.createParamMapResolver(reqDefs, reqMapDefs);

	}

	/**
	 * @param paramIndex
	 * @param parameter
	 * @param reqBodyAnnoEntry
	 * @return
	 */
	private RequestBodyResolver createBodyResolver(ArgDefEntry<RequestBody> reqBodyAnnoEntry) {
		Class<?> converterClazz = reqBodyAnnoEntry.getDefinition().converter();
		RequestBodyConverter converter = null;
		if (converterClazz == RequestBodyConverter.class || converterClazz == null) {
			// create default body converter;
			converter = new TypeAutoAdapterBodyConverter(reqBodyAnnoEntry.getArgType());
		} else {
			if (!ClassUtils.isAssignable(RequestBodyConverter.class, converterClazz)) {
				throw new IllegalHttpServiceDefinitionException(
						"The specified body converter doesn't implement the interface "
								+ RequestBodyConverter.class.getName() + "!");
			}
			converter = (RequestBodyConverter) BeanUtils.instantiate(converterClazz);
		}

		RequestBodyDefinition reqBodyDef = new RequestBodyDefinition(reqBodyAnnoEntry.getDefinition().required(),
				converter);
		ArgDefEntry<RequestBodyDefinition> reqBodyDefEntry = new ArgDefEntry<RequestBodyDefinition>(
				reqBodyAnnoEntry.getIndex(), reqBodyAnnoEntry.getArgType(), reqBodyDef);
		return RequestBodyResolvers.createArgumentResolver(reqBodyDefEntry);
	}

	/**
	 * 检查传入的三个参数中是否有两个或两个以上为非空；
	 * 
	 * @param reqBodyAnno
	 * @param reqParamAnno
	 * @param pathParamAnno
	 * @return 有两个或两个以上为非空时返回 true；
	 * 
	 *         全部为 null 或只有一个为 null 时，返回 false；
	 */
	private static boolean hasConflictiveAnnotation(RequestBody reqBodyAnno, RequestParam reqParamAnno,
			RequestParamMap reqParamsAnno, PathParam pathParamAnno) {
		return 1 < (reqBodyAnno == null ? 0 : 1) + (reqParamAnno == null ? 0 : 1) + (reqParamsAnno == null ? 0 : 1)
				+ (pathParamAnno == null ? 0 : 1);
	}

	/**
	 * 解析被调用的方法，映射为 http 请求；
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		ServiceActionContext actionContext = actions.get(method);
		if (actionContext == null) {
			throw new UnsupportedOperationException("The invoked method was not a service action!");
		}
		try {
			InnerRequest request = resolveRequest(actionContext, args);
			HttpUriRequest httpRequest = buildRequest(request);
			if (authorizationResolver != null) {
				Authorization auth = authorizationResolver.authorize(request);
				//设置自定义头；
				setCutomHeaders(httpRequest, request.getCustomHeaders());
				//设置认证属性；
				buildAuthorization(httpRequest, auth);
			}
			httpRequest.setHeader("Content-Type", "application/json");
			CloseableHttpClient httpClient = null;
			if (!setting.isHttps()) {
				httpClient = HttpClients.createDefault();
			} else {
				httpClient = HttpClients.custom().setSSLSocketFactory(createSSLConnectionSocketFactory()).build();
			}

			CloseableHttpResponse response = httpClient.execute(httpRequest);
			try {
				// 引发 http 异常；
				if (response.getStatusLine().getStatusCode() >= 400) {
					processAndThrowHttpException(actionContext, response);
					// 注：上一步已抛出异常；
					return null;
				}
				InputStream respStream = response.getEntity().getContent();
				Object respObject = actionContext.getResponseConverter().toResponseObject(respStream);
				return respObject;
			} finally {
				response.close();
			}
		} catch (Exception e) {
			if (isCustomThownException(e, actionContext)) {
				throw e;
			}
			if (e instanceof HttpServiceException) {
				throw (HttpServiceException) e;
			}
			throw new HttpServiceException(e.getMessage(), e);
		}
	}

	private void setCutomHeaders(HttpUriRequest httpRequest, Properties customHeaders) {
		Set<String> names = customHeaders.stringPropertyNames();
		for (String name : names) {
			httpRequest.setHeader(name, customHeaders.getProperty(name));
		}
	}
	/**
	 * 判断指定的异常是否属于指定服务操作的接口方法通过 throws 声明的异常；
	 * 
	 * @param e
	 * @param actionContext
	 * @return
	 */
	private boolean isCustomThownException(Exception e, ServiceActionContext actionContext) {
		Class<?> exType = e.getClass();
		Class<?>[] thrownExTypes = actionContext.getThrownExceptionTypes();
		for (Class<?> thrExType : thrownExTypes) {
			if (thrExType.isAssignableFrom(exType)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 处理 HTTP 错误，并抛出 HttpStatusException 异常；
	 * 
	 * @param actionContext
	 * @param response
	 */
	private void processAndThrowHttpException(ServiceActionContext actionContext, CloseableHttpResponse response)
			throws HttpStatusException {
		String content = null;
		if (actionContext.isResolveContentOnHttpError()) {
			try {
				InputStream respStream = response.getEntity().getContent();
				content = (String) StringResponseConverter.INSTANCE.toResponseObject(respStream);
			} catch (UnsupportedOperationException e) {
				throw new HttpServiceException(e.getMessage(), e);
			} catch (IOException e) {
				throw new HttpServiceException(e.getMessage(), e);
			} catch (Exception e) {
				if (e instanceof HttpServiceException) {
					throw (HttpServiceException) e;
				}
				throw new HttpServiceException(e.getMessage(), e);
			}
		}
		throw new HttpStatusException(response.getStatusLine().getStatusCode(), response.getStatusLine().toString(),
				content);
	}

	private InnerRequest resolveRequest(ServiceActionContext actionContext, Object[] args) throws IOException {
		switch (actionContext.getRequestMethod()) {
		case GET:
			return resolveGetRequest(actionContext, args);
		case POST:
			return resolvePostRequest(actionContext, args);
		case PUT:
			return resolvePutRequest(actionContext, args);
		case DELETE:
			return resolveDeleteRequest(actionContext, args);
		default:
			throw new UnsupportedOperationException(
					"Unsupported http method '" + actionContext.getRequestMethod() + "'!");
		}
	}

	/**
	 * 创建请求；
	 * 
	 * @param actionContext
	 * @param args
	 * @return
	 */
	private HttpUriRequest buildRequest(Request request) {
		InputStream bodyStream = null;
		if (request.getBody() != null) {
			bodyStream = new ByteArrayInputStream(request.getBody().array());
		}
		switch (request.getMethod()) {
		case GET:
			return new HttpGet(request.getUri());
		case POST:
			HttpPost httppost = new HttpPost(request.getUri());
			// 查询参数以 post body 方式提交；
			HttpEntity postEntity = new InputStreamEntity(bodyStream);
			httppost.setEntity(postEntity);
			return httppost;
		case PUT:
			HttpPut httpput = new HttpPut(request.getUri());
			// 查询参数以 put body 方式提交；
			HttpEntity putEntity = new InputStreamEntity(bodyStream);
			httpput.setEntity(putEntity);
			return httpput;
		case DELETE:
			// HttpDelete httpDelete = new HttpDelete(uri);
			LocalHttpDelete httpDelete = new LocalHttpDelete(request.getUri());
			// 查询参数以 delete body 方式提交
			HttpEntity entity = new InputStreamEntity(bodyStream);
			httpDelete.setEntity(entity);
			return httpDelete;
		default:
			throw new UnsupportedOperationException(
					"Unsupported http method '" + request.getMethod() + "'!");
		}
	}

	/**
	 * 设置http请求头的Authorization属性
	 * 
	 * @param request
	 * @param setting
	 */
	private void buildAuthorization(HttpUriRequest request, Authorization setting) {
		StringBuilder authBuilder = new StringBuilder();
		authBuilder.append(setting.getAlg()).append(" ").append(setting.getSenderName()).append(":")
				.append(setting.getSecretKey());
		request.addHeader("Authorization", authBuilder.toString());
	}

	/**
	 * 创建 http post 请求；
	 * 
	 * @param actionContext
	 * @param args
	 * @return
	 * @throws IOException 
	 */
	private InnerRequest resolvePostRequest(ServiceActionContext actionContext, Object[] args) throws IOException {
		Map<String, String> pathParams = actionContext.getPathParamResolver().resolve(args);
		Properties queryParams = actionContext.getRequestParamResolver().resolve(args);
		InputStream inputStream = actionContext.getRequestBodyResolver().resolve(args);
		URI uri = actionContext.getPathTemplate().generateRequestURI(pathParams, queryParams,
				ServiceActionContext.DEFAULT_CHARSET);
		byte[] bytes = BytesUtils.copyToBytes(inputStream);
		return new InnerRequest(HttpMethod.POST, uri, ByteBuffer.wrap(bytes));
	}

	/**
	 * 创建http get请求
	 * 
	 * @param actionContext
	 * @param args
	 * @return
	 */
	private InnerRequest resolveGetRequest(ServiceActionContext actionContext, Object[] args) {
		Map<String, String> pathParams = actionContext.getPathParamResolver().resolve(args);
		Properties queryParams = actionContext.getRequestParamResolver().resolve(args);
		URI uri = actionContext.getPathTemplate().generateRequestURI(pathParams, queryParams,
				ServiceActionContext.DEFAULT_CHARSET);
		return new InnerRequest(HttpMethod.GET, uri, null);
	}

	/**
	 * 创建http put请求
	 * 
	 * @param actionContext
	 * @param args
	 * @return
	 * @throws IOException 
	 */
	private InnerRequest resolvePutRequest(ServiceActionContext actionContext, Object[] args) throws IOException {
		Map<String, String> pathParams = actionContext.getPathParamResolver().resolve(args);
		Properties queryParams = actionContext.getRequestParamResolver().resolve(args);
		InputStream inputStream = actionContext.getRequestBodyResolver().resolve(args);
		URI uri = actionContext.getPathTemplate().generateRequestURI(pathParams, queryParams,
				ServiceActionContext.DEFAULT_CHARSET);
		byte[] bytes = BytesUtils.copyToBytes(inputStream);
		return new InnerRequest(HttpMethod.PUT, uri, ByteBuffer.wrap(bytes));
	}

	/**
	 * 创建http delete请求
	 * 
	 * @param actionContext
	 * @param args
	 * @return
	 * @throws IOException 
	 */
	private InnerRequest resolveDeleteRequest(ServiceActionContext actionContext, Object[] args) throws IOException {
		Map<String, String> pathParams = actionContext.getPathParamResolver().resolve(args);
		Properties queryParams = actionContext.getRequestParamResolver().resolve(args);
		InputStream inputStream = actionContext.getRequestBodyResolver().resolve(args);
		URI uri = actionContext.getPathTemplate().generateRequestURI(pathParams, queryParams,
				ServiceActionContext.DEFAULT_CHARSET);
		byte[] bytes = BytesUtils.copyToBytes(inputStream);
		return new InnerRequest(HttpMethod.DELETE, uri, ByteBuffer.wrap(bytes));
	}

	/**
	 * 创建SSL安全连接
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	private SSLConnectionSocketFactory createSSLConnectionSocketFactory()
			throws NoSuchAlgorithmException, KeyManagementException {
		SSLConnectionSocketFactory sslsf = null;
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, new TrustManager[] { trustManager }, null);
		sslsf = new SSLConnectionSocketFactory(context, NoopHostnameVerifier.INSTANCE);
		return sslsf;
	}

	/**
	 * 重新验证方法，取消SSL验证（信任所有证书）
	 */
	private static TrustManager trustManager = new X509TrustManager() {

		@Override
		public void checkClientTrusted(X509Certificate[] ax509certificate, String s) throws CertificateException {
			// TODO Auto-generated method stub

		}

		@Override
		public void checkServerTrusted(X509Certificate[] ax509certificate, String s) throws CertificateException {
			// TODO Auto-generated method stub

		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

	};

	private static class InnerRequest implements Request {

		private HttpMethod method;

		private URI uri;

		private ByteBuffer body;
		
		private Properties headers = new Properties();

		public InnerRequest(HttpMethod method, URI uri, ByteBuffer body) {
			this.method = method;
			this.uri = uri;
			this.body = body;
		}

		/* (non-Javadoc)
		 * @see cn.bubi.baas.utils.http.agent.Request#getMethod()
		 */
		@Override
		public HttpMethod getMethod() {
			return method;
		}

		/* (non-Javadoc)
		 * @see cn.bubi.baas.utils.http.agent.Request#getUri()
		 */
		@Override
		public URI getUri() {
			return uri;
		}

		/* (non-Javadoc)
		 * @see cn.bubi.baas.utils.http.agent.Request#getBody()
		 */
		@Override
		public ByteBuffer getBody() {
			return body;
		}

		@Override
		public void setHeader(String name, String value) {
			headers.setProperty(name, value);
		}
		
		public Properties getCustomHeaders(){
			return headers;
		}

	}
}
