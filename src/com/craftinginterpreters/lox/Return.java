package com.craftinginterpreters.lox;

/**
 * Implements the returning of values from within functions back to the caller.
 */
public class Return extends RuntimeException
{
    final Object value;

    Return(Object value)
    {
        super(null, null, false, false);
        this.value = value;
    }
}
