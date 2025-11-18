package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.ChatbotHistDTO;
import kr.co.api.flobankapi.mapper.ChatbotHistMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotHistService {
    private final ChatbotHistMapper chatbotHistMapper;

    public void insertHist(ChatbotHistDTO histDTO){
        chatbotHistMapper.insertHist(histDTO);
    }

    public List<ChatbotHistDTO> selectHist(String sessId){
        return chatbotHistMapper.selectHist(sessId);
    }

}
