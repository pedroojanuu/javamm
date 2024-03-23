class A {
    int[] foo(int ...myVarArgs) {
        return myVarArgs;
    }

    int varArgsTests() {
        int[] a;
        a = new int[10];

        this.foo([1, 2, 3, 4]);
        this.foo(a);
        this.foo(new int[1]);

        return 0;
    }
}
