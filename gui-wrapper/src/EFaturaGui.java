import java.awt.*;
import java.nio.file.Path;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

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

    private final AtClientRunner atClientRunner = new AtClientRunner();

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

        Path detectedJar = JarLocator.findDefaultJarFile();
        Path defaultJar = detectedJar != null ? detectedJar : cwd.resolve("FACTEMICLI-2.9.1-100067-cmdClient.jar");
        jarPathField.setText(defaultJar.toAbsolutePath().toString());

        Path defaultXml = JarLocator.findDefaultXmlFile();
        if (defaultXml != null) {
            inputPathField.setText(defaultXml.toAbsolutePath().toString());
        }

        yearField.setText(JarLocator.getDefaultYear());
        monthField.setText(JarLocator.getDefaultMonth());
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
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros JAR (*.jar)", "jar"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar ficheiro SAF-T XML");
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros XML (*.xml)", "xml"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void runJar() {
        ValidationResult validation = InputValidator.validate(
                jarPathField.getText().trim(),
                nifField.getText().trim(),
                String.valueOf(passwordField.getPassword()),
                yearField.getText().trim(),
                monthField.getText().trim(),
                inputPathField.getText().trim()
        );

        if (!validation.ok) {
            JOptionPane.showMessageDialog(this, validation.message, "Validacao", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clearLog();
        List<String> command = AtClientRunner.buildCommand(
                jarPathField.getText().trim(),
                nifField.getText().trim(),
                String.valueOf(passwordField.getPassword()),
                yearField.getText().trim(),
                monthField.getText().trim(),
                String.valueOf(operationCombo.getSelectedItem()),
                inputPathField.getText().trim(),
                testModeCheck.isSelected()
        );
        String initialJarPath = jarPathField.getText().trim();
        appendLog("Comando: " + String.join(" ", AtClientRunner.maskPasswordForDisplay(command)));

        runButton.setEnabled(false);
        stopButton.setEnabled(true);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    int exitCode = atClientRunner.runProcess(
                            command,
                            initialJarPath,
                            this::publishSafe,
                            newPath -> jarPathField.setText(newPath)
                    );
                    publish("Processo terminado com exit code: " + exitCode);
                } catch (Exception ex) {
                    publish("Erro ao executar JAR: " + ex.getMessage());
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
        atClientRunner.stopProcess(this::appendLog);
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
        UiScaler.applyScale(getContentPane(), uiScale);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EFaturaGui gui = new EFaturaGui();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}
