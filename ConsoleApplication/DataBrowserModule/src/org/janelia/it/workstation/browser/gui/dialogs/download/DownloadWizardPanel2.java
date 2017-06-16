package org.janelia.it.workstation.browser.gui.dialogs.download;

import javax.swing.event.ChangeListener;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.activity_logging.ActivityLogHelper;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public class DownloadWizardPanel2 implements WizardDescriptor.Panel<WizardDescriptor> {

    /**
     * The visual component that displays this panel. If you need to access the
     * component from this class, just use getComponent().
     */
    private DownloadVisualPanel2 component;

    @Override
    public DownloadVisualPanel2 getComponent() {
        if (component == null) {
            component = new DownloadVisualPanel2();
        }
        return component;
    }

    @Override
    public HelpCtx getHelp() {
        // Show no Help button for this panel:
        return HelpCtx.DEFAULT_HELP;
        // If you have context help:
        // return new HelpCtx("help.key.here");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addChangeListener(ChangeListener l) {
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    }

    @Override
    public void readSettings(WizardDescriptor wiz) {
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        getComponent().init(state);
    }

    @Override
    public void storeSettings(WizardDescriptor wiz) {
        ActivityLogHelper.logUserAction("DownloadWizard.storeSettings", 2);
        DownloadWizardState state = (DownloadWizardState)wiz.getProperty(DownloadWizardIterator.PROP_WIZARD_STATE);
        state.setSplitChannels(getComponent().isSplitChannels());
        state.setOutputExtensions(getComponent().getOutputExtensions());
        // Updated serialized state
        FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "outputExtensions", state.getOutputExtensionString());
        FrameworkImplProvider.setLocalPreferenceValue(DownloadWizardState.class, "splitChannels", state.isSplitChannels());
    }

}
