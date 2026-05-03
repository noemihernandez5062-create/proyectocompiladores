import java_cup.runtime.Symbol;

%%

%class Lexer
%unicode
%cup
%line
%column

%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline + 1, yycolumn + 1);
  }

  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline + 1, yycolumn + 1, value);
  }
%}

WHITE = [ \t\r\n\f]+
ID = [a-zA-Z_][a-zA-Z0-9_]*
NUM = [0-9]+(\.[0-9]+)?
STRING = \'([^\'\\]|\\.)*\'
LINE_COMMENT = "--".*
BLOCK_COMMENT = "/\\*"([^*]|\\*+[^*/])*"\\*/"

%%

{WHITE}         { }
{LINE_COMMENT}  { }
{BLOCK_COMMENT} { }

"CREATE"|"create"        { return symbol(sym.CREATE); }
"DATABASE"|"database"    { return symbol(sym.DATABASE); }
"USE"|"use"              { return symbol(sym.USE); }
"TABLE"|"table"          { return symbol(sym.TABLE); }
"SELECT"|"select"        { return symbol(sym.SELECT); }
"FROM"|"from"            { return symbol(sym.FROM); }
"WHERE"|"where"          { return symbol(sym.WHERE); }
"UPDATE"|"update"        { return symbol(sym.UPDATE); }
"SET"|"set"              { return symbol(sym.SET); }
"INSERT"|"insert"        { return symbol(sym.INSERT); }
"INTO"|"into"            { return symbol(sym.INTO); }
"VALUES"|"values"        { return symbol(sym.VALUES); }
"JOIN"|"join"            { return symbol(sym.JOIN); }
"ON"|"on"                { return symbol(sym.ON); }
"AS"|"as"                { return symbol(sym.AS); }

"INT"|"int"              { return symbol(sym.INT_TYPE); }
"VARCHAR"|"varchar"      { return symbol(sym.VARCHAR); }
"DATETIME"|"datetime"    { return symbol(sym.DATETIME); }
"DECIMAL"|"decimal"      { return symbol(sym.DECIMAL); }
"CONTEO"|"conteo"        { return symbol(sym.CONTEO); }

"("             { return symbol(sym.LPAREN); }
")"             { return symbol(sym.RPAREN); }
","             { return symbol(sym.COMMA); }
";"             { return symbol(sym.SEMI); }
"."             { return symbol(sym.DOT); }
"*"             { return symbol(sym.STAR); }
"="             { return symbol(sym.ASSIGN); }

{NUM}           { return symbol(sym.NUMBER, yytext()); }
{STRING}        { return symbol(sym.STRING_LITERAL, yytext()); }
{ID}            { return symbol(sym.IDENT, yytext()); }

. {
    System.err.println("Error lexico: simbolo invalido '" + yytext() +
                       "' en linea " + (yyline + 1) +
                       ", columna " + (yycolumn + 1));
    System.exit(1);
}

<<EOF>>         { return symbol(sym.EOF); }
