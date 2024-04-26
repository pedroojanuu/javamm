import Other;
// import Person;

class BasicMethods extends Other {

    public int[] func4(){
        int[] k;
        k = new int[1];
        return k;
    }

}

/*
import Other;
BasicMethods extends Other {


.method public func4().array.i32 {
tmp0.i32 :=.i32 1.i32;
tmp1.array.i32 :=.array.i32 new(array, tmp0.i32).array.i32;
k.array.i32 :=.array.i32 tmp1.array.i32;
ret.array.i32 k.array.i32;
}

.construct BasicMethods().V {
invokespecial(this, "").V;
}
}
 */