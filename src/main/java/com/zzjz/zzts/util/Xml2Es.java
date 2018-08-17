package com.zzjz.zzts.util;

import org.apache.http.HttpHost;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author 房桂堂
 * @description 解析xml存储到elasticsearch
 * @date 2018/7/31 14:34
 */
public class Xml2Es {

    private final static Logger LOGGER = LoggerFactory.getLogger(Xml2Es.class);

    /**
     * 解析xml存储到elasticsearch.
     * @param file 漏洞xml文件路径
     */
    static void analysis(File file) {
        LOGGER.info("调用Xml2Es的analysis方法");
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        BulkRequest request = new BulkRequest();
        List<Map<String, Object>> dangerList = new ArrayList<>();
        Document document;
        try {
            SAXReader saxReader = new SAXReader();
            // 读取XML文件,获得document对象
            document = saxReader.read(file);
            Element root = document.getRootElement();
            String taskName = root.element("data").element("report").element("task").elementText("name");
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
                    jsonMap.put("task_name", taskName);
                    jsonMap.put("vul_id",  vulnElement.element("vul_id").getData());
                    jsonMap.put("plugin_id",  vulnElement.element("plugin_id").getData());
                    jsonMap.put("name",  vulnElement.element("name").getData());
                    jsonMap.put("threat_category",  vulnElement.element("threat_category").getData());
                    jsonMap.put("cve_id",  vulnElement.element("cve_id").getData());
                    jsonMap.put("nsfocus_id",  vulnElement.element("nsfocus_id").getData());
                    jsonMap.put("bugtraq_id",  vulnElement.element("bugtraq_id").getData());
                    double riskPoints = Double.valueOf(vulnElement.element("risk_points").getData().toString());
                    jsonMap.put("risk_points", riskPoints);
                    jsonMap.put("solution",  vulnElement.element("solution").getData());
                    jsonMap.put("description",  vulnElement.element("description").getData());
                    jsonMap.put("insert_time",  new Date());
                    String dayStr = new DateTime().toString("yyyy.MM.dd");
                    request.add(new IndexRequest("vuln_xml_12j-" + dayStr, "doc").source(jsonMap));
                    //将高风险的漏洞插入eventlog表中
                    if (riskPoints >= 7) {dangerList.add(jsonMap);}
                }
            }
            BulkResponse bulkResponse = client.bulk(request);
            LOGGER.info("漏洞插入执行结果:" +  (bulkResponse.hasFailures() ? "有错误" : "成功"));
            LOGGER.info("漏洞插入执行用时:" + bulkResponse.getTook().getMillis() + "毫秒");
            if (!bulkResponse.hasFailures()){
                insertEventlog(dangerList);
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
    }

    /**
     * 将漏洞数据组织后插入eventlog表.
     * @param dangerList 需要插入的漏洞数据
     * @throws IOException IOException
     */
    static void insertEventlog(List<Map<String, Object>> dangerList) throws IOException {
        LOGGER.info("调用Xml2Es的insertEventlog方法");
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        BulkRequest request = new BulkRequest();
        String dayStr = new DateTime().toString("yyyy.MM.dd");
        for (Map<String, Object> map : dangerList) {
            Map<String, Object> jsonMap = new HashMap<>();
            String ip = (String) map.get("ip");
            //通过查询资产表获取相关信息
            GetRequest getRequest = new GetRequest("soc-system", "res", ip);
            GetResponse getResponse = client.get(getRequest);
            if (!getResponse.isSourceEmpty()) {
                jsonMap.put("dept", getResponse.getSourceAsMap().get("dept"));
                String manager = (String) getResponse.getSourceAsMap().get("manager");
                jsonMap.put("assetName", manager + "-" + ip);
            } else {
                jsonMap.put("dept", "无");
                jsonMap.put("assetName", map.get("ip"));
            }
            jsonMap.put("dataType", "资产");
            jsonMap.put("eventType", "漏洞报警");
            jsonMap.put("eventPriority", "高");
            jsonMap.put("eventScore", 85);
            jsonMap.put("assetIP", map.get("ip"));
            jsonMap.put("eventName", map.get("threat_category"));
            jsonMap.put("eventUserMsg", "ip为" + map.get("ip") + "的机器检测到" + map.get("name"));
            jsonMap.put("@timestamp", new Date());
            request.add(new IndexRequest("eventlog_" + dayStr, "doc").source(jsonMap));
        }
        BulkResponse bulkResponse = client.bulk(request);
        LOGGER.info("Eventlog插入执行结果:" +  (bulkResponse.hasFailures() ? "有错误" : "成功"));
        LOGGER.info("Eventlog插入执行用时:" + bulkResponse.getTook().getMillis() + "毫秒");
        client.close();
    }

    public static void main(String[] args) throws IOException {
        //File file = new File("D:\\12J部署相关\\病毒_漏洞_入侵检测\\bak-12j\\绿盟_RSAS\\3.xml");
        //analysis(file);
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));
        GetRequest getRequest = new GetRequest("soc-system", "res", "11.39.137.21");
        GetResponse getResponse = client.get(getRequest);
        System.out.println(11);
    }
}
