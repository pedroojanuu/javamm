class MyAdditionalTests {
    boolean varargsFunc(int... a) {
        return a[0] < 5;
    }
    int testVarargsFunc() {
        int r;
        r = 0;
        while (this.varargsFunc(r, 2, 3, 4, 5)) {
            r = r + 1;
        }
        if (this.varargsFunc(1, 2, 3, 4, 5)) {
            r = r * 2;
        }
        else {
            r = r / 2;
        }

        return r;
    }
    int testAnotherVarargsFunc() {
        int r;
        int[] arr;
        r = 0;
        while (this.varargsFunc([r, 1, 2, 3, 4, 5])) {
            r = r + 1;
        }
        arr = new int[10];
        arr[0] = 1;

        while (this.varargsFunc(arr)) {
            arr[0] = arr[0] + 1;
        }

        if (this.varargsFunc([r, 1, 2, 3])) {
            r = r * 2;
        }
        else {
            r = r / 2;
        }
        if (this.varargsFunc(arr)) {
            r = r + arr[0];
        }
        else {
            r = r - arr[0];
        }
        return r;
    }
}
