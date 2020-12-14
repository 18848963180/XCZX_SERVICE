package com.xuecheng.manage_course.service;


import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.manage_course.dao.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;

@Service
@Transactional
public class CategoryService {

    @Autowired
    CategoryMapper categoryMapper;

    public CategoryNode findList() {
        return categoryMapper.selectList();
    }
}
