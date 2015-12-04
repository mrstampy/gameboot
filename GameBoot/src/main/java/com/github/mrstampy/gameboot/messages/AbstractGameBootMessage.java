package com.github.mrstampy.gameboot.messages;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public abstract class AbstractGameBootMessage {

	private Integer id;

	private MessageType type;

	protected AbstractGameBootMessage(MessageType type) {
		setType(type);
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
