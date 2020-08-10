package mofokom.slee.testfw.mocks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author wozza
 */
public class MultiException extends Exception {

    List<Throwable> e = new ArrayList<>();

    public MultiException(List<Throwable> e) {
        this.e = e;
    }

    public List<Throwable> getE() {
        return e;
    }

    @Override
    public String getMessage() {
        return e.stream().map(ex -> ex.getMessage()).collect(joining("MultiException[", "---", "] MultiException"));
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        e.forEach(ex -> ex.printStackTrace(s));
    }

}
