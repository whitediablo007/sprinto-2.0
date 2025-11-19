package ru.get.tms.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.User;

public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

  @Query("SELECT * FROM users WHERE email = :email")
  Mono<User> findByEmail(String email);

  @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
  Mono<Boolean> existsByEmail(String email);
}
