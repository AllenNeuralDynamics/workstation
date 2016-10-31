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

package org.janelia.horta.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.janelia.geometry3d.ConstVector3;
import org.janelia.geometry3d.Vector3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generate sorted list of up to eight max resolution blocks near current focus
 * @author brunsc
 */
public class Finest8DisplayBlockChooser 
implements BlockChooser
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    /*
     Choose the eight closest maximum resolution blocks to the current focus point.
    */
    @Override
    public List<BlockTileKey> chooseBlocks(BlockTileSource source, ConstVector3 focus, ConstVector3 previousFocus) 
    {
        // Find up to eight closest blocks adjacent to focus
        BlockTileResolution resolution = source.getMaximumResolution();
        
        ConstVector3 blockSize = ((KtxOctreeBlockTileSource)source).getBlockSize(resolution);
        float dxa[] = new float[] {
            0f,
            -blockSize.getX(),
            +blockSize.getX()};
        float dya[] = new float[] {
            0f,
            -blockSize.getY(),
            +blockSize.getY()};
        float dza[] = new float[] {
            0f,
            -blockSize.getZ(),
            +blockSize.getZ()};
        
        List<BlockTileKey> result0 = new ArrayList<>();
        
        // Enumerate all 27 nearby blocks
        for (float dx : dxa) {
            for (float dy : dya) {
                for (float dz : dza) {
                    ConstVector3 location = focus.plus(new Vector3(dx, dy, dz));
                    BlockTileKey tileKey = source.getBlockKeyAt(location, resolution);
                    if (tileKey == null)
                        continue;
                    ConstVector3 centroid = tileKey.getCentroid();
                    logger.info("tile location = " + location);
                    logger.info("tile centroid = " + centroid);
                    result0.add(tileKey);
                }
            }
        }
        // Sort the blocks strictly by distance to focus
        Collections.sort(result0, new BlockComparator(focus));
        
        // Return only the closest 8 blocks
        List<BlockTileKey> result = new ArrayList<>();
        int listLen = Math.min(8, result0.size());
        for (int i = 0; i < listLen; ++i) {
            result.add(result0.get(i));
        }
        
        return result;
    }
    
    // Sort blocks by distance from focus to block centroid
    private static class BlockComparator implements Comparator<BlockTileKey> {
        private final ConstVector3 focus;
        
        BlockComparator(ConstVector3 focus) {
            this.focus = focus;
        }
        
        @Override
        public int compare(BlockTileKey block1, BlockTileKey block2) {
            ConstVector3 c1 = block1.getCentroid().minus(focus);
            ConstVector3 c2 = block2.getCentroid().minus(focus);
            float d1 = c1.dot(c1); // distance squared
            float d2 = c2.dot(c2);
            return d1 < d2 ? -1 : d1 > d2 ? 1 : 0;
        }
        
    }
    
}
