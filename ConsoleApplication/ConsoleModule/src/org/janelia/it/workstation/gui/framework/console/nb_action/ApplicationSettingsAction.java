/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.janelia.it.workstation.gui.framework.console.nb_action;

import org.janelia.it.workstation.gui.framework.pref_controller.PrefController;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

@ActionID(
        category = "Edit/Preferences",
        id = "ApplicationSettingsAction"
)
@ActionRegistration(
        displayName = "#CTL_ApplicationSettingsAction"
)
@ActionReferences({
    @ActionReference(path = "Menu/Edit/Preferences", position = 100),
    @ActionReference(path = "Shortcuts", name = "M-F3")
})
@Messages("CTL_ApplicationSettingsAction=Application Settings...")
public final class ApplicationSettingsAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        new EditingActionDelegate()
                .establishPrefController(PrefController.APPLICATION_EDITOR);
    }
}