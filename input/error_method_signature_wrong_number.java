class A {
    int foo() {
        return 0;
    }
    int abc(int a) {
        return 0;
    }
    int testing() {
        int a;
        a = this.abc(a);
        a = this.foo();
        a = this.abc();
        return 0;
    }

    public static void main(String[] args) {}
}
