package org.janelia.workstation.controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.janelia.console.viewerapi.dialogs.NeuronGroupsDialog;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmNeuronTagMap;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.workstation.controller.access.ProjectInitFacade;
import org.janelia.workstation.controller.access.ProjectInitFacadeImpl;
import org.janelia.workstation.controller.access.RefreshHandler;
import org.janelia.workstation.controller.eventbus.*;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.workstation.controller.model.TmModelManager;
import org.janelia.workstation.controller.access.TiledMicroscopeDomainMgr;
import org.janelia.workstation.controller.tileimagery.URLBasedTileLoader;
import org.janelia.workstation.core.api.ClientDomainUtils;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.api.web.JadeServiceClient;
import org.janelia.workstation.core.options.ApplicationOptions;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.*;

public class TmViewerManager implements GlobalViewerController {
    private final Logger log = LoggerFactory.getLogger(TmViewerManager.class);
    private static final TmViewerManager instance = new TmViewerManager();
    private TmModelManager modelManager = new TmModelManager();
    private TiledMicroscopeDomainMgr tmDomainMgr;
    private NeuronManager neuronManager;
    private ProjectInitFacade projectInit;
    private DomainObject currProject;

    public enum ToolSet {
        NEURON
    }

    public static TmViewerManager getInstance() {
        return instance;
    }

    public TmViewerManager() {
        this.tmDomainMgr = TiledMicroscopeDomainMgr.getDomainMgr();
        setNeuronManager(new NeuronManager(modelManager));
        EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SAMPLEWORKSPACE).register(this);
    }

    public NeuronManager getNeuronManager() {
        return neuronManager;
    }

    public void setNeuronManager(NeuronManager neuronManager) {
        this.neuronManager = neuronManager;
    }


    /**
     *  method for going through the full sequence of loading a TmWorkspace or TmSample.
     *  It's async since we'd like the UI to be responsive while loading goes on.
     *  The order of initialization for a regular workstation instance is to the following;
     *  1. Clear Viewers
     *  2. Load Annotation Data
     *  3. Load Tile Imagery Data/Stack Information
     *  4. Notify Viewers
     *
      */
    public void loadProject(DomainObject project) {
        projectInit = new ProjectInitFacadeImpl(project);
        projectInit.clearViewers();
        currProject = project;
        if (currProject instanceof TmWorkspace) {
            projectInit.loadAnnotationData((TmWorkspace)currProject);
        } else {
            loadImagery((TmSample)currProject);
        }
    }

    // once the data has been loaded
    @Subscribe
    public void dataLoadComplete(LoadEvent dataEvent) {
        if (dataEvent.getType()!= LoadEvent.Type.METADATA_COMPLETE)
            return;
        try {
            loadImagery(dataEvent.getSample());
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
    }

    private void loadImagery(TmSample sample) {
        projectInit.loadImagery(sample);
    }


    public boolean editsAllowed() {
        if (modelManager.getCurrentWorkspace()==null) return false;
        return ClientDomainUtils.hasWriteAccess(modelManager.getCurrentWorkspace());
    }

    public void loadUserPreferences() throws Exception {
        TmNeuronTagMap currentTagMap = modelManager.getCurrentTagMap();
        if (modelManager.getCurrentSample()==null || modelManager.getCurrentSample().getId()==null) return;
        Map<String,Map<String,Object>> tagGroupMappings = FrameworkAccess.getRemotePreferenceValue(DomainConstants.PREFERENCE_CATEGORY_MOUSELIGHT, modelManager.getCurrentSample().getId().toString(), null);
        if (tagGroupMappings!=null && currentTagMap!=null) {
            currentTagMap.saveTagGroupMappings(tagGroupMappings);
            //   if (neuronSetAdapter!=null && neuronSetAdapter.getMetaWorkspace()!=null) {
            //     neuronSetAdapter.getMetaWorkspace().setTagMetadata(currentTagMap);
            //   }

            // set toggled group properties on load-up
            Iterator<String> groupTags = tagGroupMappings.keySet().iterator();
            while (groupTags.hasNext()) {
                String groupKey = groupTags.next();
                Set<TmNeuronMetadata> neurons = getNeuronManager().getNeuronsForTag(groupKey);
                List<TmNeuronMetadata> neuronList = new ArrayList<TmNeuronMetadata>(neurons);
                Map<String,Object> groupMapping = currentTagMap.geTagGroupMapping(groupKey);
                if (groupMapping!=null && groupMapping.get("toggled")!=null && ((Boolean)groupMapping.get("toggled"))) {
                    String property = (String)groupMapping.get("toggleprop");
                    // these two prop changes ought to be in annmodel, not annmgr, and annmgr should call into model;
                    //  fixed for visiblity, but not for others yet
                    if (property.equals(NeuronGroupsDialog.PROPERTY_RADIUS)) {
                        //LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronUserToggleRadius(neuronList, true);
                    } else if (property.equals(NeuronGroupsDialog.PROPERTY_VISIBILITY)) {
                        //setNeuronVisibility(neuronList, false);
                    } else if (property.equals(NeuronGroupsDialog.PROPERTY_READONLY)) {
                        //LargeVolumeViewerTopComponent.getInstance().getAnnotationMgr().setNeuronNonInteractable(neuronList, true);
                    }
                }
            }
        }
    }

    @Subscribe
    public void loadComplete(LoadEvent event) {
        if (event.getType()!= LoadEvent.Type.PROJECT_COMPLETE)
            return;
        final TmWorkspace workspace = modelManager.getCurrentWorkspace();
        if (workspace==null) {
            // this is a sample
        }
        // Update TC, in case the load bypassed it
        // LargeVolumeViewerTopComponent.getInstance().setCurrent(workspace==null ? getCurrentSample() : workspace);
        //SwingUtilities.invokeLater(() -> fireWorkspaceLoaded(workspace));
        // load user preferences
        try {
            loadUserPreferences();
            // register with Message Server to receive async updates
            RefreshHandler.getInstance().ifPresent(rh -> rh.setAnnotationModel(getNeuronManager()));
            //TaskWorkflowViewTopComponent.getInstance().loadHistory();
        } catch (Exception error) {
            FrameworkAccess.handleException(error);
        }
        SwingUtilities.invokeLater(() -> {
            EventBus selectBus = EventBusRegistry.getInstance().getEventRegistry(EventBusRegistry.EventBusType.SELECTION);
            SelectionEvent evt = new SelectionEvent(SelectionEvent.Type.CLEAR);
            evt.setCategory(AnnotationCategory.NEURON);
            selectBus.post(evt);
        });
        if (workspace!=null) {
            ////activityLog.logLoadWorkspace(workspace.getId());
        }

    }
}
