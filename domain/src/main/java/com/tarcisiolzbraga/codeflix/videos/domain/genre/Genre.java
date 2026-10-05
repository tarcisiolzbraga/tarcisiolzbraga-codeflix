package com.tarcisiolzbraga.codeflix.videos.domain.genre;

import com.tarcisiolzbraga.codeflix.videos.domain.Entity;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.validation.ValidationHandler;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

// Réplica do gênero que o admin-codeflix governa, com a mesma forma dos outros: imutável, só
// with(...), sem newX(...) nem activate()/deactivate().
//
// É o primeiro agregado daqui com relação, e ela é guardada só pelo id da categoria, nunca pelo
// objeto. Quem resolve esses ids em categorias é a borda, pelo gateway da categoria — e é lá que os
// inativos ficam de fora, de graça.
//
// Um detalhe de que esta réplica depende: a tabela genre_category NÃO é capturada pelo CDC, então
// mudar os vínculos no admin só chega aqui porque o Genre de lá chama refreshUpdatedAt() em update,
// addCategory, addCategories e removeCategory, o que toca a linha do genre e gera o evento. Se algum
// dia um vínculo mudar sem tocar a linha, o catálogo fica velho em silêncio.
public final class Genre extends Entity<GenreID> {

    private final String name;
    private final boolean active;
    private final Set<CategoryID> categories;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Genre(
            final GenreID id,
            final String name,
            final boolean active,
            final Set<CategoryID> categories,
            final Instant createdAt,
            final Instant updatedAt) {
        super(id);
        this.name = name;
        this.active = active;
        this.categories = Set.copyOf(Objects.requireNonNull(categories, "'categories' should not be null"));
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Genre with(
            final GenreID id,
            final String name,
            final boolean active,
            final Set<CategoryID> categories,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Genre(id, name, active, categories, createdAt, updatedAt);
    }

    public static Genre with(final Genre genre) {
        return with(
                genre.getId(),
                genre.getName(),
                genre.isActive(),
                genre.getCategories(),
                genre.getCreatedAt(),
                genre.getUpdatedAt());
    }

    @Override
    public void validate(final ValidationHandler handler) {
        new GenreValidator(this, handler).validate();
    }

    public String getName() {
        return this.name;
    }

    public boolean isActive() {
        return this.active;
    }

    public Set<CategoryID> getCategories() {
        return this.categories;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }
}
