/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.report.impl;

import java.util.Date;

import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Run;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.report.Report;
import org.geoserver.taskmanager.report.ReportBuilder;
import org.geoserver.taskmanager.report.Report.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A very simple report builder.
 * Contains all information that matters in simple text format.
 * 
 * @author Niels Charlier
 *
 */
@Service
public class SimpleReportBuilderImpl implements ReportBuilder {

    @Autowired
    private TaskManagerDao dao;

    @Override
    @Transactional
    public Report buildBatchReport(String batchName) {
        StringBuilder reportContent = new StringBuilder();
        
        Batch batch = dao.getBatch(batchName);
                
        Date lastStart = null;
        Run lastRun = null;
        for (BatchElement element : batch.getElements()) {
            Task task = element.getTask();
            if (element.getRuns().size() == 0) {
                break; //end of the (first) run
            }
            
            Run run = element.getRuns().get(element.getRuns().size() - 1);
            if (lastStart != null && run.getStart().before(lastStart)) {
                break; //this is a previous run, end of the run
            }
            
            lastRun = run;
            
            reportContent.append(task.getFullName() + ", started " + run.getStart() + ", ended " + 
                    run.getEnd() + ", status is " + run.getStatus() + "\n");
            if (run.getMessage() != null) {
                reportContent.append("\tmessage: " + run.getMessage() + " (check logs for more details) \n");
            }
                                
        }
        
        StringBuilder reportTitle = new StringBuilder("Report: Batch " + batchName + " ");
        Type type;
        
        switch (lastRun.getStatus()) {
        case FAILED:
            reportTitle.append("has failed");
            type = Type.FAILED;
            break;
        case ROLLED_BACK:
            reportTitle.append("was cancelled");
            type = Type.CANCELLED;
            break;
        default:
            reportTitle.append("was successful");
            type = Type.SUCCESS;
        }
        
        return new Report(reportTitle.toString(), reportContent.toString(), type);
    }

}
