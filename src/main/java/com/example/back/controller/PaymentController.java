package com.example.back.controller;

import com.example.back.entity.Squad;
import com.example.back.service.SquadService;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    @Value("${cashfree.appId}")
    private String appId;

    @Value("${cashfree.secretKey}")
    private String secretKey;

    private final SquadService squadService;

    @PostMapping(value = "/create-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) throws Exception {
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);

        // 1. PRE-REGISTER Squad to ensure user isn't charged for a duplicate & hold their spot
        try {
            squadService.preRegisterSquad(data, orderId);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Collections.singletonMap("message", e.getMessage()));
        }
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-version", "2023-08-01");
        headers.set("x-client-id", appId);
        headers.set("x-client-secret", secretKey);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("order_id", orderId);
        requestBody.put("order_amount", 100.0);
        requestBody.put("order_currency", "INR");
        
        JSONObject customerDetails = new JSONObject();
        customerDetails.put("customer_id", "cust_" + UUID.randomUUID().toString().substring(0, 8));
        customerDetails.put("customer_phone", data.getOrDefault("phone", "9999999999").toString());
        requestBody.put("customer_details", customerDetails);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity("https://api.cashfree.com/pg/orders", entity, String.class);
        
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> data) {
        String orderId = (String) data.get("orderId");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-version", "2023-08-01");
        headers.set("x-client-id", appId);
        headers.set("x-client-secret", secretKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange("https://api.cashfree.com/pg/orders/" + orderId + "/payments", HttpMethod.GET, entity, String.class);
            JSONArray paymentsArray = new JSONArray(response.getBody());
            
            boolean isSuccess = false;
            for (int i = 0; i < paymentsArray.length(); i++) {
                JSONObject payment = paymentsArray.getJSONObject(i);
                if ("SUCCESS".equals(payment.optString("payment_status"))) {
                    isSuccess = true;
                    break;
                }
            }

            if (isSuccess) {
                try {
                    Squad savedSquad = squadService.finalizeRegistration(orderId);
                    return ResponseEntity.ok(savedSquad);
                } catch (RuntimeException e) {
                    return ResponseEntity.status(400).body(Collections.singletonMap("message", "Registration finalization failed: " + e.getMessage()));
                }
            } else {
                return ResponseEntity.status(400).body(Collections.singletonMap("message", "Payment is still pending or failed. Please contact support if deducted."));
            }
        } catch (Exception e) {
            // Do NOT show failure if verification API fails temporarily
            return ResponseEntity.ok(Collections.singletonMap("pendingMessage", "Verification is running in background. You will receive a confirmation soon."));
        }
    }

    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<?> cashfreeWebhook(@RequestBody Map<String, Object> payload) {
        try {
            if (payload.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                if (data.containsKey("order")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> order = (Map<String, Object>) data.get("order");
                    String orderId = (String) order.get("order_id");
                    
                    if (data.containsKey("payment")) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> payment = (Map<String, Object>) data.get("payment");
                        if ("SUCCESS".equals(payment.get("payment_status"))) {
                            squadService.finalizeRegistration(orderId);
                        }
                    }
                }
            }
            return ResponseEntity.ok("Webhook received");
        } catch (Exception e) {
             return ResponseEntity.status(500).body("Error processing webhook");
        }
    }
}
