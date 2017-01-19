package cn.hq.utils.http.agent;

import cn.hq.utils.http.RequestBodyConverter;

class RequestBodyDefinition {
	
	private boolean required;
	
	private RequestBodyConverter converter;
	
	public RequestBodyDefinition(boolean required, RequestBodyConverter converter) {
		this.required = required;
		this.converter = converter;
	}

	public RequestBodyConverter getConverter() {
		return converter;
	}

	public boolean isRequired() {
		return required;
	}
	
}
