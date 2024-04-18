import ImportedClass;

class A extends B {
    int testing() {
        int a;
        this.self();
        this.foo();
        a = this.self();

        return this.foo();
    }
}
