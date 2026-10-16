package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.configuration.AmqpProperties;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Esvazia as filas antes de cada teste, como a MySQLCleanUpExtension faz com as tabelas: o broker é
// um só para todas as classes, e mensagem deixada por um teste faria o seguinte ler o que não é
// seu. Extensão à parte porque fila não é banco, e o nome de cada uma diz o que ela limpa.
public class RabbitCleanUpExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(final ExtensionContext context) {
        final var applicationContext = SpringExtension.getApplicationContext(context);
        final var admin = applicationContext.getBean(RabbitAdmin.class);
        final var properties = applicationContext.getBean(AmqpProperties.class);
        purge(admin, List.of(
                properties.queues().videoCreated().queue(), properties.queues().videoEncoded().queue()));
    }

    private void purge(final RabbitAdmin admin, final List<String> queues) {
        queues.forEach(queue -> admin.purgeQueue(queue, true));
    }
}
