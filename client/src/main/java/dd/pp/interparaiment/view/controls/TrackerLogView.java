package dd.pp.interparaiment.view.controls;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;

public class TrackerLogView extends JPanel {
    private final JBTextArea area = new JBTextArea();

    public TrackerLogView() {
        super(new BorderLayout());

        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        add(new JBScrollPane(area), BorderLayout.CENTER);
    }

    public void appendLine(String line) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendLine(line));
            return;
        }

        area.append(line);
        area.append("\n");

        area.setCaretPosition(area.getDocument().getLength());
    }

    public void clear() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clear);
            return;
        }
        area.setText("");
    }
}
