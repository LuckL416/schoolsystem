package com.school.dormrepair.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceDefault implements SmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceDefault.class);

    @Override
    public void send(String phone, String content) {
        log.info("[SMS] To: {}, Content: {}", phone, content);
    }
}
