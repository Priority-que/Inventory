package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xixi.entity.ArrivalItem;
import com.xixi.pojo.vo.arrival.ArrivalItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ArrivalItemMapper extends BaseMapper<ArrivalItem> {
    List<ArrivalItemVO> getArrivalItemByArrivalId(@Param("arrivalId") Long arrivalId);
}
