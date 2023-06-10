package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;
public class LoxClass extends LoxInstance implements LoxCallable
{
    final String name;
    final LoxClass superclass;
    private final Map<String, LoxFunction> methods;

    LoxClass(LoxClass metaClass, String name,
             LoxClass superclass, Map<String, LoxFunction> methods)
    {
        super(metaClass);
        this.name = name;
        this.superclass = superclass;
        this.methods = methods;
    }

    @Override
    public String toString()
    {
        return name;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments)
    {
        LoxInstance instance = new LoxInstance(this);

        LoxFunction initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        LoxFunction initializer = findMethod("init");
        if (initializer == null) return 0;
        return initializer.arity();
    }

    LoxFunction findMethod(String name)
    {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    /**
     * Try to find an instance method before attempting to pass up the chain
     * to the superclass for fields and class methods.
     * @param name the property to return
     * @return The returned property.
     * @throws RuntimeError if the property is not found.
     */
    @Override
    Object get(Token name)
    {
        LoxFunction method = findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        return super.get(name);
    }
}
