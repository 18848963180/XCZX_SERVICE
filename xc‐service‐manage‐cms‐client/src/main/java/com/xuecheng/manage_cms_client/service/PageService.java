package com.xuecheng.manage_cms_client.service;


import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Objects;
import java.util.Optional;


@Service
public class PageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageService.class);

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private CmsSiteRepository cmsSiteRepository;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private GridFSBucket gridFSBucket;


    //将html页面保存到物理路径
    public void savePageToServerPath(String pageId) {

        //根据页面id查寻cmspage信息
        CmsPage cmsPage = this.findCmsPageByPageId(pageId);
        if (Objects.isNull(cmsPage)) {
            LOGGER.error("根据页面id查询页面信息为空pageID;{}", pageId);
        }

        //根据站点id查询站点信息
        CmsSite cmsSite = this.findCmsSiteByPageId(cmsPage.getSiteId());
        String htmlFileId = cmsPage.getHtmlFileId();
        //根据模板文件id获取模板文件输入流
        InputStream inputStream = this.findFileById(htmlFileId);
        if (inputStream == null) {
            LOGGER.error("遛信息获取异常:htmlFileId:{}", htmlFileId);
            return;
        }
        String sitePhysicalPath = cmsSite.getSitePhysicalPath();
        String path = sitePhysicalPath + cmsPage.getPagePhysicalPath() + cmsPage.getPageName();

        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(new File(path));
            IOUtils.copy(inputStream, outputStream);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public InputStream findFileById(String fileTemplateId) {

        GridFSFile gff = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileTemplateId)));
        GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gff.getObjectId());
        GridFsResource gridFsResource = new GridFsResource(gff, gridFSDownloadStream);
        try {
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public CmsPage findCmsPageByPageId(String pageId) {
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        return optional.isPresent() ? optional.get() : null;
    }

    public CmsSite findCmsSiteByPageId(String pageId) {
        Optional<CmsSite> optional = cmsSiteRepository.findById(pageId);
        return optional.isPresent() ? optional.get() : null;
    }
}
