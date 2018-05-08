/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.taskmanager.external.ExtTypes;
import org.geoserver.taskmanager.external.FileReference;
import org.geoserver.taskmanager.schedule.ParameterInfo;
import org.geoserver.taskmanager.schedule.TaskContext;
import org.geoserver.taskmanager.schedule.TaskException;
import org.geoserver.taskmanager.schedule.TaskResult;
import org.geoserver.taskmanager.schedule.TaskType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CopyFileTaskTypeImpl implements TaskType {

    public static final String NAME = "CopyFile";

    public static final String PARAM_SOURCE_SERVICE = "sourceService";

    public static final String PARAM_TARGET_SERVICE = "targetService";
    
    public static final String PARAM_SOURCE_PATH = "sourcePath";

    public static final String PARAM_TARGET_PATH = "targetPath";

    @Autowired
    protected ExtTypes extTypes;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, ParameterInfo> getParameterInfo() {
        Map<String, ParameterInfo> paramInfo = new HashMap<String, ParameterInfo>();
        ParameterInfo sourceService = new ParameterInfo(PARAM_SOURCE_SERVICE, extTypes.fileService, true);
        ParameterInfo targetService = new ParameterInfo(PARAM_TARGET_SERVICE, extTypes.fileService, true);        
        paramInfo.put(PARAM_SOURCE_SERVICE, sourceService);    
        paramInfo.put(PARAM_TARGET_SERVICE, targetService);
        paramInfo.put(PARAM_SOURCE_PATH, new ParameterInfo(PARAM_SOURCE_PATH, extTypes.file(false, true), true)
                .dependsOn(sourceService));
        paramInfo.put(PARAM_TARGET_PATH, new ParameterInfo(PARAM_TARGET_PATH, extTypes.file(false, false), true)
                .dependsOn(targetService));
        return paramInfo;
    }

    @Override
    public TaskResult run(TaskContext ctx) throws TaskException {        
        final FileReference source = (FileReference) 
                ctx.getBatchContext().get(ctx.getParameterValues().get(PARAM_SOURCE_PATH));
        final FileReference target = (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);
                
        
        try {
            target.getService().create(target.getNextVersion(), 
                    source.getService().read(source.getLatestVersion()));
        } catch (IOException e) {
            throw new TaskException(e);
        }
        
        return new TaskResult() {

            @Override
            public void commit() throws TaskException {
                try {
                    target.getService().delete(target.getLatestVersion());
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }

            @Override
            public void rollback() throws TaskException {
                try {
                    target.getService().delete(target.getNextVersion());
                } catch (IOException e) {
                    throw new TaskException(e);
                }
            }
            
        };
    }

    @Override
    public void cleanup(TaskContext ctx) throws TaskException {
        final FileReference target = (FileReference) ctx.getParameterValues().get(PARAM_TARGET_PATH);
                
        try {
            target.getService().delete(target.getLatestVersion());
        } catch (IOException e) {
            throw new TaskException(e);
        }
    }

}
