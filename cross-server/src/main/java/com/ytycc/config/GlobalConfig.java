package com.ytycc.config;

import com.ytycc.dispatch.ForwardServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GlobalConfig {

    @Value("${forwardPort}")
    private Integer forwardPort;

    @Bean("forwardServer")
    public ForwardServer forwardServer() {
        return new ForwardServer(forwardPort);
    }
}
