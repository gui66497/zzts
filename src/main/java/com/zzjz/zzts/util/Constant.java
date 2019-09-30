package com.zzjz.zzts.util;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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

    public static final String EVENTLOG_INDEX = "eventlog_2*";

    public static final String SPECIALFOCUS_INDEX = "specialfocus";

    /**
     * 交换机ip1.3.6.1.2.1.2.2.1.10
     */
    public static final String SWITCH_IP = "192.168.1.1";

    /**
     * 交换机协议
     */
    public static final String COMMUNITY = "123qweASD";

    /**
     * 需要监控的交换机端口
     */
    public static final String SWITCH_PORT = "GigabitEthernet1/0/1";

    /**
     * 32位交换机流量存储最大值 2^32
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
         * 接口输入值(32位)
         */
        ifInOctets(".1.3.6.1.2.1.2.2.1.10"),

        /**
         * 接口输出值(32位)
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

    /**
     * byte(字节)根据长度转成kb(千字节)和mb(兆字节)或gb
     * @param bytes 字节
     * @return String
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

    /**
     * bit(位)根据长度转成kb(千字节)和mb(兆字节)或gb
     * @param bytes 字节
     * @return String
     */
    public static String bit2kb(long bytes, int scale) {
        BigDecimal filesize = new BigDecimal(bytes);
        //8 * 1024 * 1024 * 2024 * 1024
        BigDecimal petabyte = new BigDecimal( 8796093022208L);
        BigDecimal returnValue = filesize.divide(petabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "PB");
        }
        //8 * 1024 * 1024 * 2024
        BigDecimal gigabyte = new BigDecimal( 8589934592L);
        returnValue = filesize.divide(gigabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "GB");
        }
        BigDecimal megabyte = new BigDecimal(8 * 1024 * 1024);
        returnValue = filesize.divide(megabyte, scale, BigDecimal.ROUND_HALF_UP);
        if (returnValue.floatValue() >= 1) {
            return (returnValue + "MB");
        }
        BigDecimal kilobyte = new BigDecimal(8 * 1024);
        returnValue = filesize.divide(kilobyte, scale, BigDecimal.ROUND_HALF_UP);
        return (returnValue + "KB");
    }

    /**
     * bit转mb
     * @param bits 位
     * @return M
     */
    public static float bitToMB(long bits) {
        BigDecimal size = new BigDecimal(bits);
        BigDecimal megabyte = new BigDecimal(8* 1024 * 1024);
        return size.divide(megabyte, 1, BigDecimal.ROUND_UP)
                .floatValue();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        CompletionService<String> pool = new ExecutorCompletionService<>(executorService);
        long t1 = System.currentTimeMillis();
        AtomicInteger integer = new AtomicInteger(0);
        System.out.println( " 正在检测,请等待: " );
        List<Future<String>> resultList = new ArrayList<>();
        for ( int  i = 1 ;i < 244 ;i ++ )
        {
            String T = "192.168.1." + i;
            resultList.add(pool.submit(() -> {
                InetAddress address  =  InetAddress.getByName(T);
                // 1000 ms
                if (address.isReachable(1000)) {
                    integer.incrementAndGet();
                    System.out.print(" ");
                    System.out.println("IP地址: " + T + "主机名:" + address.getHostName());
                    return "IP地址: " + T + "主机名:" + address.getHostName();
                }
                return "";
            }));

        }
        executorService.shutdown();
        for(int i = 0; i < resultList.size(); i++){
            String result = pool.take().get();
            System.out.println(result);
        }
        System.out.println( "共发现主机:" + integer);

        long t2 = System.currentTimeMillis();
        System.out.println("共耗时:" + (t2 -t1));
    }
}
