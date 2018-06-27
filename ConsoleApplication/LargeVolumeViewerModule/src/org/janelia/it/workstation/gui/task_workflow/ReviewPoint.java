package org.janelia.it.workstation.gui.task_workflow;

import org.janelia.it.jacs.shared.geom.Vec3;

/**
 *
 * @author schauderd
 */
public class ReviewPoint {
    private Vec3 location;
    private float[] rotation;
    private float zoomLevel;
    private boolean interpolate;
    
    ReviewPoint() {
        
    }

    public Vec3 getLocation() {
        return location;
    }
    
    public void setLocation(Vec3 location) {
        this.location = location;
    }

    public float[] getRotation() {
        return rotation;
    }

    public void setRotation(float[] rotation) {
        this.rotation = rotation;
    }
    
    public float getZoomLevel() {
        return zoomLevel;
    }

    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
    
    public boolean getInterpolate() {
        return interpolate;
    }
    
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
}


