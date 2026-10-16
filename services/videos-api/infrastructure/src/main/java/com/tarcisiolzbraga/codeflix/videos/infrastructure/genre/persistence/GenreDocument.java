package com.tarcisiolzbraga.codeflix.videos.infrastructure.genre.persistence;

import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.Genre;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

// Índice singular, como a tabela de origem no admin-codeflix.
//
// As categorias ficam aqui, no próprio documento, e não numa junção: o Elasticsearch não faz junção,
// e esta é a forma que serve ao filtro "gêneros desta categoria" numa consulta só.
@Document(indexName = "genre")
public class GenreDocument {

    // UUID e não texto: o _id do Elasticsearch é string no protocolo, mas o que guardamos nele é
    // um UUID, e o tipo passa a dizer. A conversão é do Spring Data, na borda do cliente.
    @Id
    private UUID id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "name"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String name;

    @Field(type = FieldType.Boolean, name = "active")
    private boolean active;

    // Keyword: são ids, para casar exato e filtrar, nunca para analisar como texto. UUID e não
    // texto, como no resto: o campo guarda id, e o tipo diz isso.
    @Field(type = FieldType.Keyword, name = "categories")
    private Set<UUID> categories;

    @Field(type = FieldType.Date, name = "created_at")
    private Instant createdAt;

    @Field(type = FieldType.Date, name = "updated_at")
    private Instant updatedAt;

    public GenreDocument() {
    }

    public GenreDocument(
            final UUID id,
            final String name,
            final boolean active,
            final Set<UUID> categories,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.active = active;
        this.categories = categories;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static GenreDocument from(final Genre genre) {
        return new GenreDocument(
                genre.getId().value(),
                genre.getName(),
                genre.isActive(),
                genre.getCategories().stream().map(CategoryID::value).collect(Collectors.toSet()),
                genre.getCreatedAt(),
                genre.getUpdatedAt());
    }

    public Genre toGenre() {
        return Genre.with(
                GenreID.from(this.id),
                this.name,
                this.active,
                this.categories == null
                        ? Set.of()
                        : this.categories.stream().map(CategoryID::from).collect(Collectors.toUnmodifiableSet()),
                this.createdAt,
                this.updatedAt);
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public Set<UUID> getCategories() {
        return this.categories;
    }

    public void setCategories(final Set<UUID> categories) {
        this.categories = categories;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
