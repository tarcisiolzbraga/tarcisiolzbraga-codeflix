package com.tarcisiolzbraga.codeflix.videos.infrastructure.video;

import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.videos.domain.category.CategoryID;
import com.tarcisiolzbraga.codeflix.videos.domain.genre.GenreID;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.Video;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoGateway;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoID;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.elasticsearch.SearchTerms;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence.VideoDocument;
import com.tarcisiolzbraga.codeflix.videos.infrastructure.video.persistence.VideoRepository;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchOperations;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class VideoElasticsearchGateway implements VideoGateway {

    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String ACTIVE = "active";
    private static final String PUBLISHED = "published";
    private static final String RATING = "rating";
    private static final String LAUNCHED_AT = "launched_at";
    private static final String CATEGORIES = "categories";
    private static final String GENRES = "genres";
    private static final String CAST_MEMBERS = "cast_members";
    private static final String KEYWORD_SUFFIX = ".keyword";

    private final VideoRepository videoRepository;
    private final SearchOperations searchOperations;

    public VideoElasticsearchGateway(
            final VideoRepository videoRepository, final SearchOperations searchOperations) {
        this.videoRepository = Objects.requireNonNull(videoRepository, "'videoRepository' should not be null");
        this.searchOperations = Objects.requireNonNull(searchOperations, "'searchOperations' should not be null");
    }

    @Override
    public Video save(final Video video) {
        this.videoRepository.save(VideoDocument.from(video));
        return video;
    }

    @Override
    public void deleteById(final VideoID id) {
        this.videoRepository.deleteById(id.getValue());
    }

    // Vazio para o que o catálogo não serve: inativo ou não publicado. O documento continua gravado,
    // então publicar no admin faz aparecer na hora.
    @Override
    public Optional<Video> findById(final VideoID id) {
        return this.videoRepository
                .findById(id.getValue())
                .filter(VideoDocument::isVisibleInTheCatalog)
                .map(VideoDocument::toVideo);
    }

    @Override
    public Pagination<Video> findAll(final VideoSearchQuery query) {
        final var page = PageRequest.of(query.page(), query.perPage(), sortOf(query));
        final var result = this.searchOperations.search(queryOf(query, page), VideoDocument.class);
        final var items = result.stream()
                .map(SearchHit::getContent)
                .map(VideoDocument::toVideo)
                .toList();
        return new Pagination<>(query.page(), query.perPage(), result.getTotalHits(), items);
    }

    // Cada filtro entra como subcriteria, um por vez, para ficar agrupado: encadeados no mesmo nível,
    // o resultado ficaria à mercê da precedência entre and e or, e o "ou" do texto deixaria passar
    // vídeo que não devia.
    private static Query queryOf(final VideoSearchQuery query, final PageRequest page) {
        var criteria = new Criteria(ACTIVE).is(true).and(new Criteria(PUBLISHED).is(true));
        final var terms = query.terms();
        if (terms != null && !terms.isBlank()) {
            criteria = criteria.subCriteria(SearchTerms.across(terms, TITLE, DESCRIPTION));
        }
        if (query.rating() != null) {
            criteria = criteria.subCriteria(new Criteria(RATING).is(query.rating().getLabel()));
        }
        if (query.launchedAt() != null) {
            criteria = criteria.subCriteria(new Criteria(LAUNCHED_AT).is(query.launchedAt()));
        }
        criteria = withIn(criteria, CATEGORIES, query.categories(), CategoryID::getValue);
        criteria = withIn(criteria, GENRES, query.genres(), GenreID::getValue);
        criteria = withIn(criteria, CAST_MEMBERS, query.castMembers(), CastMemberID::getValue);
        return new CriteriaQuery(criteria, page);
    }

    private static <T> Criteria withIn(
            final Criteria criteria,
            final String field,
            final Collection<T> ids,
            final Function<T, String> value) {
        if (ids.isEmpty()) {
            return criteria;
        }
        return criteria.subCriteria(new Criteria(field).in(ids.stream().map(value).toList()));
    }

    private static Sort sortOf(final VideoSearchQuery query) {
        final var field = TITLE.equals(query.sort()) ? TITLE + KEYWORD_SUFFIX : query.sort();
        return Sort.by(Sort.Direction.fromString(query.direction()), field);
    }
}
