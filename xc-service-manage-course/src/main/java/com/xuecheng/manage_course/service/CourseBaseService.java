package com.xuecheng.manage_course.service;


import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.cms.response.CoursePublishResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class CourseBaseService {

    @Autowired
    CourseMapper courseMapper;

    @Autowired
    CourseBaseRepository courseBaseRepository;

    @Autowired
    CourseMarketRepository courseMarketRepository;

    @Autowired
    CoursePicRepository coursePicRepository;

    @Autowired
    CoursePubRepository coursePubRepository;

    @Autowired
    TeachplanMapper teachplanMapper;

    @Autowired
    TeachplanRepository teachplanRepository;

    @Autowired
    TeachplanMediaRepository teachplanMediaRepository;

    @Autowired
    CmsPageClient cmsPageClient;

    @Value("${course‐publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course‐publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course‐publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course‐publish.siteId}")
    private String publish_siteId;
    @Value("${course‐publish.templateId}")
    private String publish_templateId;
    @Value("${course‐publish.previewUrl}")
    private String previewUrl;


    public QueryResponseResult findCourseList(int page, int size, CourseListRequest courseListRequest) {
        if (Objects.isNull(courseListRequest)) {
            courseListRequest = new CourseListRequest();
        }
        if (page <= 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }

        PageHelper.startPage(page, size);
        Page<CourseInfo> courseList = courseMapper.findCourseListPage(courseListRequest);

        List<CourseInfo> result = courseList.getResult();
        long total = courseList.getTotal();
        QueryResult<CourseInfo> courseIncfoQueryResult = new QueryResult<CourseInfo>();
        courseIncfoQueryResult.setList(result);
        courseIncfoQueryResult.setTotal(total);
        return new QueryResponseResult(CommonCode.SUCCESS, courseIncfoQueryResult);
    }

    public AddCourseResult addCourseBase(CourseBase courseBase) {
        courseBase.setStatus("202001");
        CourseBase save = courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS, save.getId());
    }

    public CourseBase getCourseBaseById(String courseId) {
        Optional<CourseBase> opt = courseBaseRepository.findById(courseId);
        return opt.isPresent() ? opt.get() : null;
    }

    public ResponseResult updateCourseBase(String id, CourseBase courseBase) {
        CourseBase courseBaseById = this.getCourseBaseById(id);

        if (Objects.isNull(courseBaseById)) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //修改课程信息
        courseBaseById.setName(courseBase.getName());
        courseBaseById.setMt(courseBase.getMt());
        courseBaseById.setSt(courseBase.getSt());
        courseBaseById.setGrade(courseBase.getGrade());
        courseBaseById.setStudymodel(courseBase.getStudymodel());
        courseBaseById.setUsers(courseBase.getUsers());
        courseBaseById.setDescription(courseBase.getDescription());
        CourseBase save = courseBaseRepository.save(courseBaseById);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CourseMarket getCourseMarketById(String courseId) {
        Optional<CourseMarket> opt = courseMarketRepository.findById(courseId);
        return opt.isPresent() ? opt.get() : null;
    }

    public ResponseResult updateCourseMarket(String id, CourseMarket courseMarket) {
        CourseMarket courseMarketById = this.getCourseMarketById(id);
        if (courseMarketById != null) {
            courseMarketById.setCharge(courseMarket.getCharge());
            courseMarketById.setStartTime(courseMarket.getStartTime());//课程有效期，开始时间
            courseMarketById.setEndTime(courseMarket.getEndTime());//课程有效期，结束时间
            courseMarketById.setPrice(courseMarket.getPrice());
            courseMarketById.setQq(courseMarket.getQq());
            courseMarketById.setValid(courseMarket.getValid());
            courseMarketRepository.save(courseMarketById);
        } else {
            //添加课程营销信息
            courseMarketById = new CourseMarket();
            BeanUtils.copyProperties(courseMarket, courseMarketById);
            //设置课程id
            courseMarketById.setId(id);
            courseMarketRepository.save(courseMarketById);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    public CourseView getCourseView(String id) {
        CourseView courseView = new CourseView();
        //课程基础信息
        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            courseView.setCourseBase(courseBaseOptional.get());
        }
        //课程图片信息
        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()) {
            courseView.setCoursePic(coursePicOptional.get());
        }
        //课程营销信息
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()) {
            courseView.setCourseMarket(marketOptional.get());
        }
        //课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        if (!Objects.isNull(teachplanNode)) {
            courseView.setTeachplanNode(teachplanNode);
        }

        return courseView;
    }

    /**
     * 预览课程
     *
     * @param id
     * @return
     */
    public CoursePublishResult preview(String id) {
        CourseBase courseBase = this.getCourseBaseById(id);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(id + ".html");
        //页面别名
        cmsPage.setPageAliase(courseBase.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre + id);

        CmsPageResult cmsPageResult = cmsPageClient.save(cmsPage);
        if (!CmsPageResult.SUCCESS) {
            return new CoursePublishResult(CommonCode.FAIL, null);
        }
        String pageId = cmsPageResult.getCmsPage().getPageId();
        String url = previewUrl + pageId;
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    public CoursePublishResult publish(String id) {
        CourseBase courseBase = this.getCourseBaseById(id);
        //发布课程预览页面
        CmsPage cmsPage = new CmsPage();
        //站点
        cmsPage.setSiteId(publish_siteId);//课程预览站点
        //模板
        cmsPage.setTemplateId(publish_templateId);
        //页面名称
        cmsPage.setPageName(id + ".html");
        //页面别名
        cmsPage.setPageAliase(courseBase.getName());
        //页面访问路径
        cmsPage.setPageWebPath(publish_page_webpath);
        //页面存储路径
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        //数据url
        cmsPage.setDataUrl(publish_dataUrlPre + id);

        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);

        if (!cmsPostPageResult.isSuccess()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }

        this.saveCoursePubState(id);

        CoursePub coursePub = this.createCoursePub(id);
        this.saveCoursePub(id, coursePub);

        return new CoursePublishResult(CommonCode.SUCCESS, cmsPostPageResult.getPageUrl());
    }

    private void saveCoursePub(String id, CoursePub coursePub) {
        if (StringUtils.isEmpty(id)) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_COURSEIDISNULL);
        }
        Optional<CoursePub> coursePubOptional = coursePubRepository.findById(id);
        CoursePub coursePubNew = coursePubOptional.isPresent() ? coursePubOptional.get() : new CoursePub();

        BeanUtils.copyProperties(coursePub, coursePubNew);
        coursePubNew.setId(id);
        coursePubNew.setTimestamp(new Date());
        coursePubNew.setPubTime(new SimpleDateFormat("YYYY‐MM‐dd HH:mm:ss").format(new Date()));
        CoursePub saveCoursePub = coursePubRepository.save(coursePubNew);
        if (Objects.isNull(saveCoursePub)) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }
    }

    private CoursePub createCoursePub(String id) {
        CoursePub coursePub = new CoursePub();

        Optional<CourseBase> courseBaseOptional = courseBaseRepository.findById(id);
        if (courseBaseOptional.isPresent()) {
            BeanUtils.copyProperties(courseBaseOptional.get(), coursePub);
        }

        Optional<CoursePic> coursePicOptional = coursePicRepository.findById(id);
        if (coursePicOptional.isPresent()) {
            BeanUtils.copyProperties(coursePicOptional.get(), coursePub);
        }

        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if (courseMarketOptional.isPresent()) {
            BeanUtils.copyProperties(courseMarketOptional.get(), coursePub);
        }
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String nodeListJson = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(nodeListJson);
        return coursePub;
    }

    private void saveCoursePubState(String id) {
        CourseBase courseBase = this.getCourseBaseById(id);
        courseBase.setStatus("202002");
        courseBaseRepository.save(courseBase);
    }

    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if (Objects.isNull(teachplanMedia)) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(teachplanId);
        if (!teachplanOptional.isPresent()) {
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Teachplan teachplan = teachplanOptional.get();
        String grade = teachplan.getGrade();
        if (StringUtils.isEmpty(grade) || !grade.equals("3")) {
            ExceptionCast.cast(CourseCode.COURSE_PUBLISH_CREATE_INDEX_ERROR);
        }
        Optional<TeachplanMedia> teachplanMediaOptional = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia one = teachplanMediaOptional.isPresent() ? teachplanMediaOptional.get() : new TeachplanMedia();
        //保存媒资信息与课程计划信息
        one.setTeachplanId(teachplanId);
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
