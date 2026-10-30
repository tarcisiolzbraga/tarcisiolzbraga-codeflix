package com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tarcisiolzbraga.codeflix.subscription.infrastructure.ratelimit.RateLimitProperties.Tier;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import java.time.Duration;
import java.util.Objects;

// Um balde por origem e por degrau, guardados num cache com teto.
//
// **O teto é a parte que importa.** Num mapa comum, cada origem nova deixaria uma entrada para
// sempre: quem quisesse derrubar o serviço pediria de um IP diferente a cada vez e encheria a
// memória com os próprios baldes — a defesa virava o ataque. Aqui o Caffeine descarta por tamanho e
// por tempo sem uso.
//
// Descartar um balde **devolve fichas** a quem for descartado, e isso é aceitável de propósito: a
// expiração é maior que o período de recarga, então um balde só sai depois do instante em que já
// estaria cheio de novo. Quem está dentro do limite não percebe; quem está martelando mantém o
// balde vivo pelo próprio acesso e não é descartado.
public class BucketRegistry {

    // 100 mil baldes é da ordem de alguns MB e cobre tráfego real com folga; acima disso o Caffeine
    // descarta o menos usado, que é o visitante de uma requisição só.
    private static final long MAX_BUCKETS = 100_000;

    private final Cache<String, Bucket> buckets;

    public BucketRegistry(final Duration longestPeriod) {
        Objects.requireNonNull(longestPeriod, "'longestPeriod' should not be null");
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAX_BUCKETS)
                // O dobro do maior período em uso: garante que nenhum balde saia antes de ter se
                // recarregado, que é o que torna o descarte inofensivo.
                .expireAfterAccess(longestPeriod.multipliedBy(2))
                .build();
    }

    public Bucket bucketOf(final String tierName, final String client, final Tier tier) {
        return this.buckets.get(tierName + "|" + client, key -> newBucket(tier));
    }

    private static Bucket newBucket(final Tier tier) {
        return Bucket.builder().addLimit(limitOf(tier)).build();
    }

    // refillGreedy, e não refillIntervally: a ficha volta aos poucos ao longo do período, em vez de
    // o balde encher de uma vez na virada. Com a recarga em bloco, quem fosse recusado passaria a
    // bater no relógio esperando a virada, e o limite viraria rajada periódica.
    private static Bandwidth limitOf(final Tier tier) {
        return Bandwidth.builder()
                .capacity(tier.capacity())
                .refillGreedy(tier.capacity(), tier.period())
                .build();
    }
}
