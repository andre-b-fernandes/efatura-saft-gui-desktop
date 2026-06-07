import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EFaturaGui extends JFrame {
    private double uiScale = 1.0;

    private final JTextField jarPathField = new JTextField();
    private final JTextField nifField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField yearField = new JTextField();
    private final JTextField monthField = new JTextField();
    private final JComboBox<String> operationCombo = new JComboBox<>(new String[]{"validar", "enviar"});
    private final JComboBox<String> scaleCombo = new JComboBox<>(new String[]{"95%", "100%", "110%"});
    private final JTextField inputPathField = new JTextField();
    private final JCheckBox testModeCheck = new JCheckBox("Modo de testes (-t)");

    private final JTextArea logArea = new JTextArea();
    private final JButton runButton = new JButton("Executar");
    private final JButton stopButton = new JButton("Parar");

    private volatile Process currentProcess;

    public EFaturaGui() {
        setTitle("e-Fatura SAF-T Wrapper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        buildUi();
        preloadDefaults();
        attachActions();
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("e-Fatura SAF-T Wrapper");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.add(title, BorderLayout.WEST);

        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scalePanel.add(new JLabel("Escala"));
        scalePanel.add(scaleCombo);
        header.add(scalePanel, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;

        addLabeledField(form, c, "JAR", jarPathField, this::chooseJarFile);
        addLabeledField(form, c, "NIF", nifField, null);
        addLabeledField(form, c, "Password", passwordField, null);
        addLabeledField(form, c, "Ano (YYYY)", yearField, null);
        addLabeledField(form, c, "Mes (MM)", monthField, null);
        addLabeledCombo(form, c, "Operacao", operationCombo);
        addLabeledField(form, c, "Ficheiro SAF-T", inputPathField, this::chooseInputFile);

        c.gridx = 1;
        c.gridy++;
        c.gridwidth = 2;
        form.add(testModeCheck, c);
        c.gridwidth = 1;

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(runButton);
        stopButton.setEnabled(false);
        controls.add(stopButton);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(form, BorderLayout.CENTER);
        top.add(controls, BorderLayout.SOUTH);

        JScrollPane topScroll = new JScrollPane(top);
        topScroll.setBorder(BorderFactory.createEmptyBorder());
        topScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        topScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        topScroll.getVerticalScrollBar().setUnitIncrement(18);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, logScroll);
        splitPane.setResizeWeight(0.58);
        splitPane.setContinuousLayout(true);

        root.add(header, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);

        setContentPane(root);
        applyUiScale();
    }

    private void addLabeledField(JPanel panel, GridBagConstraints c, String label, JTextField field, Runnable browseAction) {
        c.gridx = 0;
        c.weightx = 0;
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(textLabel, c);

        c.gridx = 1;
        c.weightx = 1;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 30));
        panel.add(field, c);

        c.gridx = 2;
        c.weightx = 0;
        if (browseAction != null) {
            JButton button = new JButton("Escolher");
            button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            button.addActionListener(e -> browseAction.run());
            panel.add(button, c);
        } else {
            panel.add(new JLabel(""), c);
        }

        c.gridy++;
    }

    private void addLabeledCombo(JPanel panel, GridBagConstraints c, String label, JComboBox<String> combo) {
        c.gridx = 0;
        c.weightx = 0;
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(textLabel, c);

        c.gridx = 1;
        c.weightx = 1;
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        combo.setPreferredSize(new Dimension(combo.getPreferredSize().width, 30));
        panel.add(combo, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(new JLabel(""), c);

        c.gridy++;
    }

    private void preloadDefaults() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path defaultJar = cwd.resolve("FACTEMICLI-2.9.1-100067-cmdClient.jar");
        jarPathField.setText(defaultJar.toAbsolutePath().toString());

        Path defaultXml = cwd.resolve("F_D05_515105422_SAFT_20260501_20260531.xml");
        if (Files.exists(defaultXml)) {
            inputPathField.setText(defaultXml.toAbsolutePath().toString());
        }

        LocalDate now = LocalDate.now();
        yearField.setText(String.valueOf(now.getYear()));
        monthField.setText(String.format("%02d", now.getMonthValue()));
    }

    private void attachActions() {
        runButton.addActionListener(e -> runJar());
        stopButton.addActionListener(e -> stopJar());
        scaleCombo.addActionListener(e -> {
            int idx = scaleCombo.getSelectedIndex();
            if (idx == 0) {
                uiScale = 0.95;
            } else if (idx == 2) {
                uiScale = 1.10;
            } else {
                uiScale = 1.0;
            }
            applyUiScale();
        });
    }

    private void chooseJarFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar JAR");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar ficheiro SAF-T XML");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runJar() {
        ValidationResult validation = validateInputs();
        if (!validation.ok) {
            JOptionPane.showMessageDialog(this, validation.message, "Validacao", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clearLog();
        List<String> command = buildCommand();
        appendLog("Comando: " + String.join(" ", maskPasswordForDisplay(command)));

        runButton.setEnabled(false);
        stopButton.setEnabled(true);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(false);

                try {
                    currentProcess = pb.start();
                    publish("Processo iniciado...");

                    Thread stdoutThread = new Thread(() -> streamToLog(currentProcess.getInputStream(), "OUT", this::publishSafe));
                    Thread stderrThread = new Thread(() -> streamToLog(currentProcess.getErrorStream(), "ERR", this::publishSafe));

                    stdoutThread.start();
                    stderrThread.start();

                    int exitCode = currentProcess.waitFor();
                    stdoutThread.join();
                    stderrThread.join();

                    publish("Processo terminado com exit code: " + exitCode);
                } catch (Exception ex) {
                    publish("Erro ao executar JAR: " + ex.getMessage());
                } finally {
                    currentProcess = null;
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String line : chunks) {
                    appendLog(line);
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
            }

            private void publishSafe(String text) {
                publish(text);
            }
        };

        worker.execute();
    }

    private void stopJar() {
        Process process = currentProcess;
        if (process != null && process.isAlive()) {
            process.destroy();
            appendLog("Pedido de paragem enviado ao processo.");
        }
    }

    private ValidationResult validateInputs() {
        String jar = jarPathField.getText().trim();
        if (jar.isEmpty() || !Files.exists(Path.of(jar))) {
            return ValidationResult.fail("Indique um caminho valido para o JAR.");
        }

        String nif = nifField.getText().trim();
        if (nif.isEmpty()) {
            return ValidationResult.fail("NIF e obrigatorio.");
        }

        String password = String.valueOf(passwordField.getPassword());
        if (password.isBlank()) {
            return ValidationResult.fail("Password e obrigatoria.");
        }

        String year = yearField.getText().trim();
        if (!year.matches("\\d{4}")) {
            return ValidationResult.fail("Ano invalido. Use YYYY.");
        }

        String month = monthField.getText().trim();
        if (!month.matches("\\d{2}")) {
            return ValidationResult.fail("Mes invalido. Use MM.");
        }
        int monthNumber = Integer.parseInt(month);
        if (monthNumber < 1 || monthNumber > 12) {
            return ValidationResult.fail("Mes invalido. Deve estar entre 01 e 12.");
        }

        String input = inputPathField.getText().trim();
        if (input.isEmpty() || !Files.exists(Path.of(input))) {
            return ValidationResult.fail("Indique um ficheiro SAF-T XML valido.");
        }

        return ValidationResult.ok();
    }

    private List<String> buildCommand() {
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-jar");
        command.add(jarPathField.getText().trim());

        command.add("-n");
        command.add(nifField.getText().trim());

        command.add("-p");
        command.add(String.valueOf(passwordField.getPassword()));

        command.add("-a");
        command.add(yearField.getText().trim());

        command.add("-m");
        command.add(monthField.getText().trim());

        command.add("-op");
        command.add(String.valueOf(operationCombo.getSelectedItem()));

        command.add("-i");
        command.add(inputPathField.getText().trim());

        if (testModeCheck.isSelected()) {
            command.add("-t");
        }

        return command;
    }

    private List<String> maskPasswordForDisplay(List<String> command) {
        List<String> masked = new ArrayList<>(command);
        for (int i = 0; i < masked.size() - 1; i++) {
            if ("-p".equals(masked.get(i))) {
                masked.set(i + 1, "********");
            }
        }
        return masked;
    }

    private void streamToLog(InputStream stream, String prefix, java.util.function.Consumer<String> sink) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sink.accept("[" + prefix + "] " + line);
            }
        } catch (IOException ex) {
            sink.accept("[" + prefix + "] Erro de leitura: " + ex.getMessage());
        }
    }

    private void appendLog(String text) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(text + System.lineSeparator());
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void clearLog() {
        logArea.setText("");
    }

    private void applyUiScale() {
        applyScaleRecursively(getContentPane());
        revalidate();
        repaint();
    }

    private void applyScaleRecursively(Component component) {
        if (component instanceof JLabel) {
            JLabel label = (JLabel) component;
            Font font = getBaseFont(label);
            if (font != null) {
                label.setFont(font.deriveFont((float) scaled(font.getSize2D())));
            }
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;
            Font font = getBaseFont(button);
            if (font != null) {
                button.setFont(font.deriveFont((float) scaled(font.getSize2D())));
            }
            Dimension preferred = button.getPreferredSize();
            button.setPreferredSize(new Dimension(preferred.width, Math.max(scaled(28), preferred.height)));
        } else if (component instanceof JTextField) {
            JTextField field = (JTextField) component;
            Font font = getBaseFont(field);
            if (font != null) {
                field.setFont(font.deriveFont((float) scaled(font.getSize2D())));
            }
            Dimension preferred = field.getPreferredSize();
            field.setPreferredSize(new Dimension(preferred.width, scaled(30)));
        } else if (component instanceof JComboBox) {
            JComboBox<?> combo = (JComboBox<?>) component;
            Font font = getBaseFont(combo);
            if (font != null) {
                combo.setFont(font.deriveFont((float) scaled(font.getSize2D())));
            }
            Dimension preferred = combo.getPreferredSize();
            combo.setPreferredSize(new Dimension(preferred.width, scaled(30)));
        } else if (component instanceof JCheckBox) {
            JCheckBox checkBox = (JCheckBox) component;
            Font font = getBaseFont(checkBox);
            if (font != null) {
                checkBox.setFont(font.deriveFont((float) scaled(font.getSize2D())));
            }
        } else if (component instanceof JTextArea) {
            JTextArea textArea = (JTextArea) component;
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, scaled(13)));
        }

        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                applyScaleRecursively(child);
            }
        }
    }

    private int scaled(double value) {
        return Math.max(10, (int) Math.round(value * uiScale));
    }

    private Font getBaseFont(JComponent component) {
        Object base = component.getClientProperty("baseFont");
        if (base instanceof Font) {
            return (Font) base;
        }

        Font current = component.getFont();
        component.putClientProperty("baseFont", current);
        return current;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EFaturaGui gui = new EFaturaGui();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }

    private static class ValidationResult {
        final boolean ok;
        final String message;

        private ValidationResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }
}
