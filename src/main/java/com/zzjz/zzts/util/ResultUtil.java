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
        Result result = new Result();
        result.setCode(ResultCode.SUCCESS);
        result.setMessage(DEFAULT_SUCCESS_MESSAGE);
        return result;
    }

    public static Result genSuccessResult(Object data) {
        Result result = new Result();
        result.setCode(ResultCode.SUCCESS);
        result.setMessage(DEFAULT_SUCCESS_MESSAGE);
        result.setData(data);
        return result;
    }

    public static Result genFailResult(String message) {
        Result result = new Result();
        result.setCode(ResultCode.FAIL);
        result.setMessage(message);
        return result;
    }
}
