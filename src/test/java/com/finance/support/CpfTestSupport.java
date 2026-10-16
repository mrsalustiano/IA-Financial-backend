package com.finance.support;

import java.util.Random;

/** Gera CPFs válidos (dígitos verificadores corretos) para os testes. */
public final class CpfTestSupport {

    private static final Random RANDOM = new Random();

    private CpfTestSupport() {
    }

    public static String randomValidCpf() {
        int[] digits = new int[9];
        for (int i = 0; i < 9; i++) {
            digits[i] = RANDOM.nextInt(10);
        }
        int d1 = checkDigit(digits, 9);
        int d2 = checkDigit(append(digits, d1), 10);
        StringBuilder builder = new StringBuilder();
        for (int digit : digits) {
            builder.append(digit);
        }
        return builder.append(d1).append(d2).toString();
    }

    private static int[] append(int[] source, int value) {
        int[] result = new int[source.length + 1];
        System.arraycopy(source, 0, result, 0, source.length);
        result[source.length] = value;
        return result;
    }

    private static int checkDigit(int[] digits, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
