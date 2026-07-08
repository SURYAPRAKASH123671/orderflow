package com.orderflow.inventory;

import com.orderflow.inventory.domain.Product;
import com.orderflow.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableCaching
@EnableDiscoveryClient
@SpringBootApplication
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDemoProducts(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            productRepository.saveAll(List.of(
                    new Product("Mechanical Keyboard", 25, BigDecimal.valueOf(2499)),
                    new Product("Wireless Mouse", 40, BigDecimal.valueOf(999)),
                    new Product("USB-C Hub", 18, BigDecimal.valueOf(1799))
            ));
        };
    }
}
