/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Map;

import org.geoserver.taskmanager.util.Named;

/**
 * A Task Type.
 * 
 * @author Niels Charlier
 *
 */
public interface TaskType extends Named {
    
    /**
     * Return parameter info for this task type.
     * 
     * @return the parameter info
     */
    public Map<String, ParameterInfo> getParameterInfo();
    
    /**
     * Run a task, based on these parameter values.
     * @param ctx TODO
     * @return the task result
     */
    public TaskResult run(TaskContext ctx) throws TaskException;
    
    /**
     * Do a clean-up for this task (for example, if this task publishes something, remove it).
     * @param ctx TODO
     * @throws TaskException 
     */
    public void cleanup(TaskContext ctx) throws TaskException;

}
