import B;
class A {

    int c;

    B bar() {
        return new B();
    }

    int a() {
        B b;
        b = this.bar();
        return b.foo(1, 2*5, 54, 55);
    }
}
