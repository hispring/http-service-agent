package test.cn.hq.utils.http.agent;

import cn.hq.utils.http.StringConverter;

public class CustomBooleanConverter implements StringConverter {

	@Override
	public String toString(Object obj) {
		Boolean value = (Boolean) obj;
		return value.booleanValue() ? "1" : "0";
	}

}
