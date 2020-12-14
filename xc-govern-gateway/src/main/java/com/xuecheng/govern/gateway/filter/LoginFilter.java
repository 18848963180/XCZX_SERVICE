package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    AuthService authService;

    @Override
    public String filterType() {
        //四种类型：pre、routing、post、error
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    @Override
    public Object run() throws ZuulException {
        //上下文请求对象
        RequestContext currentContext = RequestContext.getCurrentContext();
        HttpServletRequest request = currentContext.getRequest();
        //cookie中uid是否存在
        String tokenFromCookie = authService.getTokenFromCookie(request);
        if (StringUtils.isEmpty(tokenFromCookie)) {
            this.access_denied(currentContext);
        }


        //redis当中是否存在
        long expire = authService.getExpire(tokenFromCookie);
        if (expire < 0L) {
            this.access_denied(currentContext);
        }
        //请求头里面 jwt信息是否存在
        String jwtFromHeader = authService.getJwtFromHeader(request);

        if (StringUtils.isEmpty(jwtFromHeader)) {
            this.access_denied(currentContext);
        }
        return null;
    }

    private void access_denied(RequestContext currentContext) {
        currentContext.setSendZuulResponse(false);
        //设置响应内容
        ResponseResult responseResult = new ResponseResult(CommonCode.UNAUTHENTICATED);
        String responseResultString = JSON.toJSONString(responseResult);
        currentContext.setResponseBody(responseResultString);
        currentContext.setResponseStatusCode(200);//响应状态码
        currentContext.getResponse().setContentType("application/json;charset=utf‐8");

    }

}
