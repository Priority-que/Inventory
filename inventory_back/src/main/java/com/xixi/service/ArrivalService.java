package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.arrival.ArrivalDTO;
import com.xixi.pojo.query.arrival.ArrivalQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.arrival.ArrivalVO;

public interface ArrivalService {
    IPage<ArrivalVO> getArrivalPage(ArrivalQuery arrivalQuery);
    ArrivalVO getArrivalById(Long arrivalId);
    Result addArrival(ArrivalDTO arrivalDTO);
}
