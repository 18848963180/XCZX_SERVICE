package com.xuecheng.manage_course.service;


import com.xuecheng.framework.domain.course.CoursePic;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.dao.CoursePicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
@Transactional
public class CoursePicService {

    @Autowired
    CoursePicRepository coursePicRepository;

    public ResponseResult addCoursePic(String courseId, String pic) {
        Optional<CoursePic> opt = coursePicRepository.findById(courseId);
        CoursePic coursePic = opt.isPresent() ? opt.get() : new CoursePic();
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CoursePic findCoursePic(String courseId) {
        Optional<CoursePic> opt = coursePicRepository.findById(courseId);
        return opt.isPresent() ? opt.get() : null;
    }

    public ResponseResult deleteCoursePic(String courseId) {
        long num = coursePicRepository.deleteByCourseid(courseId);
        return new ResponseResult(num > 0 ? CommonCode.SUCCESS : CommonCode.FAIL);
    }
}
