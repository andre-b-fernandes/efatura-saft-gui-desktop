import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class UiScaler {

    public static void applyScale(Container container, double uiScale) {
        applyScaleRecursively(container, uiScale);
    }

    private static void applyScaleRecursively(Component component, double uiScale) {
        if (component instanceof JLabel label) {
            Font font = getBaseFont(label);
            if (font != null) {
                label.setFont(font.deriveFont((float) scaled(font.getSize2D(), uiScale)));
            }
        } else if (component instanceof JButton button) {
            Font font = getBaseFont(button);
            if (font != null) {
                button.setFont(font.deriveFont((float) scaled(font.getSize2D(), uiScale)));
            }
            Dimension preferred = button.getPreferredSize();
            button.setPreferredSize(new Dimension(preferred.width, Math.max(scaled(28, uiScale), preferred.height)));
        } else if (component instanceof JTextField field) {
            Font font = getBaseFont(field);
            if (font != null) {
                field.setFont(font.deriveFont((float) scaled(font.getSize2D(), uiScale)));
            }
            Dimension preferred = field.getPreferredSize();
            field.setPreferredSize(new Dimension(preferred.width, scaled(30, uiScale)));
        } else if (component instanceof JComboBox<?> combo) {
            Font font = getBaseFont(combo);
            if (font != null) {
                combo.setFont(font.deriveFont((float) scaled(font.getSize2D(), uiScale)));
            }
            Dimension preferred = combo.getPreferredSize();
            combo.setPreferredSize(new Dimension(preferred.width, scaled(30, uiScale)));
        } else if (component instanceof JCheckBox checkBox) {
            Font font = getBaseFont(checkBox);
            if (font != null) {
                checkBox.setFont(font.deriveFont((float) scaled(font.getSize2D(), uiScale)));
            }
        } else if (component instanceof JTextArea textArea) {
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, scaled(13, uiScale)));
        }

        if (component instanceof Container c) {
            for (Component child : c.getComponents()) {
                applyScaleRecursively(child, uiScale);
            }
        }
    }

    private static int scaled(double value, double uiScale) {
        return Math.max(10, (int) Math.round(value * uiScale));
    }

    private static Font getBaseFont(JComponent component) {
        Object base = component.getClientProperty("baseFont");
        if (base instanceof Font) {
            return (Font) base;
        }

        Font current = component.getFont();
        component.putClientProperty("baseFont", current);
        return current;
    }
}
