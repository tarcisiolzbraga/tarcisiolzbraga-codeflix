package com.tarcisiolzbraga.codeflix.admin.application.castmember.delete;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberGateway;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.IntegrationTest;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence.CastMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@IntegrationTest
class DeleteCastMemberUseCaseIT {

    @Autowired
    private DeleteCastMemberUseCase useCase;

    @Autowired
    private CastMemberRepository castMemberRepository;

    @MockitoSpyBean
    private CastMemberGateway castMemberGateway;

    @Test
    void givenPersistedCastMember_whenCallExecute_thenRemoveIt() {
        final var castMember = givenPersistedCastMember();

        this.useCase.execute(castMember.getId().getValue());

        assertEquals(0, this.castMemberRepository.count());
    }

    @Test
    void givenUnknownId_whenCallExecute_thenDoNothing() {
        givenPersistedCastMember();

        this.useCase.execute(CastMemberID.unique().getValue());

        assertEquals(1, this.castMemberRepository.count());
    }

    @Test
    void givenFailingGateway_whenCallExecute_thenPropagateTheException() {
        final var castMember = givenPersistedCastMember();
        final var expectedException = new IllegalStateException("banco indisponível");
        doThrow(expectedException).when(this.castMemberGateway).deleteById(any());

        final var actualException = assertThrows(
                IllegalStateException.class, () -> this.useCase.execute(castMember.getId().getValue()));

        assertSame(expectedException, actualException);
        assertEquals(1, this.castMemberRepository.count());
    }

    private CastMember givenPersistedCastMember() {
        return this.castMemberGateway.create(CastMember.newCastMember("Vin Diesel", CastMemberType.ACTOR, true));
    }
}
