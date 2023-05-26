package com.craftinginterpreters.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst
{
    /**
     * Maint entrypoint for the AST Generator.
     * @param args Expects a single parameter: The path for the output
     *             class to be saved in.  (Note filename is automatically
     *             set.
     * @throws IOException
     */
    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.out.println("Usage: GenerateAst <output_directory>");
            System.exit(64);
        }
        String outputDir = args[0];

        defineAst(outputDir, "Expr", Arrays.asList(
            "Binary   : Expr left, Token operator, Expr right",
            "Grouping : Expr expression",
            "Literal  : Object value",
            "Unary    : Token operator, Expr right"
        ));
    }

    /**
     * Generate and write the Java class file.
     * @param outputDir The directory to save the file in
     * @param baseName Base name of the Class (and Java file)
     * @param types List of the subclass definitions to add to the file
     * @throws IOException
     */
    private static void defineAst(
        String outputDir, String baseName, List<String> types)
        throws IOException
    {
        String path = outputDir +"/"+ baseName +".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");

        writer.println("package com.craftinginterpreters.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class "+ baseName);
        writer.println("{");

        // The AST classes
        for (String type: types) {
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println("}");
        writer.close();
    }

    private static void defineType(
        PrintWriter writer, String baseName,
        String className, String fieldList)
    {
        writer.println("  static class "+ className +" extends "+ baseName);
        writer.println("  {");

        // Constructor
        writer.println("    "+ className + "("+ fieldList +")");
        writer.println("     {");

        // Store parameters in fields
        String[] fields = fieldList.split(", ");
        for (String field: fields) {
            String name = field.split(" ")[1];
            writer.println("      this."+ name +" = "+ name +";");
        }
        writer.println("    }");

        // Fields
        writer.println();
        for (String field: fields) {
            writer.println("    final "+ field +";");
        }

        writer.println("  }");
    }

}
