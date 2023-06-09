package com.craftinginterpreters.lox;

import java.util.Map;
import java.util.HashMap;

public class LoxInstance
{
    private LoxClass klass;
    private final Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass)
    {
        this.klass = klass;
    }

    @Override
    public String toString()
    {
        return klass.name + " instance";
    }

    /**
     * Look up a property value from an object instance and return it
     * @param name the property to return
     * @return The returned property.
     * @throws RuntimeError if the property is not found.
     */
    Object get(Token name)
    {
        if (fields.containsKey(name.lexeme)) {
            return fields.get(name.lexeme);
        }

        LoxFunction method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name,
            "Undefined property '" + name.lexeme + "'.");
    }

    /**
     * Set a property on an object instance.
     * @param name the name of the property to set
     * @param value the value to set the property to
     */
    void set(Token name, Object value)
    {
        fields.put(name.lexeme, value);
    }
}
