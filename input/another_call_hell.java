class Simple {
    public Simple self(Simple s, int a) {
        return this;
    }
    public int returnInt() {
        return 2;
    }
    public int test() {
        return this.self(this.self(new Simple(), 2).self(new Simple(),2) ,2).returnInt();
    }

    public static void main(String[] args) {}
}
