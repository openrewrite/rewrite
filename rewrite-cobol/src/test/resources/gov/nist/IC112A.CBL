      *HEADER,COBOL,IC112A                                                            
000100 IDENTIFICATION DIVISION.                                         IC1124.2
000200 PROGRAM-ID.                                                      IC1124.2
000300     IC112A.                                                      IC1124.2
000400****************************************************************  IC1124.2
000500*                                                              *  IC1124.2
000600*    VALIDATION FOR:-                                          *  IC1124.2
000700*                                                              *  IC1124.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1124.2
000900*                                                              *  IC1124.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1124.2
001100*                                                              *  IC1124.2
001200****************************************************************  IC1124.2
001300*                                                              *  IC1124.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  IC1124.2
001500*                                                              *  IC1124.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  IC1124.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  IC1124.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  IC1124.2
001900*                                                              *  IC1124.2
002000****************************************************************  IC1124.2
002100****************************************************************  IC1124.2
002200*                                                                 IC1124.2
002300*         THE ROUTINE IC112 IS A MAIN PROGRAM WHICH HAS A FILE    IC1124.2
002400*    DESCRIPTION FOR A SEQUENTIAL MASS STORAGE FILE WITH FIXED    IC1124.2
002500*    LENGTH RECORDS.  THE FILE IS CREATED, CLOSED AND OPENED AS   IC1124.2
002600*    AN INPUT FILE.  THE MAIN ROUTINE READS THE FILE AND VERIFIES IC1124.2
002700*    THAT THE FILE IS CORRECT.  THE FILE IS CLOSED AND OPENED     IC1124.2
002800*    AGAIN AS AN INPUT FILE.  A RECORD IS READ AND A CALL IS MADE IC1124.2
002900*    TO THE SUBPROGRAM IC113 WITH THE FILE DESCRIPTION 01 RECORD  IC1124.2
003000*    LISTED AS ONE OF THE OPERANDS OF THE USING PHRASE.  THE      IC1124.2
003100*    SUBPROGRAM IC113 COMPARES THE FIELDS IN THE INPUT RECORD TO  IC1124.2
003200*    THE VALUES WRITTEN WHEN THE FILE WAS CREATED.                IC1124.2
003300*                                                                 IC1124.2
003400*         THIS PROGRAM WAS ADAPTED FROM THE SEQUENTIAL I-O TEST   IC1124.2
003500*    CONTAINED IN ROUTINE SQ104.  IF ANY ERRORS OCCUR IN RUNNING  IC1124.2
003600*    THE ROUTINE SQ104, THE RESULTS OF THE TESTS IN THE ROUTINES  IC1124.2
003700*    IC112 AND IC113 ARE INCONCLUSIVE.                            IC1124.2
003800*                                                                 IC1124.2
003900*******************************************                       IC1124.2
004000 ENVIRONMENT DIVISION.                                            IC1124.2
004100 CONFIGURATION SECTION.                                           IC1124.2
004200 SOURCE-COMPUTER.                                                 IC1124.2
004300     XXXXX082.                                                    IC1124.2
004400 OBJECT-COMPUTER.                                                 IC1124.2
004500     XXXXX083.                                                    IC1124.2
004600 INPUT-OUTPUT SECTION.                                            IC1124.2
004700 FILE-CONTROL.                                                    IC1124.2
004800     SELECT PRINT-FILE ASSIGN TO                                  IC1124.2
004900     XXXXX055.                                                    IC1124.2
005000     SELECT SQ-FS3 ASSIGN TO                                      IC1124.2
005100     XXXXX014                                                     IC1124.2
005200     ORGANIZATION IS SEQUENTIAL                                   IC1124.2
005300     ACCESS MODE IS SEQUENTIAL.                                   IC1124.2
005400 DATA DIVISION.                                                   IC1124.2
005500 FILE SECTION.                                                    IC1124.2
005600 FD  PRINT-FILE.                                                  IC1124.2
005700 01  PRINT-REC PICTURE X(120).                                    IC1124.2
005800 01  DUMMY-RECORD PICTURE X(120).                                 IC1124.2
005900 FD  SQ-FS3                                                       IC1124.2
006000     BLOCK CONTAINS 120 CHARACTERS                                IC1124.2
006100     RECORD CONTAINS 120 CHARACTERS                               IC1124.2
006200     LABEL RECORDS ARE STANDARD                                   IC1124.2
006300C    VALUE OF                                                     IC1124.2
006400C    XXXXX074                                                     IC1124.2
006500C    IS                                                           IC1124.2
006600C    XXXXX075                                                     IC1124.2
006700*    XXXXX069                                                     IC1124.2
006800     DATA RECORD SQ-FS3R1-F-G-120.                                IC1124.2
006900 01  SQ-FS3R1-F-G-120.                                            IC1124.2
007000     02  FILLER PIC X(120).                                       IC1124.2
007100 WORKING-STORAGE SECTION.                                         IC1124.2
007200 01  WRK-CS-09V00 PICTURE S9(9) USAGE COMP VALUE ZERO.            IC1124.2
007300 01  RECORDS-IN-ERROR  PIC S9(5) USAGE COMP VALUE 0.              IC1124.2
007400 01  ERROR-FLAG PICTURE 9 VALUE 0.                                IC1124.2
007500 01  EOF-FLAG PICTURE 9 VALUE 0.                                  IC1124.2
007600 01  FILE-RECORD-INFORMATION-REC.                                 IC1124.2
007700     03 FILE-RECORD-INFO-SKELETON.                                IC1124.2
007800        05 FILLER                 PICTURE X(48)       VALUE       IC1124.2
007900             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IC1124.2
008000        05 FILLER                 PICTURE X(46)       VALUE       IC1124.2
008100             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IC1124.2
008200        05 FILLER                 PICTURE X(26)       VALUE       IC1124.2
008300             ",LFIL=000000,ORG=  ,LBLR= ".                        IC1124.2
008400        05 FILLER                 PICTURE X(37)       VALUE       IC1124.2
008500             ",RECKEY=                             ".             IC1124.2
008600        05 FILLER                 PICTURE X(38)       VALUE       IC1124.2
008700             ",ALTKEY1=                             ".            IC1124.2
008800        05 FILLER                 PICTURE X(38)       VALUE       IC1124.2
008900             ",ALTKEY2=                             ".            IC1124.2
009000        05 FILLER                 PICTURE X(7)        VALUE SPACE.IC1124.2
009100     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IC1124.2
009200        05 FILE-RECORD-INFO-P1-120.                               IC1124.2
009300           07 FILLER              PIC X(5).                       IC1124.2
009400           07 XFILE-NAME           PIC X(6).                      IC1124.2
009500           07 FILLER              PIC X(8).                       IC1124.2
009600           07 XRECORD-NAME         PIC X(6).                      IC1124.2
009700           07 FILLER              PIC X(1).                       IC1124.2
009800           07 REELUNIT-NUMBER     PIC 9(1).                       IC1124.2
009900           07 FILLER              PIC X(7).                       IC1124.2
010000           07 XRECORD-NUMBER       PIC 9(6).                      IC1124.2
010100           07 FILLER              PIC X(6).                       IC1124.2
010200           07 UPDATE-NUMBER       PIC 9(2).                       IC1124.2
010300           07 FILLER              PIC X(5).                       IC1124.2
010400           07 ODO-NUMBER          PIC 9(4).                       IC1124.2
010500           07 FILLER              PIC X(5).                       IC1124.2
010600           07 XPROGRAM-NAME        PIC X(5).                      IC1124.2
010700           07 FILLER              PIC X(7).                       IC1124.2
010800           07 XRECORD-LENGTH       PIC 9(6).                      IC1124.2
010900           07 FILLER              PIC X(7).                       IC1124.2
011000           07 CHARS-OR-RECORDS    PIC X(2).                       IC1124.2
011100           07 FILLER              PIC X(1).                       IC1124.2
011200           07 XBLOCK-SIZE          PIC 9(4).                      IC1124.2
011300           07 FILLER              PIC X(6).                       IC1124.2
011400           07 RECORDS-IN-FILE     PIC 9(6).                       IC1124.2
011500           07 FILLER              PIC X(5).                       IC1124.2
011600           07 XFILE-ORGANIZATION   PIC X(2).                      IC1124.2
011700           07 FILLER              PIC X(6).                       IC1124.2
011800           07 XLABEL-TYPE          PIC X(1).                      IC1124.2
011900        05 FILE-RECORD-INFO-P121-240.                             IC1124.2
012000           07 FILLER              PIC X(8).                       IC1124.2
012100           07 XRECORD-KEY          PIC X(29).                     IC1124.2
012200           07 FILLER              PIC X(9).                       IC1124.2
012300           07 ALTERNATE-KEY1      PIC X(29).                      IC1124.2
012400           07 FILLER              PIC X(9).                       IC1124.2
012500           07 ALTERNATE-KEY2      PIC X(29).                      IC1124.2
012600           07 FILLER              PIC X(7).                       IC1124.2
012700 01  TEST-RESULTS.                                                IC1124.2
012800     02 FILLER                   PIC X      VALUE SPACE.          IC1124.2
012900     02 FEATURE                  PIC X(20)  VALUE SPACE.          IC1124.2
013000     02 FILLER                   PIC X      VALUE SPACE.          IC1124.2
013100     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IC1124.2
013200     02 FILLER                   PIC X      VALUE SPACE.          IC1124.2
013300     02  PAR-NAME.                                                IC1124.2
013400       03 FILLER                 PIC X(19)  VALUE SPACE.          IC1124.2
013500       03  PARDOT-X              PIC X      VALUE SPACE.          IC1124.2
013600       03 DOTVALUE               PIC 99     VALUE ZERO.           IC1124.2
013700     02 FILLER                   PIC X(8)   VALUE SPACE.          IC1124.2
013800     02 RE-MARK                  PIC X(61).                       IC1124.2
013900 01  TEST-COMPUTED.                                               IC1124.2
014000     02 FILLER                   PIC X(30)  VALUE SPACE.          IC1124.2
014100     02 FILLER                   PIC X(17)  VALUE                 IC1124.2
014200            "       COMPUTED=".                                   IC1124.2
014300     02 COMPUTED-X.                                               IC1124.2
014400     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IC1124.2
014500     03 COMPUTED-N               REDEFINES COMPUTED-A             IC1124.2
014600                                 PIC -9(9).9(9).                  IC1124.2
014700     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IC1124.2
014800     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IC1124.2
014900     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IC1124.2
015000     03       CM-18V0 REDEFINES COMPUTED-A.                       IC1124.2
015100         04 COMPUTED-18V0                    PIC -9(18).          IC1124.2
015200         04 FILLER                           PIC X.               IC1124.2
015300     03 FILLER PIC X(50) VALUE SPACE.                             IC1124.2
015400 01  TEST-CORRECT.                                                IC1124.2
015500     02 FILLER PIC X(30) VALUE SPACE.                             IC1124.2
015600     02 FILLER PIC X(17) VALUE "       CORRECT =".                IC1124.2
015700     02 CORRECT-X.                                                IC1124.2
015800     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IC1124.2
015900     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IC1124.2
016000     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IC1124.2
016100     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IC1124.2
016200     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IC1124.2
016300     03      CR-18V0 REDEFINES CORRECT-A.                         IC1124.2
016400         04 CORRECT-18V0                     PIC -9(18).          IC1124.2
016500         04 FILLER                           PIC X.               IC1124.2
016600     03 FILLER PIC X(2) VALUE SPACE.                              IC1124.2
016700     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IC1124.2
016800 01  CCVS-C-1.                                                    IC1124.2
016900     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIC1124.2
017000-    "SS  PARAGRAPH-NAME                                          IC1124.2
017100-    "       REMARKS".                                            IC1124.2
017200     02 FILLER                     PIC X(20)    VALUE SPACE.      IC1124.2
017300 01  CCVS-C-2.                                                    IC1124.2
017400     02 FILLER                     PIC X        VALUE SPACE.      IC1124.2
017500     02 FILLER                     PIC X(6)     VALUE "TESTED".   IC1124.2
017600     02 FILLER                     PIC X(15)    VALUE SPACE.      IC1124.2
017700     02 FILLER                     PIC X(4)     VALUE "FAIL".     IC1124.2
017800     02 FILLER                     PIC X(94)    VALUE SPACE.      IC1124.2
017900 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IC1124.2
018000 01  REC-CT                        PIC 99       VALUE ZERO.       IC1124.2
018100 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IC1124.2
018200 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IC1124.2
018300 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IC1124.2
018400 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IC1124.2
018500 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IC1124.2
018600 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IC1124.2
018700 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IC1124.2
018800 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IC1124.2
018900 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IC1124.2
019000 01  CCVS-H-1.                                                    IC1124.2
019100     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1124.2
019200     02  FILLER                    PIC X(42)    VALUE             IC1124.2
019300     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IC1124.2
019400     02  FILLER                    PIC X(39)    VALUE SPACES.     IC1124.2
019500 01  CCVS-H-2A.                                                   IC1124.2
019600   02  FILLER                        PIC X(40)  VALUE SPACE.      IC1124.2
019700   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IC1124.2
019800   02  FILLER                        PIC XXXX   VALUE             IC1124.2
019900     "4.2 ".                                                      IC1124.2
020000   02  FILLER                        PIC X(28)  VALUE             IC1124.2
020100            " COPY - NOT FOR DISTRIBUTION".                       IC1124.2
020200   02  FILLER                        PIC X(41)  VALUE SPACE.      IC1124.2
020300                                                                  IC1124.2
020400 01  CCVS-H-2B.                                                   IC1124.2
020500   02  FILLER                        PIC X(15)  VALUE             IC1124.2
020600            "TEST RESULT OF ".                                    IC1124.2
020700   02  TEST-ID                       PIC X(9).                    IC1124.2
020800   02  FILLER                        PIC X(4)   VALUE             IC1124.2
020900            " IN ".                                               IC1124.2
021000   02  FILLER                        PIC X(12)  VALUE             IC1124.2
021100     " HIGH       ".                                              IC1124.2
021200   02  FILLER                        PIC X(22)  VALUE             IC1124.2
021300            " LEVEL VALIDATION FOR ".                             IC1124.2
021400   02  FILLER                        PIC X(58)  VALUE             IC1124.2
021500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1124.2
021600 01  CCVS-H-3.                                                    IC1124.2
021700     02  FILLER                      PIC X(34)  VALUE             IC1124.2
021800            " FOR OFFICIAL USE ONLY    ".                         IC1124.2
021900     02  FILLER                      PIC X(58)  VALUE             IC1124.2
022000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IC1124.2
022100     02  FILLER                      PIC X(28)  VALUE             IC1124.2
022200            "  COPYRIGHT   1985 ".                                IC1124.2
022300 01  CCVS-E-1.                                                    IC1124.2
022400     02 FILLER                       PIC X(52)  VALUE SPACE.      IC1124.2
022500     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IC1124.2
022600     02 ID-AGAIN                     PIC X(9).                    IC1124.2
022700     02 FILLER                       PIC X(45)  VALUE SPACES.     IC1124.2
022800 01  CCVS-E-2.                                                    IC1124.2
022900     02  FILLER                      PIC X(31)  VALUE SPACE.      IC1124.2
023000     02  FILLER                      PIC X(21)  VALUE SPACE.      IC1124.2
023100     02 CCVS-E-2-2.                                               IC1124.2
023200         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IC1124.2
023300         03 FILLER                   PIC X      VALUE SPACE.      IC1124.2
023400         03 ENDER-DESC               PIC X(44)  VALUE             IC1124.2
023500            "ERRORS ENCOUNTERED".                                 IC1124.2
023600 01  CCVS-E-3.                                                    IC1124.2
023700     02  FILLER                      PIC X(22)  VALUE             IC1124.2
023800            " FOR OFFICIAL USE ONLY".                             IC1124.2
023900     02  FILLER                      PIC X(12)  VALUE SPACE.      IC1124.2
024000     02  FILLER                      PIC X(58)  VALUE             IC1124.2
024100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IC1124.2
024200     02  FILLER                      PIC X(13)  VALUE SPACE.      IC1124.2
024300     02 FILLER                       PIC X(15)  VALUE             IC1124.2
024400             " COPYRIGHT 1985".                                   IC1124.2
024500 01  CCVS-E-4.                                                    IC1124.2
024600     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IC1124.2
024700     02 FILLER                       PIC X(4)   VALUE " OF ".     IC1124.2
024800     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IC1124.2
024900     02 FILLER                       PIC X(40)  VALUE             IC1124.2
025000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IC1124.2
025100 01  XXINFO.                                                      IC1124.2
025200     02 FILLER                       PIC X(19)  VALUE             IC1124.2
025300            "*** INFORMATION ***".                                IC1124.2
025400     02 INFO-TEXT.                                                IC1124.2
025500       04 FILLER                     PIC X(8)   VALUE SPACE.      IC1124.2
025600       04 XXCOMPUTED                 PIC X(20).                   IC1124.2
025700       04 FILLER                     PIC X(5)   VALUE SPACE.      IC1124.2
025800       04 XXCORRECT                  PIC X(20).                   IC1124.2
025900     02 INF-ANSI-REFERENCE           PIC X(48).                   IC1124.2
026000 01  HYPHEN-LINE.                                                 IC1124.2
026100     02 FILLER  PIC IS X VALUE IS SPACE.                          IC1124.2
026200     02 FILLER  PIC IS X(65)    VALUE IS "************************IC1124.2
026300-    "*****************************************".                 IC1124.2
026400     02 FILLER  PIC IS X(54)    VALUE IS "************************IC1124.2
026500-    "******************************".                            IC1124.2
026600 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IC1124.2
026700     "IC112A".                                                    IC1124.2
026800 PROCEDURE DIVISION.                                              IC1124.2
026900 CCVS1 SECTION.                                                   IC1124.2
027000 OPEN-FILES.                                                      IC1124.2
027100     OPEN     OUTPUT PRINT-FILE.                                  IC1124.2
027200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   IC1124.2
027300     MOVE    SPACE TO TEST-RESULTS.                               IC1124.2
027400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             IC1124.2
027500     GO TO CCVS1-EXIT.                                            IC1124.2
027600 CLOSE-FILES.                                                     IC1124.2
027700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IC1124.2
027800 TERMINATE-CCVS.                                                  IC1124.2
027900*    EXIT PROGRAM.                                                IC1124.2
028000*TERMINATE-CALL.                                                  IC1124.2
028100     STOP     RUN.                                                IC1124.2
028200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IC1124.2
028300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IC1124.2
028400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IC1124.2
028500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IC1124.2
028600     MOVE "****TEST DELETED****" TO RE-MARK.                      IC1124.2
028700 PRINT-DETAIL.                                                    IC1124.2
028800     IF REC-CT NOT EQUAL TO ZERO                                  IC1124.2
028900             MOVE "." TO PARDOT-X                                 IC1124.2
029000             MOVE REC-CT TO DOTVALUE.                             IC1124.2
029100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IC1124.2
029200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IC1124.2
029300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IC1124.2
029400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IC1124.2
029500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IC1124.2
029600     MOVE SPACE TO CORRECT-X.                                     IC1124.2
029700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IC1124.2
029800     MOVE     SPACE TO RE-MARK.                                   IC1124.2
029900 HEAD-ROUTINE.                                                    IC1124.2
030000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1124.2
030100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IC1124.2
030200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1124.2
030300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IC1124.2
030400 COLUMN-NAMES-ROUTINE.                                            IC1124.2
030500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1124.2
030600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1124.2
030700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IC1124.2
030800 END-ROUTINE.                                                     IC1124.2
030900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IC1124.2
031000 END-RTN-EXIT.                                                    IC1124.2
031100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1124.2
031200 END-ROUTINE-1.                                                   IC1124.2
031300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IC1124.2
031400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IC1124.2
031500      ADD PASS-COUNTER TO ERROR-HOLD.                             IC1124.2
031600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IC1124.2
031700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IC1124.2
031800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IC1124.2
031900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IC1124.2
032000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IC1124.2
032100  END-ROUTINE-12.                                                 IC1124.2
032200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IC1124.2
032300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IC1124.2
032400         MOVE "NO " TO ERROR-TOTAL                                IC1124.2
032500         ELSE                                                     IC1124.2
032600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IC1124.2
032700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IC1124.2
032800     PERFORM WRITE-LINE.                                          IC1124.2
032900 END-ROUTINE-13.                                                  IC1124.2
033000     IF DELETE-COUNTER IS EQUAL TO ZERO                           IC1124.2
033100         MOVE "NO " TO ERROR-TOTAL  ELSE                          IC1124.2
033200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IC1124.2
033300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IC1124.2
033400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1124.2
033500      IF   INSPECT-COUNTER EQUAL TO ZERO                          IC1124.2
033600          MOVE "NO " TO ERROR-TOTAL                               IC1124.2
033700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IC1124.2
033800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IC1124.2
033900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IC1124.2
034000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IC1124.2
034100 WRITE-LINE.                                                      IC1124.2
034200     ADD 1 TO RECORD-COUNT.                                       IC1124.2
034300*    IF RECORD-COUNT GREATER 50                                   IC1124.2
034400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IC1124.2
034500*        MOVE SPACE TO DUMMY-RECORD                               IC1124.2
034600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IC1124.2
034700*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             IC1124.2
034800*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     IC1124.2
034900*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IC1124.2
035000*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IC1124.2
035100*        MOVE ZERO TO RECORD-COUNT.                               IC1124.2
035200     PERFORM WRT-LN.                                              IC1124.2
035300 WRT-LN.                                                          IC1124.2
035400     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IC1124.2
035500     MOVE SPACE TO DUMMY-RECORD.                                  IC1124.2
035600 BLANK-LINE-PRINT.                                                IC1124.2
035700     PERFORM WRT-LN.                                              IC1124.2
035800 FAIL-ROUTINE.                                                    IC1124.2
035900     IF   COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE. IC1124.2
036000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IC1124.2
036100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1124.2
036200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IC1124.2
036300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1124.2
036400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1124.2
036500     GO TO  FAIL-ROUTINE-EX.                                      IC1124.2
036600 FAIL-ROUTINE-WRITE.                                              IC1124.2
036700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IC1124.2
036800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IC1124.2
036900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IC1124.2
037000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IC1124.2
037100 FAIL-ROUTINE-EX. EXIT.                                           IC1124.2
037200 BAIL-OUT.                                                        IC1124.2
037300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IC1124.2
037400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IC1124.2
037500 BAIL-OUT-WRITE.                                                  IC1124.2
037600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IC1124.2
037700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IC1124.2
037800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IC1124.2
037900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IC1124.2
038000 BAIL-OUT-EX. EXIT.                                               IC1124.2
038100 CCVS1-EXIT.                                                      IC1124.2
038200     EXIT.                                                        IC1124.2
038300 SECT-IC112-0001 SECTION.                                         IC1124.2
038400 SEQ-INIT-007.                                                    IC1124.2
038500     MOVE FILE-RECORD-INFO-SKELETON                               IC1124.2
038600          TO FILE-RECORD-INFO-P1-120 (1).                         IC1124.2
038700     MOVE "SQ-FS3" TO XFILE-NAME (1).                             IC1124.2
038800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           IC1124.2
038900     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       IC1124.2
039000     MOVE 120 TO XRECORD-LENGTH (1).                              IC1124.2
039100     MOVE "CH" TO CHARS-OR-RECORDS (1).                           IC1124.2
039200     MOVE 120 TO XBLOCK-SIZE (1).                                 IC1124.2
039300     MOVE 000649 TO RECORDS-IN-FILE (1).                          IC1124.2
039400     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         IC1124.2
039500     MOVE "S" TO XLABEL-TYPE (1).                                 IC1124.2
039600     MOVE 000001 TO XRECORD-NUMBER (1).                           IC1124.2
039700     OPEN OUTPUT SQ-FS3.                                          IC1124.2
039800 SEQ-TEST-007.                                                    IC1124.2
039900     MOVE FILE-RECORD-INFO-P1-120 (1) TO SQ-FS3R1-F-G-120.        IC1124.2
040000     WRITE SQ-FS3R1-F-G-120.                                      IC1124.2
040100     IF XRECORD-NUMBER (1) EQUAL TO 649                           IC1124.2
040200        GO TO SEQ-WRITE-007.                                      IC1124.2
040300     ADD 1 TO XRECORD-NUMBER (1).                                 IC1124.2
040400     GO TO SEQ-TEST-007.                                          IC1124.2
040500 SEQ-WRITE-007.                                                   IC1124.2
040600     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        IC1124.2
040700     MOVE "SEQ-TEST-007" TO PAR-NAME.                             IC1124.2
040800     MOVE "FILE CREATED, RECS =" TO COMPUTED-A.                   IC1124.2
040900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     IC1124.2
041000     PERFORM PRINT-DETAIL.                                        IC1124.2
041100     CLOSE SQ-FS3.                                                IC1124.2
041200*        A MASS STORAGE SEQUENTIAL FILE WITH 120 CHARACTER        IC1124.2
041300*    RECORDS HAS BEEN CREATED.  THE FILE CONTAINS 649 RECORDS.    IC1124.2
041400 SEQ-INIT-008.                                                    IC1124.2
041500     MOVE ZERO TO WRK-CS-09V00.                                   IC1124.2
041600*        THIS TEST READS AND CHECKS THE FILE CREATED IN           IC1124.2
041700*    SEQ-TEST-007.                                                IC1124.2
041800     OPEN INPUT SQ-FS3.                                           IC1124.2
041900 SEQ-TEST-008.                                                    IC1124.2
042000     READ SQ-FS3 RECORD                                           IC1124.2
042100         AT END GO TO SEQ-TEST-008-1.                             IC1124.2
042200     MOVE SQ-FS3R1-F-G-120 TO FILE-RECORD-INFO-P1-120 (1).        IC1124.2
042300     ADD 1 TO WRK-CS-09V00.                                       IC1124.2
042400     IF WRK-CS-09V00 GREATER THAN 649                             IC1124.2
042500         MOVE "MORE THAN 649 RECORDS" TO RE-MARK                  IC1124.2
042600         GO TO SEQ-FAIL-008.                                      IC1124.2
042700     IF WRK-CS-09V00 NOT EQUAL TO XRECORD-NUMBER (1)              IC1124.2
042800         ADD 1 TO RECORDS-IN-ERROR                                IC1124.2
042900         GO TO SEQ-TEST-008.                                      IC1124.2
043000     IF XFILE-NAME (1) NOT EQUAL TO "SQ-FS3"                      IC1124.2
043100         ADD 1 TO RECORDS-IN-ERROR                                IC1124.2
043200         GO TO SEQ-TEST-008.                                      IC1124.2
043300     IF XLABEL-TYPE (1) NOT EQUAL TO "S"                          IC1124.2
043400     ADD 1 TO RECORDS-IN-ERROR.                                   IC1124.2
043500     GO TO SEQ-TEST-008.                                          IC1124.2
043600 SEQ-TEST-008-1.                                                  IC1124.2
043700     IF RECORDS-IN-ERROR EQUAL TO ZERO                            IC1124.2
043800          GO TO SEQ-PASS-008.                                     IC1124.2
043900     MOVE "ERRORS IN READING SQ-FS3" TO RE-MARK.                  IC1124.2
044000 SEQ-FAIL-008.                                                    IC1124.2
044100     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IC1124.2
044200     PERFORM FAIL.                                                IC1124.2
044300     GO TO SEQ-WRITE-008.                                         IC1124.2
044400 SEQ-PASS-008.                                                    IC1124.2
044500     PERFORM PASS.                                                IC1124.2
044600     MOVE "FILE VERIFIED RECS =" TO COMPUTED-A.                   IC1124.2
044700     MOVE WRK-CS-09V00 TO CORRECT-18V0.                           IC1124.2
044800 SEQ-WRITE-008.                                                   IC1124.2
044900     MOVE "SEQ-TEST-008" TO PAR-NAME.                             IC1124.2
045000     MOVE "VERIFY FILE SQ-FS3" TO FEATURE.                        IC1124.2
045100     PERFORM PRINT-DETAIL.                                        IC1124.2
045200 SEQ-CLOSE-008.                                                   IC1124.2
045300     CLOSE SQ-FS3.                                                IC1124.2
045400 LINK-INIT-08.                                                    IC1124.2
045500     MOVE ZERO TO WRK-CS-09V00.                                   IC1124.2
045600     MOVE ZERO TO RECORDS-IN-ERROR.                               IC1124.2
045700     OPEN INPUT SQ-FS3.                                           IC1124.2
045800*                                                                 IC1124.2
045900*         LINK-TEST-08 READS THE FILE SQ-FS3 AND CALLS THE SUB-   IC1124.2
046000*    PROGRAM IC113 TO CHECK THE FIELDS IN THE RECORD.  THE FILE   IC1124.2
046100*    DESCRIPTION RECORD IS ONE OF THE OPERANDS IN THE USING       IC1124.2
046200*    PHRASE OF THE CALL STATEMENT.                                IC1124.2
046300*                                                                 IC1124.2
046400     MOVE ZERO TO ERROR-FLAG.                                     IC1124.2
046500 LINK-TEST-08.                                                    IC1124.2
046600     READ SQ-FS3 RECORD                                           IC1124.2
046700          AT END    MOVE "UNEXPECTED EOF" TO COMPUTED-A           IC1124.2
046800                    MOVE 1 TO EOF-FLAG                            IC1124.2
046900                    GO TO LINK-FAIL-08.                           IC1124.2
047000     CALL "IC113A" USING RECORDS-IN-ERROR   SQ-FS3R1-F-G-120      IC1124.2
047100             ERROR-FLAG  WRK-CS-09V00.                            IC1124.2
047200     IF WRK-CS-09V00 LESS THAN 649                                IC1124.2
047300          GO TO LINK-TEST-08.                                     IC1124.2
047400 LINK-TEST-08-01.                                                 IC1124.2
047500     IF ERROR-FLAG EQUAL TO ZERO                                  IC1124.2
047600          GO TO LINK-PASS-08.                                     IC1124.2
047700     MOVE "ERROR IN RECORD(S)" TO COMPUTED-A.                     IC1124.2
047800 LINK-FAIL-08.                                                    IC1124.2
047900     MOVE RECORDS-IN-ERROR TO CORRECT-18V0.                       IC1124.2
048000     MOVE "CORRECT COL. = RECORDS-IN-ERROR" TO RE-MARK.           IC1124.2
048100     PERFORM FAIL.                                                IC1124.2
048200     GO TO LINK-WRITE-08.                                         IC1124.2
048300 LINK-PASS-08.                                                    IC1124.2
048400     PERFORM PASS.                                                IC1124.2
048500 LINK-WRITE-08.                                                   IC1124.2
048600     MOVE "LINK-TEST-08" TO PAR-NAME.                             IC1124.2
048700     MOVE "USING FD 01 RECORD" TO FEATURE.                        IC1124.2
048800     PERFORM PRINT-DETAIL.                                        IC1124.2
048900 LINK-INIT-09.                                                    IC1124.2
049000     MOVE ZERO TO RECORDS-IN-ERROR  ERROR-FLAG.                   IC1124.2
049100*                                                                 IC1124.2
049200*         LINK-TEST-09 READS THE FILE SQ-FS3.  THE AT END PHRASE  IC1124.2
049300*    OF THE READ STATEMENT SHOULD BE EXECUTED.  A CALL TO THE     IC1124.2
049400*    SUBPROGRAM IC113 IS CONTAINED IN THE AT END PHRASE WITH      IC1124.2
049500*    THE FD 01 RECORD AS ONE OF THE USING OPERANDS.               IC1124.2
049600*                                                                 IC1124.2
049700 LINK-TEST-09-01.                                                 IC1124.2
049800     IF EOF-FLAG EQUAL TO 1                                       IC1124.2
049900           CALL "IC113A" USING RECORDS-IN-ERROR   SQ-FS3R1-F-G-120IC1124.2
050000                              ERROR-FLAG  WRK-CS-09V00            IC1124.2
050100           GO TO LINK-TEST-09-02.                                 IC1124.2
050200 LINK-TEST-09.                                                    IC1124.2
050300     READ SQ-FS3                                                  IC1124.2
050400          AT END CALL "IC113A" USING RECORDS-IN-ERROR             IC1124.2
050500                      SQ-FS3R1-F-G-120  ERROR-FLAG WRK-CS-09V00   IC1124.2
050600                 GO TO LINK-TEST-09-02.                           IC1124.2
050700     MOVE "MORE THAN 649 RECORDS" TO RE-MARK.                     IC1124.2
050800     GO TO LINK-FAIL-09.                                          IC1124.2
050900 LINK-TEST-09-02.                                                 IC1124.2
051000     IF ERROR-FLAG EQUAL TO 1                                     IC1124.2
051100          GO TO LINK-PASS-09.                                     IC1124.2
051200     MOVE "ERROR FLAG NOT SET IN SUBPRGRM" TO RE-MARK.            IC1124.2
051300 LINK-FAIL-09.                                                    IC1124.2
051400     PERFORM FAIL.                                                IC1124.2
051500     GO TO LINK-WRITE-09.                                         IC1124.2
051600 LINK-PASS-09.                                                    IC1124.2
051700     PERFORM PASS.                                                IC1124.2
051800 LINK-WRITE-09.                                                   IC1124.2
051900     MOVE "LINK-TEST-09" TO PAR-NAME.                             IC1124.2
052000     MOVE "CALL AFTER AT END" TO FEATURE.                         IC1124.2
052100     PERFORM PRINT-DETAIL.                                        IC1124.2
052200     CLOSE   SQ-FS3.                                              IC1124.2
052300 EXIT-IC112.                                                      IC1124.2
052400     EXIT.                                                        IC1124.2
052500*FILE-DUMP SECTION.                                               IC1124.2
052600*FILE-3-DUMP-INIT.                                                IC1124.2
052700*    OPEN INPUT SQ-FS3.                                           IC1124.2
052800*    MOVE ZERO TO WRK-CS-09V00.                                   IC1124.2
052900*FILE-3-DUMP.                                                     IC1124.2
053000*    ADD 1 TO WRK-CS-09V00.                                       IC1124.2
053100*    IF WRK-CS-09V00 GREATER THAN 649                             IC1124.2
053200*         GO TO FILE-3-DUMP-EXTRA.                                IC1124.2
053300*    READ SQ-FS3 RECORD AT END                                    IC1124.2
053400*         GO TO FILE-3-DUMP-END.                                  IC1124.2
053500*    PERFORM FILE-3-DUMP-WRITE.                                   IC1124.2
053600*    GO TO FILE-3-DUMP.                                           IC1124.2
053700*FILE-3-DUMP-WRITE.                                               IC1124.2
053800*    MOVE SQ-FS3R1-F-G-120 TO DUMMY-RECORD.                       IC1124.2
053900*    PERFORM WRITE-LINE.                                          IC1124.2
054000*FILE-3-DUMP-EXTRA.                                               IC1124.2
054100*    PERFORM BLANK-LINE-PRINT 5 TIMES.                            IC1124.2
054200*FILE-3-DUMP-MORE.                                                IC1124.2
054300*    READ SQ-FS3 RECORD AT END                                    IC1124.2
054400*         GO TO FILE-3-DUMP-END.                                  IC1124.2
054500*    PERFORM FILE-3-DUMP-WRITE.                                   IC1124.2
054600*    ADD 1 TO WRK-CS-09V00.                                       IC1124.2
054700*    IF WRK-CS-09V00 LESS THAN 669                                IC1124.2
054800*         GO TO FILE-3-DUMP-MORE.                                 IC1124.2
054900*FILE-3-DUMP-END.                                                 IC1124.2
055000*    CLOSE SQ-FS3.                                                IC1124.2
055100*FILE-3-DUMP-EXIT.                                                IC1124.2
055200*    EXIT.                                                        IC1124.2
055300 CCVS-EXIT SECTION.                                               IC1124.2
055400 CCVS-999999.                                                     IC1124.2
055500     GO TO CLOSE-FILES.                                           IC1124.2
      *END-OF,IC112A                                                                  
