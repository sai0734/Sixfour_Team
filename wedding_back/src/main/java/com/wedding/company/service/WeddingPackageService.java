package com.wedding.company.service;

import com.wedding.company.dto.WeddingPackageDTO;
import com.wedding.company.dto.WeddingPackageSearchDTO;
import com.wedding.global.dto.PageResponseDTO;

public interface WeddingPackageService {

    PageResponseDTO<com.wedding.company.dto.WeddingPackageListDTO> getList(WeddingPackageSearchDTO searchDTO);
    WeddingPackageDTO get(Long weddingPackageId);
    Long register(WeddingPackageDTO dto);
    void modify(WeddingPackageDTO dto);
    void remove(Long weddingPackageId);
}
