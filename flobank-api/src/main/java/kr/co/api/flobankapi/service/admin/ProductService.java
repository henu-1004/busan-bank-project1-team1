package kr.co.api.flobankapi.service.admin;

import kr.co.api.flobankapi.config.FilePathConfig;
import kr.co.api.flobankapi.dto.*;
import kr.co.api.flobankapi.mapper.admin.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;

@RequiredArgsConstructor
@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final FilePathConfig filePathConfig;

    @Transactional
    public void insertProduct(ProductDTO dto,
                              List<ProductLimitDTO> limits,
                              List<ProductPeriodDTO> periods,
                              ProductWithdrawRuleDTO wdrwInfo,
                              List<ProductWithdrawAmtDTO> withdrawAmts,
                              MultipartFile pdfFile) throws Exception {
        dto.setDpstInfoPdf(null);

        if (pdfFile != null && !pdfFile.isEmpty()) {
            // 여기서 경로를 생성해서 다시 집어넣음
            String savedPath = saveProductPdf(pdfFile);
            dto.setDpstInfoPdf(savedPath);
        }

        // 2. DB 저장 (이제 null 아니면 경로만 들어감)
        productMapper.insertProduct(dto);


            // 1) 기본 상품 INSERT
            productMapper.insertProduct(dto);
            String dpstId = dto.getDpstId();


            // 2) 최소/최대 금액 정리
            if (limits != null && !limits.isEmpty()) {
                List<ProductLimitDTO> clean = new ArrayList<>();
                Set<String> seen = new HashSet<>();

                for (ProductLimitDTO l : limits) {
                    if (l.getLmtCurrency() == null || l.getLmtCurrency().trim().isEmpty()) continue;
                    if (seen.add(l.getLmtCurrency())) clean.add(l);
                }

                limits = clean;
            }


            // 3) 최소/최대 금액 INSERT
            if (dto.getDpstType() == 1 && limits != null && !limits.isEmpty()) {

                Map<String, Object> map = new HashMap<>();
                map.put("dpstId", dpstId);
                map.put("limits", limits);

                productMapper.insertProductLimits(map);
            }


            // 4) 가입기간 INSERT
            if (periods != null && !periods.isEmpty()) {

                Map<String, Object> map2 = new HashMap<>();
                map2.put("dpstId", dpstId);
                map2.put("list", periods);

                productMapper.insertProductPeriods(map2);
            }


            // 5) 분할 인출 규정 INSERT
            if (wdrwInfo != null) {
                wdrwInfo.setDpstId(dpstId);
                productMapper.insertWithdrawalRule(wdrwInfo);
            }


            // 6) 통화별 최소 출금금액 INSERT
            if (withdrawAmts != null && !withdrawAmts.isEmpty()) {

                Map<String, Object> map3 = new HashMap<>();
                map3.put("dpstId", dpstId);
                map3.put("list", withdrawAmts);

                productMapper.insertWithdrawalAmounts(map3);
            }
        }

    private String saveProductPdf(MultipartFile file) throws Exception {

        // 1. yml에 적힌 경로 그대로 가져옴 ("/app/uploads/products" 등)
        String basePath = filePathConfig.getPdfProductsPath();

        // 경로 설정이 비어있으면 에러 방지용 처리
        if (basePath == null || basePath.isBlank()) {
            throw new Exception("파일 업로드 경로(pdf-products-path)가 설정되지 않았습니다.");
        }

        // 2. 원본 파일명 정제
        String original = file.getOriginalFilename();
        String safeName = StringUtils.cleanPath(original);

        // 3. 파일명 중복 방지 (UUID)
        String storedName = UUID.randomUUID() + "_" + safeName;

        // 4. 저장할 폴더 객체 생성 (경로는 basePath 그대로 사용)
        File folder = new File(basePath);


        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                // 로컬 윈도우의 권한 문제나 경로 문제일 수 있음
            }
        }

        // 5. 파일 객체 생성
        File dest = new File(folder, storedName);


        if (!dest.isAbsolute()) {
            dest = dest.getAbsoluteFile();
        }


        file.transferTo(dest);


        return "/uploads/products/" + storedName;
    }


    public List<ProductDTO> getProductsByStatus(int status) {
        return productMapper.getProductsByStatus(status);
    }


    public void updateStatus(String dpstId, int status) {
        productMapper.updateStatus(dpstId, status);
    }


    public ProductDTO getProductById(String dpstId) {
        return productMapper.getProductById(dpstId);
    }



    public List<ProductPeriodDTO> getPeriods(String dpstId) {
        return productMapper.getPeriods(dpstId);
    }


    public ProductWithdrawRuleDTO getWithdrawRule(String dpstId) {
        return productMapper.getWithdrawRule(dpstId);
    }


    public List<ProductWithdrawAmtDTO> getWithdrawAmts(String dpstId) {
        return productMapper.getWithdrawAmts(dpstId);
    }


    public List<ProductLimitDTO> getLimits(String dpstId) {
        return productMapper.getLimits(dpstId);
    }


    // 상품 자동 업데이트
    public void updateOpenedProducts() {
        productMapper.updateStatusToOpened();
    }





}