package org.janelia.it.workstation.browser.nb_action;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;

import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.janelia.it.workstation.browser.components.StartPageTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

@ActionID(
        category = "Help",
        id = "StartPageMenuAction"
)
@ActionRegistration(
        displayName = "#CTL_StartPageMenuAction",
        lazy = true
)
@ActionReference(path = "Menu/Help", position = 140)
@Messages("CTL_StartPageMenuAction=Start Page")
public final class StartPageMenuAction extends AbstractAction {
    
    @Override
    public void actionPerformed(ActionEvent e) {
        ActivityLogHelper.logUserAction("StartPageMenuAction.actionPerformed");
        StartPageTopComponent topComp = null;
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        for (TopComponent tc: tcs) {
            if (tc instanceof StartPageTopComponent) {                
                topComp = (StartPageTopComponent) tc;               
                break;
            }
        }
        if(topComp == null){            
            topComp = StartPageTopComponent.findComp();
        }
       
        if (topComp!=null) {
            topComp.openAtTabPosition(0);
            topComp.requestActive();
            // This runs later to avoid NPE from Swing EventQueue
            final StartPageTopComponent tc = topComp;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    tc.requestFocusInWindow();
                }
            });
        }
    }
}
