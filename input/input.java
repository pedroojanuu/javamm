import io.iiiii;
import io;


class StructureFields {

    int i1;
    int i2;

    boolean b1;
    boolean b2;

    public int x() {
        i1 = 2;
        i2 = 2*i1;
        return 1;
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
