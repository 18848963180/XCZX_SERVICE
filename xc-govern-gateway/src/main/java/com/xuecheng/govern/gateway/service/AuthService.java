package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    RedisTemplate redisTemplate;

    public String getTokenFromCookie(HttpServletRequest request) {
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        String access_token = cookieMap.get("uid");
        if (StringUtils.isEmpty(access_token)) {
            return null;
        }
        return access_token;
    }

    public long getExpire(String access_token) {
        //token在redis中的key
        String key = "user_token:" + access_token;
        Long expire = redisTemplate.getExpire(key);
        return expire;
    }

    public String getJwtFromHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)) {
            return null;
        }
        if (!authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization;
    }
}
