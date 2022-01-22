package org.janelia.workstation.browser.gui.components;

import com.google.common.eventbus.Subscribe;
import org.janelia.model.domain.DomainObject;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.gui.cdmip.ColorDepthImage;
import org.janelia.model.domain.gui.cdmip.ColorDepthMatch;
import org.janelia.model.domain.sample.LSMImage;
import org.janelia.model.domain.sample.NeuronFragment;
import org.janelia.model.domain.sample.Sample;
import org.janelia.workstation.common.gui.util.UIUtils;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.selection.DomainObjectSelectionEvent;
import org.janelia.workstation.core.model.DomainObjectMapper;
import org.janelia.workstation.core.model.MappingType;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Manages the life cycle of domain viewers based on user generated selected events. This manager
 * either reuses existing viewers, or creates them as needed and docks them in the appropriate place.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DomainViewerManager implements ViewerManager<DomainViewerTopComponent>  {

    private final static Logger log = LoggerFactory.getLogger(DomainViewerManager.class);
    
    public static DomainViewerManager instance;
    
    private DomainViewerManager() {
    }
    
    public static DomainViewerManager getInstance() {
        if (instance==null) {
            instance = new DomainViewerManager();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    /* Manage the active instance of this top component */
    
    private DomainViewerTopComponent activeInstance;
    @Override
    public void activate(DomainViewerTopComponent instance) {
        activeInstance = instance;
    }
    @Override
    public boolean isActive(DomainViewerTopComponent instance) {
        return activeInstance == instance;
    }
    @Override
    public DomainViewerTopComponent getActiveViewer() {
        return activeInstance;
    }

    @Override
    public String getViewerName() {
        return "DomainViewerTopComponent";
    }
    
    @Override
    public Class<DomainViewerTopComponent> getViewerClass() {
        return DomainViewerTopComponent.class;
    }

    @Subscribe
    public void domainObjectsSelected(DomainObjectSelectionEvent event) {

        // We only care about single selections
        DomainObject domainObject = event.getObjectIfSingle();
        if (domainObject==null) {
            return;
        }
        
        // We only care about selection events
        if (!event.isSelect()) {
            log.debug("Event is not selection: {}",event);
            return;
        }

        // We only care about events generated by a domain list viewer
        if (!UIUtils.hasAncestorWithType((Component)event.getSource(),DomainListViewTopComponent.class)) {
            log.trace("Event source is not a list view: {}",event);
            return;
        }

        log.info("domainObjectSelected({})",Reference.createFor(domainObject));
        
        DomainViewerTopComponent viewer = DomainViewerManager.getInstance().getActiveViewer();
        if (viewer!=null) {
            // If we are reacting to a selection event in another viewer, then this load is not user driven.
            viewer.loadDomainObject(domainObject, false);
        }
        else {
            log.debug("No active viewer available");
        }
    }

    // TODO: This is the wrong abstraction and it won't work with the new color depth module.
    //       We need to change the behavior so that the color depth components generate a sample selection event
    //       that is processed generically above.
//    @Subscribe
//    public void colorDepthMatchSelected(ColorDepthMatchSelectionEvent event) {
//
//        // We only care about single selections
//        ColorDepthMatch match = event.getObjectIfSingle();
//        if (match==null) {
//            return;
//        }
//
//        // We only care about selection events
//        if (!event.isSelect()) {
//            log.debug("Event is not selection: {}",event);
//            return;
//        }
//
//        // We only care about events generated by a search result
//        if (!UIUtils.hasAncestorWithType((Component)event.getSource(),ColorDepthSearchEditorPanel.class)) {
//            log.trace("Event source is not a color depth search editor: {}",event);
//            return;
//        }
//
//        log.info("colorDepthMatchSelected({})",match);
//        if (match.getImageRef()==null) return;
//
//        SimpleWorker worker = new SimpleWorker() {
//
//            private Sample sample;
//
//            @Override
//            protected void doStuff() throws Exception {
//                ColorDepthImage image = DomainMgr.getDomainMgr().getModel().getDomainObject(match.getImageRef());
//                if (image != null) {
//                    sample = DomainMgr.getDomainMgr().getModel().getDomainObject(image.getSampleRef());
//                }
//            }
//
//            @Override
//            protected void hadSuccess() {
//                if (sample != null) {
//                    DomainViewerTopComponent viewer = DomainViewerManager.getInstance().getActiveViewer();
//                    if (viewer!=null) {
//                        // If we are reacting to a selection event in another viewer, then this load is not user driven.
//                        viewer.loadDomainObject(sample, false);
//                    }
//                }
//            }
//
//            @Override
//            protected void hadError(Throwable error) {
//                FrameworkAccess.handleExceptionQuietly(error);
//            }
//        };
//
//        worker.execute();
//    }

    public static MappingType getMappingTypeToLoad(Class<? extends DomainObject> clazz) {
        try {
            if (Sample.class.isAssignableFrom(clazz)) {
                return MappingType.Sample;
            }
            else if (NeuronFragment.class.isAssignableFrom(clazz)) {
                return MappingType.Sample;
            }
            else if (LSMImage.class.isAssignableFrom(clazz)) {
                return MappingType.Sample;
            }
            else if (ColorDepthMatch.class.isAssignableFrom(clazz)) {
                return MappingType.Sample;
            }
            else if (ColorDepthImage.class.isAssignableFrom(clazz)) {
                return MappingType.Sample;
            }
            else {
                return null;
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
            return null;
        }
    }

    /**
     * Get the object that should be loaded when the the given object is double clicked.
     * TODO: this method does database lookups, so it should always be called in a background thread
     * @param domainObject
     * @return
     */
    public static DomainObject getObjectToLoad(DomainObject domainObject) {
        MappingType mappingTypeToLoad = getMappingTypeToLoad(domainObject.getClass());
        DomainObjectMapper mapper = new DomainObjectMapper(Collections.singletonList(domainObject));
        try {
            List<DomainObject> mapped = mapper.map(mappingTypeToLoad);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped.get(0);
            }
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
        return null;
    }
    
}
