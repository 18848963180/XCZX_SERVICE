package com.xuecheng.order.mq;


import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class ChooseCourseTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChooseCourseTask.class);


    @Autowired
    TaskService taskService;

    @RabbitListener(queues = RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE)
    public void receiveFinishChoosecourseTask(XcTask xcTask) {
        LOGGER.info("receiveChoosecourseTask...{}", xcTask.getId());
        //接收到 的消息id
        String id = xcTask.getId();
        //删除任务，添加历史任务
        taskService.delTask(id);
    }

    @Scheduled(fixedDelay = 6000)
    public void sendChoosecourseTask() {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.MINUTE, -1);
        List<XcTask> taskList = taskService.findTaskList(100, calendar.getTime());

        for (XcTask xcTask : taskList) {

            if (taskService.getTask(xcTask.getId(), xcTask.getVersion()) > 0) {

                taskService.publish(xcTask, xcTask.getMqExchange(), xcTask.getMqRoutingkey());
                LOGGER.info("send choose course task id:{}", xcTask.getId());
            }
        }
    }


}
