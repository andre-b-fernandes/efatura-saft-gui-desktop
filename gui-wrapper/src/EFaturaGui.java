import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class EFaturaGui extends JFrame {

    private static final Color ACCENT = new Color(0x2563EB);
    private static final Color ACCENT_DARK = new Color(0x1D4ED8);
    private static final Color SUCCESS = new Color(0x16A34A);
    private static final Color ERROR = new Color(0xDC2626);
    private static final Color WARNING = new Color(0xD97706);
    private static final Color NEUTRAL_BG = new Color(0xF1F5F9);
    private static final Color NEUTRAL_TEXT = new Color(0x334155);

    private enum SummaryState { IDLE, RUNNING, SUCCESS, FAILURE }

    @FunctionalInterface
    private interface SimpleDocListener extends DocumentListener {
        void onChange();

        @Override
        default void insertUpdate(DocumentEvent e) { onChange(); }
        @Override
        default void removeUpdate(DocumentEvent e) { onChange(); }
        @Override
        default void changedUpdate(DocumentEvent e) { onChange(); }
    }

    private double uiScale = 1.0;

    private final JTextField jarPathField = new JTextField();
    private final JTextField nifField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JSpinner periodoSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.MONTH));
    private final JComboBox<String> operationCombo = new JComboBox<>(new String[]{"validar", "enviar"});
    private final JComboBox<String> scaleCombo = new JComboBox<>(new String[]{"95%", "100%", "110%"});
    private final JTextField inputPathField = new JTextField();
    private final JCheckBox testModeCheck = new JCheckBox("Modo de testes (-t)");

    private final JTextPane logArea = new JTextPane();
    private final JLabel summaryIconLabel = new JLabel();
    private final JLabel summaryTextLabel = new JLabel();
    private final JTextField faturasValueField = new JTextField();
    private final JTextField creditosValueField = new JTextField();
    private final JTextField debitosValueField = new JTextField();
    private final JPanel summaryCenterPanel = new JPanel(new CardLayout());
    private final JPanel summaryBanner = new JPanel(new BorderLayout(8, 0));
    private final JButton runButton = new JButton("Executar");
    private final JButton stopButton = new JButton("Parar");

    private final AtClientRunner atClientRunner = new AtClientRunner();
    private final StringBuilder currentRunLog = new StringBuilder();
    private JSplitPane splitPane;

    public EFaturaGui() {
        setTitle("e-Fatura SAF-T Wrapper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        buildUi();
        preloadDefaults();
        attachActions();
        updateFieldValidity();
        setSummaryState(SummaryState.IDLE, "Pronto para executar.");

        // Log starts collapsed; the form and the Executar/Parar controls stay fully visible.
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                int collapsedLogHeight = 90;
                splitPane.setDividerLocation(splitPane.getHeight() - collapsedLogHeight - splitPane.getDividerSize());
                removeComponentListener(this);
            }
        });
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(14, 14, 14, 14));
        root.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("e-Fatura SAF-T Wrapper");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(ACCENT_DARK);
        header.add(title, BorderLayout.WEST);

        JPanel scalePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        scalePanel.setOpaque(false);
        scalePanel.add(new JLabel("Escala"));
        scalePanel.add(scaleCombo);
        header.add(scalePanel, BorderLayout.EAST);

        JPanel ficheirosPanel = createGroupPanel("Ficheiros");
        GridBagConstraints fc = freshConstraints();
        addLabeledField(ficheirosPanel, fc, "JAR", jarPathField, this::chooseJarFile);
        addLabeledField(ficheirosPanel, fc, "Ficheiro SAF-T", inputPathField, this::chooseInputFile);

        JPanel authPanel = createGroupPanel("Autenticacao");
        GridBagConstraints ac = freshConstraints();
        addLabeledFieldPair(authPanel, ac, "NIF", nifField, "Password", passwordField);

        JPanel periodoPanel = createGroupPanel("Periodo & Operacao");
        GridBagConstraints pc = freshConstraints();
        addLabeledSpinner(periodoPanel, pc, "Periodo (Ano-Mes)", periodoSpinner);
        addLabeledCombo(periodoPanel, pc, "Operacao", operationCombo);
        pc.gridx = 1;
        pc.gridwidth = 2;
        periodoPanel.add(testModeCheck, pc);
        pc.gridwidth = 1;

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(ficheirosPanel);
        form.add(Box.createVerticalStrut(8));
        form.add(authPanel);
        form.add(Box.createVerticalStrut(8));
        form.add(periodoPanel);

        runButton.setOpaque(true);
        runButton.setContentAreaFilled(true);
        runButton.setBackground(ACCENT);
        runButton.setForeground(Color.WHITE);
        runButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        runButton.setFocusPainted(false);

        stopButton.setOpaque(true);
        stopButton.setContentAreaFilled(true);
        stopButton.setBackground(ERROR);
        stopButton.setForeground(Color.WHITE);
        stopButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stopButton.setFocusPainted(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(runButton);
        stopButton.setEnabled(false);
        controls.add(stopButton);

        JScrollPane topScroll = new JScrollPane(form);
        topScroll.setBorder(BorderFactory.createEmptyBorder());
        topScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        topScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        topScroll.getVerticalScrollBar().setUnitIncrement(18);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Logs"));

        summaryIconLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        summaryTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        summaryBanner.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel successCard = new JPanel(new GridLayout(1, 3, 12, 0));
        successCard.setOpaque(false);
        successCard.add(buildSuccessDetailColumn("Faturas", faturasValueField));
        successCard.add(buildSuccessDetailColumn("Creditos", creditosValueField));
        successCard.add(buildSuccessDetailColumn("Debitos", debitosValueField));

        summaryCenterPanel.setOpaque(false);
        summaryCenterPanel.add(summaryTextLabel, "text");
        summaryCenterPanel.add(successCard, "success");

        summaryBanner.add(summaryIconLabel, BorderLayout.WEST);
        summaryBanner.add(summaryCenterPanel, BorderLayout.CENTER);

        JPanel logPanel = new JPanel(new BorderLayout(0, 6));
        logPanel.add(summaryBanner, BorderLayout.NORTH);
        logPanel.add(logScroll, BorderLayout.CENTER);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScroll, logPanel);
        splitPane.setResizeWeight(1.0);
        splitPane.setContinuousLayout(true);

        root.add(header, BorderLayout.NORTH);
        root.add(splitPane, BorderLayout.CENTER);
        root.add(controls, BorderLayout.SOUTH);

        setContentPane(root);
        applyUiScale();
    }

    // Read-only, selectable field so the user can copy a single total out of the success banner.
    private JPanel buildSuccessDetailColumn(String title, JTextField valueField) {
        JPanel column = new JPanel(new BorderLayout(0, 2));
        column.setOpaque(false);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(NEUTRAL_TEXT);
        column.add(titleLabel, BorderLayout.NORTH);

        valueField.setEditable(false);
        valueField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        valueField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        valueField.setBackground(Color.WHITE);
        column.add(valueField, BorderLayout.CENTER);

        return column;
    }

    private JPanel createGroupPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        titledBorder.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
        titledBorder.setTitleColor(ACCENT_DARK);
        panel.setBorder(titledBorder);
        panel.setBackground(NEUTRAL_BG);
        return panel;
    }

    private GridBagConstraints freshConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        return c;
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

    private void addLabeledSpinner(JPanel panel, GridBagConstraints c, String label, JSpinner spinner) {
        c.gridx = 0;
        c.weightx = 0;
        JLabel textLabel = new JLabel(label);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(textLabel, c);

        c.gridx = 1;
        c.weightx = 1;
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM"));
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(spinner.getPreferredSize().width, 30));
        panel.add(spinner, c);

        c.gridx = 2;
        c.weightx = 0;
        panel.add(new JLabel(""), c);

        c.gridy++;
    }

    // Places two short fields (e.g. NIF/Password) side by side in the same row instead of stacking them.
    private void addLabeledFieldPair(
            JPanel panel, GridBagConstraints c,
            String leftLabel, JTextField leftField,
            String rightLabel, JTextField rightField
    ) {
        c.gridx = 0;
        c.weightx = 0;
        JLabel leftTextLabel = new JLabel(leftLabel);
        leftTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(leftTextLabel, c);

        c.gridx = 1;
        c.weightx = 1;
        leftField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        leftField.setPreferredSize(new Dimension(leftField.getPreferredSize().width, 30));
        panel.add(leftField, c);

        c.gridx = 2;
        c.weightx = 0;
        JLabel rightTextLabel = new JLabel(rightLabel);
        rightTextLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(rightTextLabel, c);

        c.gridx = 3;
        c.weightx = 1;
        rightField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rightField.setPreferredSize(new Dimension(rightField.getPreferredSize().width, 30));
        panel.add(rightField, c);

        c.gridy++;
    }

    private void preloadDefaults() {
        Path detectedJar = JarLocator.findDefaultJarFile();
        if (detectedJar != null) {
            jarPathField.setText(detectedJar.toAbsolutePath().toString());
        }

        Path defaultXml = JarLocator.findDefaultXmlFile();
        if (defaultXml != null) {
            inputPathField.setText(defaultXml.toAbsolutePath().toString());
        }

        periodoSpinner.setValue(new Date());
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

        SimpleDocListener validityListener = () -> updateFieldValidity();
        jarPathField.getDocument().addDocumentListener(validityListener);
        nifField.getDocument().addDocumentListener(validityListener);
        passwordField.getDocument().addDocumentListener(validityListener);
        inputPathField.getDocument().addDocumentListener(validityListener);
    }

    // Live-updates each required field's border so missing/invalid input is obvious without submitting.
    private void updateFieldValidity() {
        setFieldValidityBorder(jarPathField, InputValidator.isJarPathValid(jarPathField.getText().trim()));
        setFieldValidityBorder(nifField, InputValidator.isNifValid(nifField.getText().trim()));
        setFieldValidityBorder(passwordField, InputValidator.isPasswordValid(String.valueOf(passwordField.getPassword())));
        setFieldValidityBorder(inputPathField, InputValidator.isInputPathValid(inputPathField.getText().trim()));
    }

    private void setFieldValidityBorder(JTextField field, boolean valid) {
        Object base = field.getClientProperty("baseBorder");
        if (base == null) {
            base = field.getBorder();
            field.putClientProperty("baseBorder", base);
        }
        field.setBorder(valid ? (Border) base : new LineBorder(ERROR, 2));
    }

    private void chooseJarFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar JAR");
        chooser.setCurrentDirectory(resolveStartDirectory(jarPathField.getText().trim()).toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros JAR (*.jar)", "jar"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            jarPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void chooseInputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecionar ficheiro SAF-T XML");
        chooser.setCurrentDirectory(resolveStartDirectory(inputPathField.getText().trim()).toFile());
        chooser.setFileFilter(new FileNameExtensionFilter("Ficheiros XML (*.xml)", "xml"));
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            inputPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    // Prefers the existing field's parent folder, falling back to the directory the app was launched from.
    private Path resolveStartDirectory(String currentFieldValue) {
        if (!currentFieldValue.isEmpty()) {
            Path candidate = Path.of(currentFieldValue).toAbsolutePath();
            Path parent = Files.isDirectory(candidate) ? candidate : candidate.getParent();
            if (parent != null && Files.isDirectory(parent)) {
                return parent;
            }
        }
        return Path.of(System.getProperty("user.dir"));
    }

    private String getSelectedYear() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime((Date) periodoSpinner.getValue());
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    private String getSelectedMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime((Date) periodoSpinner.getValue());
        return String.format("%02d", calendar.get(Calendar.MONTH) + 1);
    }

    private void runJar() {
        String jarPath = jarPathField.getText().trim();
        String nif = nifField.getText().trim();
        String password = String.valueOf(passwordField.getPassword());
        String year = getSelectedYear();
        String month = getSelectedMonth();
        String operation = String.valueOf(operationCombo.getSelectedItem());
        String inputPath = inputPathField.getText().trim();
        boolean testMode = testModeCheck.isSelected();

        ValidationResult validation = InputValidator.validate(jarPath, nif, password, year, month, inputPath);

        if (!validation.ok) {
            JOptionPane.showMessageDialog(this, validation.message, "Validacao", JOptionPane.WARNING_MESSAGE);
            return;
        }

        clearLog();
        currentRunLog.setLength(0);
        setSummaryState(SummaryState.RUNNING, "A processar pedido...");
        List<String> command = AtClientRunner.buildCommand(jarPath, nif, password, year, month, operation, inputPath, testMode);
        appendLog("Comando: " + String.join(" ", AtClientRunner.maskPasswordForDisplay(command)));

        runButton.setEnabled(false);
        stopButton.setEnabled(true);

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<String> activeCommand = command;
                String activeJarPath = jarPath;
                int exitCode = -1;
                try {
                    // A stale JAR self-updates and exits without running the requested operation;
                    // retry once with the freshly downloaded JAR so the real validation result is shown.
                    for (int attempt = 1; attempt <= 2; attempt++) {
                        ProcessOutcome outcome = atClientRunner.runProcess(activeCommand, activeJarPath, this::publishSafe);
                        String newJarPath = outcome.updatedJarPath();
                        if (newJarPath != null && attempt < 2 && Files.exists(Path.of(newJarPath))) {
                            publish("[INFO] JAR atualizado com sucesso. A repetir o pedido com a nova versao...");
                            activeJarPath = newJarPath;
                            activeCommand = AtClientRunner.buildCommand(
                                    activeJarPath, nif, password, year, month, operation, inputPath, testMode);
                            String updatedPath = activeJarPath;
                            SwingUtilities.invokeLater(() -> jarPathField.setText(updatedPath));
                            continue;
                        }
                        exitCode = outcome.exitCode();
                        break;
                    }
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
                    currentRunLog.append(line).append(System.lineSeparator());
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                stopButton.setEnabled(false);
                applyRunSummary(AtResponseParser.parseSummary(currentRunLog.toString()));
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
            StyledDocument doc = logArea.getStyledDocument();
            SimpleAttributeSet attrs = new SimpleAttributeSet();
            StyleConstants.setForeground(attrs, classifyLogColor(text));
            StyleConstants.setBold(attrs, text.startsWith("Comando:"));
            try {
                doc.insertString(doc.getLength(), text + System.lineSeparator(), attrs);
            } catch (BadLocationException ex) {
                // Should not happen when always inserting at doc.getLength().
            }
            logArea.setCaretPosition(doc.getLength());
        });
    }

    private Color classifyLogColor(String text) {
        if (text.startsWith("Comando:")) {
            return ACCENT_DARK;
        }
        if (text.contains("[E] ") || text.contains("errorCause") || text.trim().startsWith("at ")) {
            return ERROR;
        }
        if (text.contains("HTTP Connection Response: 200")) {
            return SUCCESS;
        }
        return NEUTRAL_TEXT;
    }

    private void clearLog() {
        logArea.setText("");
    }

    // Reflects the parsed <response> outcome (or a generic failure) in the summary banner.
    private void applyRunSummary(RunSummary summary) {
        if (!summary.recognized()) {
            setSummaryState(SummaryState.IDLE, "Nao foi possivel interpretar o resultado. Consulte o log.");
            return;
        }
        if (summary.success()) {
            setSummarySuccessDetails(summary);
        } else {
            String firstError = summary.errors().isEmpty() ? "Consulte o log para mais detalhes." : summary.errors().get(0);
            String suffix = summary.errors().size() > 1 ? " (ver detalhes no log)" : "";
            setSummaryState(SummaryState.FAILURE, "Falha - " + firstError + suffix);
        }
    }

    private void setSummaryState(SummaryState state, String text) {
        applyBannerChrome(state);
        summaryTextLabel.setText("<html>" + htmlEscape(text) + "</html>");
        summaryTextLabel.setForeground(NEUTRAL_TEXT);
        ((CardLayout) summaryCenterPanel.getLayout()).show(summaryCenterPanel, "text");
    }

    // Shows totals as separate copyable fields instead of a single line, so a single value can be selected/copied.
    private void setSummarySuccessDetails(RunSummary summary) {
        applyBannerChrome(SummaryState.SUCCESS);
        faturasValueField.setText(summary.totalFaturas() != null ? summary.totalFaturas() : "-");
        creditosValueField.setText(summary.totalCreditos() != null ? summary.totalCreditos() : "-");
        debitosValueField.setText(summary.totalDebitos() != null ? summary.totalDebitos() : "-");
        faturasValueField.setCaretPosition(0);
        creditosValueField.setCaretPosition(0);
        debitosValueField.setCaretPosition(0);
        ((CardLayout) summaryCenterPanel.getLayout()).show(summaryCenterPanel, "success");
    }

    private void applyBannerChrome(SummaryState state) {
        Color background;
        String icon;
        switch (state) {
            case RUNNING -> {
                background = new Color(0xFEF3C7);
                icon = "...";
            }
            case SUCCESS -> {
                background = new Color(0xDCFCE7);
                icon = "OK";
            }
            case FAILURE -> {
                background = new Color(0xFEE2E2);
                icon = "X";
            }
            default -> {
                background = NEUTRAL_BG;
                icon = "i";
            }
        }
        summaryBanner.setBackground(background);
        summaryIconLabel.setText(icon);
        summaryIconLabel.setForeground(switch (state) {
            case SUCCESS -> SUCCESS;
            case FAILURE -> ERROR;
            case RUNNING -> WARNING;
            default -> NEUTRAL_TEXT;
        });
    }

    // Wraps in HTML so long messages (e.g. AT client errors) wrap to the window width instead of being truncated.
    private String htmlEscape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void applyUiScale() {
        UiScaler.applyScale(getContentPane(), uiScale);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ex) {
            // Fall back to the platform default L&F silently.
        }
        SwingUtilities.invokeLater(() -> {
            EFaturaGui gui = new EFaturaGui();
            gui.setLocationRelativeTo(null);
            gui.setVisible(true);
        });
    }
}
