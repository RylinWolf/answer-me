package com.wolfhouse.answerme.ai;

import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Rylin Wolf
 */
@Configuration
@ConfigurationProperties(prefix = "custom.zhi-pu")
@Data
public class AiConfig {
    private String apiKey;

    @Bean
    public ClientV4 client() {
        return new ClientV4.Builder(apiKey).build();
    }
}
