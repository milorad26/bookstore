package com.bookstore.controller;

import com.bookstore.dto.BulkDiscountRuleDTO;
import com.bookstore.service.BulkDiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulk-discounts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BulkDiscountController {

    private final BulkDiscountService bulkDiscountService;

    /**
     * Get all bulk discount rules
     */
    @GetMapping
    public ResponseEntity<List<BulkDiscountRuleDTO>> getAllRules() {
        log.info("GET /api/bulk-discounts - Fetching all bulk discount rules");
        List<BulkDiscountRuleDTO> rules = bulkDiscountService.getAllRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get active bulk discount rules (public endpoint for customers)
     */
    @GetMapping("/active")
    public ResponseEntity<List<BulkDiscountRuleDTO>> getActiveRules() {
        log.info("GET /api/bulk-discounts/active - Fetching active bulk discount rules");
        List<BulkDiscountRuleDTO> rules = bulkDiscountService.getActiveRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a bulk discount rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<BulkDiscountRuleDTO> getRuleById(@PathVariable Long id) {
        log.info("GET /api/bulk-discounts/{} - Fetching bulk discount rule", id);
        BulkDiscountRuleDTO rule = bulkDiscountService.getRuleById(id);
        return ResponseEntity.ok(rule);
    }

    /**
     * Create a new bulk discount rule (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkDiscountRuleDTO> createRule(@Valid @RequestBody BulkDiscountRuleDTO ruleDTO) {
        log.info("POST /api/bulk-discounts - Creating new bulk discount rule: {}", ruleDTO.getName());
        BulkDiscountRuleDTO createdRule = bulkDiscountService.createRule(ruleDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRule);
    }

    /**
     * Update an existing bulk discount rule (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BulkDiscountRuleDTO> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody BulkDiscountRuleDTO ruleDTO) {
        log.info("PUT /api/bulk-discounts/{} - Updating bulk discount rule", id);
        BulkDiscountRuleDTO updatedRule = bulkDiscountService.updateRule(id, ruleDTO);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * Delete a bulk discount rule (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        log.info("DELETE /api/bulk-discounts/{} - Deleting bulk discount rule", id);
        bulkDiscountService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }
}
