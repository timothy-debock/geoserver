/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.schedule;

import java.util.Map;

import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Task;

/**
 * Task Context
 * 
 * @author Niels Charlier
 *
 */
public interface TaskContext {

    Task getTask();

    BatchRun getBatchRun();

    Map<String, Object> getParameterValues() throws TaskException;

    Map<Object, Object> getTempValues();

    boolean isInterruptMe();

}