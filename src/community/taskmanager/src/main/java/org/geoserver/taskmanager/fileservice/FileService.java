/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.fileservice;

import org.geoserver.taskmanager.util.Named;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;


/**
 * Persist and read files. All actions on this service are relative to the configured rootFolder.
 * 
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 *
 */
public interface FileService extends Serializable, Named {
    
    /**
     * User-friendly description of this file service. 
     * 
     * @return description
     */
        String getDescription();

    /**
     * List existing all nested folders in this file service.
     * e.g.
     *  /foo/
     *  /foo/bar/
     *  /other/
     *
     * @return list of existing sub folders
     * @throws IOException
     */
    List<String> listSubfolders() throws IOException;

    /**
     * Create a file in the file service
     * 
     * @param filePath the path of the file, relative to this service
     * @param content the content of the file
     * @return a location string that can be used to configure a Geoserver store
     * @throws IOException
     */
    String create(String filePath, InputStream content) throws IOException;

    /**
     * Check if this file exists.
     * 
     * @param filePath the path of the file, relative to this service
     * @return whether the file exists
     * @throws IOException
     */
    boolean checkFileExists(String filePath) throws IOException;

    /**
     * Delete this file.
     * 
     * @param filePath the path of the file, relative to this service
     * @return whether anything was actually deleted.
     * @throws IOException
     */
    boolean delete(String filePath) throws IOException;

    /**
     * Read this file.
     * 
     * @param filePath the path of the file, relative to this service
     * @return inputstream with data
     * @throws IOException
     */
    InputStream read(String filePath) throws IOException;

    /**
     * Returns the rootFolder. All actions on the service are relative to the rootFolder.
     * @return the rootFolder.
     */
    String getRootFolder();
}
