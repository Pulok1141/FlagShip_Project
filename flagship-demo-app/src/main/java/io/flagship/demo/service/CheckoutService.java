package io.flagship.demo.service;

import io.flagship.starter.annotation.FeatureFlag;
import io.flagship.starter.annotation.UserKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CheckoutService {

    private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);

    public record CheckoutResponse(
            String flowType,
            String userId,
            double amount,
            String status,
            String message
    ) {}

    /**
     * Guarded by FlagShip feature flag 'new-checkout-flow'.
     * If the flag is enabled for this user, executes modern 1-click checkout.
     * If disabled or kill-switch is active, transparently executes legacyCheckout fallback.
     */
    @FeatureFlag(key = "new-checkout-flow", fallbackMethod = "legacyCheckout")
    public CheckoutResponse processCheckout(@UserKey String userId, double amount) {
        log.info("Processing modern 1-click checkout flow for user: {}, amount: ${}", userId, amount);
        return new CheckoutResponse(
                "MODERN_1_CLICK_CHECKOUT",
                userId,
                amount,
                "SUCCESS",
                "Order processed instantaneously via Next-Gen 1-Click Checkout!"
        );
    }

    public CheckoutResponse legacyCheckout(String userId, double amount) {
        log.info("Processing legacy 3-step checkout flow for user: {}, amount: ${}", userId, amount);
        return new CheckoutResponse(
                "LEGACY_3_STEP_CHECKOUT",
                userId,
                amount,
                "SUCCESS",
                "Order processed through standard multi-step legacy checkout."
        );
    }
}
