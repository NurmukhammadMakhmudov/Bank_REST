package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.UserAuthProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u.id AS id, u.username AS username, u.password AS password, u.status AS status, u.role AS role FROM User u WHERE u.username = :username")
    Optional<UserAuthProjection> findAuthByUsername(@Param("username") String username);

    int countUserByUsername(@Param("username") String username);

    Optional<User> findByUsername(String username);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.username = :username")
    boolean existsByUsername(@Param("username") String username);

}
