package com.example.team3Project.domain.sourcing;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sourcing")
public class SourcingController {

    @GetMapping("/")
    public String mainHome() {
        return "Hello World!";
    }
    
    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public Map<String, Object> handleFileUpload(@RequestPart("file") 
                                    MultipartFile file,
                                    @RequestPart("data") Map<String, Object> json
                                    ) {
        
        Map<String,Object> response = new HashMap<>();
        Map<String,Object> dataPart = (Map<String,Object>) json.get("data");

        // 파일이 비어있다면 json에 따로 넣기.
        
        if (file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "파일을 선택해 주세요.");
            return response;
        }
        
        System.out.println(dataPart);


        if (dataPart != null) {
            String title = (String) dataPart.get("title");
            String original_price = String.valueOf(dataPart.get("original_price"));

            System.out.println("상품명:  " + title);
            System.out.println("가격:  " + original_price);
        }
        
        return response;

    }
    

}
