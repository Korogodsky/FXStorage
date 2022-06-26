
package fxstorage;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import javafx.concurrent.Task;

public class FileLoaderTask extends Task<Void>{

    private final Connection dbConnection;
    private final String fileName;
    private final Thread self;
    private final FormController controller;

    public FileLoaderTask(String fileName, Connection dbConnection, FormController controller) {
        this.dbConnection = dbConnection;
        this.fileName = fileName;
        self = new Thread(this);
        this.controller = controller;
    }
    
    public void upload(){
        self.start();
    }
    
    private void uploadFile() {

        controller.appendMessage("Uploading " + fileName);

        String id = UUID.randomUUID().toString();
        controller.showImage(fileName, id);
        
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {}

        PreparedStatement prepStatement;
    
        InputStream inStream;
        try {
            inStream = new FileInputStream(fileName);
        } catch (IOException ex) {
            controller.appendMessage(ex.getMessage());
            controller.activateMessage();
            return;
        }

        try {

            prepStatement = dbConnection.prepareStatement("INSERT INTO images (id, description, data) VALUES (?, ?, ?)");
            prepStatement.setString(1, id);
            prepStatement.setString(2, fileName);
            prepStatement.setBinaryStream(3, inStream);

            prepStatement.executeUpdate();
            prepStatement.close();

        } catch (SQLException ex) {
            controller.appendMessage(ex.getMessage());
            controller.activateMessage();
            return;
        }

        try {            
            inStream.close();
        } catch (IOException ex) {
            controller.appendMessage(ex.getMessage());
            controller.activateMessage();
            return;
        }
        
        controller.selectImage();
        controller.appendMessage("Uploaded " + fileName);
        
    }

    @Override
    protected Void call() throws Exception {
        uploadFile();
        return null;
    }

    @Override
    protected void running() {
        Progress.getInstance().incTotal(controller);
    }
    
    @Override
    protected void failed() {
        Progress.getInstance().incDone(controller);
    }

    @Override
    protected void cancelled() {
        Progress.getInstance().incDone(controller);
    }

    @Override
    protected void succeeded() {
        Progress.getInstance().incDone(controller);
    }

}
