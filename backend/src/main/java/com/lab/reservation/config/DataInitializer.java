package com.lab.reservation.config;

import com.lab.reservation.entity.Device;
import com.lab.reservation.entity.User;
import com.lab.reservation.repository.DeviceRepository;
import com.lab.reservation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** 首次启动初始化测试账号与示例设备,便于验收演示 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            createUser("student1", "张同学", User.Role.STUDENT);
            createUser("teacher1", "李老师", User.Role.TEACHER);
            createUser("admin", "管理员", User.Role.ADMIN);
        }
        if (deviceRepository.count() == 0) {
            createDevice("示波器", "DS1054Z", "实验楼 A-301");
            createDevice("3D 打印机", "Ender-3 V3", "实验楼 B-102");
            createDevice("高性能工作站", "ThinkStation P620", "实验楼 A-405");
        }
    }

    private void createUser(String username, String realName, User.Role role) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        userRepository.save(u);
    }

    private void createDevice(String name, String model, String location) {
        Device d = new Device();
        d.setName(name);
        d.setModel(model);
        d.setLocation(location);
        d.setStatus(Device.Status.AVAILABLE);
        deviceRepository.save(d);
    }
}
