package de.trafficsim.logic.vehicles;

import de.trafficsim.gui.graphics.AreaGraphicsContext;
import de.trafficsim.logic.network.Path;
import de.trafficsim.logic.streets.StreetCross;
import de.trafficsim.logic.streets.StreetRoundAbout;
import de.trafficsim.logic.streets.StreetTJunction;
import de.trafficsim.logic.streets.tracks.Track;
import de.trafficsim.logic.streets.tracks.TrafficPriorityChecker;
import de.trafficsim.util.Util;
import de.trafficsim.util.geometry.Position;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.util.*;

import static de.trafficsim.util.Util.CAR_SIZE;
import static de.trafficsim.util.Util.VEHICLE_LENGTH;

public class Vehicle {
    private static final double MIN_DIST_SIDEWAY = CAR_SIZE*3;
    public static final double MIN_DIST = CAR_SIZE + 2;
    protected int LOOKAHEAD_LIMIT = 1;

    private boolean braking = false;
    protected double velocity = 1.0;
    protected double currentPosInTrack = 0;

    //Maximal Acceleration and Deceleration
    private final double maxAcceleration = 7; // m/s²
    private final double maxDeceleration = 10; // m/s²

    //Calculate the maxVelocity
    public final double maxVelocity = Util.kmhToMs(50); // m/s

    protected Track currentTrack;

    protected Path path;

    //Current index in the path
    private int currentTrackNumber;

    private boolean active = true;

    public Color color;

    public Vehicle(double velocity, Path path){
        this.velocity = velocity;
        switchTrack(path.get(0));
        this.path = path;

        color = Util.getRandomColor();
    }
    String debugBrakeReason = "";

    /**
     * Get the distance to the
     * @param position
     * @param lookDistance
     * @return
     */
    public double getLookAheadDist(double position, double lookDistance){
        debugBrakeReason = "";
        List<Vehicle> vehicles = currentTrack.getVehiclesOnTrack();
        double minDist = Double.POSITIVE_INFINITY;
        boolean obstacleFound = false;
        //Calculate dist to vehicles on Curr Track
        for (Vehicle vehicle : vehicles) {
            if (vehicle != this){
                double delta = vehicle.getCurrentPosInTrack() - VEHICLE_LENGTH/2 - position;
                //System.out.println("Delta =" + delta + "Min Dist =" + minDist);
                if(delta > -VEHICLE_LENGTH /2 && delta <= 0){
                    minDist = 0;
                    //System.out.println("Normally this shouldn't happen");
                }
                if(delta > 0){
                    if (minDist > delta) {
                        minDist = delta;
                    }

                    debugBrakeReason += "Vehicle ";
                    obstacleFound = true;
                }
            }
        }
        //Look at neighbour Tracks
        if(currentTrackNumber + 1 < path.size()){
            for (Track track : currentTrack.getOutTrackList()) {
                Track nextTrack = path.get(currentTrackNumber+1);
                if(track != nextTrack){
                    //track.select();
                    if (!track.getVehiclesOnTrack().isEmpty()){
                        double minDistInOtherTrack = Double.POSITIVE_INFINITY;
                        for (Vehicle vehicle : track.getVehiclesOnTrack()) {
                            if (vehicle.currentPosInTrack - VEHICLE_LENGTH/2 < minDistInOtherTrack) {
                                minDistInOtherTrack = vehicle.currentPosInTrack;
                            }
                        }
                        if (minDistInOtherTrack < CAR_SIZE+2) {
                            double d = ((currentTrack.getLength() - position) + minDistInOtherTrack) - (VEHICLE_LENGTH/2);
                            if (d < minDist) {
                                minDist = d;
                                debugBrakeReason += "SideWayDist ";
                                obstacleFound = true;
                            }
                        }
                        Position posOnPath = nextTrack.getPosOnArea(minDistInOtherTrack);
                        Position posOffPath = track.getPosOnArea(minDistInOtherTrack);
                        double dist = posOnPath.distance(posOffPath);
                        //System.out.println("Dist Sideways First Track= " +dist +"Vehicles ="+this);
                        if(dist < MIN_DIST_SIDEWAY){
                            double d = (currentTrack.getLength()-position) + CAR_SIZE + 1;
                            if (d < minDist) {
                                debugBrakeReason += "SideWay ";
                                obstacleFound = true;
                                minDist = d;
                            }
                        }
                    }
                }
            }
        }
        //Calculate dist to StopPoint on CurrTrack
        if (currentTrack.hasStopPoint()) {
            if (currentTrack.isStopPointEnabled() || !(checkIfStopPointPassPossible(currentTrack))) {
                double delta = currentTrack.getStopPointPosition() - position;
                //System.out.println("Stop Point Delta = " + delta + " Min Dist = " + minDist);
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                    debugBrakeReason = "Stop ";
                    obstacleFound = true;
                }
            }
        }
        if (currentTrack.hasPriorityStopPoint()) {
            TrafficPriorityChecker checker = currentTrack.getPriorityStopPoint();
            if (!checker.checkFree(this)) {
                double delta = checker.getStopPointPos() - position;
                //System.out.println("Prio Stop Point Delta = " + delta + " Min Dist = " + minDist);
                if(delta > 0){
                    minDist = (minDist > delta ? delta : minDist);
                    debugBrakeReason += "Prio ";
                    obstacleFound = true;
                }
            }
        }
        if (!obstacleFound){
            double accumulator = currentTrack.getLength() - position;
            for (int i = currentTrackNumber + 1; i < path.size() && accumulator < lookDistance && !obstacleFound ; i++) {
                Track actTrack = path.get(i);

                for (Vehicle vehicle : actTrack.getVehiclesOnTrack()) {
                    double distOfVehicleInTrack = vehicle.getCurrentPosInTrack() - VEHICLE_LENGTH/2;
                    if (distOfVehicleInTrack + accumulator < minDist){
                        minDist = distOfVehicleInTrack + accumulator;
                    }
                    //Found something. No need to check following Tracks
                    debugBrakeReason += "NextVehicle ";
                    obstacleFound = true;
                }
                if (actTrack.hasStopPoint()) {
                    if (actTrack.isStopPointEnabled() || !(checkIfStopPointPassPossible(actTrack))) {
                        double distOfStopPointInTrack = actTrack.getStopPointPosition();
                        if (distOfStopPointInTrack + accumulator < minDist){
                            minDist = distOfStopPointInTrack + accumulator;
                        }
                        //Found something. No need to check following Tracks
                        debugBrakeReason += "NextStop ";
                        obstacleFound = true;
                    }
                }
                if (actTrack.hasPriorityStopPoint()) {
                    TrafficPriorityChecker checker = actTrack.getPriorityStopPoint();
                    if (!checker.checkFree(this)) {
                        double distOfStopPointInTrack = checker.getStopPointPos();
                        if (distOfStopPointInTrack + accumulator < minDist){
                            minDist = distOfStopPointInTrack + accumulator;
                        }
                        //Found something. No need to check following Tracks
                        debugBrakeReason += "NextPrio ";
                        obstacleFound = true;
                    }
                }
                if(!obstacleFound){
                    if (i+1 < path.size()){
                        Track OutTrackInPath = path.get(i+1);
                        for (Track track : actTrack.getOutTrackList()) {
                            if(track != OutTrackInPath){
                                //track.select();
                                if (!track.getVehiclesOnTrack().isEmpty()){
                                    double minDistInOtherTrack = Double.POSITIVE_INFINITY;
                                    for (Vehicle vehicle : track.getVehiclesOnTrack()) {
                                        if (vehicle.currentPosInTrack < minDistInOtherTrack) {
                                            minDistInOtherTrack = vehicle.currentPosInTrack;
                                        }
                                    }
                                    if (minDistInOtherTrack < CAR_SIZE+2) {
                                        double d = (accumulator + minDistInOtherTrack + actTrack.getLength()) - (VEHICLE_LENGTH/2);
                                        if (d < minDist) {
                                            minDist = d;
                                            debugBrakeReason += "SideWayDist ";
                                            obstacleFound = true;
                                        }
                                    }
                                    Position posOnPath = actTrack.getPosOnArea(minDistInOtherTrack);
                                    Position posOffPath = track.getPosOnArea(minDistInOtherTrack);
                                    double dist = posOnPath.distance(posOffPath);
                                    //System.out.println("Dist Sideways First Track= " +dist +"Vehicles ="+this);
                                    if(dist < MIN_DIST_SIDEWAY){
                                        double d = accumulator+actTrack.getLength() + CAR_SIZE + 1;
                                        if (d < minDist) {
                                            debugBrakeReason += "SideWay ";
                                            obstacleFound = true;
                                            minDist = d;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                accumulator += actTrack.getLength();
            }
        }
        //System.out.println("Min Dist =" + minDist);
        return minDist;
    }

    private boolean checkIfStopPointPassPossible(Track currentTrack) {
        int stopPointIndexInPath = -1;
        for(int i = currentTrackNumber; i < currentTrackNumber + 10 && i < path.size(); i++){
            if (path.get(i) == currentTrack){
                stopPointIndexInPath = i;
            }
        }
        if (stopPointIndexInPath > -1){
            for(int j = stopPointIndexInPath + 1; j < stopPointIndexInPath + 3 && j < path.size(); j++){
                if (!path.get(j).getVehiclesOnTrack().isEmpty()){
                    return false;
                }
            }
        }
        return true;
    }

    private void accelerate(double delta, double value) {
        velocity += delta * maxAcceleration * value;
        if (velocity >= maxVelocity) {
            velocity = maxVelocity;
        }
    }

    private void brake(double delta, double value) {
        velocity -= delta * maxDeceleration * value;
        if (velocity < 0) {
            velocity = 0;
        }
    }

    private double brakeDistance() {
        return (velocity * velocity) / (2 * maxDeceleration);
    }

    private double accelerationDistance() {
        return (maxVelocity*maxVelocity - velocity*velocity) / (2 * maxAcceleration);
    }

    private double accelerationTime() {
        return (maxVelocity-velocity) / maxAcceleration;
    }

    public double getTimeForDist(double distance) {
        double accDist = accelerationDistance();
        double accTime = accelerationTime();
        if (distance < accDist ) {
            return accTime;
        } else {
            double remaningDist = distance - accDist;
            return accTime+(remaningDist/maxVelocity);
        }
    }
    public double debugLastLookDist = 0;


    public void move(double delta) {
        blinkTimer += delta;
        if (blinkTimer > blinkTime){
            blinkerOn = !blinkerOn;
            blinkTimer = 0;
        }
        double brakeDist = brakeDistance();
        double dist = getLookAheadDist(currentPosInTrack, VEHICLE_LENGTH + brakeDist + 5);
        debugLastLookDist = dist;
        //System.out.println("Dist =" + dist);
        if (!(braking = !(velocity * delta + MIN_DIST + brakeDist < dist))) {
            accelerate(delta, 1);
        } else {
            brake(delta, 1);
        }
        double newPositionInCurrentTrack = currentPosInTrack + velocity * delta;
        if (currentTrack.getLength() < newPositionInCurrentTrack) {
            currentTrackNumber++;

            if (currentTrackNumber < path.size() && currentTrack.getOutTrackList().size() > 0) {
                Track nextTrack = path.get(currentTrackNumber);
                double distanceInNewTrack = newPositionInCurrentTrack - currentTrack.getLength();
                currentPosInTrack = distanceInNewTrack;
                switchTrack(nextTrack);
            } else {
                active = false;
            }
        } else {
            currentPosInTrack = newPositionInCurrentTrack;
        }
    }

    protected void switchTrack(Track newTrack) {
        if (currentTrack != null) {
            currentTrack.removeVehicle(this);
        }
        newTrack.addVehicle(this);
        currentTrack = newTrack;
    }

    public boolean isActive() {
        return active;
    }

    public double getCurrentPosInTrack() {
        return currentPosInTrack;
    }

    public Position getPosition() {
        return currentTrack.getPosOnArea(currentPosInTrack);
    }

    public double getDirection() {
        return currentTrack.getDirectionOnPos(currentPosInTrack);
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public List<Track> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return "T:" + currentTrack.id + " P: " + Util.DOUBLE_FORMAT_0_00.format(currentPosInTrack) + " V:" + Util.DOUBLE_FORMAT_0_00.format(velocity);
    }

    private double blinkTimer = 0;
    private boolean blinkerOn = true;
    private double blinkTime = Math.random()*0.5+0.25;

    public void draw(AreaGraphicsContext agc, boolean selected) {
        agc.setFill(color);
        //agc.setFill(Color.hsb(velocity*8.64, 1, 1));
        //agc.setFill(Color.hsb((currentTrackNumber/((double)path.size()))*120, 1, 1));

        agc.gc.fillRoundRect(-CAR_SIZE, -(CAR_SIZE /2), CAR_SIZE *2, CAR_SIZE, CAR_SIZE / 2, CAR_SIZE / 2);
        agc.setFill(Color.hsb(0, braking ? 1 : 0.6, braking ? 1 : 0.6, 1));
        agc.gc.fillRoundRect(-CAR_SIZE + CAR_SIZE*0.05,-CAR_SIZE/2 + CAR_SIZE*0.1, CAR_SIZE*0.2,CAR_SIZE*0.8,CAR_SIZE / 2, CAR_SIZE / 2);

        boolean leftBlink = false;
        boolean rightBlink = false;

        Track formerTrack = null;
        for (Track track : path.subList(currentTrackNumber,(currentTrackNumber+3 <= path.size())?currentTrackNumber+3:path.size())) {
            if (track.getStreet() instanceof  StreetCross || track.getStreet() instanceof StreetTJunction){
                if (track.getInDir().isLeftOf(track.getOutDir())){
                    rightBlink = true;
                    break;
                }
                if (track.getInDir().isLeftOf(track.getOutDir().rotateClockWise().rotateClockWise())){
                    leftBlink = true;
                    break;
                }
            }
            if (track.getStreet() instanceof StreetRoundAbout && currentTrack.getStreet() instanceof StreetRoundAbout){
                if (track.getInDir().isLeftOf(track.getOutDir())){
                    if (formerTrack != null && formerTrack.getOutDir().isLeftOf(formerTrack.getInDir())){
                        rightBlink = true;
                        break;
                    }
                }
            }
            formerTrack = track;

        }

        double blinkerSize = CAR_SIZE*0.2;
        if (leftBlink && blinkerOn){
            agc.gc.setFill(Color.rgb(255,255,0));
        } else{
            agc.gc.setFill(Color.rgb(133,55,1));
        }
        agc.gc.fillRect(CAR_SIZE - blinkerSize,-CAR_SIZE / 2,blinkerSize,blinkerSize);
        if (rightBlink && blinkerOn){
            agc.gc.setFill(Color.rgb(255,255,0));
        } else{
            agc.gc.setFill(Color.rgb(133,55,1));
        }
        agc.gc.fillRect(CAR_SIZE - blinkerSize,CAR_SIZE / 2 - blinkerSize,blinkerSize,blinkerSize);

        if (selected) {
            agc.gc.setLineWidth(3*agc.scale);
            agc.setStroke(Color.WHITE);
            agc.gc.strokeRoundRect(-CAR_SIZE, -(CAR_SIZE /2), CAR_SIZE *2, CAR_SIZE, CAR_SIZE / 2, CAR_SIZE / 2);


            if (debugLastLookDist < 100000) {
                agc.gc.setLineWidth(agc.scale*1.5);
                agc.setStroke(Color.RED);
                agc.gc.strokeOval(-debugLastLookDist, -debugLastLookDist, debugLastLookDist*2, debugLastLookDist*2);
            }


            double brakeDist = brakeDistance();
            agc.setStroke(Color.FUCHSIA);
            agc.gc.strokeOval(-brakeDist, -brakeDist, brakeDist*2, brakeDist*2);
        }
    }

    public int getCurrentTrackNumber() {
        return currentTrackNumber;
    }

    public Track getNextTrack() {
        if (currentTrackNumber+1 >= path.size()) {
            return null;
        }
        return path.get(currentTrackNumber + 1);
    }

    public double getVelocity() {
        return velocity;
    }

    /*public double distanceToTrack(Track target, double maxDist) {

        //target.select(Color.PURPLE);
        double dist = 0;
        for (int i = currentTrackNumber; i < path.size(); i++) {
            Track track = path.get(i);
            //track.select(Color.LIGHTBLUE);
            if (track == target) {
                //track.select(Color.LIME);
                return dist;
            }
            if (i == currentTrackNumber) {
                dist += track.getLength()-currentPosInTrack;
            } else {
                dist += track.getLength();
            }
            System.out.println(dist);
            if (dist > maxDist) {
                return Double.POSITIVE_INFINITY;
            }
        }
        return dist;
    }*/

    public double distanceToTrack(Track track, Track target, double maxDist) {
        //track.select(Color.RED.deriveColor(maxDist*3, 1, 1, 1));
        double length;
        if (track == currentTrack) {
            length = track.getLength()-getCurrentPosInTrack();
        } else {
            length = track.getLength();
        }
        if (track == target) {
            //track.select(Color.WHITE);
            return 0;
        } else {
            if (track.getLength() < maxDist) {
                double minDist = Double.POSITIVE_INFINITY;
                for (Track t0 : track.getOutTrackList()) {
                    double dist = distanceToTrack(t0, target, maxDist - length);
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                return length + minDist;
            } else {
                return Double.POSITIVE_INFINITY;
            }
        }
    }






    public List<Pair<Vehicle, Color>> debug;
    public Color debugColor;
    public TrafficPriorityChecker debugPoint;

    public boolean isBraking() {
        return braking;
    }
}
