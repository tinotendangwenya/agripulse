package com.agripulse.backend.account;

import com.agripulse.backend.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAdminRepository extends Repository<User, Long> {
    @Query("""
        select u from User u
        where :q = '' or lower(u.fullName) like lower(concat('%', :q, '%'))
           or lower(u.email) like lower(concat('%', :q, '%'))
           or u.phone like concat('%', :q, '%')
        order by u.id desc
        """)
    List<User> search(@Param("q") String q, Pageable pageable);
}
