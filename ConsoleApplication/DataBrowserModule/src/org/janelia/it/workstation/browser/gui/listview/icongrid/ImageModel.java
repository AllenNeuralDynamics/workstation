package org.janelia.it.workstation.browser.gui.listview.icongrid;

import java.awt.image.BufferedImage;
import java.util.List;

import org.janelia.it.jacs.model.domain.ontology.Annotation;
import org.janelia.it.workstation.browser.model.ImageDecorator;

/**
 * Provides a standardized API for accessing information about a set of image objects of type T, 
 * with ids of type S. 
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public interface ImageModel<T,S> {

    public T getImageByUniqueId(S id) throws Exception;
    
    public S getImageUniqueId(T imageObject);
    
    public String getImageFilepath(T imageObject);
    
    public String getImageTitle(T imageObject);

    public String getImageSubtitle(T imageObject);

    public BufferedImage getStaticIcon(T imageObject);
    
    public List<Annotation> getAnnotations(T imageObject);
    
    public List<ImageDecorator> getDecorators(T imageObject);

}
