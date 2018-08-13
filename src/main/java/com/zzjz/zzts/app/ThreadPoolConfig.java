package com.zzjz.zzts.app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author 房桂堂
 * @description ThreadPoolConfig
 * @date 2018/8/13 14:29
 */
@Configuration
public class ThreadPoolConfig {

    @Bean
    public ExecutorService getThreadPool(){
        return Executors.newFixedThreadPool(3);
    }
}
