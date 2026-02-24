package com.example.medaiassistant.repository;

import com.example.medaiassistant.model.ServerConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerConfigurationRepository extends JpaRepository<ServerConfiguration, Integer> {
    ServerConfiguration findByConfigName(String configName);
}
