import B;
class A {
    public int foo(B b, boolean a) {
        return 0;
    }
    public A self() {
        return this;
    }
    public B bar() {
        return new B();
    }
    int testing() {
        boolean p2;
        p2 = this.bar().another();
        p2 = this.self().self().self().bar().another();
        this.foo(this.bar(), p2);
        return 0;
    }
}
