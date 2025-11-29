package org.sigma.ir;

import org.junit.jupiter.api.Test;
import org.sigma.semantics.SigmaType;
import org.sigma.semantics.TypeRegistry;
import org.sigma.parser.Ast;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LocalVariableAllocator.
 *
 * Tests JVM local variable slot allocation for both static and instance methods,
 * parameter allocation, local variable allocation, and wide type handling.
 */
public class LocalVariableAllocatorTest {

    // Helper method to create a long type (not in TypeRegistry yet)
    private static SigmaType createLongType() {
        return new SigmaType.PrimitiveType("long", 8);
    }

    // ============ Static Method Tests ============

    @Test
    public void testStaticMethodStartsAtSlotZero() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);
        assertEquals(0, allocator.getMaxLocals(), "Static method should start with slot 0");
        assertFalse(allocator.isInstanceMethod(), "Should not be an instance method");
    }

    @Test
    public void testStaticMethodParameterAllocation() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "a", 1, 0),
            new Ast.Parameter("int", "b", 1, 0)
        );

        allocator.allocateParameters(params);

        assertEquals(0, allocator.getSlot("a"), "First parameter should be at slot 0");
        assertEquals(1, allocator.getSlot("b"), "Second parameter should be at slot 1");
        assertEquals(2, allocator.getMaxLocals(), "Max locals should be 2");
        assertTrue(allocator.hasVariable("a"), "Should have parameter 'a'");
        assertTrue(allocator.hasVariable("b"), "Should have parameter 'b'");
    }

    @Test
    public void testStaticMethodLocalVariableAllocation() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        // Allocate parameters first
        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "x", 1, 0),
            new Ast.Parameter("int", "y", 1, 0)
        );
        allocator.allocateParameters(params);

        // Allocate local variables
        int sumSlot = allocator.allocateVariable("sum", TypeRegistry.INT);
        int resultSlot = allocator.allocateVariable("result", TypeRegistry.INT);

        assertEquals(0, allocator.getSlot("x"), "Parameter x at slot 0");
        assertEquals(1, allocator.getSlot("y"), "Parameter y at slot 1");
        assertEquals(2, sumSlot, "Local variable 'sum' should be at slot 2");
        assertEquals(3, resultSlot, "Local variable 'result' should be at slot 3");
        assertEquals(4, allocator.getMaxLocals(), "Max locals should be 4");
    }

    // ============ Instance Method Tests ============

    @Test
    public void testInstanceMethodReservesSlotZeroForThis() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(true);

        assertEquals(1, allocator.getMaxLocals(), "Instance method should reserve slot 0 for 'this'");
        assertTrue(allocator.isInstanceMethod(), "Should be an instance method");
        assertTrue(allocator.hasVariable("this"), "Should have 'this' variable");
        assertEquals(0, allocator.getSlot("this"), "'this' should be at slot 0");
    }

    @Test
    public void testInstanceMethodParameterAllocation() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(true);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "a", 1, 0),
            new Ast.Parameter("String", "b", 1, 0)
        );

        allocator.allocateParameters(params);

        assertEquals(0, allocator.getSlot("this"), "'this' at slot 0");
        assertEquals(1, allocator.getSlot("a"), "First parameter should be at slot 1");
        assertEquals(2, allocator.getSlot("b"), "Second parameter should be at slot 2");
        assertEquals(3, allocator.getMaxLocals(), "Max locals should be 3");
    }

    @Test
    public void testInstanceMethodLocalVariableAllocation() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(true);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "value", 1, 0)
        );
        allocator.allocateParameters(params);

        int tempSlot = allocator.allocateVariable("temp", TypeRegistry.INT);

        assertEquals(0, allocator.getSlot("this"), "'this' at slot 0");
        assertEquals(1, allocator.getSlot("value"), "Parameter at slot 1");
        assertEquals(2, tempSlot, "Local variable 'temp' should be at slot 2");
        assertEquals(3, allocator.getMaxLocals(), "Max locals should be 3");
    }

    // ============ Wide Type Tests (double/long occupy 2 slots) ============

    @Test
    public void testDoubleTypeOccupiesTwoSlots() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        int slot1 = allocator.allocateVariable("d1", TypeRegistry.DOUBLE);
        int slot2 = allocator.allocateVariable("d2", TypeRegistry.DOUBLE);

        assertEquals(0, slot1, "First double at slot 0");
        assertEquals(2, slot2, "Second double at slot 2 (skip slot 1)");
        assertEquals(4, allocator.getMaxLocals(), "Max locals should be 4 (2 doubles = 4 slots)");
    }

    @Test
    public void testLongTypeOccupiesTwoSlots() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        SigmaType longType = createLongType();
        int slot1 = allocator.allocateVariable("l1", longType);
        int slot2 = allocator.allocateVariable("l2", longType);

        assertEquals(0, slot1, "First long at slot 0");
        assertEquals(2, slot2, "Second long at slot 2 (skip slot 1)");
        assertEquals(4, allocator.getMaxLocals(), "Max locals should be 4");
    }

    @Test
    public void testMixedWidthTypes() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        int intSlot = allocator.allocateVariable("i", TypeRegistry.INT);
        int doubleSlot = allocator.allocateVariable("d", TypeRegistry.DOUBLE);
        int intSlot2 = allocator.allocateVariable("j", TypeRegistry.INT);

        assertEquals(0, intSlot, "int at slot 0");
        assertEquals(1, doubleSlot, "double at slot 1-2");
        assertEquals(3, intSlot2, "int at slot 3");
        assertEquals(4, allocator.getMaxLocals(), "Max locals should be 4");
    }

    @Test
    public void testDoubleParameterOccupiesTwoSlots() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "a", 1, 0),
            new Ast.Parameter("double", "d", 1, 0),
            new Ast.Parameter("int", "b", 1, 0)
        );

        allocator.allocateParameters(params);

        assertEquals(0, allocator.getSlot("a"), "int parameter at slot 0");
        assertEquals(1, allocator.getSlot("d"), "double parameter at slot 1-2");
        assertEquals(3, allocator.getSlot("b"), "int parameter at slot 3");
        assertEquals(4, allocator.getMaxLocals(), "Max locals should be 4");
    }

    @Test
    public void testLongParameterOccupiesTwoSlots() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("long", "timestamp", 1, 0),
            new Ast.Parameter("int", "count", 1, 0)
        );

        allocator.allocateParameters(params);

        assertEquals(0, allocator.getSlot("timestamp"), "long parameter at slot 0-1");
        assertEquals(2, allocator.getSlot("count"), "int parameter at slot 2");
        assertEquals(3, allocator.getMaxLocals(), "Max locals should be 3");
    }

    // ============ Error Cases ============

    @Test
    public void testDuplicateParameterThrowsException() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "x", 1, 0),
            new Ast.Parameter("int", "x", 1, 0)  // Duplicate name
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> allocator.allocateParameters(params),
            "Should throw exception for duplicate parameter name"
        );

        assertTrue(exception.getMessage().contains("Duplicate parameter name: x"));
    }

    @Test
    public void testDuplicateVariableThrowsException() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        allocator.allocateVariable("temp", TypeRegistry.INT);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> allocator.allocateVariable("temp", TypeRegistry.INT),
            "Should throw exception for duplicate variable name"
        );

        assertTrue(exception.getMessage().contains("Variable already allocated: temp"));
    }

    @Test
    public void testGetSlotForNonexistentVariableThrowsException() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> allocator.getSlot("nonexistent"),
            "Should throw exception for nonexistent variable"
        );

        assertTrue(exception.getMessage().contains("Variable not found: nonexistent"));
    }

    @Test
    public void testParameterAndLocalCannotHaveSameName() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "value", 1, 0)
        );
        allocator.allocateParameters(params);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> allocator.allocateVariable("value", TypeRegistry.INT),
            "Should throw exception when local has same name as parameter"
        );

        assertTrue(exception.getMessage().contains("Variable already allocated: value"));
    }

    // ============ Helper Method Tests ============

    @Test
    public void testHasVariableReturnsFalseForNonexistent() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);
        assertFalse(allocator.hasVariable("nonexistent"), "Should return false for nonexistent variable");
    }

    @Test
    public void testHasVariableReturnsTrueForExisting() {
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        allocator.allocateVariable("temp", TypeRegistry.INT);

        assertTrue(allocator.hasVariable("temp"), "Should return true for existing variable");
    }

    // ============ Complex Scenario Tests ============

    @Test
    public void testCompleteMethodWithMixedTypes() {
        // Simulate: double compute(int count, long timestamp, double rate)
        LocalVariableAllocator allocator = new LocalVariableAllocator(false);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("int", "count", 1, 0),
            new Ast.Parameter("long", "timestamp", 1, 0),
            new Ast.Parameter("double", "rate", 1, 0)
        );
        allocator.allocateParameters(params);

        // Local variables
        int tempSlot = allocator.allocateVariable("temp", TypeRegistry.INT);
        int resultSlot = allocator.allocateVariable("result", TypeRegistry.DOUBLE);

        // Verify slot layout:
        // 0: count (int, 1 slot)
        // 1-2: timestamp (long, 2 slots)
        // 3-4: rate (double, 2 slots)
        // 5: temp (int, 1 slot)
        // 6-7: result (double, 2 slots)

        assertEquals(0, allocator.getSlot("count"));
        assertEquals(1, allocator.getSlot("timestamp"));
        assertEquals(3, allocator.getSlot("rate"));
        assertEquals(5, tempSlot);
        assertEquals(6, resultSlot);
        assertEquals(8, allocator.getMaxLocals());
    }

    @Test
    public void testCompleteInstanceMethodScenario() {
        // Simulate instance method: void process(String name, int age)
        LocalVariableAllocator allocator = new LocalVariableAllocator(true);

        List<Ast.Parameter> params = Arrays.asList(
            new Ast.Parameter("String", "name", 1, 0),
            new Ast.Parameter("int", "age", 1, 0)
        );
        allocator.allocateParameters(params);

        // Local variables
        int messageSlot = allocator.allocateVariable("message", TypeRegistry.STRING);
        int counterSlot = allocator.allocateVariable("counter", TypeRegistry.INT);

        // Verify slot layout:
        // 0: this (reference)
        // 1: name (reference)
        // 2: age (int)
        // 3: message (reference)
        // 4: counter (int)

        assertEquals(0, allocator.getSlot("this"));
        assertEquals(1, allocator.getSlot("name"));
        assertEquals(2, allocator.getSlot("age"));
        assertEquals(3, messageSlot);
        assertEquals(4, counterSlot);
        assertEquals(5, allocator.getMaxLocals());
    }
}
