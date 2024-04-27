import ioPlus;
class ArrayAccess {

    public int foo(int[] a) {
        int result;

//        result = a[a.length];
//        result = a[1] + a[2];
//        result = result + a[3];
//        result = 1 + a[4];

        a[0] = 1;
        a[1] = 2;
        a[2] = 3;
        a[3] = 4;
        a[4] = 5;


        return result;
    }


}

/*
import ioPlus;
ArrayAccess extends Object {


.method public foo(a.array.i32).i32 {
a[0.i32].i32 :=.i32 1.i32;
a[1.i32].i32 :=.i32 2.i32;
a[2.i32].i32 :=.i32 3.i32;
a[3.i32].i32 :=.i32 4.i32;
a[4.i32].i32 :=.i32 5.i32;
ret.i32 result.i32;
}

.construct ArrayAccess().V {
invokespecial(this, "").V;
}
}
 */