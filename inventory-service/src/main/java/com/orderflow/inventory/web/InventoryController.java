package com.orderflow.inventory.web;

import com.orderflow.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    public ProductResponse getProduct(@PathVariable("productId") Long productId) {
        return ProductResponse.from(inventoryService.getProduct(productId));
    }

    @PatchMapping("/{productId}/decrement")
    public ResponseEntity<ProductResponse> decrementStock(
            @PathVariable("productId") Long productId,
            @Valid @RequestBody DecrementStockRequest request
    ) {
        return ResponseEntity.ok(ProductResponse.from(inventoryService.decrementStock(productId, request.quantity())));
    }
}
