package de.trafficsim.gui;

import de.trafficsim.gui.graphics.Area;
import de.trafficsim.logic.streets.Street;
import de.trafficsim.logic.vehicles.Vehicle;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.AnchorPane;

public class GuiController {

    private static GuiController instance;

    @FXML
    AnchorPane paneCanvas;

    @FXML
    CheckBox checkShowTracks;

    @FXML
    CheckBox checkShowBoundingBox;

    @FXML
    CheckBox checkShowHitBox;


    private Area area;

    public void start() {
        area = new Area();
        paneCanvas.getChildren().add(area);
        area.widthProperty().bind(paneCanvas.widthProperty().subtract(20));
        area.heightProperty().bind(paneCanvas.heightProperty().subtract(20));

        checkShowTracks.setOnAction(event -> area.setShowTracks(checkShowTracks.isSelected()));
        checkShowBoundingBox.setOnAction(event -> area.setShowBoundingBox(checkShowBoundingBox.isSelected()));
        checkShowHitBox.setOnAction(event -> area.setShowHitBox(checkShowHitBox.isSelected()));
    }


    public void update(double delta) {
        area.draw(delta);
    }

    public void addStreet(Street street) {
        area.addStreet(street);
    }

    public void removeStreet(Street street) {
        area.removeStreet(street);
    }

    public void addVehicle(Vehicle vehicle) {
        area.addVehicle(vehicle);
    }

    public void removeVehicle(Vehicle vehicle) {
        area.removeVehicle(vehicle);
    }

    public static GuiController getInstance() {
        if (instance == null) {
            instance = new GuiController();
        }
        return instance;
    }
}
