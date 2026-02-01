package kr.java.aibe4_finalproject_team04_test.repository;

import kr.java.aibe4_finalproject_team04_test.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}