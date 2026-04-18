package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Arrival;
import com.xixi.pojo.query.arrival.ArrivalQuery;
import com.xixi.pojo.vo.arrival.ArrivalVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArrivalMapper extends BaseMapper<Arrival> {
    IPage<ArrivalVO> getArrivalPage(IPage<ArrivalVO> iPage,@Param("q") ArrivalQuery arrivalQuery);

    ArrivalVO getArrivalById(Long arrivalId);

    Long lockById(Long arrivalId);
}
