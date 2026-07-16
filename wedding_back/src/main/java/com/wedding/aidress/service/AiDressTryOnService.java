package com.wedding.aidress.service;

import com.wedding.aidress.DTO.AiDressTryOnRequestDTO;
import com.wedding.aidress.DTO.AiDressTryOnResponseDTO;
import com.wedding.company.domain.DressTryOnItemDTO;
import com.wedding.global.dto.PageRequestDTO;
import com.wedding.global.dto.PageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface AiDressTryOnService {

  PageResponseDTO<DressTryOnItemDTO> getDressList(PageRequestDTO pageRequestDTO);

  String saveMyPhoto(String memberEmail, MultipartFile file);

  String getMyPhoto(String memberEmail);

  AiDressTryOnResponseDTO tryOn(String memberEmail, AiDressTryOnRequestDTO requestDTO);
}
