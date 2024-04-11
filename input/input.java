import io;
class Factorial {
    int a;
    // int... b;    // Variable declarations, field declarations and method returns cannot be vararg
    String e;
    int myMain;
    String _mainvalue;
    int ifValue;
    int elseValue;
    int lengthValue;
    public int computeFactorial(int num){
        int num_aux ;
        if (num < 1)
            num_aux = 1;
        else
            num_aux = num * (this.computeFactorial(num-1));
        return num_aux;
    }

    public static void main(String[] args){
        int a;
        int b;
        int c;
        Simple s;
        a = 20;
        b = 10;
        s = new Simple();
        c = s.add(a,b);
        io.println(c);
    }

    public int constInstr(){
        int c;
        c = 0;
        c = 4;
        c = 8;
        c = 14;
        c = 250;
        c = 400;
        c = 1000;
        c = 100474650;
        c = 10;
        return c;
    }


}