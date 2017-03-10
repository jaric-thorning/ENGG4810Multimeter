package main;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiView extends Application {
	private String fxmlFileName = "/gui_test.fxml";
	private String GuiTitle = "Digital Multimeter Mark 1.0";
	
	public GuiView() {
		
		System.out.println("Initialised GuiView");
	}
	
	// PUT IN LISTENERS FOR RE-SIZING THE ELEMENTS ON THE SCREEN

	@Override
	public void start(Stage primaryStage) throws Exception {
		// Load the FXML file
		FXMLLoader loader = new FXMLLoader(this.getClass().getResource(fxmlFileName));
		
		// Set up the scene and add it to the stage
		Scene scene  = new Scene(loader.load()); // can customise height
		primaryStage.setScene(scene);
		primaryStage.setResizable(true); // enable maximisation of screen
		
		// Set window title
		primaryStage.setTitle(GuiTitle);
		
		// Display the GUI
		primaryStage.show();
	}
	

}
