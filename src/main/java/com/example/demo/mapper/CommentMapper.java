package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Comment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {
    Page<Comment> findPage(Page<Comment> page);

    // 自定义批量插入方法
    int batchInsert(@Param("list") List<Comment> comments);
}