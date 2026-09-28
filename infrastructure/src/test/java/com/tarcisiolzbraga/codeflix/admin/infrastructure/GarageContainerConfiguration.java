package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

// A mesma imagem do docker compose, pelo mesmo motivo do MySQL: o teste fala com o armazenamento de
// verdade. Não há módulo pronto do Testcontainers para o Garage, então a configuração é manual;
// --single-node e --default-bucket deixam o container pronto sem nenhum comando depois da subida.
@TestConfiguration(proxyBeanMethods = false)
public class GarageContainerConfiguration {

    private static final DockerImageName GARAGE_IMAGE = DockerImageName.parse("dxflrs/garage:v2.3.0");
    private static final int S3_PORT = 3900;

    private static final String BUCKET = "codeflix-medias-test";
    private static final String ACCESS_KEY = "GK0000000000000000000test";
    private static final String SECRET_KEY = "0000000000000000000000000000000000000000000000000000000000000001";

    private static final String CONFIGURATION =
            """
            metadata_dir = "/var/lib/garage/meta"
            data_dir = "/var/lib/garage/data"
            db_engine = "lmdb"
            replication_factor = 1

            rpc_bind_addr = "[::]:3901"
            rpc_public_addr = "127.0.0.1:3901"

            [s3_api]
            s3_region = "garage"
            api_bind_addr = "[::]:3900"
            root_domain = ".s3.garage"
            """;

    // Em variável e devolvido no fim, como o do MySQL: encadear faria o compilador do Eclipse
    // acusar vazamento de recurso.
    @Bean
    GenericContainer<?> garageContainer() {
        final GenericContainer<?> container = new GenericContainer<>(GARAGE_IMAGE);
        container.withCopyToContainer(Transferable.of(CONFIGURATION), "/etc/garage.toml");
        container.withEnv("GARAGE_RPC_SECRET", SECRET_KEY);
        container.withEnv("GARAGE_DEFAULT_ACCESS_KEY", ACCESS_KEY);
        container.withEnv("GARAGE_DEFAULT_SECRET_KEY", SECRET_KEY);
        container.withEnv("GARAGE_DEFAULT_BUCKET", BUCKET);
        container.withCommand("/garage", "server", "--single-node", "--default-bucket");
        container.withExposedPorts(S3_PORT);
        container.waitingFor(Wait.forLogMessage(".*Creating default bucket.*", 1));
        return container;
    }

    @Bean
    DynamicPropertyRegistrar garageProperties(final GenericContainer<?> garageContainer) {
        return registry -> {
            registry.add(
                    "storage.endpoint",
                    () -> "http://%s:%d".formatted(garageContainer.getHost(), garageContainer.getMappedPort(S3_PORT)));
            registry.add("storage.bucket", () -> BUCKET);
            registry.add("storage.access-key", () -> ACCESS_KEY);
            registry.add("storage.secret-key", () -> SECRET_KEY);
        };
    }
}
