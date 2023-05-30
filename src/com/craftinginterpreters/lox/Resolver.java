package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Stack;

/**
 * Class to perform static variable resolution after syntax tree compilation
 */
public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>
{
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private FunctionType currentFunction = FunctionType.NONE;

    Resolver(Interpreter interpreter)
    {
        this.interpreter = interpreter;
    }

    private enum FunctionType
    {
        NONE,
        FUNCTION
    }

    /**
     * Process code blocks
     * @param stmt The block to process
     */
    @Override
    public Void visitBlockStmt(Stmt.Block stmt)
    {
        beginScope();
        resolve(stmt.statements);
        endScope();
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt)
    {
        declare(stmt.name);
        if (stmt.initializer != null) {
            resolve(stmt.initializer);
        }
        define(stmt.name);
        return null;
    }

    @Override
    public Void visitVariableExpr(Expr.Variable expr)
    {
        if (!scopes.isEmpty() &&
                scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
            Lox.error(expr.name, "Can't read local variable in its own " +
                "initializer.");
        }

        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitAssignExpr(Expr.Assign expr)
    {
        resolve(expr.value);
        resolveLocal(expr, expr.name);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt)
    {
        declare(stmt.name);
        define(stmt.name);
        resolveFunction(stmt, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt)
    {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt)
    {
        resolve(stmt.condition);
        resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) resolve(stmt.elseBranch);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt)
    {
        resolve(stmt.expression);
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt)
    {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            resolve(stmt.value);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt)
    {
        resolve(stmt.condition);
        resolve(stmt.body);
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt)
    {
        return null;
    }

    @Override
    public Void visitContinueStmt(Stmt.Continue stmt)
    {
        return null;
    }

    @Override
    public Void visitBinaryExpr(Expr.Binary expr)
    {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitCallExpr(Expr.Call expr)
    {
        resolve(expr.callee);

        for (Expr argument : expr.arguments) {
            resolve(argument);
        }

        return null;
    }

    @Override
    public Void visitGroupingExpr(Expr.Grouping expr)
    {
        resolve(expr.expression);
        return null;
    }

    @Override
    public Void visitLiteralExpr(Expr.Literal expr)
    {
        return null;
    }

    @Override
    public Void visitLogicalExpr(Expr.Logical expr)
    {
        resolve(expr.left);
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitUnaryExpr(Expr.Unary expr)
    {
        resolve(expr.right);
        return null;
    }

    @Override
    public Void visitTernaryExpr(Expr.Ternary expr)
    {
        resolve(expr.test);
        resolve(expr.trueBranch);
        resolve(expr.falseBranch);
        return null;
    }


    /**
     * Helper function ... walks a list of statements and attempts variable
     * resolution for each one
     * @param statements List of statements to resolve
     */
    void resolve(List<Stmt> statements)
    {
        for (Stmt statement: statements) {
            resolve(statement);
        }
    }

    /**
     * Applies the visitor pattern to an individual statement.  Resolves for
     * a single expression
     * @param stmt the statement to resolve
     */
    void resolve(Stmt stmt)
    {
        stmt.accept(this);
    }

    /**
     * Applies the visitor pattern to an expression.
     * @param expr the expression to resolve
     */
    void resolve(Expr expr)
    {
        expr.accept(this);
    }

    /**
     * Generate a new scope on the stack
     */
    private void beginScope()
    {
        scopes.push(new HashMap<String, Boolean>());
    }

    /**
     * Remove a scope from the stack
     */
    private void endScope()
    {
        scopes.pop();
    }

    /**
     * Handle variable declaration
     * @param name The name of the variable to declare
     */
    private void declare(Token name)
    {
        if (scopes.isEmpty()) return;

        Map<String, Boolean> scope = scopes.peek();

        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already a variable with this name in this scope.");
        }

        scope.put(name.lexeme, false);
    }

    /**
     * Once a variable has been initialised, update its scope entry to
     * indicate definition
     * @param name The variable to be defined.
     */
    private void define(Token name)
    {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    /**
     * Helper function determines if we can actually resolve a variable
     * @param expr the expression to process
     * @param name the variable to resolve
     */
    private void resolveLocal(Expr expr, Token name)
    {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            if (scopes.get(i).containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - 1 - i);
                return;
            }
        }
    }

    /**
     * Helper function to resolve variables inside a function's body
     * @param function the function body to process
     */
    private void resolveFunction(Stmt.Function function, FunctionType type)
    {
        FunctionType enclosingFunction = currentFunction;
        currentFunction = type;

        beginScope();
        for (Token param : function.params) {
            declare(param);
            define(param);
        }
        resolve(function.body);
        endScope();

        currentFunction = enclosingFunction;
    }
}
