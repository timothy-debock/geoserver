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

    private IModel<List<MetadataTemplate>> templatesModel;

    private List<MetadataTemplate> selectedTemplates = new ArrayList<>();

    public ImportTemplateDataProvider(
            String resourceId, IModel<List<MetadataTemplate>> templatesModel) {
        this.resourceId = resourceId;

        this.templatesModel = templatesModel;
        for (MetadataTemplate template : templatesModel.getObject()) {
            if (template.getLinkedLayers().contains(resourceId)) {
                selectedTemplates.add(template);
            }
        }
    }

    @Override
    protected List<Property<MetadataTemplate>> getProperties() {
        return Arrays.asList(NAME, DESCRIPTION);
    }

    @Override
    protected List<MetadataTemplate> getItems() {
        return selectedTemplates;
    }

    public void addLink(MetadataTemplate modelObject) {
        modelObject =
                templatesModel.getObject().get(templatesModel.getObject().indexOf(modelObject));
        modelObject.getLinkedLayers().add(resourceId);
        selectedTemplates.add(modelObject);
        selectedTemplates.sort(new MetadataTemplateComparator());
    }

    public void removeLinks(List<MetadataTemplate> templates) {
        Iterator<MetadataTemplate> iterator = new ArrayList<>(templates).iterator();
        while (iterator.hasNext()) {
            MetadataTemplate modelObject = iterator.next();

            modelObject.getLinkedLayers().remove(resourceId);
            selectedTemplates.remove(modelObject);
        }
    }

    /**
     * The remain values are used in the dropdown.
     *
     * @return
     */
    public List<MetadataTemplate> getUnlinkedItems() {
        List<MetadataTemplate> result = new ArrayList<>(templatesModel.getObject());
        result.removeAll(selectedTemplates);
        return result;
    }

    private class MetadataTemplateComparator implements Comparator<MetadataTemplate> {

        public int compare(MetadataTemplate obj1, MetadataTemplate obj2) {
            int priority1 = Integer.MAX_VALUE;
            if (obj1 != null) {
                priority1 = templatesModel.getObject().indexOf(obj1);
            }
            int priority2 = Integer.MAX_VALUE;
            if (obj2 != null) {
                priority2 = templatesModel.getObject().indexOf(obj2);
            }
            return Integer.compare(priority1, priority2);
        }
    }
}
