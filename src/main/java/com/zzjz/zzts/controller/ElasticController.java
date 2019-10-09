package com.zzjz.zzts.controller;

import com.google.gson.JsonObject;
import com.zzjz.zzts.Entity.Alarm;
import com.zzjz.zzts.Entity.ElasticVo;
import com.zzjz.zzts.Entity.Link;
import com.zzjz.zzts.Entity.Result;
import com.zzjz.zzts.Entity.SpecialFocus;
import com.zzjz.zzts.service.ElasticService;
import com.zzjz.zzts.util.Constant;
import com.zzjz.zzts.util.ResultUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
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
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author 房桂堂
 * @description ElasticController
 * @date 2018/7/27 13:01
 */
@Component
@Path("es")
public class ElasticController {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticController.class);

    @Autowired
    ElasticService elasticService;

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
     * 获取所有节点的连接情况
     * @return 连接情况
     */
    @GET
    @Path("isReachAbleAll2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Link> isReachAbleAll2() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.IP_SECTION);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(100);
        searchRequest.source(searchSourceBuilder);
        List<Link> links = new ArrayList<>();
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            while (it.hasNext()) {
                boolean isReachAble = false;
                SearchHit hit = (SearchHit) it.next();
                Link link = new Link();
                Map<String, Object> map = hit.getSourceAsMap();
                String area = (String) map.get("area");
                if ("上海".equals(area)) {
                    //过滤掉上海地区
                    continue;
                }
                List<String> getways = (List<String>) map.get("gateway");
                //一旦能连通其中任一网关则代表连接成功
                for (String getway : getways) {
                    if (isIpReachable(getway)) {
                        isReachAble = true;
                        break;
                    }
                }
                link.setArea(area);
                link.setIsLink(isReachAble ? 1 : 0);
                links.add(link);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return links;
    }

    /**
     * 获取所有节点的连接情况(优化版)
     * @return
     */
    @GET
    @Path("isReachAbleAll")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Link> isReachAbleAll() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        SearchRequest searchRequest = new SearchRequest(Constant.IP_SECTION);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(100);
        searchRequest.source(searchSourceBuilder);
        List<Link> links = new ArrayList<>();
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            Iterator it = searchResponse.getHits().iterator();
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            CompletionService<String> pool = new ExecutorCompletionService<String>(executorService);
            List<Future<String>> resultList = new ArrayList<>();
            while (it.hasNext()) {
                SearchHit hit = (SearchHit) it.next();
                Map<String, Object> map = hit.getSourceAsMap();
                List<String> getways = (List<String>) map.get("gateway");
                String area = (String) map.get("area");
                if ("上海".equals(area)) {
                    //过滤掉上海地区
                    continue;
                }
                resultList.add(pool.submit(() -> {
                    long t1 = System.currentTimeMillis();
                    boolean isReachAble = false;
                    Link link = new Link();
                    //一旦能连通其中任一网关则代表连接成功
                    for (String getway : getways) {
                        if (isIpReachable(getway)) {
                            isReachAble = true;
                            break;
                        }
                    }
                    link.setArea((String) map.get("area"));
                    link.setIsLink(isReachAble ? 1 : 0);
                    links.add(link);
                    long t2 = System.currentTimeMillis();
                    return "task " + map.get("area") + " completed.耗时：" + (t2 - t1);
                }));
            }
            //关闭线程池
            executorService.shutdown();
            for(int i = 0; i < resultList.size(); i++){
                String result = pool.take().get();
                System.out.println(result);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return links;
    }

    /**
     * 判断指定ip的连通性.
     * @param ip ip
     * @return 是否通
     */
    public boolean isIpReachable(String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }
        boolean isIpReachable = false;
        InetAddress address;
        try {
            address = InetAddress.getByName(ip);
            isIpReachable = address.isReachable(500);
            //System.out.println("isIpReachable: " + isIpReachable);
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
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return resMap;
    }

    /**
     * 获取实时监测相关数据.
     * @return 数据
     */
    @GET
    @Path("realMonitor/{hourStr}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String realMonitor(@PathParam("hourStr") String hourStr) {
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime;
        int hours;
        if (hourStr.contains(":")) {
            hours = 12;
            //假时间
            oldTime = hourStr;
        } else {
            hours = Integer.parseInt(hourStr);
            oldTime = DateTime.now().minusHours(hours).toString(format);
        }
        JsonObject bigJson = new JsonObject();
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        LOGGER.info("查询的起始时间为" + oldTime);
        //1.交换机流量数据
        JsonObject jsonObject = elasticService.snmpData(hours, Constant.SWITCH_PORT);

        System.out.println("网络总流量:" + jsonObject);
        bigJson.add("网络总流量", jsonObject);
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
            bigJson.addProperty("终端告警", nwzdjgCount);
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
            bigJson.addProperty("入侵检测", rqjcCount);
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
            bigJson.addProperty("防病毒告警", virusCount);

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
        return bigJson.toString();
    }

    /**
     * 病毒感染机器数排行
     * @return 排行
     */
    @GET
    @Path("virusRank/{months}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Integer>  virusRank(@PathParam("months") int months) {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        Map<String, Set<String>> resMap = new HashMap<>();
        //病毒排行默认按最近1个月来
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusMonths(months).toString(format);
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
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
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
     * 获取报警信息
     * @return 报警信息
     */
    @GET
    @Path("getAlarms/{days}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result getAlarm(@PathParam("days") int days) {
        // 时间范围是最近7天
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = DateTime.now().minusDays(days).toString(format);
        LOGGER.info("开始调用getAlarms接口");
        LOGGER.info("查询的起始时间为" + oldTime);

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));

        // 获取特别关注列表
        List<SpecialFocus> focusList = elasticService.specialList();
        // 报警列表
        Set<Alarm> alarmSet = new HashSet<>();

        for (SpecialFocus focus : focusList) {
            String eventType = focus.getEventType();
            String event = focus.getEvent();
            System.out.println("\n当前specialFocus:" + eventType + "(eventTpe)-" + event + "(event)");

            // 过滤掉已处理的
            SearchRequest searchRequest = new SearchRequest(Constant.EVENTLOG_INDEX);
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(QueryBuilders.boolQuery()
                    .must(QueryBuilders.matchPhraseQuery("eventType", eventType))
                    .mustNot(QueryBuilders.matchPhraseQuery("handled", "已处理"))
                    .must(QueryBuilders.rangeQuery("@timestamp")
                            .format(format).gte(oldTime).timeZone("Asia/Shanghai"))
            );
            searchRequest.source(searchSourceBuilder);
            try {
                SearchResponse searchResponse = client.search(searchRequest);
                SearchHits hits = searchResponse.getHits();
                Iterator it = searchResponse.getHits().iterator();

                while (it.hasNext()) {
                    SearchHit hit = (SearchHit) it.next();
                    // eventlog数据中的事件名
                    String eventName = hit.getSourceAsMap().get("eventName").toString();
                    System.out.println("type匹配到了，该eventName为" + eventName);
                    if (eventName.contains(event)) {
                        // 将es中的utc时间转为cst时间
                        String utcTime = hit.getSourceAsMap().get("@timestamp").toString();
                        String cstTime = new DateTime(utcTime).toDateTime(DateTimeZone.forID("Asia/Shanghai")).toString("yyyy-MM-dd HH:mm:ss");
                        Alarm alarm = new Alarm(cstTime, hit.getSourceAsMap().get("assetName").toString(),
                                hit.getSourceAsMap().get("eventUserMsg").toString());
                        alarmSet.add(alarm);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResultUtil.genSuccessResult(alarmSet);
    }

    /**
     * 将制定eventlog数据置为已处理
     * @param elasticVo elasticVo
     * @return 结果
     */
    @POST
    @Path("handle")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result handle(ElasticVo elasticVo) {
        LOGGER.info("开始调用handle接口");

        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        UpdateRequest updateRequest = new UpdateRequest(elasticVo.getIndex(), "doc", elasticVo.getId());
        Map<String, Object> jsonMap = new HashMap<>();
        String handleStr = elasticVo.isHandled() ? "已处理" : "未处理";
        jsonMap.put("handled", handleStr);
        updateRequest.doc(jsonMap);

        try {
            UpdateResponse updateResponse = client.update(updateRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.genFailResult(e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResultUtil.genSuccessResult();
    }

    public static void main(String[] args) {
        try  {
            long t1 = System.currentTimeMillis();
            int  count = 0 ;
            System.out.println( " 正在检测,请等待: " );
            for ( int  i = 1 ;i < 244 ;i ++ )
            {
                String T = "192.168.1." + i;
                System.out.print( " = ");
                InetAddress address  =  InetAddress.getByName(T);
                // 1000 ms
                if (address.isReachable( 1000 ))
                {
                    count ++ ;
                    System.out.print( " " );
                    System.out.println( " IP地址:  " + T + "   主机名:  " + address.getHostName());
                }
            }
            long t2 = System.currentTimeMillis();
            System.out.println( "  共发现主机: " + count);
            System.out.println( "共耗时：" + (t2 - t1));
        } catch  (IOException e) {
            e.printStackTrace();
        }
    }

}
