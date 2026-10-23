package com.ruralhealthcare.medicine.service;

import com.ruralhealthcare.medicine.dto.OcrExtractionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface OcrService {

    OcrExtractionResponse extractText(MultipartFile image);
}
