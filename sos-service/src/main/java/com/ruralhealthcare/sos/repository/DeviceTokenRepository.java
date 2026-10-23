package com.ruralhealthcare.sos.repository;

import com.ruralhealthcare.sos.domain.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByUserIdAndFcmToken(UUID userId, String fcmToken);

    Optional<DeviceToken> findByFcmToken(String fcmToken);

    List<DeviceToken> findByUserIdAndActiveTrue(UUID userId);

    List<DeviceToken> findByUserIdInAndActiveTrue(Collection<UUID> userIds);

    List<DeviceToken> findByUserIdInAndActiveTrueAndEmergencyAlertOptInTrue(Collection<UUID> userIds);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.fcmToken = :fcmToken")
    void deleteByFcmToken(@Param("fcmToken") String fcmToken);

    @Query("SELECT d FROM DeviceToken d WHERE d.emergencyAlertOptIn = true AND d.active = true AND d.userId <> :excludeUserId AND d.userType <> com.ruralhealthcare.sos.domain.UserType.DOCTOR")
    List<DeviceToken> findOptedInEmergencyHelpers(@Param("excludeUserId") UUID excludeUserId);
}
