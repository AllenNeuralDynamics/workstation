package org.janelia.workstation.core.actions;

import java.util.Collection;

import org.janelia.workstation.core.events.selection.ChildSelectionModel;
import org.janelia.workstation.core.model.ImageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Current viewer context which can be used to construct context menus.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ViewerContext<T,S> {

    private static final Logger log = LoggerFactory.getLogger(ViewerContext.class);

    private ChildSelectionModel<T,S> selectionModel;
    private ChildSelectionModel<T,S> editSelectionModel;
    private ImageModel<T,S> imageModel;

    public ViewerContext(ChildSelectionModel<T,S> selectionModel,
                         ChildSelectionModel<T,S> editSelectionModel,
                         ImageModel<T,S> imageModel) {
        this.selectionModel = selectionModel;
        this.editSelectionModel = editSelectionModel;
        this.imageModel = imageModel;
    }

    public Object getContextObject() {
        return selectionModel.getParentObject();
    }

    public ChildSelectionModel<T,S> getSelectionModel() {
        return selectionModel;
    }

    public ChildSelectionModel<T,S> getEditSelectionModel() {
        return editSelectionModel;
    }

    public ImageModel<T,S> getImageModel() {
        return imageModel;
    }

    public boolean isMultiple() {
        return selectionModel.getSelectedIds().size() > 1;
    }

    public T getLastSelectedObject() {
        return getImageModel().getImageByUniqueId(selectionModel.getLastSelectedId());
    }

    public Collection<T> getSelectedObjects() {
        return selectionModel.getObjects();
    }
}
