import Nothing.B;
import io;

class A {
    B foo() {
        return new B();
    }
    int X() {
        int a;
        a = this.foo().bar();
        return 0;
    }
}
