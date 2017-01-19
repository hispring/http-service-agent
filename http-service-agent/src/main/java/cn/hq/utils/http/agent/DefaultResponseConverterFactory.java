package cn.hq.utils.http.agent;

import java.lang.reflect.Method;

import cn.hq.utils.http.HttpAction;
import cn.hq.utils.http.ResponseConverter;
import cn.hq.utils.http.ResponseConverterFactory;
import cn.hq.utils.http.converters.ByteArrayResponseConverter;
import cn.hq.utils.http.converters.JsonResponseConverter;
import cn.hq.utils.http.converters.StringResponseConverter;

public class DefaultResponseConverterFactory implements ResponseConverterFactory {
	
	public static final DefaultResponseConverterFactory INSTANCE = new DefaultResponseConverterFactory();
	
	private DefaultResponseConverterFactory() {
	}

	@Override
	public ResponseConverter createResponseConverter(HttpAction actionDef, Method mth) {
		Class<?> retnClazz = mth.getReturnType();
		// create default response converter;
		if (byte[].class == retnClazz) {
			return ByteArrayResponseConverter.INSTANCE;
		}
		if (String.class == retnClazz) {
			return StringResponseConverter.INSTANCE;
		}
		
		// TODO:未处理 基本类型、输入输出流；
		return new JsonResponseConverter(retnClazz);
	}

}
