package ui;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.function.Consumer;

public final class LSRDisplay {
    private final JFrame frame;
    private final LSRDisplayForm form;
    private final JFileChooser chooser;

    public LSRDisplay(String title) {
        FlatIntelliJLaf.setup();
        frame = new JFrame(title);

        form = new LSRDisplayForm();
        frame.setContentPane(form.contentPanel);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        chooser = new JFileChooser(System.getProperty("user.dir"));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setFileFilter(new FileNameExtensionFilter("LSR Files", "lsr"));
    }

    public LSRDisplay() {
        this("LSR Display");
    }

    public void showNodeAddInfo(final String message, final NodeInfo infoType) {
        final JTextField textField = switch (infoType) {
            case NODE_ADD -> form.nodeAddField;
            case NODE_DELETE -> form.nodeDelField;
            case NODE_LINK_BROKEN -> form.nodeBrokenField;
        };

        textField.setText(message);
    }

    public void showFileContent(final String fileContent) {
        form.fileTextArea.setText(fileContent);
    }

    public void updateStatus(final String message) {
        form.statusTextArea
                .insert(message.endsWith("\n") ? message : message + '\n', 0);
    }

    public void clearStatus() {
        form.statusTextArea.setText(null);
    }

    private void addEvent(final JButton component, final Runnable callback) {
        component.addActionListener(_ -> callback.run());
    }

    public void onSingleStep(final Runnable callback) {
        addEvent(form.singleStepButton, callback);
    }

    public void onComputeAll(final Runnable callback) {
        addEvent(form.computeAllButton, callback);
    }

    public void onReset(final Runnable callback) {
        addEvent(form.resetButton, callback);
    }

    public void onSelectFile(final Consumer<File> callback) {
        form.loadFileButton.addActionListener(_ -> {

            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = chooser.getSelectedFile();
                callback.accept(selectedFile);
            }
        });
    }

    public void onSelectSource(final Consumer<String> callback) {
        form.sourceComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                callback.accept((String)e.getItem());
            }
        });
    }

    public void addSourceOptions(final String[] options) {
        for (final String option : options) {
            form.sourceComboBox.addItem(option);
        }
    }
}

