package com.retrobolsa.api.security;

import com.retrobolsa.api.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de tentativas de login em memória, por e-mail. Protege contra
 * força bruta sem depender de infraestrutura externa (Redis/DB).
 * <p>
 * Não é distribuído entre múltiplas instâncias da aplicação — suficiente para
 * o volume atual do projeto; se a API escalar horizontalmente, mover para Redis.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, Deque<Instant>> attemptsByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        String normalizedKey = key == null ? "unknown" : key.toLowerCase();
        Deque<Instant> attempts = attemptsByKey.computeIfAbsent(normalizedKey, k -> new ArrayDeque<>());

        synchronized (attempts) {
            Instant windowStart = Instant.now().minusSeconds(WINDOW_SECONDS);
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(windowStart)) {
                attempts.pollFirst();
            }
            if (attempts.size() >= MAX_ATTEMPTS) {
                throw new RateLimitExceededException(
                        "Muitas tentativas de login. Aguarde um minuto antes de tentar novamente.");
            }
            attempts.addLast(Instant.now());
        }
    }
}
