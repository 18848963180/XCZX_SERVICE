package com.xuecheng.framework.domain.course;

import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class CourseView {

    private CourseBase courseBase;//基础信息
    private CoursePic coursePic;//课程图片信息
    private CourseMarket courseMarket;//课程营销信息
    private TeachplanNode teachplanNode;//课程计划信息
}
