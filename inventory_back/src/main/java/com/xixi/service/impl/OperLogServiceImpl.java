package com.xixi.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.entity.OperLog;
import com.xixi.mapper.OperLogMapper;
import com.xixi.pojo.query.log.OperLogPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.log.OperLogPageVO;
import com.xixi.service.OperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class OperLogServiceImpl implements OperLogService {
    private final OperLogMapper operLogMapper;

    @Override
    public IPage<OperLogPageVO>getOperLogPage(OperLogPageQuery operLogPageQuery){
        Integer pageNum=operLogPageQuery.getPageNum();
        Integer pageSize=operLogPageQuery.getPageSize();
        IPage<OperLogPageVO> operLogPageVOIPage=new Page<>(pageNum,pageSize);
        IPage<OperLogPageVO>page=operLogMapper.getOperLogPage(operLogPageVOIPage,operLogPageQuery);
        return page;
    }

    public Result getOperLogById(Long id){
        if(id==null){
            return Result.error("日志id不能为空！");
        }
        OperLog operLog=operLogMapper.selectById(id);
        if(operLog==null){
            return Result.error("操作日志不存在！");
        }
        return Result.success(operLog);
    }
}
