import io;

class Simple extends Another {
    Another self() {
        return this;
    }
    Another hell() {
        Another a;
        a = this.self();
        return a.extendsFunction();
    }

    Another hell2() {
        Another a;
        a = a.self();
        return a;
    }

    public static void main(String[] args) {}
}
