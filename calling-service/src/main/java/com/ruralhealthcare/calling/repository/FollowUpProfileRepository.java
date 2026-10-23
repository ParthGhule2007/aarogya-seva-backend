package com.ruralhealthcare.calling.repository;

import com.ruralhealthcare.calling.domain.FollowUpProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FollowUpProfileRepository extends JpaRepository<FollowUpProfile, UUID> {

    Optional<FollowUpProfile> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    void deleteByPhoneNumber(String phoneNumber);
}
