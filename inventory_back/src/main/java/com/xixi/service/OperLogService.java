package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.query.log.OperLogPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.log.OperLogPageVO;

public interface OperLogService {
    IPage<OperLogPageVO>getOperLogPage(OperLogPageQuery operLogPageQuery);

    Result getOperLogById(Long id);
}
