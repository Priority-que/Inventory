package com.xixi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "python-agent")
public class PythonAgentProperties {
    private String baseUrl = "http://127.0.0.1:8000";
    private int timeoutMs = 60000;
}
