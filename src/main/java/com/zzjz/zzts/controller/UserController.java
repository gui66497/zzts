package com.zzjz.zzts.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @author 房桂堂
 * @description UserController
 * @date 2018/7/27 12:01
 */
@Component
@Path("user")
public class UserController {

    @GET
    @Path("sayHello")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String sayHello() {
        return "Hello World";
    }

}
