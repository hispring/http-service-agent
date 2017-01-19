package cn.hq.utils.http.agent;

import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.util.ClassUtils;

import cn.hq.utils.http.RequestBodyConverter;
import cn.hq.utils.http.converters.ByteArrayBodyConverter;
import cn.hq.utils.http.converters.InputStreamBodyConverter;
import cn.hq.utils.http.converters.JsonBodyConverter;
import cn.hq.utils.http.converters.ObjectToStringBodyConverter;
import cn.hq.utils.io.EmptyInputStream;

/**
 * 类型自动匹配的 RequestBody 转换器；
 * @author haiq
 *
 */
class TypeAutoAdapterBodyConverter implements RequestBodyConverter{
	
	private static final RequestBodyConverter OBJECT_TO_STRING_CONVERTER = new ObjectToStringBodyConverter();
	private static final RequestBodyConverter INPUT_STREAM_CONVERTER = new InputStreamBodyConverter();
	private static final RequestBodyConverter BYTES_CONVERTER = new ByteArrayBodyConverter();
	private static final RequestBodyConverter JSON_CONVERTER = new JsonBodyConverter();
	
	private RequestBodyConverter converter;
	
	public TypeAutoAdapterBodyConverter(Class<?> argType) {
		converter = createConverter(argType);
	}
	
	private RequestBodyConverter createConverter(Class<?> argType){
		if (ClassUtils.isAssignable(InputStream.class, argType)) {
			return INPUT_STREAM_CONVERTER;
		}
		if (ClassUtils.isAssignable(String.class, argType)) {
			return OBJECT_TO_STRING_CONVERTER;
		}
		if (ClassUtils.isAssignable(byte[].class, argType)) {
			return BYTES_CONVERTER;
		}
		if (ClassUtils.isPrimitiveOrWrapper(argType)) {
			return OBJECT_TO_STRING_CONVERTER;
		}
		if (ClassUtils.isAssignable(OutputStream.class, argType)) {
			throw new IllegalHttpServiceDefinitionException("Unsupported type for the request body argument!");
		}
		//默认按照 JSON 方式返回；
		return JSON_CONVERTER;
	}

	@Override
	public InputStream toInputStream(Object param) {
		if (param == null) {
			return EmptyInputStream.INSTANCE;
		}
		return converter.toInputStream(param);
	}

}
