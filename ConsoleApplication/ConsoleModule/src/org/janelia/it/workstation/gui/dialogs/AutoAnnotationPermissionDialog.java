package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;

import org.janelia.it.workstation.gui.framework.access.Accessibility;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.janelia.it.jacs.model.user_data.Subject;
import org.janelia.it.jacs.model.util.PermissionTemplate;
import org.janelia.it.jacs.shared.utils.EntityUtils;
import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.util.SubjectComboBoxRenderer;

/**
 * A dialog for selecting permissions to auto-add for any annotations
 * made while the auto-annotation is active.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AutoAnnotationPermissionDialog extends ModalDialog implements Accessibility {

    public static String AUTO_SHARE_TEMPLATE = "OntologyOutline.AutoShareTemplate";
    
    private static final Font separatorFont = new Font("Sans Serif", Font.BOLD, 12);
    
    private final JPanel attrPanel;
    private final JComboBox subjectCombobox;
    private final JCheckBox readCheckbox;
    private final JCheckBox writeCheckbox;
    
    private PermissionTemplate template;
   
    private boolean pressedOk;
    
    public AutoAnnotationPermissionDialog() {

        setTitle("Auto-share new annotations");

        attrPanel = new JPanel(new MigLayout("wrap 2, ins 20"));
        add(attrPanel, BorderLayout.CENTER);

        addSeparator(attrPanel, "User");

        subjectCombobox = new JComboBox();
        subjectCombobox.setEditable(false);
        subjectCombobox.setToolTipText("Choose a user or group");

        SubjectComboBoxRenderer renderer = new SubjectComboBoxRenderer();
        subjectCombobox.setRenderer(renderer);
        subjectCombobox.setMaximumRowCount(20);

        attrPanel.add(subjectCombobox, "gap para, span 2");

        addSeparator(attrPanel, "Permissions");

        readCheckbox = new JCheckBox("Read");
        readCheckbox.setEnabled(false);
        readCheckbox.setSelected(true);
        attrPanel.add(readCheckbox, "gap para, span 2");

        writeCheckbox = new JCheckBox("Write");
        attrPanel.add(writeCheckbox, "gap para, span 2");

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelAndClose();
            }
        });

        JButton okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pressedOk = true;
                saveAndClose();
            }
        });

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(buttonPane, BorderLayout.SOUTH);
    }

    private void addSeparator(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setFont(separatorFont);
        panel.add(label, "split 2, span, gaptop 10lp");
        panel.add(new JSeparator(SwingConstants.HORIZONTAL), "growx, wrap, gaptop 10lp");
    }

    public boolean showAutoAnnotationConfiguration() {
        pressedOk = false;
            
        try {
            template = (PermissionTemplate)SessionMgr.getSessionMgr().getModelProperty(AUTO_SHARE_TEMPLATE);

            List<Subject> subjects = new ArrayList<>(ModelMgr.getModelMgr().getSubjects());
            EntityUtils.sortSubjects(subjects);
            
            DefaultComboBoxModel model = (DefaultComboBoxModel) subjectCombobox.getModel();
            model.removeAllElements();
            
            Subject currSubject = null;
            for (Subject subject : subjects) {
                model.addElement(subject);
                if (template!=null && template.getSubjectKey().equals(subject.getKey())) {
                    currSubject = subject;
                }
            }
            
            if (template!=null) {
                if (currSubject != null) {
                    model.setSelectedItem(currSubject);
                }

                String permissions = template.getPermissions();
                if (permissions!=null) {
                    readCheckbox.setSelected(permissions.contains("r"));
                    writeCheckbox.setSelected(permissions.contains("w"));
                }
            }
            
            packAndShow();
        }
        catch (Exception e) {
            SessionMgr.getSessionMgr().handleException(e);
        }
            
        return pressedOk;
    }

    private void cancelAndClose() {
        template = null;
        setVisible(false);
    }
    
    private void saveAndClose() {
        
        if (template==null) {
            template = new PermissionTemplate();
        }
        
        final Subject subject = (Subject) subjectCombobox.getSelectedItem();
        template.setSubjectKey(subject.getKey());
        
        final boolean read = readCheckbox.isSelected();
        final boolean write = writeCheckbox.isSelected();
        String permissions = (read ? "r" : "") + (write ? "w" : "");
        template.setPermissions(permissions);
        
        SessionMgr.getSessionMgr().setModelProperty(AUTO_SHARE_TEMPLATE, template);
        
        setVisible(false);
    }
    
    public PermissionTemplate getTemplate() {
        return template;
    }
    
    public String getSharingStatus() {
        StringBuilder sb = new StringBuilder();
        if (template==null) {
            sb.append("Not auto-sharing annotations");
        }
        else {
            sb.append("Auto-sharing new annotations with ");
            sb.append(EntityUtils.getNameFromSubjectKey(template.getSubjectKey()));
            sb.append(" with permissions '");
            sb.append(template.getPermissions()).append("'");
        }
        return sb.toString();
    }
    
    @Override
    public boolean isAccessible() {
        return true;
    }
}