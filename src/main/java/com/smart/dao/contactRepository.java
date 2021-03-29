package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contact;
import com.smart.entities.User;

public interface contactRepository extends JpaRepository<Contact, Integer> {
//pagination
	@Query("from Contact as c where c.user.id =:userId and c.added = 1")
	// current page-page
	// contact per page-5
	public Page<Contact> findContactByUser(@Param("userId") int userId, Pageable pageable);
	
	//pagination
		@Query("from Contact as c where c.user.id =:userId and c.added = 0")
		// current page-page
		// contact per page-5
		public Page<Contact> findContactByUserRequest(@Param("userId") int userId, Pageable pageable);

	// search
	public List<Contact> findByNameContainingAndUser(String name, User user);

	public Contact findByPhoneContainingAndUser(String phone, User user);
}
