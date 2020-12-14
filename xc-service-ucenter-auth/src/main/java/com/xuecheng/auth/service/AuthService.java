package com.xuecheng.auth.service;


import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.exception.ExceptionCast;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    LoadBalancerClient loadBalancerClient;

    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        //申请令牌
        AuthToken authToken = this.applyToken(username, password, clientId, clientSecret);
        //存储令牌到redis
        if (Objects.isNull(authToken)) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }

        String authTokenJsonString = JSON.toJSONString(authToken);
        boolean saveTokenResult = this.setTokenToRedis(authToken.getAccess_token(), authTokenJsonString, tokenValiditySeconds);
        if (!saveTokenResult) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }

    private boolean setTokenToRedis(String access_token, String authTokenJsonString, int tokenValiditySeconds) {
        String key = "user_token:" + access_token;
        redisTemplate.boundValueOps(key).set(authTokenJsonString,
                tokenValiditySeconds, TimeUnit.SECONDS);

        //获取剩余时间
        Long expire = redisTemplate.getExpire(key);
        return expire > 0L;
    }

    private AuthToken applyToken(String username, String password, String clientId, String clientSecret) {
        //客户端实例
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);

        if (Objects.isNull(serviceInstance)) {
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_AUTHSERVER_NOTFOUND);
        }

        //获取令牌的url
        String path = serviceInstance.getUri().toString() + "/auth/oauth/token";

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("username", username);
        formData.add("password", password);
        MultiValueMap<String, String> header = new LinkedMultiValueMap<>();
        header.add("Authorization", this.httpbasic(clientId, clientSecret));

        //处理远程调用的400 401 402  的问题
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if (response.getRawStatusCode() != 400 && response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });

        Map map = null;
        try {
            ResponseEntity<Map> exchange = restTemplate.exchange(path,
                    HttpMethod.POST, new HttpEntity<MultiValueMap<String, String>>(formData, header), Map.class);
            map = exchange.getBody();
        } catch (RestClientException e) {
            e.printStackTrace();
        }
        if (map == null ||
                StringUtils.isEmpty((String) map.get("access_token")) ||
                StringUtils.isEmpty((String) map.get("refresh_token")) ||
                StringUtils.isEmpty((String) map.get("jti"))
        ) {
            String error_description = (String) map.get("error_description");
            if (StringUtils.isNotEmpty(error_description)) {
                if (error_description.equals("坏的凭证")) {
                    ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
                } else if (error_description.indexOf("UserDetailsService returned null") >= 0) {
                    ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
                }
            }

            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }

        AuthToken authToken = new AuthToken();
        authToken.setAccess_token((String) map.get("access_token"));
        authToken.setJwt_token((String) map.get("jti"));
        authToken.setRefresh_token((String) map.get("refresh_token"));
        return authToken;
    }

    private String httpbasic(String clientId, String clientSecret) {
        return "Basic " + new String(Base64.getEncoder().encode((clientId + ":" + clientSecret).getBytes()));
    }

    public AuthToken getUserToken(String token) {
        String userToken = "user_token:" + token;
        String jwtJson = (String) redisTemplate.opsForValue().get(userToken);
        if (StringUtils.isNotEmpty(jwtJson)) {
            AuthToken authToken = JSON.parseObject(jwtJson, AuthToken.class);
            return authToken;
        }
        return null;
    }

    //从redis中删除令牌
    public boolean delToken(String access_token){
        String name = "user_token:" + access_token;
        redisTemplate.delete(name);
        return true;
    }
}
