import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AtResponseParser {

    private static final Pattern RESPONSE_PATTERN =
            Pattern.compile("<response code=\"(-?\\d+)\">(.*?)</response>", Pattern.DOTALL);
    private static final Pattern ERROR_PATTERN =
            Pattern.compile("<error>(.*?)</error>", Pattern.DOTALL);
    private static final Pattern TOTAL_FATURAS_PATTERN =
            Pattern.compile("<totalFaturas>(.*?)</totalFaturas>", Pattern.DOTALL);
    private static final Pattern TOTAL_CREDITOS_PATTERN =
            Pattern.compile("<totalCreditos>(.*?)</totalCreditos>", Pattern.DOTALL);
    private static final Pattern TOTAL_DEBITOS_PATTERN =
            Pattern.compile("<totalDebitos>(.*?)</totalDebitos>", Pattern.DOTALL);

    public static RunSummary parseSummary(String fullLog) {
        Matcher responseMatcher = RESPONSE_PATTERN.matcher(fullLog);
        // Keep the last match: a jar auto-update retry re-runs the command, and only the final attempt matters.
        String code = null;
        String body = null;
        while (responseMatcher.find()) {
            code = responseMatcher.group(1);
            body = responseMatcher.group(2);
        }
        if (code == null) {
            return new RunSummary(false, false, null, null, null, List.of());
        }

        boolean success = "200".equals(code);

        List<String> errors = new ArrayList<>();
        Matcher errorMatcher = ERROR_PATTERN.matcher(body);
        while (errorMatcher.find()) {
            errors.add(errorMatcher.group(1).trim());
        }

        return new RunSummary(
                true,
                success,
                extractFirst(TOTAL_FATURAS_PATTERN, body),
                extractFirst(TOTAL_CREDITOS_PATTERN, body),
                extractFirst(TOTAL_DEBITOS_PATTERN, body),
                errors
        );
    }

    private static String extractFirst(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
