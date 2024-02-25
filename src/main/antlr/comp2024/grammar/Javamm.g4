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

IMPORT : 'import';
CLASS : 'class';
EXTENDS : 'extends';
PUBLIC : 'public';
RETURN : 'return';
STATIC : 'static';
MAIN : 'main';
VOID : 'void';
STRING : 'String';
INT : 'int';
BOOLEAN : 'boolean';
IF : 'if';
ELSE : 'else';
WHILE : 'while';
LENGTH : 'length';
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
    : importList+=importDecl* class=classDecl EOF
    ;

importDecl
    : IMPORT id+=ID ('.' id+=ID)* SEMI
    ;

classDecl
    : CLASS name=ID (EXTENDS extended_name=ID)?
        LCURLY
        varList+=varDecl*
        methodList+=methodDecl*
        RCURLY
    ;

varDecl
    : varType=type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        methodType=type name=ID
        LPAREN parameters+=param(',' parameters+=param)* RPAREN
        LCURLY vars+=varDecl* stmts+=stmt* RETURN returnExpr=expr SEMI RCURLY #OtherMethod
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN STRING LSQUARE RSQUARE parameter=ID RPAREN
        LCURLY vars+=varDecl* stmts+=stmt* RCURLY #MainMethod
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
    : LCURLY stmts+=stmt* RCURLY #StmtGroup
    | IF LPAREN condition=expr RPAREN ifBody=stmt ELSE elseBody=stmt #IfStmt
    | WHILE LPAREN condition=expr RPAREN body=stmt #WhileStmt
    | expr SEMI #ExprStmt
    | ID EQUALS value=expr SEMI #AssignStmt
    | ID LSQUARE index=expr RSQUARE EQUALS value=expr SEMI #ArrayAssignStmt
    ;

expr
    : EXCL expr #NotExpr
    | left=expr op=(MUL | DIV) right=expr #BinaryExpr
    | left=expr op=(ADD | SUB) right=expr #BinaryExpr
    | left=expr op=LT right=expr #BinaryExpr
    | left=expr op=AND right=expr #BinaryExpr
    | name=expr LSQUARE index=expr RSQUARE #ArrayIndexExpr
    | expr '.' LENGTH #LenExpr
    | object=expr '.' method=ID LPAREN (args+=expr(',' args+=expr)*)? RPAREN #MethodCallExpr
    | NEW INT LSQUARE size=expr RSQUARE #NewArrayExpr
    | NEW name=ID LPAREN RPAREN #NewObjExpr
    | LPAREN expr RPAREN #ParenExpr
    | LSQUARE (elems+=expr(',' elems+=expr)*)? RSQUARE #ArrayDeclExpr
    | value=INTEGER #IntLiteralExpr
    | TRUE #TrueLiteralExpr
    | FALSE #FalseLiteralExpr
    | name=ID #IDLiteralExpr
    | THIS #ThisExpr
    ;
