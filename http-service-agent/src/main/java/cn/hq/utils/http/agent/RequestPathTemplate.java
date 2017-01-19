package cn.hq.utils.http.agent;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import cn.hq.utils.PathUtils;

class RequestPathTemplate {
	private ServerConfiguration serverConfig;
	private String servicePath;
	private String actionPath;

	public RequestPathTemplate(ServerConfiguration serverConfig, String servicePath, String actionPath) {
		this.serverConfig = serverConfig;
		this.servicePath = PathUtils.standardize(servicePath);
		this.actionPath = PathUtils.standardize(actionPath);
	}
	

	/**
	 * 更新请求路径；
	 * 
	 * @param pathVariableName
	 * @param value
	 */
	private static String updateActionPath(String actionPath, String pathVariableName, String value) {
		String pathVarName = String.format("{%s}", pathVariableName);
		actionPath = actionPath.replace(pathVarName, value);
		return actionPath;
	}

	/**
	 * 返回完整的请求URL；
	 * 
	 * @param pathParams
	 *            路径参数；
	 * @param queryParams
	 *            查询参数；
	 * @return
	 */
	public URI generateRequestURI(Map<String, String> pathParams, Properties queryParams,
			Charset encodingCharset) {
		// 生成路径；
		String reallyActionPath = createActionPath(pathParams);
		String path = PathUtils.concatPaths(serverConfig.getContextPath(), servicePath, reallyActionPath);
		path = PathUtils.absolute(path);

		// 生成查询字符串；
		URIBuilder uriBuilder = new URIBuilder();
		uriBuilder.setCharset(encodingCharset);
		if (serverConfig.isHttps()) {
			uriBuilder.setScheme("https");
		}else{
			uriBuilder.setScheme("http");
		}
		
		uriBuilder.setHost(serverConfig.getHost());
		uriBuilder.setPort(serverConfig.getPort());
		uriBuilder.setPath(path.toString());
		List<NameValuePair> queryParameters = RequestUtils.createQueryParameters(queryParams);
		uriBuilder.setParameters(queryParameters);
		try {
			return uriBuilder.build();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		}
	}

	private String createActionPath(Map<String, String> pathParams) {
		String reallyActionPath = actionPath;
		if (pathParams != null) {
			for (Entry<String, String> pathParam : pathParams.entrySet()) {
				reallyActionPath = updateActionPath(reallyActionPath, pathParam.getKey(), pathParam.getValue());
			}
		}
		return reallyActionPath;
	}

}
