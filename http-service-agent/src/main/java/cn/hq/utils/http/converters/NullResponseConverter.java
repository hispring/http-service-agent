package cn.hq.utils.http.converters;

import java.io.InputStream;

import cn.hq.utils.http.ResponseConverter;

public class NullResponseConverter implements ResponseConverter {
	
	public static final ResponseConverter INSTANCE = new NullResponseConverter();
	
	private NullResponseConverter() {
	}

	@Override
	public Object toResponseObject(InputStream responseStream) {
		return null;
	}

}
