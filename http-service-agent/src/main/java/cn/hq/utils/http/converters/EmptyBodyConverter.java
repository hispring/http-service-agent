package cn.hq.utils.http.converters;

import java.io.InputStream;

import cn.hq.utils.http.RequestBodyConverter;
import cn.hq.utils.io.EmptyInputStream;

public class EmptyBodyConverter implements RequestBodyConverter{

	@Override
	public InputStream toInputStream(Object param) {
		return EmptyInputStream.INSTANCE;
	}

}
