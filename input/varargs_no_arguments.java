class A {
    int foo(int... a) {
        return 1;
    }
    int x() {
        return this.foo();
    }
}