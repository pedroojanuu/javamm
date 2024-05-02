
class Simple {
    int a;
    int function() {
        a = 5;
        return a;
    }
    public static void main(String[] args) {
        Simple s;
        s = new Simple();
        s.function();
        s.main(args);
    }
}
