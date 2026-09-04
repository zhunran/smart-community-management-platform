package com.property.module.payment.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱配置
 *
 * 敏感信息（商户私钥）通过环境变量注入，不存储在源码或配置文件中。
 */
@Slf4j
@Getter
@Configuration
@ConditionalOnProperty(name = "alipay.app-id")
public class AlipayConfig {

    @Value("${alipay.app-id}")
    private String appId;

    @Value("${alipay.gateway}")
    private String gateway;

    @Value("${alipay.merchant-private-key}")
    private String merchantPrivateKey;

    @Value("${alipay.alipay-public-key}")
    private String alipayPublicKey;

    @Value("${alipay.notify-url}")
    private String notifyUrl;

    @Value("${alipay.return-url}")
    private String returnUrl;

    @Value("${alipay.return-web-url:http://localhost:5273/payment/return}")
    private String returnWebUrl;

    @PostConstruct
    public void init() {
        log.info("支付宝沙箱配置已加载 [appId={}, gateway={}]", appId, gateway);
    }

    /**
     * 创建支付宝客户端 Bean
     * 设置连接超时 5s，读超时 10s（缩短默认超时，避免对账任务被阻塞）
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gateway,
                appId,
                merchantPrivateKey,
                "JSON",
                "UTF-8",
                alipayPublicKey,
                "RSA2",
                 "5000",      // connectTimeout（毫秒）
                 "10000"      // readTimeout（毫秒）
        );
    }
}
