package com.tarcisiolzbraga.codeflix.videos.infrastructure.castmember.persistence;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberType;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

// Índice singular, como a tabela de origem no admin-codeflix.
@Document(indexName = "cast_member")
public class CastMemberDocument {

    @Id
    private String id;

    // O nome serve a busca por palavra e a ordenação, que querem coisas incompatíveis: o subcampo
    // keyword guarda o valor inteiro, para ordenar.
    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "name"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String name;

    // Keyword, e não Text: o tipo é um valor fechado, para filtrar e agrupar, nunca para analisar.
    @Field(type = FieldType.Keyword, name = "type")
    private String type;

    @Field(type = FieldType.Boolean, name = "active")
    private boolean active;

    @Field(type = FieldType.Date, name = "created_at")
    private Instant createdAt;

    @Field(type = FieldType.Date, name = "updated_at")
    private Instant updatedAt;

    public CastMemberDocument() {
    }

    public CastMemberDocument(
            final String id,
            final String name,
            final String type,
            final boolean active,
            final Instant createdAt,
            final Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CastMemberDocument from(final CastMember castMember) {
        return new CastMemberDocument(
                castMember.getId().getValue(),
                castMember.getName(),
                castMember.getType().name(),
                castMember.isActive(),
                castMember.getCreatedAt(),
                castMember.getUpdatedAt());
    }

    // Tipo gravado que esta versão não conhece vira nulo, e o validador do domínio o reporta: um
    // índice antigo não deve derrubar a leitura do catálogo inteiro.
    public CastMember toCastMember() {
        return CastMember.with(
                CastMemberID.from(this.id),
                this.name,
                CastMemberType.of(this.type).orElse(null),
                this.active,
                this.createdAt,
                this.updatedAt);
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
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
