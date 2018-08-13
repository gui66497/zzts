package com.zzjz.zzts.service;

import com.google.gson.JsonObject;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author 房桂堂
 * @description ElasticService
 * @date 2018/8/13 9:45
 */
public interface ElasticService {

    /**
     * 获取指定端口指定时间段的输入输出流量.
     * @param hours 小时数
     * @param portName 端口
     * @return 流量
     */
    JsonObject snmpData(int hours, String portName);

    /**
     * 计算出指定端口,指定时间到现在的的总流量.
     *
     * @param flowType 输入或输出
     * @param portName 端口名
     * @param oldTime  时间
     * @return 流量
     */
    long calData(String flowType, String portName, String oldTime);

    /**
     * 计算溢出次数
     * @param hits 数据
     * @return 次数
     */
    int spillCount(SearchHits hits);

    /**
     * 计算溢出次数
     * @param vals 数据
     * @return 次数
     */
    int spillCount(List<Long> vals);

}
