package com.github.mrstampy.gameboot.messages;

public class Response extends AbstractGameBootMessage {

	public enum ResponseCode {
		SUCCESS, FAILURE, WARNING, INFO, ALERT
	}

	private ResponseCode responseCode;

	private Object response;

	public Response() {
		super(MessageType.RESPONSE);
	}

	public Response(ResponseCode responseCode) {
		this();

		setResponseCode(responseCode);
	}

	public Response(ResponseCode responseCode, Object response) {
		this(responseCode);

		setResponse(response);
	}

	public ResponseCode getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(ResponseCode responseCode) {
		this.responseCode = responseCode;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

}
