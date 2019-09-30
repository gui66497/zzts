package com.zzjz.zzts.Entity;

/**
 * @Description 统一API响应结果封装(参考https://zhuanlan.zhihu.com/p/46107614)
 * @Author 房桂堂
 * @Date 2019/9/30 14:07
 */
public class Result {

    private int code;
    private String message;
    private Object data;

    public Result setCode(ResultCode resultCode) {
        this.code = resultCode.code;
        return this;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
