/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.metadata.web.panel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.metadata.data.model.MetadataTemplate;
import org.geoserver.metadata.data.service.MetadataTemplateService;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * DataProvider that manages the list of linked templates for a layer.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class ImportTemplateDataProvider extends GeoServerDataProvider<MetadataTemplate> {

    private static final long serialVersionUID = -8246320435114536132L;

    public static final Property<MetadataTemplate> NAME =
            new BeanProperty<MetadataTemplate>("name", "name");

    public static final Property<MetadataTemplate> DESCRIPTION =
            new BeanProperty<MetadataTemplate>("description", "description");

    private final String resourceId;

    private List<MetadataTemplate> allTemplates = new ArrayList<>();

    private IModel<List<MetadataTemplate>> linkedTemplatesModel;

    public ImportTemplateDataProvider(
            String resourceId, IModel<List<MetadataTemplate>> linkedTemplatesModel) {
        this.resourceId = resourceId;

        this.linkedTemplatesModel = linkedTemplatesModel;
        MetadataTemplateService service =
                GeoServerApplication.get()
                        .getApplicationContext()
                        .getBean(MetadataTemplateService.class);
        allTemplates = service.list();
    }

    @Override
    protected List<Property<MetadataTemplate>> getProperties() {
        return Arrays.asList(NAME, DESCRIPTION);
    }

    @Override
    protected List<MetadataTemplate> getItems() {
        return linkedTemplatesModel.getObject();
    }

    public void addLink(MetadataTemplate modelObject) {
        modelObject.getLinkedLayers().add(resourceId);
        linkedTemplatesModel.getObject().add(modelObject);
        linkedTemplatesModel.getObject().sort(new MetadataTemplateComparator());
    }

    public void removeLinks(List<MetadataTemplate> templates) {
        Iterator<MetadataTemplate> iterator = new ArrayList<>(templates).iterator();
        while (iterator.hasNext()) {
            MetadataTemplate modelObject = iterator.next();

            modelObject.getLinkedLayers().remove(resourceId);
            linkedTemplatesModel.getObject().remove(modelObject);
        }
    }

    /**
     * The remain values are used in the dropdown.
     *
     * @return
     */
    public List<MetadataTemplate> getUnlinkedItems() {
        List<MetadataTemplate> result = new ArrayList<>(allTemplates);
        result.removeAll(linkedTemplatesModel.getObject());
        return result;
    }

    private class MetadataTemplateComparator implements Comparator<MetadataTemplate> {

        public int compare(MetadataTemplate obj1, MetadataTemplate obj2) {
            int priority1 = Integer.MAX_VALUE;
            if (obj1 != null) {
                priority1 = allTemplates.indexOf(obj1);
            }
            int priority2 = Integer.MAX_VALUE;
            if (obj2 != null) {
                priority2 = allTemplates.indexOf(obj2);
            }
            return Integer.compare(priority1, priority2);
        }
    }
}
