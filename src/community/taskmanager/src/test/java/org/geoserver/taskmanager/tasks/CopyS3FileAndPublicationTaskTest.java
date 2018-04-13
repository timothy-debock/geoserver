/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.tasks;

import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import org.geoserver.data.CatalogWriter;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.beans.TestTaskTypeImpl;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.external.ExternalGS;
import org.geoserver.taskmanager.fileservice.FileService;
import org.geoserver.taskmanager.fileservice.impl.S3FileServiceImpl;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.util.LookupService;
import org.geoserver.taskmanager.util.TaskManagerDataUtil;
import org.geoserver.taskmanager.util.TaskManagerTaskUtil;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test why the lock happens on the minio s3 service when tying to copy a geotiff larger then 60 MB.
 *
 * To run this test you should have  2 s3 servers configured in s3.properties file.
 * e.g. test.s3.endpoint=http://172.21.128.97:9000
 * and rasterpubliekminio.s3.endpoint=http://172.21.128.97:9000
 *
 test.s3.endpoint=http://127.0.0.1:9000
 test.s3.user=7LTMPHY39K6WWTNA0GS4
 test.s3.password=zOcucDSyszV8JxtuPZoo7K1PLzfKD5QPJaNIh5Bn

 rasterpubliekminio.s3.endpoint=http://127.0.0.1:9000
 rasterpubliekminio.s3.user=7LTMPHY39K6WWTNA0GS4
 rasterpubliekminio.s3.password=zOcucDSyszV8JxtuPZoo7K1PLzfKD5QPJaNIh5Bn

 *
 * An empty geoserver should be running on http://localhost:9090/geoserver,
 * i should contain the s3geotiff module with same s3.properties as this test.
 *
 * De test will hang when trying to commit if the source file is larger than 60MB.
 *
 * @author Timothy
 */
@Ignore
public class CopyS3FileAndPublicationTaskTest extends AbstractTaskManagerTest {

    private final static Logger LOGGER = Logger.getLogger("CopyS3FileTaskTest");

    //configure these constants
    private static QName REMOTE_COVERAGE = new QName("gs", "mylayer", "gs");
    private static String REMOTE_COVERAGE_TYPE = "S3GeoTiff";

    private static String SOURCE_ALIAS = "test";
    private static String SOURCE_FILE = "test/carbon_sd_31370_40.tif";
    private static String SOURCE_URL = SOURCE_ALIAS + "://" + SOURCE_FILE;

    private static String TARGET_ALIAS = "rasterpubliekminio";
    private static String TARGET_FILE = "new/carbon_sd_31370_40.tif";
    private static String TARGET_URL = TARGET_ALIAS +"://" + TARGET_FILE;

    private static final String ATT_SOURCE = "source";
    private static final String ATT_TARGET = "target";

    private static final String ATT_LAYER = "layer";
    private static final String ATT_EXT_GS = "geoserver";
    /*private static final String ATT_FILE_SOURCE = "file-source";
    private static final String ATT_FILE_TARGET = "file-target";*/

    @Autowired
    private LookupService<ExternalGS> extGeoservers;

    @Autowired
    private TaskManagerDao dao;
    
    @Autowired
    private TaskManagerFactory fac;
    
    @Autowired
    private TaskManagerDataUtil dataUtil;

    @Autowired
    private TaskManagerTaskUtil taskUtil;
    
    @Autowired
    private BatchJobService bjService;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private LookupService<FileService> fileServices;

    private Configuration config;
    
    private Batch batch;

    @Override
    public boolean setupDataDirectory() throws Exception {
        try {
            FileService fileService = fileServices.get(S3FileServiceImpl.S3_NAME_PREFIX +
                    SOURCE_ALIAS);
            Assume.assumeNotNull(fileService);
            Assume.assumeTrue("File exists on s3 service",
                    fileService.checkFileExists(SOURCE_FILE));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Assume.assumeTrue("S3 service is configured and available", false);
        }

        DATA_DIRECTORY.addWcs11Coverages();
        Map<String, String> params = new HashMap<String, String>();
        params.put(CatalogWriter.COVERAGE_TYPE_KEY, REMOTE_COVERAGE_TYPE);
        params.put(CatalogWriter.COVERAGE_URL_KEY, SOURCE_URL);
        DATA_DIRECTORY.addCustomCoverage(REMOTE_COVERAGE, params);
        return true;
    }

    @Before
    public void setupBatch() throws Exception {
        Assume.assumeTrue(extGeoservers.get("mygs").getRESTManager().getReader().existGeoserver());

        try {
            FileService fileService = fileServices.get(S3FileServiceImpl.S3_NAME_PREFIX + SOURCE_ALIAS);
            Assume.assumeNotNull(fileService);
            Assume.assumeTrue("File exists on s3 service",
                    fileService.checkFileExists(SOURCE_FILE));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            Assume.assumeTrue("S3 service is configured and available", false);
        }
        
        config = fac.createConfiguration();  
        config.setName("my_config");
        config.setWorkspace("some_ws");
        
        Task task1 = fac.createTask();
        task1.setName("task1");
        task1.setType(CopyS3FileTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task1, CopyS3FileTaskTypeImpl.PARAM_SOURCE, ATT_SOURCE);
        dataUtil.setTaskParameterToAttribute(task1, CopyS3FileTaskTypeImpl.PARAM_TARGET, ATT_TARGET);
        dataUtil.addTaskToConfiguration(config, task1);
        
        config = dao.save(config);
        task1 = config.getTasks().get("task1");


        //Publication task
        Task task2 = fac.createTask();
        task2.setName("task2");
        task2.setType(FileRemotePublicationTaskTypeImpl.NAME);
        dataUtil.setTaskParameterToAttribute(task2, FileRemotePublicationTaskTypeImpl.PARAM_LAYER, ATT_LAYER);
        dataUtil.setTaskParameterToAttribute(task2, FileRemotePublicationTaskTypeImpl.PARAM_EXT_GS, ATT_EXT_GS);
        dataUtil.setTaskParameterToAttribute(task2, FileRemotePublicationTaskTypeImpl.PARAM_FILE, ATT_TARGET);
        dataUtil.addTaskToConfiguration(config, task2);

        config = dao.save(config);
        task2 = config.getTasks().get("task2");

        //create batch
        batch = fac.createBatch();
        
        batch.setName("my_batch");
        dataUtil.addBatchElement(batch, task1);
        dataUtil.addBatchElement(batch, task2);

        batch = bjService.saveAndSchedule(batch);

    }

    @After
    public void clearDataFromDatabase() {
        if (batch != null) {
            dao.delete(batch);
        }
        if (config != null) {
            dao.delete(config);
        }
    }

    @Test
    public void testSuccessAndCleanup() throws SchedulerException, SQLException, IOException {
                
        dataUtil.setConfigurationAttribute(config, ATT_SOURCE, SOURCE_URL);
        dataUtil.setConfigurationAttribute(config, ATT_TARGET, TARGET_URL);
        dataUtil.setConfigurationAttribute(config, ATT_LAYER,
                REMOTE_COVERAGE.getPrefix() + ":" + REMOTE_COVERAGE.getLocalPart());
        dataUtil.setConfigurationAttribute(config, ATT_EXT_GS, "mygs");

        config = dao.save(config);
        
        Trigger trigger = TriggerBuilder.newTrigger()
                .forJob(batch.getId().toString())
                .startNow()
                .build();
        scheduler.scheduleJob(trigger);
        
        while (scheduler.getTriggerState(trigger.getKey()) != TriggerState.NONE) {}
        
        FileService fileService = fileServices.get(S3FileServiceImpl.S3_NAME_PREFIX + TARGET_ALIAS);

        GeoServerRESTManager restManager = extGeoservers.get("mygs").getRESTManager();
        assertTrue(fileService.checkFileExists(TARGET_FILE));
        assertTrue(restManager.getReader().existsCoveragestore(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertTrue(restManager.getReader().existsCoverage(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(),
                REMOTE_COVERAGE.getLocalPart()));
        assertTrue(restManager.getReader().existsLayer(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));

        assertTrue(taskUtil.cleanup(config));
        
        assertTrue(taskUtil.cleanup(config));      

        assertFalse(fileService.checkFileExists(TARGET_FILE));


        assertFalse(restManager.getReader().existsCoveragestore(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart()));
        assertFalse(restManager.getReader().existsCoverage(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(),
                REMOTE_COVERAGE.getLocalPart()));
        assertFalse(restManager.getReader().existsLayer(
                REMOTE_COVERAGE.getPrefix(), REMOTE_COVERAGE.getLocalPart(), true));
    }
    

}
