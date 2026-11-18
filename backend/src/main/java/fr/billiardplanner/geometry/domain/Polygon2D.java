package fr.billiardplanner.geometry.domain;

import java.util.List;
import java.util.Objects;

public record Polygon2D(List<Point2D> vertices) {

    private static final double EPSILON = 1e-9;

    public Polygon2D {
        Objects.requireNonNull(vertices, "Vertices are required");
        vertices = List.copyOf(vertices);

        if (vertices.size() < 3) {
            throw new IllegalArgumentException(
                "Polygon requires at least three vertices"
            );
        }

        if (vertices.stream().distinct().count() != vertices.size()) {
            throw new IllegalArgumentException(
                "Polygon contains duplicate vertices"
            );
        }

        if (Math.abs(signedArea(vertices)) <= EPSILON) {
            throw new IllegalArgumentException(
                "Polygon area must be greater than zero"
            );
        }

        if (hasSelfIntersection(vertices)) {
            throw new IllegalArgumentException(
                "Polygon must not self-intersect"
            );
        }
    }

    public double area() {
        return Math.abs(signedArea(vertices));
    }

    public double perimeter() {
        double total = 0;

        for (int i = 0; i < vertices.size(); i++) {
            Point2D current = vertices.get(i);
            Point2D next = vertices.get((i + 1) % vertices.size());

            total += current.distanceTo(next);
        }

        return total;
    }

    private static double signedArea(List<Point2D> points) {
        double sum = 0;

        for (int i = 0; i < points.size(); i++) {
            Point2D a = points.get(i);
            Point2D b = points.get((i + 1) % points.size());

            sum += a.x() * b.y() - b.x() * a.y();
        }

        return sum / 2.0;
    }

    private static boolean hasSelfIntersection(List<Point2D> points) {
        int n = points.size();

        for (int i = 0; i < n; i++) {
            Point2D a = points.get(i);
            Point2D b = points.get((i + 1) % n);

            for (int j = i + 1; j < n; j++) {
                if (j == i || (i + 1) % n == j
                        || (j + 1) % n == i) {
                    continue;
                }

                Point2D c = points.get(j);
                Point2D d = points.get((j + 1) % n);

                if (segmentsIntersect(a, b, c, d)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean segmentsIntersect(
        Point2D a, Point2D b,
        Point2D c, Point2D d
    ) {
        double o1 = orientation(a, b, c);
        double o2 = orientation(a, b, d);
        double o3 = orientation(c, d, a);
        double o4 = orientation(c, d, b);

        if (((o1 > EPSILON && o2 < -EPSILON)
                || (o1 < -EPSILON && o2 > EPSILON))
                && ((o3 > EPSILON && o4 < -EPSILON)
                || (o3 < -EPSILON && o4 > EPSILON))) {
            return true;
        }

        return (Math.abs(o1) <= EPSILON && onSegment(a, b, c))
            || (Math.abs(o2) <= EPSILON && onSegment(a, b, d))
            || (Math.abs(o3) <= EPSILON && onSegment(c, d, a))
            || (Math.abs(o4) <= EPSILON && onSegment(c, d, b));
    }

    private static double orientation(
        Point2D a, Point2D b, Point2D c
    ) {
        return (b.x() - a.x()) * (c.y() - a.y())
            - (b.y() - a.y()) * (c.x() - a.x());
    }

    private static boolean onSegment(
        Point2D a, Point2D b, Point2D p
    ) {
        return p.x() >= Math.min(a.x(), b.x()) - EPSILON
            && p.x() <= Math.max(a.x(), b.x()) + EPSILON
            && p.y() >= Math.min(a.y(), b.y()) - EPSILON
            && p.y() <= Math.max(a.y(), b.y()) + EPSILON;
    }
}