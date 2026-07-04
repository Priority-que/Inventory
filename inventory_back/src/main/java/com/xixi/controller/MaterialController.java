package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.material.MaterialDTO;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.vo.material.MaterialPageVO;
import com.xixi.pojo.vo.Result;
import com.xixi.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
@Tag(name = "物料管理", description = "物料管理接口")
public class MaterialController {
    private final MaterialService materialService;
    @Operation(summary = "分页查询物料", operationId = "getMaterialPage")
    @GetMapping("/getMaterialPage")
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','WAREHOUSE')")
    public Result getMaterialPage(MaterialPageQuery materialPageQuery){
        IPage<MaterialPageVO> materialPage = materialService.getMaterialPage(materialPageQuery);
        return Result.success(materialPage);
    }
    @Operation(summary = "新增物料", operationId = "addMaterial")
    @PostMapping("/addMaterial")
    @PreAuthorize("hasRole('ADMIN')")
    public Result addMaterial(@RequestBody MaterialDTO materialDTO){
        return materialService.addMaterial(materialDTO);
    }
    @Operation(summary = "更新物料", operationId = "updateMaterial")
    @PutMapping("/updateMaterial")
    @PreAuthorize("hasRole('ADMIN')")
    public Result updateMaterial(@RequestBody MaterialDTO materialDTO){
        return materialService.updateMaterial(materialDTO);
    }
    @Operation(summary = "批量删除物料", operationId = "deleteMaterialByIds")
    @DeleteMapping("/deleteMaterialByIds/{ids}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result deleteMaterialByIds(@PathVariable List<Integer> ids){
        return materialService.deleteMaterialByIds(ids);
    }
}

