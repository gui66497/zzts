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

    public static final String SNMP_DATA_INDEX = "snmp_data_12j-*";

    /**
     * 交换机ip
     */
    public static final String SWITCH_IP = "192.168.1.1";

    /**
     * 交换机协议
     */
    public static final String COMMUNITY = "123qweASD";

    /**
     * 交换机流量存储最大值 2^32
     */
    public static final long SNMP_MAX_DATA = 4294967296L;

    /**
     * oid的枚举
     */
    public enum Oid {

        /**
         * 交换机名称
         */
        sysName(".1.3.6.1.2.1.1.5.0"),

        /**
         * 接口的对应的文字描述
         */
        ipDescr(".1.3.6.1.2.1.2.2.1.2 "),

        /**
         * 接口输入值
         */
        ifInOctets(".1.3.6.1.2.1.2.2.1.10"),

        /**
         * 接口输出值
         */
        ifOutOctets(".1.3.6.1.2.1.2.2.1.16");

        /**
         * 对应的oid值
         */
        private String value;

        /**
         * 构造方法
         * @param value
         */
        private Oid(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

}
