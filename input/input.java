import io;

class ArrayVarargs {

    int foo(int... a) {
        return a[0];
    }

    int bar() {
        int res;

        res = this.foo(1, 2, 3);

        this.foo(4);

        return res;
    }
    /*
    .method bar().i32 {
      tmp2.array.i32 :=.array.i32 new(array, 3.i32).array.i32;
      __varargs_array_0.array.i32 :=.array.i32 tmp2.array.i32;
      __varargs_array_0.array.i32[0.i32].i32 :=.i32 1.i32;
      __varargs_array_0.array.i32[1.i32].i32 :=.i32 2.i32;
      __varargs_array_0.array.i32[2.i32].i32 :=.i32 3.i32;
      res.i32 :=.i32 invokevirtual(this.ArrayVarargs, "foo", __varargs_array_0.array.i32).i32;

      tmp3.array.i32 :=.array.i32 new(array, 1.i32).array.i32;
      __varargs_array_1.array.i32 :=.array.i32 tmp3.array.i32;
      __varargs_array_1.array.i32[0.i32].i32 :=.i32 4.i32;
      res.i32 :=.i32 invokevirtual(this.ArrayVarargs, "foo", __varargs_array_1.array.i32).i32;

      ret.i32 res.i32;
   }
     */

}
