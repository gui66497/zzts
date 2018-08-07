package com.zzjz.zzts.controller;

import com.zzjz.zzts.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author 房桂堂
 * @description ElasticController
 * @date 2018/7/27 13:01
 */
@Component
@Path("es")
public class ElasticController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticController.class);

    /**
     * 判断指定区域的连通性.
     * @param location 区域中文
     * @return 是否连通
     */
    @GET
    @Path("isReachAble/{location}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String isReachAble(@PathParam("location") String location) {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.IP_SECTION);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("area", location));
        searchRequest.source(searchSourceBuilder);
        boolean isReachAble = false;
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            long count = searchResponse.getHits().getTotalHits();
            if (count < 1) {
                return "没有找到 " + location + " 这个地区!";
            }
            Map<String, Object> map = searchResponse.getHits().getAt(0).getSourceAsMap();
            List<String> getways = (List<String>) map.get("gateway");
            //一旦能连通其中任一网关则代表连接成功
            for (String getway : getways) {
                if (isIpReachable(getway)) {
                    isReachAble = true;
                    break;
                }
            }
            System.out.println(searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
            return "执行出错:" + e.getMessage();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return isReachAble ? "success" : "fail";
    }


    /**
     * 判断指定ip的连通性.
     * @param ip ip
     * @return 是否通
     */
    public boolean isIpReachable(String ip) {
        boolean isIpReachable = false;
        InetAddress address;
        try {
            address = InetAddress.getByName(ip);
            isIpReachable = address.isReachable(1000);
            System.out.println("isIpReachable: " + isIpReachable);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isIpReachable;
    }

    /**
     * 获取最新的机器统计数量.
     * @return 机器数量
     */
    @GET
    @Path("assetCount")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer> assetCount() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        Map<String, Integer> resMap = new LinkedHashMap<>();
        SearchRequest searchRequest = new SearchRequest(Constant.NWZDJG_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchPhraseQuery("logType", "STA"));
        searchSourceBuilder.sort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            //获取最近的一次数量统计
            Map<String, Object> map = searchResponse.getHits().getAt(0).getSourceAsMap();
            resMap.put("总数", (Integer) map.get("clientCount"));
            resMap.put("在线", (Integer) map.get("onlineCount"));
            resMap.put("杀毒软件", (Integer) map.get("antivirusSoftwareCount"));
            resMap.put("win7", (Integer) map.get("win7"));
            resMap.put("winxp", (Integer) map.get("winxp"));
            resMap.put("win2k2", (Integer) map.get("win2k2"));
            resMap.put("其它", (Integer) map.get("win2k") + (Integer) map.get("vista") +
                    (Integer) map.get("win2008") + (Integer) map.get("win95") +
                    (Integer) map.get("win98") + (Integer) map.get("winnt"));
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resMap;
    }

    /**
     * 获取实时监测相关数据.
     * @return 数据
     */
    @GET
    @Path("realMonitor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> realMonitor() {
        Map<String, String> resMap = new LinkedHashMap<>();
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        //实时监控默认按最近24小时来
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusHours(Constant.MONITOR_HOUR).toString(format);
        LOGGER.info("查询的起始时间为" + oldTime);
        //1.防火墙流量数据(字节)
        int firewallData = 0;
        SearchRequest searchRequest = new SearchRequest(Constant.FIREWALL_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                            .must(QueryBuilders.existsQuery("receivePacket"))
                            .must(QueryBuilders.rangeQuery("@timestamp")
                                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                int receivePacket = (int) hit.getSourceAsMap().get("receivePacket");
                int sendPacket = (int) hit.getSourceAsMap().get("sendPacket");
                firewallData += (receivePacket + sendPacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("防火墙流量:" + bytes2kb(firewallData));
        resMap.put("防火墙流量", bytes2kb(firewallData));
        //2.终端告警数量
        SearchRequest searchRequest2 = new SearchRequest(Constant.NWZDJG_INDEX);
        SearchSourceBuilder searchSourceBuilder2 = new SearchSourceBuilder();
        searchSourceBuilder2.query(QueryBuilders.boolQuery()
                .should(QueryBuilders.matchPhraseQuery("logType", "WAR"))
                .should(QueryBuilders.matchPhraseQuery("logType", "AUD"))
                .must(QueryBuilders.rangeQuery("@timestamp")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai"))
                .minimumShouldMatch(1));
        searchRequest2.source(searchSourceBuilder2);
        try {
            SearchResponse searchResponse = client.search(searchRequest2);
            long nwzdjgCount = searchResponse.getHits().totalHits;
            System.out.println("终端告警:" + nwzdjgCount);
            resMap.put("终端告警", String.valueOf(nwzdjgCount));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //3.入侵告警数量
        SearchRequest searchRequest3 = new SearchRequest(Constant.RQJC_INDEX);
        SearchSourceBuilder searchSourceBuilder3 = new SearchSourceBuilder();
        searchSourceBuilder3.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("eventtime")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai"))
                .mustNot(QueryBuilders.termQuery("danger_value", 0)));
        searchRequest3.source(searchSourceBuilder3);
        try {
            SearchResponse searchResponse = client.search(searchRequest3);
            long rqjcCount = searchResponse.getHits().totalHits;
            System.out.println("入侵检测:" + rqjcCount);
            resMap.put("入侵告警", String.valueOf(rqjcCount));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //4.病毒数量
        SearchRequest searchRequest4 = new SearchRequest(Constant.VIRUS_INDEX);
        SearchSourceBuilder searchSourceBuilder4 = new SearchSourceBuilder();
        searchSourceBuilder4.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("ctime")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchRequest4.source(searchSourceBuilder4);
        try {
            SearchResponse searchResponse = client.search(searchRequest4);
            long virusCount = searchResponse.getHits().totalHits;
            System.out.println("病毒检测:" + virusCount);
            resMap.put("防病毒告警", String.valueOf(virusCount));

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                //关闭终端
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resMap;
    }

    /**
     * 病毒感染机器数排行
     * @return 排行
     */
    @GET
    @Path("virusRank")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer>  virusRank() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        Map<String, Set<String>> resMap = new HashMap<>();
        //病毒排行默认按最近1个月来
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusMonths(1).toString(format);
        LOGGER.info("查询的起始时间为" + oldTime);
        //1.防火墙流量数据(字节)
        SearchRequest searchRequest = new SearchRequest(Constant.VIRUS_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //默认查前1万条
        searchSourceBuilder.size(10000);
        searchSourceBuilder.query(QueryBuilders.boolQuery()
                .must(QueryBuilders.rangeQuery("@timestamp")
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                String name = (String) hit.getSourceAsMap().get("name");
                String ipStr = (String) hit.getSourceAsMap().get("ip");
                if (StringUtils.isNotBlank(ipStr) && ipStr.contains(";")) {
                    String[] ipArr = ipStr.split(";");
                    if (resMap.get(name) == null) {
                        resMap.put(name, new HashSet<>(Arrays.asList(ipArr)));
                    } else {
                        Set<String> originSet = resMap.get(name);
                        originSet.addAll(Arrays.asList(ipArr));
                        resMap.put(name, originSet);
                    }
                }
            }
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //将map结果统计个数并排序
        // 降序比较器
        Comparator<Map.Entry<String, Integer>> valueComparator = (o1, o2) -> o2.getValue() - o1.getValue();
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : resMap.entrySet()) {
            map.put(entry.getKey(), entry.getValue().size());
        }
        // map转换成list进行排序
        List<Map.Entry<String, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort(valueComparator);
        Map<String, Integer> sortedMap = new LinkedHashMap<>();
        Iterator<Map.Entry<String, Integer>> iter = list.iterator();
        Map.Entry<String, Integer> tmpEntry = null;
        while (iter.hasNext()) {
            tmpEntry = iter.next();
            sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
        }
        return sortedMap;
    }

    /**
     * byte(字节)根据长度转成kb(千字节)和mb(兆字节)或gb
     *
     * @param bytes 字节
     * @return string
     */
    public static String bytes2kb(long bytes) {
        BigDecimal filesize = new BigDecimal(bytes);
        BigDecimal gigabyte = new BigDecimal(1024 * 1024 * 1024);
        float returnValue = filesize.divide(gigabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1) {
            return (returnValue + "GB");
        }
        BigDecimal megabyte = new BigDecimal(1024 * 1024);
        returnValue = filesize.divide(megabyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        if (returnValue > 1) {
            return (returnValue + "MB");
        }
        BigDecimal kilobyte = new BigDecimal(1024);
        returnValue = filesize.divide(kilobyte, 2, BigDecimal.ROUND_UP)
                .floatValue();
        return (returnValue + "KB");
    }

    public static void main(String[] args) {
        System.out.println(bytes2kb(212000));
    }

}
