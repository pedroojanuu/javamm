import B;
class A {
    public int foo1(B b, boolean a) {
        return 0;
    }

    public B bar() {
        return new B();
    }
    public int testing() {
        this.foo1(this.bar(), this.bar().another());

        return 0;
    }

}