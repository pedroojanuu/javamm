class A {
    A self() {
        return this;
    }
    int foo() {
        return 0;
    }
    int testing() {
        return this.self().self().self().self().self().self().self().self().self().self().self().self().foo();
    }
}
