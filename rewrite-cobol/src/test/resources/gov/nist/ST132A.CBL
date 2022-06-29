      *HEADER,COBOL,ST132A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1324.2
000200 PROGRAM-ID.                                                      ST1324.2
000300     ST132A.                                                      ST1324.2
000400****************************************************************  ST1324.2
000500*                                                              *  ST1324.2
000600*    VALIDATION FOR:-                                          *  ST1324.2
000700*                                                              *  ST1324.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1324.2
000900*                                                              *  ST1324.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1324.2
001100*                                                              *  ST1324.2
001200****************************************************************  ST1324.2
001300*                                                              *  ST1324.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1324.2
001500*                                                              *  ST1324.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1324.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1324.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1324.2
001900*                                                              *  ST1324.2
002000****************************************************************  ST1324.2
002100*                                                                 ST1324.2
002200*    THIS PROGRAM EXERCISES THE FOLLOWING                         ST1324.2
002300*        SAME SORT AREA CLAUSE                                    ST1324.2
002400*        USING-OUTPUT PROCEDURE COMBINATION                       ST1324.2
002500*        MULTI REEL SORT                                          ST1324.2
002600*        RELEASE FROM                                             ST1324.2
002700*        RETURN INTO.                                             ST1324.2
002800*    THERE ARE 3 SORTS. THE FIRST GENERATES THE INPUT DATA IN THE ST1324.2
002900*        INPUT PROCEDURE. THE SORT RESULTS ARE TESTED IN THE      ST1324.2
003000*        OUTPUT PROCEDURE WHICH ALSO CREATES A 2-REEL FILE (VIA   ST1324.2
003100*        CLOSE REEL) FOR INPUT TO THE SECOND SORT.                ST1324.2
003200*    THE SECOND SORT (USING-OUTPUT PROCEDURE) IS TESTED IN THE    ST1324.2
003300*        OUTPUT PROCEDURE.                                        ST1324.2
003400*    THE THIRD SORT EXERCISES A SORT-FILE FOR THE SECOND TIME.    ST1324.2
003500*        SUCCESSFUL EXECUTION IS THE SOLE TEST OF THIS SORT.      ST1324.2
003600                                                                  ST1324.2
003700 ENVIRONMENT DIVISION.                                            ST1324.2
003800 CONFIGURATION SECTION.                                           ST1324.2
003900 SOURCE-COMPUTER.                                                 ST1324.2
004000     XXXXX082.                                                    ST1324.2
004100 OBJECT-COMPUTER.                                                 ST1324.2
004200     XXXXX083.                                                    ST1324.2
004300 INPUT-OUTPUT SECTION.                                            ST1324.2
004400 FILE-CONTROL.                                                    ST1324.2
004500     SELECT PRINT-FILE ASSIGN TO                                  ST1324.2
004600     XXXXX055.                                                    ST1324.2
004700     SELECT SORT4  ASSIGN TO                                      ST1324.2
004800     XXXXX027.                                                    ST1324.2
004900     SELECT SORT5  ASSIGN TO                                      ST1324.2
005000     XXXXX028.                                                    ST1324.2
005100     SELECT FILE4 ASSIGN TO                                       ST1324.2
005200     XXXXX006.                                                    ST1324.2
005300 I-O-CONTROL.                                                     ST1324.2
005400     SAME SORT AREA FOR SORT5 SORT4.                              ST1324.2
005500 DATA DIVISION.                                                   ST1324.2
005600 FILE SECTION.                                                    ST1324.2
005700 FD  PRINT-FILE.                                                  ST1324.2
005800 01  PRINT-REC PICTURE X(120).                                    ST1324.2
005900 01  DUMMY-RECORD PICTURE X(120).                                 ST1324.2
006000 FD  FILE4                                                        ST1324.2
006100     BLOCK CONTAINS 10 RECORDS                                    ST1324.2
006200     LABEL RECORDS ARE STANDARD                                   ST1324.2
006300C    VALUE OF                                                     ST1324.2
006400C    XXXXX074                                                     ST1324.2
006500C    IS                                                           ST1324.2
006600C    XXXXX075                                                     ST1324.2
006700*    XXXXX069                                                     ST1324.2
006800     DATA RECORD IS R4.                                           ST1324.2
006900 01  R4 PICTURE X(120).                                           ST1324.2
007000 SD  SORT4                                                        ST1324.2
007100     RECORD CONTAINS 120                                          ST1324.2
007200     DATA RECORD IS S4.                                           ST1324.2
007300 01  S4.                                                          ST1324.2
007400     02  S4-KEYS.                                                 ST1324.2
007500         03  S4-KEY1 PICTURE A(10).                               ST1324.2
007600         03  S4-KEY2 PICTURE 9(10).                               ST1324.2
007700     02  FILLER PICTURE X(100).                                   ST1324.2
007800 SD  SORT5                                                        ST1324.2
007900     RECORD 120                                                   ST1324.2
008000     DATA RECORD S5.                                              ST1324.2
008100 01  S5.                                                          ST1324.2
008200     02  S5-KEYS.                                                 ST1324.2
008300         03  S5-KEY1 PICTURE A(10).                               ST1324.2
008400         03  S5-KEY2 PICTURE 9(10).                               ST1324.2
008500     02  FILLER PICTURE X(100).                                   ST1324.2
008600 WORKING-STORAGE SECTION.                                         ST1324.2
008700 77  C0 PICTURE 9 COMPUTATIONAL VALUE ZERO.                       ST1324.2
008800 77  C1 PICTURE 9 COMPUTATIONAL VALUE 1.                          ST1324.2
008900 77  SUBSCRIPT-1 PICTURE 99 COMPUTATIONAL VALUE ZERO.             ST1324.2
009000 77  SUBSCRIPT-2 PICTURE 99 COMPUTATIONAL.                        ST1324.2
009100 01  ALPHA-TABLE.                                                 ST1324.2
009200     02  ALPHA-TAB PICTURE A(25)                                  ST1324.2
009300         VALUE "ABCDEFGHIJKLMNPQRSTUVWXYZ".                       ST1324.2
009400     02  ALPHA-TBL REDEFINES ALPHA-TAB PICTURE A OCCURS 25 TIMES. ST1324.2
009500 01  WKEYS.                                                       ST1324.2
009600     02  WKEY-1.                                                  ST1324.2
009700         03  FILLER PICTURE AAA VALUE "PQR".                      ST1324.2
009800         03  WKEY-1A PICTURE A.                                   ST1324.2
009900         03  FILLER PICTURE A(5) VALUE "ABCDE".                   ST1324.2
010000         03  WKEY-1B PICTURE A.                                   ST1324.2
010100     02  WKEY-2.                                                  ST1324.2
010200         03  FILLER PICTURE 9    VALUE 7.                         ST1324.2
010300         03  WKEY-2A PICTURE 9 VALUE ZERO.                        ST1324.2
010400         03  FILLER PICTURE 9(7) VALUE 1234567.                   ST1324.2
010500         03  WKEY-2B PICTURE 9 VALUE ZERO.                        ST1324.2
010600     02  FILLER PICTURE X(100).                                   ST1324.2
010700 01  FILE-RECORD-INFORMATION-REC.                                 ST1324.2
010800     03 FILE-RECORD-INFO-SKELETON.                                ST1324.2
010900        05 FILLER                 PICTURE X(48)       VALUE       ST1324.2
011000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1324.2
011100        05 FILLER                 PICTURE X(46)       VALUE       ST1324.2
011200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1324.2
011300        05 FILLER                 PICTURE X(26)       VALUE       ST1324.2
011400             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1324.2
011500        05 FILLER                 PICTURE X(37)       VALUE       ST1324.2
011600             ",RECKEY=                             ".             ST1324.2
011700        05 FILLER                 PICTURE X(38)       VALUE       ST1324.2
011800             ",ALTKEY1=                             ".            ST1324.2
011900        05 FILLER                 PICTURE X(38)       VALUE       ST1324.2
012000             ",ALTKEY2=                             ".            ST1324.2
012100        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1324.2
012200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1324.2
012300        05 FILE-RECORD-INFO-P1-120.                               ST1324.2
012400           07 FILLER              PIC X(5).                       ST1324.2
012500           07 XFILE-NAME           PIC X(6).                      ST1324.2
012600           07 FILLER              PIC X(8).                       ST1324.2
012700           07 XRECORD-NAME         PIC X(6).                      ST1324.2
012800           07 FILLER              PIC X(1).                       ST1324.2
012900           07 REELUNIT-NUMBER     PIC 9(1).                       ST1324.2
013000           07 FILLER              PIC X(7).                       ST1324.2
013100           07 XRECORD-NUMBER       PIC 9(6).                      ST1324.2
013200           07 FILLER              PIC X(6).                       ST1324.2
013300           07 UPDATE-NUMBER       PIC 9(2).                       ST1324.2
013400           07 FILLER              PIC X(5).                       ST1324.2
013500           07 ODO-NUMBER          PIC 9(4).                       ST1324.2
013600           07 FILLER              PIC X(5).                       ST1324.2
013700           07 XPROGRAM-NAME        PIC X(5).                      ST1324.2
013800           07 FILLER              PIC X(7).                       ST1324.2
013900           07 XRECORD-LENGTH       PIC 9(6).                      ST1324.2
014000           07 FILLER              PIC X(7).                       ST1324.2
014100           07 CHARS-OR-RECORDS    PIC X(2).                       ST1324.2
014200           07 FILLER              PIC X(1).                       ST1324.2
014300           07 XBLOCK-SIZE          PIC 9(4).                      ST1324.2
014400           07 FILLER              PIC X(6).                       ST1324.2
014500           07 RECORDS-IN-FILE     PIC 9(6).                       ST1324.2
014600           07 FILLER              PIC X(5).                       ST1324.2
014700           07 XFILE-ORGANIZATION   PIC X(2).                      ST1324.2
014800           07 FILLER              PIC X(6).                       ST1324.2
014900           07 XLABEL-TYPE          PIC X(1).                      ST1324.2
015000        05 FILE-RECORD-INFO-P121-240.                             ST1324.2
015100           07 FILLER              PIC X(8).                       ST1324.2
015200           07 XRECORD-KEY          PIC X(29).                     ST1324.2
015300           07 FILLER              PIC X(9).                       ST1324.2
015400           07 ALTERNATE-KEY1      PIC X(29).                      ST1324.2
015500           07 FILLER              PIC X(9).                       ST1324.2
015600           07 ALTERNATE-KEY2      PIC X(29).                      ST1324.2
015700           07 FILLER              PIC X(7).                       ST1324.2
015800 01  TEST-RESULTS.                                                ST1324.2
015900     02 FILLER                   PIC X      VALUE SPACE.          ST1324.2
016000     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1324.2
016100     02 FILLER                   PIC X      VALUE SPACE.          ST1324.2
016200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1324.2
016300     02 FILLER                   PIC X      VALUE SPACE.          ST1324.2
016400     02  PAR-NAME.                                                ST1324.2
016500       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1324.2
016600       03  PARDOT-X              PIC X      VALUE SPACE.          ST1324.2
016700       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1324.2
016800     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1324.2
016900     02 RE-MARK                  PIC X(61).                       ST1324.2
017000 01  TEST-COMPUTED.                                               ST1324.2
017100     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1324.2
017200     02 FILLER                   PIC X(17)  VALUE                 ST1324.2
017300            "       COMPUTED=".                                   ST1324.2
017400     02 COMPUTED-X.                                               ST1324.2
017500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1324.2
017600     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1324.2
017700                                 PIC -9(9).9(9).                  ST1324.2
017800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1324.2
017900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1324.2
018000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1324.2
018100     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1324.2
018200         04 COMPUTED-18V0                    PIC -9(18).          ST1324.2
018300         04 FILLER                           PIC X.               ST1324.2
018400     03 FILLER PIC X(50) VALUE SPACE.                             ST1324.2
018500 01  TEST-CORRECT.                                                ST1324.2
018600     02 FILLER PIC X(30) VALUE SPACE.                             ST1324.2
018700     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1324.2
018800     02 CORRECT-X.                                                ST1324.2
018900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1324.2
019000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1324.2
019100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1324.2
019200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1324.2
019300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1324.2
019400     03      CR-18V0 REDEFINES CORRECT-A.                         ST1324.2
019500         04 CORRECT-18V0                     PIC -9(18).          ST1324.2
019600         04 FILLER                           PIC X.               ST1324.2
019700     03 FILLER PIC X(2) VALUE SPACE.                              ST1324.2
019800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1324.2
019900 01  CCVS-C-1.                                                    ST1324.2
020000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1324.2
020100-    "SS  PARAGRAPH-NAME                                          ST1324.2
020200-    "       REMARKS".                                            ST1324.2
020300     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1324.2
020400 01  CCVS-C-2.                                                    ST1324.2
020500     02 FILLER                     PIC X        VALUE SPACE.      ST1324.2
020600     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1324.2
020700     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1324.2
020800     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1324.2
020900     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1324.2
021000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1324.2
021100 01  REC-CT                        PIC 99       VALUE ZERO.       ST1324.2
021200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1324.2
021300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1324.2
021400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1324.2
021500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1324.2
021600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1324.2
021700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1324.2
021800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1324.2
021900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1324.2
022000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1324.2
022100 01  CCVS-H-1.                                                    ST1324.2
022200     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1324.2
022300     02  FILLER                    PIC X(42)    VALUE             ST1324.2
022400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1324.2
022500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1324.2
022600 01  CCVS-H-2A.                                                   ST1324.2
022700   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1324.2
022800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1324.2
022900   02  FILLER                        PIC XXXX   VALUE             ST1324.2
023000     "4.2 ".                                                      ST1324.2
023100   02  FILLER                        PIC X(28)  VALUE             ST1324.2
023200            " COPY - NOT FOR DISTRIBUTION".                       ST1324.2
023300   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1324.2
023400                                                                  ST1324.2
023500 01  CCVS-H-2B.                                                   ST1324.2
023600   02  FILLER                        PIC X(15)  VALUE             ST1324.2
023700            "TEST RESULT OF ".                                    ST1324.2
023800   02  TEST-ID                       PIC X(9).                    ST1324.2
023900   02  FILLER                        PIC X(4)   VALUE             ST1324.2
024000            " IN ".                                               ST1324.2
024100   02  FILLER                        PIC X(12)  VALUE             ST1324.2
024200     " HIGH       ".                                              ST1324.2
024300   02  FILLER                        PIC X(22)  VALUE             ST1324.2
024400            " LEVEL VALIDATION FOR ".                             ST1324.2
024500   02  FILLER                        PIC X(58)  VALUE             ST1324.2
024600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1324.2
024700 01  CCVS-H-3.                                                    ST1324.2
024800     02  FILLER                      PIC X(34)  VALUE             ST1324.2
024900            " FOR OFFICIAL USE ONLY    ".                         ST1324.2
025000     02  FILLER                      PIC X(58)  VALUE             ST1324.2
025100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1324.2
025200     02  FILLER                      PIC X(28)  VALUE             ST1324.2
025300            "  COPYRIGHT   1985 ".                                ST1324.2
025400 01  CCVS-E-1.                                                    ST1324.2
025500     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1324.2
025600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1324.2
025700     02 ID-AGAIN                     PIC X(9).                    ST1324.2
025800     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1324.2
025900 01  CCVS-E-2.                                                    ST1324.2
026000     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1324.2
026100     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1324.2
026200     02 CCVS-E-2-2.                                               ST1324.2
026300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1324.2
026400         03 FILLER                   PIC X      VALUE SPACE.      ST1324.2
026500         03 ENDER-DESC               PIC X(44)  VALUE             ST1324.2
026600            "ERRORS ENCOUNTERED".                                 ST1324.2
026700 01  CCVS-E-3.                                                    ST1324.2
026800     02  FILLER                      PIC X(22)  VALUE             ST1324.2
026900            " FOR OFFICIAL USE ONLY".                             ST1324.2
027000     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1324.2
027100     02  FILLER                      PIC X(58)  VALUE             ST1324.2
027200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1324.2
027300     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1324.2
027400     02 FILLER                       PIC X(15)  VALUE             ST1324.2
027500             " COPYRIGHT 1985".                                   ST1324.2
027600 01  CCVS-E-4.                                                    ST1324.2
027700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1324.2
027800     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1324.2
027900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1324.2
028000     02 FILLER                       PIC X(40)  VALUE             ST1324.2
028100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1324.2
028200 01  XXINFO.                                                      ST1324.2
028300     02 FILLER                       PIC X(19)  VALUE             ST1324.2
028400            "*** INFORMATION ***".                                ST1324.2
028500     02 INFO-TEXT.                                                ST1324.2
028600       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1324.2
028700       04 XXCOMPUTED                 PIC X(20).                   ST1324.2
028800       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1324.2
028900       04 XXCORRECT                  PIC X(20).                   ST1324.2
029000     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1324.2
029100 01  HYPHEN-LINE.                                                 ST1324.2
029200     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1324.2
029300     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1324.2
029400-    "*****************************************".                 ST1324.2
029500     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1324.2
029600-    "******************************".                            ST1324.2
029700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1324.2
029800     "ST132A".                                                    ST1324.2
029900 PROCEDURE DIVISION.                                              ST1324.2
030000 CCVS1 SECTION.                                                   ST1324.2
030100 OPEN-FILES.                                                      ST1324.2
030200     OPEN    OUTPUT PRINT-FILE.                                   ST1324.2
030300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1324.2
030400     MOVE    SPACE TO TEST-RESULTS.                               ST1324.2
030500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1324.2
030600     MOVE    ZERO TO REC-SKL-SUB.                                 ST1324.2
030700     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1324.2
030800 CCVS-INIT-FILE.                                                  ST1324.2
030900     ADD     1 TO REC-SKL-SUB.                                    ST1324.2
031000     MOVE    FILE-RECORD-INFO-SKELETON                            ST1324.2
031100          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1324.2
031200 CCVS-INIT-EXIT.                                                  ST1324.2
031300     GO TO CCVS1-EXIT.                                            ST1324.2
031400 CLOSE-FILES.                                                     ST1324.2
031500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1324.2
031600 TERMINATE-CCVS.                                                  ST1324.2
031700*    EXIT PROGRAM.                                                ST1324.2
031800*TERMINATE-CALL.                                                  ST1324.2
031900     STOP     RUN.                                                ST1324.2
032000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1324.2
032100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1324.2
032200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1324.2
032300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1324.2
032400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1324.2
032500 PRINT-DETAIL.                                                    ST1324.2
032600     IF REC-CT NOT EQUAL TO ZERO                                  ST1324.2
032700             MOVE "." TO PARDOT-X                                 ST1324.2
032800             MOVE REC-CT TO DOTVALUE.                             ST1324.2
032900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1324.2
033000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1324.2
033100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1324.2
033200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1324.2
033300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1324.2
033400     MOVE SPACE TO CORRECT-X.                                     ST1324.2
033500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1324.2
033600     MOVE     SPACE TO RE-MARK.                                   ST1324.2
033700 HEAD-ROUTINE.                                                    ST1324.2
033800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1324.2
033900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1324.2
034000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1324.2
034100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1324.2
034200 COLUMN-NAMES-ROUTINE.                                            ST1324.2
034300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1324.2
034400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1324.2
034500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1324.2
034600 END-ROUTINE.                                                     ST1324.2
034700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1324.2
034800 END-RTN-EXIT.                                                    ST1324.2
034900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1324.2
035000 END-ROUTINE-1.                                                   ST1324.2
035100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1324.2
035200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1324.2
035300      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1324.2
035400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1324.2
035500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1324.2
035600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1324.2
035700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1324.2
035800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1324.2
035900  END-ROUTINE-12.                                                 ST1324.2
036000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1324.2
036100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1324.2
036200         MOVE "NO " TO ERROR-TOTAL                                ST1324.2
036300         ELSE                                                     ST1324.2
036400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1324.2
036500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1324.2
036600     PERFORM WRITE-LINE.                                          ST1324.2
036700 END-ROUTINE-13.                                                  ST1324.2
036800     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1324.2
036900         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1324.2
037000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1324.2
037100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1324.2
037200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1324.2
037300      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1324.2
037400          MOVE "NO " TO ERROR-TOTAL                               ST1324.2
037500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1324.2
037600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1324.2
037700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1324.2
037800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1324.2
037900 WRITE-LINE.                                                      ST1324.2
038000     ADD 1 TO RECORD-COUNT.                                       ST1324.2
038100*    IF RECORD-COUNT GREATER 42                                   ST1324.2
038200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1324.2
038300*        MOVE SPACE TO DUMMY-RECORD                               ST1324.2
038400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1324.2
038500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1324.2
038600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1324.2
038700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1324.2
038800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1324.2
038900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1324.2
039000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1324.2
039100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1324.2
039200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1324.2
039300*        MOVE ZERO TO RECORD-COUNT.                               ST1324.2
039400     PERFORM WRT-LN.                                              ST1324.2
039500 WRT-LN.                                                          ST1324.2
039600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1324.2
039700     MOVE SPACE TO DUMMY-RECORD.                                  ST1324.2
039800 BLANK-LINE-PRINT.                                                ST1324.2
039900     PERFORM WRT-LN.                                              ST1324.2
040000 FAIL-ROUTINE.                                                    ST1324.2
040100     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1324.2
040200            GO TO   FAIL-ROUTINE-WRITE.                           ST1324.2
040300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1324.2
040400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1324.2
040500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1324.2
040600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1324.2
040700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1324.2
040800     GO TO  FAIL-ROUTINE-EX.                                      ST1324.2
040900 FAIL-ROUTINE-WRITE.                                              ST1324.2
041000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1324.2
041100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1324.2
041200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1324.2
041300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1324.2
041400 FAIL-ROUTINE-EX. EXIT.                                           ST1324.2
041500 BAIL-OUT.                                                        ST1324.2
041600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1324.2
041700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1324.2
041800 BAIL-OUT-WRITE.                                                  ST1324.2
041900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1324.2
042000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1324.2
042100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1324.2
042200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1324.2
042300 BAIL-OUT-EX. EXIT.                                               ST1324.2
042400 CCVS1-EXIT.                                                      ST1324.2
042500     EXIT.                                                        ST1324.2
042600 SRT-4 SECTION.                                                   ST1324.2
042700 FOURTH-SORT.                                                     ST1324.2
042800     SORT SORT4                                                   ST1324.2
042900         DESCENDING S4-KEY1 S4-KEY2                               ST1324.2
043000             INPUT PROCEDURE SRT-4-INPUT                          ST1324.2
043100             OUTPUT PROCEDURE SRT-4-OUTPUT.                       ST1324.2
043200     GO TO SRT-5.                                                 ST1324.2
043300 SRT-4-INPUT SECTION.                                             ST1324.2
043400 P1-CREATE-S4.                                                    ST1324.2
043500     ADD C1 TO SUBSCRIPT-1.                                       ST1324.2
043600     PERFORM P3-CREATE-S4 2 TIMES.                                ST1324.2
043700 P2-CREATE-S4.                                                    ST1324.2
043800     PERFORM P1-CREATE-S4.                                        ST1324.2
043900     GO TO SRT-4-IN-EXIT.                                         ST1324.2
044000 P3-CREATE-S4.                                                    ST1324.2
044100     MOVE ALPHA-TBL (SUBSCRIPT-1) TO WKEY-1A.                     ST1324.2
044200     MOVE C0 TO SUBSCRIPT-2.                                      ST1324.2
044300     PERFORM P4-CREATE-S4 25 TIMES.                               ST1324.2
044400 P4-CREATE-S4.                                                    ST1324.2
044500     ADD C1 TO SUBSCRIPT-2.                                       ST1324.2
044600     MOVE ALPHA-TBL (SUBSCRIPT-2) TO WKEY-1B.                     ST1324.2
044700     MOVE WKEYS TO S4.                                            ST1324.2
044800     RELEASE S4.                                                  ST1324.2
044900     IF WKEY-2B IS EQUAL TO 9                                     ST1324.2
045000         IF WKEY-2A IS EQUAL TO 9                                 ST1324.2
045100             MOVE 0 TO WKEY-2A                                    ST1324.2
045200         ELSE                                                     ST1324.2
045300             ADD C1 TO WKEY-2A                                    ST1324.2
045400         END-IF                                                   ST1324.2
045500         MOVE C0 TO WKEY-2B                                       ST1324.2
045600     ELSE ADD C1 TO WKEY-2B.                                      ST1324.2
045700 SRT-4-IN-EXIT.                                                   ST1324.2
045800     EXIT.                                                        ST1324.2
045900 SRT-4-OUTPUT SECTION.                                            ST1324.2
046000 OPEN-SRT4-OUT.                                                   ST1324.2
046100     OPEN     OUTPUT FILE4.                                       ST1324.2
046200     MOVE     "SORT, OUTPUT PROC" TO FEATURE.                     ST1324.2
046300 SORT-TEST-16.                                                    ST1324.2
046400     PERFORM  RETURN-SORT4.                                       ST1324.2
046500     IF       S4-KEYS EQUAL TO "PQRBABCDEZ7912345679"             ST1324.2
046600              PERFORM PASS-1 GO TO SORT-WRITE-16.                 ST1324.2
046700     GO       TO SORT-FAIL-16.                                    ST1324.2
046800 SORT-DELETE-16.                                                  ST1324.2
046900     PERFORM  DE-LETE-1.                                          ST1324.2
047000     GO       TO SORT-WRITE-16.                                   ST1324.2
047100 SORT-FAIL-16.                                                    ST1324.2
047200     MOVE     S4-KEYS TO COMPUTED-A.                              ST1324.2
047300     MOVE     "PQRBABCDEZ7912345679" TO CORRECT-A.                ST1324.2
047400     PERFORM  FAIL-1.                                             ST1324.2
047500 SORT-WRITE-16.                                                   ST1324.2
047600     MOVE     "SORT-TEST-16" TO PAR-NAME.                         ST1324.2
047700     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
047800 SORT-TEST-17.                                                    ST1324.2
047900     PERFORM  RETURN-SORT4 59 TIMES.                              ST1324.2
048000     IF       S4-KEYS EQUAL TO "PQRAABCDEV7212345670"             ST1324.2
048100              PERFORM PASS-1 GO TO SORT-WRITE-17.                 ST1324.2
048200     GO       TO SORT-FAIL-17.                                    ST1324.2
048300 SORT-DELETE-17.                                                  ST1324.2
048400     PERFORM  DE-LETE-1.                                          ST1324.2
048500     GO       TO SORT-WRITE-17.                                   ST1324.2
048600 SORT-FAIL-17.                                                    ST1324.2
048700     MOVE     S4-KEYS TO COMPUTED-A.                              ST1324.2
048800     MOVE     "PQRAABCDEV7212345670" TO CORRECT-A.                ST1324.2
048900     PERFORM  FAIL-1.                                             ST1324.2
049000 SORT-WRITE-17.                                                   ST1324.2
049100     MOVE     "SORT-TEST-17" TO PAR-NAME.                         ST1324.2
049200     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
049300 SORT-TEST-18.                                                    ST1324.2
049400*    CLOSE    FILE4 REEL.                                         ST1324.2
049500                                                                  ST1324.2
049600*    MOVE     "MINOR *CLOSE REEL* DELETED" TO RE-MARK.            ST1324.2
049700*        NOTE CLOSE REEL DELETED FOR THIS RUN    XXXXX     XXXXX. ST1324.2
049800     PERFORM  RETURN-SORT4 40 TIMES.                              ST1324.2
049900     IF       S4-KEYS EQUAL TO "PQRAABCDEA7012345670"             ST1324.2
050000              PERFORM PASS-1 GO TO SORT-WRITE-18.                 ST1324.2
050100     GO       TO SORT-FAIL-18.                                    ST1324.2
050200 SORT-DELETE-18.                                                  ST1324.2
050300     PERFORM  DE-LETE-1.                                          ST1324.2
050400     GO       TO SORT-WRITE-18.                                   ST1324.2
050500 SORT-FAIL-18.                                                    ST1324.2
050600     MOVE     S4-KEYS TO COMPUTED-A.                              ST1324.2
050700     MOVE     "PQRAABCDEA7012345670" TO CORRECT-A.                ST1324.2
050800     PERFORM  FAIL-1.                                             ST1324.2
050900 SORT-WRITE-18.                                                   ST1324.2
051000     MOVE     "SORT-TEST-18" TO PAR-NAME.                         ST1324.2
051100     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
051200 CLOSE-1.                                                         ST1324.2
051300     CLOSE    FILE4.                                              ST1324.2
051400     GO       TO EXIT-1.                                          ST1324.2
051500 RETURN-SORT4.                                                    ST1324.2
051600     RETURN   SORT4 RECORD INTO R4 AT END GO TO TERMINAL-1.       ST1324.2
051700*    NOTE     RETURN WITH ALL OPTIONAL WORDS.                     ST1324.2
051800     WRITE    R4.                                                 ST1324.2
051900 TERMINAL-1.                                                      ST1324.2
052000     PERFORM  FAIL-1.                                             ST1324.2
052100     MOVE     "TERMINAL-1" TO PAR-NAME.                           ST1324.2
052200     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1324.2
052300     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
052400     MOVE     SPACE TO FEATURE.                                   ST1324.2
052500     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1324.2
052600     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
052700     MOVE     "LAST SUCCESSFUL TEST" TO RE-MARK.                  ST1324.2
052800     PERFORM  PRINT-DETAIL-1.                                     ST1324.2
052900     GO       TO CLOSE-1.                                         ST1324.2
053000 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1324.2
053100 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1324.2
053200 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1324.2
053300 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1324.2
053400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1324.2
053500 PRINT-DETAIL-1.                                                  ST1324.2
053600     IF REC-CT NOT EQUAL TO ZERO                                  ST1324.2
053700             MOVE "." TO PARDOT-X                                 ST1324.2
053800             MOVE REC-CT TO DOTVALUE.                             ST1324.2
053900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    ST1324.2
054000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             ST1324.2
054100        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             ST1324.2
054200          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             ST1324.2
054300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1324.2
054400     MOVE SPACE TO CORRECT-X.                                     ST1324.2
054500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1324.2
054600     MOVE     SPACE TO RE-MARK.                                   ST1324.2
054700 WRITE-LINE-1.                                                    ST1324.2
054800     ADD 1 TO RECORD-COUNT.                                       ST1324.2
054900*    IF RECORD-COUNT GREATER 50                                   ST1324.2
055000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1324.2
055100*        MOVE SPACE TO DUMMY-RECORD                               ST1324.2
055200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1324.2
055300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           ST1324.2
055400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   ST1324.2
055500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        ST1324.2
055600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1324.2
055700*        MOVE ZERO TO RECORD-COUNT.                               ST1324.2
055800     PERFORM WRT-LN-1.                                            ST1324.2
055900 WRT-LN-1.                                                        ST1324.2
056000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1324.2
056100     MOVE SPACE TO DUMMY-RECORD.                                  ST1324.2
056200 BLANK-LINE-PRINT-1.                                              ST1324.2
056300     PERFORM WRT-LN-1.                                            ST1324.2
056400 FAIL-ROUTINE-1.                                                  ST1324.2
056500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     ST1324.2
056600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      ST1324.2
056700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1324.2
056800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1324.2
056900     GO TO FAIL-ROUTINE-EX-1.                                     ST1324.2
057000 FAIL-RTN-WRITE-1.                                                ST1324.2
057100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         ST1324.2
057200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. ST1324.2
057300 FAIL-ROUTINE-EX-1. EXIT.                                         ST1324.2
057400 BAIL-OUT-1.                                                      ST1324.2
057500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     ST1324.2
057600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             ST1324.2
057700 BAIL-OUT-WRITE-1.                                                ST1324.2
057800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1324.2
057900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1324.2
058000 BAIL-OUT-EX-1. EXIT.                                             ST1324.2
058100 EXIT-1.                                                          ST1324.2
058200     EXIT.                                                        ST1324.2
058300 SRT-5 SECTION.                                                   ST1324.2
058400 FIFTH-SORT.                                                      ST1324.2
058500     SORT SORT5                                                   ST1324.2
058600         ASCENDING S5-KEY1 S5-KEY2                                ST1324.2
058700             USING FILE4                                          ST1324.2
058800             OUTPUT PROCEDURE SRT-5-OUTPUT.                       ST1324.2
058900     GO TO SRT-6.                                                 ST1324.2
059000 SRT-5-OUTPUT SECTION.                                            ST1324.2
059100 OPEN-SRT5-OUT.                                                   ST1324.2
059200     OPEN     OUTPUT FILE4.                                       ST1324.2
059300     MOVE     "SORT, OUTPUT PROC" TO FEATURE.                     ST1324.2
059400 SORT-TEST-19.                                                    ST1324.2
059500     PERFORM  RETURN-SORT5.                                       ST1324.2
059600     IF       S5-KEYS EQUAL TO "PQRAABCDEA7012345670"             ST1324.2
059700              PERFORM PASS-2 GO TO SORT-WRITE-19.                 ST1324.2
059800     GO       TO SORT-FAIL-19.                                    ST1324.2
059900 SORT-DELETE-19.                                                  ST1324.2
060000     PERFORM  DE-LETE-2.                                          ST1324.2
060100     GO       TO SORT-WRITE-19.                                   ST1324.2
060200 SORT-FAIL-19.                                                    ST1324.2
060300     MOVE     S5-KEYS TO COMPUTED-A.                              ST1324.2
060400     MOVE     "PQRAABCDEA7012345670" TO CORRECT-A.                ST1324.2
060500     PERFORM  FAIL-2.                                             ST1324.2
060600 SORT-WRITE-19.                                                   ST1324.2
060700     MOVE     "SORT-TEST-19" TO PAR-NAME.                         ST1324.2
060800     PERFORM  PRINT-DETAIL-2.                                     ST1324.2
060900 SORT-TEST-20.                                                    ST1324.2
061000     PERFORM  RETURN-SORT5 99 TIMES.                              ST1324.2
061100     IF       S5-KEYS EQUAL TO "PQRBABCDEZ7912345679"             ST1324.2
061200              PERFORM PASS-2 GO TO SORT-WRITE-20.                 ST1324.2
061300     GO       TO SORT-FAIL-20.                                    ST1324.2
061400 SORT-DELETE-20.                                                  ST1324.2
061500     PERFORM  DE-LETE-2.                                          ST1324.2
061600     GO       TO SORT-WRITE-20.                                   ST1324.2
061700 SORT-FAIL-20.                                                    ST1324.2
061800     MOVE     S5-KEYS TO COMPUTED-A.                              ST1324.2
061900     MOVE     "PQRBABCDEZ7912345679" TO CORRECT-A.                ST1324.2
062000     PERFORM  FAIL-2.                                             ST1324.2
062100 SORT-WRITE-20.                                                   ST1324.2
062200     MOVE     "SORT-TEST-20" TO PAR-NAME.                         ST1324.2
062300     PERFORM  PRINT-DETAIL-2.                                     ST1324.2
062400 CLOSE-2.                                                         ST1324.2
062500     CLOSE    FILE4.                                              ST1324.2
062600     GO       TO EXIT-2.                                          ST1324.2
062700 RETURN-SORT5.                                                    ST1324.2
062800     RETURN   SORT5 INTO R4 END GO TO TERMINAL-2.                 ST1324.2
062900*    NOTE     RETURN WITHOUT OPTIONAL WORDS.                      ST1324.2
063000     WRITE    R4.                                                 ST1324.2
063100 TERMINAL-2.                                                      ST1324.2
063200     PERFORM  FAIL-2.                                             ST1324.2
063300     MOVE     "TERMINAL-2" TO PAR-NAME.                           ST1324.2
063400     MOVE     "END OF FILE PREMATURELY" TO RE-MARK.               ST1324.2
063500     PERFORM  PRINT-DETAIL-2.                                     ST1324.2
063600     MOVE     SPACE TO FEATURE.                                   ST1324.2
063700     MOVE     "FOUND, PREVIOUS TEST WAS" TO RE-MARK.              ST1324.2
063800     PERFORM  PRINT-DETAIL-2.                                     ST1324.2
063900     MOVE     "LAST SUCCESSFUL TEST." TO RE-MARK.                 ST1324.2
064000     PERFORM  PRINT-DETAIL-2.                                     ST1324.2
064100     GO       TO CLOSE-2.                                         ST1324.2
064200 INSPT-2. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1324.2
064300 PASS-2.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1324.2
064400 FAIL-2.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1324.2
064500 DE-LETE-2.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1324.2
064600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1324.2
064700 PRINT-DETAIL-2.                                                  ST1324.2
064800     IF REC-CT NOT EQUAL TO ZERO                                  ST1324.2
064900             MOVE "." TO PARDOT-X                                 ST1324.2
065000             MOVE REC-CT TO DOTVALUE.                             ST1324.2
065100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-2.    ST1324.2
065200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-2             ST1324.2
065300        PERFORM FAIL-ROUTINE-2 THRU FAIL-ROUTINE-EX-2             ST1324.2
065400          ELSE PERFORM BAIL-OUT-2 THRU BAIL-OUT-EX-2.             ST1324.2
065500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1324.2
065600     MOVE SPACE TO CORRECT-X.                                     ST1324.2
065700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1324.2
065800     MOVE     SPACE TO RE-MARK.                                   ST1324.2
065900 WRITE-LINE-2.                                                    ST1324.2
066000     ADD 1 TO RECORD-COUNT.                                       ST1324.2
066100*    IF RECORD-COUNT GREATER 50                                   ST1324.2
066200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1324.2
066300*        MOVE SPACE TO DUMMY-RECORD                               ST1324.2
066400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1324.2
066500*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-2           ST1324.2
066600*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-2 2 TIMES   ST1324.2
066700*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-2        ST1324.2
066800*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1324.2
066900*        MOVE ZERO TO RECORD-COUNT.                               ST1324.2
067000     PERFORM WRT-LN-2.                                            ST1324.2
067100 WRT-LN-2.                                                        ST1324.2
067200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1324.2
067300     MOVE SPACE TO DUMMY-RECORD.                                  ST1324.2
067400 BLANK-LINE-PRINT-2.                                              ST1324.2
067500     PERFORM WRT-LN-2.                                            ST1324.2
067600 FAIL-ROUTINE-2.                                                  ST1324.2
067700     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.     ST1324.2
067800     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.      ST1324.2
067900     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1324.2
068000     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   ST1324.2
068100     GO TO FAIL-ROUTINE-EX-2.                                     ST1324.2
068200 FAIL-RTN-WRITE-2.                                                ST1324.2
068300     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-2         ST1324.2
068400     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-2 2 TIMES. ST1324.2
068500 FAIL-ROUTINE-EX-2. EXIT.                                         ST1324.2
068600 BAIL-OUT-2.                                                      ST1324.2
068700     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-2.     ST1324.2
068800     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-2.             ST1324.2
068900 BAIL-OUT-WRITE-2.                                                ST1324.2
069000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1324.2
069100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   ST1324.2
069200 BAIL-OUT-EX-2. EXIT.                                             ST1324.2
069300 EXIT-2.                                                          ST1324.2
069400     EXIT.                                                        ST1324.2
069500 SRT-6 SECTION.                                                   ST1324.2
069600 SIXTH-SORT.                                                      ST1324.2
069700     SORT SORT4                                                   ST1324.2
069800         ASCENDING S4-KEY2                                        ST1324.2
069900             USING FILE4                                          ST1324.2
070000             OUTPUT PROCEDURE SRT-6-OUTPUT.                       ST1324.2
070100 STOP-RUN.                                                        ST1324.2
070200     GO TO    CCVS-EXIT.                                          ST1324.2
070300 SRT-6-OUTPUT SECTION.                                            ST1324.2
070400 SORT-TEST-21.                                                    ST1324.2
070500     PERFORM  PASS-3.                                             ST1324.2
070600     MOVE     "2 SORTS ON ONE FILE" TO FEATURE.                   ST1324.2
070700     MOVE     "SORT-TEST-21" TO PAR-NAME.                         ST1324.2
070800     PERFORM  PRINT-DETAIL-3.                                     ST1324.2
070900*    NOTE     THIS TESTS THE ABILITY TO SORT A FILE A SECOND TIME.ST1324.2
071000     GO       TO EXIT-3.                                          ST1324.2
071100 RETURN-FOR-THE-HELLUVIT.                                         ST1324.2
071200     RETURN   SORT4 AT END GO TO PASS-3.                          ST1324.2
071300*        NOTE THE STANDARD REQUIRES THAT EVERY OUTPUT PROCEDURE   ST1324.2
071400*             HAVE AT LEAST ONE RETURN STATEMENT --- THE ABOVE    ST1324.2
071500*             STATEMENT IS COMPILED BUT NEVER EXECUTED.           ST1324.2
071600 PASS-3.                                                          ST1324.2
071700     MOVE "PASS" TO P-OR-F.  ADD 1 TO PASS-COUNTER.               ST1324.2
071800 FAIL-3.                                                          ST1324.2
071900     ADD      1 TO ERROR-COUNTER.                                 ST1324.2
072000     MOVE "FAIL*" TO P-OR-F.                                      ST1324.2
072100 DE-LETE-3.                                                       ST1324.2
072200     MOVE     SPACE TO P-OR-F.                                    ST1324.2
072300     MOVE     "    ************    " TO COMPUTED-A.               ST1324.2
072400     MOVE     "    ************    " TO CORRECT-A.                ST1324.2
072500     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1324.2
072600     ADD 1 TO DELETE-COUNTER.                                     ST1324.2
072700 PRINT-DETAIL-3.                                                  ST1324.2
072800     IF REC-CT NOT EQUAL TO ZERO                                  ST1324.2
072900             MOVE "." TO PARDOT-X                                 ST1324.2
073000             MOVE REC-CT TO DOTVALUE.                             ST1324.2
073100     MOVE     TEST-RESULTS TO PRINT-REC.                          ST1324.2
073200     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1324.2
073300     MOVE     SPACE TO P-OR-F.                                    ST1324.2
073400     MOVE     SPACE TO COMPUTED-A.                                ST1324.2
073500     MOVE SPACE TO CORRECT-A.                                     ST1324.2
073600     IF     REC-CT EQUAL TO ZERO                                  ST1324.2
073700              MOVE SPACE TO PAR-NAME.                             ST1324.2
073800     MOVE     SPACE TO RE-MARK.                                   ST1324.2
073900 EXIT-3.                                                          ST1324.2
074000     EXIT.                                                        ST1324.2
074100 CCVS-EXIT SECTION.                                               ST1324.2
074200 CCVS-999999.                                                     ST1324.2
074300     GO TO CLOSE-FILES.                                           ST1324.2
      *END-OF,ST132A                                                                  
