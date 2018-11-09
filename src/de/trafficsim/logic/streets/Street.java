package de.trafficsim.logic.streets;

import de.trafficsim.gui.views.StreetView;
import de.trafficsim.logic.streets.tracks.Track;
import de.trafficsim.logic.streets.tracks.TrackCurve;
import de.trafficsim.logic.streets.tracks.TrackStraight;
import de.trafficsim.logic.vehicles.Vehicle;
import de.trafficsim.logic.vehicles.VehicleManager;
import de.trafficsim.util.Direction;
import de.trafficsim.util.geometry.Position;

import java.util.ArrayList;
import java.util.List;

public abstract class Street {
    protected Position position;

    private List<Track> tracks;

    private List<Track> inTracks;
    private List<Track> outTracks;

    public final StreetType type;

    protected final Direction rotation;

    public Street(Position position, StreetType type, Direction rotation) {
        this.rotation = rotation;
        this.position = position;
        this.type = type;
        tracks = new ArrayList<>();
        inTracks = new ArrayList<>();
        outTracks = new ArrayList<>();

    }

    public Street(Position position, StreetType type) {
        this(position, type, Direction.NORTH);
    }

    protected Position createPosition(double x, double y) {
        return new Position(x, y).rotate(rotation);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position p) {
        position = p;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    protected Track addInOutTrack(Track track) {
        inTracks.add(track);
        outTracks.add(track);
        return addTrack(track);
    }

    protected Track addInTrack(Track track) {
        inTracks.add(track);
        return addTrack(track);
    }

    protected Track addOutTrack(Track track) {
        outTracks.add(track);
        return addTrack(track);
    }

    protected Track addTrack(Track track) {
        tracks.add(track);
        return track;
    }

    protected void addTracks(Track... tracks) {
        for (Track track : tracks) {
            addTrack(track);
        }
    }

    protected Track addTrackBetween(Track from, Track to) {
        Track track;
        if (from.getOutDir().isHorizontal() ^ to.getInDir().isHorizontal()) {
            track = new TrackCurve(from.getTo(), to.getFrom(), from.getOutDir(), this);
        } else {
            track = new TrackStraight(from.getTo(), to.getFrom(), this);
        }
        from.connectOutToInOf(track); track.connectOutToInOf(to);
        return addTrack(track);
    }

    public void disconnect() {
        for (Track track : inTracks) {
            track.disconnectAllIngoing();
        }
        for (Track track : outTracks) {
            track.disconnectAllOutgoing();
        }
    }

    public abstract StreetView createView();

    public List<Track> getInTracks() {
        return inTracks;
    }

    public List<Track> getOutTracks() {
        return outTracks;
    }

    public void removeAllVehicles() {
        List<Vehicle> toRemove = new ArrayList<>();
        for (Track track : getTracks()) {
            toRemove.addAll(track.getVehiclesOnTrack());
        }
        VehicleManager vehicleManager = VehicleManager.getInstance();
        for (Vehicle vehicle : toRemove) {
            vehicleManager.removeVehicle(vehicle);
        }
    }

    public Street createRotated() {
        return this;
    }

    public Direction getRotation() {
        return rotation;
    }
}
