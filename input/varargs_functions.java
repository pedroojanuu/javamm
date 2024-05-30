class VarArgsFunctions {
    int foo(int... a) {
        return 0;
    }
    int other() {
        return 0;
    }
    int[] another() {
        return [1, 2, 3, 4];
    }
    int test() {
        int v;
        v = this.foo(this.other());
        v = this.foo(this.other(), this.other(), this.another()[0]);
        v = this.foo(this.another());
        v = this.foo();
        return v;
    }
}
