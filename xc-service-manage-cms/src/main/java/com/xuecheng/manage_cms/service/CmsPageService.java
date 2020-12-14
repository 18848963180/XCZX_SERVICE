package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitMqConfig;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


@Service
public class CmsPageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CmsSiteRepository cmsSiteRepository;

    public QueryResponseResult findPageList(int page, int size, QueryPageRequest queryPageRequest) {

        if (Objects.isNull(queryPageRequest)) {
            queryPageRequest = new QueryPageRequest();
        }

        page = page <= 0 ? 0 : page - 1;
        size = size <= 0 ? 10 : size;
        Pageable p = PageRequest.of(page, size);
        CmsPage cms = new CmsPage();
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())) {
            //根据站点id查询
            cms.setSiteId(queryPageRequest.getSiteId());
        }
        if (StringUtils.isNotEmpty(queryPageRequest.getTemplateId())) {
            //根据页面模板id查询
            cms.setTemplateId(queryPageRequest.getTemplateId());
        }

        if (StringUtils.isNotEmpty(queryPageRequest.getPageAliase())) {
            //根据页面别名查询
            cms.setPageAliase(queryPageRequest.getPageAliase());
        }

        //查询条件设置器
        Example<CmsPage> e = Example.of(cms,
                //ExampleMatcher.matching().withMatcher:条件过滤规则
                ExampleMatcher.matching().withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains()));
        Page<CmsPage> all = cmsPageRepository.findAll(e, p);
        QueryResult queryResult = new QueryResult();
        queryResult.setList(all.getContent());
        queryResult.setTotal(all.getTotalElements());
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS, queryResult);
        return queryResponseResult;
    }

    public CmsPageResult add(CmsPage cmsPage) {
        CmsPage all = cmsPageRepository.
                findByPageNameAndSiteIdAndAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (Objects.isNull(all)) {
            CmsPage save = cmsPageRepository.save(cmsPage);
            return new CmsPageResult(CommonCode.SUCCESS, save);
        }
        ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        return new CmsPageResult(CommonCode.FAIL, null);

    }

    public CmsPage findById(String id) {
        Optional<CmsPage> cmsPage = cmsPageRepository.findById(id);

        return cmsPage.isPresent() ? cmsPage.get() : null;

    }

    public CmsPageResult edit(String id, CmsPage cmsPage) {
        CmsPage one = this.findById(id);
        if (one != null) {
            //更新模板id
            one.setTemplateId(cmsPage.getTemplateId());
            //更新所属站点
            one.setSiteId(cmsPage.getSiteId());
            //更新页面别名
            one.setPageAliase(cmsPage.getPageAliase());
            //更新页面名称
            one.setPageName(cmsPage.getPageName());
            //更新访问路径
            one.setPageWebPath(cmsPage.getPageWebPath());
            //页面类型
            one.setPageType(cmsPage.getPageType());
            //更新数据路径
            one.setDataUrl(cmsPage.getDataUrl());
            //更新物理路径
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            //执行更新
            CmsPage save = cmsPageRepository.save(one);
            if (save != null) {
                //返回成功
                CmsPageResult cmsPageResult = new CmsPageResult(CommonCode.SUCCESS, save);
                return cmsPageResult;
            }
        }
        //返回失败
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    public ResponseResult delete(String id) {
        CmsPage one = this.findById(id);
        if (one != null) {
            //删除页面
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    public String getPageHtml(String pageId) {
        CmsPage cmsPage = this.findById(pageId);
        if (Objects.isNull(cmsPage)) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        //获取页面模型数据
        Map model = this.getModelByPageId(cmsPage.getDataUrl());

        if (model == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //获取页面模板数据
        String templateContent = this.getTemplatePageId(cmsPage.getTemplateId());
        if (StringUtils.isBlank(templateContent)) {
            //页面模板为空
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //生成html页面
        String html = this.generateHtml(model, templateContent);

        if (StringUtils.isBlank(html)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        return html;
    }

    private String generateHtml(Map model, String templateContent) {

        try {
            //获取freemarker模板配置类
            Configuration configuration = new Configuration(Configuration.getVersion());
            //获取模板加载器
            StringTemplateLoader templateLoader = new StringTemplateLoader();
            templateLoader.putTemplate("template", templateContent);

            //设置模板加载器
            configuration.setTemplateLoader(templateLoader);
            //获取模板
            Template template = configuration.getTemplate("template");
            //填充数据
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            return html;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTemplatePageId(String templateId) {
        if (StringUtils.isBlank(templateId)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        Optional<CmsTemplate> temOpt = cmsTemplateRepository.findById(templateId);
        if (!temOpt.isPresent()) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        CmsTemplate cmsTemplate = temOpt.get();

        //模板文件id
        String templateFileId = cmsTemplate.getTemplateFileId();
        //使用gridfsTemplate获取文件对象
        GridFSFile gfsFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
        //打开下载流
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gfsFile.getObjectId());
        GridFsResource gridFsResource = new GridFsResource(gfsFile, gridFSDownloadStream);
        try {
            String content = IOUtils.toString(gridFsResource.getInputStream(), "utf-8");
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Map getModelByPageId(String dataUrl) {

        if (StringUtils.isBlank(dataUrl)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        return forEntity.getBody();
    }

    public ResponseResult postPage(String pageId) {

        //获取页面html文档
        String pageHtml = this.getPageHtml(pageId);
        if (StringUtils.isEmpty(pageHtml)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_HTMLISNULL);
        }
        //剥保存静态文件
        CmsPage cmsPage = this.saveHtml(pageId, pageHtml);
        this.sendPostPage(cmsPage);
        //mq发送消息
        return new ResponseResult(CommonCode.SUCCESS);
    }

    private void sendPostPage(CmsPage cmsPage) {
        String pageId1 = JSON.toJSONString(Collections.singletonMap("pageId", cmsPage.getPageId()));
        rabbitTemplate.convertAndSend(RabbitMqConfig.EX_ROUTING_CMS_POSTPAGE, cmsPage.getSiteId(), pageId1);

    }

    private CmsPage saveHtml(String cmsPageId, String htmlStr) {
        CmsPage cmsPage = this.findById(cmsPageId);
        if (cmsPage == null) {
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }
        if (StringUtils.isNotBlank(cmsPage.getHtmlFileId())) {
            gridFsTemplate.delete(Query.query(Criteria.where("id").is(cmsPage.getHtmlFileId())));
        }

        InputStream inputStream = null;
        ObjectId objectId = null;
        try {
            inputStream = IOUtils.toInputStream(htmlStr, "utf-8");
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    public CmsPageResult save(CmsPage cmsPage) {
        CmsPage one = cmsPageRepository.findByPageNameAndSiteIdAndAndPageWebPath(cmsPage.getPageName(),
                cmsPage.getSiteId(),
                cmsPage.getPageWebPath());

        if (Objects.isNull(one)) {
            return this.add(cmsPage);
        }

        return this.edit(one.getPageId(), cmsPage);
    }

    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        //添加页面
        CmsPageResult cmsPageResult = this.save(cmsPage);
        if (!cmsPageResult.isSuccess()) {
            //保存失败
            return new CmsPostPageResult(CommonCode.FAIL, null);
        }

        CmsPage cmsPage1 = cmsPageResult.getCmsPage();

        String pageId = cmsPage1.getPageId();

        ResponseResult responseResult = this.postPage(pageId);
        if (!responseResult.isSuccess()) {
            return new CmsPostPageResult(CommonCode.FAIL, null);
        }

        //查询站点信息 拼装路径
        CmsSite site = this.findCmsSiteById(cmsPage1.getSiteId());
        if (Objects.isNull(site)) {
            return new CmsPostPageResult(CommonCode.FAIL, null);
        }
        String pageUrl = site.getSiteDomain() + site.getSiteWebPath() + cmsPage1.getPageWebPath() + cmsPage1.getPageName();

        return new CmsPostPageResult(CommonCode.SUCCESS, pageUrl);
    }

    private CmsSite findCmsSiteById(String siteId) {
        Optional<CmsSite> siteOptional = cmsSiteRepository.findById(siteId);
        return siteOptional.isPresent() ? siteOptional.get() : null;
    }
}
