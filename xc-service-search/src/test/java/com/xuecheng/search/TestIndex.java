package com.xuecheng.search;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestIndex {

    @Autowired
    RestHighLevelClient highLevelClient;

    @Autowired
    RestClient restClient;


    //新建索引库
    @Test
    public void testCreateIndex() throws IOException {
        //创建索引
        CreateIndexRequest xc_cource = new CreateIndexRequest("xc_course");
        //索引表设置
        xc_cource.settings(Settings.builder().put("number_of_shards", 1).put("number_of_replicas", 0));
        //创建映射
        xc_cource.mapping("doc", "{\n" +
                "                \"properties\": {\n" +
                "                    \"description\": {\n" +
                "                        \"type\": \"text\",\n" +
                "                        \"analyzer\": \"ik_max_word\",\n" +
                "                        \"search_analyzer\": \"ik_smart\"\n" +
                "                    },\n" +
                "                    \"name\": {\n" +
                "                        \"type\": \"text\",\n" +
                "                        \"analyzer\": \"ik_max_word\",\n" +
                "                        \"search_analyzer\": \"ik_smart\"\n" +
                "                    },\n" +
                "        \"pic\":{                    \n" +
                "            \"type\":\"text\",                        \n" +
                "            \"index\":false                        \n" +
                "        },                    \n" +
                "                    \"price\": {\n" +
                "                        \"type\": \"float\"\n" +
                "                    },\n" +
                "                    \"studymodel\": {\n" +
                "                        \"type\": \"keyword\"\n" +
                "                    },\n" +
                "                    \"timestamp\": {\n" +
                "                        \"type\": \"date\",\n" +
                "                        \"format\": \"yyyy‐MM‐dd HH:mm:ss||yyyy‐MM‐dd||epoch_millis\"\n" +
                "                    }\n" +
                "                }\n" +
                "            }", XContentType.JSON);
        //创建索引操作客户端
        IndicesClient indices = highLevelClient.indices();
        //创建响应对象
        CreateIndexResponse createIndexResponse = indices.create(xc_cource);
        //得到响应结果
        boolean acknowledged = createIndexResponse.isAcknowledged();
    }

    @Test
    public void testDeleteIndex() throws IOException {
        //删除索引的请求对象
        DeleteIndexRequest xc_course = new DeleteIndexRequest("xc_course");
//        执行删除索引的动作
        DeleteIndexResponse delete = highLevelClient.indices().delete(xc_course);
//        删除索引的结果
        boolean acknowledged = delete.isAcknowledged();
    }

    @Test
    public void testAddDoc() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "spring cloud实战");
        map.put("description", "本课程主要从四个章节进行讲解： 1.微服务架构入门 2.spring cloud基础入门 3.实战Spring Boot 4.注册中心eureka。");
        map.put("studymodel", "201001");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy‐MM‐dd HH:mm:ss");
        map.put("timestamp", dateFormat.format(new Date()));
        map.put("price", 5.6f);

        //索引请求对象
        IndexRequest indexRequest = new IndexRequest("xc_course", "doc");
        //指定索引文档内容
        indexRequest.source(map);
        IndexResponse index = highLevelClient.index(indexRequest);
        System.out.println(index.getResult());
    }


    @Test
    public void getDoc() throws IOException {
        GetRequest getRequest = new GetRequest("xc_course", "doc", "KU6CI3YBTtQrVW2fvbta");
        GetResponse documentFields = highLevelClient.get(getRequest);
        boolean exists = documentFields.isExists();
        System.out.println(exists);
        Map<String, Object> sourceAsMap = documentFields.getSourceAsMap();
        System.out.println(sourceAsMap);
    }

    @Test
    public void testSearchAll() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});
        //设置分页
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(2);
        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }

    @Test
    public void testTermQuery() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
//        searchSourceBuilder.query(QueryBuilders.termQuery("name","spring"));
        searchSourceBuilder.query(QueryBuilders.termQuery("_id",
                Arrays.asList(new String[]{"1", "2", "5"})));
        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});

        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }

    @Test
    public void testMatchQuery() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
        //会对关键字进行拆词,如 spring开发 拆成 spring 开发  or 代表只要有其一即可
        searchSourceBuilder.query(QueryBuilders.matchQuery("description", "spring开发").operator(Operator.OR));
        //百分之80 匹配上即可
        searchSourceBuilder.query(QueryBuilders.matchQuery("description", "spring开发").minimumShouldMatch("80%"));
        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});

        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }

    //多条件匹配查询
    @Test
    public void testMultiQuery() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
        //多条件匹配 第一个参数为要查询的值,后面为根据那些字段进行查询 field:需要加权的字段
        searchSourceBuilder.query(QueryBuilders.
                multiMatchQuery("spring 框架", "name", "description").
                minimumShouldMatch("50%").field("name", 10));


        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});

        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }

    //布尔查询,多种查询方式一起使用
    @Test
    public void testBoolQuery() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
        //多条件匹配 第一个参数为要查询的值,后面为根据那些字段进行查询 field:需要加权的字段
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.
                multiMatchQuery("spring 框架", "name", "description").
                minimumShouldMatch("50%").field("name", 10);
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("studymodel", "201001");
        //布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //绑定查询关系
//        must：表示必须，多个查询条件必须都满足。（通常使用must）
//        should：表示或者，多个查询条件只要有一个满足即可。
//        must_not：表示非。
        boolQueryBuilder.must(multiMatchQueryBuilder);
        boolQueryBuilder.must(termQueryBuilder);

        searchSourceBuilder.query(boolQueryBuilder);


        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});

        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }

    //过滤器
    @Test
    public void testFilter() throws IOException {
        //查询请求
        SearchRequest xc_course = new SearchRequest("xc_course");
        xc_course.types("doc");
        //查询条件构建
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //设置查询方式
        //多条件匹配 第一个参数为要查询的值,后面为根据那些字段进行查询 field:需要加权的字段
        MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.
                multiMatchQuery("spring 框架", "name", "description").
                minimumShouldMatch("50%").field("name", 10);
        //布尔查询
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //绑定查询关系
//        must：表示必须，多个查询条件必须都满足。（通常使用must）
//        should：表示或者，多个查询条件只要有一个满足即可。
//        must_not：表示非。
        boolQueryBuilder.must(multiMatchQueryBuilder);

        //过滤查询
        boolQueryBuilder.filter(QueryBuilders.termQuery("studymodel", "201001"));
        //rangeQuery:范围查询
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gt(50).lt(100));


        searchSourceBuilder.query(boolQueryBuilder);

        //排序
        searchSourceBuilder.sort(new FieldSortBuilder("price").order(SortOrder.DESC));


        //高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        //设置高亮的标签
        highlightBuilder.preTags("<tag>");
        highlightBuilder.postTags("</tag");
        //设置需要高亮的字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));

        searchSourceBuilder.highlighter(highlightBuilder);

        //过滤条件
        searchSourceBuilder.fetchSource(new String[]{"name", "studymodel"}, new String[]{});

        //设置查询条件
        xc_course.source(searchSourceBuilder);
        //执行查询
        SearchResponse search = highLevelClient.search(xc_course);
        //获取查询结果
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            String index = hit.getIndex();
            String type = hit.getType();
            String id = hit.getId();
            float score = hit.getScore();
            String sourceAsString = hit.getSourceAsString();
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String name = (String) sourceAsMap.get("name");


            //取出高亮字段内容
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null) {
                HighlightField nameField = highlightFields.get("name");
                if (nameField != null) {
                    Text[] fragments = nameField.getFragments();
                    StringBuffer stringBuffer = new StringBuffer();
                    for (Text str : fragments) {
                        stringBuffer.append(str.string());
                    }
                    name = stringBuffer.toString();
                }
            }
            String studymodel = (String) sourceAsMap.get("studymodel");
            String description = (String) sourceAsMap.get("description");
            System.out.println(index);
            System.out.println(type);
            System.out.println(id);
            System.out.println(score);
            System.out.println(sourceAsString);
            System.out.println(name);
            System.out.println(studymodel);
            System.out.println(description);
            System.out.println("-----------------------");
        }

    }
}
