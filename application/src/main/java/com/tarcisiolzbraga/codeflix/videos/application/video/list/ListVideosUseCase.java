package com.tarcisiolzbraga.codeflix.videos.application.video.list;

import com.tarcisiolzbraga.codeflix.videos.application.UseCase;
import com.tarcisiolzbraga.codeflix.videos.application.video.VideoOutput;
import com.tarcisiolzbraga.codeflix.videos.domain.pagination.Pagination;
import com.tarcisiolzbraga.codeflix.videos.domain.video.VideoSearchQuery;

public abstract class ListVideosUseCase extends UseCase<VideoSearchQuery, Pagination<VideoOutput>> {
}
