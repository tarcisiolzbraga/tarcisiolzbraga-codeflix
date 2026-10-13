package com.tarcisiolzbraga.codeflix.subscription.domain.plan;

import com.tarcisiolzbraga.codeflix.subscription.domain.AggregateRoot;
import com.tarcisiolzbraga.codeflix.subscription.domain.money.Money;
import com.tarcisiolzbraga.codeflix.subscription.domain.utils.InstantUtils;
import java.time.Instant;

// O plano que o assinante escolhe: nome, descrição, preço e se está em oferta.
//
// Sem deletedAt, que a referência tem: lá ele é preenchido ao desativar e limpo ao ativar, o que o
// torna um espelho do `active` que nenhuma regra do serviço consulta — e os CLAUDE.md do monorepo
// dizem que aqui não há coluna de exclusão lógica.
public class Plan extends AggregateRoot<PlanId> {

    private static final int NAME_MAX_LENGTH = 100;
    private static final int DESCRIPTION_MAX_LENGTH = 500;

    private String name;
    private String description;
    private boolean active;
    private Money price;
    private Instant createdAt;
    private Instant updatedAt;

    private Plan(
            final PlanId anId,
            final int version,
            final String aName,
            final String aDescription,
            final Boolean isActive,
            final Money aPrice,
            final Instant createdAt,
            final Instant updatedAt) {
        super(anId, version);
        this.setName(aName);
        this.setDescription(aDescription);
        this.setActive(isActive);
        this.setPrice(aPrice);
        this.setCreatedAt(createdAt);
        this.setUpdatedAt(updatedAt);
    }

    public static Plan newPlan(
            final PlanId anId,
            final String aName,
            final String aDescription,
            final Boolean isActive,
            final Money aPrice) {
        final var now = InstantUtils.now();
        return new Plan(anId, 0, aName, aDescription, isActive, aPrice, now, now);
    }

    public static Plan with(
            final PlanId anId,
            final int version,
            final String aName,
            final String aDescription,
            final Boolean isActive,
            final Money aPrice,
            final Instant createdAt,
            final Instant updatedAt) {
        return new Plan(anId, version, aName, aDescription, isActive, aPrice, createdAt, updatedAt);
    }

    // Não toca na versão de propósito: ela é do lock otimista, e quem a incrementa é o UPDATE do
    // repositório. A referência a incrementa aqui e ainda compara por ela no WHERE, e com isso
    // nenhuma linha casa — alterar um plano nunca funcionaria.
    public void execute(final PlanCommand... commands) {
        if (commands == null || commands.length == 0) {
            return;
        }

        for (final var command : commands) {
            switch (command) {
                case PlanCommand.ChangePlan c -> apply(c);
                case PlanCommand.ActivatePlan c -> apply(c);
                case PlanCommand.DeactivatePlan c -> apply(c);
            }
        }

        this.setUpdatedAt(InstantUtils.now());
    }

    public String name() {
        return this.name;
    }

    public String description() {
        return this.description;
    }

    public boolean active() {
        return this.active;
    }

    public Money price() {
        return this.price;
    }

    public Instant createdAt() {
        return this.createdAt;
    }

    public Instant updatedAt() {
        return this.updatedAt;
    }

    private void apply(final PlanCommand.ChangePlan command) {
        this.setName(command.name());
        this.setDescription(command.description());
        this.setPrice(command.price());
    }

    private void apply(final PlanCommand.ActivatePlan command) {
        this.active = true;
    }

    private void apply(final PlanCommand.DeactivatePlan command) {
        this.active = false;
    }

    private void setName(final String name) {
        this.assertArgumentNotEmpty(name, "'name' should not be empty");
        this.assertArgumentMaxLength(
                name, NAME_MAX_LENGTH, "'name' should not have more than %d characters".formatted(NAME_MAX_LENGTH));
        this.name = name;
    }

    private void setDescription(final String description) {
        this.assertArgumentNotEmpty(description, "'description' should not be empty");
        this.assertArgumentMaxLength(
                description,
                DESCRIPTION_MAX_LENGTH,
                "'description' should not have more than %d characters".formatted(DESCRIPTION_MAX_LENGTH));
        this.description = description;
    }

    private void setActive(final Boolean active) {
        this.assertArgumentNotNull(active, "'active' should not be null");
        this.active = active;
    }

    private void setPrice(final Money price) {
        this.assertArgumentNotNull(price, "'price' should not be null");
        this.price = price;
    }

    private void setCreatedAt(final Instant createdAt) {
        this.createdAt = this.assertArgumentNotNull(createdAt, "'createdAt' should not be null");
    }

    private void setUpdatedAt(final Instant updatedAt) {
        this.updatedAt = this.assertArgumentNotNull(updatedAt, "'updatedAt' should not be null");
    }
}
