package com.xuecheng.manage_course.service;


import com.xuecheng.framework.domain.course.CourseBase;
import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.dao.CourseBaseRepository;
import com.xuecheng.manage_course.dao.TeachplanMapper;
import com.xuecheng.manage_course.dao.TeachplanRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TeachplanService {

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    public TeachplanNode findTeachplanList(String courseId) {
        return teachplanMapper.selectList(courseId);
    }


    public ResponseResult addTeachplan(Teachplan teachplan) {
        //校验课程id和课程计划名称
        String courseid = teachplan.getCourseid();
        if (teachplan == null ||
                StringUtils.isEmpty(courseid) ||
                StringUtils.isEmpty(teachplan.getPname())) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }

        String parentid = teachplan.getParentid();

        if (StringUtils.isEmpty(parentid)) {
            //获取课程根结点，如果没有则添加根结点
            parentid = this.getTeachplanRoot(courseid);
        }

        Optional<Teachplan> parentOpt = teachplanRepository.findById(parentid);
        if (!parentOpt.isPresent()) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Teachplan teachplanParent = parentOpt.get();
        String grade = teachplanParent.getGrade();
        teachplan.setParentid(parentid);
        teachplan.setGrade(grade.equals("1") ? "2" : "3");
        teachplan.setStatus("0");
        teachplan.setCourseid(teachplanParent.getCourseid());
        teachplanRepository.save(teachplan);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private String getTeachplanRoot(String courseid) {

        Optional<CourseBase> courseOpt = courseBaseRepository.findById(courseid);

        if (!courseOpt.isPresent()) {
            return null;
        }

        CourseBase courseBase = courseOpt.get();
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseid, "0");

        if (teachplanList == null || teachplanList.size() <= 0) {
            Teachplan teachplanRoot = new Teachplan();
            //新增一个根结点
            teachplanRoot.setCourseid(courseid);
            teachplanRoot.setPname(courseBase.getName());
            teachplanRoot.setParentid("0");
            teachplanRoot.setGrade("1");//1级
            teachplanRoot.setStatus("0");//未发布
            teachplanRepository.save(teachplanRoot);
            return teachplanRoot.getId();
        }
        return teachplanList.get(0).getId();
    }
}
