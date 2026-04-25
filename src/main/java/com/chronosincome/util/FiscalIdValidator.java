package com.chronosincome.util;

import com.chronosincome.exception.BusinessException;

public class FiscalIdValidator {

    private FiscalIdValidator() {}

    public static void validate(String fiscalId) {
        if (fiscalId == null || fiscalId.isBlank()) {
            throw new BusinessException("CNPJ ou EIN é obrigatório");
        }

        String digits = fiscalId.replaceAll("[^0-9]", "");

        if (digits.length() == 14) {
            validateCnpj(digits);
        } else if (digits.length() == 9) {
            validateEin(digits);
        } else {
            throw new BusinessException("Formato de CNPJ ou EIN inválido");
        }
    }

    // Validação de CNPJ com dígitos verificadores
    private static void validateCnpj(String digits) {
        if (digits.chars().distinct().count() == 1) {
            throw new BusinessException("CNPJ inválido");
        }

        int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < 12; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        }
        int first = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        sum = 0;
        for (int i = 0; i < 13; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        }
        int second = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        boolean valid = first == Character.getNumericValue(digits.charAt(12))
                && second == Character.getNumericValue(digits.charAt(13));

        if (!valid) {
            throw new BusinessException("CNPJ inválido");
        }
    }

    // Validação de EIN — formato XX-XXXXXXX (9 dígitos)
    private static void validateEin(String digits) {
        // EIN não possui dígito verificador, mas validamos prefixos inválidos
        String prefix = digits.substring(0, 2);
        // Prefixos reservados pelo IRS que não são atribuídos a empresas
        if (prefix.equals("00") || prefix.equals("07") || prefix.equals("08")
                || prefix.equals("09") || prefix.equals("17") || prefix.equals("18")
                || prefix.equals("19") || prefix.equals("28") || prefix.equals("29")
                || prefix.equals("49") || prefix.equals("69") || prefix.equals("70")
                || prefix.equals("78") || prefix.equals("79") || prefix.equals("89")) {
            throw new BusinessException("EIN inválido");
        }
    }
}
