package com.zzjz.zzts.util;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import java.io.File;

/**
 * @author 房桂堂
 * @description MyFileMonitor
 * @date 2018/7/31 14:22
 */
public class MyFileMonitor {

    FileAlterationMonitor monitor = null;

    public MyFileMonitor(long interval) throws Exception {
        monitor = new FileAlterationMonitor(interval);
    }

    public void monitor(String path, FileAlterationListener listener) {
        FileAlterationObserver observer = new FileAlterationObserver(new File(path));
        monitor.addObserver(observer);
        observer.addListener(listener);
    }

    public void stop() throws Exception {
        monitor.stop();
    }

    public void start() throws Exception {
        monitor.start();
    }

    public static void main(String[] args) throws Exception {
        MyFileMonitor m = new MyFileMonitor(5000);
        m.monitor("D:\\zzjz_work\\Projects\\zzts\\src\\main\\resources\\logstash", new MyFileListener());
        m.start();
    }
}
