package com.github.mrstampy.gameboot.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Response extends AbstractGameBootMessage {

	public enum ResponseCode {
		SUCCESS, FAILURE, WARNING, INFO, ALERT
	}

	private ResponseCode responseCode;

	private Object[] response;

	public Response() {
		super(MessageType.RESPONSE);
	}

	public Response(ResponseCode responseCode, Object... response) {
		this();
		setResponseCode(responseCode);
		setResponse(response);
	}

	public ResponseCode getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}

	public Object[] getResponse() {
		return response;
	}

	public void setResponse(Object... response) {
		this.response = response;
	}

	@JsonIgnore
	public boolean isSuccess() {
		return isResponseCode(ResponseCode.SUCCESS);
	}

	private boolean isResponseCode(ResponseCode rc) {
		return rc == getResponseCode();
	}

}
