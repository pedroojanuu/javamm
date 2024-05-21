import ImportedClass;

class A extends B {
    int testing() {
        int a;
        this.self();
        this.foo();
        a = this.self();

        return this.foo();
    }
    int another_test() {
        A a;
        boolean b;
        b = a.foo();
        return 0;
    }

    public static void main(String[] args) {}
}
