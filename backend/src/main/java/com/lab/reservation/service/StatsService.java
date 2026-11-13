package com.lab.reservation.service;

import com.lab.reservation.entity.Reservation;
import com.lab.reservation.entity.User;
import com.lab.reservation.repository.DeviceRepository;
import com.lab.reservation.repository.ReservationRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    /** 管理端统计:总量、状态分布、近 7 天趋势、热门设备 */
    public Map<String, Object> overview() {
        List<Reservation> all = reservationRepository.findAll();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        long approved = byStatus.getOrDefault("APPROVED", 0L);
        long rejected = byStatus.getOrDefault("REJECTED", 0L);
        long finished = approved + rejected;

        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = users.stream()
                .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReservations", all.size());
        stats.put("pendingCount", byStatus.getOrDefault("PENDING", 0L));
        stats.put("approvedCount", approved);
        stats.put("rejectedCount", rejected);
        stats.put("cancelledCount", byStatus.getOrDefault("CANCELLED", 0L));
        stats.put("approvalRate", finished == 0 ? 0 : Math.round(approved * 100.0 / finished));
        stats.put("totalUsers", users.size());
        stats.put("studentCount", byRole.getOrDefault(User.Role.STUDENT.name(), 0L));
        stats.put("teacherCount", byRole.getOrDefault(User.Role.TEACHER.name(), 0L));
        stats.put("adminCount", byRole.getOrDefault(User.Role.ADMIN.name(), 0L));
        stats.put("totalDevices", deviceRepository.count());
        stats.put("availableDevices", deviceRepository.findAll().stream()
                .filter(d -> d.getStatus() == com.lab.reservation.entity.Device.Status.AVAILABLE)
                .count());
        stats.put("trend", last7Days(all));
        stats.put("topDevices", topDevices(all));
        return stats;
    }

    private List<Map<String, Object>> last7Days(List<Reservation> all) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long count = all.stream()
                    .filter(r -> r.getCreatedAt().toLocalDate().equals(day))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(fmt));
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> topDevices(List<Reservation> all) {
        return all.stream()
                .collect(Collectors.groupingBy(r -> r.getDevice().getName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();
    }
}
