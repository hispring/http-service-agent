package cn.hq.utils.http.converters;

import java.io.InputStream;

import cn.hq.utils.http.RequestBodyConverter;

public class InputStreamBodyConverter implements RequestBodyConverter{

	@Override
	public InputStream toInputStream(Object param) {
		return (InputStream)param;
	}

}
