import io;

class A {
    int a;

    public int x(){
        a = 10;
        io.print(a);
        return a;
    }

    public static void main(String[] args){
        A a;
        a = new A();
        a.x();
    }
}