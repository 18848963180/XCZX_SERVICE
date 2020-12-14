package com.xuecheng.api.couser;

import com.xuecheng.framework.domain.cms.response.CoursePublishResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "课程管理接口", description = "课程管理")
public interface CouserControllerApi {

    @ApiOperation("课程计划查询")
    public TeachplanNode findTeachplanList(String courseId);

    @ApiOperation("添加课程计划")
    public ResponseResult addTeachplan(Teachplan teachplan);

    @ApiOperation("查询课程列表")
    public QueryResponseResult findCourseList(int page,
                                              int size,
                                              CourseListRequest courseListRequest);

    @ApiOperation("获取课程基础信息")
    public CourseBase getCourseBaseById(String courseId);

    @ApiOperation("更新课程基础信息")
    public ResponseResult updateCourseBase(String id,CourseBase courseBase);

    @ApiOperation("添加课程信息")
    public AddCourseResult addCourseBase(CourseBase courseBase);

    @ApiOperation(" 获取课程营销信息")
    public CourseMarket getCourseMarketById(String courseId);

    @ApiOperation(" 更新课程营销信息")
    public ResponseResult updateCourseMarket(String id,CourseMarket courseMarket);

    @ApiOperation("上传课程图片")
    public ResponseResult addCoursePic(String courseId,String pic);

    @ApiOperation("获取课程基础图片信息")
    public CoursePic findCoursePic(String courseId);

    @ApiOperation("删除课程基础图片信息")
    public ResponseResult deleteCoursePic(String courseId);

    @ApiOperation("课程视图信息")
    public CourseView courseview(String id);

    @ApiOperation("课程预览")
    public CoursePublishResult preview(String id);

    @ApiOperation("课程发布")
    public CoursePublishResult publish(String id);

    @ApiOperation("保存媒资信息")
    public ResponseResult savemedia(TeachplanMedia teachplanMedia);
}
