import io;

class Simple {
    public Simple self(Simple s, int a) {
        return this;
    }
    public int returnInt() {
        return 2;
    }
    public int test() {
        return this.self(this.self(this, 2), 2).self(this, 2).self(this.self(this, 2), 2).self(this, 2).returnInt();
    }
    public static void main(String[] args) {
        io.println(new Simple().test());
    }
}
