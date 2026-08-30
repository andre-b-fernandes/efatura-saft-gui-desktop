import java.util.List;

public record RunSummary(
        boolean recognized,
        boolean success,
        String totalFaturas,
        String totalCreditos,
        String totalDebitos,
        List<String> errors
) {
}
