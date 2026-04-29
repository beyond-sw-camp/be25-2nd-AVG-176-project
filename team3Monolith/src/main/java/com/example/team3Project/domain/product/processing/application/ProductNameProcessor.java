package com.example.team3Project.domain.product.processing.application;

import com.example.team3Project.domain.policy.dto.BlockedWordResponse;
import com.example.team3Project.domain.policy.dto.PolicyBundle;
import com.example.team3Project.domain.policy.dto.ReplacementWordResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ProductNameProcessor {

    public boolean containsBlockedWord(String productName, PolicyBundle policyBundle) {
        for (BlockedWordResponse blockedWord : policyBundle.getBlockedWords()) {
            if (productName.contains(blockedWord.getBlockedWord())) {
                return true;
            }
        }
        return false;
    }

    public String applyReplacementWords(String productName, PolicyBundle policyBundle) {
        String processedName = productName;
        for (ReplacementWordResponse replacementWord : policyBundle.getReplacementWords()) {
            processedName = processedName.replace(
                    replacementWord.getSourceWord(),
                    replacementWord.getReplacementWord()
            );
        }
        return processedName;
    }

    public Optional<String> processProductName(PolicyBundle policyBundle, String productName) {
        if (containsBlockedWord(productName, policyBundle)) {
            return Optional.empty();
        }
        return Optional.of(applyReplacementWords(productName, policyBundle));
    }
}
