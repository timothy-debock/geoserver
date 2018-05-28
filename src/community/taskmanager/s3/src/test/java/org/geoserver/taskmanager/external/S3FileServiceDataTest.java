/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.external;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import org.apache.commons.io.IOUtils;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.external.FileService;
import org.geoserver.taskmanager.external.impl.FileServiceImpl;
import org.geoserver.taskmanager.external.impl.S3FileServiceImpl;
import org.geoserver.taskmanager.util.LookupService;
import org.geotools.util.logging.Logging;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Logger;


/**
 * Test data methods.
 *
 * @author Timothy De Bock - timothy.debock.github@gmail.com
 */
public class S3FileServiceDataTest extends AbstractTaskManagerTest {

    private final static Logger LOGGER = Logging.getLogger(S3FileServiceDataTest.class);

    @Autowired
    LookupService<FileService> fileServiceRegistry;

    @Test
    public void testFileRegistry() {
        Assert.assertEquals(4, fileServiceRegistry.names().size());
        
        FileService fs = fileServiceRegistry.get("s3-test-source");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof S3FileServiceImpl);
        Assert.assertEquals("http://127.0.0.1:9000",
                ((S3FileServiceImpl) fs).getEndpoint());
        Assert.assertEquals("source",
                ((S3FileServiceImpl) fs).getRootFolder());

        fs = fileServiceRegistry.get("s3-test-target");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof S3FileServiceImpl);
        Assert.assertEquals("http://127.0.0.1:9000",
                ((S3FileServiceImpl) fs).getEndpoint());
        Assert.assertEquals("target",
                ((S3FileServiceImpl) fs).getRootFolder());
        
        fs = fileServiceRegistry.get("temp-directory");
        Assert.assertNotNull(fs);
        Assert.assertTrue(fs instanceof FileServiceImpl);
        Assert.assertEquals("/tmp", ((FileServiceImpl) fs).getRootFolder());
    }
    
    /**
     * This test assumes access to aws compatible service.
     *
     * @throws IOException
     */
    @Test
    public void testFileServiceS3CreateSubFolders() throws IOException {
        S3FileServiceImpl service = getS3FileService();

        String filename = System.currentTimeMillis() + "-test.txt";
        String filenamePath = "new-bucket/New_Folder/" + filename;

        Assert.assertFalse(service.checkFileExists(filenamePath));
        String content = "test the file service";

        //create
        try (InputStream is = IOUtils.toInputStream(content, "UTF-8")) {
            service.create(filenamePath, is);
        }

        //exists
        boolean fileExists = service.checkFileExists(filenamePath);
        Assert.assertTrue(fileExists);

        //read
        try (InputStream is = service.read(filenamePath)) {
            String actualContent = IOUtils.toString(is);
            Assert.assertEquals(content, actualContent);
        }

        //is create in the root folder?
        Assert.assertTrue(getS3Client().doesObjectExist(service.getRootFolder(), filenamePath));

        //delete action
        service.delete(filenamePath);
        Assert.assertFalse(service.checkFileExists(filenamePath));

    }


    @Test
    public void testListSubFoldersS3() throws IOException {
        FileService service = getS3FileService();
        String rootFolder = "tmp" + System.currentTimeMillis();
        ((S3FileServiceImpl) service).setRootFolder(rootFolder);

        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/a.txt", content);
        }
        
        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/bar/b.txt", content);
        }
        
        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("foo/bar/foobar/barfoo/c.txt", content);
        }
        
        try (InputStream content = IOUtils.toInputStream("test the file service", "UTF-8")) {
            service.create("hello/d.txt", content);
        }
        
        List<String> folders = service.listSubfolders();

        Assert.assertEquals(5, folders.size());
        Assert.assertTrue(folders.contains("foo"));
        Assert.assertTrue(folders.contains("foo/bar"));
        Assert.assertTrue(folders.contains("foo/bar/foobar/barfoo"));
        Assert.assertTrue(folders.contains("foo/bar/foobar"));
        Assert.assertTrue(folders.contains("hello"));

        service.delete(rootFolder);

    }
    /**
     * Add the properties to your S3 service here.
     * @return
     */
    private S3FileServiceImpl getS3FileService() {
        S3FileServiceImpl s3FileService = 
                new S3FileServiceImpl(
                "http://127.0.0.1:9000",
                "P3Z48TR2OZAZDP8C3P9E",
                "sCNEAhfGtlhA8Mjq1AReBcMl0oMGX1zE3vppQRXB",
                "alias",
                "source"
        );
        List<String> folders = null;
        try {
            folders = s3FileService.listSubfolders();
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
        Assume.assumeNotNull(folders);

        return s3FileService;
    }


    private AmazonS3 getS3Client() {
        AmazonS3 s3;
        //custom endpoint

        S3FileServiceImpl s3FileService = getS3FileService();
        s3 = new AmazonS3Client(new BasicAWSCredentials(s3FileService.getUser(), s3FileService.getPassword()));

        final S3ClientOptions clientOptions = S3ClientOptions.builder().setPathStyleAccess(true).build();
        s3.setS3ClientOptions(clientOptions);
        String endpoint = s3FileService.getEndpoint();
        if (!endpoint.endsWith("/")) {
            endpoint = endpoint + "/";
        }
        s3.setEndpoint(endpoint);

        return s3;
    }


}
