class ArrayIndexing {
    int anotherFunction() {
        return 0;
    }
    int function() {
        int[] a;
        int another;
        a = [1, 2, 3, 4, 5, 6];
        another = a[this.anotherFunction()];

        a[this.anotherFunction()] = 100;
        return a[this.anotherFunction()];
    }
}