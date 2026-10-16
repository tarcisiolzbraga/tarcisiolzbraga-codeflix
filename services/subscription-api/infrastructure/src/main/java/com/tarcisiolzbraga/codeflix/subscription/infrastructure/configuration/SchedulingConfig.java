package com.tarcisiolzbraga.codeflix.subscription.infrastructure.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Liga o agendamento. Em classe própria, e não na Main: agendamento é assunto transversal, e
// deixá-lo aqui faz aparecer no lugar em que alguém vai procurar por ele.
//
// Vale também nos testes, porque a classe entra no contexto. Não incomoda: o cron padrão da única
// tarefa é de madrugada, então ela não dispara durante uma execução de suíte — e quem testa a
// varredura chama o caso de uso direto, sem esperar relógio.
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {}
