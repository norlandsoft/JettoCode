package com.jettech.code.service;

import com.jettech.code.entity.ServiceVersion;
import com.jettech.code.mapper.ServiceVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceVersionService {
    private final ServiceVersionMapper serviceVersionMapper;

    public List<ServiceVersion> findByServiceId(Long serviceId) {
        return serviceVersionMapper.findByServiceId(serviceId);
    }

    public ServiceVersion getById(Long id) {
        return serviceVersionMapper.findById(id);
    }

    public ServiceVersion create(ServiceVersion version) {
        version.setCreatedAt(LocalDateTime.now());
        serviceVersionMapper.insert(version);
        return version;
    }

    public ServiceVersion update(ServiceVersion version) {
        serviceVersionMapper.update(version);
        return version;
    }

    public void deleteById(Long id) {
        serviceVersionMapper.deleteById(id);
    }

    public void deleteByServiceId(Long serviceId) {
        serviceVersionMapper.deleteByServiceId(serviceId);
    }
}
