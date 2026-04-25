package com.xixi.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xixi.annotation.OperLogRecord;
import com.xixi.entity.Material;
import com.xixi.mapper.MaterialMapper;
import com.xixi.pojo.dto.material.MaterialDTO;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.material.MaterialPageVO;
import com.xixi.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {
    private final MaterialMapper materialMapper;

    @Override
    public IPage<MaterialPageVO> getMaterialPage(MaterialPageQuery materialPageQuery) {
        Integer pageNum = materialPageQuery.getPageNum();
        Integer pageSize = materialPageQuery.getPageSize();
        IPage<MaterialPageVO> materialPageVoIPage = new Page<>(pageNum, pageSize);
        return materialMapper.getMaterialPage(materialPageVoIPage, materialPageQuery);
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "物料管理",
            operationType = "CREATE",
            operationDesc = "新增物料",
            bizType = "MATERIAL"
    )
    public Result addMaterial(MaterialDTO materialDTO) {
        Material material = BeanUtil.copyProperties(materialDTO, Material.class);
        if (materialMapper.insert(material) > 0) {
            materialDTO.setId(material.getId());
            return Result.success("添加物料成功！");
        }
        return Result.error("添加物料失败！");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "物料管理",
            operationType = "UPDATE",
            operationDesc = "修改物料",
            bizType = "MATERIAL"
    )
    public Result updateMaterial(MaterialDTO materialDTO) {
        Material material = BeanUtil.copyProperties(materialDTO, Material.class);
        if (materialMapper.updateById(material) > 0) {
            return Result.success("修改物料信息成功！");
        }
        return Result.error("修改物料信息失败！");
    }

    @Override
    @OperLogRecord(
            logType = "BUSINESS",
            moduleName = "物料管理",
            operationType = "DELETE",
            operationDesc = "删除物料",
            bizType = "MATERIAL"
    )
    public Result deleteMaterialByIds(List<Integer> ids) {
        if (ids != null && !ids.isEmpty()) {
            materialMapper.deleteBatchIds(ids);
            return Result.success("删除物料信息成功！");
        }
        return Result.error("删除物料信息失败！");
    }
}
