package com.tarcisiolzbraga.codeflix.admin.infrastructure.castmember.persistence;

import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMember;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberID;
import com.tarcisiolzbraga.codeflix.admin.domain.castmember.CastMemberType;
import com.tarcisiolzbraga.codeflix.admin.infrastructure.persistence.BaseJpaEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;

@Audited
@Entity(name = "CastMember")
@Table(name = "cast_member")
public class CastMemberJpaEntity extends BaseJpaEntity {

    @Column(name = "name", nullable = false)
    private String name;

    // STRING e não ORDINAL: a coluna guarda o nome do tipo, então reordenar o enum não corrompe o que
    // já está gravado.
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private CastMemberType type;

    protected CastMemberJpaEntity() {
    }

    private CastMemberJpaEntity(final CastMember castMember) {
        super(
                castMember.getId().value(),
                castMember.isActive(),
                castMember.getCreatedAt(),
                castMember.getUpdatedAt());
        this.name = castMember.getName();
        this.type = castMember.getType();
    }

    public static CastMemberJpaEntity from(final CastMember castMember) {
        return new CastMemberJpaEntity(castMember);
    }

    public CastMember toAggregate() {
        return CastMember.with(
                CastMemberID.from(getId()), this.name, this.type, isActive(), getCreatedAt(), getUpdatedAt());
    }

    public String getName() {
        return this.name;
    }

    public CastMemberType getType() {
        return this.type;
    }
}
