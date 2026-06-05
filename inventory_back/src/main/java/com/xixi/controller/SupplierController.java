package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.mapper.SupplierMapper;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
@Tag(name = "供应商管理", description = "供应商管理接口")
public class SupplierController {
    private final SupplierService supplierService;
    @Operation(summary = "分页查询供应商", operationId = "getSupplierPage")
    @GetMapping("/getSupplierPage")
    public Result getSupplierPage(SupplierPageQuery supplierPageQuery){
        IPage<SupplierVO> iPage=supplierService.getSupplierPage(supplierPageQuery);
        return  Result.success(iPage);
    }
    @Operation(summary = "新增供应商", operationId = "addSupplier")
    @PostMapping("/addSupplier")
    public Result addSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.addSupplier(supplierDTO);
    }
    @Operation(summary = "更新供应商", operationId = "updateSupplier")
    @PutMapping("/updateSupplier")
    public Result updateSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.updateSupplier(supplierDTO);
    }
    @Operation(summary = "批量删除供应商", operationId = "deleteSupplier")
    @DeleteMapping("/deleteSupplier/{ids}")
    public Result deleteSupplier(@PathVariable List<Integer> ids){
        return supplierService.deleteSupplier(ids);
    }
}

