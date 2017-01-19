package test.cn.hq.utils.http.agent;

import java.io.InputStream;

import cn.hq.utils.http.ResponseConverter;
import cn.hq.utils.http.converters.JsonResponseConverter;

public class GetContentResponseConverter implements ResponseConverter {

	private JsonResponseConverter jsonResponseConverter = new JsonResponseConverter(DataResponse.class);

	@Override
	public Object toResponseObject(InputStream responseStream) throws Exception {
		DataResponse data = (DataResponse) jsonResponseConverter.toResponseObject(responseStream);
		return data.getContent();
	}

}
