package com.example.team3Project.domain.sourcing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingController {

    private final SourcingService sourcingService;
    

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
                                    @RequestPart("data") SourcingDTO sourcingDTO) 
    {
        Map<String,Object> response = new HashMap<>();

        // 파일이 비어있다면 에러 메세지 띄우기.

        if (file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "파일을 선택해 주세요.");
            return response;
        }
        
        List<String> errors = sourcingService.validateSourcingData(sourcingDTO);
        
        // service에서 에러가 뭐라도 하나 있더라면, 그거 확인 시키기
        if (!errors.isEmpty()) {
            response.put("status", "error");
            response.put("message", "데이터 검증 실패");
            response.put("errors", errors);
        }else {
            response.put("status", "success");
            response.put("receivedData", sourcingDTO);
        }
        
        sourcingService.saveSourcingData(sourcingDTO);

        return response;

    }
    

}
