import ui.LSRDisplay;

public class Main {
    public static void main(String[] args) {
        final LSRDisplay display = new LSRDisplay("LSR Display");
        Lsa_file lsaFile = new Lsa_file(display);
    }
}