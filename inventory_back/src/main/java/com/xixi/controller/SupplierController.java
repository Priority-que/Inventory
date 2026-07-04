package com.xixi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.pojo.dto.supplier.SupplierDTO;
import com.xixi.pojo.dto.supplier.SupplierReviewDTO;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.Result;
import com.xixi.pojo.vo.supplier.SupplierVO;
import com.xixi.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN','PURCHASER','SUPPLIER')")
    public Result getSupplierPage(SupplierPageQuery supplierPageQuery){
        IPage<SupplierVO> iPage=supplierService.getSupplierPage(supplierPageQuery);
        return  Result.success(iPage);
    }
    @Operation(summary = "新增供应商", operationId = "addSupplier")
    @PostMapping("/addSupplier")
    @PreAuthorize("hasRole('ADMIN')")
    public Result addSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.addSupplier(supplierDTO);
    }
    @Operation(summary = "更新供应商", operationId = "updateSupplier")
    @PutMapping("/updateSupplier")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER')")
    public Result updateSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.updateSupplier(supplierDTO);
    }
    @Operation(summary = "批量删除供应商", operationId = "deleteSupplier")
    @DeleteMapping("/deleteSupplier/{ids}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result deleteSupplier(@PathVariable List<Integer> ids){
        return supplierService.deleteSupplier(ids);
    }

    @Operation(summary = "提交供应商资质审核", operationId = "submitSupplierReview")
    @PutMapping("/submitReview")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPLIER')")
    public Result submitSupplierReview(@RequestBody SupplierReviewDTO supplierReviewDTO) {
        return supplierService.submitSupplierReview(supplierReviewDTO);
    }

    @Operation(summary = "审核通过供应商", operationId = "approveSupplier")
    @PutMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Result approveSupplier(@RequestBody SupplierReviewDTO supplierReviewDTO) {
        return supplierService.approveSupplier(supplierReviewDTO);
    }

    @Operation(summary = "驳回供应商", operationId = "rejectSupplier")
    @PutMapping("/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Result rejectSupplier(@RequestBody SupplierReviewDTO supplierReviewDTO) {
        return supplierService.rejectSupplier(supplierReviewDTO);
    }

    @Operation(summary = "停用供应商", operationId = "disableSupplier")
    @PutMapping("/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public Result disableSupplier(@RequestBody SupplierReviewDTO supplierReviewDTO) {
        return supplierService.disableSupplier(supplierReviewDTO);
    }
}

