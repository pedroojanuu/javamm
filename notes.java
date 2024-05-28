import io;

A {

.field public a.i32;

.method public  x().i32 {
        putfield(this, a.i32, 10.i32).V;
        tmp9.i32 :=.i32 getfield(this, a.i32).i32;
        invokestatic(io, "print", tmp9.i32).V;
        tmp10.i32 :=.i32 getfield(this, a.i32).i32;
        ret.i32 tmp10.i32;
    }

.method public static main(args.array.String).V {
        tmp11.A :=.A new(A).A;
        invokespecial(tmp11.A,"<init>").V;
        a.A :=.A tmp11.A;
        invokevirtual(a, "x").i32;
        ret.V;
    }

.construct A().V {
        invokespecial(this, "<init>").V;
    }
}