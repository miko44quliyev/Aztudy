package com.example.mscontent.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ContentUpdateRequest {

    @Size(min = 1, max = 255, message = "Başlıq 1-255 simvol arasında olmalıdır")
    private String title;

    @Size(max = 2000, message = "Təsvir 2000 simvoldan çox ola bilməz")
    private String description;

    private MultipartFile file;
}