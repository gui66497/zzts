package com.zzjz.zzts.controller;

import com.zzjz.zzts.Entity.Result;
import com.zzjz.zzts.Entity.SpecialFocus;
import com.zzjz.zzts.service.ElasticService;
import com.zzjz.zzts.util.Constant;
import com.zzjz.zzts.util.ResultUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description 特别关注列表Controller
 * @Author 房桂堂
 * @Date 2019/9/30 14:20
 */
@Component
@Path("special")
public class SpecialFocusController {

    private final static Logger LOGGER = LoggerFactory.getLogger(SpecialFocusController.class);

    @Autowired
    ElasticService elasticService;

    /**
     * 获取特别关注列表
     * @return 列表
     */
    @GET
    @Path("list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result listSpecialFocus() {
        List<SpecialFocus> specialFocusList = elasticService.specialList();
        return ResultUtil.genSuccessResult(specialFocusList);
    }

    /**
     * 新增或修改特别关注项
     * @return 结果
     */
    @POST
    @Path("addOrUpdate")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result addSpecialFocus(SpecialFocus specialFocus) {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));

        if (StringUtils.isBlank(specialFocus.getEvent()) || StringUtils.isBlank(specialFocus.getEventType())) {
            return ResultUtil.genFailResult("参数不正确");
        }
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("eventType", specialFocus.getEventType());
        jsonMap.put("event", specialFocus.getEvent());
        Result result = ResultUtil.genSuccessResult();
        if (StringUtils.isBlank(specialFocus.getId())) {
            // 新增
            IndexRequest indexRequest = new IndexRequest(Constant.SPECIALFOCUS_INDEX, "doc");
            indexRequest.source(jsonMap);
            try {
                IndexResponse indexResponse = client.index(indexRequest);
                indexResponse.getResult();
                result.setMessage("新增成功");
            } catch (Exception e) {
                e.printStackTrace();
                return ResultUtil.genFailResult(e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 修改
            UpdateRequest updateRequest = new UpdateRequest(Constant.SPECIALFOCUS_INDEX, "doc", specialFocus.getId());
            updateRequest.doc(jsonMap);
            try {
                UpdateResponse updateResponse = client.update(updateRequest);
                result.setMessage("修改成功");
            } catch (Exception e) {
                e.printStackTrace();
                return ResultUtil.genFailResult(e.getMessage());
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * 删除特别关注
     * @param id id
     * @return 结果
     */
    @DELETE
    @Path("delete/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Result listSpecialFocus(@PathParam("id") String id) {
        if (StringUtils.isBlank(id)) {
            return ResultUtil.genFailResult("参数不正确");
        }
        Result result = ResultUtil.genSuccessResult();
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(Constant.ES_HOST, Constant.ES_PORT, Constant.ES_METHOD)));

        DeleteRequest deleteRequest = new DeleteRequest(Constant.SPECIALFOCUS_INDEX, "doc", id);
        try {
            DeleteResponse deleteResponse = client.delete(deleteRequest);
            result.setMessage("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.genFailResult(e.getMessage());
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

}
