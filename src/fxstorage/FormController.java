
package fxstorage;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.Callback;

public class FormController implements Initializable {
    
    @FXML private TableView<ImageDescription> imageTable;
    @FXML private TableColumn descriptionCol;
    @FXML private TableColumn idCol;
    
    @FXML private ImageView image;
    @FXML private ComboBox driverList;
    @FXML private TextArea messageArea;
    @FXML private TabPane root;
    @FXML private Tab settingsTab;
    @FXML private ProgressBar progressBar;
    @FXML private TextField ip;
    @FXML private TextField port;
    @FXML private TextField user;
    @FXML private TextField password;
    
    private final ObservableList imageList = FXCollections.observableArrayList();
    private Connection dbConnection;
    private Statement sqlStatement;
    private PreparedStatement prepStatement;
    private static final Progress progress = Progress.getInstance();
            
    @FXML
    private void addImage(ActionEvent event) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose image");
        
        fileChooser.getExtensionFilters().addAll(
            new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
            new ExtensionFilter("All Files", "*.*"));
        
        File selectedFile = fileChooser.showOpenDialog(null);
    
        if (selectedFile != null) {
            FileLoaderTask fLoader = new FileLoaderTask(selectedFile.getAbsolutePath(), dbConnection, this);
            fLoader.upload();
        }

    }
    
    @FXML
    private void removeImage(ActionEvent event) {
        delImage();
    }
    
    @FXML
    public void selectImage(){
        
        try {
            prepStatement = dbConnection.prepareStatement("SELECT data FROM images WHERE id = ?");
            prepStatement.setString(1, imageTable.getSelectionModel().getSelectedItem().getID());
            ResultSet rs = prepStatement.executeQuery();

            if(rs.next()){
                image.setImage(new Image(rs.getBinaryStream("data")));
            }else{
                image.setImage(new Image(getClass().getResourceAsStream("load.gif")));
            }
            
            prepStatement.close();
        } catch (SQLException ex) {
            appendMessage(ex.getMessage());
        }
    }
    
    @FXML
    private void onKeyReleased(){
        selectImage();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        Callback<TableColumn, TableCell> cellFactory = new Callback<TableColumn, TableCell>() {
            @Override
            public TableCell call(TableColumn p) {
                return new EditingCell();
            }
        };
        
        descriptionCol.setCellValueFactory(new PropertyValueFactory<ImageDescription, String>("description"));
        descriptionCol.setCellFactory(cellFactory);
        descriptionCol.setOnEditCommit(
            new EventHandler<CellEditEvent<ImageDescription, String>>() {
                @Override
                public void handle(CellEditEvent<ImageDescription, String> t) {
                    ((ImageDescription) t.getTableView().getItems().get(
                        t.getTablePosition().getRow())
                        ).setDescription(t.getNewValue());
                    updateDescription();
                }
             }
        );

        idCol.setCellValueFactory(new PropertyValueFactory<ImageDescription, String>("ID"));
        
        addButton();
        
        imageTable.setItems(imageList);
        imageTable.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if(event.getCode().equals(KeyCode.DELETE)){
                    delImage();
                }
            }
        }
        );
        
        ObservableList<String> driverOptions = FXCollections.observableArrayList();

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while(drivers.hasMoreElements()){
            driverOptions.add(drivers.nextElement().toString().replaceAll("@.*", ""));
        }
        
        if(!driverOptions.isEmpty()){
            driverList.setItems(driverOptions);
            driverList.getSelectionModel().select(0);
        }
                
        ip.setText("localhost");
        port.setText("3306");
        user.setText("root");
        password.setText("root");
        
        if(!setDBConnection()) activateMessage();

        progressBar.progressProperty().bind(progress.progressProperty());

    }
    
    private void addButton() {
        TableColumn<ImageDescription, Void> colBtn = new TableColumn();

        Callback<TableColumn<ImageDescription, Void>, TableCell<ImageDescription, Void>> cellFactory
                = new Callback<TableColumn<ImageDescription, Void>, TableCell<ImageDescription, Void>>() {
            @Override
            public TableCell<ImageDescription, Void> call(final TableColumn<ImageDescription, Void> param) {
                final TableCell<ImageDescription, Void> cell = new TableCell<ImageDescription, Void>() {

                    private final Button btn = new Button("X");

                    {
                        btn.setOnAction((ActionEvent event) -> {
                            ImageDescription data = getTableView().getItems().get(getIndex());
                            delImage();
                            selectImage();
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };

        colBtn.setCellFactory(cellFactory);

        imageTable.getColumns().add(colBtn);

    }

    @FXML
    private void setConnection(ActionEvent event) {
        setDBConnection();
    }
    
    public boolean setDBConnection(){
        if(!loadDriver()){
            appendMessage("Data base connector not found!");
            return false;
        }
        
        String protocol = "mysql";
        
        String connectionStr = "jdbc:"
                                + protocol + "://"
                                + ip.getText()
                                + ":" + port.getText()
                                + "/?user=" + user.getText()
                                + "&password=" + password.getText();

        try {
            dbConnection = DriverManager.getConnection(connectionStr);
            appendMessage("Connection established.");
        } catch (SQLException ex) {
            appendMessage("Cannot connect! " + ex.getMessage());
            return false;
        }
        
        if(!checkDB()){return false;}

        return selectImages();
    }

    public void showImage(String fileName, String id){
        
        ImageDescription iDescription = new ImageDescription(fileName, id);
        imageList.add(iDescription);
        imageTable.getSelectionModel().select(iDescription);
        selectImage();

    }
    
    public void appendMessage(String message){
        messageArea.appendText(message + "\n");
    }
    
    public void activateMessage(){
        root.getSelectionModel().select(settingsTab);
    }
    
    public void showProgress(){
        progressBar.setVisible(true);
    }
    
    public void hideProgress(){
        progressBar.setVisible(false);
    }

    public boolean loadDriver() {
        try {
            Class.forName(driverList.getSelectionModel().getSelectedItem().toString());
        } catch (ClassNotFoundException ex) {return false;}
        
        return true;
    }
    
    public boolean checkDB(){
        try {
            sqlStatement = dbConnection.createStatement();
            sqlStatement.executeUpdate("CREATE DATABASE IF NOT EXISTS imageDB");
            sqlStatement.executeUpdate("USE imageDB");
            sqlStatement.executeUpdate("CREATE TABLE IF NOT EXISTS images (id varchar(36), description varchar(250), data BLOB)");
            sqlStatement.close();
        } catch (SQLException ex) {
            appendMessage(ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    private void delImage(){
        try {
            prepStatement = dbConnection.prepareStatement("DELETE FROM images WHERE id = ?");
            prepStatement.setString(1, imageTable.getSelectionModel().getSelectedItem().getID());
            prepStatement.execute();
            prepStatement.close();

            imageList.remove(imageTable.getSelectionModel().getSelectedItem());
        } catch (SQLException ex) {
            appendMessage(ex.getMessage());
        }
    }
    
    private void updateDescription(){
        try {
            prepStatement = dbConnection.prepareStatement("UPDATE images SET description = ? WHERE id = ?");
            prepStatement.setString(1, imageTable.getSelectionModel().getSelectedItem().getDescription());
            prepStatement.setString(2, imageTable.getSelectionModel().getSelectedItem().getID());
            prepStatement.execute();
            prepStatement.close();
        } catch (SQLException ex) {
            appendMessage(ex.getMessage());
        }        
    }
    
    public boolean selectImages(){
        try {
            prepStatement = dbConnection.prepareStatement("SELECT id, description FROM images");
            ResultSet rs = prepStatement.executeQuery();
            while(rs.next()){
                imageList.add(new ImageDescription(rs.getString("description"), rs.getString("id")));
            }
            prepStatement.close();
        } catch (SQLException ex) {
            appendMessage(ex.getMessage());
        }
        
        return true;
    }

}
