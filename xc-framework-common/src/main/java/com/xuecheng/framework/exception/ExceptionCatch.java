package com.xuecheng.framework.exception;

import com.google.common.collect.ImmutableMap;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.model.response.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

//异常捕获类
@ControllerAdvice
public class ExceptionCatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionCatch.class);

    //使用EXCEPTION存放异常的类型和异常的信息,ImmutableMap的特点是 一旦创建就不可改变,且是线程安全的
    public static ImmutableMap<Class<? extends Throwable>, ResultCode> EXCEPTIONS;

    //使用builder来构建一个异常类型和异常信息
    public static ImmutableMap.Builder<Class<? extends Throwable>, ResultCode> builder = ImmutableMap.builder();

    static {
        builder.put(HttpMessageNotReadableException.class, CommonCode.INVALID_PARAM);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseResult exception(Exception e) {
        LOGGER.error("cath Exception:{}", e.getMessage());
        if (EXCEPTIONS == null) {
            EXCEPTIONS = builder.build();
        }
        final ResultCode resultCode = EXCEPTIONS.get(e.getClass());
        final ResponseResult rr = resultCode != null ? new ResponseResult(resultCode) : new ResponseResult(CommonCode.SERVER_ERROR);
        return rr;
    }


    @ExceptionHandler(CustomException.class)//捕获该类型的异常
    @ResponseBody
    public ResponseResult customException(CustomException ce) {
        LOGGER.error("cath Exception:{}\r\nexception:", ce.getMessage(), ce);
        ResultCode resultCode = ce.getResultCode();
        return new ResponseResult(resultCode);
    }
}
