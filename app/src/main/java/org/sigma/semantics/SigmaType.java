package org.sigma.semantics;

/**
 * Base class for all types in the Sigma language.
 * Provides type representation, equality checking, and compatibility checking.
 */
public abstract class SigmaType {

    /**
     * Get the name of this type (e.g., "int", "String", "Calculator")
     */
    public abstract String getName();

    /**
     * Check if this type is compatible with another type.
     * Used for assignment checking and type inference.
     */
    public abstract boolean isCompatibleWith(SigmaType other);

    /**
     * Check if this type is a primitive type
     */
    public boolean isPrimitive() {
        return this instanceof PrimitiveType;
    }

    /**
     * Check if this type is a reference type (class or null)
     */
    public boolean isReference() {
        return this instanceof ClassType || this instanceof NullType;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SigmaType)) return false;
        return this.getName().equals(((SigmaType) obj).getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    // ============ Type Subclasses ============

    /**
     * Primitive types: int, double, float, boolean
     */
    public static class PrimitiveType extends SigmaType {
        private final String name;
        private final int size; // in bytes

        public PrimitiveType(String name, int size) {
            this.name = name;
            this.size = size;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isCompatibleWith(SigmaType other) {
            if (this.equals(other)) return true;

            if (other instanceof PrimitiveType) {
                PrimitiveType otherPrim = (PrimitiveType) other;
                boolean thisNumeric = isNumeric(this.name);
                boolean otherNumeric = isNumeric(otherPrim.name);

                // Allow numeric conversions both widening and narrowing
                if (thisNumeric && otherNumeric) {
                    return true;
                }
            }

            return false;
        }

        private boolean isNumeric(String name) {
            return name.equals("int") || name.equals("float") || name.equals("double");
        }

        public int getSize() {
            return size;
        }
    }

    /**
     * Class/reference types (String, user-defined classes)
     */
    public static class ClassType extends SigmaType {
        private final String name;

        public ClassType(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isCompatibleWith(SigmaType other) {
            // Exact match
            if (this.equals(other)) return true;

            // Null can be assigned to any class type
            if (other instanceof NullType) return true;

            // TODO: Add inheritance support later
            return false;
        }
    }

    /**
     * Void type (return type only, cannot be assigned to variables)
     */
    public static class VoidType extends SigmaType {
        private static final VoidType INSTANCE = new VoidType();

        private VoidType() {}

        public static VoidType getInstance() {
            return INSTANCE;
        }

        @Override
        public String getName() {
            return "void";
        }

        @Override
        public boolean isCompatibleWith(SigmaType other) {
            // Void is only compatible with void
            return other instanceof VoidType;
        }
    }

    /**
     * Null type (special type for null literal)
     */
    public static class NullType extends SigmaType {
        private static final NullType INSTANCE = new NullType();

        private NullType() {}

        public static NullType getInstance() {
            return INSTANCE;
        }

        @Override
        public String getName() {
            return "null";
        }

        @Override
        public boolean isCompatibleWith(SigmaType other) {
            // Null is compatible with any reference type
            return other instanceof ClassType || other instanceof NullType;
        }
    }

    /**
     * Error type (used when type resolution fails)
     */
    public static class ErrorType extends SigmaType {
        private static final ErrorType INSTANCE = new ErrorType();

        private ErrorType() {}

        public static ErrorType getInstance() {
            return INSTANCE;
        }

        @Override
        public String getName() {
            return "<error>";
        }

        @Override
        public boolean isCompatibleWith(SigmaType other) {
            // Error type is compatible with everything to suppress cascading errors
            return true;
        }
    }
}
