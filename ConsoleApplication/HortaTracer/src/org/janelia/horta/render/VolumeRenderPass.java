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

package org.janelia.horta.render;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.Framebuffer;
import org.janelia.gltools.GL3Actor;
import org.janelia.gltools.RenderPass;
import org.janelia.gltools.RenderTarget;
import org.janelia.gltools.material.DepthSlabClipper;
import org.janelia.gltools.texture.Texture2d;

/**
 *
 * @author Christopher Bruns
 */
public class VolumeRenderPass extends RenderPass
implements DepthSlabClipper
{
    private final RenderTarget rgbaTarget;
    private final RenderTarget coreDepthTarget;
    // private final RenderTarget depthTarget;
    private final float[] clearColor4 = new float[] {0,0,0,0};
    private final int[] clearColor4i = new int[] {0,0,0,0};
    // private final float[] depthOne = new float[] {1};
    private final int targetAttachments[];
    private Texture2d cachedDepthTexture = null;
    
    // private float relativeSlabThickness = 0.5f;
    private float relativeZNear = 0.92f;
    private float relativeZFar = 1.08f;

    public VolumeRenderPass(GLAutoDrawable drawable)
    {
        super(new Framebuffer(drawable));
        
        // Create render targets
        // Create G-Buffer for deferred rendering
        final int rgbaAttachment = GL3.GL_COLOR_ATTACHMENT0;
        // final int depthAttachment = GL3.GL_DEPTH_ATTACHMENT;
        final int coreDepthAttachment = GL3.GL_COLOR_ATTACHMENT1;
        rgbaTarget = framebuffer.addRenderTarget(GL3.GL_RGBA12,
                rgbaAttachment);
        // depthTarget = framebuffer.addRenderTarget(GL3.GL_DEPTH_COMPONENT24, // 16? 24? 32? // 16 does not work; unspecified does not work
        //         depthAttachment);
        coreDepthTarget = framebuffer.addRenderTarget(GL3.GL_RG32UI,
                coreDepthAttachment);
        
        // Attach render targets to renderer
        addRenderTarget(rgbaTarget);
        addRenderTarget(coreDepthTarget);
        targetAttachments = new int[renderTargets.size()];
        for (int rt = 0; rt < renderTargets.size(); ++rt) {
            targetAttachments[rt] = renderTargets.get(rt).getAttachment();
        }
        rgbaTarget.setDirty(true);
    }
    
    @Override
    public void dispose(GL3 gl) {
        super.dispose(gl);
        framebuffer.dispose(gl);
    }
    
    @Override
    public void init(GL3 gl) {
        framebuffer.init(gl);
        super.init(gl);
    }
    
    @Override
    protected void renderScene(GL3 gl, AbstractCamera camera)
    {
        // if (true) return; // TODO
        if (! rgbaTarget.isDirty())
            return;

        gl.glDrawBuffers(targetAttachments.length, targetAttachments, 0);

        gl.glClearBufferfv(GL3.GL_COLOR, 0, clearColor4, 0);
        // gl.glClearBufferfv(GL3.GL_DEPTH, 0, depthOne, 0);
        gl.glClearBufferuiv(GL3.GL_COLOR, 1, clearColor4i, 0); // pick buffer...

        // Blend intensity channel, but not pick channel
        gl.glDisablei(GL3.GL_BLEND, 0); // TODO
        gl.glDisablei(GL3.GL_BLEND, 1); // TODO - how to write pick for BRIGHTER image?

        super.renderScene(gl, camera);

        for (RenderTarget rt : new RenderTarget[] {rgbaTarget, coreDepthTarget}) 
        {
            rt.setHostBufferNeedsUpdate(true);
            rt.setDirty(false);
        }
        gl.glDrawBuffers(1, targetAttachments, 0);
    }
    
    public RenderTarget getRgbaTexture() {
        return rgbaTarget;
    }
    
    public RenderTarget getCoreDepthTexture() {
        return coreDepthTarget;
    }
    
    public Framebuffer getFramebuffer() {
        return framebuffer;
    }
    
    @Override
    public void setOpaqueDepthTexture(Texture2d depthTexture) {
        if (cachedDepthTexture == depthTexture)
            return;
        cachedDepthTexture = depthTexture;
        for (GL3Actor actor : getActors()) {
            if (actor instanceof DepthSlabClipper) {
                DepthSlabClipper ba = (DepthSlabClipper) actor;
                ba.setOpaqueDepthTexture(depthTexture);
            }
        }
    }
    
    @Override
    public void addActor(GL3Actor actor)
    {
        super.addActor(actor);
        if (actor instanceof DepthSlabClipper) {
            DepthSlabClipper dsc = (DepthSlabClipper)actor;
            dsc.setOpaqueDepthTexture(cachedDepthTexture);
            dsc.setRelativeSlabThickness(relativeZNear, relativeZFar);
        }
    }

    @Override
    public void setRelativeSlabThickness(float zNear, float zFar)
    {
        if ((zNear == relativeZNear) && (zFar == relativeZFar))
            return; // nothing changed        
        relativeZNear = zNear;
        relativeZFar = zFar;
        for (GL3Actor actor : getActors()) {
            if (actor instanceof DepthSlabClipper) {
                DepthSlabClipper ba = (DepthSlabClipper) actor;
                ba.setRelativeSlabThickness(zNear, zFar);
            }
        }
        rgbaTarget.setDirty(true);
        coreDepthTarget.setDirty(true);
    }

    public float getRelativeZNear() {
        return relativeZNear;
    }

    public float getRelativeZFar() {
        return relativeZFar;
    }
    
}
