package org.janelia.it.workstation.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.*;

import net.miginfocom.swing.MigLayout;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;

/**
 * A dialog for entering username and password, with some additional options.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoginDialog extends ModalDialog {

    private final JPanel mainPanel;
    private final JLabel usernameLabel;
    private final JLabel passwordLabel;
    private final JLabel emailLabel;
    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JTextField emailField;
    private final JCheckBox rememberCheckbox;
    private final JButton cancelButton;
    private final JButton okButton;
    
    public LoginDialog() {

        setTitle("Login");
        
        mainPanel = new JPanel(new MigLayout("wrap 2"));
        
        usernameLabel = new JLabel("User Name");
        usernameField = new JTextField(20);
        
        passwordLabel = new JLabel("Password");
        passwordField = new JPasswordField(20);
        
        emailLabel = new JLabel("Email");
        emailField = new JTextField(20);
        
        rememberCheckbox = new JCheckBox("Remember Password");
        rememberCheckbox.setSelected(true);
        
        mainPanel.add(emailLabel);
        mainPanel.add(emailField);
        mainPanel.add(usernameLabel);
        mainPanel.add(usernameField);
        mainPanel.add(passwordLabel);
        mainPanel.add(passwordField);
        mainPanel.add(rememberCheckbox, "span 2");

        cancelButton = new JButton("Cancel");
        cancelButton.setToolTipText("Close without saving changes");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        
        okButton = new JButton("OK");
        okButton.setToolTipText("Close and save changes");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveAndClose();
            }
        });
                
        getRootPane().setDefaultButton(okButton);
                
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(cancelButton);
        buttonPane.add(okButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.SOUTH);
        
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentShown(ComponentEvent e) {
                if (StringUtils.isEmpty(emailField.getText())) {
                    emailField.requestFocus();
                }
                else if (StringUtils.isEmpty(usernameField.getText())) {
                    usernameField.requestFocus();
                }
                else if (passwordField.getPassword().length==0) {
                    passwordField.requestFocus();
                }
                else {
                    okButton.requestFocus();
                }
            }
        });
    }

    public void showDialog() {
        
        String email = (String) getModelProperty(SessionMgr.USER_EMAIL, "");
        String username = (String) getModelProperty(SessionMgr.USER_NAME, "");
        String password = (String) getModelProperty(SessionMgr.USER_PASSWORD, "");
        Boolean remember = (Boolean) getModelProperty(SessionMgr.REMEMBER_PASSWORD, Boolean.TRUE);
        
        emailField.setText(email);
        usernameField.setText(username);
        passwordField.setText(password);
        rememberCheckbox.setSelected(remember);
        
        packAndShow();
    }

    private void saveAndClose() {

        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        sessionMgr.setModelProperty(SessionMgr.USER_EMAIL, email);
        sessionMgr.setModelProperty(SessionMgr.USER_NAME, username);
        sessionMgr.setModelProperty(SessionMgr.USER_PASSWORD, rememberCheckbox.isSelected()?password:null);
        sessionMgr.setModelProperty(SessionMgr.REMEMBER_PASSWORD, rememberCheckbox.isSelected());
        
        if (StringUtils.isEmpty(email)) {
            Object[] options = { "Fix Email", "Exit Program" };
            final int answer = JOptionPane.showOptionDialog(null, 
                    "Please correct your email information.", "Email Invalid",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer != 0) {
                SessionMgr.getSessionMgr().systemExit();
            }
            else {
                return;
            }
        }
        
        boolean loginSuccess = SessionMgr.getSessionMgr().loginSubject(username, password);
        
        if (!loginSuccess) {
            Object[] options = { "Fix Login", "Exit Program" };
            final int answer = JOptionPane.showOptionDialog(null, 
                    "Please correct your login information.", "Login Information Invalid",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (answer != 0) {
                SessionMgr.getSessionMgr().systemExit();
            }
        }
        else {
            setVisible(false);
        }
    }
    
    private Object getModelProperty(String key, Object defaultValue) {
        final SessionMgr sessionMgr = SessionMgr.getSessionMgr();
        Object value = sessionMgr.getModelProperty(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}