package com.github.mrstampy.gameboot.data.entity.repository;

import org.springframework.data.repository.CrudRepository;

import com.github.mrstampy.gameboot.data.entity.User;

public interface UserRepository extends CrudRepository<User, Integer> {

	User findByUserName(String userName);
	
}
