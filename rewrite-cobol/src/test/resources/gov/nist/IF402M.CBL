      *HEADER,COBOL,IF402M                                                            
000100 IDENTIFICATION DIVISION.                                         IF4024.2
000200 PROGRAM-ID.                                                      IF4024.2
000300      IF402M.                                                     IF4024.2
000400                                                                  IF4024.2
000500                                                                  IF4024.2
000600*THIS PROGRAM TESTS THE FLAGGING OF HIGH SUBSET INTRINSIC FUNCTIONIF4024.2
000700*FEATURES.                                                        IF4024.2
000800******************************************************************IF4024.2
000900*    THIS PROGRAMS CONTAINS TESTS FOR THE FOLLOWING INTRINSIC    *IF4024.2
001000*    FUNCTIONS:  LENGTH, LOG, LOG10, LOWER-CASE, MAX, MEAN,      *IF4024.2
001100*                MEDIAN, MIDRANGE, MIN, MOD, NUMVAL, NUMVAL-C,   *IF4024.2
001200*                ORD, ORD-MAX  AND ORD-MIN.                      *IF4024.2
001300******************************************************************IF4024.2
001400                                                                  IF4024.2
001500 ENVIRONMENT DIVISION.                                            IF4024.2
001600 CONFIGURATION SECTION.                                           IF4024.2
001700 SOURCE-COMPUTER.                                                 IF4024.2
001800     XXXXX082.                                                    IF4024.2
001900 OBJECT-COMPUTER.                                                 IF4024.2
002000     XXXXX083.                                                    IF4024.2
002100                                                                  IF4024.2
002200 DATA DIVISION.                                                   IF4024.2
002300 FILE SECTION.                                                    IF4024.2
002400 WORKING-STORAGE SECTION.                                         IF4024.2
002500 01  WS-AN-TEMP   PICTURE    X(3).                                IF4024.2
002600 01  WS-TABLE-VALUE   PICTURE X(27)                               IF4024.2
002700                      VALUE "ABCDEFGHIJKLMNOPQRSTUVWXYZ ".        IF4024.2
002800 01  WS-TABLE-TOTAL REDEFINES WS-TABLE-VALUE.                     IF4024.2
002900     05  WS-TABLE-LV3 OCCURS 3 TIMES.                             IF4024.2
003000         10  WS-TABLE-LV2 OCCURS 3 TIMES.                         IF4024.2
003100             15  WS-TABLE PICTURE X OCCURS 3 TIMES.               IF4024.2
003200                                                                  IF4024.2
003300 PROCEDURE DIVISION.                                              IF4024.2
003400 IF402M-LENGTH.                                                   IF4024.2
003500     IF FUNCTION LENGTH ("ABC") = FUNCTION LENGTH ("ABC")         IF4024.2
003600                 CONTINUE.                                        IF4024.2
003700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
003800                                                                  IF4024.2
003900 IF402M-LOG.                                                      IF4024.2
004000     IF FUNCTION LOG (1.0) = FUNCTION LOG (1.0)                   IF4024.2
004100                 CONTINUE.                                        IF4024.2
004200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
004300                                                                  IF4024.2
004400 IF402M-LOG10.                                                    IF4024.2
004500     IF FUNCTION LOG10 (1.0) = FUNCTION LOG10 (1.0)               IF4024.2
004600                 CONTINUE.                                        IF4024.2
004700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
004800                                                                  IF4024.2
004900 IF402M-LOWER-CASE.                                               IF4024.2
005000     MOVE FUNCTION LOWER-CASE ("ABC") TO WS-AN-TEMP.              IF4024.2
005100*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
005200                                                                  IF4024.2
005300 IF402M-MAX.                                                      IF4024.2
005400     IF FUNCTION MAX (5, 6, 10, 3, 7) =                           IF4024.2
005500        FUNCTION MAX (5, 6, 10, 3, 7)                             IF4024.2
005600                 CONTINUE.                                        IF4024.2
005700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
005800                                                                  IF4024.2
005900     MOVE FUNCTION MAX (WS-TABLE (ALL, ALL, ALL)) TO WS-AN-TEMP.  IF4024.2
006000*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
006100                                                                  IF4024.2
006200 IF402M-MEAN.                                                     IF4024.2
006300     IF FUNCTION MEAN (5, -2, -14, 0) =                           IF4024.2
006400        FUNCTION MEAN (5, -2, -14, 0)                             IF4024.2
006500                 CONTINUE.                                        IF4024.2
006600*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
006700                                                                  IF4024.2
006800 IF402M-MEDIAN.                                                   IF4024.2
006900     IF FUNCTION MEDIAN (5, -2, -14, 0) =                         IF4024.2
007000        FUNCTION MEDIAN (5, -2, -14, 0)                           IF4024.2
007100                 CONTINUE.                                        IF4024.2
007200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
007300                                                                  IF4024.2
007400 IF402M-MIDRANGE.                                                 IF4024.2
007500     IF FUNCTION MIDRANGE (5, -2, -14, 0) =                       IF4024.2
007600        FUNCTION MIDRANGE (5, -2, -14, 0)                         IF4024.2
007700                 CONTINUE.                                        IF4024.2
007800*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
007900                                                                  IF4024.2
008000 IF402M-MIN.                                                      IF4024.2
008100     IF FUNCTION MIN (5, 6, 10, 3, 7) =                           IF4024.2
008200        FUNCTION MIN (5, 6, 10, 3, 7)                             IF4024.2
008300                 CONTINUE.                                        IF4024.2
008400*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
008500                                                                  IF4024.2
008600     MOVE FUNCTION MIN (WS-TABLE (ALL, ALL, ALL)) TO WS-AN-TEMP.  IF4024.2
008700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
008800                                                                  IF4024.2
008900 IF402M-MOD.                                                      IF4024.2
009000     IF FUNCTION MOD (6, 6) = FUNCTION MOD (6, 6)                 IF4024.2
009100                 CONTINUE.                                        IF4024.2
009200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
009300                                                                  IF4024.2
009400 IF402M-NUMVAL.                                                   IF4024.2
009500     IF FUNCTION NUMVAL ("4738") = FUNCTION NUMVAL ("4738")       IF4024.2
009600                 CONTINUE.                                        IF4024.2
009700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
009800                                                                  IF4024.2
009900 IF402M-NUMVAL-C.                                                 IF4024.2
010000     IF FUNCTION NUMVAL-C ("-$1,234.56") =                        IF4024.2
010100        FUNCTION NUMVAL-C ("-$1,234.56")                          IF4024.2
010200                 CONTINUE.                                        IF4024.2
010300*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
010400                                                                  IF4024.2
010500 IF402M-ORD.                                                      IF4024.2
010600     IF FUNCTION ORD ("A") = FUNCTION ORD ("A")                   IF4024.2
010700                 CONTINUE.                                        IF4024.2
010800*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
010900                                                                  IF4024.2
011000 IF402M-ORD-MAX.                                                  IF4024.2
011100     IF FUNCTION ORD-MAX (5, 3, 2, 8, 3, 1) =                     IF4024.2
011200        FUNCTION ORD-MAX (5, 3, 2, 8, 3, 1)                       IF4024.2
011300                 CONTINUE.                                        IF4024.2
011400*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
011500                                                                  IF4024.2
011600 IF402M-ORD-MIN.                                                  IF4024.2
011700     IF FUNCTION ORD-MIN (5, 3, 2, 8, 3, 1) =                     IF4024.2
011800        FUNCTION ORD-MIN (5, 3, 2, 8, 3, 1)                       IF4024.2
011900                 CONTINUE.                                        IF4024.2
012000*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4024.2
012100                                                                  IF4024.2
012200 IF402M-END.                                                      IF4024.2
012300                                                                  IF4024.2
012400*TOTAL NUMBER OF FLAGS EXPECTED = 17.                             IF4024.2
      *END-OF,IF402M                                                                  
