package com.ngmj.towechat.config;

import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignDecider {
    @Bean
    public Decoder feignDecoder() {
        return new JacksonDecoder();
    }
    @Bean
    public Encoder feignEncoder() {
        return new JacksonEncoder();
    }
}
