package com.zzjz.zzts.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zzjz.zzts.service.ElasticService;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Autowired
    ElasticService elasticService;

    /**
     * 每过1分钟 获取交换机输入流量和输出流量并存到es中
     */
    //@Scheduled(cron = "0 0/2 * * * *")
    public void timer() {
        System.out.println("当前时间为:" + new DateTime().toString("yyyy-MM:dd HH:mm:ss"));

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
            inJsonMap.put("insert_time", new Date());
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
            outJsonMap.put("insert_time", new Date());
            request.add(new IndexRequest("snmp_data_12j-" + dayStr, "doc").source(outJsonMap));
        });

        BulkResponse bulkResponse;
        try {
            bulkResponse = client.bulk(request);
            LOGGER.info("snmp插入执行结果:" + (bulkResponse.hasFailures() ? "有错误" : "成功"));
            LOGGER.info("snmp插入执行用时:" + bulkResponse.getTook().getMillis() + "毫秒");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(11);
    }

    /**
     * snmpwalk指定动作
     *
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
     * 获取指定端口指定时间段的输入输出流量.
     *
     * @param hours 时间小时数
     * @return 流量
     */
    @GET
    @Path("snmpData/{hours}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String snmpData(@PathParam("hours") int hours) {
        String portName = Constant.SWITCH_PORT;
        JsonObject json = elasticService.snmpData(hours, portName);
        return json.toString();
    }

    /**
     * 获取指定时间到现在的流量趋势(包括输入,输出,总计)
     *
     * @param hour 时间的小时数
     * @return 流量(单位MB)
     */
    @GET
    @Path("snmpDataTrend/{hour}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String snmpDataTrend(@PathParam("hour") int hour) {
        //Todo 这里暂时写死时间和端口和拆分数
        //[{"name":"输入", "data":[1,2,3]},{"name":"输出", "data":[1,4,5]}]
        //默认将时间拆成六份 也就是7个时间点
        int split = 6;
        DateTime now = new DateTime();
        String format = "yyyy-MM-dd HH:mm:ss";
        List<String> timeList = new ArrayList<>();
        //间隔 hour*10
        int interval = hour * 10;
        for (int i = 0; i <= split + 1; i++) {
            //第一位的时间为辅助计算用 最后应remove
            timeList.add(now.minusMinutes((split - i + 1) * interval).toString(format));
        }
        Map<String, Long> inMap = calDataByTimeList(timeList, "in", Constant.SWITCH_PORT);
        Map<String, Long> outMap = calDataByTimeList(timeList, "out", Constant.SWITCH_PORT);
        JsonObject oJson = new JsonObject();
        //{"type":"输入", "data":[{"time1":"01:00", "val":100}]}
        //转成前端需要的格式
        JsonArray timeArray = new JsonArray();
        for (int i = 1; i < timeList.size(); i++) {
             timeArray.add(timeList.get(i));
        }
        oJson.add("timeArr", timeArray);
        JsonArray inArray = new JsonArray();
        JsonArray outArray = new JsonArray();
        inMap.forEach((k, v) -> inArray.add(Constant.bitToMB(v)));
        outMap.forEach((k, v) -> outArray.add(Constant.bitToMB(v)));
        //输入
        JsonObject inJson = new JsonObject();
        inJson.addProperty("type", "in");
        inJson.add("datas", inArray);
        //输出
        JsonObject outJson = new JsonObject();
        outJson.addProperty("type", "out");
        outJson.add("datas", outArray);
        //总计
        JsonArray allArray = new JsonArray();
        for (int i = 0; i < inArray.size(); i++) {
            allArray.add(inArray.get(i).getAsBigDecimal().add(outArray.get(i).getAsBigDecimal()));
        }
        JsonObject allJson = new JsonObject();
        allJson.addProperty("type", "all");
        allJson.add("datas", allArray);

        JsonArray dataArray = new JsonArray();
        dataArray.add(inJson);
        dataArray.add(outJson);
        dataArray.add(allJson);

        oJson.add("timeData", dataArray);
        return oJson.toString();
    }

    /**
     * 根据时间列表计算每个时间点的流量
     * @param timeList 时间list
     * @param flowType 类型
     * @param portName 端口
     * @return map
     */
    Map<String, Long> calDataByTimeList(List<String> timeList, String flowType, String portName) {
        String format = "yyyy-MM-dd HH:mm:ss";
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.SNMP_DATA_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("flow_type", flowType))
                .must(QueryBuilders.matchPhraseQuery("port_name", portName))
                .must(QueryBuilders.rangeQuery("insert_time")
                        .format(format).gte(timeList.get(0)).lte(timeList.get(timeList.size() - 1)).timeZone("Asia/Shanghai")));
        searchSourceBuilder.sort(SortBuilders.fieldSort("insert_time").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder);
        Map<String, List<Long>> resMap = new LinkedHashMap<>();
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                String hitTime = (String) hit.getSourceAsMap().get("insert_time");
                int timeIndex = judgeTimeRange(timeList, hitTime);
                String key = timeList.get(timeIndex + 1);
                if (resMap.get(key) == null) {
                    List<Long> valList = new ArrayList<>();
                    valList.add(Long.valueOf(hit.getSourceAsMap().get("port_value").toString()));
                    resMap.put(key, valList);
                } else {
                    List<Long> valList = resMap.get(key);
                    valList.add(Long.valueOf(hit.getSourceAsMap().get("port_value").toString()));
                    resMap.put(key, valList);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("得到时间点和其对应的流量序列");
        System.out.println(resMap);
        Map<String, Long> dataMap = new LinkedHashMap<>();
        resMap.forEach((k, v) -> {
            int spill = elasticService.spillCount(v);
            if (spill == 0) {
                //数据没有溢出 直接用最后时间点值-开始时间点值
                dataMap.put(k, (v.get(v.size() - 1) - v.get(0)));
            } else {
                //出现了溢出数据 val = （spill-1）*max+lastVal+（max-firstVal）
                LOGGER.warn("出现了溢出数据！！！！！！！！！！！！！！！！");
                Long val = (spill - 1) * Constant.SNMP_MAX_DATA + v.get(v.size() - 1) +  (Constant.SNMP_MAX_DATA - v.get(0));
                dataMap.put(k, val);
            }
        });
        return dataMap;
    }

    /**
     * 查询指定时间范围内的平均流量.
     *
     * @param flowType 流量类型
     * @param portName 端口
     * @param oldTime  >时间
     * @param newTime  <时间
     * @return 流量
     */
    long calAverageData(String flowType, String portName, String oldTime, String newTime) {
        String format = "yyyy-MM-dd HH:mm:ss";
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.SNMP_DATA_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.termsQuery("flow_type", flowType))
                .must(QueryBuilders.matchPhraseQuery("port_name", portName))
                .must(QueryBuilders.rangeQuery("insert_time")
                        .format(format).gte(oldTime).lte(newTime).timeZone("Asia/Shanghai")));

        searchSourceBuilder.sort(SortBuilders.fieldSort("insert_time").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            SearchHits hits = searchResponse.getHits();
            if (hits.totalHits > 1) {
                int spill = elasticService.spillCount(hits);
                if (spill == 0) {
                    //数据没有溢出 直接用最后时间点值-开始时间点值
                    long res = Long.valueOf(hits.getAt((int) (hits.totalHits - 1)).getSourceAsMap().get("port_value").toString()) -
                            Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                    return res / (hits.totalHits - 1);

                } else {
                    //出现了溢出数据 val = （spill-1）*max+lastVal+（max-firstVal）
                    LOGGER.error("出现了溢出数据！！！！！！！！！！！！！！！！");
                    long firstVal = Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                    long lastVal = Long.valueOf(hits.getAt((int) (hits.totalHits - 1)).getSourceAsMap().get("port_value").toString());
                    long res = (spill - 1) * Constant.SNMP_MAX_DATA + lastVal + (Constant.SNMP_MAX_DATA - firstVal);
                    return res / (hits.totalHits - 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 判断所给时间的区间.
     * @param timeList timeList
     * @param time time
     * @return 序号
     */
    int judgeTimeRange(List<String> timeList, String time) {
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
        DateTime t = new DateTime(time);
        for (int i = 0; i < timeList.size(); i++) {
            DateTime oldTime = DateTime.parse(timeList.get(i), format);
            DateTime newTime = DateTime.parse(timeList.get(i + 1), format);
            if (t.isAfter(oldTime) && t.isBefore(newTime) || t.equals(oldTime)) {
                return i;
            }
        }
        return -1;
    }
}
