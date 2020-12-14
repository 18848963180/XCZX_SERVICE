package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class CmsConfigService {

    @Autowired
    private CmsConfigRepository cmsConfigRepository;


    public CmsConfig getModel(String id) {
        Optional<CmsConfig> cmsConfig = cmsConfigRepository.findById(id);
        return cmsConfig.isPresent() ? cmsConfig.get() : null;
    }
}
