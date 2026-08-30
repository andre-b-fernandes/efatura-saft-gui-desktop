import java.nio.file.Files;
import java.nio.file.Path;

public class InputValidator {

    public static ValidationResult validate(
            String jarPath,
            String nif,
            String password,
            String year,
            String month,
            String inputPath
    ) {
        if (!isJarPathValid(jarPath)) {
            return ValidationResult.fail("Indique um caminho valido para o JAR.");
        }

        if (!isNifValid(nif)) {
            return ValidationResult.fail("NIF e obrigatorio.");
        }

        if (!isPasswordValid(password)) {
            return ValidationResult.fail("Password e obrigatoria.");
        }

        if (!isYearValid(year)) {
            return ValidationResult.fail("Ano invalido. Use YYYY.");
        }

        if (!month.matches("\\d{2}")) {
            return ValidationResult.fail("Mes invalido. Use MM.");
        }
        if (!isMonthValid(month)) {
            return ValidationResult.fail("Mes invalido. Deve estar entre 01 e 12.");
        }

        if (!isInputPathValid(inputPath)) {
            return ValidationResult.fail("Indique um ficheiro SAF-T XML valido.");
        }

        return ValidationResult.ok();
    }

    public static boolean isJarPathValid(String jarPath) {
        return !jarPath.isEmpty() && Files.exists(Path.of(jarPath));
    }

    public static boolean isNifValid(String nif) {
        return !nif.isEmpty();
    }

    public static boolean isPasswordValid(String password) {
        return !password.isBlank();
    }

    public static boolean isYearValid(String year) {
        return year.matches("\\d{4}");
    }

    public static boolean isMonthValid(String month) {
        if (!month.matches("\\d{2}")) {
            return false;
        }
        int monthNumber = Integer.parseInt(month);
        return monthNumber >= 1 && monthNumber <= 12;
    }

    public static boolean isInputPathValid(String inputPath) {
        return !inputPath.isEmpty() && Files.exists(Path.of(inputPath));
    }
}

