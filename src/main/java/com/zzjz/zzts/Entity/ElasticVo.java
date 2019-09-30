package com.zzjz.zzts.Entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @Description Todo
 * @Author 房桂堂
 * @Date 2019/9/30 16:02
 */
public class ElasticVo {

    /**
     * 索引
     */
    private String index;

    /**
     * id
     */
    private String id;

    /**
     * 是否处理
     */
    @JsonProperty
    private boolean isHandled;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isHandled() {
        return isHandled;
    }

    public void setHandled(boolean handled) {
        isHandled = handled;
    }
}
