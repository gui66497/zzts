package com.zzjz.zzts.util;

/**
 * @author 房桂堂
 * @description Constant
 * @date 2018/7/31 14:43
 */
public class Constant {

    /**
     * 漏洞报表监控目录
     */
    public static final String MONITOR_PATH = "D:\\zzjz_work\\Projects\\zzts\\src\\main\\resources\\logstash\\";

    /**
     * 漏洞报表监控时间间隔(毫秒)
     */
    public static final int MONITOR_INTERVAL = 10000;

    /**
     * xml后缀
     */
    public static final String XML = ".xml";

    /**
     * zip后缀
     */
    public static final String ZIP = ".zip";

    /**
     * Elasticsearch的ip
     */
    public static final String ES_HOST = "192.168.1.188";

    /**
     * Elasticsearch的rest端口
     */
    public static final int ES_PORT = 9200;

    /**
     * Elasticsearch的rest端口
     */
    public static final String ES_METHOD = "http";

    /**
     * 实时监测从过去去多少小时开始
     */
    public static final int MONITOR_HOUR = 24;

    public static final String IP_SECTION = "ip_section";

    public static final String RQJC_INDEX = "rqjc_jdbc_12j-*";

    public static final String FIREWALL_INDEX = "firewall-jns*";

    public static final String NWZDJG_INDEX = "nwzdjg-*";

    public static final String VIRUS_INDEX = "virus_12j-*";

}
