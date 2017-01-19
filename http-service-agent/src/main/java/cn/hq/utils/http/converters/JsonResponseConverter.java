package cn.hq.utils.http.converters;

import java.io.InputStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.hq.utils.http.ResponseConverter;

public class JsonResponseConverter implements ResponseConverter {

	private Class<?> clazz;

	public JsonResponseConverter(Class<?> clazz) {
		this.clazz = clazz;
	}

	@Override
	public Object toResponseObject(InputStream responseStream) throws Exception {
		String jsonResponse = (String) StringResponseConverter.INSTANCE.toResponseObject(responseStream);
		if (jsonResponse == null) {
			return null;
		}
		jsonResponse = jsonResponse.trim();
		// TODO: 未指定“日期时间”格式的策略；
		return JSON.toJavaObject(JSONObject.parseObject(jsonResponse), clazz);
	}

}
