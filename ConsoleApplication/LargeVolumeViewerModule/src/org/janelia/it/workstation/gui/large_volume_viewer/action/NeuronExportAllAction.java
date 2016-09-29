package org.janelia.it.workstation.gui.large_volume_viewer.action;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationManager;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.AnnotationModel;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport;
import org.janelia.it.workstation.gui.large_volume_viewer.annotation.SwcExport.ExportParameters;
import org.janelia.it.workstation.gui.large_volume_viewer.top_component.LargeVolumeViewerTopComponent;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;

@ActionID(
        category = "Large Volume Viewer",
        id = "NeuronExportAllAction"
)
@ActionRegistration(
        displayName = "Export all neurons to SWC file",
        lazy = true
)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "O-E")
})
public class NeuronExportAllAction extends AbstractAction {

    public NeuronExportAllAction() {
        super("Export SWC file...");
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        AnnotationManager annotationMgr = LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr();
        AnnotationModel annotationModel = annotationMgr.getAnnotationModel();
        SwcExport export = new SwcExport();
        ExportParameters params = export.getExportParameters(annotationModel.getCurrentWorkspace().getName());
        if ( params != null ) {
            annotationMgr.exportAllNeuronsAsSWC(params.getSelectedFile(), params.getDownsampleModulo());
        }
    }
}
