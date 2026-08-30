import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

public class AtClientRunner {

    private static final Charset PROCESS_OUTPUT_CHARSET = resolveNativeCharset();

    private static final Pattern UPDATE_VERSION_PATTERN =
            Pattern.compile("nova vers[aã]o\\s+([0-9]+(?:\\.[0-9]+)+-[0-9]+)", Pattern.CASE_INSENSITIVE);
    private static final String UPDATE_PROMPT_MARKER = "Indique o caminho completo para guardar o novo jar";

    private volatile Process currentProcess;

    public static List<String> buildCommand(
            String jarPath,
            String nif,
            String password,
            String year,
            String month,
            String operation,
            String inputPath,
            boolean testMode
    ) {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPath);

        command.add("-n");
        command.add(nif);

        command.add("-p");
        command.add(password);

        command.add("-a");
        command.add(year);

        command.add("-m");
        command.add(month);

        command.add("-op");
        command.add(operation);

        command.add("-i");
        command.add(inputPath);

        if (testMode) {
            command.add("-t");
        }

        return command;
    }

    public static List<String> maskPasswordForDisplay(List<String> command) {
        List<String> masked = new ArrayList<>(command);
        for (int i = 0; i < masked.size() - 1; i++) {
            if ("-p".equals(masked.get(i))) {
                masked.set(i + 1, "********");
            }
        }
        return masked;
    }

    public int runProcess(
            List<String> command,
            String initialJarPath,
            Consumer<String> logSink,
            Consumer<String> updatedJarPathCallback
    ) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        currentProcess = pb.start();
        logSink.accept("Processo iniciado...");

        Thread stdoutThread = new Thread(() -> handleStdout(currentProcess, initialJarPath, logSink, updatedJarPathCallback));
        Thread stderrThread = new Thread(() -> streamToLog(currentProcess.getErrorStream(), "ERR", logSink));

        stdoutThread.start();
        stderrThread.start();

        int exitCode = currentProcess.waitFor();
        stdoutThread.join();
        stderrThread.join();

        currentProcess = null;
        return exitCode;
    }

    public void stopProcess(Consumer<String> logSink) {
        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
            logSink.accept("Pedido de paragem enviado ao processo.");
        }
    }

    private void streamToLog(InputStream stream, String prefix, Consumer<String> sink) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, PROCESS_OUTPUT_CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.accept("[" + prefix + "] " + line);
            }
        } catch (IOException ex) {
            sink.accept("[" + prefix + "] Erro de leitura: " + ex.getMessage());
        }
    }

    private void handleStdout(
            Process process,
            String currentJarPath,
            Consumer<String> sink,
            Consumer<String> updatedJarPathCallback
    ) {
        AtomicReference<String> pendingVersion = new AtomicReference<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), PROCESS_OUTPUT_CHARSET))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.accept("[OUT] " + line);

                Matcher versionMatcher = UPDATE_VERSION_PATTERN.matcher(line);
                if (versionMatcher.find()) {
                    pendingVersion.set(versionMatcher.group(1));
                    sink.accept("[INFO] O cliente de linha de comandos esta desatualizado. Nova versao detetada: "
                            + pendingVersion.get() + ". A preparar transferencia automatica...");
                }

                if (line.contains(UPDATE_PROMPT_MARKER)) {
                    respondToUpdatePrompt(process, currentJarPath, pendingVersion.get(), sink, updatedJarPathCallback);
                }
            }
        } catch (IOException ex) {
            sink.accept("[OUT] Erro de leitura: " + ex.getMessage());
        }
    }

    private void respondToUpdatePrompt(
            Process process,
            String currentJarPath,
            String version,
            Consumer<String> sink,
            Consumer<String> updatedJarPathCallback
    ) {
        Path targetDir = resolveUpdateJarDirectory(currentJarPath);
        try {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), PROCESS_OUTPUT_CHARSET), true);
            writer.println(targetDir.toAbsolutePath());
            writer.flush();
            String fileName = version != null
                    ? "FACTEMICLI-" + version + "-cmdClient.jar"
                    : "FACTEMICLI-cmdClient-atualizado.jar";
            Path expectedJar = targetDir.resolve(fileName);
            sink.accept("[INFO] A transferir nova versao do cliente para a pasta: " + targetDir.toAbsolutePath());
            SwingUtilities.invokeLater(() -> updatedJarPathCallback.accept(expectedJar.toAbsolutePath().toString()));
        } catch (Exception ex) {
            sink.accept("[ERRO] Nao foi possivel responder automaticamente ao pedido de atualizacao do JAR: " + ex.getMessage());
            sink.accept("[ERRO] Atualize manualmente o cliente de linha de comandos (JAR) e volte a executar.");
        }
    }

    private Path resolveUpdateJarDirectory(String currentJarPath) {
        Path currentJar = Path.of(currentJarPath);
        Path dir = currentJar.getParent();
        if (dir == null || !Files.isWritable(dir)) {
            dir = Path.of(System.getProperty("java.io.tmpdir"));
        }
        return dir;
    }

    private static Charset resolveNativeCharset() {
        String nativeEncoding = System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding"));
        if (nativeEncoding != null) {
            try {
                return Charset.forName(nativeEncoding);
            } catch (Exception ex) {
                // fall through
            }
        }
        return Charset.defaultCharset();
    }
}
