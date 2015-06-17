/*
 * Licensed under the Janelia Farm Research Campus Software Copyright 1.1
 * 
 * Copyright (c) 2014, Howard Hughes Medical Institute, All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *     1. Redistributions of source code must retain the above copyright notice, 
 *        this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright 
 *        notice, this list of conditions and the following disclaimer in the 
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the Howard Hughes Medical Institute nor the names 
 *        of its contributors may be used to endorse or promote products derived 
 *        from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, ANY 
 * IMPLIED WARRANTIES OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A 
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * REASONABLE ROYALTIES; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.janelia.gltools;

import java.awt.Color;
import javax.media.opengl.GL3;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.geometry3d.Matrix4;
import org.janelia.geometry3d.ScreenQuadMesh;
import org.janelia.gltools.material.ScreenGradientColorMaterial;

/**
 *
 * @author Christopher Bruns
 */
public class ColorBackgroundActor extends MeshActor 
{
    private ScreenQuadMesh mesh;
    
    public ColorBackgroundActor(Color color) {
        super(new ScreenQuadMesh(color), 
              new ScreenGradientColorMaterial(),
              null);
        this.mesh = (ScreenQuadMesh)geometry;
    }
    
    public ColorBackgroundActor(Color topColor, Color bottomColor) {
        super(new ScreenQuadMesh(topColor, bottomColor), 
              new ScreenGradientColorMaterial(),
              null);
        this.mesh = (ScreenQuadMesh)geometry;
    }
    
    @Override
    public void display(GL3 gl, AbstractCamera camera, Matrix4 parentModelViewMatrix) {
        gl.glDisable(GL3.GL_DEPTH_TEST);
        gl.glDisable(GL3.GL_BLEND);
        super.display(gl, camera, parentModelViewMatrix);
    }
    
    public void setColor(Color color) {
        mesh.setColor(color);
        mesh.notifyObservers();
    }
    
    public void setColor(Color topColor, Color bottomColor) {
        mesh.setTopColor(topColor);
        mesh.setBottomColor(bottomColor);
        mesh.notifyObservers();
    }
    
    public void setBottomColor(Color color) {
        mesh.setBottomColor(color);
        mesh.notifyObservers();
    }
    
    public void setTopColor(Color color) {
        mesh.setTopColor(color);
        mesh.notifyObservers();
    }
}
