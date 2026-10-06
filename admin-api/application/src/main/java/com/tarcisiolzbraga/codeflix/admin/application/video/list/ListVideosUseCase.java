package com.tarcisiolzbraga.codeflix.admin.application.video.list;

import com.tarcisiolzbraga.codeflix.admin.application.UseCase;
import com.tarcisiolzbraga.codeflix.admin.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.admin.domain.video.VideoSearchQuery;

public abstract class ListVideosUseCase extends UseCase<VideoSearchQuery, Pagination<VideoListOutput>> {
}
