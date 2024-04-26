import ioPlus;
class SimpleWhileStat {


    public int func(int a){
        int c;
        int i;
        i = 0;

        while (i < a) {
            i = i + 1;
        }

        return i;
    }

}
/*
import ioPlus;
SimpleWhileStat extends Object {


.method public func(a.i32).i32 {
i.i32 :=.i32 0.i32;
goto while_cond_0;
while_body_0:
tmp1.i32 :=.i32 i.i32 +.i32 1.i32;
i.i32 :=.i32 tmp1.i32;
while_cond_0:
tmp0.bool :=.bool i.i32 <.bool a.i32;
if (tmp0.bool) goto while_body_0;
ret.i32 i.i32;
}

.construct SimpleWhileStat().V {
invokespecial(this, "").V;
}
}
 */