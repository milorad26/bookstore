package com.bookstore.service;

import com.bookstore.dto.BulkDiscountCalculation;
import com.bookstore.dto.BulkDiscountRuleDTO;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.model.BulkDiscountRule;
import com.bookstore.model.Order;
import com.bookstore.model.OrderItem;
import com.bookstore.repository.BulkDiscountRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkDiscountService {

    private final BulkDiscountRuleRepository bulkDiscountRuleRepository;
    private final MessageSource messageSource;

    /**
     * Get all bulk discount rules
     */
    public List<BulkDiscountRuleDTO> getAllRules() {
        return bulkDiscountRuleRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get active bulk discount rules
     */
    public List<BulkDiscountRuleDTO> getActiveRules() {
        return bulkDiscountRuleRepository.findByActiveOrderByPriorityDescMinQuantityAsc(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get a bulk discount rule by ID
     */
    public BulkDiscountRuleDTO getRuleById(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        BulkDiscountRule rule = bulkDiscountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("bulk.discount.notfound", new Object[]{id}, locale)));
        return convertToDTO(rule);
    }

    /**
     * Create a new bulk discount rule
     */
    @Transactional
    public BulkDiscountRuleDTO createRule(BulkDiscountRuleDTO ruleDTO) {
        BulkDiscountRule rule = convertToEntity(ruleDTO);
        BulkDiscountRule savedRule = bulkDiscountRuleRepository.save(rule);
        log.info("Created bulk discount rule: {} ({}% off for {} or more items)", 
            savedRule.getName(), savedRule.getDiscountPercentage(), savedRule.getMinQuantity());
        return convertToDTO(savedRule);
    }

    /**
     * Update an existing bulk discount rule
     */
    @Transactional
    public BulkDiscountRuleDTO updateRule(Long id, BulkDiscountRuleDTO ruleDTO) {
        Locale locale = LocaleContextHolder.getLocale();
        BulkDiscountRule existingRule = bulkDiscountRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                    messageSource.getMessage("bulk.discount.notfound", new Object[]{id}, locale)));

        existingRule.setName(ruleDTO.getName());
        existingRule.setDescription(ruleDTO.getDescription());
        existingRule.setMinQuantity(ruleDTO.getMinQuantity());
        existingRule.setMaxQuantity(ruleDTO.getMaxQuantity());
        existingRule.setDiscountPercentage(ruleDTO.getDiscountPercentage());
        existingRule.setAppliesToCategory(ruleDTO.getAppliesToCategory());
        existingRule.setActive(ruleDTO.getActive());
        existingRule.setPriority(ruleDTO.getPriority());

        BulkDiscountRule updatedRule = bulkDiscountRuleRepository.save(existingRule);
        log.info("Updated bulk discount rule: {}", updatedRule.getName());
        return convertToDTO(updatedRule);
    }

    /**
     * Delete a bulk discount rule
     */
    @Transactional
    public void deleteRule(Long id) {
        Locale locale = LocaleContextHolder.getLocale();
        if (!bulkDiscountRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                messageSource.getMessage("bulk.discount.notfound", new Object[]{id}, locale));
        }
        bulkDiscountRuleRepository.deleteById(id);
        log.info("Deleted bulk discount rule with ID: {}", id);
    }

    /**
     * Calculate bulk discount for an order based on total quantity
     */
    public BulkDiscountCalculation calculateBulkDiscount(Order order) {
        // Calculate total quantity of all items in the order
        int totalQuantity = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return calculateBulkDiscountByQuantity(totalQuantity, order);
    }

    /**
     * Calculate bulk discount for a given quantity and order subtotal
     */
    public BulkDiscountCalculation calculateBulkDiscountByQuantity(int totalQuantity, Order order) {
        BulkDiscountCalculation calculation = new BulkDiscountCalculation();
        calculation.setTotalQuantity(totalQuantity);
        calculation.setDiscountAmount(BigDecimal.ZERO);
        calculation.setDiscountPercentage(BigDecimal.ZERO);

        // Find applicable rules
        List<BulkDiscountRule> applicableRules = bulkDiscountRuleRepository.findApplicableRules(totalQuantity);

        if (applicableRules.isEmpty()) {
            // No applicable rule, check for next tier
            List<BulkDiscountRule> allActiveRules = bulkDiscountRuleRepository.findByActiveOrderByPriorityDescMinQuantityAsc(true);
            BulkDiscountRule nextTier = findNextTier(totalQuantity, allActiveRules);
            
            if (nextTier != null) {
                calculation.setQuantityToNextTier(nextTier.getMinQuantity() - totalQuantity);
                calculation.setNextTierPercentage(nextTier.getDiscountPercentage());
                calculation.setMessage("Add " + calculation.getQuantityToNextTier() + 
                    " more book(s) for " + nextTier.getDiscountPercentage() + "% off!");
            }
            
            return calculation;
        }

        // Apply the first (highest priority/percentage) applicable rule
        BulkDiscountRule applicableRule = applicableRules.get(0);
        
        // Calculate discount amount based on order subtotal (before delivery fee and other discounts)
        BigDecimal subtotal = order.getOrderItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discountAmount = subtotal
                .multiply(applicableRule.getDiscountPercentage())
                .divide(new BigDecimal("100"), RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        calculation.setDiscountAmount(discountAmount);
        calculation.setDiscountPercentage(applicableRule.getDiscountPercentage());
        calculation.setRuleName(applicableRule.getName());
        
        // Check for next tier
        BulkDiscountRule nextTier = findNextTier(totalQuantity, 
            bulkDiscountRuleRepository.findByActiveOrderByPriorityDescMinQuantityAsc(true));
        
        if (nextTier != null && nextTier.getDiscountPercentage().compareTo(applicableRule.getDiscountPercentage()) > 0) {
            calculation.setQuantityToNextTier(nextTier.getMinQuantity() - totalQuantity);
            calculation.setNextTierPercentage(nextTier.getDiscountPercentage());
            calculation.setMessage("Add " + calculation.getQuantityToNextTier() + 
                " more book(s) for " + nextTier.getDiscountPercentage() + "% off!");
        } else {
            calculation.setMessage(applicableRule.getDiscountPercentage() + "% bulk discount applied!");
        }

        log.info("Calculated bulk discount for {} items: {} ({}%)", 
            totalQuantity, discountAmount, applicableRule.getDiscountPercentage());

        return calculation;
    }

    /**
     * Find the next tier discount rule
     */
    private BulkDiscountRule findNextTier(int currentQuantity, List<BulkDiscountRule> rules) {
        return rules.stream()
                .filter(rule -> rule.getMinQuantity() > currentQuantity)
                .findFirst()
                .orElse(null);
    }

    /**
     * Apply bulk discount to order
     */
    @Transactional
    public void applyBulkDiscountToOrder(Order order) {
        BulkDiscountCalculation calculation = calculateBulkDiscount(order);
        
        if (calculation.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Set bulk discount amount
            order.setBulkDiscountAmount(calculation.getDiscountAmount());
            
            // Update total amount by subtracting bulk discount
            BigDecimal newTotal = order.getTotalAmount().subtract(calculation.getDiscountAmount());
            order.setTotalAmount(newTotal);
            
            log.info("Applied bulk discount of {} to order {}", 
                calculation.getDiscountAmount(), order.getId());
        }
    }

    /**
     * Convert entity to DTO
     */
    private BulkDiscountRuleDTO convertToDTO(BulkDiscountRule rule) {
        BulkDiscountRuleDTO dto = new BulkDiscountRuleDTO();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setMinQuantity(rule.getMinQuantity());
        dto.setMaxQuantity(rule.getMaxQuantity());
        dto.setDiscountPercentage(rule.getDiscountPercentage());
        dto.setAppliesToCategory(rule.getAppliesToCategory());
        dto.setActive(rule.getActive());
        dto.setPriority(rule.getPriority());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    /**
     * Convert DTO to entity
     */
    private BulkDiscountRule convertToEntity(BulkDiscountRuleDTO dto) {
        BulkDiscountRule rule = new BulkDiscountRule();
        rule.setName(dto.getName());
        rule.setDescription(dto.getDescription());
        rule.setMinQuantity(dto.getMinQuantity());
        rule.setMaxQuantity(dto.getMaxQuantity());
        rule.setDiscountPercentage(dto.getDiscountPercentage());
        rule.setAppliesToCategory(dto.getAppliesToCategory());
        rule.setActive(dto.getActive() != null ? dto.getActive() : true);
        rule.setPriority(dto.getPriority() != null ? dto.getPriority() : 0);
        return rule;
    }
}
