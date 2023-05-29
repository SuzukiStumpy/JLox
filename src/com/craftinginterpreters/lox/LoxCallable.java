package com.craftinginterpreters.lox;

import java.util.List;

/**
 * Interface to implement the LoxCallable protocol.  Used by anything that
 * needs to act as a function call (ie: functions, methods, classes, etc)
 */
interface LoxCallable
{
    Object call(Interpreter interpreter, List<Object> arguments);
    int arity();
}
