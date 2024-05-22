import Nothing.B;
import io;
import arr;

A {


.method foo().B {
tmp0.B :=.B new(B).B;
invokespecial(tmp0.B,"<init>").V;
ret.B tmp0.B;
}

.method myfunc().NotImportedClass {
tmp1.NotImportedClass :=.NotImportedClass new(NotImportedClass).NotImportedClass;
invokespecial(tmp1.NotImportedClass,"<init>").V;
ret.NotImportedClass tmp1.NotImportedClass;
}

.method X().i32 {
tmp2.B :=.B invokevirtual(this.A, "foo").B;
tmp3.i32 :=.i32 invokevirtual(tmp2.B, "bar").i32;
a.i32 :=.i32 tmp3.i32;
ret.i32 0.i32;
}

.method assumptions().i32 {
tmp4.array.i32 :=.array.i32 (arr, "genarray", 10.i32).array.i32;
a.array.i32 :=.array.i32 tmp4.array.i32;
ret.i32 0.i32;
}

.method public anotherone().i32 {
tmp5.A :=.A new(A).A;
invokespecial(tmp5.A,"<init>").V;
a.A :=.A tmp5.A;
tmp6.B :=.B invokevirtual(a.A, "foo").B;
invokevirtual(tmp6.B, "anyfunctionnnnnnnnnnnnnnnnn").V;
ret.i32 0.i32;
}

.method public static main(args.array.String).V {
ret.V;
}

.construct A().V {
invokespecial(this, "<init>").V;
}
}