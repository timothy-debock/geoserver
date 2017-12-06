package org.geoserver.taskmanager.web.action;

import org.geoserver.taskmanager.web.ConfigurationPage;
import org.geoserver.web.data.resource.ResourceConfigurationPage;
import org.springframework.stereotype.Component;

@Component
public class LayerEditAction implements Action {
    
    private final static String NAME = "LayerEdit";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void execute(ConfigurationPage onPage, String value) {
        String[] prefixname = value.split(":", 2);
        onPage.setResponsePage(new ResourceConfigurationPage(prefixname[0], prefixname[1])
            .setReturnPage(onPage));
    }

}
