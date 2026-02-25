package com.jettech.code.service;

import com.jettech.code.entity.ServiceEntity;
import com.jettech.code.entity.ServiceVersion;
import com.jettech.code.mapper.ServiceMapper;
import com.jettech.code.mapper.ServiceVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceEntityService {
    private final ServiceMapper serviceMapper;
    private final ServiceVersionMapper serviceVersionMapper;

    public List<ServiceEntity> findByApplicationId(Long applicationId) {
        return serviceMapper.findByApplicationId(applicationId);
    }

    public ServiceEntity getById(Long id) {
        return serviceMapper.findById(id);
    }

    public List<ServiceVersion> getVersionsByServiceId(Long serviceId) {
        return serviceVersionMapper.findByServiceId(serviceId);
    }

    @Transactional
    public ServiceEntity create(ServiceEntity service) {
        service.setCreatedAt(LocalDateTime.now());
        service.setUpdatedAt(LocalDateTime.now());
        serviceMapper.insert(service);
        return service;
    }

    @Transactional
    public ServiceEntity update(ServiceEntity service) {
        service.setUpdatedAt(LocalDateTime.now());
        serviceMapper.update(service);
        return service;
    }

    @Transactional
    public void deleteById(Long id) {
        serviceVersionMapper.deleteByServiceId(id);
        serviceMapper.deleteById(id);
    }

    public void deleteByApplicationId(Long applicationId) {
        List<ServiceEntity> services = serviceMapper.findByApplicationId(applicationId);
        for (ServiceEntity service : services) {
            serviceVersionMapper.deleteByServiceId(service.getId());
        }
        serviceMapper.deleteByApplicationId(applicationId);
    }
}
