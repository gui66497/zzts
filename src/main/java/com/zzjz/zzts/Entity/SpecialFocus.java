package com.zzjz.zzts.Entity;

/**
 * @Description 特别关注事项
 * @Author 房桂堂
 * @Date 2019/9/30 11:10
 */
public class SpecialFocus {

    /**
     * id
     */
    private String id;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件
     */
    private String event;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
