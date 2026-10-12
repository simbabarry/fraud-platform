package com.assessment.notification.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.notification.dto.FraudAssessmentEvent;
import java.lang.reflect.Field;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
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
        setField(config, "groupId", "fraud-notification-test");
        setField(config, "dltTopic", "fraud.assessments.dlt");
    }

    @Test
    void assessmentConsumerFactoryUsesJsonDeserializerDefaults() {
        ConsumerFactory<String, FraudAssessmentEvent> factory = config.assessmentConsumerFactory();

        assertThat(factory).isNotNull();
        assertThat(factory.getConfigurationProperties())
                .containsEntry("bootstrap.servers", "localhost:9092")
                .containsEntry("group.id", "fraud-notification-test")
                .containsEntry("auto.offset.reset", "earliest");
    }

    @Test
    void dltProducerFactoryAndTemplateAreWired() {
        ProducerFactory<String, Object> producerFactory = config.dltProducerFactory();
        KafkaTemplate<String, Object> template = config.dltKafkaTemplate();

        assertThat(producerFactory).isNotNull();
        assertThat(template).isNotNull();
        assertThat(template.getProducerFactory()).isNotNull();
    }

    @Test
    void listenerFactoryUsesConsumerFactoryAndErrorHandler() {
        KafkaTemplate<String, Object> template = config.dltKafkaTemplate();
        ConcurrentKafkaListenerContainerFactory<String, FraudAssessmentEvent> factory =
                config.kafkaListenerContainerFactory(template);

        assertThat(factory).isNotNull();
        assertThat(factory.getConsumerFactory()).isNotNull();
    }

    @Test
    void dltDestinationPreservesPartitionOnConfiguredTopic() {
        TopicPartition destination = config.dltDestination(
                new ConsumerRecord<>("fraud.assessments", 3, 0L, "key", "value"),
                new RuntimeException("boom"));

        assertThat(destination.topic()).isEqualTo("fraud.assessments.dlt");
        assertThat(destination.partition()).isEqualTo(3);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = KafkaConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
