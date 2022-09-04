package com.example.demo.repository;

import com.example.demo.entity.VerificationToken;
import com.example.demo.enums.VerificationTypeEnum;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

  Optional<VerificationToken> findByToken(String token);

  VerificationToken findFirstByAccount_IdAndVerificationTypeOrderByIdDesc(
      Long accountId, VerificationTypeEnum verificationTypeEnum);

  VerificationToken findFirstByAccount_IdAndVerificationTypeOrderByExpiryDateInUtcDesc(
      Long accountId, VerificationTypeEnum verificationTypeEnum);
}
