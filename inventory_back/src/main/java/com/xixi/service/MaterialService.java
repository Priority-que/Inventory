package com.xixi.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.material.MaterialDTO;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.vo.material.MaterialPageVO;
import com.xixi.pojo.vo.Result;

import java.util.List;

public interface MaterialService {
    IPage<MaterialPageVO> getMaterialPage(MaterialPageQuery materialPageQuery);

    Result addMaterial(MaterialDTO materialDTO);

    Result updateMaterial(MaterialDTO materialDTO);

    Result deleteMaterialByIds(List<Integer> ids);
}
