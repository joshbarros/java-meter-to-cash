package com.metertocash.tariff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metertocash.common.Channels;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class QueueConfig {

    @Bean
    public Queue readDlq() {
        return QueueBuilder.durable(Channels.READ_DLQ).build();
    }

    @Bean
    @DependsOn("readDlq")
    public Queue readQueue() {
        return QueueBuilder.durable(Channels.READ)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.READ_DLQ)
                .build();
    }

    @Bean
    public Queue actualDlq() {
        return QueueBuilder.durable(Channels.ACTUAL_DLQ).build();
    }

    @Bean
    @DependsOn("actualDlq")
    public Queue actualQueue() {
        return QueueBuilder.durable(Channels.ACTUAL)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.ACTUAL_DLQ)
                .build();
    }

    @Bean
    public Queue rateDlq() {
        return QueueBuilder.durable(Channels.RATE_DLQ).build();
    }

    @Bean
    @DependsOn("rateDlq")
    public Queue rateQueue() {
        return QueueBuilder.durable(Channels.RATE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.RATE_DLQ)
                .build();
    }

    @Bean
    public Queue adjustDlq() {
        return QueueBuilder.durable(Channels.ADJUST_DLQ).build();
    }

    @Bean
    @DependsOn("adjustDlq")
    public Queue adjustQueue() {
        return QueueBuilder.durable(Channels.ADJUST)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.ADJUST_DLQ)
                .build();
    }

    @Bean
    public Queue noticeDlq() {
        return QueueBuilder.durable(Channels.NOTICE_DLQ).build();
    }

    @Bean
    @DependsOn("noticeDlq")
    public Queue noticeQueue() {
        return QueueBuilder.durable(Channels.NOTICE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.NOTICE_DLQ)
                .build();
    }

    @Bean
    public Queue openBalanceDlq() {
        return QueueBuilder.durable(Channels.OPEN_BALANCE_DLQ).build();
    }

    @Bean
    @DependsOn("openBalanceDlq")
    public Queue openBalanceQueue() {
        return QueueBuilder.durable(Channels.OPEN_BALANCE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.OPEN_BALANCE_DLQ)
                .build();
    }

    @Bean
    public Queue applyDlq() {
        return QueueBuilder.durable(Channels.APPLY_DLQ).build();
    }

    @Bean
    @DependsOn("applyDlq")
    public Queue applyQueue() {
        return QueueBuilder.durable(Channels.APPLY)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.APPLY_DLQ)
                .build();
    }

    @Bean
    public Queue balanceCutDlq() {
        return QueueBuilder.durable(Channels.BALANCE_CUT_DLQ).build();
    }

    @Bean
    @DependsOn("balanceCutDlq")
    public Queue balanceCutQueue() {
        return QueueBuilder.durable(Channels.BALANCE_CUT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.BALANCE_CUT_DLQ)
                .build();
    }

    @Bean
    public Queue tenderDlq() {
        return QueueBuilder.durable(Channels.TENDER_DLQ).build();
    }

    @Bean
    @DependsOn("tenderDlq")
    public Queue tenderQueue() {
        return QueueBuilder.durable(Channels.TENDER)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.TENDER_DLQ)
                .build();
    }

    @Bean
    public Queue fieldCutDlq() {
        return QueueBuilder.durable(Channels.FIELD_CUT_DLQ).build();
    }

    @Bean
    @DependsOn("fieldCutDlq")
    public Queue fieldCutQueue() {
        return QueueBuilder.durable(Channels.FIELD_CUT)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.FIELD_CUT_DLQ)
                .build();
    }

    @Bean
    public Queue fieldRestoreDlq() {
        return QueueBuilder.durable(Channels.FIELD_RESTORE_DLQ).build();
    }

    @Bean
    @DependsOn("fieldRestoreDlq")
    public Queue fieldRestoreQueue() {
        return QueueBuilder.durable(Channels.FIELD_RESTORE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.FIELD_RESTORE_DLQ)
                .build();
    }

    @Bean
    public Queue premiseOffDlq() {
        return QueueBuilder.durable(Channels.PREMISE_OFF_DLQ).build();
    }

    @Bean
    @DependsOn("premiseOffDlq")
    public Queue premiseOffQueue() {
        return QueueBuilder.durable(Channels.PREMISE_OFF)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.PREMISE_OFF_DLQ)
                .build();
    }

    @Bean
    public Queue premiseOnDlq() {
        return QueueBuilder.durable(Channels.PREMISE_ON_DLQ).build();
    }

    @Bean
    @DependsOn("premiseOnDlq")
    public Queue premiseOnQueue() {
        return QueueBuilder.durable(Channels.PREMISE_ON)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.PREMISE_ON_DLQ)
                .build();
    }

    @Bean
    public Queue advanceDlq() {
        return QueueBuilder.durable(Channels.ADVANCE_DLQ).build();
    }

    @Bean
    @DependsOn("advanceDlq")
    public Queue advanceQueue() {
        return QueueBuilder.durable(Channels.ADVANCE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.ADVANCE_DLQ)
                .build();
    }

    @Bean
    public Queue rewindDlq() {
        return QueueBuilder.durable(Channels.REWIND_DLQ).build();
    }

    @Bean
    @DependsOn("rewindDlq")
    public Queue rewindQueue() {
        return QueueBuilder.durable(Channels.REWIND)
                .deadLetterExchange("")
                .deadLetterRoutingKey(Channels.REWIND_DLQ)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
