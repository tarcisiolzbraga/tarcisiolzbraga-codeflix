package com.tarcisiolzbraga.codeflix.admin.infrastructure.storage;

import com.tarcisiolzbraga.codeflix.admin.domain.video.Resource;
import java.util.Optional;
import java.util.Set;

// O armazenamento visto pela aplicação: guarda, devolve e apaga arquivo por nome. Não há listagem
// por prefixo porque o nome de cada arquivo é calculado a partir do vídeo e do tipo da mídia.
public interface StorageService {

    void store(String name, Resource resource);

    Optional<Resource> get(String name);

    void delete(Set<String> names);
}
