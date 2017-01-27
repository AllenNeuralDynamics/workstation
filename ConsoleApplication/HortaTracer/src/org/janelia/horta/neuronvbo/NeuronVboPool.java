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
package org.janelia.horta.neuronvbo;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.media.opengl.GL3;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.janelia.geometry3d.AbstractCamera;
import org.janelia.gltools.BasicShaderProgram;
import org.janelia.gltools.ShaderProgram;
import org.janelia.gltools.ShaderStep;
import org.janelia.gltools.texture.Texture2d;
import org.openide.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For improved rendering performance with large numbers of neurons, NeuronVboPool
 * distributes all the neurons among a finite set of vertex buffer objects. Instead
 * of using a separate vbo for each neuron, like we were doing before.
 * @author brunsc
 */
public class NeuronVboPool implements Iterable<NeuronModel>
{
    // Use pool size to balance:
    //  a) static rendering performance (more vbos means more draw calls, means slower rendering)
    //  b) edit update speed (more vbos means fewer neurons per vbo, means faster edit-to-display time)
    // Weird: POOL_SIZE=30 animates much faster than POOL_SIZE=5 with about 120 neurons / 300,000 vertices
    // (smaller blocks for the win...)
    private final static int POOL_SIZE = 30;
    
    // Maintain vbos in a structure sorted by how much stuff is in each one.
    private final NavigableMap<Integer, Deque<NeuronVbo>> vbos = new TreeMap<>();
    // private final List<NeuronVbo> vbos;
    // private int nextVbo = 0;

    // private Set<NeuronModel> dirtyNeurons; // Track incremental updates
    // private Map<NeuronModel, NeuronVbo> neuronVbos;
    // TODO: increase after initial debugging
    
    // Shaders...
    // Be sure to synchronize these constants with the actual shader source uniform layout
    private final ShaderProgram conesShader = new ConesShader();
    private final ShaderProgram spheresShader = new SpheresShader();
    private final static int VIEW_UNIFORM = 1;
    private final static int PROJECTION_UNIFORM = 2;
    private final static int LIGHTPROBE_UNIFORM = 3;
    private final static int SCREENSIZE_UNIFORM = 4;
    private final static int RADIUS_OFFSET_UNIFORM = 5;
    private final static int RADIUS_SCALE_UNIFORM = 6;
    private final Texture2d lightProbeTexture;
    
    private float radiusOffset = 0.0f; // amount to add to every radius, in micrometers
    private float radiusScale = 1.0f; // amount to multiply every radius, in micrometers
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    public NeuronVboPool() 
    {
        // this.vbos = new ArrayList<>();
        for (int i = 0; i < POOL_SIZE; ++i) {
            insertVbo(new NeuronVbo());
        }
        
        lightProbeTexture = new Texture2d();
        try {
            lightProbeTexture.loadFromPpm(getClass().getResourceAsStream(
                    "/org/janelia/gltools/material/lightprobe/"
                            + "Office1W165Both.ppm"));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private void insertVbo(NeuronVbo vbo) {
        Integer vboSize = vboSize(vbo);
        if (! vbos.containsKey(vboSize))
            vbos.put(vboSize, new ConcurrentLinkedDeque<NeuronVbo>());
        Collection<NeuronVbo> subList = vbos.get(vboSize);
        subList.add(vbo);
    }
    
    // Method vboSize is used to determine whether one vbo has more stuff in
    // it than another
    private static Integer vboSize(NeuronVbo vbo) {
        return vbo.getVertexCount();
    }
    
    private NeuronVbo popEmptiestVbo() {
        Map.Entry<Integer, Deque<NeuronVbo>> entry = vbos.firstEntry();
        Integer vboSize = entry.getKey();
        Deque<NeuronVbo> vboDeque = entry.getValue();
        NeuronVbo vbo = vboDeque.removeFirst();
        if (vboDeque.isEmpty())
            vbos.remove(vboSize); // That was the last of its kind
        return vbo;
    }
    
    public float getRadiusOffset() {
        return radiusOffset;
    }

    public void setRadiusOffset(float radiusOffset) {
        this.radiusOffset = radiusOffset;
    }

    public float getRadiusScale() {
        return radiusScale;
    }

    public void setRadiusScale(float radiusScale) {
        this.radiusScale = radiusScale;
    }
    
    private void setUniforms(GL3 gl, float[] modelViewMatrix, float[] projectionMatrix, float[] screenSize) {
        gl.glUniformMatrix4fv(VIEW_UNIFORM, 1, false, modelViewMatrix, 0);
        gl.glUniformMatrix4fv(PROJECTION_UNIFORM, 1, false, projectionMatrix, 0);
        gl.glUniform2fv(SCREENSIZE_UNIFORM, 1, screenSize, 0);
        gl.glUniform1i(LIGHTPROBE_UNIFORM, 0);
        gl.glUniform1f(RADIUS_OFFSET_UNIFORM, radiusOffset);
        gl.glUniform1f(RADIUS_SCALE_UNIFORM, radiusScale);        
    }
    
    void display(GL3 gl, AbstractCamera camera) 
    {
        float[] modelViewMatrix = camera.getViewMatrix().asArray();
        float[] projectionMatrix = camera.getProjectionMatrix().asArray();
        float[] screenSize = new float[] {
            camera.getViewport().getWidthPixels(),
            camera.getViewport().getHeightPixels()
        };
        lightProbeTexture.bind(gl, 0);
        
        // First pass: draw all the connections (edges) between adjacent neuron anchor nodes.
        // These edges are drawn as truncated cones, tapering width between
        // the radii of the adjacent nodes.
        conesShader.load(gl);
        setUniforms(gl, modelViewMatrix, projectionMatrix, screenSize);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.displayEdges(gl);
        }
        
        // TODO: Second pass: repeat display loop for spheres/nodes
        spheresShader.load(gl);
        setUniforms(gl, modelViewMatrix, projectionMatrix, screenSize);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.displayNodes(gl);
        }
    }

    void dispose(GL3 gl) {
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.dispose(gl);
        }
        lightProbeTexture.dispose(gl);
        conesShader.dispose(gl);
        spheresShader.dispose(gl);
    }

    void init(GL3 gl) {
        conesShader.init(gl);
        spheresShader.init(gl);
        lightProbeTexture.init(gl);
        for (NeuronVbo vbo : new VboIterable()) {
            vbo.init(gl);
        }
    }

    void add(NeuronModel neuron) 
    {
        // To keep the vbos balanced, always insert into the emptiest vbo
        NeuronVbo emptiestVbo = popEmptiestVbo();
        final boolean doLogStats = false;
        if (doLogStats) {
            log.info("Emptiest vbo ({}) contains {} neurons and {} vertices", 
                    emptiestVbo.toString(), 
                    emptiestVbo.getNeuronCount(),
                    emptiestVbo.getVertexCount());
        }
        emptiestVbo.add(neuron);
        if (doLogStats) {
            log.info("Emptiest vbo ({}) now contains {} neurons and {} vertices after insersion", 
                    emptiestVbo.toString(), 
                    emptiestVbo.getNeuronCount(),
                    emptiestVbo.getVertexCount());
        }
        insertVbo(emptiestVbo); // Reinsert into its new sorted location
    }

    void remove(NeuronModel neuron) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    boolean isEmpty() {
        for (NeuronVbo vbo : new VboIterable())
            if (! vbo.isEmpty())
                return false;
        return true;
    }

    boolean contains(NeuronModel neuron) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Iterator<NeuronModel> iterator() {
        return new NeuronIterator();
    }
    
    private static class ConesShader extends BasicShaderProgram
    {
        public ConesShader()
        {
            try {
                // Cones and spheres share a vertex shader
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "ConesColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }

    private static class SpheresShader extends BasicShaderProgram
    {
        public SpheresShader()
        {
            try {
                getShaderSteps().add(new ShaderStep(GL3.GL_VERTEX_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorVrtx430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_GEOMETRY_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorGeom430.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "imposter_fns330.glsl"))
                );
                getShaderSteps().add(new ShaderStep(GL3.GL_FRAGMENT_SHADER,
                        getClass().getResourceAsStream(
                                "/org/janelia/horta/shader/"
                                        + "SpheresColorFrag430.glsl"))
                );
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }        
        }
    }

    private class VboIterable implements Iterable<NeuronVbo>
    {
        @Override
        public Iterator<NeuronVbo> iterator() {
            return new VboIterator();
        }   
    }
    
    private class VboIterator implements Iterator<NeuronVbo>
    {
        private final Collection<NeuronVbo> EMPTY_LIST = Collections.<NeuronVbo>emptyList();

        private final Iterator<Integer> sizeIterator;
        private Iterator<NeuronVbo> vboIterator = EMPTY_LIST.iterator();

        public VboIterator() {
            sizeIterator = vbos.keySet().iterator();
            if (sizeIterator.hasNext()) {
                Integer currentSize = sizeIterator.next();
                vboIterator = vbos.get(currentSize).iterator();
            }
        }
        
        private void advanceToNextVbo()
        {
            // Advance to next actual neuron
            while ( sizeIterator.hasNext() && (! vboIterator.hasNext()) ) {
                Integer currentSize = sizeIterator.next();
                vboIterator = vbos.get(currentSize).iterator();
            }
        }
        
        @Override
        public boolean hasNext() {
            advanceToNextVbo();
            return vboIterator.hasNext();
        }

        @Override
        public NeuronVbo next() {
            advanceToNextVbo();
            return vboIterator.next();
        }
        
    }
    
    private class NeuronIterator implements Iterator<NeuronModel> 
    {
        private final Collection<NeuronModel> EMPTY_LIST = Collections.<NeuronModel>emptyList();

        private final Iterator<NeuronVbo> vboIterator;
        private Iterator<NeuronModel> neuronIterator = EMPTY_LIST.iterator(); // iterator for one vbo
        
        public NeuronIterator() {
            vboIterator = new VboIterator();
            if (vboIterator.hasNext()) {
                NeuronVbo currentVbo = vboIterator.next();
                neuronIterator = currentVbo.iterator();
            }
        }
        
        private void advanceToNextNeuron()
        {
            // Advance to next actual neuron
            while ( vboIterator.hasNext() && (! neuronIterator.hasNext()) ) {
                NeuronVbo currentVbo = vboIterator.next();
                neuronIterator = currentVbo.iterator();
            }
        }
        
        @Override
        public boolean hasNext() 
        {
            advanceToNextNeuron();
            return neuronIterator.hasNext();
        }

        @Override
        public NeuronModel next() 
        {
            advanceToNextNeuron();
            return neuronIterator.next();
        }
    }
}
