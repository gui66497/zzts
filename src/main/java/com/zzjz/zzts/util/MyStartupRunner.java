package com.zzjz.zzts.util;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @author 房桂堂
 * @description 项目启动后执行方法
 * @date 2018/7/31 15:14
 */
@Component
public class MyStartupRunner implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        System.out.println("启动后执行目录监控,路径为 " + Constant.MONITOR_PATH + " 时间间隔为 " + Constant.MONITOR_INTERVAL + "毫秒");
        MyFileMonitor m = new MyFileMonitor(Constant.MONITOR_INTERVAL);
        m.monitor(Constant.MONITOR_PATH, new MyFileListener());
        m.start();
    }
}
