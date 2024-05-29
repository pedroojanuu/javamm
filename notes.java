WhileIfFunctions {


.method otherFunction(value.i32).i32 {
        ret.i32 value.i32;
    }

.method anotherFunction(value.i32).i32 {
        tmp0.i32 :=.i32 20.i32 -.i32 value.i32;
        ret.i32 tmp0.i32;
    }

.method whileTest().i32 {
        value.i32 :=.i32 0.i32;
goto while_cond_0;
        while_body_0:
        tmp2.i32 :=.i32 value.i32 +.i32 1.i32;
        value.i32 :=.i32 tmp2.i32;
        while_cond_0:
        tmp1.bool :=.bool invokevirtual(this.WhileIfFunctions, "otherFunction", value.i32).i32 <.bool invokevirtual(this.WhileIfFunctions, "anotherFunction", value.i32).i32;
        if (tmp1.bool) goto while_body_0;
        ret.i32 value.i32;
    }

.method ifTest().i32 {
        value.i32 :=.i32 5.i32;
        tmp3.bool :=.bool invokevirtual(this.WhileIfFunctions, "otherFunction", value.i32).i32 <.bool invokevirtual(this.WhileIfFunctions, "anotherFunction", value.i32).i32;
        if (tmp3.bool) goto if_then_0;
        tmp4.i32 :=.i32 0.i32 -.i32 100.i32;
        value.i32 :=.i32 tmp4.i32;
goto if_end_0;
        if_then_0:
        value.i32 :=.i32 100.i32;
        if_end_0:
        ret.i32 value.i32;
    }

.construct WhileIfFunctions().V {
        invokespecial(this, "<init>").V;
    }
}
