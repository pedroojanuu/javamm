import io;

X {


.method a().i32 {
tmp0.io :=.io new(io).io;
invokespecial(tmp0.io,"<init>").V;
io.io :=.io tmp0.io;
i.i32 :=.i32 0.i32;
tmp1.i32 :=.i32 10.i32;
tmp2.array.i32 :=.array.i32 new(array, tmp1.i32).array.i32;
arr.array.i32 :=.array.i32 tmp2.array.i32;
arr[0.i32].i32 :=.i32 0.i32;
tmp3.array.i32 :=.array.i32 new(array, 2.i32).array.i32;
__varargs_array_0.array.i32 :=.array.i32 tmp3.array.i32;
__varargs_array_0.array.i32[0.i32].i32 :=.i32 0.i32;
tmp4.array.i32 :=.array.i32 new(array, 3.i32).array.i32;
__varargs_array_1.array.i32 :=.array.i32 tmp4.array.i32;
__varargs_array_1.array.i32[0.i32].i32 :=.i32 1.i32;
__varargs_array_1.array.i32[1.i32].i32 :=.i32 2.i32;
__varargs_array_1.array.i32[2.i32].i32 :=.i32 3.i32;
tmp5.i32 :=.i32 __varargs_array_1.array.i32[0.i32].i32;
__varargs_array_0.array.i32[1.i32].i32 :=.i32 tmp5.i32;
tmp6.i32 :=.i32 __varargs_array_0.array.i32[1.i32].i32;
arr[1.i32].i32 :=.i32 tmp6.i32;
goto while_cond_0;
while_body_0:
tmp8.i32 :=.i32 arr.array.i32[i.i32].i32;
invokestatic(io.io, "println", tmp8.i32).V;
tmp9.i32 :=.i32 i.i32 +.i32 1.i32;
i.i32 :=.i32 tmp9.i32;
while_cond_0:
tmp7.bool :=.bool i.i32 <.bool 2.i32;
if (tmp7.bool) goto while_body_0;
tmp10.i32 :=.i32 arr.array.i32[0.i32].i32;
tmp11.i32 :=.i32 arr.array.i32[tmp10.i32].i32;
tmp12.i32 :=.i32 arr.array.i32[tmp11.i32].i32;
tmp13.i32 :=.i32 arr.array.i32[tmp12.i32].i32;
ret.i32 tmp13.i32;
}

.method public static main(args.array.String).V {
ret.V;
}

.construct X().V {
invokespecial(this, "<init>").V;
}
}