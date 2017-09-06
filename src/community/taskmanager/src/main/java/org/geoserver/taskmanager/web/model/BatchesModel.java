/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.taskmanager.util.TaskManagerBeans;

public class BatchesModel extends GeoServerDataProvider<Batch> {

    private static final long serialVersionUID = -8246320435114536132L;

    public static final Property<Batch> WORKSPACE = new BeanProperty<Batch>("workspace", "workspace");
    public static final Property<Batch> DESCRIPTION = new BeanProperty<Batch>("description", "description");
    public static final Property<Batch> FREQUENCY = new BeanProperty<Batch>("frequency", "frequency");
    public static final Property<Batch> ENABLED = new BeanProperty<Batch>("enabled", "enabled");
    public static final Property<Batch> NAME = new BeanProperty<Batch>("name", "name");
    
    public static final Property<Batch> FULL_NAME = new AbstractProperty<Batch>("name") {
        private static final long serialVersionUID = 6588177543318699677L;

        @Override
        public Object getPropertyValue(Batch item) {
            return item.getFullName();
        }        
    };
    
    
    private IModel<Configuration> configurationModel;
    
    public BatchesModel() {
        
    }
    
    public BatchesModel(IModel<Configuration> configurationModel) {
        this.configurationModel = configurationModel;
    }
    
    @Override
    protected List<Property<Batch>> getProperties() {
        return Arrays.asList(WORKSPACE, configurationModel == null ? FULL_NAME : NAME, DESCRIPTION, FREQUENCY, ENABLED);
    }

    @Override
    protected List<Batch> getItems() {
        return configurationModel == null ? TaskManagerBeans.get().getDao().getBatches() : 
            new ArrayList<Batch>(configurationModel.getObject().getBatches().values());
    }

}
