package com.example.team3Project.domain.dummyMarket.api;

import com.example.team3Project.domain.dummyMarket.application.ProductPageService;
import com.example.team3Project.domain.dummyMarket.dto.ProductListPageDto;
import com.example.team3Project.domain.dummyMarket.dto.ProductPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductPageController {

    private final ProductPageService productPageService;

    // 전체 상품 목록 페이지는 판매자 구분 없이 모든 상품을 보여준다.
    @GetMapping({"", "/"})
    public String productDefault(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            Model model
    ) {
        try {
            ProductListPageDto dto = productPageService.getProductListPage(page, size);
            model.addAttribute("page", dto);
            return "product-list";
        } catch (RuntimeException e) {
            model.addAttribute("message", e.getMessage());
            return "product-empty";
        }
    }

    // 상품 상세 페이지의 사이드바는 해당 상품과 같은 판매자의 상품만 보여준다.
    @GetMapping("/{id}")
    public String productDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int sidebarPage,
            @RequestParam(defaultValue = "5") int sidebarSize,
            Model model
    ) {
        try {
            ProductPageDto dto = productPageService.getProductPage(id, sidebarPage, sidebarSize);
            model.addAttribute("product", dto);
            return "product";
        } catch (RuntimeException e) {
            model.addAttribute("message", e.getMessage());
            return "product-empty";
        }
    }
}
