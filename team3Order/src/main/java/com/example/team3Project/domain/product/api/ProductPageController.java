package com.example.team3Project.domain.product.api;

import com.example.team3Project.domain.product.application.ProductPageService;
import com.example.team3Project.domain.product.dto.ProductPageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductPageController {

    private final ProductPageService productPageService;

    @GetMapping({"", "/"})
    public String productDefault(Model model) {
        try {
            Long firstId = productPageService.getFirstProductId();
            return "redirect:/product/" + firstId;
        } catch (RuntimeException e) {
            model.addAttribute("message", e.getMessage());
            return "product-empty";
        }
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        try {
            ProductPageDto dto = productPageService.getProductPage(id);
            model.addAttribute("product", dto);
            return "product";
        } catch (RuntimeException e) {
            model.addAttribute("message", e.getMessage());
            return "product-empty";
        }
    }
}
