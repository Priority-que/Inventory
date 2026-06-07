package com.xixi.mapper;

import com.xixi.pojo.vo.statistics.StatisticsSummaryVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StatisticsMapper {

    StatisticsSummaryVO getSummary();
}
