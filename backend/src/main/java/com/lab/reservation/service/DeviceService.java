package com.lab.reservation.service;

import com.lab.reservation.entity.Device;
import com.lab.reservation.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public List<Device> listAll() {
        return deviceRepository.findAll();
    }

    public Device create(Device device) {
        device.setId(null);
        return deviceRepository.save(device);
    }

    public Device update(Long id, Device input) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        device.setName(input.getName());
        device.setModel(input.getModel());
        device.setLocation(input.getLocation());
        device.setStatus(input.getStatus());
        device.setDescription(input.getDescription());
        return deviceRepository.save(device);
    }

    public void delete(Long id) {
        deviceRepository.deleteById(id);
    }
}
