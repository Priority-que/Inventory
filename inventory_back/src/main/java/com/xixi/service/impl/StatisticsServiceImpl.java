package com.xixi.service.impl;

import com.xixi.mapper.StatisticsMapper;
import com.xixi.pojo.vo.statistics.StatisticsSummaryVO;
import com.xixi.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public StatisticsSummaryVO getSummary() {
        return statisticsMapper.getSummary();
    }
}
