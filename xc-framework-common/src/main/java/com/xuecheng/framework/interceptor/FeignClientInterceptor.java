package com.xuecheng.framework.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Objects;

public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (!Objects.isNull(requestAttributes)) {
            HttpServletRequest request = requestAttributes.getRequest();
            //取出所有的头信息
            Enumeration<String> headerNames = request.getHeaderNames();
            if (!Objects.isNull(headerNames)) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String header = request.getHeader(headerName);
                    requestTemplate.header(headerName, header);
                }
            }
        }

    }
}
