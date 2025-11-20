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
        if (pdfFile != null && !pdfFile.isEmpty()) {
            String savedPath = saveProductPdf(pdfFile); // 아래 만든 메서드 호출
            dto.setDpstInfoPdf(savedPath); // DTO에 경로 세팅 ("/uploads/products/...")
        }


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

        // yml에서 경로 가져오기 (/app/uploads/pdf_products)
        String basePath = filePathConfig.getPdfProductsPath();

        if (basePath == null || basePath.isBlank()) {
            return null;
        }

        // 원본 파일명 정제 (특수문자 제거 등)
        String original = file.getOriginalFilename();
        String safeName = StringUtils.cleanPath(original).replaceAll("[^a-zA-Z0-9._-]", "_");

        // 유니크 파일명 생성 (UUID)
        String storedName = UUID.randomUUID() + "_" + safeName;

        // 저장할 파일 객체 생성
        File dest = new File(basePath, storedName);

        // 절대 경로로 변환 (도커/리눅스 환경 호환성 확보)
        if (!dest.isAbsolute()) {
            dest = dest.getAbsoluteFile();
        }

        // 폴더 없으면 생성
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }

        // 파일 저장
        file.transferTo(dest);

        // DB에 저장할 웹 접근 경로 리턴
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