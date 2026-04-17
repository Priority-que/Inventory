package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.OperLog;
import com.xixi.pojo.query.log.OperLogPageQuery;
import com.xixi.pojo.vo.log.OperLogPageVO;
import org.apache.ibatis.annotations.Param;

public interface OperLogMapper extends BaseMapper<OperLog> {
    IPage<OperLogPageVO> getOperLogPage(IPage<OperLogPageVO> operLogPageVOIPage, @Param("q")OperLogPageQuery operLogPageQuery);
}
