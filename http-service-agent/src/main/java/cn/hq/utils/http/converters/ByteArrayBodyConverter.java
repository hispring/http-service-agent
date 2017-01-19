package cn.hq.utils.http.converters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import cn.hq.utils.http.RequestBodyConverter;

public class ByteArrayBodyConverter implements RequestBodyConverter{

	@Override
	public InputStream toInputStream(Object param) {
		byte[] bytes = (byte[])param;
		return new ByteArrayInputStream(bytes);
	}

}
