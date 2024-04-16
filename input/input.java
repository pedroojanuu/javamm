class StructureFields {

    int a;
    int b;

    public static void main (String[] args) {}

    public int x() {
        StructureFields s;
        s = new StructureFields();

        return s.a;
    }

    int y (int a, int b) {
        return a + b;
    }
}
