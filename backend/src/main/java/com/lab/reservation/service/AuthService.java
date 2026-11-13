package com.lab.reservation.service;

import com.lab.reservation.dto.LoginRequest;
import com.lab.reservation.dto.RegisterRequest;
import com.lab.reservation.entity.User;
import com.lab.reservation.repository.UserRepository;
import com.lab.reservation.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!user.isEnabled()) {
            throw new IllegalArgumentException("账号已被禁用,请联系管理员");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "realName", user.getRealName(),
                        "role", user.getRole().name()));
    }

    public User currentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    /** 未登录页注册:仅允许学生 / 教师 */
    public Map<String, Object> register(RegisterRequest request) {
        if (request.getRole() != User.Role.STUDENT && request.getRole() != User.Role.TEACHER) {
            throw new IllegalArgumentException("仅支持注册学生或教师账号");
        }
        return create(request);
    }

    /** 管理员在管理端创建新的管理员账号 */
    public Map<String, Object> registerAdmin(RegisterRequest request) {
        request.setRole(User.Role.ADMIN);
        return create(request);
    }

    private Map<String, Object> create(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("用户名已存在,请更换");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName().trim());
        user.setRole(request.getRole());
        userRepository.save(user);
        return Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "realName", user.getRealName(),
                "role", user.getRole().name());
    }
}
