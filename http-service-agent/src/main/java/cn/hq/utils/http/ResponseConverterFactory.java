package cn.hq.utils.http;

import java.lang.reflect.Method;

public interface ResponseConverterFactory {

	ResponseConverter createResponseConverter(HttpAction actionDef, Method mth);
	
}
