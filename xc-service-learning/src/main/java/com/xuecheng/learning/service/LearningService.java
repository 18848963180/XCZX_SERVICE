package com.xuecheng.learning.service;

import com.xuecheng.framework.domain.learning.XcLearningCourse;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.dao.XcLearningCourseRepository;
import com.xuecheng.learning.dao.XcTaskHisRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
public class LearningService {

    @Autowired
    XcLearningCourseRepository xcLearningCourseRepository;

    @Autowired
    XcTaskHisRepository xcTaskHisRepository;


    public ResponseResult addcourse(String userId,
                                    String courseId,
                                    String valid,
                                    Date startTime,
                                    Date endTime,
                                    XcTask xcTask) {

        if (StringUtils.isEmpty(courseId)) {
            //ExceptionCast.cast(LearningCode.LEARNING_GETMEDIA_ERROR);
        }
        if (StringUtils.isEmpty(userId)) {
            //ExceptionCast.cast(LearningCode.CHOOSECOURSE_USERISNULL);
        }
        if (xcTask == null || StringUtils.isEmpty(xcTask.getId())) {
            //ExceptionCast.cast(LearningCode.CHOOSECOURSE_TASKISNULL);
        }

        Optional<XcTaskHis> taskHisOptional = xcTaskHisRepository.findById(xcTask.getId());
        if (taskHisOptional.isPresent()) {
            return new ResponseResult(CommonCode.SUCCESS);
        }

        XcLearningCourse xcLearningCourse = xcLearningCourseRepository.findByUserIdAndCourseId(userId, courseId);

        if (Objects.isNull(xcLearningCourse)) {
            xcLearningCourse = new XcLearningCourse();
            xcLearningCourse.setUserId(userId);
            xcLearningCourse.setCourseId(courseId);
            xcLearningCourse.setValid(valid);
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        } else {
            xcLearningCourse.setValid(valid);
            xcLearningCourse.setStartTime(startTime);
            xcLearningCourse.setEndTime(endTime);
            xcLearningCourse.setStatus("501001");
            xcLearningCourseRepository.save(xcLearningCourse);
        }

        //向任务历史表中添加数据
        XcTaskHis xcTaskHis = new XcTaskHis();
        BeanUtils.copyProperties(xcTask, xcLearningCourse);
        xcTaskHisRepository.save(xcTaskHis);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
