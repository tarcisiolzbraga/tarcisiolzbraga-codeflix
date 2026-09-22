package com.tarcisiolzbraga.codeflix.admin.infrastructure;

import com.tarcisiolzbraga.codeflix.admin.infrastructure.category.persistence.CategoryRepository;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.genre.persistence.GenreRepository;
import java.util.List;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Esvazia as tabelas antes de cada teste de integração, no lugar de um @BeforeEach por classe.
// Roda antes dos @BeforeEach do próprio teste, então quem semeia dados continua funcionando.
public class MySQLCleanUpExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(final ExtensionContext context) {
        final var applicationContext = SpringExtension.getApplicationContext(context);
        cleanUp(List.of(
                applicationContext.getBean(GenreRepository.class),
                applicationContext.getBean(CategoryRepository.class)));
    }

    // A ordem importa: um repositório que referencia outro vem antes, por causa das foreign keys.
    private void cleanUp(final List<CrudRepository<?, ?>> repositories) {
        repositories.forEach(CrudRepository::deleteAll);
    }
}
