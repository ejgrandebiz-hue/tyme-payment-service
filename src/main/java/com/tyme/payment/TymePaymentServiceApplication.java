package com.tyme.payment;

import com.tyme.payment.config.IdempotencyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(IdempotencyProperties.class)
public class TymePaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TymePaymentServiceApplication.class, args);
    }

}
