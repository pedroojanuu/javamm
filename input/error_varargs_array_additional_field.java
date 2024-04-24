class A {
    int varargs(int... a) {
        return a[0];
    }
    int main() {
        int b;
        int[] c;
        b = this.varargs(c, 2);
        return 0;
    }
}
