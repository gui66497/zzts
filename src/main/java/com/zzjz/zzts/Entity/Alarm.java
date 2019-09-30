package com.zzjz.zzts.Entity;

import java.util.Objects;

/**
 * @author 房桂堂
 * @description 报警实体
 * @date 2019/3/22 17:12
 */
public class Alarm {

    /**
     * 时间
     */
    private String date;

    /**
     * 资产
     */
    private String asset;

    /**
     * 报警信息
     */
    private String msg;


    public Alarm(String msg, String date) {
        this.msg = msg;
        this.date = date;
    }

    public Alarm(String date, String asset, String msg) {
        this.date = date;
        this.asset = asset;
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAsset() {
        return asset;
    }

    public void setAsset(String asset) {
        this.asset = asset;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Alarm) {
            Alarm alarm = (Alarm) object;
            return Objects.equals(this.asset + this.msg, alarm.getAsset() + alarm.getMsg());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(msg);
    }
}
