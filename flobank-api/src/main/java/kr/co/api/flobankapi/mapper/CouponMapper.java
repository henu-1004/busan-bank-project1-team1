package kr.co.api.flobankapi.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponMapper {
    void updateCouponStatus(Long couponNo);
}
