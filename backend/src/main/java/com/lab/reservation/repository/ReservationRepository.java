package com.lab.reservation.repository;

import com.lab.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    List<Reservation> findAllByOrderByCreatedAtDesc();

    Optional<Reservation> findByProcessInstanceId(String processInstanceId);

    long countByApplicantId(Long applicantId);

    /** 查询某设备在指定时间段内与 [start, end) 重叠的预约(用于冲突检测 / 占用时段) */
    @Query("select r from Reservation r where r.device.id = :deviceId "
            + "and r.status in :statuses "
            + "and r.startTime < :end and r.endTime > :start")
    List<Reservation> findOverlapping(@Param("deviceId") Long deviceId,
                                      @Param("statuses") List<Reservation.Status> statuses,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /** 同上,但排除指定预约(修改预约时排除自身) */
    @Query("select r from Reservation r where r.device.id = :deviceId "
            + "and r.status in :statuses "
            + "and r.startTime < :end and r.endTime > :start "
            + "and r.id <> :excludeId")
    List<Reservation> findOverlappingExcluding(@Param("deviceId") Long deviceId,
                                               @Param("statuses") List<Reservation.Status> statuses,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end,
                                               @Param("excludeId") Long excludeId);
}
