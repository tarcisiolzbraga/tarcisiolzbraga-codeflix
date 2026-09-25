package com.tarcisiolzbraga.codeflix.admin.application.castmember.list;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.SearchQuery;

public abstract class ListCastMembersUseCase extends UseCase<SearchQuery, Pagination<CastMemberListOutput>> {
}
