package test.cn.hq.utils.http.agent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hq.utils.http.HttpMethod;

public class HttpRequestCollector extends HttpServlet{

	private static final long serialVersionUID = -8014615357825392276L;
	
	private List<HttpRequestInfo> reqRecords = new LinkedList<HttpRequestInfo>();
	
	private String requestPath = null;
	
	private String authorization = null;
	
	private String responseText = null;
	
	private String requestMethod = null;
	
	private String requestBody = null;
	
	public HttpRequestCollector(String responseText) {
		this.responseText = responseText;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reqRecords.add(new HttpRequestInfo(HttpMethod.GET, req.getParameterMap()));
		requestPath = req.getRequestURI();
		authorization = req.getHeader("Authorization");
		requestMethod = req.getMethod();
		resp.getWriter().print(responseText);
		resp.getWriter().flush();
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		reqRecords.add(new HttpRequestInfo(HttpMethod.POST, req.getParameterMap()));
		requestPath = req.getRequestURI();
		authorization = req.getHeader("Authorization");
		requestMethod = req.getMethod();
		requestBody = readRequestBody(req);
		resp.getWriter().print(responseText);
		resp.getWriter().flush();
	}
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		reqRecords.add(new HttpRequestInfo(HttpMethod.PUT, req.getParameterMap()));
		requestPath = req.getRequestURI();
		authorization = req.getHeader("Authorization");
		requestMethod = req.getMethod();
		requestBody = readRequestBody(req);
		resp.getWriter().print(responseText);
		resp.getWriter().flush();
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
		reqRecords.add(new HttpRequestInfo(HttpMethod.DELETE, req.getParameterMap()));
		requestPath = req.getRequestURI();
		authorization = req.getHeader("Authorization");
		requestMethod = req.getMethod();
		resp.getWriter().print(responseText);
		resp.getWriter().flush();
	}
	
	private String readRequestBody(HttpServletRequest request) throws UnsupportedEncodingException, IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
		String line = null;
		StringBuilder builder = new StringBuilder();
		while((line = reader.readLine()) != null){
			builder.append(line);
		}
		return builder.toString();
	}
	
	
	
	public Iterator<HttpRequestInfo> getRequestRecords(){
		return reqRecords.iterator();
	}
	
	public String getRequestPath(){
		return requestPath;
	}
	
	public String getAuthorization(){
		return authorization;
	}
	
	public String getRequestMethod(){
		return requestMethod;
	}
	
	public String getRequestBody(){
		return requestBody;
	}
	
	public void clearRequestRecords(){
		reqRecords.clear();
		requestPath = null;
	}
}
