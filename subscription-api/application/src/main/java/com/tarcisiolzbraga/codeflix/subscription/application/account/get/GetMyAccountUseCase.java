package com.tarcisiolzbraga.codeflix.subscription.application.account.get;

import com.tarcisiolzbraga.codeflix.subscription.application.UseCase;
import com.tarcisiolzbraga.codeflix.subscription.application.account.AccountOutput;

// A entrada é o id de quem chama no provedor de identidade, que vem do `sub` do token. Não é um id
// de conta: com id de conta na entrada, a rota teria de perguntar se aquela conta é de quem pediu.
public abstract class GetMyAccountUseCase extends UseCase<String, AccountOutput> {}
