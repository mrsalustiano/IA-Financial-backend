package com.finance.domain.validator;

import com.finance.shared.exception.ValidationException;

public final class CpfValidator {

    private CpfValidator() {
    }

    /**
     * Valida CPF aceitando somente os 11 digitos numericos (mascara removida), sem digitos repetidos
     * e com os dois digitos verificadores corretos.
     */
    public static boolean isValid(String rawCpf) {
        String cpf = sanitize(rawCpf);
        if (cpf == null || cpf.length() != 11 || cpf.chars().distinct().count() == 1) {
            return false;
        }
        int digit9 = checkDigit(cpf, 9);
        int digit10 = checkDigit(cpf, 10);
        return digit9 == Character.getNumericValue(cpf.charAt(9))
            && digit10 == Character.getNumericValue(cpf.charAt(10));
    }

    public static void ensureValid(String rawCpf) {
        if (!isValid(rawCpf)) {
            throw new ValidationException("CPF inválido");
        }
    }

    public static String sanitize(String rawCpf) {
        if (rawCpf == null) {
            return null;
        }
        String digits = rawCpf.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private static int checkDigit(String cpf, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int index = 0; index < length; index++) {
            sum += Character.getNumericValue(cpf.charAt(index)) * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
