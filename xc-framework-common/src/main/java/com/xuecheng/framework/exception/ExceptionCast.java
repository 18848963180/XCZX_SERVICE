package com.xuecheng.framework.exception;

import com.xuecheng.framework.model.response.ResultCode;

public class ExceptionCast {

    //静态方法抛出异常类
    public static void cast(ResultCode resultCode) {
        throw new CustomException(resultCode);
    }
}
