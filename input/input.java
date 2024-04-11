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
}
