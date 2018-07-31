package com.zzjz.zzts.util;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;

/**
 * @author 房桂堂
 * @description 文件监控listener
 * @date 2018/7/31 14:17
 */
public class MyFileListener implements FileAlterationListener {

    private final static Logger LOGGER = LoggerFactory.getLogger(MyFileListener.class);

    @Override
    public void onStart(FileAlterationObserver fileAlterationObserver) {
        LOGGER.info("MyFileListener 启动");
    }

    @Override
    public void onDirectoryCreate(File file) {
        LOGGER.info("创建了文件夹 " + file.getName());
    }

    @Override
    public void onDirectoryChange(File file) {
        LOGGER.info("修改了文件夹 " + file.getName());
    }

    @Override
    public void onDirectoryDelete(File file) {
        LOGGER.info("删除了文件夹 " + file.getName());
    }

    @Override
    public void onFileCreate(File file) {
        LOGGER.info("创建了文件名为 " + file.getName() + " 的文件");
        //判断如果是zip或xml类型的文件就调用 解析程序
        String fileName = file.getName();
        String prefix = fileName.substring(fileName.lastIndexOf("."));
        if (Constant.ZIP.equals(prefix)) {
            //zip文件 需要解压
            try {
                UnZipUtil.unZipFiles(file, Constant.MONITOR_PATH);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (Constant.XML.equals(prefix)) {
            LOGGER.info("调用xml2es方法");
            Xml2Es.analysis(file);
        }
    }

    @Override
    public void onFileChange(File file) {
        LOGGER.info("文件更新 " + file.getName());
    }

    @Override
    public void onFileDelete(File file) {
        LOGGER.info("文件删除 " + file.getName());
    }

    @Override
    public void onStop(FileAlterationObserver fileAlterationObserver) {
        //LOGGER.info("MyFileListener 关闭");
    }
}
