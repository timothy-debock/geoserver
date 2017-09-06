/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;

@Component
public class DbRemotePublicationTaskTypeImpl extends AbstractRemotePublicationTaskTypeImpl {

    public static final String NAME = "RemoteDbPublication";

    public static final String PARAM_DB_NAME = "database";
    
    @Autowired
    ExtTypes extTypes;
    
    @PostConstruct
    @Override
    public void initParamInfo() {
        super.initParamInfo();
        paramInfo.put(PARAM_DB_NAME, new ParameterInfo(PARAM_DB_NAME, extTypes.dbName, true));
    }
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected boolean createStore(ExternalGS extGS, GeoServerRESTManager restManager,
            StoreInfo store, Map<String, Object> parameterValues) throws IOException {
        final DbSource db = (DbSource) parameterValues.get(PARAM_DB_NAME);
        return restManager.getStoreManager().create(store.getWorkspace().getName(), 
                db.getStoreEncoder(store.getName()));
    }

    @Override
    protected boolean mustCleanUpStore() {
        return false;
    }

}
