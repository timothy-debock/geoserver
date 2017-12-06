package org.geoserver.taskmanager.web.action;

import org.geoserver.taskmanager.util.Named;
import org.geoserver.taskmanager.web.ConfigurationPage;

public interface Action extends Named {
    
    void execute(ConfigurationPage onPage, String value);

}
