package com.example.back.controller;

import com.example.back.entity.Squad;
import com.example.back.service.SquadService;
import lombok.RequiredArgsConstructor;
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
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    @Value("${cashfree.app_id}")
    private String appId;

    @Value("${cashfree.secret_key}")
    private String secretKey;

    private final SquadService squadService;

    @PostMapping(value = "/create-order", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createOrder() throws Exception {
        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        
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
        customerDetails.put("customer_phone", "9999999999");
        requestBody.put("customer_details", customerDetails);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity("https://api.cashfree.com/pg/orders", entity, String.class);
        
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/verify")
    @Transactional
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> data) throws Exception {
        String orderId = (String) data.get("orderId");
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-version", "2023-08-01");
        headers.set("x-client-id", appId);
        headers.set("x-client-secret", secretKey);
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange("https://api.cashfree.com/pg/orders/" + orderId, HttpMethod.GET, entity, String.class);
        
        JSONObject jsonResponse = new JSONObject(response.getBody());
        String orderStatus = jsonResponse.optString("order_status");

        if ("PAID".equals(orderStatus)) {
            if (data.containsKey("formData") && data.get("formData") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> formData = (Map<String, Object>) data.get("formData");
                
                Squad squad = new Squad();
                squad.setSquadName((String) formData.get("squadName"));
                squad.setLeaderName((String) formData.get("leaderName"));
                squad.setPhone((String) formData.get("phone"));
                squad.setEmail((String) formData.get("email"));
                squad.setP1Name((String) formData.get("p1Name")); squad.setP1Uid((String) formData.get("p1Uid"));
                squad.setP2Name((String) formData.get("p2Name")); squad.setP2Uid((String) formData.get("p2Uid"));
                squad.setP3Name((String) formData.get("p3Name")); squad.setP3Uid((String) formData.get("p3Uid"));
                squad.setP4Name((String) formData.get("p4Name")); squad.setP4Uid((String) formData.get("p4Uid"));
                squad.setP5Name((String) formData.get("p5Name")); squad.setP5Uid((String) formData.get("p5Uid"));

                squad.setPaymentStatus("SUCCESS");
                
                Squad savedSquad = squadService.registerSquad(squad);
                return ResponseEntity.ok(savedSquad);
            } 
            
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.status(400).body("failure");
        }
    }
}
