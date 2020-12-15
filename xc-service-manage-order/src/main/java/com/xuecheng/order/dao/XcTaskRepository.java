package com.xuecheng.order.dao;

import com.xuecheng.framework.domain.task.XcTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;

public interface XcTaskRepository extends JpaRepository<XcTask, String> {

    //查询改时间之前的数据记录
    Page<XcTask> findByUpdateTimeBefore(Pageable pageable, Date updateTime);

    @Modifying
    @Query("update XcTask t set t.updateTime = :updateTime where t.id = :taskId")
    public int updateTaskTime(@Param("updateTime") Date updateTime, @Param("taskId") String taskId);

    @Modifying
    @Query("update XcTask t set t.version = :version +1 where t.id = :taskId and t.version = :version")
    public int updateTaskVersion(String taskId, int version);
}
