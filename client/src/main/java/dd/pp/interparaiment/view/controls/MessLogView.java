package dd.pp.interparaiment.view.controls;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;

public class MessLogView extends JPanel {
    private final ConsoleView consoleView;

    public MessLogView(final Project project) {
        super(new BorderLayout());

        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);

        this.consoleView = builder.getConsole();

        add(this.consoleView.getComponent(), BorderLayout.CENTER);
    }

    public void logInfo(final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logInfo(message));
            return;
        }

        printWithLabel("INFO", message, ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    public void logStringWithGreenLabel(final String label, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logStringWithGreenLabel(label, message));
            return;
        }

        printWithLabel(label, message, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }

    public void logStringWithBlueLabel(final String label, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logStringWithBlueLabel(label, message));
            return;
        }

        printWithLabel(label, message, ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
    }

    public void logStringWithRedLabel(final String label, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logStringWithRedLabel(label, message));
            return;
        }

        printWithLabel(label, message, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public void logEmpty() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::logEmpty);
            return;
        }

        this.consoleView.print(System.lineSeparator(), ConsoleViewContentType.SYSTEM_OUTPUT);
    }

    public void logStringWithYellowLabel(final String label, final String message) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logStringWithYellowLabel(label, message));
            return;
        }

        printWithLabel(label, message, ConsoleViewContentType.LOG_INFO_OUTPUT);
    }

    public void logHyperlinkWithGreenLabel(final String label, final String message, final HyperlinkInfo hyperlink) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logHyperlinkWithGreenLabel(label, message, hyperlink));
            return;
        }

        printWithLabel(label, message, hyperlink, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }

    public void logHyperlinkWithBlueLabel(final String label, final String message, final HyperlinkInfo hyperlink) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logHyperlinkWithBlueLabel(label, message, hyperlink));
            return;
        }

        printWithLabel(label, message, hyperlink, ConsoleViewContentType.LOG_VERBOSE_OUTPUT);
    }

    public void logHyperlinkWithRedLabel(final String label, final String message, final HyperlinkInfo hyperlink) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logHyperlinkWithRedLabel(label, message, hyperlink));
            return;
        }

        printWithLabel(label, message, hyperlink, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }

    public void logHyperlinkWithYellowLabel(final String label, final String message, final HyperlinkInfo hyperlink) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> logHyperlinkWithYellowLabel(label, message, hyperlink));
            return;
        }

        printWithLabel(label, message, hyperlink, ConsoleViewContentType.LOG_INFO_OUTPUT);
    }



    public void clear() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::clear);
            return;
        }
        this.consoleView.clear();
    }

    private void printWithLabel(final String label, final String message, final ConsoleViewContentType viewContentType) {
        this.consoleView.print("[", ConsoleViewContentType.SYSTEM_OUTPUT);
        this.consoleView.print(label, viewContentType);
        this.consoleView.print("] --- ", ConsoleViewContentType.SYSTEM_OUTPUT);

        this.consoleView.print(message, ConsoleViewContentType.SYSTEM_OUTPUT);
        this.consoleView.print(System.lineSeparator(), ConsoleViewContentType.NORMAL_OUTPUT);
    }

    private void printWithLabel(final String label, final String message, final HyperlinkInfo hyperlink, final ConsoleViewContentType viewContentType) {
        this.consoleView.print("[", ConsoleViewContentType.SYSTEM_OUTPUT);
        this.consoleView.print(label, viewContentType);
        this.consoleView.print("] --- ", ConsoleViewContentType.SYSTEM_OUTPUT);

        this.consoleView.printHyperlink(message, hyperlink);
        this.consoleView.print(System.lineSeparator(), ConsoleViewContentType.NORMAL_OUTPUT);
    }

    //    ConsoleViewContentType.LOG_DEBUG_OUTPUT      light green
    //    ConsoleViewContentType.LOG_VERBOSE_OUTPUT    blue
    //    ConsoleViewContentType.LOG_WARNING_OUTPUT    white
    //    ConsoleViewContentType.LOG_ERROR_OUTPUT      red
    //    ConsoleViewContentType.SYSTEM_OUTPUT         white
    //    ConsoleViewContentType.LOG_INFO_OUTPUT       yellow
}
