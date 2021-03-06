package de.trafficsim.logic.streets.tracks;

import de.trafficsim.gui.graphics.AreaGraphicsContext;
import de.trafficsim.logic.streets.Street;
import de.trafficsim.logic.vehicles.Vehicle;
import de.trafficsim.util.Direction;
import de.trafficsim.util.geometry.Position;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

import static de.trafficsim.util.Util.VEHICLE_LENGTH;

public abstract class Track {
    protected final Street street;

    private static int nextID = 0;

    public final int id;

    protected Direction inDir;
    protected Direction outDir;

    protected double length;

    protected Position from;

    protected Position to;

    protected List<Vehicle> vehiclesOnTrack = new ArrayList<>();

    protected List<Track> inTrackList = new ArrayList<>();

    protected List<Track> outTrackList = new ArrayList<>();

    protected boolean selected;

    protected boolean stopPoint;

    protected double stopPointPosition;
    protected boolean stopPointEnabled;

    private TrafficPriorityChecker priorityStopPoint;


    public Track(Position from, Position to, double length, Street street) {
        this.from = from;
        this.to = to;
        this.length = length;
        this.street = street;
        id = nextID;
        nextID++;
    }

    public void connectOutToInOf(Track track) {
        track.inTrackList.add(this);
        outTrackList.add(track);
    }

    public void disconnectOutFromInOf(Track track) {
        track.inTrackList.remove(this);
        outTrackList.remove(track);
    }

    public void disconnectAllOutgoing() {
        for (int i = outTrackList.size()-1; i >= 0 ; i--) {
            disconnectOutFromInOf(outTrackList.get(i));
        }
    }

    public void disconnectAllIngoing() {
        for (int i = inTrackList.size()-1; i >= 0 ; i--) {
            inTrackList.get(i).disconnectOutFromInOf(this);
        }
    }


    /**
     * Rendert Grundlegene Tackvisualisierung (Connection Punkte)
     */
    public void render(AreaGraphicsContext agc) {
        agc.gc.setLineWidth(2*agc.scale);
        double a = 1;
        double b = (a*2)/3;
        if (inTrackList.size() < 1) {
            agc.gc.setStroke(Color.LIME);
            agc.gc.strokeOval(from.x - a, from.y - a, a*2, a*2);
            agc.gc.strokeLine(from.x, from.y, from.x+inDir.vector.x*2, from.y+inDir.vector.y*2);
        }
        if (outTrackList.size() < 1) {
            agc.gc.setStroke(Color.RED);
            agc.gc.strokeOval(to.x - a, to.y - a, a*2, a*2);
            agc.gc.strokeLine(to.x, to.y, to.x+outDir.vector.x*2, to.y+outDir.vector.y*2);
        }


        agc.gc.setStroke(Color.YELLOW);
        if (inTrackList.size() > 0) {
            agc.gc.strokeLine(from.x - b, from.y - b, from.x + b, from.y + b);
            agc.gc.strokeLine(from.x - b, from.y + b, from.x + b, from.y - b);
        }

        if (hasPriorityStopPoint()) {
            Vehicle currentVehicle = priorityStopPoint.getCurrentVehicle();
            agc.gc.setStroke(currentVehicle == null ? Color.LIMEGREEN : Color.ORANGE);
            Position p = getPosOnArea(priorityStopPoint.getStopPointPos()).sub(street.getPosition());
            agc.gc.strokeOval(p.x-1, p.y-1, 2, 2);
        }

        if (!selected) {
            if (vehiclesOnTrack.size() > 0) {
                agc.gc.setStroke(Color.LIME);
                //agc.gc.strokeOval(from.x - a, from.y - a, a*2, a*2);
            } else {
                agc.gc.setStroke(Color.CYAN.deriveColor(0, 1, 1, 0.2));
            }
        } else {
            agc.gc.setStroke(debugColor);
        }


        renderTrack(agc);
        selected = false;
    }

    public void createStopPoint(double position, boolean enabled) {
        stopPoint = true;
        stopPointPosition = position;
        stopPointEnabled = enabled;
    }

    public void enableStopPoint(boolean enabled) {
        stopPointEnabled = enabled;
    }

    /**
     * Track Spezifische Visualisierung
     * @param agc
     */
    protected abstract void renderTrack(AreaGraphicsContext agc);

    public abstract Position getPosOnArea(double pos);

    public double getLength() {
        return length;
    }

    public Position getFrom() {
        return from;
    }
    public Position getTo() {
        return to;
    }
    public List<Track> getInTrackList() {
        return inTrackList;
    }

    public List<Track> getOutTrackList() {
        return outTrackList;
    }

    public abstract double getDirectionOnPos(double currentPosInTrack);

    public void removeVehicle(Vehicle vehicle) {
        vehiclesOnTrack.remove(vehicle);
    }

    public void addVehicle(Vehicle vehicle) {
        vehiclesOnTrack.add(vehicle);
    }

    public Direction getOutDir() {
        return outDir;
    }

    public Direction getInDir() {
        return inDir;
    }

    public List<Vehicle> getVehiclesOnTrack() {
        return vehiclesOnTrack;
    }

    public Street getStreet() {
        return street;
    }

    private Color debugColor = Color.ORANGERED;
    public void select() {
        selected = true;
        debugColor = Color.ORANGERED;
    }

    public void select(Color color) {
        selected = true;
        debugColor = color;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Track{" +
                "id=" + id +
                ",V: " +vehiclesOnTrack.size() +
                '}';
    }

    public boolean isSelected() {
        return selected;
    }

    public boolean isFree() {
        return vehiclesOnTrack.size() < 1;
    }

    public boolean hasStopPoint() {
        return stopPoint;
    }

    public double getStopPointPosition() {
        return stopPointPosition;
    }

    public boolean isStopPointEnabled() {
        return stopPointEnabled;
    }

    public TrafficPriorityChecker getPriorityStopPoint() {
        return priorityStopPoint;
    }

    public void setPriorityStopPoint(TrafficPriorityChecker priorityStopPoint) {
        this.priorityStopPoint = priorityStopPoint;
        street.addPriorityStopPoint(priorityStopPoint);
    }

    public boolean hasPriorityStopPoint() {
        return priorityStopPoint != null;
    }
    
    public double getDistToNextObstacle(Vehicle vehicle) {
        double from;
        if (vehicle.getCurrentTrack() == this) {
            from = vehicle.getCurrentPosInTrack();
        } else {
            from = 0;
        }
        double minDist = Double.POSITIVE_INFINITY;
        if (hasStopPoint()) {
            if (isStopPointEnabled()) {
                double delta = getStopPointPosition() - from;
                //System.out.println("Stop Point Delta = " + delta + " Min Dist = " + minDist);
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                }
            }
        }
        if (hasPriorityStopPoint()) {
            TrafficPriorityChecker checker = getPriorityStopPoint();
            if (!checker.checkFree(vehicle)) {
                double delta = checker.getStopPointPos() - from;
                //System.out.println("Prio Stop Point Delta = " + delta + " Min Dist = " + minDist);
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                }
            }
        }
        //Calculate dist to vehicles on Curr Track
        for (Vehicle vehicleOnTrack : vehiclesOnTrack) {
            if (vehicleOnTrack != vehicle){
                double delta = vehicleOnTrack.getCurrentPosInTrack() - VEHICLE_LENGTH/2 - from ;
                //System.out.println("Delta =" + delta + "Min Dist =" + minDist);
                if(delta > -VEHICLE_LENGTH /2 && delta <= 0){
                    minDist = 0;
                    //System.out.println("Normally this shouldn't happen");
                }
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                }
            }
        }
        return minDist;
    }

    public double getDistToNextVehicle(Vehicle vehicle) {
        double from;
        if (vehicle.getCurrentTrack() == this) {
            from = vehicle.getCurrentPosInTrack();
        } else {
            from = 0;
        }
        double minDist = Double.POSITIVE_INFINITY;
        if (hasStopPoint()) {
            if (isStopPointEnabled()) {
                double delta = getStopPointPosition() - from;
                //System.out.println("Stop Point Delta = " + delta + " Min Dist = " + minDist);
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                }
            }
        }
        //Calculate dist to vehicles on Curr Track
        for (Vehicle vehicleOnTrack : vehiclesOnTrack) {
            if (vehicleOnTrack != vehicle){
                double delta = vehicleOnTrack.getCurrentPosInTrack() - VEHICLE_LENGTH/2 - from ;
                //System.out.println("Delta =" + delta + "Min Dist =" + minDist);
                if(delta > -VEHICLE_LENGTH /2 && delta <= 0){
                    minDist = 0;
                    //System.out.println("Normally this shouldn't happen");
                }
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                }
            }
        }
        return minDist;
    }

    public boolean isAreaFree(double from, double to) {
        for (Vehicle vehicle : vehiclesOnTrack) {
            double pos = vehicle.getCurrentPosInTrack();
            if (pos >= from && pos <= to) {
                return false;
            }
        }
        return true;
    }
}
