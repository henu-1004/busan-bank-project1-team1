package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.BriefingDTO;
import kr.co.api.flobankapi.mapper.BriefingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BriefingService {
    private final BriefingMapper briefingMapper;
    public BriefingDTO getLatestBrifing(String mode){
        if(mode == null || mode.isBlank()){
            mode = "oneday";
        }
        return  briefingMapper.selectLatestBriefingByMode(mode);
    }
}
