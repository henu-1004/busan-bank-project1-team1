package kr.co.api.flobankapi.mapper;

import kr.co.api.flobankapi.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface DepositMapper {

    List<ProductDTO> findActiveProducts();

    int countActiveProducts();

    ProductDTO findProductById(String dpstId);

    String findTermsFileByTitle(String productName);

    public ProductDTO selectDpstProduct(String dpstId);
    public List<ProductLimitDTO> limitOfDpst(String dpstId);
    public ProductPeriodDTO dpstMinMaxPeriod(String dpstId);
    public List<ProductPeriodDTO> dpstFixedPeriods(String dpstId);
    public ProductWithdrawRuleDTO getDpstWdrwInfo(String dpstId);
    public List<ProductWithdrawAmtDTO> getDpstAmtList(String dpstId);
    public List<CustAcctDTO> getKRWAccts(String acctCustCode);
    public CustFrgnAcctDTO getFrgnAcct(String frgnAcctCustCode);
    public List<FrgnAcctBalanceDTO> getFrgnAcctBalList(String balFrgnAcctNo);
    public List<CurrencyInfoDTO> getAllCurrencies();
    public CurrencyInfoDTO getCurrency(String curCode);

    List<DepositRateDTO> findRatesByBaseDate(@Param("baseDate") Date baseDate);

    public int getPrefRate(String currency);
    public String getAcctPw(String acctNo);
    public String getFrgnAcctPw(String frgnAcctNo);
    public InterestRateDTO getRecentInterest(String currency);
}
