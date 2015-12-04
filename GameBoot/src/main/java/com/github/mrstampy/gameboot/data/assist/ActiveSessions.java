package com.github.mrstampy.gameboot.data.assist;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.github.mrstampy.gameboot.data.entity.UserSession;

@Component
public class ActiveSessions {

	private Map<String, Long> sessions = new ConcurrentHashMap<>();

	public void addSession(UserSession session) {
		sessions.put(session.getUser().getUserName(), session.getId());
	}

	public boolean hasSession(String userName) {
		return sessions.containsKey(userName);
	}

	public boolean hasSession(long id) {
		return sessions.containsValue(id);
	}

	public void removeSession(UserSession session) {
		sessions.remove(session.getUser().getUserName());
	}

	public Collection<Long> getSessionIds() {
		return sessions.values();
	}
}
