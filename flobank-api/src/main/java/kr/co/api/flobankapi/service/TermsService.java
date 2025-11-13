package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.TermsDTO;
import kr.co.api.flobankapi.mapper.TermsInfoMapper;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TermsService {

    private final TermsInfoMapper TIM;

    @Value("${terms.base-dir}")
    private String termsBaseDir;

    public TermsService(TermsInfoMapper tim) {
        TIM = tim;
    }

    public List<TermsDTO> getTermsByType(int termType){
        List<TermsDTO> list = TIM.findByType(termType);

        for(TermsDTO t : list){
            String fileName = t.getTermFile();

            if (fileName == null || fileName.isBlank()){
                t.setTermContent("저장안됨");
                continue;
            }
            Path path = Paths.get(termsBaseDir, fileName);
            try {
                String content = Files.readString(path, StandardCharsets.UTF_8);
                t.setTermContent(content);
            } catch (Exception e){
                t.setTermContent("err");
        }
    }
        return list;
    }
}
