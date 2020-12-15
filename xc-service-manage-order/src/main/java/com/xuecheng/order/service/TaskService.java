package com.xuecheng.order.service;


import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {
    @Autowired
    XcTaskRepository taskRepository;

    @Autowired
    XcTaskHisRepository xcTaskHisRepository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    public List<XcTask> findTaskList(int count, Date updateTime) {
        Pageable pageAble = new PageRequest(0, count);
        Page<XcTask> byUpdateTimeBefore = taskRepository.findByUpdateTimeBefore(pageAble, updateTime);
        return byUpdateTimeBefore.getContent();
    }

    @Transactional
    public void publish(XcTask xcTask, String ex, String routingKey) {

        Optional<XcTask> byId = taskRepository.findById(xcTask.getId());
        if (byId.isPresent()) {
            XcTask xcTaskOne = byId.get();
            //发送消息
            rabbitTemplate.convertAndSend(ex, routingKey, xcTask);
            xcTaskOne.setUpdateTime(new Date());
            taskRepository.save(xcTaskOne);
        }
    }

    @Transactional
    public int getTask(String taskId, int version) {
        return taskRepository.updateTaskVersion(taskId, version);
    }

    @Transactional
    public void delTask(String taskId) {

        Optional<XcTask> taskOptional = taskRepository.findById(taskId);
        if (taskOptional.isPresent()) {
            XcTask xcTask = taskOptional.get();
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask, xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            taskRepository.delete(xcTask);
        }
    }
}
