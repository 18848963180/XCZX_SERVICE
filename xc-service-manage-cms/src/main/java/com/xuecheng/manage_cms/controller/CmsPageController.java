package com.xuecheng.manage_cms.controller;

import com.xuecheng.api.cms.CmsPageControllerApi;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.service.CmsPageService;
import com.xuecheng.manage_cms.service.CmsSiteService;
import com.xuecheng.manage_cms.service.CmsTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;


@RestController
@RequestMapping("/cms/page")
public class CmsPageController implements CmsPageControllerApi {

    @Autowired
    private CmsPageService cmsPageService;

    @Autowired
    private CmsTemplateService cmsTemplateService;

    @Autowired
    private CmsSiteService cmsSiteService;

    @Override
    @GetMapping("/list/{page}/{size}")
    public QueryResponseResult findList(@PathVariable("page") int page,
                                        @PathVariable("size") int size,
                                        QueryPageRequest queryPageRequest) {
        return cmsPageService.findPageList(page, size, queryPageRequest);
    }

    @Override
    @PostMapping("add")
    public CmsPageResult add(@RequestBody CmsPage cmsPage) {
        return cmsPageService.add(cmsPage);
    }

    @Override
    @GetMapping("get/{id}")
    public CmsPageResult findById(@PathVariable("id") String id) {
        CmsPage cmsPage = cmsPageService.findById(id);
        return new CmsPageResult(Objects.isNull(cmsPage) ? CommonCode.FAIL : CommonCode.SUCCESS, cmsPage);
    }

    @Override
    @PutMapping("edit/{id}")
    public CmsPageResult edit(@PathVariable("id") String id, @RequestBody CmsPage cmsPage) {
        return cmsPageService.edit(id, cmsPage);
    }

    @Override
    @PostMapping("postPage/{pageId}")
    public ResponseResult postPage(@PathVariable("pageId") String pageId) {
        return cmsPageService.postPage(pageId);
    }

    @Override
    @DeleteMapping("del/{id}")
    public ResponseResult delete(@PathVariable("id") String id) {
        return cmsPageService.delete(id);
    }

    @Override
    @GetMapping("/template/list")
    public QueryResponseResult findAllTemplateList() {
        return cmsTemplateService.findAllTemplateList();
    }

    @Override
    @GetMapping("/site/list")
    public QueryResponseResult findAllSiteList() {
        return cmsSiteService.findAllSiteList();
    }

    @Override
    @PostMapping("/save")
    public CmsPageResult save(@RequestBody CmsPage cmsPage) {
        return cmsPageService.save(cmsPage);
    }

    @Override
    @PostMapping("/postPageQuick")
    public CmsPostPageResult postPageQuick(@RequestBody CmsPage cmsPage) {
        return cmsPageService.postPageQuick(cmsPage);
    }

}
