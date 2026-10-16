package com.finance.adapters.out.mongodb;

import com.finance.domain.model.User;
import com.finance.shared.exception.DatabaseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserMongoAdapterTest {

    @Mock
    private UserMongoRepository repository;

    @Mock
    private MongoTemplate mongoTemplate;

    private final UserPersistenceMapper persistenceMapper = Mappers.getMapper(UserPersistenceMapper.class);

    private UserMongoAdapter adapter() {
        return new UserMongoAdapter(repository, persistenceMapper, mongoTemplate);
    }

    private User user() {
        return User.create("u-1", "a@test.com", "52998224725", "hash", true, Instant.now());
    }

    @Test
    void savesAndReadsBack() {
        when(repository.save(any(UserDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findById("u-1")).thenReturn(Optional.of(new UserDocument("u-1", "a@test.com",
            "52998224725", "hash", true, false, null, Instant.now(), Instant.now())));
        when(repository.findByEmailIgnoreCaseAndDeletedFalse("a@test.com"))
            .thenReturn(Optional.of(new UserDocument("u-1", "a@test.com", "52998224725", "hash", true, false,
                null, Instant.now(), Instant.now())));

        User saved = adapter().save(user());

        assertThat(saved.getEmail()).isEqualTo("a@test.com");
        assertThat(adapter().findById("u-1")).isPresent();
        assertThat(adapter().findByEmail("a@test.com")).isPresent();
        assertThat(adapter().findById("missing")).isEmpty();
    }

    @Test
    void mapsDuplicateKeyToConflict() {
        when(repository.save(any(UserDocument.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> adapter().save(user())).isInstanceOf(DatabaseException.class)
            .hasCauseInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void checksUniquenessWithAndWithoutExcludeId() {
        when(repository.existsByEmailIgnoreCaseAndDeletedFalse("a@test.com")).thenReturn(true);
        when(repository.existsByEmailIgnoreCaseAndDeletedFalseAndIdNot("a@test.com", "u-9")).thenReturn(false);
        when(repository.existsByCpfAndDeletedFalse("52998224725")).thenReturn(true);
        when(repository.existsByCpfAndDeletedFalseAndIdNot("52998224725", "u-9")).thenReturn(false);

        assertThat(adapter().existsByEmail("a@test.com", null)).isTrue();
        assertThat(adapter().existsByEmail("a@test.com", "u-9")).isFalse();
        assertThat(adapter().existsByCpf("52998224725", null)).isTrue();
        assertThat(adapter().existsByCpf("52998224725", "u-9")).isFalse();
    }

    @Test
    void listsActiveUserIdsAndWrapsFailure() {
        when(mongoTemplate.find(isA(Query.class), eq(UserDocument.class)))
            .thenReturn(List.of(new UserDocument("u-1", "a@test.com", "52998224725", "hash", true, false, null,
                Instant.now(), Instant.now())));
        assertThat(adapter().findAllActiveUserIds()).containsExactly("u-1");

        when(mongoTemplate.find(isA(Query.class), eq(UserDocument.class))).thenThrow(mock(IllegalStateException.class));
        assertThatThrownBy(() -> adapter().findAllActiveUserIds()).isInstanceOf(DatabaseException.class);
    }
}
