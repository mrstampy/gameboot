package com.github.mrstampy.gameboot.processor;

import java.lang.invoke.MethodHandles;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.Response;
import com.github.mrstampy.gameboot.messages.Response.ResponseCode;

public abstract class AbstractGameBootProcessor<M extends AbstractGameBootMessage> implements GameBootProcessor<M> {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Transactional
	@Override
	public final Response process(M message) throws Exception {
		log.debug("Processing message {}", message);

		validate(message);

		Response response = processImpl(message);
		response.setId(message.getId());

		log.debug("Created response {} for {}", response, message);

		return response;
	}

	protected void fail(String message) {
		throw new RuntimeException(message);
	}

	protected Response success(Object... message) {
		return new Response(ResponseCode.SUCCESS, message);
	}

	protected Response failure(Object... message) {
		return new Response(ResponseCode.FAILURE, message);
	}

	protected abstract void validate(M message) throws Exception;

	protected abstract Response processImpl(M message) throws Exception;

}
