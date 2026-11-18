package fr.billiardplanner.geometry.domain;

import java.util.Objects;

public record CoordinateSystem(
    MeasurementUnit unit,
    boolean yAxisDown,
    double unitsPerMeter
) {
    public CoordinateSystem {
        Objects.requireNonNull(unit, "Measurement unit is required");

        if (!Double.isFinite(unitsPerMeter) || unitsPerMeter <= 0) {
            throw new IllegalArgumentException(
                "Units per meter must be positive and finite"
            );
        }
    }

    public static CoordinateSystem millimeters() {
        return new CoordinateSystem(
            MeasurementUnit.MILLIMETER,
            false,
            1000
        );
    }

    public double toMeters(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Value must be finite");
        }

        return value / unitsPerMeter;
    }
}