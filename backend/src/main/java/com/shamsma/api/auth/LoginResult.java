package com.shamsma.api.auth;

import com.shamsma.api.auth.dto.AuthResponse;

record LoginResult(AuthResponse response, String refreshToken) {}
