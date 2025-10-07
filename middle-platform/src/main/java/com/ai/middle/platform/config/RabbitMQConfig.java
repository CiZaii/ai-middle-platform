package com.ai.middle.platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_OCR = "kb.ocr.queue";
    public static final String QUEUE_VECTORIZATION = "kb.vectorization.queue";
    public static final String QUEUE_QA_GENERATION = "kb.qa.queue";
    public static final String QUEUE_KG_GENERATION = "kb.kg.queue";
    public static final String QUEUE_DLX = "kb.dlx.queue";

    public static final String EXCHANGE_OCR = "kb.ocr.exchange";
    public static final String EXCHANGE_VECTORIZATION = "kb.vectorization.exchange";
    public static final String EXCHANGE_QA = "kb.qa.exchange";
    public static final String EXCHANGE_KG = "kb.kg.exchange";
    public static final String EXCHANGE_DLX = "kb.dlx.exchange";

    public static final String ROUTING_KEY_OCR = "kb.ocr";
    public static final String ROUTING_KEY_VECTORIZATION = "kb.vectorization";
    public static final String ROUTING_KEY_QA = "kb.qa";
    public static final String ROUTING_KEY_KG = "kb.kg";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(QUEUE_OCR)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "ocr.failed")
                .withArgument("x-message-ttl", 600_000)
                .build();
    }

    @Bean
    public DirectExchange ocrExchange() {
        return new DirectExchange(EXCHANGE_OCR);
    }

    @Bean
    public Binding ocrBinding() {
        return BindingBuilder.bind(ocrQueue()).to(ocrExchange()).with(ROUTING_KEY_OCR);
    }

    @Bean
    public Queue vectorizationQueue() {
        return QueueBuilder.durable(QUEUE_VECTORIZATION)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "vectorization.failed")
                .withArgument("x-message-ttl", 600_000)
                .build();
    }

    @Bean
    public DirectExchange vectorizationExchange() {
        return new DirectExchange(EXCHANGE_VECTORIZATION);
    }

    @Bean
    public Binding vectorizationBinding() {
        return BindingBuilder.bind(vectorizationQueue()).to(vectorizationExchange())
                .with(ROUTING_KEY_VECTORIZATION);
    }

    @Bean
    public Queue qaQueue() {
        return QueueBuilder.durable(QUEUE_QA_GENERATION)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "qa.failed")
                .withArgument("x-message-ttl", 600_000)
                .build();
    }

    @Bean
    public DirectExchange qaExchange() {
        return new DirectExchange(EXCHANGE_QA);
    }

    @Bean
    public Binding qaBinding() {
        return BindingBuilder.bind(qaQueue()).to(qaExchange()).with(ROUTING_KEY_QA);
    }

    @Bean
    public Queue kgQueue() {
        return QueueBuilder.durable(QUEUE_KG_GENERATION)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLX)
                .withArgument("x-dead-letter-routing-key", "kg.failed")
                .withArgument("x-message-ttl", 600_000)
                .build();
    }

    @Bean
    public DirectExchange kgExchange() {
        return new DirectExchange(EXCHANGE_KG);
    }

    @Bean
    public Binding kgBinding() {
        return BindingBuilder.bind(kgQueue()).to(kgExchange()).with(ROUTING_KEY_KG);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(QUEUE_DLX).build();
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    @Bean
    public Binding ocrDlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("ocr.failed");
    }

    @Bean
    public Binding vectorizationDlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("vectorization.failed");
    }

    @Bean
    public Binding qaDlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("qa.failed");
    }

    @Bean
    public Binding kgDlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("kg.failed");
    }
}
