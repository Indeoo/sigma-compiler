package org.sigma.postfix;

/**
 * Metadata describing a function that can be invoked via PSM CALL/RET.
 */
public record PostfixFunction(String name, String returnType, int parameterCount) {
}
