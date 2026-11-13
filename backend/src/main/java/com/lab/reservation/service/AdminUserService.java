package com.lab.reservation.service;

import com.lab.reservation.entity.User;
import com.lab.reservation.repository.ReservationRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Map<String, Object>> listUsers() {
        return userRepository.findAllByOrderByIdAsc().stream().map(u -> {
            Map<String, Object> view = new LinkedHashMap<>();
            view.put("id", u.getId());
            view.put("username", u.getUsername());
            view.put("realName", u.getRealName());
            view.put("role", u.getRole().name());
            view.put("enabled", u.isEnabled());
            return view;
        }).toList();
    }

    @Transactional
    public void setEnabled(Long operatorId, Long userId, boolean enabled) {
        User user = findUser(userId);
        if (!enabled) {
            if (user.getId().equals(operatorId)) {
                throw new IllegalStateException("不能禁用当前登录的账号");
            }
            if (user.getRole() == User.Role.ADMIN
                    && userRepository.countByRoleAndEnabledIsTrue(User.Role.ADMIN) <= 1) {
                throw new IllegalStateException("至少需要保留一个可用的管理员账号");
            }
        }
        user.setEnabled(enabled);
    }

    @Transactional
    public void resetPassword(Long userId, String password) {
        if (password == null || password.trim().length() < 6) {
            throw new IllegalArgumentException("新密码至少 6 位");
        }
        findUser(userId).setPassword(passwordEncoder.encode(password.trim()));
    }

    @Transactional
    public void deleteUser(Long operatorId, Long userId) {
        User user = findUser(userId);
        if (user.getId().equals(operatorId)) {
            throw new IllegalStateException("不能删除当前登录的账号");
        }
        if (user.getRole() == User.Role.ADMIN && userRepository.countByRole(User.Role.ADMIN) <= 1) {
            throw new IllegalStateException("至少需要保留一个管理员账号");
        }
        if (reservationRepository.countByApplicantId(userId) > 0) {
            throw new IllegalStateException("该用户存在预约记录,请先禁用账号而非删除");
        }
        userRepository.delete(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
