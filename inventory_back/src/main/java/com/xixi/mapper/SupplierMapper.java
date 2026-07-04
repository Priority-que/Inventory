package com.xixi.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.xixi.entity.Supplier;
import com.xixi.pojo.query.supplier.SupplierPageQuery;
import com.xixi.pojo.vo.supplier.SupplierVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier>{

    public IPage<SupplierVO> getSupplierPage(IPage<SupplierVO> supplierPageVOIPage, @Param("q") SupplierPageQuery supplierPageQuery);
    @Select("select * from supplier where id =#{id} and deleted = 0")
    Supplier getSupplierById(Long id);
    @Select("select * from supplier where user_id = #{userId} and deleted = 0 limit 1")
    Supplier getSupplierByUserId(Long userId);
    Integer updateFileRound(@Param("id") Long id, @Param("fileRound") Integer fileRound);

    @Update("update supplier set status = 'PENDING', submit_time = #{submitTime}, " +
            "review_time = null, review_user_id = null, review_note = null " +
            "where id = #{id} and deleted = 0")
    int submitReview(@Param("id") Long id, @Param("submitTime") LocalDateTime submitTime);

    @Update("update supplier set status = #{status}, review_time = #{reviewTime}, " +
            "review_user_id = #{reviewUserId}, review_note = #{reviewNote} " +
            "where id = #{id} and deleted = 0")
    int updateReviewStatus(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("reviewTime") LocalDateTime reviewTime,
                           @Param("reviewUserId") Long reviewUserId,
                           @Param("reviewNote") String reviewNote);
}
