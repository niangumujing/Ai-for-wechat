package com.ngmj.towechat.config;


import com.alibaba.dashscope.aigc.generation.Generation;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@AllArgsConstructor
public class AiConfig {
    @Bean
    public Generation callWithMessage() {
        return new Generation();
    }

}
