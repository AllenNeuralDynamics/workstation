package org.janelia.it.workstation.api.entity_model.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks the state of entity selections in a set of different viewers. All entities are referred to by a String,
 * since the identifier could be either an entityId, or a uniqueId (tree path), or maybe something else in the future.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntitySelectionModel {

    private static final Logger log = LoggerFactory.getLogger(EntitySelectionModel.class);
    
    public static final String CATEGORY_OUTLINE = "outline";
    public static final String CATEGORY_MAIN_VIEW = "mainViewer";
    public static final String CATEGORY_SEC_VIEW = "secViewer";
    public static final String CATEGORY_CROSS_VIEW = "crossViewer";
    public static final String CATEGORY_ONTOLOGY = "ontology";
    public static final String CATEGORY_ALIGNMENT_BOARD_VIEW = "alignmentBoardViewer"; //LLF

    private final Map<String, List<String>> selectionModels = new HashMap<>();
    private final List<String> latestGlobalSelection = new ArrayList<>();
    private String activeCategory;
    
    public EntitySelectionModel() {
        selectionModels.put(CATEGORY_OUTLINE, new ArrayList<String>());
        selectionModels.put(CATEGORY_MAIN_VIEW, new ArrayList<String>());
        selectionModels.put(CATEGORY_SEC_VIEW, new ArrayList<String>());
        selectionModels.put(CATEGORY_CROSS_VIEW, new ArrayList<String>());
        selectionModels.put(CATEGORY_ONTOLOGY, new ArrayList<String>());
        selectionModels.put(CATEGORY_ALIGNMENT_BOARD_VIEW, new ArrayList<String>());
    }

    private List<String> getCategory(String category) {
        List<String> selected = selectionModels.get(category);
        if (selected == null) {
            throw new IllegalArgumentException("Unknown selection category " + category);
        }
        return selected;
    }

    public String getActiveCategory() {
        return activeCategory;
    }
    
    public void deselectAll(String category) {
        List<String> selected = getCategory(category);
        selected.clear();
        if (selected.equals(latestGlobalSelection)) {
            latestGlobalSelection.clear();
        }
    }
    
    public boolean isSelected(String category, String identifier) {
        List<String> selected = getCategory(category);
        return selected.contains(identifier);
    }

    public void selectEntity(String category, String identifier, boolean clearAll) {
        if (!CATEGORY_OUTLINE.equals(category) && !CATEGORY_ONTOLOGY.equals(category)) {
            this.activeCategory = category;
        }
        List<String> selected = getCategory(category);
        if (clearAll) {
            selected.clear();
            latestGlobalSelection.clear();
        }
        if (selected.contains(identifier)) {
            return;
        }
        selected.add(identifier);
        latestGlobalSelection.add(identifier);
        ModelMgr.getModelMgr().notifyEntitySelected(category, identifier, clearAll);
    }

    public void deselectEntity(String category, String identifier) {
        List<String> selected = getCategory(category);
        if (!selected.contains(identifier)) {
            return;
        }
        selected.remove(identifier);
        latestGlobalSelection.remove(identifier);
        ModelMgr.getModelMgr().notifyEntityDeselected(category, identifier);
    }

    public List<String> getSelectedEntitiesIds(String category) {
        return getCategory(category);
    }
    
    public String getLastSelectedEntityIdByCategory(String category) {
        List<String> selected = getCategory(category);
        if (selected.isEmpty()) {
            return null;
        }
        return selected.get(selected.size() - 1);
    }

    public List<String> getLatestGlobalSelection() {
        if (null != latestGlobalSelection && latestGlobalSelection.size() > 0) {
            return latestGlobalSelection;
        }
        return null;
    }

}