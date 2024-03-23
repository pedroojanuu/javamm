grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSQUARE : '[';
RSQUARE : ']';
MUL : '*' ;
DIV : '/';
ADD : '+' ;
SUB : '-';
ELLIPSIS : '...';
AND : '&&';
LT : '<';
EXCL : '!';

// Reserved words
IMPORT : 'import';
CLASS : 'class';
EXTENDS : 'extends';
PUBLIC : 'public';
RETURN : 'return';
STATIC : 'static';
VOID : 'void';
INT : 'int';
BOOLEAN : 'boolean';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
NEW : 'new';
TRUE : 'true';
FALSE : 'false';
THIS : 'this';

INTEGER : '0' | [1-9] [0-9]* ;
ID : [a-zA-Z$_] [a-zA-Z0-9$_]* ;

WS : [ \t\n\r\f]+ -> skip ;

SINGLE_LINE_COMMENT : ('//' .*? '\n') -> skip;
MULTI_LINE_COMMENT: ('/*' .*? '*/') -> skip;

program
    : importDecl* classDeclaration=classDecl EOF
    ;

importDecl
    : IMPORT id+=ID ('.' id+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID (EXTENDS extended_name=ID)?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : varType=type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : (PUBLIC {$isPublic=true;})?
        methodType=type name=ID
        LPAREN (param (',' param)*)? RPAREN
        LCURLY varDecl* stmt* RETURN returnExpr=expr SEMI RCURLY #OtherMethod
    | (PUBLIC {$isPublic=true;})?
        STATIC {$isStatic=true;}
        methodType=VOID name=ID  // name="main"
        LPAREN ID LSQUARE RSQUARE parameterName=ID RPAREN   // String[]
        LCURLY varDecl* stmt* RCURLY #MainMethod
    ;

type
    : INT LSQUARE RSQUARE #IntArray
    | INT ELLIPSIS #VarArgs
    | BOOLEAN #Boolean
    | INT #Int
    | name=ID #OtherClasses
    ;

param
    : paramType=type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #StmtGroup
    | IF LPAREN condition=expr RPAREN ifBody=stmt ELSE elseBody=stmt #IfStmt
    | WHILE LPAREN condition=expr RPAREN body=stmt #WhileStmt
    | expr SEMI #ExprStmt
    | id=ID EQUALS value=expr SEMI #AssignStmt
    | id=ID LSQUARE index=expr RSQUARE EQUALS value=expr SEMI #ArrayAssignStmt
    ;

expr
    : LPAREN expr RPAREN #ParenExpr
    | name=expr LSQUARE index=expr RSQUARE #ArrayIndexExpr
    | object=expr '.' method=ID LPAREN arglist? RPAREN #MethodCallExpr
    | object=expr '.' member=ID #MemberAccessExpr  // Member access (e.g. this.variable, array.length)
    | EXCL expr #NotExpr
    | NEW INT LSQUARE size=expr RSQUARE #NewArrayExpr
    | NEW id=ID LPAREN RPAREN #NewObjExpr
    | left=expr op=(MUL | DIV) right=expr #BinaryExpr
    | left=expr op=(ADD | SUB) right=expr #BinaryExpr
    | left=expr op=LT right=expr #BinaryExpr
    | left=expr op=AND right=expr #BinaryExpr
    | value=INTEGER #IntLiteralExpr
    | TRUE #TrueLiteralExpr
    | FALSE #FalseLiteralExpr
    | id=ID #IdLiteralExpr
    | THIS #ThisExpr
    | LSQUARE (elems+=expr (',' elems+=expr)*)? RSQUARE #ArrayDeclExpr
    ;

arglist
    : args+=expr(',' args+=expr)*
    ;
