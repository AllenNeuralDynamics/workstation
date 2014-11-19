package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;

import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.children.NeuronNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.ObjectiveNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(SampleNode.class);
    
    public SampleNode(TreeNodeChildFactory parentChildFactory, Sample sample) throws Exception {
        super(parentChildFactory, 
                DomainExplorerTopComponent.isShowNeurons()
                        ?Children.create(new NeuronNodeFactory(sample), true)
                        :Children.create(new ObjectiveNodeFactory(sample), true), sample);
    }
    
    private Sample getSample() {
        return (Sample)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSample().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("beaker.png").getImage();
    }
    
    @Override
    public String get2dImageFilepath(String role) {
        Sample sample = getSample();
        List<String> objectives = new ArrayList<String>(sample.getObjectives().keySet());
        if (objectives.isEmpty()) return null;
        Collections.sort(objectives);
        String largestObjective = objectives.get(objectives.size()-1);
        ObjectiveSample objSample = sample.getObjectives().get(largestObjective);
        if (objSample==null) return null;
        SamplePipelineRun run = objSample.getLatestRun();
        HasFiles lastResult = null;
        if (run==null || run.getResults()==null) return null;
        for(PipelineResult result : run.getResults()) {
            if (result instanceof HasFiles) {
                lastResult = (HasFiles)result;
            }
        }
        if (lastResult!=null) {
            return DomainUtils.get2dImageFilepath(lastResult, role);
        }
        return null;
    }
}
