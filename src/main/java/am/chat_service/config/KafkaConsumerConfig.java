package am.chat_service.config;

import am.chat_service.messaging.event.ChatDocumentMessageEvent;
import am.chat_service.messaging.event.ChatStatusUpdateEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    private <T> Map<String, Object> baseConfig(Class<T> targetType) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        config.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, targetType.getName());
        config.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "am.chat_service.messaging.event");
        config.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return config;
    }

    @Bean
    public CommonErrorHandler chatConsumerErrorHandler() {
        DefaultErrorHandler handler = new DefaultErrorHandler(
                (record, exception) -> LOG.error(
                        "Dropping unprocessable record from topic {} partition {} offset {} after retries",
                        record.topic(), record.partition(), record.offset(), exception),
                new FixedBackOff(2000L, 3L)
        );
        handler.setAckAfterHandle(true);
        return handler;
    }

    @Bean
    public ConsumerFactory<String, ChatStatusUpdateEvent> chatStatusConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConfig(ChatStatusUpdateEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatStatusUpdateEvent> chatStatusKafkaListenerContainerFactory(
            ConsumerFactory<String, ChatStatusUpdateEvent> chatStatusConsumerFactory,
            CommonErrorHandler chatConsumerErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, ChatStatusUpdateEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatStatusConsumerFactory);
        factory.setCommonErrorHandler(chatConsumerErrorHandler);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ChatDocumentMessageEvent> chatDocumentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(baseConfig(ChatDocumentMessageEvent.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatDocumentMessageEvent> chatDocumentKafkaListenerContainerFactory(
            ConsumerFactory<String, ChatDocumentMessageEvent> chatDocumentConsumerFactory,
            CommonErrorHandler chatConsumerErrorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, ChatDocumentMessageEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatDocumentConsumerFactory);
        factory.setCommonErrorHandler(chatConsumerErrorHandler);
        return factory;
    }
}
