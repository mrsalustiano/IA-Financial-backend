package com.finance.adapters.out.mongodb;

import com.finance.domain.model.RefreshToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RefreshTokenMongoAdapterTest {

    @Mock
    private RefreshTokenMongoRepository repository;

    @InjectMocks
    private RefreshTokenMongoAdapter adapter;

    @Test
    void savesAndFindsByHash() {
        RefreshToken token = com.finance.domain.model.RefreshToken.create("rt-1", "u-1", "hash",
            Instant.now().plusSeconds(600), Instant.now());
        when(repository.save(any(RefreshTokenDocument.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findByTokenHashAndDeletedFalse("hash"))
            .thenReturn(Optional.of(new RefreshTokenDocument("rt-1", "u-1", "hash",
                Instant.now().plusSeconds(600), false, null, false, null, Instant.now(), Instant.now())));

        RefreshToken saved = adapter.save(token);
        Optional<RefreshToken> found = adapter.findByTokenHash("hash");

        assertThat(saved.getTokenHash()).isEqualTo("hash");
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("u-1");
        assertThat(adapter.findByTokenHash("missing")).isEmpty();
    }
}
