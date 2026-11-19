package io.flagship.demo.controller;

import io.flagship.core.model.EvaluationContext;
import io.flagship.core.model.EvaluationResult;
import io.flagship.demo.service.CheckoutService;
import io.flagship.starter.client.FlagShipClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    private final CheckoutService checkoutService;
    private final FlagShipClient flagShipClient;

    public CheckoutController(CheckoutService checkoutService, FlagShipClient flagShipClient) {
        this.checkoutService = checkoutService;
        this.flagShipClient = flagShipClient;
    }

    @GetMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestParam(defaultValue = "user-12345") String user,
            @RequestParam(defaultValue = "49.99") double amount
    ) {
        // Execute feature-flagged service method
        CheckoutService.CheckoutResponse res = checkoutService.processCheckout(user, amount);

        // Track experiment conversion goal event
        flagShipClient.track("new-checkout-flow", user, "checkout_completed", amount);

        return ResponseEntity.ok(Map.of(
                "checkout", res,
                "experimentEventTracked", "checkout_completed"
        ));
    }

    @GetMapping("/banner")
    public ResponseEntity<?> banner(@RequestParam(defaultValue = "user-12345") String user) {
        EvaluationContext ctx = EvaluationContext.ofUser(user);
        String bannerText = flagShipClient.getString("promo-banner-text", ctx, "Welcome to our Store!");
        return ResponseEntity.ok(Map.of("user", user, "bannerText", bannerText));
    }

    @GetMapping("/discount")
    public ResponseEntity<?> discount(@RequestParam(defaultValue = "user-12345") String user) {
        EvaluationContext ctx = EvaluationContext.ofUser(user);
        double discount = flagShipClient.getNumber("discount-percent", ctx, 0.0);
        return ResponseEntity.ok(Map.of("user", user, "discountPercent", discount));
    }

    @GetMapping("/flag-status")
    public ResponseEntity<?> flagStatus(
            @RequestParam(defaultValue = "new-checkout-flow") String flagKey,
            @RequestParam(defaultValue = "user-12345") String user
    ) {
        EvaluationContext ctx = EvaluationContext.ofUser(user);
        EvaluationResult res = flagShipClient.evaluate(flagKey, ctx, false);
        return ResponseEntity.ok(res);
    }
}
