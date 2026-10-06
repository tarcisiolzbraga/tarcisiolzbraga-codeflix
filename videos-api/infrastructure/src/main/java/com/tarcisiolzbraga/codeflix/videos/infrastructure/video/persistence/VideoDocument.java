package com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Rating;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoDetails;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoFlags;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoMedias;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoReferences;
import java.time.Instant;
import java.time.Year;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

// Índice singular, como a tabela de origem no admin-codeflix.
//
// Plano de propósito, ao contrário do domínio, que agrupa em value objects: os filtros da listagem
// apontam para campos de primeiro nível (rating, launched_at, categories, genres, cast_members), e
// aninhar obrigaria a consulta a atravessar objetos sem ganho algum.
//
// Sem construtor cheio: achatado ele teria dezessete parâmetros, acima do limite de dez do projeto.
// Quem monta é o from(...), pelos setters que o Spring Data já exige de todo modo.
@Document(indexName = "video")
public class VideoDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, name = "title"),
            otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword))
    private String title;

    @Field(type = FieldType.Text, name = "description")
    private String description;

    @Field(type = FieldType.Integer, name = "launched_at")
    private Integer launchedAt;

    @Field(type = FieldType.Double, name = "duration")
    private double duration;

    // Keyword: valor fechado, para filtrar.
    @Field(type = FieldType.Keyword, name = "rating")
    private String rating;

    @Field(type = FieldType.Boolean, name = "opened")
    private boolean opened;

    @Field(type = FieldType.Boolean, name = "published")
    private boolean published;

    @Field(type = FieldType.Boolean, name = "active")
    private boolean active;

    // Endereços de arquivo: keyword, nunca analisados como texto.
    @Field(type = FieldType.Keyword, name = "video")
    private String video;

    @Field(type = FieldType.Keyword, name = "trailer")
    private String trailer;

    @Field(type = FieldType.Keyword, name = "banner")
    private String banner;

    @Field(type = FieldType.Keyword, name = "thumbnail")
    private String thumbnail;

    @Field(type = FieldType.Keyword, name = "thumbnail_half")
    private String thumbnailHalf;

    @Field(type = FieldType.Keyword, name = "categories")
    private Set<String> categories;

    @Field(type = FieldType.Keyword, name = "genres")
    private Set<String> genres;

    @Field(type = FieldType.Keyword, name = "cast_members")
    private Set<String> castMembers;

    @Field(type = FieldType.Date, name = "created_at")
    private Instant createdAt;

    @Field(type = FieldType.Date, name = "updated_at")
    private Instant updatedAt;

    public static VideoDocument from(final Video source) {
        final var document = new VideoDocument();
        document.id = source.getId().getValue();
        final var details = source.getDetails();
        document.title = details.title();
        document.description = details.description();
        document.launchedAt = details.launchedAt() == null ? null : details.launchedAt().getValue();
        document.duration = details.duration();
        document.rating = details.rating() == null ? null : details.rating().getLabel();
        final var flags = source.getFlags();
        document.opened = flags.opened();
        document.published = flags.published();
        document.active = flags.active();
        final var medias = source.getMedias();
        document.video = medias.video();
        document.trailer = medias.trailer();
        document.banner = medias.banner();
        document.thumbnail = medias.thumbnail();
        document.thumbnailHalf = medias.thumbnailHalf();
        final var references = source.getReferences();
        document.categories = references.categories().stream().map(CategoryID::getValue).collect(Collectors.toSet());
        document.genres = references.genres().stream().map(GenreID::getValue).collect(Collectors.toSet());
        document.castMembers =
                references.castMembers().stream().map(CastMemberID::getValue).collect(Collectors.toSet());
        document.createdAt = source.getCreatedAt();
        document.updatedAt = source.getUpdatedAt();
        return document;
    }

    // Classificação gravada que esta versão não conhece vira nulo, e o validador do domínio a
    // reporta: um índice antigo não deve derrubar a leitura do catálogo inteiro.
    public Video toVideo() {
        return Video.with(
                VideoID.from(this.id),
                VideoDetails.with(
                        this.title,
                        this.description,
                        this.launchedAt == null ? null : Year.of(this.launchedAt),
                        this.duration,
                        Rating.of(this.rating).orElse(null)),
                new VideoFlags(this.opened, this.published, this.active),
                VideoMedias.with(this.video, this.trailer, this.banner, this.thumbnail, this.thumbnailHalf),
                VideoReferences.with(
                        idsOf(this.categories, CategoryID::from),
                        idsOf(this.genres, GenreID::from),
                        idsOf(this.castMembers, CastMemberID::from)),
                this.createdAt,
                this.updatedAt);
    }

    private static <T> Set<T> idsOf(final Set<String> values, final Function<String, T> factory) {
        if (values == null) {
            return Set.of();
        }
        return values.stream().map(factory).collect(Collectors.toUnmodifiableSet());
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public boolean isPublished() {
        return this.published;
    }

    public void setPublished(final boolean published) {
        this.published = published;
    }

    public boolean isVisibleInTheCatalog() {
        return this.active && this.published;
    }
}
