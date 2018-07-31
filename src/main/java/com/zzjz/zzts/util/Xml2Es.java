package com.zzjz.zzts.util;

import org.apache.http.HttpHost;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author 房桂堂
 * @description 解析xml存储到elasticsearch
 * @date 2018/7/31 14:34
 */
public class Xml2Es {

    private final static Logger LOGGER = LoggerFactory.getLogger(Xml2Es.class);

    static void analysis(File file) {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        BulkRequest request = new BulkRequest();
        Document document;
        try {
            SAXReader saxReader = new SAXReader();
            // 读取XML文件,获得document对象
            document = saxReader.read(file);
            Element root = document.getRootElement();
            Iterator targetIt = root.element("data").element("report").element("targets").elementIterator("target");
            //遍历target
            while (targetIt.hasNext()) {
                Element element = (Element) targetIt.next();
                String targetIp = (String) element.element("ip").getData();
                Iterator vulnIt = element.element("vuln_detail").elementIterator("vuln");
                //遍历vuln
                while (vulnIt.hasNext()) {
                    Map<String, Object> jsonMap = new HashMap<>();
                    Element vulnElement = (Element) vulnIt.next();
                    jsonMap.put("ip",  targetIp);
                    jsonMap.put("vul_id",  vulnElement.element("vul_id").getData());
                    jsonMap.put("plugin_id",  vulnElement.element("plugin_id").getData());
                    jsonMap.put("name",  vulnElement.element("name").getData());
                    jsonMap.put("threat_category",  vulnElement.element("threat_category").getData());
                    jsonMap.put("cve_id",  vulnElement.element("cve_id").getData());
                    jsonMap.put("nsfocus_id",  vulnElement.element("nsfocus_id").getData());
                    jsonMap.put("bugtraq_id",  vulnElement.element("bugtraq_id").getData());
                    jsonMap.put("risk_points", Double.valueOf(vulnElement.element("risk_points").getData().toString()));
                    jsonMap.put("solution",  vulnElement.element("solution").getData());
                    jsonMap.put("description",  vulnElement.element("description").getData());
                    jsonMap.put("insert_time",  new Date());
                    String dayStr = new DateTime().toString("yyyy.MM.dd");
                    request.add(new IndexRequest("vuln_xml_12j-" + dayStr, "doc").source(jsonMap));
                }
            }
            BulkResponse bulkResponse = client.bulk(request);
            LOGGER.info("执行结果:" +  (bulkResponse.hasFailures() ? "有错误" : "成功"));
            LOGGER.info("执行用时:" + bulkResponse.getTookInMillis() + "毫秒");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
