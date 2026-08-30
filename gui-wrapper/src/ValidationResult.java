public class ValidationResult {
    public final boolean ok;
    public final String message;

    private ValidationResult(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, "");
    }

    public static ValidationResult fail(String message) {
        return new ValidationResult(false, message);
    }
}
