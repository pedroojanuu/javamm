import io;

class Simple {
    int a;
    int b;
    int main() {
        return new io().returnInt();
    }
    int main2() {
        int a;
        a = this.main();
        a = io.print(5);
        return 0;
    }
    int assignField() {
        a = b;
        b = a;
        return b - a;
    }
}
