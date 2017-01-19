package cn.hq.utils.http.converters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.util.StreamUtils;

import cn.hq.utils.http.ResponseConverter;

public class ByteArrayResponseConverter implements ResponseConverter {

	public static final ByteArrayResponseConverter INSTANCE = new ByteArrayResponseConverter();

	private ByteArrayResponseConverter() {
	}

	@Override
	public Object toResponseObject(InputStream responseStream) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamUtils.copy(responseStream, out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}
