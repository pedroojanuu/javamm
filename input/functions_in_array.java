import imported;
class FunctionsArray {
    int function() {
        return 10;
    }
    int[] otherFunction() {
        return [1, 2, 3, 4];
    }
    int test() {
        int[] a;
        int b;
        int[] c;
        a = [1, 2, this.function(), this.otherFunction()[1]];

        b = this.otherFunction()[1];

        c = imported.function([1, 2, 3], [1, 2, 3][1]);
        c = imported.anotherFunction();
        return b;
    }
}
