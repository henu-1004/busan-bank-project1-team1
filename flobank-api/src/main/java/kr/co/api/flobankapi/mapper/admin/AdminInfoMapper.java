package kr.co.api.flobankapi.mapper.admin;

import kr.co.api.flobankapi.dto.AdminInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminInfoMapper {
    @Select("""
            SELECT
                ADMIN_ID   AS adminId,
                ADMIN_PW   AS adminPw,
                ADMIN_TYPE AS adminType,
                ADMIN_PH AS adminPh
            FROM TB_ADMIN_INFO
            WHERE ADMIN_ID = #{adminId}
            """)
    AdminInfoDTO findById(String adminId);
}
