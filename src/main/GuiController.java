package main;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.RadioButton;

public class GuiController implements Initializable {
	@FXML
	private RadioButton connRBtn = new RadioButton();
	private boolean connected = false;
	@FXML
	private RadioButton disConnRBtn = new RadioButton();
	private boolean disConnected = true;
	
//	private static GuiController instance;
	
	public GuiController() {
		System.out.println("Initialised GuiController");
//		instance = this;
	}
	
//	public static GuiController getInstance() {
//		return instance;
//	}
	
	
	// TODO: Have a value for when the function is off/on. 
	// Make sure you can't execute things, when it's disabled
	/**
	 * launches connected functionality
	 * ie. loading graph values, etc
	 */
	@FXML
	private void selectConnected() {
		if (connected) {
			connRBtn.setDisable(false);
			System.out.println("CONNECTING");
		} else {
			connRBtn.setDisable(true);
			System.out.println("Uh-oh need to have that optical cable connected");
		}
	}
	
	@FXML 
	private void selectDisconnected() {
		// DISCONNECTED STUFF
		System.out.println("You can disconnect");
	}
	
	private void testConnection() {
		// Faking that it's connected
		connected = false;
		connRBtn.setDisable(true);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// TODO Auto-generated method stub
		
		//TODO: DO A PING TEST TO SEE IF THERE'S A CONNECTION.
		testConnection();
		System.out.println("connected: " + connected);
	}

}
