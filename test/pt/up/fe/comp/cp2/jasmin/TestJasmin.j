.class public Test
.super java/lang/Object

; Default constructor
.method public <init>()V
    aload_0             ; // Push 'this' reference onto the stack
    invokespecial java/lang/Object/<init>()V  ; //  Call Object's constructor
    return              ; // Return from the constructor
.end method

; Main method
.method public static main([Ljava/lang/String;)V
   .limit stack 99      ; (// Optional) No items needed on stack
   .limit locals 99     ; // (Optional) No local variables needed

   ; Call the foo() method
   invokestatic Test/foo()I

   ; Store the result in local variable (e.g., index 1)
   istore_1

   getstatic java/lang/System.out Ljava/io/PrintStream;
   iload_1
   invokevirtual java/io/PrintStream.println(I)V ;
   return              ; // Return from main (void)
.end method

; foo instance method
.method public static foo()I
   .limit stack 99        ; // Maximum stack size for this method
   .limit locals 99       ; // Space for 4 local variables (including 'this')

   iconst_1              ; // Push the constant 1
   istore_1              ; // Store into local variable 1 (a)
   iconst_2              ; // Push the constant 2
   istore_2              ; // Store into local variable 2 (b)

   iload_1               ; // Load local variable 1 (a) onto the stack
   iload_2               ; // Load local variable 2 (b) onto the stack
   iadd                  ; // Add the top two values on the stack
   istore_3              ; // Store the result into local variable 3 (c)

   iload_3               ; // Load local variable 3 (c)
   ireturn               ; // Return the integer value
.end method