package com.lab.reservation.repository;

import com.lab.reservation.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFlagFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.userId = :userId and n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}
