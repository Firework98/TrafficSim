package de.trafficsim.logic.streets;

import de.trafficsim.logic.streets.tracks.Track;
import de.trafficsim.logic.streets.tracks.TrackCurve;
import de.trafficsim.util.geometry.Position;

public class StreetRoundAbout extends Street {
    public StreetRoundAbout(Position position) {
        super(position);

        Track r0 = new TrackCurve(new Position(50, 0), new Position(0, 50), false);
        Track r1 = new TrackCurve(new Position(0, 50), new Position(-50, 0), true);
        Track r2 = new TrackCurve(new Position(-50, 0), new Position(0, -50), false);
        Track r3 = new TrackCurve(new Position(0, -50), new Position(50, 0), true);

        r0.connectOutToInOf(r1);
        r1.connectOutToInOf(r2);
        r2.connectOutToInOf(r3);
        r3.connectOutToInOf(r0);

        tracks.add(r0);
        tracks.add(r1);
        tracks.add(r2);
        tracks.add(r3);
    }
}