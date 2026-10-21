package com.example.admin.module.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.admin.module.notice.entity.SysNotice;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.module.notice.dto.MessageQuery;
import com.example.admin.module.notice.dto.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice> {
    IPage<MessageVO> selectMessagePage(Page<MessageVO> page,
                                       @Param("query") MessageQuery query,
                                       @Param("userId") Long userId,
                                       @Param("deptId") Long deptId);

    MessageVO selectAccessibleMessage(@Param("id") Long id,
                                      @Param("userId") Long userId,
                                      @Param("deptId") Long deptId);

    long countUnread(@Param("userId") Long userId, @Param("deptId") Long deptId);

    List<Long> selectUnreadIds(@Param("userId") Long userId, @Param("deptId") Long deptId);
}
