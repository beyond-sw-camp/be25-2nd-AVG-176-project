package com.example.team3Project;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UploadController {

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload"; // upload.html (또는 템플릿)을 반환합니다.
    }


}
