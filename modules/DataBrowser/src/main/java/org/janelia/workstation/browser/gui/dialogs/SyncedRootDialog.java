package org.janelia.workstation.browser.gui.dialogs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.janelia.model.domain.files.DiscoveryAgentType;
import org.janelia.model.domain.files.SyncedRoot;
import org.janelia.workstation.common.gui.dialogs.ModalDialog;
import org.janelia.workstation.common.gui.support.GroupedKeyValuePanel;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.web.AsyncServiceClient;
import org.janelia.workstation.core.workers.AsyncServiceMonitoringWorker;
import org.janelia.workstation.core.workers.BackgroundWorker;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CancellationException;

public class SyncedRootDialog extends ModalDialog {

    private JTextField pathTextField;
    private JTextField nameField;
    private JTextField depthField;
    private SyncedRoot syncedRoot;
    private HashMap<DiscoveryAgentType, JCheckBox> agentTypeMap = new LinkedHashMap<>();


    public SyncedRootDialog() {

        setLayout(new BorderLayout());

        GroupedKeyValuePanel attrPanel = new GroupedKeyValuePanel();

        attrPanel.addSeparator("Synchronized Folder");

        JLabel instructions = new JLabel(
                "<html><font color='#959595' size='-1'>" +
                        "Specify a path to search, in Linux path style e.g. /misc/public<br>" +
                        "</font></html>");

        attrPanel.addItem(instructions);

        this.pathTextField = new JTextField(50);
        pathTextField.setToolTipText("The filepath must be accessible to the backend JADE service");
        attrPanel.addItem("Path", pathTextField);

        this.nameField = new JTextField(50);
        nameField.setToolTipText("Name of the Synchronized Folder in the Workstation. If blank, the filepath will be used.");
        attrPanel.addItem("Name (optional)", nameField);

        this.depthField = new JTextField(20);
        depthField.setToolTipText("Depth of folders to traverse when discovering files");
        depthField.setText("2");
        attrPanel.addItem("Depth", depthField);

        final JPanel agentPanel = new JPanel();
        agentPanel.setLayout(new BoxLayout(agentPanel, BoxLayout.PAGE_AXIS));

        for (DiscoveryAgentType value : DiscoveryAgentType.values()) {
            JCheckBox checkbox = new JCheckBox(value.getLabel());
            agentPanel.add(checkbox);
            agentTypeMap.put(value, checkbox);
        }

        attrPanel.addItem("Discover", agentPanel);

        add(attrPanel, BorderLayout.CENTER);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close dialog without doing anything");
        cancelButton.addActionListener(e -> setVisible(false));

        JButton okButton = new JButton("Save and Synchronize");
        okButton.setToolTipText("Close dialog and begin data synchronization");
        okButton.addActionListener(e -> saveAndClose());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    public void showDialog() {
        showDialog(null);
    }

    public void showDialog(SyncedRoot syncedRoot) {

        this.syncedRoot = syncedRoot;

        if (syncedRoot != null) {
            setTitle("Edit Synchronized Folder");
            pathTextField.setText(syncedRoot.getFilepath());
            nameField.setText(syncedRoot.getName());
            depthField.setText(syncedRoot.getDepth()+"");

            for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
                JCheckBox checkBox = agentTypeMap.get(agentType);
                checkBox.setSelected(syncedRoot.getDiscoveryAgents().contains(agentType));
            }
        }
        else {
            setTitle("Add Synchronized Folder");
        }

        // Show dialog and wait
        packAndShow();
    }

    private void saveAndClose() {

        if (syncedRoot == null) {
            syncedRoot = new SyncedRoot();
        }

        syncedRoot.setFilepath(pathTextField.getText());

        if (StringUtils.isBlank(syncedRoot.getFilepath())) {
            JOptionPane.showMessageDialog(this, "You must enter a filepath to continue", "No filepath given", JOptionPane.ERROR_MESSAGE);
            return;
        }

        syncedRoot.setName(StringUtils.isBlank(nameField.getText()) ? syncedRoot.getFilepath() : nameField.getText());

        try {
            syncedRoot.setDepth(Integer.parseInt(depthField.getText()));
            if (syncedRoot.getDepth() < 1 || syncedRoot.getDepth() > 20) throw new NumberFormatException();
        }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Enter a number between 1 and 20 for depth", "Invalid depth", JOptionPane.ERROR_MESSAGE);
            return;
        }

        for (DiscoveryAgentType agentType : DiscoveryAgentType.values()) {
            JCheckBox checkBox = agentTypeMap.get(agentType);
            if (checkBox.isSelected()) {
                syncedRoot.getDiscoveryAgents().add(agentType);
            }
            else {
                syncedRoot.getDiscoveryAgents().remove(agentType);
            }
        }

        if (syncedRoot.getDiscoveryAgents().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select one or more discovery agents", "No agents selected", JOptionPane.ERROR_MESSAGE);
            return;
        }

        refreshSyncedRoot(syncedRoot);

        setVisible(false);
    }

    public static void refreshSyncedRoot(SyncedRoot root) {

        BackgroundWorker worker = new AsyncServiceMonitoringWorker() {

            private String taskDisplayName = "Synchronizing Folder "+root.getName();

            @Override
            public String getName() {
                return taskDisplayName;
            }

            @Override
            protected void doStuff() throws Exception {

                SyncedRoot savedRoot = DomainMgr.getDomainMgr().getModel().save(root);

                setStatus("Submitting task " + taskDisplayName);
                AsyncServiceClient asyncServiceClient = new AsyncServiceClient();
                ImmutableList.Builder<String> serviceArgsBuilder = ImmutableList.<String>builder()
                        .add("-syncedRootId", savedRoot.getId().toString());

                Long taskId = asyncServiceClient.invokeService("syncedRoot",
                        serviceArgsBuilder.build(),
                        null,
                        ImmutableMap.of()
                );

                setServiceId(taskId);

                // Wait until task is finished
                super.doStuff();

                if (isCancelled()) throw new CancellationException();
                setStatus("Done");
            }

        };
        worker.setSuccessCallback(() -> {
            SimpleWorker.runInBackground(() -> DomainMgr.getDomainMgr().getModel().invalidateAll());
            return null;
        });
        worker.executeWithEvents();
    }
}
