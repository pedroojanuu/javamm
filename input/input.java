import B;
class A {
    int c;
    B bar() {
        return new B();
    }
    int a() {
        int c;
        c = 2;
        return this.bar().foo();
    }
}