package com.github.mrstampy.gameboot.data.entity.repository;

import org.springframework.data.repository.CrudRepository;

import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.UserSession;

public interface UserSessionRepository extends CrudRepository<UserSession, Long> {

	UserSession findByUserAndEndedIsNull(User user);

	UserSession findByIdAndEndedIsNull(Long id);
}
