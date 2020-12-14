package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EsCourseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EsCourseService.class);

    @Autowired
    RestHighLevelClient highLevelClient;

    @Value("${xuecheng.elasticsearch.course.index}")
    String es_index;

    @Value("${xuecheng.elasticsearch.course.type}")
    String es_type;

    @Value("${xuecheng.elasticsearch.course.source_field}")
    String es_filed;

    /**
     * es查询课程信息
     *
     * @param page
     * @param size
     * @param courseSearchParam
     * @return
     */
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        SearchRequest searchRequest = new SearchRequest(es_index);
        searchRequest.types(es_index);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //设置过滤的字段
        searchSourceBuilder.fetchSource(es_filed.split(","), new String[]{});

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //根据关键字查询name等字段
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            boolQueryBuilder.must(
                    QueryBuilders
                            .multiMatchQuery(courseSearchParam.getKeyword(), "name", "teachplan", "description")
                            .minimumShouldMatch("70%")
                            .field("neme", 10));
        }

        //根据一级分类过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }

        //根据二级分类过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", courseSearchParam.getSt()));
        }

        //这个是根据啥过滤
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }

        //构建查询
        searchSourceBuilder.query(boolQueryBuilder);
        //设置分页
        if (page <= 0) {
            page = 1;
        }
        if (size <= 0) {
            size = 20;
        }
        searchSourceBuilder.from((page - 1) * size);
        searchSourceBuilder.size(size);

        searchSourceBuilder.highlighter(
                new HighlightBuilder()
                        .field("name")
                        .preTags("<font class='eslight'>")
                        .postTags("</font>"));
        //构建查询源
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = null;
        try {
            //查询
            searchResponse = highLevelClient.search(searchRequest);

        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("xuecheng search error..{}", e.getMessage());
            return new QueryResponseResult(CommonCode.SUCCESS, new QueryResult<CoursePub>());
        }

        //获取结果集
        SearchHits searchHits = searchResponse.getHits();

        long totalHits = searchHits.getTotalHits();

        SearchHit[] hits = searchHits.getHits();


        List<CoursePub> resultList = new ArrayList<>(size);
        for (SearchHit hit : hits) {
            CoursePub coursePub = new CoursePub();

            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            String name = (String) sourceAsMap.get("name");
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField nameHlight = highlightFields.get("name");
            if (Objects.isNull(nameHlight)) {
                Text[] fragments = nameHlight.getFragments();
                StringBuilder sb = new StringBuilder();
                for (Text fragment : fragments) {
                    sb.append(fragment.string());
                }
                name = sb.toString();
            }

            //取出名称
            coursePub.setName(name);
            //图片
            coursePub.setPic((String) sourceAsMap.get("pic"));
            //价格
            coursePub.setPrice((Double) sourceAsMap.get("price"));
            coursePub.setPrice_old((Double) sourceAsMap.get("price_old"));
            resultList.add(coursePub);
        }
        QueryResult<CoursePub> queryResult = new QueryResult<CoursePub>();
        queryResult.setTotal(totalHits);
        queryResult.setList(resultList);
        return new QueryResponseResult<CoursePub>(CommonCode.SUCCESS, queryResult);
    }
}
