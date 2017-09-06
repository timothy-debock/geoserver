package org.geoserver.taskmanager.util;

import javax.annotation.PostConstruct;

import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.geoserver.taskmanager.tasks.CopyTableTaskTypeImpl;
import org.geoserver.taskmanager.tasks.CreateViewTaskTypeImpl;
import org.geoserver.taskmanager.tasks.DbRemotePublicationTaskTypeImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Lazy(false)
public class TestInitializer {
    
    
    @Autowired
    private TaskManagerDao dao;
    
    @Autowired
    private TaskManagerFactory fac;
    
    @Autowired
    private TaskManagerDataUtil util;

    @Autowired
    private TaskManagerTaskUtil tutil;
    
    @Autowired
    private BatchJobService bjService;
    
    @PostConstruct
    public void init() {
        Configuration config = fac.createConfiguration();  
        config.setTemplate(true);
        config.setName("my_template");
        config.setWorkspace("some_ws");
        config.setDescription("configuration dudes");
        
        Task task1 = tutil.initTask(CopyTableTaskTypeImpl.NAME, "task1");
        util.addTaskToConfiguration(config, task1);
        
        Task task2 = tutil.initTask(CreateViewTaskTypeImpl.NAME, "task2");
        util.addTaskToConfiguration(config, task2);
        
        Task task3 = tutil.initTask(DbRemotePublicationTaskTypeImpl.NAME, "task3");
        util.addTaskToConfiguration(config, task3);
        
        config = dao.save(config);
        
        
        task1 = config.getTasks().get("task1");
        task2 = config.getTasks().get("task2");
        task3 = config.getTasks().get("task3");
        
        
        Batch batch = fac.createBatch();
        
        batch.setName("my_batch");
        util.addBatchElement(batch, task1);
        util.addBatchElement(batch, task2);      
        util.addBatchElement(batch, task3);        
        
        util.addBatchToConfiguration(config, batch);
        
        batch = bjService.saveAndSchedule(batch);
    }

}
