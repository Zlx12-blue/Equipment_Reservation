package com.lab.reservation.controller;

import com.lab.reservation.dto.ApiResponse;
import com.lab.reservation.entity.Device;
import com.lab.reservation.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /** 所有角色均可查看设备列表 */
    @GetMapping
    public ApiResponse<List<Device>> list() {
        return ApiResponse.ok(deviceService.listAll());
    }

    /** 设备增删改仅管理员可操作 */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Device> create(@RequestBody Device device) {
        return ApiResponse.ok(deviceService.create(device));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Device> update(@PathVariable Long id, @RequestBody Device device) {
        return ApiResponse.ok(deviceService.update(id, device));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        deviceService.delete(id);
        return ApiResponse.ok();
    }
}
