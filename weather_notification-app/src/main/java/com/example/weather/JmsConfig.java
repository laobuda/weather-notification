package com.example.weather;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsMessagingTemplate;

import jakarta.jms.ConnectionFactory;

@Configuration
@EnableJms
public class JmsConfig {

    @Value("${spring.artemis.broker-url:tcp://localhost:61616}")
    private String brokerUrl;

    @Value("${spring.artemis.user:admin}")
    private String user;

    @Value("${spring.artemis.password:admin}")
    private String password;

    @Bean
    public ConnectionFactory connectionFactory() {
        return new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(brokerUrl, user, password);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setConcurrency("1-1");
        return factory;
    }

    @Bean
    public JmsMessagingTemplate jmsMessagingTemplate(ConnectionFactory connectionFactory) {
        return new JmsMessagingTemplate(connectionFactory);
    }
}
