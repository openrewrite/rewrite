      *HEADER,COBOL,IC207A,SUBRTN,IC208A                                              
000100 IDENTIFICATION DIVISION.                                         IC2084.2
000200 PROGRAM-ID.                                                      IC2084.2
000300     IC208A.                                                      IC2084.2
000400****************************************************************  IC2084.2
000500*                                                              *  IC2084.2
000600*    VALIDATION FOR:-                                          *  IC2084.2
000700*                                                              *  IC2084.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC2084.2
000900*                                                              *  IC2084.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC2084.2
001100*                                                              *  IC2084.2
001200****************************************************************  IC2084.2
001300*                                                              *  IC2084.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC2084.2
001500*                                                              *  IC2084.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC2084.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC2084.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC2084.2
001900*                                                              *  IC2084.2
002000****************************************************************  IC2084.2
002100*        THE SUBPROGRAM IC208 CONTAINS TABLES AND AN INDEX        IC2084.2
002200*    DATA ITEM WHICH ARE DEFINED IN THE LINKAGE SECTION           IC2084.2
002300*    AND NAMED AS OPERANDS IN THE USING PHRASE OF THE             IC2084.2
002400*    PROCEDURE DIVISION HEADER.  ONE OF THE TABLES IS DEFINED     IC2084.2
002500*    WITH AN OCCURS DEPENDING ON CLAUSE AND HAS CONDITION-NAME    IC2084.2
002600*    ENTRIES ASSOCIATED WITH IT.  THE SEARCH STATEMENT IS USED    IC2084.2
002700*    TO TEST THE VARIABLE LENGTH TABLE CAPABILITY.                IC2084.2
002800****************************************************************  IC2084.2
002900 ENVIRONMENT DIVISION.                                            IC2084.2
003000 CONFIGURATION SECTION.                                           IC2084.2
003100 SOURCE-COMPUTER.                                                 IC2084.2
003200     XXXXX082.                                                    IC2084.2
003300 OBJECT-COMPUTER.                                                 IC2084.2
003400     XXXXX083.                                                    IC2084.2
003500 DATA DIVISION.                                                   IC2084.2
003600 LINKAGE SECTION.                                                 IC2084.2
003700 77  INDEX-1 USAGE IS INDEX.                                      IC2084.2
003800 77  DN3 PICTURE 99.                                              IC2084.2
003900 01  TABLE-01.                                                    IC2084.2
004000     02  DN1 PICTURE X                                            IC2084.2
004100             OCCURS 1 TO 15 TIMES                                 IC2084.2
004200             DEPENDING ON DN3                                     IC2084.2
004300             INDEXED BY IN1.                                      IC2084.2
004400         88  CN1 VALUE "A".                                       IC2084.2
004500         88  CN2 VALUE "H".                                       IC2084.2
004600         88  CN3 VALUE "O".                                       IC2084.2
004700 01  TABLE-02.                                                    IC2084.2
004800     02  DN2 PICTURE X                                            IC2084.2
004900             OCCURS 8 TIMES.                                      IC2084.2
005000 PROCEDURE DIVISION USING TABLE-01, TABLE-02, INDEX-1, DN3.       IC2084.2
005100 SECT-IC208-0001 SECTION.                                         IC2084.2
005200 LINK-TEST-01.                                                    IC2084.2
005300*    THIS TEST USES THE INDEX DATA ITEM SET IN THE CALLING        IC2084.2
005400*    PROGRAM TO SET AN INDEX AND REFERENCE A TABLE ITEM.          IC2084.2
005500     SET IN1 TO INDEX-1.                                          IC2084.2
005600     MOVE DN1 (IN1) TO DN2 (1).                                   IC2084.2
005700*        LINK-TEST-02 TESTS THE VARIABLE LENGTH TABLE CAPABILITY  IC2084.2
005800*    WITH THE DATA-NAME WHOSE CONTENTS IS THE TABLE LENGTH        IC2084.2
005900*    DEFINED IN THE LINKAGE SECTION.                              IC2084.2
006000 LINK-TEST-02-01.                                                 IC2084.2
006100     SET IN1 TO 1.                                                IC2084.2
006200     SEARCH DN1 VARYING IN1                                       IC2084.2
006300         AT END MOVE "Z" TO DN2 (2)                               IC2084.2
006400         WHEN DN1 (IN1) EQUAL TO "D"                              IC2084.2
006500             MOVE "D" TO DN2 (2).                                 IC2084.2
006600 LINK-TEST-02-02.                                                 IC2084.2
006700     SET IN1 TO 1.                                                IC2084.2
006800     SEARCH DN1 VARYING IN1                                       IC2084.2
006900         AT END MOVE "Y" TO DN2 (3)                               IC2084.2
007000         WHEN DN1 (IN1) EQUAL TO "B"                              IC2084.2
007100             MOVE "B" TO DN2 (3).                                 IC2084.2
007200 LINK-TEST-02-03.                                                 IC2084.2
007300     MOVE 7 TO DN3.                                               IC2084.2
007400     SET IN1 TO 1.                                                IC2084.2
007500     SEARCH DN1 VARYING IN1                                       IC2084.2
007600         AT END MOVE "X" TO DN2 (4)                               IC2084.2
007700         WHEN DN1 (IN1) EQUAL TO "H"                              IC2084.2
007800             MOVE "H" TO DN2 (4).                                 IC2084.2
007900 LINK-TEST-02-04.                                                 IC2084.2
008000     SET IN1 TO 1.                                                IC2084.2
008100     SEARCH DN1 VARYING IN1                                       IC2084.2
008200         AT END MOVE "W" TO DN2 (5)                               IC2084.2
008300         WHEN DN1 (IN1) EQUAL TO "G"                              IC2084.2
008400             MOVE "G" TO DN2 (5).                                 IC2084.2
008500 LINK-TEST-03.                                                    IC2084.2
008600     MOVE 10 TO DN3.                                              IC2084.2
008700*        LINK-TEST-03 TESTS THE USE OF CONDITION-NAMES WHICH      IC2084.2
008800*    WERE DEFINED IN THE LINKAGE SECTION.                         IC2084.2
008900 LINK-TEST-03-01.                                                 IC2084.2
009000     IF CN1 (1)  MOVE "A" TO DN2 (6).                             IC2084.2
009100 LINK-TEST-03-02.                                                 IC2084.2
009200     IF CN1 (5)  MOVE "N" TO DN2 (7)                              IC2084.2
009300         ELSE MOVE "V" TO DN2 (7).                                IC2084.2
009400 LINK-TEST-03-03.                                                 IC2084.2
009500     IF CN2 (8) MOVE "H" TO DN2 (8).                              IC2084.2
009600 LINK-TEST-04.                                                    IC2084.2
009700*        THIS TEST SETS THE INDEX DATA ITEM AND TABLE LENGTH      IC2084.2
009800*    FOR REFERENCE IN THE CALLING PROGRAM.                        IC2084.2
009900     SET IN1 TO 2.                                                IC2084.2
010000     SET INDEX-1 TO IN1.                                          IC2084.2
010100     MOVE 9 TO DN3.                                               IC2084.2
010200 EXIT-IC208.                                                      IC2084.2
010300     EXIT PROGRAM.                                                IC2084.2
      *END-OF,IC208A                                                                  
