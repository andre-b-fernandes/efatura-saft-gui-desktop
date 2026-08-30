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
        if (jarPath.isEmpty() || !Files.exists(Path.of(jarPath))) {
            return ValidationResult.fail("Indique um caminho valido para o JAR.");
        }

        if (nif.isEmpty()) {
            return ValidationResult.fail("NIF e obrigatorio.");
        }

        if (password.isBlank()) {
            return ValidationResult.fail("Password e obrigatoria.");
        }

        if (!year.matches("\\d{4}")) {
            return ValidationResult.fail("Ano invalido. Use YYYY.");
        }

        if (!month.matches("\\d{2}")) {
            return ValidationResult.fail("Mes invalido. Use MM.");
        }
        int monthNumber = Integer.parseInt(month);
        if (monthNumber < 1 || monthNumber > 12) {
            return ValidationResult.fail("Mes invalido. Deve estar entre 01 e 12.");
        }

        if (inputPath.isEmpty() || !Files.exists(Path.of(inputPath))) {
            return ValidationResult.fail("Indique um ficheiro SAF-T XML valido.");
        }

        return ValidationResult.ok();
    }
}
