package com.xuecheng.ucenter.service;

import com.xuecheng.framework.domain.ucenter.XcCompanyUser;
import com.xuecheng.framework.domain.ucenter.XcUser;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import com.xuecheng.ucenter.dao.XcCompanyUserRepository;
import com.xuecheng.ucenter.dao.XcUserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class UcenterService {

    @Autowired
    XcUserRepository userRepository;

    @Autowired
    XcCompanyUserRepository companyUserRepository;

    public XcUserExt getUserext(String username) {

        XcUser user = userRepository.findByUsername(username);

        if (Objects.isNull(user)) {
            return null;
        }

        XcUserExt userExt = new XcUserExt();

        BeanUtils.copyProperties(user, userExt);


        XcCompanyUser companyUser = companyUserRepository.findByUserId(userExt.getId());

        if (Objects.isNull(companyUser)) {
            userExt.setCompanyId(companyUser.getCompanyId());
        }


        return userExt;
    }
}
