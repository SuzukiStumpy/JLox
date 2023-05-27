package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Class implements the variable mapping table (or 'Environment') for the
 * interpreter.
 */
class Environment
{
    // Storage for the variable symbol table.
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Create and store a variable into the table (either new or a redefinition)
     * @param name The name of the variable
     * @param value the value to store
     */
    void define(String name, Object value)
    {
        values.put(name, value);
    }

    /**
     * Retrieve the value of a variable
     * @param name the variable to retrieve
     * @return the value of the variable from the map
     */
    Object get(Token name)
    {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        throw new RuntimeError(name, "Undefined variable '"+ name.lexeme +"'.");
    }

    /**
     * Allows for assignment to a variable that has been previously declared
     * @param name name of the variable to assign
     * @param value the value to assign
     */
    void assign(Token name, Object value)
    {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '"+ name.lexeme +"'.");
    }
}
