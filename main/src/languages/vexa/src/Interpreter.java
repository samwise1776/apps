import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Minimal Vexa interpreter. Supported statements: text declarations and print calls. */
public final class Interpreter {
    private final Map<String, String> variables = new HashMap<>();

    public static void main(String[] args) {
        if (args.length != 1) { System.err.println("Usage: java Interpreter <file.vexa>"); System.exit(2); }
        try { new Interpreter().execute(Path.of(args[0])); }
        catch (IllegalArgumentException | IOException exception) { System.err.println("Vexa error: " + exception.getMessage()); System.exit(1); }
    }

    void execute(Path file) throws IOException {
        if (!file.toString().endsWith(".vexa")) throw new IllegalArgumentException("files must use the .vexa extension");
        if (!Files.isRegularFile(file)) throw new IllegalArgumentException("file does not exist: " + file);
        int lineNumber = 0;
        for (String raw : Files.readAllLines(file)) {
            lineNumber++; String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("text ")) declare(line.substring(5), lineNumber);
            else if (line.startsWith("print(") && line.endsWith(")")) print(line.substring(6,line.length()-1).trim(),lineNumber);
            else throw new IllegalArgumentException("line " + lineNumber + ": unknown command: " + line);
        }
    }
    private void declare(String expression, int line) {
        String[] parts=expression.split("=",2);
        if(parts.length!=2 || !parts[0].trim().matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalArgumentException("line "+line+": invalid variable declaration");
        variables.put(parts[0].trim(), quoted(parts[1].trim(),line));
    }
    private void print(String value,int line) {
        if(value.startsWith("\"") && value.endsWith("\"")){System.out.println(quoted(value,line));return;}
        if(!variables.containsKey(value))throw new IllegalArgumentException("line "+line+": unknown variable: "+value);
        System.out.println(variables.get(value));
    }
    private String quoted(String value,int line){if(value.length()<2||!value.startsWith("\"")||!value.endsWith("\""))throw new IllegalArgumentException("line "+line+": text values require quotes");return value.substring(1,value.length()-1);}
}
