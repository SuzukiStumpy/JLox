package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

/**
 * Class implements the variable mapping table (or 'Environment') for the
 * interpreter.
 */
class Environment
{
    // The parent environment for this scope (if any)
    final Environment enclosing;

    // Storage for the variable symbol table.
    private final Map<String, Object> values = new HashMap<>();

    /**
     * Base constructor.  Used when instantiating the global environment
     */
    Environment() {
        enclosing = null;
    }

    /**
     * When generating new scopes, pass in a reference to the enclosing scope
     * @param enclosing The parent/enclosing scope
     */
    Environment(Environment enclosing)
    {
        this.enclosing = enclosing;
    }

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

        if (enclosing != null) return enclosing.get(name);

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

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '"+ name.lexeme +"'.");
    }
}
