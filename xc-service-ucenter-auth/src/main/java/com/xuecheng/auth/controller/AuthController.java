package com.xuecheng.auth.controller;

import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import com.xuecheng.framework.web.BaseController;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Objects;

public class AuthController extends BaseController implements AuthControllerApi {

    @Value("${auth.clientId}")
    String clientId;
    @Value("${auth.clientSecret}")
    String clientSecret;
    @Value("${auth.cookieDomain}")
    String cookieDomain;
    @Value("${auth.cookieMaxAge}")
    int cookieMaxAge;
    @Value("${auth.tokenValiditySeconds}")
    int tokenValiditySeconds;

    @Autowired
    AuthService authService;

    @Override
    @PostMapping("/userlogin")
    public LoginResult login(LoginRequest loginRequest) {
        if (Objects.isNull(loginRequest) || StringUtils.isEmpty(loginRequest.getUsername())) {
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        if (Objects.isNull(loginRequest) || StringUtils.isEmpty(loginRequest.getPassword())) {
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }

        AuthToken authToken = authService.login(loginRequest.getUsername(),
                loginRequest.getPassword(), clientId, clientSecret);
        //将令牌写入cookie
        // 访问token
        String access_token = authToken.getAccess_token();
        //将访问令牌存储到cookie
        saveCookie(access_token);
        return new LoginResult(CommonCode.SUCCESS, access_token);
    }

    private void saveCookie(String access_token) {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
//添加cookie 认证令牌，最后一个参数设置为false，表示允许浏览器获取
        CookieUtil.addCookie(response,
                cookieDomain, "/", "uid", access_token, cookieMaxAge, false);
    }

    @Override
    @PostMapping("/userlogout")
    public ResponseResult logout() {
        String token = this.getTokenFormCookie();
        authService.delToken(token);
        this.clearCookie(token);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void clearCookie(String token) {
        CookieUtil.addCookie(response,
                cookieDomain, "/", "uid", token, 0, false);
    }

    @Override
    @GetMapping("/userjwt")
    public JwtResult userjwt() {
        String token = this.getTokenFormCookie();
        AuthToken authToken = authService.getUserToken(token);
        return Objects.isNull(authToken) ? new JwtResult(CommonCode.FAIL, null) : new JwtResult(CommonCode.SUCCESS, authToken.getJwt_token());
    }

    private String getTokenFormCookie() {
        Map<String, String> uid = CookieUtil.readCookie(request, "uid");
        return uid.get("uid");

    }
}
