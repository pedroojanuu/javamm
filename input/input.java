import io;

class Simple {

    int field_1;
    Foo mc;
    Simple s2;


    public int constInstr(){
        return 1 + field_1;
    }

    public int add(int a, int b){
        int c;
        c = a + this.constInstr();
        return c;
    }

    public static void main(String[] args) {
        io.println((new Simple()).add(2, 3) + 3);
    }

}
