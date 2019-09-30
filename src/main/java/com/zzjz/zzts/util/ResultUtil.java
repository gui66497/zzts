package com.zzjz.zzts.util;

import com.zzjz.zzts.Entity.Result;
import com.zzjz.zzts.Entity.ResultCode;

/**
 * @Description 响应结果生成工具
 * @Author 房桂堂
 * @Date 2019/9/30 14:09
 */
public class ResultUtil {
    private static final String DEFAULT_SUCCESS_MESSAGE = "SUCCESS";

    public static Result genSuccessResult() {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE);
    }

    public static Result genSuccessResult(Object data) {
        return new Result()
                .setCode(ResultCode.SUCCESS)
                .setMessage(DEFAULT_SUCCESS_MESSAGE)
                .setData(data);
    }

    public static Result genFailResult(String message) {
        return new Result()
                .setCode(ResultCode.FAIL)
                .setMessage(message);
    }
}
