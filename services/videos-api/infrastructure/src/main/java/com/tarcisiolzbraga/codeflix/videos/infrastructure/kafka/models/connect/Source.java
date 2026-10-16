package com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka.models.connect;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// De onde a mudança veio. Serve ao log: com o banco, a tabela e a posição no binlog dá para casar
// uma mensagem daqui com o que aconteceu no admin-codeflix.
@JsonIgnoreProperties(ignoreUnknown = true)
public record Source(
        @JsonProperty("connector") String connector,
        @JsonProperty("name") String name,
        @JsonProperty("db") String database,
        @JsonProperty("table") String table,
        @JsonProperty("file") String binlogFile,
        @JsonProperty("pos") Long binlogPosition) {
}
