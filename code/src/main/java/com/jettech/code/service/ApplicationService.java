package com.jettech.code.service;

import com.jettech.code.entity.Application;
import com.jettech.code.mapper.ApplicationMapper;
import com.jettech.code.mapper.ServiceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {
    private final ApplicationMapper applicationMapper;
    private final ServiceMapper serviceMapper;

    public List<Application> findAll() {
        return applicationMapper.findAll();
    }

    public Application getById(Long id) {
        return applicationMapper.findById(id);
    }

    public Application create(Application application) {
        application.setCreatedAt(LocalDateTime.now());
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.insert(application);
        return application;
    }

    public Application update(Application application) {
        application.setUpdatedAt(LocalDateTime.now());
        applicationMapper.update(application);
        return application;
    }

    public void deleteById(Long id) {
        serviceMapper.deleteByApplicationId(id);
        applicationMapper.deleteById(id);
    }
}
