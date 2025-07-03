package com.chat_server.common.propertis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "custom.auth.header")
public class AuthHeaderProperties {
    private String tokenExpiration;

    public String getTokenExpiration() {
        return tokenExpiration;
    }

    public void setTokenExpiration(String tokenExpiration) {
        this.tokenExpiration = tokenExpiration;
    }
}
