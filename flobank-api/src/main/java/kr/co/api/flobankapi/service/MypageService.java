package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.mapper.MypageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MypageService {
    private final MypageMapper mypageMapper;

    public void saveAcct(){
        mypageMapper.insertAcct();
    }
}
