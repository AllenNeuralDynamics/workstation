package org.janelia.it.workstation.browser.gui.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.it.workstation.browser.gui.support.buttons.DropDownButton;

/**
 * A button which allows the user to select multiple values from a drop-down list. 
 * 
 * When using this abstract class, you need to provide an implementation which manages the state. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class SelectionButton<T> extends DropDownButton {
    
    private static final int MAX_VALUES_STRING_LENGTH = 20;
    
    private String label;
    private boolean isRadio;

    public SelectionButton(String label) {
        this(label, false);
    }
    
    public SelectionButton(String label, boolean radio) {
        this.label = label;
        this.isRadio = radio;
    }
    
    public void update() {
        updateText();
        populateFacetMenu();
    }

    private void updateText() {

        StringBuilder text = new StringBuilder();
        text.append(label);
        List<String> valueLabels = new ArrayList<>(getSelectedValueNames());
        if (!valueLabels.isEmpty()) {
            Collections.sort(valueLabels);
                text.append(" (");
                text.append(StringUtils.getCommaDelimited(valueLabels, MAX_VALUES_STRING_LENGTH));
                text.append(")");
        }
        
        setText(text.toString());
    }
    
    private void populateFacetMenu() {
        
        removeAll();

        Collection<T> values = getValues();
        if (values!=null) {
            
            Set<String> selectedValueNames = new HashSet<>(getSelectedValueNames());
    
            if (!isRadio && !selectedValueNames.isEmpty()) {
                final JMenuItem menuItem = new JMenuItem("Clear selected");
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        clearSelected();
                        update();
                    }
                });
                addMenuItem(menuItem);
            }

            if (!isRadio && selectedValueNames.size() != values.size()) {
                final JMenuItem selectAllItem = new JMenuItem("Select All");
                selectAllItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        selectAll();
                        update();
                    }
                });
                addMenuItem(selectAllItem);
            }
        
            for (final T value : values) {
                boolean selected = selectedValueNames.contains(getName(value));
                if (isHidden(value) && !selected) {
                    // Skip anything that is not selected, and which doesn't have results. Clicking it would be futile.
                    continue;
                }
                String label = getLabel(value);
                final JMenuItem menuItem = isRadio 
                        ? new JRadioButtonMenuItem(label, selected) 
                        : new JCheckBoxMenuItem(label, selected);
                menuItem.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (menuItem.isSelected()) {
                            updateSelection(value, true);
                        }
                        else {
                            updateSelection(value, false);
                        }
                        update();
                    }
                });
                addMenuItem(menuItem);
            }
        }
    }
   
    /**
     * Returns the possible values.
     * @return
     */
    protected abstract Collection<T> getValues();

    /**
     * Returns the list of value names which are currently selected.
     * @return
     */
    protected abstract Collection<String> getSelectedValueNames();
    
    /**
     * Returns the given value's name. Returns the value's toString by default.
     * @param value
     * @return
     */
    protected String getName(T value) {
        return value==null ? null : value.toString();
    }
    
    /**
     * Return the given value's label. Returns getName(value) by default.
     * @param value
     * @return
     */
    protected String getLabel(T value) {
        return getName(value);
    }
    
    /**
     * Should the given value be hidden in the list if it isn't already selected?
     * Returns false by default.
     * @param value
     * @return
     */
    protected boolean isHidden(T value) {
        return false;
    }
    
    /**
     * Clear all selections. Called when the user clicks the "Clear selected" menu option.
     */
    protected abstract void clearSelected();
    
    /**
     * Select all values. Called when the user clicks the "Select all" menu option.
     */
    protected abstract void selectAll();
    
    /**
     * Set the selection for a given value. Called when the user clicks on a menu option.
     * @param value
     * @param selected
     */
    protected abstract void updateSelection(T value, boolean selected);
    
}
