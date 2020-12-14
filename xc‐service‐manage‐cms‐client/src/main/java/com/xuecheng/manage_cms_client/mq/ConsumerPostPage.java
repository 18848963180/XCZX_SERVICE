package com.xuecheng.manage_cms_client.mq;


import com.alibaba.fastjson.JSON;
import com.xuecheng.manage_cms_client.service.PageService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

//页面发布信息的消费者
@Component
public class ConsumerPostPage {
    @Autowired
    private PageService pageService;

    @RabbitListener(queues = "${xuecheng.mq.queue}")
    public void postPage(String message) {
        Map map = JSON.parseObject(message, Map.class);
        String pageId = (String) map.get("pageId");
        pageService.savePageToServerPath(pageId);
    }
}
