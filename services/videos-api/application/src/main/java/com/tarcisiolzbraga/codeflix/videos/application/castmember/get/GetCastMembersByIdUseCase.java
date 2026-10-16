package com.tarcisiolzbraga.codeflix.videos.application.castmember.get;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.castmember.CastMemberOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.castmember.CastMemberID;
import java.util.List;
import java.util.Set;

// Devolve List, e não Pagination, porque o resultado é limitado pelos ids recebidos.
public abstract class GetCastMembersByIdUseCase extends UseCase<Set<CastMemberID>, List<CastMemberOutput>> {
}
