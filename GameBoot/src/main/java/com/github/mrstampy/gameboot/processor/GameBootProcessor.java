package com.github.mrstampy.gameboot.processor;

import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.MessageType;
import com.github.mrstampy.gameboot.messages.Response;

public interface GameBootProcessor<M extends AbstractGameBootMessage> {

	MessageType getType();

	Response process(M message) throws Exception;
}
