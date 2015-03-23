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
package org.janelia.horta;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.janelia.console.viewerapi.SampleLocation;
import org.janelia.console.viewerapi.ViewerLocationAcceptor;
import org.janelia.geometry3d.PerspectiveCamera;
import org.janelia.geometry3d.Vantage;
import org.janelia.geometry3d.Vector3;
import static org.janelia.horta.NeuronTracerTopComponent.BASE_YML_FILE;
import org.janelia.horta.volume.StaticVolumeBrickSource;
import org.janelia.scenewindow.SceneWindow;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleLocationAcceptor implements ViewerLocationAcceptor {

    private String currentSource;
    private NeuronTraceLoader loader;
    private NeuronTracerTopComponent nttc;
    private SceneWindow sceneWindow;

    private static final Logger logger = LoggerFactory.getLogger(SampleLocationAcceptor.class);
    
    public SampleLocationAcceptor(
            String currentSource, 
            NeuronTraceLoader loader, 
            NeuronTracerTopComponent nttc, 
            SceneWindow sceneWindow) {
        this.currentSource = currentSource;
        this.loader = loader;
        this.nttc = nttc;
        this.sceneWindow = sceneWindow;
    }
    
    @Override
    public void acceptLocation(final SampleLocation sampleLocation) throws Exception {
        Runnable task = new Runnable() {
            @Override
            public void run()
            {
                ProgressHandle progress
                        = ProgressHandleFactory.createHandle("Loading View in Horta...");
                progress.start();
                try {
                    progress.setDisplayName("Loading brain specimen (tilebase.cache.yml)...");
                    // TODO - ensure that Horta viewer is open
                    // First ensure that this component uses same sample.
                    StaticVolumeBrickSource volumeSource = setSampleUrl(sampleLocation.getSampleUrl(), progress);
                    if (volumeSource == null) {
                        throw new IOException("Loading volume source failed");
                    }
                    progress.setDisplayName("Centering on location...");
                    setCameraLocation(sampleLocation);
                    progress.switchToIndeterminate(); // TODO - enhance tile loading with a progress listener
                    progress.setDisplayName("Loading brain tile image...");
                    loader.loadTileAtCurrentFocus(volumeSource);
                    sceneWindow.getGLAutoDrawable().display();
                } catch (final IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run()
                        {
                            JOptionPane.showMessageDialog(
                                    sceneWindow.getOuterComponent(), 
                                    "Error loading brain specimen: " + ex.getMessage(), 
                                    "Brain Raw Data Error", 
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    });                   
                }
                finally {
                    progress.finish();
                }
            }
        };
        RequestProcessor.getDefault().post(task);
    }

    /**
     * Returns true if Url was changed. False otherwise.
     * 
    */
    private StaticVolumeBrickSource setSampleUrl(URL focusUrl, ProgressHandle progress) {
        String urlStr = focusUrl.toString();
        // Check: if same as current source, no need to change that.
        if (urlStr.equals(currentSource))
            return nttc.getVolumeSource();
        URI uri;
        try {
            uri = focusUrl.toURI();
            URI yamlUri = new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath() + "/" + BASE_YML_FILE,
                    uri.getFragment()
            );
            logger.info("Constructed URI: {}.", uri);
            URL yamlUrl = yamlUri.toURL();
            InputStream stream1 = yamlUrl.openStream();
            StaticVolumeBrickSource volumeSource = nttc.loadYaml(stream1, loader, progress);
            return volumeSource;
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
    
    private boolean setCameraLocation(SampleLocation sampleLocation) {
        // Now, position this component over other component's
        // focus.
        PerspectiveCamera pCam = (PerspectiveCamera) sceneWindow.getCamera();
        Vantage v = pCam.getVantage();
        Vector3 focusVector3 = new Vector3(
            (float)sampleLocation.getFocusXUm(),
            (float)sampleLocation.getFocusYUm(),
            (float)sampleLocation.getFocusZUm());
        
        if (!v.setFocusPosition(focusVector3)) {
            logger.warn("Did not change focus as directed.");
        }        
        v.setDefaultFocus(focusVector3);

        double zoom = sampleLocation.getMicrometersPerWindowHeight();
        if (zoom > 0) {
            v.setSceneUnitsPerViewportHeight((float)zoom);
            logger.info("Set micrometers per view height to " + zoom);
            v.setDefaultSceneUnitsPerViewportHeight((float)zoom);
        }

        v.notifyObservers();
        return true;
    }
    
}