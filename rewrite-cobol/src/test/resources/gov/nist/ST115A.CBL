      *HEADER,COBOL,ST115A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1154.2
000200 PROGRAM-ID.                                                      ST1154.2
000300     ST115A.                                                      ST1154.2
000400****************************************************************  ST1154.2
000500*                                                              *  ST1154.2
000600*    VALIDATION FOR:-                                          *  ST1154.2
000700*                                                              *  ST1154.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1154.2
000900*                                                              *  ST1154.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1154.2
001100*                                                              *  ST1154.2
001200****************************************************************  ST1154.2
001300*                                                              *  ST1154.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1154.2
001500*                                                              *  ST1154.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1154.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1154.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1154.2
001900*        X-XXXP01     SQ-FS1                                   *  ST1154.2
002000*        X-XXX065     4 DIGIT INTEGER FOR THE NUMBER           *  ST1154.2
002100*                        RECORDS IN THE FILE SQ-FS1.           *  ST1154.2
002200*                                                              *  ST1154.2
002300*                                                              *  ST1154.2
002400****************************************************************  ST1154.2
002500*                                                                 ST1154.2
002600*                                                                 ST1154.2
002700*    OBJECTIVE -                                                  ST1154.2
002800*        ROUTINE ST115 BUILDS A SEQUENTIAL FILE SQ-FS1 WHICH      ST1154.2
002900*    IS THEN PASSED TO ST116 TO BE SORTED.                        ST1154.2
003000*                                                                 ST1154.2
003100*                                                                 ST1154.2
003200*    FEATURES TESTED -                                            ST1154.2
003300*    *   FIXED LENGTH RECORDS                                     ST1154.2
003400*    *   OCCURS CLAUSES                                           ST1154.2
003500*                                                                 ST1154.2
003600*                                                                 ST1154.2
003700*                                                                 ST1154.2
003800*    FILES USED -                                                 ST1154.2
003900*    *   FILE SQ-FS1 CAN BE ON MAGNETIC TAPE OR MASS-STORAGE.     ST1154.2
004000*                                                                 ST1154.2
004100*        SQ-FS1 -                                                 ST1154.2
004200*    THE NUMBER OF RECORDS ON SQ-FS1 IS A VARIABLE (X-65).  THIS  ST1154.2
004300*        NUMBER SHOULD BE LARGE ENOUGH TO FORCE THE SORT ROUTINE  ST1154.2
004400*        IN ST116 TO BE NON-CORE RESIDENT.  THAT IS FORCE         ST1154.2
004500*        THE SYSTEM TO USE SOME MEANS OF AUX. STORAGE FOR THE SORTST1154.2
004600*        SUB-STRINGS.                                             ST1154.2
004700*    FIXED LENGTH RECORDS ( 507 CHARACTERS PER RECORD )           ST1154.2
004800*    BLOCKED 1                                                    ST1154.2
004900*    RESERVE 2 AREAS                                              ST1154.2
005000*                                                                 ST1154.2
005100*                                                                 ST1154.2
005200*                                                                 ST1154.2
005300*    OPTIONS RECOMMENDED -                                        ST1154.2
005400*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1154.2
005500*                 FILE SQ-FS1 ONCE IT HAS BEEN CREATED.           ST1154.2
005600*                                                                 ST1154.2
005700*                                                                 ST1154.2
005800*    TEST DESCRIPTIONS -                                          ST1154.2
005900*        NOT APPLICABLE.                                          ST1154.2
006000*                                                                 ST1154.2
006100*                                                                 ST1154.2
006200*    ************************************************************ ST1154.2
006300 ENVIRONMENT DIVISION.                                            ST1154.2
006400 CONFIGURATION SECTION.                                           ST1154.2
006500 SOURCE-COMPUTER.                                                 ST1154.2
006600     XXXXX082.                                                    ST1154.2
006700 OBJECT-COMPUTER.                                                 ST1154.2
006800     XXXXX083.                                                    ST1154.2
006900 INPUT-OUTPUT SECTION.                                            ST1154.2
007000 FILE-CONTROL.                                                    ST1154.2
007100     SELECT PRINT-FILE ASSIGN TO                                  ST1154.2
007200     XXXXX055.                                                    ST1154.2
007300     SELECT SQ-FS1 ASSIGN TO                                      ST1154.2
007400     XXXXP001                                                     ST1154.2
007500     ORGANIZATION IS SEQUENTIAL                                   ST1154.2
007600     ACCESS MODE IS SEQUENTIAL.                                   ST1154.2
007700 DATA DIVISION.                                                   ST1154.2
007800 FILE SECTION.                                                    ST1154.2
007900 FD  PRINT-FILE.                                                  ST1154.2
008000 01  PRINT-REC PICTURE X(120).                                    ST1154.2
008100 01  DUMMY-RECORD PICTURE X(120).                                 ST1154.2
008200 FD  SQ-FS1                                                       ST1154.2
008300     LABEL RECORDS STANDARD                                       ST1154.2
008400C    VALUE OF                                                     ST1154.2
008500C    XXXXX074                                                     ST1154.2
008600C    IS                                                           ST1154.2
008700C    XXXXX075                                                     ST1154.2
008800*    XXXXX069                                                     ST1154.2
008900     BLOCK CONTAINS 1 RECORDS                                     ST1154.2
009000     RECORD CONTAINS 507 CHARACTERS                               ST1154.2
009100     DATA RECORD IS SQ-FS1R1-F-G-507.                             ST1154.2
009200 01  SQ-FS1R1-F-G-507.                                            ST1154.2
009300     10 REC-PREAMBLE PIC X(120).                                  ST1154.2
009400     10  LENGTH-1 PIC 999.                                        ST1154.2
009500     10  THE-THREE-KEYS.                                          ST1154.2
009600     20 KEY-1.                                                    ST1154.2
009700         30 ALPHAN-KEY PIC X.                                     ST1154.2
009800         30 NUM-KEY PIC 999.                                      ST1154.2
009900     20 KEY-2.                                                    ST1154.2
010000         30 ALPHAN-KEY PIC X.                                     ST1154.2
010100         30 NUM-KEY PIC 999.                                      ST1154.2
010200     20 KEY-3.                                                    ST1154.2
010300         30 ALPHAN-KEY PIC X.                                     ST1154.2
010400         30 NUM-KEY PIC 999.                                      ST1154.2
010500     10  STUFF-1 OCCURS                                           ST1154.2
010600             31 TIMES.                                            ST1154.2
010700         30  FILL-ME-UPS  PIC  X(12).                             ST1154.2
010800 WORKING-STORAGE SECTION.                                         ST1154.2
010900 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1154.2
011000 77  WRK-DU-999-0001 PIC 999.                                     ST1154.2
011100 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1154.2
011200 77  WRK-DU-999-0002 PIC 999 VALUE 0.                             ST1154.2
011300 77  WRK-DU-04V00 PIC 9(4) VALUE ZERO.                            ST1154.2
011400*77  COUNT-OF-RECS PIC 9(6) VALUE ZERO.                           ST1154.2
011500 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1154.2
011600     "/A.Z-B,Y+C*X)D(W$E$V F0U1G2T3H4S5I6R7J8Q9K;P<L=O>MN".       ST1154.2
011700 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1154.2
011800     02 CHAR PIC X OCCURS 51 TIMES.                               ST1154.2
011900 01  FILE-RECORD-INFORMATION-REC.                                 ST1154.2
012000     03 FILE-RECORD-INFO-SKELETON.                                ST1154.2
012100        05 FILLER                 PICTURE X(48)       VALUE       ST1154.2
012200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1154.2
012300        05 FILLER                 PICTURE X(46)       VALUE       ST1154.2
012400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1154.2
012500        05 FILLER                 PICTURE X(26)       VALUE       ST1154.2
012600             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1154.2
012700        05 FILLER                 PICTURE X(37)       VALUE       ST1154.2
012800             ",RECKEY=                             ".             ST1154.2
012900        05 FILLER                 PICTURE X(38)       VALUE       ST1154.2
013000             ",ALTKEY1=                             ".            ST1154.2
013100        05 FILLER                 PICTURE X(38)       VALUE       ST1154.2
013200             ",ALTKEY2=                             ".            ST1154.2
013300        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1154.2
013400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1154.2
013500        05 FILE-RECORD-INFO-P1-120.                               ST1154.2
013600           07 FILLER              PIC X(5).                       ST1154.2
013700           07 XFILE-NAME           PIC X(6).                      ST1154.2
013800           07 FILLER              PIC X(8).                       ST1154.2
013900           07 XRECORD-NAME         PIC X(6).                      ST1154.2
014000           07 FILLER              PIC X(1).                       ST1154.2
014100           07 REELUNIT-NUMBER     PIC 9(1).                       ST1154.2
014200           07 FILLER              PIC X(7).                       ST1154.2
014300           07 XRECORD-NUMBER       PIC 9(6).                      ST1154.2
014400           07 FILLER              PIC X(6).                       ST1154.2
014500           07 UPDATE-NUMBER       PIC 9(2).                       ST1154.2
014600           07 FILLER              PIC X(5).                       ST1154.2
014700           07 ODO-NUMBER          PIC 9(4).                       ST1154.2
014800           07 FILLER              PIC X(5).                       ST1154.2
014900           07 XPROGRAM-NAME        PIC X(5).                      ST1154.2
015000           07 FILLER              PIC X(7).                       ST1154.2
015100           07 XRECORD-LENGTH       PIC 9(6).                      ST1154.2
015200           07 FILLER              PIC X(7).                       ST1154.2
015300           07 CHARS-OR-RECORDS    PIC X(2).                       ST1154.2
015400           07 FILLER              PIC X(1).                       ST1154.2
015500           07 XBLOCK-SIZE          PIC 9(4).                      ST1154.2
015600           07 FILLER              PIC X(6).                       ST1154.2
015700           07 RECORDS-IN-FILE     PIC 9(6).                       ST1154.2
015800           07 FILLER              PIC X(5).                       ST1154.2
015900           07 XFILE-ORGANIZATION   PIC X(2).                      ST1154.2
016000           07 FILLER              PIC X(6).                       ST1154.2
016100           07 XLABEL-TYPE          PIC X(1).                      ST1154.2
016200        05 FILE-RECORD-INFO-P121-240.                             ST1154.2
016300           07 FILLER              PIC X(8).                       ST1154.2
016400           07 XRECORD-KEY          PIC X(29).                     ST1154.2
016500           07 FILLER              PIC X(9).                       ST1154.2
016600           07 ALTERNATE-KEY1      PIC X(29).                      ST1154.2
016700           07 FILLER              PIC X(9).                       ST1154.2
016800           07 ALTERNATE-KEY2      PIC X(29).                      ST1154.2
016900           07 FILLER              PIC X(7).                       ST1154.2
017000 01  TEST-RESULTS.                                                ST1154.2
017100     02 FILLER                   PIC X      VALUE SPACE.          ST1154.2
017200     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1154.2
017300     02 FILLER                   PIC X      VALUE SPACE.          ST1154.2
017400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1154.2
017500     02 FILLER                   PIC X      VALUE SPACE.          ST1154.2
017600     02  PAR-NAME.                                                ST1154.2
017700       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1154.2
017800       03  PARDOT-X              PIC X      VALUE SPACE.          ST1154.2
017900       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1154.2
018000     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1154.2
018100     02 RE-MARK                  PIC X(61).                       ST1154.2
018200 01  TEST-COMPUTED.                                               ST1154.2
018300     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1154.2
018400     02 FILLER                   PIC X(17)  VALUE                 ST1154.2
018500            "       COMPUTED=".                                   ST1154.2
018600     02 COMPUTED-X.                                               ST1154.2
018700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1154.2
018800     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1154.2
018900                                 PIC -9(9).9(9).                  ST1154.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1154.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1154.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1154.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1154.2
019400         04 COMPUTED-18V0                    PIC -9(18).          ST1154.2
019500         04 FILLER                           PIC X.               ST1154.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             ST1154.2
019700 01  TEST-CORRECT.                                                ST1154.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             ST1154.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1154.2
020000     02 CORRECT-X.                                                ST1154.2
020100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1154.2
020200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1154.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1154.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1154.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1154.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         ST1154.2
020700         04 CORRECT-18V0                     PIC -9(18).          ST1154.2
020800         04 FILLER                           PIC X.               ST1154.2
020900     03 FILLER PIC X(2) VALUE SPACE.                              ST1154.2
021000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1154.2
021100 01  CCVS-C-1.                                                    ST1154.2
021200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1154.2
021300-    "SS  PARAGRAPH-NAME                                          ST1154.2
021400-    "       REMARKS".                                            ST1154.2
021500     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1154.2
021600 01  CCVS-C-2.                                                    ST1154.2
021700     02 FILLER                     PIC X        VALUE SPACE.      ST1154.2
021800     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1154.2
021900     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1154.2
022000     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1154.2
022100     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1154.2
022200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1154.2
022300 01  REC-CT                        PIC 99       VALUE ZERO.       ST1154.2
022400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1154.2
022500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1154.2
022600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1154.2
022700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1154.2
022800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1154.2
022900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1154.2
023000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1154.2
023100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1154.2
023200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1154.2
023300 01  CCVS-H-1.                                                    ST1154.2
023400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1154.2
023500     02  FILLER                    PIC X(42)    VALUE             ST1154.2
023600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1154.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1154.2
023800 01  CCVS-H-2A.                                                   ST1154.2
023900   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1154.2
024000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1154.2
024100   02  FILLER                        PIC XXXX   VALUE             ST1154.2
024200     "4.2 ".                                                      ST1154.2
024300   02  FILLER                        PIC X(28)  VALUE             ST1154.2
024400            " COPY - NOT FOR DISTRIBUTION".                       ST1154.2
024500   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1154.2
024600                                                                  ST1154.2
024700 01  CCVS-H-2B.                                                   ST1154.2
024800   02  FILLER                        PIC X(15)  VALUE             ST1154.2
024900            "TEST RESULT OF ".                                    ST1154.2
025000   02  TEST-ID                       PIC X(9).                    ST1154.2
025100   02  FILLER                        PIC X(4)   VALUE             ST1154.2
025200            " IN ".                                               ST1154.2
025300   02  FILLER                        PIC X(12)  VALUE             ST1154.2
025400     " HIGH       ".                                              ST1154.2
025500   02  FILLER                        PIC X(22)  VALUE             ST1154.2
025600            " LEVEL VALIDATION FOR ".                             ST1154.2
025700   02  FILLER                        PIC X(58)  VALUE             ST1154.2
025800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1154.2
025900 01  CCVS-H-3.                                                    ST1154.2
026000     02  FILLER                      PIC X(34)  VALUE             ST1154.2
026100            " FOR OFFICIAL USE ONLY    ".                         ST1154.2
026200     02  FILLER                      PIC X(58)  VALUE             ST1154.2
026300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1154.2
026400     02  FILLER                      PIC X(28)  VALUE             ST1154.2
026500            "  COPYRIGHT   1985 ".                                ST1154.2
026600 01  CCVS-E-1.                                                    ST1154.2
026700     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1154.2
026800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1154.2
026900     02 ID-AGAIN                     PIC X(9).                    ST1154.2
027000     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1154.2
027100 01  CCVS-E-2.                                                    ST1154.2
027200     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1154.2
027300     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1154.2
027400     02 CCVS-E-2-2.                                               ST1154.2
027500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1154.2
027600         03 FILLER                   PIC X      VALUE SPACE.      ST1154.2
027700         03 ENDER-DESC               PIC X(44)  VALUE             ST1154.2
027800            "ERRORS ENCOUNTERED".                                 ST1154.2
027900 01  CCVS-E-3.                                                    ST1154.2
028000     02  FILLER                      PIC X(22)  VALUE             ST1154.2
028100            " FOR OFFICIAL USE ONLY".                             ST1154.2
028200     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1154.2
028300     02  FILLER                      PIC X(58)  VALUE             ST1154.2
028400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1154.2
028500     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1154.2
028600     02 FILLER                       PIC X(15)  VALUE             ST1154.2
028700             " COPYRIGHT 1985".                                   ST1154.2
028800 01  CCVS-E-4.                                                    ST1154.2
028900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1154.2
029000     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1154.2
029100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1154.2
029200     02 FILLER                       PIC X(40)  VALUE             ST1154.2
029300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1154.2
029400 01  XXINFO.                                                      ST1154.2
029500     02 FILLER                       PIC X(19)  VALUE             ST1154.2
029600            "*** INFORMATION ***".                                ST1154.2
029700     02 INFO-TEXT.                                                ST1154.2
029800       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1154.2
029900       04 XXCOMPUTED                 PIC X(20).                   ST1154.2
030000       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1154.2
030100       04 XXCORRECT                  PIC X(20).                   ST1154.2
030200     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1154.2
030300 01  HYPHEN-LINE.                                                 ST1154.2
030400     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1154.2
030500     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1154.2
030600-    "*****************************************".                 ST1154.2
030700     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1154.2
030800-    "******************************".                            ST1154.2
030900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1154.2
031000     "ST115A".                                                    ST1154.2
031100 PROCEDURE DIVISION.                                              ST1154.2
031200 CCVS1 SECTION.                                                   ST1154.2
031300 OPEN-FILES.                                                      ST1154.2
031400     OPEN    OUTPUT PRINT-FILE.                                   ST1154.2
031500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1154.2
031600     MOVE    SPACE TO TEST-RESULTS.                               ST1154.2
031700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1154.2
031800     MOVE    ZERO TO REC-SKL-SUB.                                 ST1154.2
031900     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1154.2
032000 CCVS-INIT-FILE.                                                  ST1154.2
032100     ADD     1 TO REC-SKL-SUB.                                    ST1154.2
032200     MOVE    FILE-RECORD-INFO-SKELETON                            ST1154.2
032300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1154.2
032400 CCVS-INIT-EXIT.                                                  ST1154.2
032500     GO TO CCVS1-EXIT.                                            ST1154.2
032600 CLOSE-FILES.                                                     ST1154.2
032700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1154.2
032800 TERMINATE-CCVS.                                                  ST1154.2
032900*    EXIT PROGRAM.                                                ST1154.2
033000*TERMINATE-CALL.                                                  ST1154.2
033100     STOP     RUN.                                                ST1154.2
033200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1154.2
033300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1154.2
033400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1154.2
033500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1154.2
033600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1154.2
033700 PRINT-DETAIL.                                                    ST1154.2
033800     IF REC-CT NOT EQUAL TO ZERO                                  ST1154.2
033900             MOVE "." TO PARDOT-X                                 ST1154.2
034000             MOVE REC-CT TO DOTVALUE.                             ST1154.2
034100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1154.2
034200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1154.2
034300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1154.2
034400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1154.2
034500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1154.2
034600     MOVE SPACE TO CORRECT-X.                                     ST1154.2
034700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1154.2
034800     MOVE     SPACE TO RE-MARK.                                   ST1154.2
034900 HEAD-ROUTINE.                                                    ST1154.2
035000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1154.2
035100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1154.2
035200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1154.2
035300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1154.2
035400 COLUMN-NAMES-ROUTINE.                                            ST1154.2
035500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1154.2
035600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1154.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1154.2
035800 END-ROUTINE.                                                     ST1154.2
035900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1154.2
036000 END-RTN-EXIT.                                                    ST1154.2
036100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1154.2
036200 END-ROUTINE-1.                                                   ST1154.2
036300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1154.2
036400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1154.2
036500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1154.2
036600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1154.2
036700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1154.2
036800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1154.2
036900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1154.2
037000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1154.2
037100  END-ROUTINE-12.                                                 ST1154.2
037200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1154.2
037300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1154.2
037400         MOVE "NO " TO ERROR-TOTAL                                ST1154.2
037500         ELSE                                                     ST1154.2
037600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1154.2
037700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1154.2
037800     PERFORM WRITE-LINE.                                          ST1154.2
037900 END-ROUTINE-13.                                                  ST1154.2
038000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1154.2
038100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1154.2
038200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1154.2
038300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1154.2
038400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1154.2
038500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1154.2
038600          MOVE "NO " TO ERROR-TOTAL                               ST1154.2
038700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1154.2
038800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1154.2
038900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1154.2
039000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1154.2
039100 WRITE-LINE.                                                      ST1154.2
039200     ADD 1 TO RECORD-COUNT.                                       ST1154.2
039300*    IF RECORD-COUNT GREATER 42                                   ST1154.2
039400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1154.2
039500*        MOVE SPACE TO DUMMY-RECORD                               ST1154.2
039600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1154.2
039700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1154.2
039800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1154.2
039900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1154.2
040000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1154.2
040100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1154.2
040200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1154.2
040300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1154.2
040400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1154.2
040500*        MOVE ZERO TO RECORD-COUNT.                               ST1154.2
040600     PERFORM WRT-LN.                                              ST1154.2
040700 WRT-LN.                                                          ST1154.2
040800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1154.2
040900     MOVE SPACE TO DUMMY-RECORD.                                  ST1154.2
041000 BLANK-LINE-PRINT.                                                ST1154.2
041100     PERFORM WRT-LN.                                              ST1154.2
041200 FAIL-ROUTINE.                                                    ST1154.2
041300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1154.2
041400            GO TO   FAIL-ROUTINE-WRITE.                           ST1154.2
041500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1154.2
041600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1154.2
041700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1154.2
041800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1154.2
041900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1154.2
042000     GO TO  FAIL-ROUTINE-EX.                                      ST1154.2
042100 FAIL-ROUTINE-WRITE.                                              ST1154.2
042200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1154.2
042300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1154.2
042400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1154.2
042500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1154.2
042600 FAIL-ROUTINE-EX. EXIT.                                           ST1154.2
042700 BAIL-OUT.                                                        ST1154.2
042800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1154.2
042900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1154.2
043000 BAIL-OUT-WRITE.                                                  ST1154.2
043100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1154.2
043200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1154.2
043300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1154.2
043400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1154.2
043500 BAIL-OUT-EX. EXIT.                                               ST1154.2
043600 CCVS1-EXIT.                                                      ST1154.2
043700     EXIT.                                                        ST1154.2
043800 SECT-ST115-0001 SECTION.                                         ST1154.2
043900 SRT-INIT-001.                                                    ST1154.2
044000     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1154.2
044100     OPEN OUTPUT SQ-FS1.                                          ST1154.2
044200     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1154.2
044300     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1154.2
044400     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       ST1154.2
044500     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1154.2
044600     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1154.2
044700     MOVE                                                         ST1154.2
044800     XXXXX065                                                     ST1154.2
044900             TO RECORDS-IN-FILE (1).                              ST1154.2
045000     MOVE    507 TO XRECORD-LENGTH (1).                           ST1154.2
045100     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1154.2
045200     MOVE "S" TO XLABEL-TYPE (1).                                 ST1154.2
045300     MOVE 000000 TO XRECORD-NUMBER (1).                           ST1154.2
045400 SRT-TEST-001.                                                    ST1154.2
045500     MOVE    001 TO WRK-DU-999-0001.                              ST1154.2
045600     MOVE    1 TO WRK-DU-04V00.                                   ST1154.2
045700 SRT-TEST-001-01.                                                 ST1154.2
045800     PERFORM SRT-TEST-001-BUILD.                                  ST1154.2
045900     ADD 1 TO WRK-DU-04V00.                                       ST1154.2
046000     IF WRK-DU-04V00 IS GREATER THAN                              ST1154.2
046100     XXXXX065                                                     ST1154.2
046200     GO TO SRT-WRITE-001.                                         ST1154.2
046300     GO TO SRT-TEST-001-01.                                       ST1154.2
046400 SRT-TEST-001-BUILD.                                              ST1154.2
046500     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-1           ST1154.2
046600         ALPHAN-KEY OF KEY-2 ALPHAN-KEY OF KEY-3.                 ST1154.2
046700     MOVE WRK-DU-04V00    TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1154.2
046800         NUM-KEY OF KEY-3.                                        ST1154.2
046900     MOVE    507 TO LENGTH-1.                                     ST1154.2
047000     PERFORM PAD-THE-RECORD-LENGTH VARYING WRK-DU-999-0002        ST1154.2
047100         FROM 1 BY 1 UNTIL WRK-DU-999-0002 IS GREATER THAN        ST1154.2
047200             31.                                                  ST1154.2
047300     ADD 1 TO XRECORD-NUMBER (1).                                 ST1154.2
047400     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1154.2
047500     ADD     001 TO WRK-DU-999-0001.                              ST1154.2
047600     IF      WRK-DU-999-0001 IS GREATER THAN 51                   ST1154.2
047700         MOVE 001 TO WRK-DU-999-0001.                             ST1154.2
047800     WRITE   SQ-FS1R1-F-G-507.                                    ST1154.2
047900 PAD-THE-RECORD-LENGTH.                                           ST1154.2
048000     MOVE THE-THREE-KEYS  TO STUFF-1 (WRK-DU-999-0002).           ST1154.2
048100 SRT-DELETE-001.                                                  ST1154.2
048200     PERFORM DE-LETE.                                             ST1154.2
048300 SRT-WRITE-001.                                                   ST1154.2
048400     MOVE    "FILE-CREATE" TO PAR-NAME.                           ST1154.2
048500     MOVE    "SQ-FS1 FILE CREATED" TO COMPUTED-A.                 ST1154.2
048600     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1154.2
048700     MOVE    "PASSED TO ST116 FOR SORTING" TO RE-MARK.            ST1154.2
048800     PERFORM PRINT-DETAIL.                                        ST1154.2
048900     MOVE    "        ************   ST116 WILL NOT PRODUCE ANY PRST1154.2
049000-    "INTED REPORT   ************" TO PRINT-REC.                  ST1154.2
049100     WRITE   PRINT-REC AFTER ADVANCING 1 LINES.                   ST1154.2
049200     CLOSE SQ-FS1.                                                ST1154.2
049300*FILEDUMP SECTION.                                                ST1154.2
049400*FILE-1-DUMP-INIT.                                                ST1154.2
049500*    OPEN INPUT SQ-FS1.                                           ST1154.2
049600*    MOVE ZERO TO COUNT-OF-RECS.                                  ST1154.2
049700*FILE-1-DUMP.                                                     ST1154.2
049800*    READ SQ-FS1 RECORD                                           ST1154.2
049900*        AT END GO TO FILE-1-DUMP-END.                            ST1154.2
050000*    ADD 1 TO COUNT-OF-RECS.                                      ST1154.2
050100*    IF COUNT-OF-RECS GREATER THAN                                ST1154.2
050200*    XXXXX065                                                     ST1154.2
050300*        GO TO FILE-1-DUMP-END.                                   ST1154.2
050400*    PERFORM FILE-1-DUMP-WRITE.                                   ST1154.2
050500*    GO TO FILE-1-DUMP.                                           ST1154.2
050600*FILE-1-DUMP-WRITE.                                               ST1154.2
050700*    MOVE SQ-FS1R1-F-G-507 TO DUMMY-RECORD.                       ST1154.2
050800*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1154.2
050900*FILE-1-DUMP-END.                                                 ST1154.2
051000*    MOVE    " SQ-FS1 RECORDS TO SORTED BY ST116 SHOWN BELOW"     ST1154.2
051100*        TO DUMMY-RECORD.                                         ST1154.2
051200*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1154.2
051300*    MOVE    COUNT-OF-RECS TO DUMMY-RECORD.                       ST1154.2
051400*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1154.2
051500*    CLOSE   SQ-FS1.                                              ST1154.2
051600 CCVS-EXIT SECTION.                                               ST1154.2
051700 CCVS-999999.                                                     ST1154.2
051800     GO TO CLOSE-FILES.                                           ST1154.2
      *END-OF,ST115A                                                                  
