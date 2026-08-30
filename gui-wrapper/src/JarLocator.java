import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JarLocator {

    public static Path findDefaultJarFile() {
        List<Path> searchDirs = new ArrayList<>();
        Path appDir = resolveAppDirectory();
        if (appDir != null) {
            searchDirs.add(appDir);
            if (appDir.getParent() != null) {
                searchDirs.add(appDir.getParent());
            }
        }
        searchDirs.add(Path.of(System.getProperty("user.dir")));

        for (Path dir : searchDirs) {
            Path candidate = findJarByPrefix(dir, "FACTEMICLI");
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private static Path resolveAppDirectory() {
        try {
            Path codeLocation = Path.of(EFaturaGui.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(codeLocation) ? codeLocation.getParent() : codeLocation;
        } catch (Exception ex) {
            return null;
        }
    }

    private static Path findJarByPrefix(Path dir, String prefix) {
        if (dir == null || !Files.isDirectory(dir)) {
            return null;
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ex) {
            return null;
        }
    }

    public static Path findDefaultXmlFile() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path defaultXml = cwd.resolve("F_D05_515105422_SAFT_20260501_20260531.xml");
        return Files.exists(defaultXml) ? defaultXml : null;
    }

    public static String getDefaultYear() {
        return String.valueOf(LocalDate.now().getYear());
    }

    public static String getDefaultMonth() {
        return String.format("%02d", LocalDate.now().getMonthValue());
    }
}
