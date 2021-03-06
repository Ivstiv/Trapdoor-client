package controllers;

import com.google.gson.JsonObject;
import communication.ServerConnection;
import core.ServiceLocator;
import data.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import misc.RichText;

import java.net.URL;
import java.util.ResourceBundle;

public class ConnectController implements Initializable {

    @FXML private Button cancelBtn, saveBtn, connectBtn;
    @FXML private GridPane pane;
    @FXML private TextField username, password, ipField, port;
    private Stage stage;
    private String ip;
    private MainController connectMain; // need it for refference to the dropdown to edit the list of items

    public ConnectController(Stage stage, String ip, MainController connectMain) {
        this.stage = stage;
        this.ip = ip;
        this.connectMain = connectMain;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        DataLoader dl = ServiceLocator.getService(DataLoader.class);

        username.setText(Config.getString("username"));

        cancelBtn.setOnAction(event -> stage.close());

        if(!ip.equals("New connection")) {
            saveBtn.setText("DELETE FROM LIST");
            SavedConnection sc = dl.getSavedConnections().get(ip);
            username.setText(sc.getUsername());
            password.setText(sc.getPassword());
            ipField.setText(sc.getIp());
            port.setText(sc.getPort()+"");
        }

        saveBtn.setOnMouseClicked(event -> {
            if(ip.equals("New connection")) {
                SavedConnection sc = new SavedConnection(
                        ipField.getText(),
                        Integer.parseInt(port.getText()),
                        username.getText(),
                        password.getText()
                );
                dl.addSavedConnection(sc);
                MenuItem i = new MenuItem(sc.getIp());
                i.setOnAction(event2 -> {
                    MenuItem click = (MenuItem) event2.getSource();
                    connectMain.openConnectWindow(click.getText());
                });
                // if the button has already this entry dont add a new one
                class Wrapper {
                    boolean exist;
                }
                Wrapper wrapper = new Wrapper(); // the exist should be effectively final
                connectMain.connectBtn.getItems().forEach((menuItem) -> {
                    if(menuItem.getText().equals(sc.getIp()))
                        wrapper.exist= true;
                });
                if(!wrapper.exist)
                    connectMain.connectBtn.getItems().add(i);
            }else{
                SavedConnection sc2 = new SavedConnection(
                        ipField.getText(),
                        Integer.parseInt(port.getText()),
                        username.getText(),
                        password.getText()
                );
                dl.removeSavedConnection(sc2);
                connectMain.connectBtn.getItems().forEach((menuItem) -> {
                    if(menuItem.getText().equals(sc2.getIp()))
                        Platform.runLater(() ->
                                connectMain.connectBtn.getItems().remove(menuItem)
                        );
                });
                stage.close();
            }
        });

        connectBtn.setOnAction(event -> {
            connectMain.clearChat();

            // stop and remove previous connection
            if(ServiceLocator.hasService(ServerConnection.class)) {
              ServiceLocator.getService(ServerConnection.class).close(true);
              ServiceLocator.removeService(ServerConnection.class);
            }

            String msg = String.format("%s (%s). . .", dl.getMessage("trying"), ipField.getText());
            connectMain.setStatusBar(new RichText(msg));
            stage.close();
            ServerConnection con = new ServerConnection(
                    ipField.getText(),
                    Integer.parseInt(port.getText()),
                    username.getText(),
                    password.getText());
            con.connect();
            ServiceLocator.initialiseService(con);
            JsonObject content = new JsonObject();
            content.addProperty("username", username.getText());
            content.addProperty("password", password.getText());
            Request connect = new Request(RequestType.CONNECT, content);
            con.sendRequest(connect);
        });

        // submit with enter
        pane.setOnKeyPressed((event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                event.consume();
                connectBtn.fire();
            }
        });


        // Make the window draggable
        Offset offset = new Offset();
        pane.setOnMousePressed(event -> {
                offset.x = stage.getX() - event.getScreenX();
                offset.y = stage.getY() - event.getScreenY();
            }
        );
        pane.setOnMouseDragged(event -> {
                stage.setX(event.getScreenX() + offset.x);
                stage.setY(event.getScreenY() + offset.y);
                stage.getScene().setCursor(Cursor.CLOSED_HAND);
            }
        );
        pane.setOnMouseReleased(event -> stage.getScene().setCursor(Cursor.DEFAULT));
    }
}
