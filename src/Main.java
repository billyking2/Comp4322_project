import app.LSRController;
import ui.LSRDisplay;

public class Main {
    public static void main(String[] args) {
        final LSRDisplay display = new LSRDisplay("LSR Display");
        LSRController ignored = new LSRController(display);
    }
}