package com.finance.domain.validator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CpfValidatorTest {

    @Test
    void acceptsValidCpf() {
        assertThat(CpfValidator.isValid("529.982.247-25")).isTrue();
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
    }

    @Test
    void rejectsInvalidCheckDigits() {
        assertThat(CpfValidator.isValid("529.982.247-24")).isFalse();
        assertThat(CpfValidator.isValid("11144477734")).isFalse();
    }

    @Test
    void rejectsRepeatedDigitsAndWrongLength() {
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("1234")).isFalse();
        assertThat(CpfValidator.isValid("123456789012")).isFalse();
        assertThat(CpfValidator.isValid("")).isFalse();
        assertThat(CpfValidator.isValid(null)).isFalse();
        assertThat(CpfValidator.isValid("abc")).isFalse();
    }

    @Test
    void ensureValidThrowsWithMessage() {
        assertThatThrownBy(() -> CpfValidator.ensureValid("12345678901"))
            .isInstanceOf(com.finance.shared.exception.ValidationException.class)
            .hasMessageContaining("CPF inválido");
        assertThat(CpfValidator.sanitize("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfValidator.sanitize(null)).isNull();
    }
}
