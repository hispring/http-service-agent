package cn.hq.utils.http.converters;

import com.alibaba.fastjson.JSON;

import cn.hq.utils.http.StringConverter;

/**
 * JSON 格式的参数转换器；
 * 
 * @author haiq
 *
 */
public class JsonConverter implements StringConverter {

	@Override
	public String toString(Object obj) {
		// TODO:未定义“日期时间”的输出格式 ；
		return JSON.toJSONString(obj);
	}

}
