package org.example.interpreter;

import org.example.semantic.SigmaType;

/**
 * Represents a value in the Sigma language runtime
 */
public class SigmaValue {

    private Object value;
    private SigmaType type;

    public SigmaValue(Object value, SigmaType type) {
        this.value = value;
        this.type = type;
    }

    public static SigmaValue createInt(int value) {
        return new SigmaValue(value, SigmaType.INT);
    }

    public static SigmaValue createDouble(double value) {
        return new SigmaValue(value, SigmaType.DOUBLE);
    }

    public static SigmaValue createString(String value) {
        return new SigmaValue(value, SigmaType.STRING);
    }

    public static SigmaValue createBoolean(boolean value) {
        return new SigmaValue(value, SigmaType.BOOLEAN);
    }

    public static SigmaValue createNull() {
        return new SigmaValue(null, SigmaType.NULL);
    }

    public static SigmaValue createVoid() {
        return new SigmaValue(null, SigmaType.VOID);
    }

    public Object getValue() {
        return value;
    }

    public SigmaType getType() {
        return type;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setType(SigmaType type) {
        this.type = type;
    }

    // Type checking methods
    public boolean isInt() { return type == SigmaType.INT; }
    public boolean isDouble() { return type == SigmaType.DOUBLE; }
    public boolean isString() { return type == SigmaType.STRING; }
    public boolean isBoolean() { return type == SigmaType.BOOLEAN; }
    public boolean isNull() { return type == SigmaType.NULL; }
    public boolean isVoid() { return type == SigmaType.VOID; }

    // Conversion methods
    public int asInt() {
        if (isInt()) return (Integer) value;
        if (isDouble()) return ((Double) value).intValue();
        throw new RuntimeException("Cannot convert " + type + " to int");
    }

    public double asDouble() {
        if (isDouble()) return (Double) value;
        if (isInt()) return ((Integer) value).doubleValue();
        throw new RuntimeException("Cannot convert " + type + " to double");
    }

    public String asString() {
        if (isString()) return (String) value;
        if (isNull()) return "null";
        return value.toString();
    }

    public boolean asBoolean() {
        if (isBoolean()) return (Boolean) value;
        throw new RuntimeException("Cannot convert " + type + " to boolean");
    }

    /**
     * Check if this value is truthy (for conditions)
     */
    public boolean isTruthy() {
        if (isNull()) return false;
        if (isBoolean()) return asBoolean();
        if (isInt()) return asInt() != 0;
        if (isDouble()) return asDouble() != 0.0;
        if (isString()) return !((String) value).isEmpty();
        return true;
    }

    /**
     * Perform arithmetic operations
     */
    public SigmaValue add(SigmaValue other) {
        if (isString() || other.isString()) {
            return createString(this.asString() + other.asString());
        }
        if (isDouble() || other.isDouble()) {
            return createDouble(this.asDouble() + other.asDouble());
        }
        if (isInt() && other.isInt()) {
            return createInt(this.asInt() + other.asInt());
        }
        throw new RuntimeException("Invalid addition: " + type + " + " + other.type);
    }

    public SigmaValue subtract(SigmaValue other) {
        if (isDouble() || other.isDouble()) {
            return createDouble(this.asDouble() - other.asDouble());
        }
        if (isInt() && other.isInt()) {
            return createInt(this.asInt() - other.asInt());
        }
        throw new RuntimeException("Invalid subtraction: " + type + " - " + other.type);
    }

    public SigmaValue multiply(SigmaValue other) {
        if (isDouble() || other.isDouble()) {
            return createDouble(this.asDouble() * other.asDouble());
        }
        if (isInt() && other.isInt()) {
            return createInt(this.asInt() * other.asInt());
        }
        throw new RuntimeException("Invalid multiplication: " + type + " * " + other.type);
    }

    public SigmaValue divide(SigmaValue other) {
        if (isDouble() || other.isDouble()) {
            double divisor = other.asDouble();
            if (divisor == 0.0) throw new RuntimeException("Division by zero");
            return createDouble(this.asDouble() / divisor);
        }
        if (isInt() && other.isInt()) {
            int divisor = other.asInt();
            if (divisor == 0) throw new RuntimeException("Division by zero");
            return createInt(this.asInt() / divisor);
        }
        throw new RuntimeException("Invalid division: " + type + " / " + other.type);
    }

    public SigmaValue modulo(SigmaValue other) {
        if (isInt() && other.isInt()) {
            int divisor = other.asInt();
            if (divisor == 0) throw new RuntimeException("Division by zero");
            return createInt(this.asInt() % divisor);
        }
        throw new RuntimeException("Invalid modulo: " + type + " % " + other.type);
    }

    /**
     * Comparison operations
     */
    public SigmaValue equals(SigmaValue other) {
        if (isNull() && other.isNull()) return createBoolean(true);
        if (isNull() || other.isNull()) return createBoolean(false);

        if (type == other.type) {
            return createBoolean(value.equals(other.value));
        }

        // Numeric comparison
        if ((isInt() || isDouble()) && (other.isInt() || other.isDouble())) {
            return createBoolean(this.asDouble() == other.asDouble());
        }

        return createBoolean(false);
    }

    public SigmaValue notEquals(SigmaValue other) {
        return createBoolean(!equals(other).asBoolean());
    }

    public SigmaValue lessThan(SigmaValue other) {
        if ((isInt() || isDouble()) && (other.isInt() || other.isDouble())) {
            return createBoolean(this.asDouble() < other.asDouble());
        }
        throw new RuntimeException("Invalid comparison: " + type + " < " + other.type);
    }

    public SigmaValue lessThanOrEqual(SigmaValue other) {
        if ((isInt() || isDouble()) && (other.isInt() || other.isDouble())) {
            return createBoolean(this.asDouble() <= other.asDouble());
        }
        throw new RuntimeException("Invalid comparison: " + type + " <= " + other.type);
    }

    public SigmaValue greaterThan(SigmaValue other) {
        if ((isInt() || isDouble()) && (other.isInt() || other.isDouble())) {
            return createBoolean(this.asDouble() > other.asDouble());
        }
        throw new RuntimeException("Invalid comparison: " + type + " > " + other.type);
    }

    public SigmaValue greaterThanOrEqual(SigmaValue other) {
        if ((isInt() || isDouble()) && (other.isInt() || other.isDouble())) {
            return createBoolean(this.asDouble() >= other.asDouble());
        }
        throw new RuntimeException("Invalid comparison: " + type + " >= " + other.type);
    }

    /**
     * Logical operations
     */
    public SigmaValue and(SigmaValue other) {
        return createBoolean(this.isTruthy() && other.isTruthy());
    }

    public SigmaValue or(SigmaValue other) {
        return createBoolean(this.isTruthy() || other.isTruthy());
    }

    public SigmaValue not() {
        return createBoolean(!this.isTruthy());
    }

    public SigmaValue negate() {
        if (isInt()) return createInt(-asInt());
        if (isDouble()) return createDouble(-asDouble());
        throw new RuntimeException("Cannot negate " + type);
    }

    @Override
    public String toString() {
        if (isNull()) return "null";
        if (isString()) return (String) value;
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SigmaValue that = (SigmaValue) obj;
        return this.equals(that).asBoolean();
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}