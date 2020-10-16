lexer grammar VersionRangeLexer;

COMMA               :     ',' ;

PROPERTY_OPEN       :     '${';
PROPERTY_CLOSE      :     '}';

OPEN_RANGE_OPEN     :     '(' ;
OPEN_RANGE_CLOSE    :     ')' ;
CLOSED_RANGE_OPEN   :     '[' ;
CLOSED_RANGE_CLOSE  :     ']' ;

Version             :     [0-9+\-_.a-zA-Z]+;
