package com.hulkhiretech.payments.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonUtil {
    private final ObjectMapper objectMapper;

    public <T> T convertJsonToObject(String jsonString, Class<T> valueType) {
        try {
            return objectMapper.readValue(jsonString, valueType);
        } catch (Exception e) {
            log.error("Error converting Json to Object: {}", e.getMessage());

            return null;
        }
    }

    // Method to convert Object to JSON String
    public String convertObjectToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error converting Object to Json: {}", e.getMessage());
            return null;
        }
    }

}