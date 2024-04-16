import A;

class StructureFields {

    A a;

    public static void main (String[] args) {}

    public A x() {
        A b;
        b = new A();
        a = b;
        return a;
    }

}
