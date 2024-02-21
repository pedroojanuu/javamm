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

INTEGER : [0-9] ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : importDecl* classDecl EOF
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
    : type name=ID SEMI
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN parameters+=param(',' parameters+=param)* RPAREN
        LCURLY varDecl* stmt* RETURN expr SEMI RCURLY #OtherMethod
    | (PUBLIC {$isPublic=true;})?
        STATIC VOID MAIN
        LPAREN STRING LSQUARE RSQUARE parameter=ID RPAREN
        LCURLY varDecl* stmt* RCURLY #MainMethod
    ;

type
    : name = INT LSQUARE RSQUARE #IntArray
    | name = INT ELLIPSIS #VarArgs
    | name = BOOLEAN #Boolean
    | name = INT #Int
    | ID #OtherClasses
    ;

param
    : type name=ID
    ;

stmt
    : LCURLY stmt* RCURLY #StmtGroup
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | ID EQUALS expr SEMI #AssignStmt
    | ID LSQUARE expr RSQUARE EQUALS expr SEMI #ArrayAssignStmt
    ;

expr
    : EXCL expr #NotExpr
    | expr op=(MUL | DIV) expr #BinaryExpr
    | expr op=(ADD | SUB) expr #BinaryExpr
    | expr op=LT expr #BinaryExpr
    | expr op=AND expr #BinaryExpr
    | expr LSQUARE expr RSQUARE #ArrayIndexExpr
    | expr '.' LENGTH #LenExpr
    | expr '.' id=ID LPAREN (expressions+=expr(',' expressions+=expr)*)? RPAREN #MethodCallExpr
    | NEW INT LSQUARE expr RSQUARE #NewArrayExpr
    | NEW id=ID LPAREN RPAREN #NewObjExpr
    | LPAREN expr RPAREN #ParenExpr
    | LSQUARE (expressions+=expr(',' expressions+=expr)*)? RSQUARE #ArrayExpr
    | value=INTEGER #IntLiteralExpr
    | TRUE #TrueLiteralExpr
    | FALSE #FalseLiteralExpr
    | name=ID #IDLiteralExpr
    | THIS #ThisExpr
    ;



