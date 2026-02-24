package com.example.medaiassistant.controller;

import com.example.medaiassistant.dto.ServerConfigDTO;
import com.example.medaiassistant.model.ServerConfiguration;
import com.example.medaiassistant.service.ServerConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/server-config")
public class ServerConfigController {

    @Autowired
    private ServerConfigService serverConfigService;

    @PostMapping
    public ServerConfiguration saveConfig(@RequestBody ServerConfigDTO configDTO) {
        return serverConfigService.saveOrUpdateConfig(configDTO);
    }

    @GetMapping("/{configName}")
    public ServerConfiguration getConfig(@PathVariable String configName) {
        return serverConfigService.getConfigByName(configName);
    }
}
