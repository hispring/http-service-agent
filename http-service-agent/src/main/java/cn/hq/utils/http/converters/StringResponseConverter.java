package cn.hq.utils.http.converters;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import cn.hq.utils.http.HttpServiceConsts;
import cn.hq.utils.http.HttpServiceException;
import cn.hq.utils.http.ResponseConverter;

/**
 * 返回字符的转换器；
 * 
 * @author haiq
 *
 */
public class StringResponseConverter implements ResponseConverter {
	
	public static final ResponseConverter INSTANCE = new StringResponseConverter();
	
	private StringResponseConverter() {
	}

	@Override
	public Object toResponseObject(InputStream responseStream) {
		String responseText = readString(responseStream);
		return responseText;
	}

	private String readString(InputStream in) {
		InputStreamReader reader = null;
		try {
			reader = new InputStreamReader(in, HttpServiceConsts.CHARSET);
			
			StringBuilder text = new StringBuilder();
			char[] buffer = new char[256];
			int len = 0;
			while((len = reader.read(buffer)) > 0){
				text.append(buffer, 0, len);
			}
			return text.toString();
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage(), e);
		} catch (IOException e) {
			throw new HttpServiceException(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore exception thrown again;
				}
			}
		}
	}

}
