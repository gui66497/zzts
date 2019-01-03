package com.zzjz.zzts.service.impl;

import com.google.gson.JsonObject;
import com.zzjz.zzts.service.ElasticService;
import com.zzjz.zzts.util.Constant;
import org.apache.http.HttpHost;
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
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

/**
 * @author 房桂堂
 * @description ElasticServiceImpl
 * @date 2018/8/13 9:46
 */
@Service
public class ElasticServiceImpl implements ElasticService {

    private final static Logger LOGGER = LoggerFactory.getLogger(ElasticServiceImpl.class);

    @Override
    public JsonObject snmpData(int hours, String portName) {
        String format = "yyyy-MM-dd HH:mm:ss";
        String oldTime = new DateTime().minusHours(hours).toString(format);
        LOGGER.info("查询的起始时间为" + oldTime);
        long inData = calData("in", portName, oldTime);
        long outData = calData("out", portName, oldTime);
        long allData = inData + outData;
        //将数据可读化
        String inDataRead = Constant.bit2kb(inData, 1);
        String outDataRead = Constant.bit2kb(outData, 1);
        String allDataRead = Constant.bit2kb(allData, 1);
        JsonObject json = new JsonObject();
        json.addProperty("in", inDataRead);
        json.addProperty("out", outDataRead);
        json.addProperty("all", allDataRead);
        return json;
    }

    @Override
    public long calData(String flowType, String portName, String oldTime) {
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
                        .format(format).gte(oldTime).timeZone("Asia/Shanghai")));
        searchSourceBuilder.sort(SortBuilders.fieldSort("insert_time").order(SortOrder.ASC));
        searchRequest.source(searchSourceBuilder);
        try {
            SearchResponse searchResponse = client.search(searchRequest);
            SearchHits hits = searchResponse.getHits();
            if (hits.totalHits == 0) {
                return 0;
            }
            int spill = spillCount(hits);
            if (spill == 0) {
                //数据没有溢出 直接用最后时间点值-开始时间点值
                long res = Long.valueOf(hits.getAt((int) (hits.totalHits - 1)).getSourceAsMap().get("port_value").toString()) -
                        Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                return res;

            } else {
                //出现了溢出数据 val = （spill-1）*max+lastVal+（max-firstVal）
                LOGGER.warn("出现了溢出数据！！！！！！！！！！！！！！！！");
                long firstVal = Long.valueOf(hits.getAt(0).getSourceAsMap().get("port_value").toString());
                long lastVal = Long.valueOf(hits.getAt((int) (hits.totalHits - 1)).getSourceAsMap().get("port_value").toString());
                long res = (spill - 1) * Constant.SNMP_MAX_DATA + lastVal + (Constant.SNMP_MAX_DATA - firstVal);
                return res;
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
        return 0;
    }

    @Override
    public int spillCount(SearchHits hits) {
        int spill = 0;
        for (int i = 0; i < hits.totalHits; i++) {
            Long nowVal = Long.valueOf(hits.getAt(i).getSourceAsMap().get("port_value").toString());
            Long preVal = i == 0 ? 0 : Long.valueOf(hits.getAt(i - 1).getSourceAsMap().get("port_value").toString());
            if ((nowVal - preVal) < 0) {
                spill++;
            }
        }
        return spill;
    }

    @Override
    public int spillCount(List<Long> vals) {
        int spill = 0;
        for (int i = 0; i < vals.size(); i++) {
            Long nowVal = vals.get(i);
            Long preVal = i == 0 ? 0L : vals.get(i - 1);
            if ((nowVal - preVal) < 0) {
                spill++;
            }
        }
        return spill;
    }

}
