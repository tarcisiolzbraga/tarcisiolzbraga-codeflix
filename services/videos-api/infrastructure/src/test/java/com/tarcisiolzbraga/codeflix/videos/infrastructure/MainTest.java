package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import org.junit.jupiter.api.Test;

// Com @IntegrationTest, e não só @SpringBootTest: o repositório do Elasticsearch confere o índice ao
// ser criado, então subir o contexto já exige o servidor de verdade.
@IntegrationTest
class MainTest {

    @Test
    void contextLoads() {
    }
}
