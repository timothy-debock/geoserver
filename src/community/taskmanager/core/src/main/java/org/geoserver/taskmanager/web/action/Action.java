/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.action;

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.geoserver.taskmanager.util.Named;
import org.geoserver.taskmanager.web.ConfigurationPage;

/**
 * 
 * @author Niels Charlier
 *
 */
public interface Action extends Named, Serializable {
    
    void execute(ConfigurationPage onPage, AjaxRequestTarget target, IModel<String> valueModel, List<String> dependentValues);

    default boolean accept(String value, List<String> dependentValues) {
        return true;
    }

}
