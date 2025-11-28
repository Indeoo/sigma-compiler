package org.sigma.ir;

/**
 * Enumeration of all opcodes in the RPN intermediate representation.
 *
 * The IR uses a stack-machine architecture with RPN-style expression evaluation.
 * Instructions operate on an implicit operand stack.
 */
public enum RPNOpcode {

    // ============ Stack Operations ============

    /**
     * PUSH constant
     * Pushes a constant value onto the stack.
     * Operand: the constant value (Integer, Double, String, Boolean, null)
     */
    PUSH,

    /**
     * LOAD variable_name
     * Loads a variable's value onto the stack.
     * Operand: variable name (String)
     */
    LOAD,

    /**
     * STORE variable_name
     * Pops the top value and stores it in a variable.
     * Operand: variable name (String)
     */
    STORE,

    /**
     * POP
     * Discards the top value on the stack.
     * Operand: none
     */
    POP,

    /**
     * DUP
     * Duplicates the top value on the stack.
     * Operand: none
     */
    DUP,

    // ============ Arithmetic Operations ============

    /**
     * ADD
     * Pops two values, pushes their sum.
     * Stack: [a, b] → [a + b]
     */
    ADD,

    /**
     * SUB
     * Pops two values, pushes their difference.
     * Stack: [a, b] → [a - b]
     */
    SUB,

    /**
     * MUL
     * Pops two values, pushes their product.
     * Stack: [a, b] → [a * b]
     */
    MUL,

    /**
     * DIV
     * Pops two values, pushes their quotient.
     * Stack: [a, b] → [a / b]
     */
    DIV,

    /**
     * MOD
     * Pops two values, pushes the remainder.
     * Stack: [a, b] → [a % b]
     */
    MOD,

    /**
     * POW
     * Pops two values, pushes a raised to the power of b.
     * Stack: [a, b] → [a ** b]
     */
    POW,

    /**
     * NEG
     * Pops a value, pushes its negation.
     * Stack: [a] → [-a]
     */
    NEG,

    // ============ Logical Operations ============

    /**
     * AND
     * Pops two boolean values, pushes their logical AND.
     * Stack: [a, b] → [a && b]
     */
    AND,

    /**
     * OR
     * Pops two boolean values, pushes their logical OR.
     * Stack: [a, b] → [a || b]
     */
    OR,

    /**
     * NOT
     * Pops a boolean value, pushes its negation.
     * Stack: [a] → [!a]
     */
    NOT,

    // ============ Comparison Operations ============

    /**
     * EQ
     * Pops two values, pushes true if equal.
     * Stack: [a, b] → [a == b]
     */
    EQ,

    /**
     * NE
     * Pops two values, pushes true if not equal.
     * Stack: [a, b] → [a != b]
     */
    NE,

    /**
     * LT
     * Pops two values, pushes true if a < b.
     * Stack: [a, b] → [a < b]
     */
    LT,

    /**
     * LE
     * Pops two values, pushes true if a <= b.
     * Stack: [a, b] → [a <= b]
     */
    LE,

    /**
     * GT
     * Pops two values, pushes true if a > b.
     * Stack: [a, b] → [a > b]
     */
    GT,

    /**
     * GE
     * Pops two values, pushes true if a >= b.
     * Stack: [a, b] → [a >= b]
     */
    GE,

    // ============ Control Flow ============

    /**
     * LABEL label_name
     * Marks a position in the instruction stream.
     * Operand: label name (String)
     */
    LABEL,

    /**
     * JUMP label_name
     * Unconditional jump to a label.
     * Operand: label name (String)
     */
    JUMP,

    /**
     * JUMP_IF_FALSE label_name
     * Pops a boolean, jumps if false.
     * Operand: label name (String)
     */
    JUMP_IF_FALSE,

    /**
     * JUMP_IF_TRUE label_name
     * Pops a boolean, jumps if true.
     * Operand: label name (String)
     */
    JUMP_IF_TRUE,

    // ============ Method Operations ============

    /**
     * CALL method_name arg_count
     * Calls a method with the specified number of arguments.
     * Arguments should already be on the stack (pushed left-to-right).
     * Operand: method name (String) and argument count (Integer)
     */
    CALL,

    /**
     * RETURN
     * Returns from a method with a value.
     * Pops the return value from the stack.
     */
    RETURN,

    /**
     * RETURN_VOID
     * Returns from a void method.
     */
    RETURN_VOID,

    // ============ Object Operations ============

    /**
     * NEW class_name
     * Creates a new instance of a class.
     * Operand: class name (String)
     */
    NEW,

    /**
     * GET_FIELD object field_name
     * Pops an object reference, pushes the field value.
     * Operand: field name (String)
     */
    GET_FIELD,

    /**
     * SET_FIELD object field_name
     * Pops a value and object reference, sets the field.
     * Operand: field name (String)
     */
    SET_FIELD,

    // ============ Special Operations ============

    /**
     * NOP
     * No operation (does nothing).
     */
    NOP,

    /**
     * HALT
     * Stops program execution.
     */
    HALT;

    /**
     * Returns true if this opcode requires an operand.
     */
    public boolean requiresOperand() {
        switch (this) {
            case PUSH:
            case LOAD:
            case STORE:
            case LABEL:
            case JUMP:
            case JUMP_IF_FALSE:
            case JUMP_IF_TRUE:
            case CALL:
            case NEW:
            case GET_FIELD:
            case SET_FIELD:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns the number of values this opcode pops from the stack.
     */
    public int getPopCount() {
        switch (this) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
            case AND:
            case OR:
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
                return 2;
            case NEG:
            case NOT:
            case POP:
            case STORE:
            case RETURN:
            case JUMP_IF_FALSE:
            case JUMP_IF_TRUE:
            case GET_FIELD:
                return 1;
            case SET_FIELD:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Returns the number of values this opcode pushes onto the stack.
     */
    public int getPushCount() {
        switch (this) {
            case PUSH:
            case LOAD:
            case DUP:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case POW:
            case NEG:
            case AND:
            case OR:
            case NOT:
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
            case NEW:
            case GET_FIELD:
                return 1;
            default:
                return 0;
        }
    }
}
