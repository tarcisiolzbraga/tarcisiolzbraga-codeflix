package com.tarcisiolzbraga.codeflix.videos.application.castmember.list;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.SearchQuery;

public abstract class ListCastMembersUseCase extends UseCase<SearchQuery, Pagination<CastMemberOutput>> {
}
