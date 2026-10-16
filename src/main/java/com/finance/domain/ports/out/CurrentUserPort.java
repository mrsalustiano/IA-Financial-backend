package com.finance.domain.ports.out;

import java.util.Optional;

/**
 * Expoe o identificador do usuario autenticado na requisicao corrente (extraido do token JWT).
 */
public interface CurrentUserPort {

    String requireUserId();

    Optional<String> currentUserId();
}
