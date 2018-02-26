/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Map;

import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Task;

/**
 * Task Context used during batch run or task clean-up.
 * 
 * @author Niels Charlier
 *
 */
public interface TaskContext {

    /**
     * @return the task
     */
    Task getTask();

    /**     
     * @return the batch run, null if this is a clean-up
     */
    BatchRun getBatchRun();

    /**
     * 
     * @return the parameter values, lazy loaded from task and configuration.
     * 
     * @throws TaskException
     */
    Map<String, Object> getParameterValues() throws TaskException;

    /**
     * During run, tasks create temporary objects that are committed to real
     * objects during the commit phase (such as a table name)
     * This maps real objects to temporary objects during a single batch run.
     * Tasks should save and look up temporary objects so that tasks within a batch can
     * work together.
     * 
     * @return the temp values for this batch run, null if this is a clean-up
     * 
     */
    Map<Object, Object> getTempValues();

    /**
     * Tasks can call this function to check if the user wants to interrupt the batch
     * and interrupt themselves.
     * If they do, they should still return a TaskResult that implements a roll back
     * of what was already done.
     * 
     * @return whether the batch run should be interrupted, false if this is a clean-up
     */
    boolean isInterruptMe();

}