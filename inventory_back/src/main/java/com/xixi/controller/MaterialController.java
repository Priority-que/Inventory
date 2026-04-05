package com.xixi.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Material;
import com.xixi.pojo.dto.material.MaterialDTO;
import com.xixi.pojo.query.material.MaterialPageQuery;
import com.xixi.pojo.vo.Material.MaterialPageVo;
import com.xixi.pojo.vo.Result;
import com.xixi.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
public class MaterialController {
    private final MaterialService materialService;
    @GetMapping("/getMaterialPage")
    public Result getMaterialPage(MaterialPageQuery materialPageQuery){
        IPage<MaterialPageVo> materialPage = materialService.getMaterialPage(materialPageQuery);
        return Result.success(materialPage);
    }
    @PostMapping("/addMaterial")
    public Result addMaterial(@RequestBody MaterialDTO materialDTO){
        return materialService.addMaterial(materialDTO);
    }
    @PutMapping("/updateMaterial")
    public Result updateMaterial(@RequestBody MaterialDTO materialDTO){
        return materialService.updateMaterial(materialDTO);
    }
    @DeleteMapping("/deleteMaterialByIds/{ids}")
    public Result deleteMaterialByIds(@PathVariable List<Integer> ids){
        return materialService.deleteMaterialByIds(ids);
    }
}
