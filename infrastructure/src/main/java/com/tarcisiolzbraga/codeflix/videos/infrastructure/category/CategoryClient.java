package com.tarcisiolzbraga.codeflix.videos.infrastructure.category;

import com.tarcisiolzbraga.codeflix.videos.infrastructure.category.models.CategoryDTO;
import java.util.Optional;

public interface CategoryClient {

    // Vazio quando o admin responde 404. Isso acontece de verdade e não é erro: entre o evento de
    // criação e esta chamada a categoria pode ter sido apagada lá, e o evento de remoção já está a
    // caminho. Qualquer outra falha sobe como exceção, para a mensagem ser tentada de novo.
    Optional<CategoryDTO> categoryOfId(String id);
}
