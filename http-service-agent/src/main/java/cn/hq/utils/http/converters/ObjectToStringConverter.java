package cn.hq.utils.http.converters;

import cn.hq.utils.http.StringConverter;

public class ObjectToStringConverter implements StringConverter {

	@Override
	public String toString(Object param) {
		return param == null ? null : param.toString();
	}

}
