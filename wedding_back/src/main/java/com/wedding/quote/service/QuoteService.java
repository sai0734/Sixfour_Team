package com.wedding.quote.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wedding.quote.dto.QuoteCompareResultDTO;
import com.wedding.quote.dto.QuoteDTO;

public interface QuoteService {

    QuoteDTO uploadAndExtract(String memberEmail, MultipartFile file);

    List<QuoteDTO> listByMember(String memberEmail);

    QuoteCompareResultDTO compare(String memberEmail, List<Long> quoteIds);

    void remove(String memberEmail, Long quoteId);

    // 소유권 확인 후 이미지 파일명만 돌려줌 (QuoteController가 CustomFileUtil.getFile()에
    // 넘길 용도 - QuoteDTO엔 파일명을 안 실어서 프론트로 새 나가지 않게 함)
    String getOwnedImageFileName(String memberEmail, Long quoteId);
}
