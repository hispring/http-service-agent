package cn.hq.utils.http.converters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.alibaba.fastjson.JSON;

import cn.hq.utils.http.HttpServiceConsts;
import cn.hq.utils.http.RequestBodyConverter;

public class JsonBodyConverter implements RequestBodyConverter {

	@Override
	public InputStream toInputStream(Object param) {
		String jsonString = JSON.toJSONString(param);
		try {
			return new ByteArrayInputStream(jsonString.getBytes(HttpServiceConsts.CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
