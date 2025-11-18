package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.BriefingDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BriefingMapper {
    BriefingDTO selectLatestBriefingByMode(@Param("mode") String mode);
}
