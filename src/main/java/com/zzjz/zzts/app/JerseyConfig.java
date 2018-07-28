package com.zzjz.zzts.app;

import com.zzjz.zzts.controller.ElasticController;
import com.zzjz.zzts.controller.UserController;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.stereotype.Component;
import javax.ws.rs.ApplicationPath;

/**
 * @author 房桂堂
 * @description JerseyConfig
 * @date 2018/7/27 12:52
 */
@Component
@ApplicationPath("zzts")
public class JerseyConfig extends ResourceConfig {
    public JerseyConfig () {
        //构造函数，在这里注册需要使用的内容，（过滤器，拦截器，API等）
        register(UserController.class);
        register(ElasticController.class);
    }
}
