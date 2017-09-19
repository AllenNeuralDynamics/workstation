package org.janelia.it.workstation.browser.gui.dialogs.download;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.enums.FileType;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.gui.support.Debouncer;
import org.janelia.it.workstation.browser.gui.support.GroupedKeyValuePanel;
import org.janelia.it.workstation.browser.gui.support.Icons;
import org.janelia.it.workstation.browser.model.descriptors.ArtifactDescriptor;
import org.janelia.it.workstation.browser.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

public final class DownloadVisualPanel2 extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(DownloadVisualPanel2.class);

    private static final String[] FORMAT_EXTENSIONS_LSM = {
            DownloadWizardState.NATIVE_EXTENSION,
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j",
            "lsm.bz2" 
    };

    private static final String[] FORMAT_EXTENSIONS_LSM_BZ2 = {
            DownloadWizardState.NATIVE_EXTENSION,
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j",
            "lsm",
    };
    
    private static final String[] FORMAT_EXTENSIONS_RAW = {
            DownloadWizardState.NATIVE_EXTENSION,
            "tif",  
            "v3dpbd", 
            "mp4", 
            "h5j"
    };
    
    private static final String[] FORMAT_EXTENSIONS_PBD = {
            DownloadWizardState.NATIVE_EXTENSION,
            "tif", 
            "v3draw", 
            "mp4", 
            "h5j",
            "lsm", 
            "lsm.bz2" 
    };

    private static final String[] FORMAT_EXTENSIONS_H5J = {
            DownloadWizardState.NATIVE_EXTENSION,
            "tif", 
            "v3draw", 
            "v3dpbd", 
            "mp4" 
    };

    private static final String[] FORMAT_EXTENSIONS_TIF = {
            DownloadWizardState.NATIVE_EXTENSION,
            "v3draw", 
            "v3dpbd", 
            "mp4", 
            "h5j" 
    };
    
    private static final Map<String,String[]> formatMap = new HashMap<>();
    
    static {
        formatMap.put("lsm", FORMAT_EXTENSIONS_LSM);
        formatMap.put("lsm.bz2", FORMAT_EXTENSIONS_LSM_BZ2);
        formatMap.put("v3draw", FORMAT_EXTENSIONS_RAW);
        formatMap.put("v3dpbd", FORMAT_EXTENSIONS_PBD);
        formatMap.put("h5j", FORMAT_EXTENSIONS_H5J);
        formatMap.put("tif", FORMAT_EXTENSIONS_TIF);
        
    }

    private DownloadWizardPanel2 wizardPanel;
    private final Debouncer debouncer = new Debouncer();
    
    // GUI
    private JPanel mainPane;
    private GroupedKeyValuePanel attrPanel;
    private Map<String,JComboBox<String>> formatCombos;
    private JCheckBox splitChannelCheckbox;
    
    // Inputs
    private List<DownloadObject> downloadObjects;
    private List<ArtifactDescriptor> artifactDescriptors;
    
    // Outputs
    private boolean loaded = false;
    private Map<String,String> selectedOutputExtensions;
    private boolean splitChannels;
    
    @Override
    public String getName() {
        return "File Processing";
    }
    
    /**
     * Creates new form DownloadVisualPanel4
     */
    public DownloadVisualPanel2(DownloadWizardPanel2 wizardPanel) {
        this.wizardPanel = wizardPanel;
        setLayout(new BorderLayout());        
    }

    public void init(DownloadWizardState state) {

        this.downloadObjects = state.getDownloadObjects();
        this.artifactDescriptors = state.getArtifactDescriptors();
        this.splitChannels = state.isSplitChannels();
        this.selectedOutputExtensions = state.getOutputExtensions();
        
        // Set default extensions
        for(String extension : formatMap.keySet()) {
            if (!selectedOutputExtensions.containsKey(extension)) {
                selectedOutputExtensions.put(extension, formatMap.get(extension)[0]);
            }
        }
        
        this.attrPanel = new GroupedKeyValuePanel();
        this.formatCombos = new HashMap<>();

        mainPane = new JPanel(new BorderLayout());
        mainPane.add(new JLabel(Icons.getLoadingIcon()));
        
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setViewportView(mainPane);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Increase scroll speed

        removeAll();
        add(scrollPane, BorderLayout.CENTER);
        
        populateSourceExtensions();
    }

    private void populateSourceExtensions() {

        if (!debouncer.queue()) {
            log.debug("Skipping populateDownloadItemList, since there is an operation already in progress");
            return;
        }
                
        SimpleWorker worker = new SimpleWorker() {

            Multiset<String> countedExtensions = TreeMultiset.create();
            
            @Override
            protected void doStuff() throws Exception {
                
                for(DownloadObject downloadObject : downloadObjects) {
                    DomainObject domainObject = downloadObject.getDomainObject();
                    log.debug("Inspecting download object '{}'", domainObject);
                    
                    for (ArtifactDescriptor artifactDescriptor : artifactDescriptors) {
                        log.debug("  Checking artifact descriptor '{}'", artifactDescriptor);

                        for (HasFiles hasFiles : artifactDescriptor.getFileSources(domainObject)) {
                            log.debug("    Checking source item '{}'", hasFiles);
                            for (FileType fileType : artifactDescriptor.getSelectedFileTypes()) {
                                log.debug("      Adding item for file type '{}'", fileType);
                                
                                if (fileType.is3dImage()) {
                                    DownloadFileItem downloadItem = new DownloadFileItem(downloadObject.getFolderPath(), domainObject);
                                    downloadItem.init(artifactDescriptor, hasFiles, fileType, null, false, false, "{GUID}_{File Name}");
                                    String sourceExtension = downloadItem.getSourceExtension();
                                    if (sourceExtension!=null) {
                                        countedExtensions.add(sourceExtension);
                                    }
                                }
                            }
                        }
                    }
                }    
            }
            
            @Override
            protected void hadSuccess() {
                
                for (String extension : countedExtensions.elementSet()) {
                    int count = countedExtensions.count(extension);
                    
                    String[] outputExtensionArray = formatMap.get(extension);
                    
                    if (outputExtensionArray==null) {
                        continue;
                    }
                    
                    JComboBox<String> formatCombo = new JComboBox<>();
                    formatCombo.setEditable(false);
                    formatCombo.setToolTipText("Choose an export format for "+extension+" image stacks");
                    attrPanel.addItem("Convert "+extension+" image stacks ("+count+" files)", formatCombo);

                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) formatCombo.getModel();
                    model.removeAllElements();
                    for (String outputExtension : outputExtensionArray) {
                        model.addElement(outputExtension);
                    }

                    // If the user already selected something, keep it selected
                    String selectedOutputExtension = selectedOutputExtensions.get(extension);
                    if (selectedOutputExtension!=null) {
                        model.setSelectedItem(selectedOutputExtension);
                    }
                    
                    formatCombos.put(extension, formatCombo);
                }
                
                loaded = true;
                wizardPanel.fireChangeEvent();
                
                splitChannelCheckbox = new JCheckBox();
                splitChannelCheckbox.setSelected(splitChannels);
                attrPanel.addItem("Split channels in 3d stacks into individual files", splitChannelCheckbox);

                mainPane.removeAll();
                mainPane.add(attrPanel);
                mainPane.updateUI();
                
                debouncer.success();
            }

            @Override
            protected void hadError(Throwable error) {
                debouncer.failure();
                ConsoleApp.handleException(error);
            }
        };

        worker.execute();
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    public boolean isSplitChannels() {
        return splitChannelCheckbox!=null && splitChannelCheckbox.isSelected();
    }

    public Map<String,String> getOutputExtensions() {
        Map<String,String> outputExtensions = new HashMap<>();
        for (String extension : selectedOutputExtensions.keySet()) {
            String outputExtension = selectedOutputExtensions.get(extension);
            outputExtensions.put(extension, outputExtension);
        }
        for (String extension : formatCombos.keySet()) {
            JComboBox<String> formatCombo = formatCombos.get(extension);
            DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) formatCombo.getModel();
            String outputExtension = (String)model.getSelectedItem();
            outputExtensions.put(extension, outputExtension);
        }
        return outputExtensions;
    }
}
