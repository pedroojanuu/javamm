import io;

class MyOtherTests {
    int array_reference(int[] a) {
        return a[0];
    }

    int array_reference_multiple_parameters(int a, int[] b) {
        return a + b[0];
    }

    int testArrayReferences() {
        int a;
        int[] arr;

        a = this.array_reference(arr);
        a = this.array_reference_multiple_parameters(a, arr);
        a = this.array_reference([1, 2, 3]);
        a = this.array_reference_multiple_parameters(a, [1, 2, 3]);
        this.array_reference([1, 2, 3]);
        this.array_reference(arr);
        return a;
    }

    int test() {
        int[] a;
        a = new int[10];
        a[0] = 1;

        a[this.testArrayReferences() + this.array_reference(a)] = a[this.testArrayReferences() + this.array_reference(a)] * 2;
        return a[this.testArrayReferences() + this.array_reference(a)];
    }

    int manyArgs(int a, int[] b, MyOtherTests t, int... arr) {
        return a + b[0] + t.test() + arr[0];
    }

    int testManyArgs() {
        int a;
        int[] b;
        MyOtherTests t;
        t = new MyOtherTests();

        a = this.manyArgs(1, b, t);
        a = this.manyArgs(1, b, t, 2);
        a = this.manyArgs(1, b, t, 2, 3, 4, 5);
        a = this.manyArgs(1, b, t, 2, 3, 4, this.test(), t.test());
        return a;
    }
}
