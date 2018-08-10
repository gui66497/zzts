package com.zzjz.zzts.controller;

import com.google.gson.JsonObject;
import com.zzjz.zzts.util.Constant;
import com.zzjz.zzts.util.SnmpData;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 房桂堂
 * @description SnmpController
 * @date 2018/8/10 9:00
 */
@Component
@Path("snmp")
public class SnmpController {

    private final static Logger LOGGER = LoggerFactory.getLogger(SnmpController.class);

    @Autowired
    SnmpData snmpData;

    /**
     * 每过30秒 获取交换机输入流量和输出流量并存到es中
     */
    @Scheduled(cron = "0 0/1 * * * *")
    public void timer(){
        //获取当前时间
        LocalDateTime localDateTime =LocalDateTime.now();
        System.out.println("当前时间为:" + localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        BulkRequest request = new BulkRequest();
        Map<String, String> ipDescMap = new HashMap<>();
        String sysName = snmpData.snmpGet(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.sysName.getValue());
        Map<String, String> ipDescMapLong = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ipDescr.getValue());
        Map<String, String> inDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifInOctets.getValue());
        Map<String, String> outDataMap = snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, Constant.Oid.ifOutOctets.getValue());
        ipDescMapLong.forEach((k, v) -> ipDescMap.put(k.substring(k.lastIndexOf(".") + 1), v));

        Map<String, Object> inJsonMap = new HashMap<>();
        String dayStr = new DateTime().toString("yyyy.MM.dd");
        //输入流量
        inDataMap.forEach((k, v) -> {
            String key = k.substring(k.lastIndexOf(".") + 1);
            inJsonMap.put("sys_name", sysName);
            inJsonMap.put("port_index", key);
            inJsonMap.put("port_name", ipDescMap.get(key));
            inJsonMap.put("port_value", Long.parseLong(v));
            inJsonMap.put("oid", k);
            inJsonMap.put("flow_type", "in");
            inJsonMap.put("insert_time",  new Date());
            request.add(new IndexRequest("snmp_data_12j-" + dayStr, "doc").source(inJsonMap));
        });
        Map<String, Object> outJsonMap = new HashMap<>();
        outDataMap.forEach((k, v) -> {
            String key = k.substring(k.lastIndexOf(".") + 1);
            outJsonMap.put("sys_name", sysName);
            outJsonMap.put("port_index", key);
            outJsonMap.put("port_name", ipDescMap.get(key));
            outJsonMap.put("port_value", Long.parseLong(v));
            outJsonMap.put("oid", k);
            outJsonMap.put("flow_type", "out");
            outJsonMap.put("insert_time",  new Date());
            request.add(new IndexRequest("snmp_data_12j-" + dayStr, "doc").source(outJsonMap));
        });

        BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(request);
            LOGGER.info("snmp插入执行结果:" +  (bulkResponse.hasFailures() ? "有错误" : "成功"));
            LOGGER.info("snmp插入执行用时:" + bulkResponse.getTook().getMillis() + "毫秒");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(11);
    }

    /**
     * snmpwalk指定动作
     * @param action 动作
     * @return 结果
     */
    @GET
    @Path("snmpWalk/{action}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> snmpWalk(@PathParam("action") String action) {
        String oidval = Constant.Oid.valueOf(action).getValue();
        return snmpData.snmpWalk(Constant.SWITCH_IP, Constant.COMMUNITY, oidval);
    }

    /**
     * 获取指定端口指定时间段的输入输出流量
     * @param timeStr
     * @return
     */
    @GET
    @Path("snmpData/{timeStr}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String snmpData(@PathParam("timeStr") String timeStr) {
        //Todo 这里暂时写死时间和端口
        timeStr = "2h";
        String portName = "GigabitEthernet1/0/1";
        String oldTime = null;
        String format = "yyyy-MM-dd HH:mm:ss";
        if ("2h".equals(timeStr)) {
            oldTime = new DateTime().minusHours(2).toString(format);
        }
        LOGGER.info("查询的起始时间为" + oldTime);
        long inData = calData("in", portName, oldTime);
        long outData = calData("out", portName, oldTime);
        JsonObject json = new JsonObject();
        json.addProperty("in", inData);
        json.addProperty("out", outData);
        return json.toString();
    }

    /**
     * 计算出指定时间到现在的指定流量.
     * @param flowType 输入或输出
     * @param portName 端口名
     * @param oldTime 时间
     * @return 流量
     */
    private long calData(String flowType, String portName, String oldTime) {
        String format = "yyyy-MM-dd HH:mm:ss";
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.SNMP_DATA_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("flow_type",flowType))
                .must(QueryBuilders.matchPhraseQuery("port_name",portName))
                .must(QueryBuilders.rangeQuery("insert_time")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchSourceBuilder.sort(SortBuilders.fieldSort("insert_time").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            SearchHits hits = searchResponse.getHits();
            int spill = 0;
            for (int i = 0; i < hits.totalHits; i++) {
                Long nowVal = Long.valueOf(hits.getAt(i).getSourceAsMap().get("port_value").toString());
                Long preVal = i == 0 ? 0 : Long.valueOf(hits.getAt(i-1).getSourceAsMap().get("port_value").toString());
                if ((nowVal - preVal) < 0) { spill ++; }
            }
            if (spill == 0) {
                //数据没有溢出 直接用最后时间点值-开始时间点值
                long res = Long.valueOf(hits.getAt((int) (hits.totalHits-1)).getSourceAsMap().get("port_value").toString()) -
                        Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                return res;

            } else {
                //出现了溢出数据 val = （spill-1）*max+lastVal+（max-firstVal）
                LOGGER.error("出现了溢出数据！！！！！！！！！！！！！！！！");
                long firstVal = Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                long lastVal = Long.valueOf(hits.getAt((int) (hits.totalHits-1)).getSourceAsMap().get("port_value").toString());
                long res = (spill - 1)*Constant.SNMP_MAX_DATA + lastVal + (Constant.SNMP_MAX_DATA - firstVal);
                return res;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
