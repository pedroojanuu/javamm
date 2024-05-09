import io;

class ArrayInitialization {

    public static void main(String[] args) {
        ArrayInitialization a;
        a = new ArrayInitialization();

        io.println(a.foo());
    }

    int foo() {
        int[] a;
        int x;
        x = 1;

        a = [x, 2*x, 3, 4];

        return a[2];
    }
}

/*
.method foo().i32 {
      tmp2.array.i32 :=.array.i32 new(array, 4.i32).array.i32;
      __varargs_array_0.array.i32 :=.array.i32 tmp2.array.i32;
      __varargs_array_0.array.i32[0.i32].i32 :=.i32 1.i32;
      __varargs_array_0.array.i32[1.i32].i32 :=.i32 2.i32;
      __varargs_array_0.array.i32[2.i32].i32 :=.i32 3.i32;
      __varargs_array_0.array.i32[3.i32].i32 :=.i32 4.i32;
      a.array.i32 :=.array.i32 __varargs_array_0.array.i32;


      tmp3.i32 :=.i32 a.array.i32[2.i32].i32;
      ret.i32 tmp3.i32;
   }
 */