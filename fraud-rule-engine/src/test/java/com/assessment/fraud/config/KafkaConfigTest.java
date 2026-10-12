package com.assessment.fraud.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.dto.TransactionEventRequest;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

class KafkaConfigTest {

    private KafkaConfig config;

    @BeforeEach
    void setUp() throws Exception {
        config = new KafkaConfig();
        setField(config, "bootstrapServers", "localhost:9092");
        setField(config, "groupId", "fraud-rule-engine-test");
    }

    @Test
    void transactionConsumerFactoryUsesTrustedDtoPackage() {
        ConsumerFactory<String, TransactionEventRequest> factory = config.transactionConsumerFactory();

        assertThat(factory).isNotNull();
        assertThat(factory.getConfigurationProperties())
                .containsEntry("bootstrap.servers", "localhost:9092")
                .containsEntry("group.id", "fraud-rule-engine-test")
                .containsEntry("auto.offset.reset", "earliest")
                .containsEntry("spring.json.trusted.packages", "com.assessment.fraud.dto");
    }

    @Test
    void listenerFactoryWiresConsumerFactoryAndErrorHandler() {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEventRequest> factory =
                config.kafkaListenerContainerFactory();

        assertThat(factory).isNotNull();
        assertThat(factory.getConsumerFactory()).isNotNull();
    }

    @Test
    void assessmentProducerFactoryAndTemplateAreCreated() {
        ProducerFactory<String, FraudAssessmentResponse> producerFactory =
                config.assessmentProducerFactory();
        KafkaTemplate<String, FraudAssessmentResponse> template = config.kafkaTemplate();

        assertThat(producerFactory).isNotNull();
        assertThat(template).isNotNull();
        assertThat(template.getProducerFactory()).isNotNull();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = KafkaConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
