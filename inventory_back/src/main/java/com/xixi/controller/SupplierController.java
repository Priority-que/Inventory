package com.xixi.controller;

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
public class SupplierController {
    private final SupplierService supplierService;
    @GetMapping("/getSupplierPage")
    public Result getSupplierPage(SupplierPageQuery supplierPageQuery){
        IPage<SupplierVO> iPage=supplierService.getSupplierPage(supplierPageQuery);
        return  Result.success(iPage);
    }
    @PostMapping("/addSupplier")
    public Result addSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.addSupplier(supplierDTO);
    }
    @PutMapping("/updateSupplier")
    public Result updateSupplier(@RequestBody SupplierDTO supplierDTO){
        return supplierService.updateSupplier(supplierDTO);
    }
    @DeleteMapping("/deleteSupplier/{ids}")
    public Result deleteSupplier(@PathVariable List<Integer> ids){
        return supplierService.deleteSupplier(ids);
    }
}
