package com.zzjz.zzts.controller;

import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * @author 房桂堂
 * @description ElasticController
 * @date 2018/7/27 13:01
 */
@Component
@Path("es")
public class ElasticController {

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
                        new HttpHost("192.168.1.188", 9200, "http")));
        SearchRequest searchRequest = new SearchRequest("ip_section");
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

}
