package com.wedding.contract.service;

import org.springframework.web.multipart.MultipartFile;

import com.wedding.contract.dto.ContractReviewResultDTO;

public interface ContractReviewService {

    ContractReviewResultDTO review(MultipartFile file);

}