package kr.co.api.flobankapi.service;

import kr.co.api.flobankapi.dto.ChatbotBadTypeDTO;
import kr.co.api.flobankapi.dto.ChatbotBadWordDTO;
import kr.co.api.flobankapi.dto.ChatbotRulesDTO;
import kr.co.api.flobankapi.mapper.ChatbotRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotRuleService {

    private final ChatbotRuleMapper chatbotRuleMapper;

    public List<ChatbotBadTypeDTO> selectBadTypeList() {
        return chatbotRuleMapper.selectBadTypeList();
    }


    public List<ChatbotBadWordDTO> selectBadWordList() {
        return chatbotRuleMapper.selectBadWordList();
    }


    public List<ChatbotRulesDTO> selectRulesList() {
        return chatbotRuleMapper.selectRulesList();
    }

    public String checkForbiddenWord(String q) {
        List<ChatbotBadWordDTO> badWords = chatbotRuleMapper.getActiveWords(); // BAD_USE_YN='Y'
        List<ChatbotBadTypeDTO> badTypes = chatbotRuleMapper.selectBadTypeList(); // type→answer 맵핑

        for (ChatbotBadWordDTO w : badWords) {
            if (w.getBadType() == 1) {
                // 🔹 욕설/비속어 : 단순 포함 체크
                if (q.contains(w.getBadWord())) {
                    return badTypes.get(0).getBtAnswer();
                }
            } else if (w.getBadType() == 2) {
                // 🔹 개인정보 : 정규식 패턴 매칭
                if (Pattern.compile(w.getBadWord()).matcher(q).find()) {

                    return badTypes.get(1).getBtAnswer();
                }
            }
        }
        return null; // 필터 대상 아님
    }

    public String checkAllForbiddenWord(String q) {
        List<ChatbotBadWordDTO> badWords = chatbotRuleMapper.selectBadWordList(); // BAD_USE_YN='Y'
        List<ChatbotBadTypeDTO> badTypes = chatbotRuleMapper.selectBadTypeList(); // type→answer 맵핑


        for (ChatbotBadWordDTO w : badWords) {
            if (w.getBadType() == 1) {
                // 🔹 욕설/비속어 : 단순 포함 체크
                if (q.contains(w.getBadWord())) {
                    return badTypes.get(0).getBtAnswer();
                }
            } else if (w.getBadType() == 2) {
                // 🔹 개인정보 : 정규식 패턴 매칭
                if (Pattern.compile(w.getBadWord()).matcher(q).find()) {

                    return badTypes.get(1).getBtAnswer();
                }
            }
        }
        return null; // 필터 대상 아님
    }

    public void insertBadWords(ChatbotBadWordDTO badWordDTO) {
        chatbotRuleMapper.insertBadWords(badWordDTO);
    }
}
