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
 /*
 .method public x().i32 {

    putfield(this, i1.i32, 2.i32).V;

    tmp0.i32 :=.i32 getfield(this, i1.i32).i32;
    tmp1.i32 :=.i32 2.i32 *.i32 tmp0.i32;
    i2.i32 :=.i32 tmp1.i32;

    ret.i32 1.i32;
    }
  */
