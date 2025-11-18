package kr.co.api.flobankapi.controller.translate;

import kr.co.api.flobankapi.dto.translate.TranslationRequestDTO;
import kr.co.api.flobankapi.dto.translate.TranslationResponseDTO;

import kr.co.api.flobankapi.service.translate.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/translate")
public class TranslationController {

    private final TranslationService translationService;

    @PostMapping
    public TranslationResponseDTO translate(@RequestBody TranslationRequestDTO request) {

        String result = translationService.translate(
                request.getText(),
                request.getTargetLang()
        );

        TranslationResponseDTO response = new TranslationResponseDTO();
        response.setTranslatedText(result);

        return response;
    }


}
