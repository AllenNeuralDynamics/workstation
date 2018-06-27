package org.janelia.it.workstation.gui.task_workflow;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.beans.PropertyEditorSupport;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComboBox;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.VantageInterface;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReviewPointNode extends AbstractNode
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ReviewPoint point;
    
    public ReviewPointNode(ReviewPoint point) {
        super(Children.create(new ReviewPointChildFactory(), true), Lookups.singleton(point));
        this.point = point;
        updateDisplayName();
       
    }
    
    private void updateDisplayName() {
        setDisplayName("Vertex"); 
    }
    
  private static class ReviewPointChildFactory extends ChildFactory
    {
        private List<ReviewPoint> pointList;
        
        public ReviewPointChildFactory() {
        }

        @Override
        protected boolean createKeys(List toPopulate)
        {
            return true;
        }
    }
    
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/VertexTip2.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public double getX() {
        return point.getLocation().getX();
    }
    
    public double getY() {
        return point.getLocation().getY();
    }
        
    public double getZ() {
        return point.getLocation().getZ();
    }
     
    public String getRotation() {
        float[] quat = point.getRotation();
        return "[" + quat[0] + "," + quat[1] + "," + quat[2] + "," + quat[3] + "]";
    }
    
    public String getNotes() {
        return point.getNote();
    }
    
    public void setNotes(String note) {
        System.out.println ("DFSFSD----" + note);
        point.setNote(note);
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            // point columns
            Property prop;
            prop = new PropertySupport.Reflection(this, double.class, "getX", null); 
            prop.setName("x");
            set.put(prop); 
            prop = new PropertySupport.Reflection(this, double.class, "getY", null); 
            prop.setName("y");
            set.put(prop); 
            prop = new PropertySupport.Reflection(this, double.class, "getZ", null); 
            prop.setName("z");
            set.put(prop); 
            // rotation
            prop = new PropertySupport.Reflection(this, String.class, "getRotation", null); 
            prop.setName("rotation"); 
            set.put(prop); 
            // review notes
            PropertySupport.Reflection reviewProp = new PropertySupport.Reflection(this, String.class, "notes"); 
            reviewProp.setPropertyEditorClass(ReviewPointPropertyEditor.class);            
            set.put(reviewProp);
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    }
}

