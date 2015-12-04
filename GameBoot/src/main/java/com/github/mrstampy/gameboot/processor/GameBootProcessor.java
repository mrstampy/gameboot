package com.github.mrstampy.gameboot.processor;

import com.github.mrstampy.gameboot.messages.AbstractGameBootMessage;
import com.github.mrstampy.gameboot.messages.MessageType;

public interface GameBootProcessor<M extends AbstractGameBootMessage> {
	
	MessageType getType();

	AbstractGameBootMessage process(M message) throws Exception;
}
