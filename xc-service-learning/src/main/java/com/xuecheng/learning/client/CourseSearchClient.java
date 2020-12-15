package com.xuecheng.learning.client;

import com.xuecheng.framework.client.XcServiceList;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * Created by Administrator.
 */
@FeignClient(value = XcServiceList.XC_SERVICE_SEARCH)
public interface CourseSearchClient {

}
