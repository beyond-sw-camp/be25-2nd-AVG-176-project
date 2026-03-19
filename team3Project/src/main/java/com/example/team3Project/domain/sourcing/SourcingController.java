package com.example.team3Project.domain.sourcing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/sourcing")
@RequiredArgsConstructor
public class SourcingController {

    private final SourcingService sourcingService;
    private final NormalizationService normalizationService;


    @GetMapping("/")
    public String mainHome() {
        return "Hello World!";
    }
    
    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public Map<String, Object> handleFileUpload(@RequestBody SourcingDTO sourcingDTO) 
    {
        Map<String,Object> response = new HashMap<>();
        
        List<String> errors = sourcingService.validateSourcingData(sourcingDTO);
        
        // service에서 에러가 뭐라도 하나 있더라면, 그거 확인 시키기
        if (!errors.isEmpty()) {
            response.put("status", "error");
            response.put("message", "데이터 검증 실패");
            response.put("errors", errors);
        }else {
            response.put("status", "success");
            response.put("receivedData", sourcingDTO);
            
            sourcingService.saveSourcingData(sourcingDTO);
        }
        
        return response;

    }

    @PostMapping("/normalization/{id}")
    public String normalization(@PathVariable Long id) {
        // 한번에 작동 시키기
        // 일단 해놔야 하는것 fastAPI 적용하여 나노 바나나 적용시키기.
        normalizationService.normalize(id);
        return "정규화 완료 상품 아이디: " + id;

    }
    

}
