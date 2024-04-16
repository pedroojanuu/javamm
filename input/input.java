import A;

class StructureFields {

    int a;

    public static void main (String[] args) {}

    public int x() {
        StructureFields s;
        s = new StructureFields();

        return s.y(a);
    }

    int y (int x) {
        return x;
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
