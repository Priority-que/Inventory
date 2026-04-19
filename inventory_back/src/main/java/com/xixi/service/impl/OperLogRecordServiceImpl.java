package com.xixi.service.impl;

import com.xixi.entity.OperLog;
import com.xixi.mapper.OperLogMapper;
import com.xixi.service.OperLogRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OperLogRecordServiceImpl implements OperLogRecordService {

    private final OperLogMapper operLogMapper;
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOperLog(OperLog operLog){
        operLogMapper.insert(operLog);
    }
}
