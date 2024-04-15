class MiscComplexArgs {

    public static void main(String[] args) {
        boolean result1;
        MiscComplexArgs m;

        m = new MiscComplexArgs();

        result1 = m.foo(!(2 < 5 && 3 < 2) && !false, 55*2);
    }

    public boolean foo (boolean x, int i) {
        return x && i < 5;
    }
}
