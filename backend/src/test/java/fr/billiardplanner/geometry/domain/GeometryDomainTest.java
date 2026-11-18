package fr.billiardplanner.geometry.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GeometryDomainTest {

    @Test
    void calculatesDistanceBetweenPoints() {
        Point2D a = new Point2D(0, 0);
        Point2D b = new Point2D(3000, 4000);

        assertEquals(5000, a.distanceTo(b), 0.000001);
    }

    @Test
    void rejectsInvalidPointCoordinates() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Point2D(Double.NaN, 0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Point2D(0, Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void calculatesLineLengthAndMidpoint() {
        LineSegment line = new LineSegment(
            new Point2D(0, 0),
            new Point2D(4000, 2000)
        );

        assertEquals(Math.hypot(4000, 2000), line.length(), 0.000001);
        assertEquals(new Point2D(2000, 1000), line.midpoint());
    }

    @Test
    void rejectsZeroLengthLine() {
        Point2D point = new Point2D(100, 100);

        assertThrows(
            IllegalArgumentException.class,
            () -> new LineSegment(point, point)
        );
    }

    @Test
    void calculatesRectangularPolygonAreaAndPerimeter() {
        Polygon2D polygon = rectangle(6000, 4000);

        assertEquals(24_000_000, polygon.area(), 0.000001);
        assertEquals(20_000, polygon.perimeter(), 0.000001);
    }

    @Test
    void rejectsSelfIntersectingPolygon() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Polygon2D(List.of(
                new Point2D(0, 0),
                new Point2D(4, 4),
                new Point2D(0, 4),
                new Point2D(4, 0)
            ))
        );
    }

    @Test
    void rejectsCollinearPolygon() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Polygon2D(List.of(
                new Point2D(0, 0),
                new Point2D(1, 1),
                new Point2D(2, 2)
            ))
        );
    }

    @Test
    void calculatesRoomAreaInSquareMeters() {
        Room room = new Room(
            UUID.randomUUID(),
            "Main showroom",
            rectangle(6000, 4000),
            List.of(),
            CoordinateSystem.millimeters()
        );

        assertEquals(24.0, room.areaSquareMeters(), 0.000001);
        assertEquals(20.0, room.perimeterMeters(), 0.000001);
    }

    @Test
    void rejectsInvalidWallConfidence() {
        LineSegment line = new LineSegment(
            new Point2D(0, 0),
            new Point2D(1000, 0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new Wall(
                UUID.randomUUID(),
                line,
                150,
                GeometrySource.PDF_VECTOR,
                1.5
            )
        );
    }

    @Test
    void convertsMillimetersToMeters() {
        CoordinateSystem system = CoordinateSystem.millimeters();

        assertEquals(2.5, system.toMeters(2500), 0.000001);
    }

    private Polygon2D rectangle(double width, double height) {
        return new Polygon2D(List.of(
            new Point2D(0, 0),
            new Point2D(width, 0),
            new Point2D(width, height),
            new Point2D(0, height)
        ));
    }
}