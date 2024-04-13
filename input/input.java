import A;

class HelloWorld {

    public boolean x() {
        A b;
        A c;
        int i;
        int j;
        boolean a;

        A.print(10);
        b = new A();
        b.foo();
        c = A.n();
        i = A.sum(10);
        j = 5 + this.y();
        a = false;

        return a;
    }

    public int y() {
        return 1;
    }
}

