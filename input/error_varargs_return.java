class A {
    int[] foo() {
        return new int[10];
    }

    int... wrong() {
        return new int[10];
    }

    public static void main(String[] args) {}
}
