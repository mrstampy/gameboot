package com.github.mrstampy.gameboot.data.entity.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.github.mrstampy.gameboot.data.entity.User;
import com.github.mrstampy.gameboot.data.entity.UserSession;

public interface UserSessionRepository extends CrudRepository<UserSession, Long> {

	UserSession findByUserAndEndedIsNull(User user);

	UserSession findByIdAndEndedIsNull(Long id);

	@Query("SELECT us FROM UserSession us JOIN FETCH us.user WHERE us.ended is null")
	List<UserSession> findByEndedIsNull();
}
