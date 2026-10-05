package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence.CastMemberRepository;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.persistence.CategoryRepository;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence.GenreRepository;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Esvazia os índices antes de cada teste de integração, no lugar de um @BeforeEach por classe.
// Roda antes dos @BeforeEach do próprio teste, então quem semeia dados continua funcionando.
//
// Aqui a ordem não importa: réplicas de leitura não têm vínculo entre si, cada índice é independente.
public class ElasticsearchCleanUpExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(final ExtensionContext context) {
        final var applicationContext = SpringExtension.getApplicationContext(context);
        cleanUp(List.of(
                applicationContext.getBean(CategoryRepository.class),
                applicationContext.getBean(CastMemberRepository.class),
                applicationContext.getBean(GenreRepository.class)));
    }

    private void cleanUp(final List<CrudRepository<?, ?>> repositories) {
        repositories.forEach(CrudRepository::deleteAll);
    }
}
