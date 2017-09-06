/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

public class DropDownPanel extends Panel {

    private static final long serialVersionUID = -1829729746678003578L;
    
    public DropDownPanel(String id, IModel<String> model, 
            IModel<? extends List<? extends String>> choiceModel) {
        this(id, model, choiceModel, null);
        
    }

    public DropDownPanel(String id, IModel<String> model, 
            IModel<? extends List<? extends String>> choiceModel,
            IModel<String> labelModel) {
        super(id, model);
                
        add(new Label("message", labelModel));
        add(new DropDownChoice<String>("dropdown", model, choiceModel).setNullValid(true));
    }
    
    @SuppressWarnings("unchecked")
    public DropDownChoice<String> getDropDownChoice() {
        return (DropDownChoice<String>) get("dropdown");
    }
}
