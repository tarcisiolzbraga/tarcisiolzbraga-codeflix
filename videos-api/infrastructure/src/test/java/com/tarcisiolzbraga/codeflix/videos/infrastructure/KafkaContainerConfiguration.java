package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

// A mesma imagem do compose do admin-codeflix, que é quem hospeda o broker: o listener é exercitado
// contra o Kafka de verdade, com mensagem publicada num tópico de verdade.
//
// Estático e fora do ciclo de vida do Spring, pelo mesmo motivo do Elasticsearch: um broker por
// contexto da suíte não se paga.
@TestConfiguration(proxyBeanMethods = false)
public class KafkaContainerConfiguration {

    private static final DockerImageName IMAGE = DockerImageName.parse("apache/kafka:4.3.1");

    private static final KafkaContainer CONTAINER = startedContainer();

    // **Os quatro**, com os nomes exatos das chaves do application.yml. A lista tinha só "category",
    // e por isso o sorteio de grupo abaixo nunca valeu para os outros três: eles seguiam no grupo da
    // aplicação, que é justamente o cenário que o comentário seguinte descreve como quebrado.
    //
    // Custou duas PRs reprovadas pelo CastMemberE2ETest, com "esperava 1, veio 0" — a mensagem foi
    // entregue ao listener de um contexto antigo, cujo mock não tinha resposta para aquele id, e o
    // dado nunca chegou ao catálogo. Consumidor novo entra nesta lista.
    private static final List<String> CONSUMERS = List.of("category", "cast-member", "genre", "video");

    // Um grupo por contexto, e é o que mantém os testes de CDC determinísticos. Com o group-id da
    // aplicação, os contextos que o Spring guarda em cache depois de cada classe continuam vivos e
    // entram no MESMO grupo: a partição do tópico fica com um só deles, então a mensagem publicada
    // por este teste pode ser entregue ao listener de um contexto antigo, cujo cliente REST simulado
    // não tem resposta preparada para aquele id — ele registra "nada a replicar", confirma o offset,
    // e o dado nunca chega ao catálogo.
    //
    // Os listeners seguem compartilhando o grupo entre si, como em produção: o que o teste isola é a
    // disputa entre contextos, que é artefato da suíte. O auto-offset-reset continua earliest,
    // também como em produção, e é ele que impede a mensagem de se perder quando a partição é
    // atribuída depois de o teste já ter publicado.

    // Duas tentativas em teste, contra as sete de produção, e isto não é economia de tempo: é o que
    // tira a suíte de um lugar em que ela falhava de vez em quando sem ninguém ter mudado nada.
    //
    // O @RetryableTopic cria um tópico por degrau mais a fila morta, então sete tentativas dão oito
    // consumidores por listener — com quatro listeners, **trinta e dois por contexto**. Essa
    // quantidade de consumidor entrando e saindo de grupo faz o broker passar o tempo rebalanceando,
    // e a entrega para além dos 40s que o teste espera: foi assim que o teste de busca por nome
    // derrubou duas PRs, com "esperava 1, veio 0", enquanto o teste ao lado, que lê os mesmos
    // registros, passava em menos de um segundo.
    //
    // Duas tentativas dão três consumidores por listener, doze no total. Nenhuma cobertura se perde:
    // teste nenhum afirma sobre retentativa ou fila morta — o que a escada protege é o admin fora do
    // ar em produção, não o que a suíte exercita.
    private static final int TEST_MAX_ATTEMPTS = 2;

    @Bean
    DynamicPropertyRegistrar kafkaProperties() {
        final var group = "test-" + UUID.randomUUID();
        return registry -> {
            registry.add("spring.kafka.bootstrap-servers", CONTAINER::getBootstrapServers);
            CONSUMERS.forEach(consumer -> {
                registry.add("kafka.consumers.%s.group-id".formatted(consumer), () -> group);
                registry.add("kafka.consumers.%s.max-attempts".formatted(consumer), () -> TEST_MAX_ATTEMPTS);
            });
        };
    }

    // O KafkaTemplate que o Boot declara vem com curingas no tipo, então não casa com uma injeção
    // de KafkaTemplate<String, String>. Este é só para o teste publicar a mensagem de CDC, que é
    // texto dos dois lados, como o consumidor espera.
    @Bean
    KafkaTemplate<String, String> testKafkaTemplate() {
        final var settings = Map.<String, Object>of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, CONTAINER.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(settings));
    }

    private static KafkaContainer startedContainer() {
        final var container = new KafkaContainer(IMAGE);
        container.start();
        return container;
    }
}
