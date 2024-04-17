import B;

class A {
    int c;

    B bar() {
        return new B();
    }

    int a() {
        return this.bar().foo();
    }
}
