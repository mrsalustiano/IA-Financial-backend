package com.finance.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private final Instant now = Instant.parse("2020-01-01T12:00:00Z");

    @Test
    void createStartsActiveAndAudited() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash", true, now);

        assertThat(user.getId()).isEqualTo("u-1");
        assertThat(user.getUserId()).isEqualTo("u-1");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getCreatedAt()).isEqualTo(now);
        assertThat(user.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void activateDeactivateAndTouchUpdatedAt() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash", true, now);

        user.deactivate();
        assertThat(user.isActive()).isFalse();
        user.activate();
        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isAfter(now);
    }

    @Test
    void changePasswordHashUpdatesUpdatedAt() {
        User user = User.create("u-1", "a@test.com", "52998224725", "hash", true, now);

        user.changePasswordHash("novo-hash");

        assertThat(user.getPasswordHash()).isEqualTo("novo-hash");
        assertThat(user.getUpdatedAt()).isAfter(now);
    }

    @Test
    void rehydratePreservesSoftDelete() {
        User user = User.rehydrate("u-2", "b@test.com", "52998224725", "hash", false, true, now, now, now);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(now);
        assertThat(user.getEmail()).isEqualTo("b@test.com");
        assertThat(user.getCpf()).isEqualTo("52998224725");
    }
}
