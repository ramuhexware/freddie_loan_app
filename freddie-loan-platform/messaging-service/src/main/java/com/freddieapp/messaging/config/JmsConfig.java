package com.freddieapp.messaging.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableJms
public class JmsConfig {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user:admin}")
    private String username;

    @Value("${spring.activemq.password:admin}")
    private String password;

    @Bean
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(username, password, brokerUrl);

        RedeliveryPolicy policy = new RedeliveryPolicy();
        policy.setMaximumRedeliveries(3);
        policy.setInitialRedeliveryDelay(1000L);
        policy.setRedeliveryDelay(2000L);
        policy.setUseExponentialBackOff(true);
        policy.setBackOffMultiplier(2.0);
        policy.setMaximumRedeliveryDelay(30000L);
        factory.setRedeliveryPolicy(policy);

        return factory;
    }

    @Bean
    public MappingJackson2MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate() {
        JmsTemplate template = new JmsTemplate(connectionFactory());
        template.setMessageConverter(messageConverter());
        template.setSessionTransacted(true);
        template.setDeliveryPersistent(true);
        template.setTimeToLive(86400000L); // 24 hours
        return template;
    }

    @Bean
    public PlatformTransactionManager jmsTransactionManager() {
        return new org.springframework.jms.connection.JmsTransactionManager(connectionFactory());
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            PlatformTransactionManager transactionManager) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setMessageConverter(messageConverter());
        factory.setTransactionManager(transactionManager);
        factory.setSessionTransacted(true);
        factory.setConcurrency("3-5");
        factory.setErrorHandler(t -> log.error("JMS listener error: {}", t.getMessage(), t));
        return factory;
    }
}
