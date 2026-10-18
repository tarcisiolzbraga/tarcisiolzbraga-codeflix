package com.tarcisiolzbraga.codeflix.videos.infrastructure;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.LoggerFactory;

// Guarda o que os listeners e a infraestrutura do Kafka registraram **durante um teste**, para a
// mensagem de falha poder dizer o motivo em vez de só a aritmética.
//
// Isto existe por causa de uma intermitência real: o `GenreE2ETest` falhava no CI com
// `expected: <2> but was: <1>`, e o log do CI não ajudava. Foi medido: num job que falhou às
// 17:03:11, **todas** as 179 linhas de log da aplicação capturadas eram de 17:04 — o encerramento. O
// que aconteceu durante o teste nunca chegou ao relatório, e por isso três investigações terminaram
// em hipótese.
//
// O custo numa execução verde é uma fila em memória que ninguém lê. O ganho é que a próxima falha
// chega com as duas respostas que importam: se o gateway devolveu vazio ("nada a replicar"), ou se a
// mensagem foi para a fila morta — e, nesse caso, com a exceção que a mandou para lá.
public final class ListenerLogCapture implements AutoCloseable {

    // Os dois lugares onde o motivo apareceria: o nosso listener, e a infraestrutura que decide
    // repetir e desistir.
    //
    // `org.springframework.kafka` inteiro, e não só `.listener`: quem registra a exceção que mandou
    // a mensagem para a fila morta é o `DeadLetterPublishingRecovererFactory`, que vive em
    // `.retrytopic`. Medido — com só `.listener`, a falha chegava com o despejo do payload e sem a
    // causa.
    private static final List<String> LOGGERS =
            List.of("com.tarcisiolzbraga.codeflix.videos.infrastructure.kafka", "org.springframework.kafka");

    // Cada entrada é aparada: o `@DltHandler` registra o envelope do Debezium inteiro, e um payload
    // de três mil caracteres enterraria as outras linhas — justamente as que explicam.
    private static final int MAX_MESSAGE = 300;

    // Fila concorrente, e não a lista do `ListAppender` do logback: quem escreve é a thread do
    // consumidor do Kafka e quem lê é a do teste. Um `ArrayList` entre as duas é corrida, e numa
    // peça que existe para explicar corrida isso seria irônico.
    private final Queue<ILoggingEvent> eventos = new ConcurrentLinkedQueue<>();

    private final AppenderBase<ILoggingEvent> appender = new AppenderBase<>() {
        @Override
        protected void append(final ILoggingEvent event) {
            ListenerLogCapture.this.eventos.add(event);
        }
    };

    private ListenerLogCapture() {
        this.appender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        this.appender.setName("captura-de-teste");
        this.appender.start();
        LOGGERS.forEach(name -> logger(name).addAppender(this.appender));
    }

    public static ListenerLogCapture start() {
        return new ListenerLogCapture();
    }

    // Só WARN e acima: é aviso e erro que explicam. INFO do Kafka encheria a mensagem de falha com
    // eleição de líder e atribuição de partição, que não dizem nada sobre o gênero que faltou.
    public List<String> warningsAndErrors() {
        return this.eventos.stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .map(event -> "[%s] %s: %s"
                        .formatted(event.getLevel(), shortNameOf(event.getLoggerName()), trimmed(messageOf(event))))
                .toList();
    }

    // A exceção vai junto, e só a primeira linha dela: é ela que diz se a mensagem morreu por
    // conexão recusada, por token, ou por outra coisa. A pilha inteira não cabe numa mensagem de
    // falha de teste.
    private static String messageOf(final ILoggingEvent event) {
        final var causa = event.getThrowableProxy();
        return causa == null
                ? event.getFormattedMessage()
                : "%s | causa: %s: %s"
                        .formatted(event.getFormattedMessage(), causa.getClassName(), causa.getMessage());
    }

    private static String trimmed(final String message) {
        return message.length() <= MAX_MESSAGE ? message : message.substring(0, MAX_MESSAGE) + "…(aparado)";
    }

    private static String shortNameOf(final String logger) {
        final var dot = logger.lastIndexOf('.');
        return dot < 0 ? logger : logger.substring(dot + 1);
    }

    private static ch.qos.logback.classic.Logger logger(final String name) {
        return (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name);
    }

    @Override
    public void close() {
        LOGGERS.forEach(name -> logger(name).detachAppender(this.appender));
        this.appender.stop();
        this.eventos.clear();
    }
}
