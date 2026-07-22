package com.shamsma.api.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "app.jwt")
record JwtKeyProperties(Resource privateKeyPath, Resource publicKeyPath) {}
