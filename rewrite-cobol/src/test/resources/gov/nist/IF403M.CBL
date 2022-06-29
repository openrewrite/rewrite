      *HEADER,COBOL,IF403M                                                            
000100 IDENTIFICATION DIVISION.                                         IF4034.2
000200 PROGRAM-ID.                                                      IF4034.2
000300      IF403M.                                                     IF4034.2
000400                                                                  IF4034.2
000500                                                                  IF4034.2
000600*THIS PROGRAM TESTS THE FLAGGING OF HIGH SUBSET INTRINSIC FUNCTIONIF4034.2
000700*FEATURES.                                                        IF4034.2
000800******************************************************************IF4034.2
000900*    THIS PROGRAMS CONTAINS TESTS FOR THE FOLLOWING INTRINSIC    *IF4034.2
001000*    FUNCTIONS:  PRESENT-VALUE, RANDOM, RANGE, REM, REVERSE,     *IF4034.2
001100*                SIN, SQRT, STANDARD-DEVIATION, SUM, TAN,        *IF4034.2
001200*                UPPER-CASE, VARIANCE AND WHEN-COMPILED.         *IF4034.2
001300******************************************************************IF4034.2
001400                                                                  IF4034.2
001500 ENVIRONMENT DIVISION.                                            IF4034.2
001600 CONFIGURATION SECTION.                                           IF4034.2
001700 SOURCE-COMPUTER.                                                 IF4034.2
001800     XXXXX082.                                                    IF4034.2
001900 OBJECT-COMPUTER.                                                 IF4034.2
002000     XXXXX083.                                                    IF4034.2
002100                                                                  IF4034.2
002200 DATA DIVISION.                                                   IF4034.2
002300 FILE SECTION.                                                    IF4034.2
002400 WORKING-STORAGE SECTION.                                         IF4034.2
002500 01  WS-AN-TEMP   PICTURE    X(21).                               IF4034.2
002600                                                                  IF4034.2
002700 PROCEDURE DIVISION.                                              IF4034.2
002800 IF403M-PRESENT-VALUE.                                            IF4034.2
002900     IF FUNCTION PRESENT-VALUE (0, 23, 12, 9) =                   IF4034.2
003000        FUNCTION PRESENT-VALUE (0, 23, 12, 9)                     IF4034.2
003100                 CONTINUE.                                        IF4034.2
003200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
003300                                                                  IF4034.2
003400 IF403M-RANDOM.                                                   IF4034.2
003500     IF FUNCTION RANDOM (1) = FUNCTION RANDOM (1)                 IF4034.2
003600                 CONTINUE.                                        IF4034.2
003700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
003800                                                                  IF4034.2
003900 IF403M-RANGE.                                                    IF4034.2
004000     IF FUNCTION RANGE (5, -2, -14, 0) =                          IF4034.2
004100        FUNCTION RANGE (5, -2, -14, 0)                            IF4034.2
004200                 CONTINUE.                                        IF4034.2
004300*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
004400                                                                  IF4034.2
004500 IF403M-REM.                                                      IF4034.2
004600     IF FUNCTION REM (0, 20) = FUNCTION REM (0, 20)               IF4034.2
004700                 CONTINUE.                                        IF4034.2
004800*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
004900                                                                  IF4034.2
005000 IF403M-REVERSE.                                                  IF4034.2
005100     MOVE FUNCTION REVERSE ("ABC") TO WS-AN-TEMP.                 IF4034.2
005200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
005300                                                                  IF4034.2
005400 IF403M-SIN.                                                      IF4034.2
005500     IF FUNCTION SIN (1.0) = FUNCTION SIN (1.0)                   IF4034.2
005600                 CONTINUE.                                        IF4034.2
005700*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
005800                                                                  IF4034.2
005900 IF403M-SQRT.                                                     IF4034.2
006000     IF FUNCTION SQRT (0) = FUNCTION SQRT (0)                     IF4034.2
006100                 CONTINUE.                                        IF4034.2
006200*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
006300                                                                  IF4034.2
006400 IF403M-STANDARD-DEVIATION.                                       IF4034.2
006500     IF FUNCTION STANDARD-DEVIATION (5, -2, -14, 0) =             IF4034.2
006600        FUNCTION STANDARD-DEVIATION (5, -2, -14, 0)               IF4034.2
006700                 CONTINUE.                                        IF4034.2
006800*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
006900                                                                  IF4034.2
007000 IF403M-SUM.                                                      IF4034.2
007100     IF FUNCTION SUM (5, -2, -14, 0) =                            IF4034.2
007200        FUNCTION SUM (5, -2, -14, 0)                              IF4034.2
007300                 CONTINUE.                                        IF4034.2
007400*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
007500                                                                  IF4034.2
007600 IF403M-TAN.                                                      IF4034.2
007700     IF FUNCTION TAN (1.0) = FUNCTION TAN (1.0)                   IF4034.2
007800                 CONTINUE.                                        IF4034.2
007900*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
008000                                                                  IF4034.2
008100 IF403M-UPPER-CASE.                                               IF4034.2
008200     MOVE FUNCTION UPPER-CASE ("abc") TO WS-AN-TEMP.              IF4034.2
008300*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
008400                                                                  IF4034.2
008500 IF403M-VARIANCE.                                                 IF4034.2
008600     IF FUNCTION VARIANCE (5, -2, -14, 0) =                       IF4034.2
008700        FUNCTION VARIANCE (5, -2, -14, 0)                         IF4034.2
008800                 CONTINUE.                                        IF4034.2
008900*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
009000                                                                  IF4034.2
009100 IF403M-WHEN-COMPILED.                                            IF4034.2
009200     MOVE FUNCTION WHEN-COMPILED TO WS-AN-TEMP.                   IF4034.2
009300*MESSAGE EXPECTED FOR ABOVE STATEMENT: NON-CONFORMING STANDARD    IF4034.2
009400                                                                  IF4034.2
009500 IF403M-END.                                                      IF4034.2
009600                                                                  IF4034.2
009700*TOTAL NUMBER OF FLAGS EXPECTED = 13.                             IF4034.2
      *END-OF,IF403M                                                                  
