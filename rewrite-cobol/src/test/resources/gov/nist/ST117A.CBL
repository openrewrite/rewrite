      *HEADER,COBOL,ST115A,SUBPRG,ST117A                                              
000100 IDENTIFICATION DIVISION.                                         ST1174.2
000200 PROGRAM-ID.                                                      ST1174.2
000300     ST117A.                                                      ST1174.2
000400****************************************************************  ST1174.2
000500*                                                              *  ST1174.2
000600*    VALIDATION FOR:-                                          *  ST1174.2
000700*                                                              *  ST1174.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1174.2
000900*                                                              *  ST1174.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1174.2
001100*                                                              *  ST1174.2
001200****************************************************************  ST1174.2
001300*                                                              *  ST1174.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1174.2
001500*                                                              *  ST1174.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1174.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1174.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1174.2
001900*                                                              *  ST1174.2
002000****************************************************************  ST1174.2
002100*                                                                 ST1174.2
002200*                                                                 ST1174.2
002300*    ST117                                                        ST1174.2
002400*                                                                 ST1174.2
002500*                                                                 ST1174.2
002600*    OBJECTIVE -                                                  ST1174.2
002700*        ROUTINE ST117 CHECKS THE FILE ( SQ-FS2 ) WHICH IS GIVEN  ST1174.2
002800*    BY THE SORT IN ST116.  THE ALPHANUMERIC KEYS AND NUMERIC KEYSST1174.2
002900*    ARE BOTH CHECKED BY ST117.                                   ST1174.2
003000*                                                                 ST1174.2
003100*                                                                 ST1174.2
003200*    FEATURES TESTED -                                            ST1174.2
003300*    *   FIXED LENGTH RECORDS                                     ST1174.2
003400*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1174.2
003500*                                                                 ST1174.2
003600*                                                                 ST1174.2
003700*                                                                 ST1174.2
003800*    ANSI X3.23-1974 REFERENCES -                                 ST1174.2
003900*    *   SECTION 4.4  THE SORT STATEMENT  PAGE VII-14             ST1174.2
004000*                                                                 ST1174.2
004100*                                                                 ST1174.2
004200*    FILES USED -                                                 ST1174.2
004300*    *   FILE SQ-FS2 CAN BE ON MAGNETIC TAPE OR MASS-STORAGE.     ST1174.2
004400*                                                                 ST1174.2
004500*        SQ-FS2 -                                                 ST1174.2
004600*    NUMBER OF RECORDS IS SET IN X-65                             ST1174.2
004700*    FIXED LENGTH ( 507 CHARACTERS PER RECORD )                   ST1174.2
004800*    BLOCKED 2                                                    ST1174.2
004900*    RESERVE 4 AREAS                                              ST1174.2
005000*                                                                 ST1174.2
005100*                                                                 ST1174.2
005200*    X-CARDS USED  -                                              ST1174.2
005300*    X-XXXD02     SQ-FS2                                          ST1174.2
005400*    X-XXX063     NATIVE COLLATING SEQUENCE ASCENDING ORDER (NOTE ST1174.2
005500*        THAT THE QUOTE CHARACTER IS NOT TO APPEAR IN THE X-63    ST1174.2
005600*        CARD AND THE DOLLAR SIGN $ IS TO APPEAR TWICE WHEREVER   ST1174.2
005700*        THE $ BELONGS IN THE NATIVE COLLATING SEQUENCE).  IF     ST1174.2
005800*        THE NATIVE COLLATING SEQUENCE IS ACTUALLY THE ASCII      ST1174.2
005900*        COLLATING SEQUENCE SEE BELOW FOR A SAMPLE X-63 CARD..... ST1174.2
006000*                                                                 ST1174.2
006100*    X-63  " $$()*+,-./0123456789;<=>ABCDEFGHIJKLMNOPQRSTUVWXYZ". ST1174.2
006200*                                                                 ST1174.2
006300*    X-XXX065     4 DIGIT INTEGER FOR THE NUMBER OF RECORDS IN    ST1174.2
006400*                      THE FILE SQ-FS2.                           ST1174.2
006500*                                                                 ST1174.2
006600*                                                                 ST1174.2
006700*    OPTIONS RECOMMENDED -                                        ST1174.2
006800*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1174.2
006900*                 FILE SQ-FS2.                                    ST1174.2
007000*                                                                 ST1174.2
007100*                                                                 ST1174.2
007200*    TEST DESCRIPTIONS -                                          ST1174.2
007300*        THE INTEGER X-65 IS DIVIDED BY 51.  THIS IS THE NUMBER   ST1174.2
007400*    OF DUPLICATE RECORD KEYS THAT ARE EXPECTED IN SQ-FS2.  THESE ST1174.2
007500*    KEYS SHOULD BE THE LOWEST CHARACTER IN THE NATIVE COLLATING  ST1174.2
007600*    SEQUENCE.  ALL OF THE NUMERIC KEYS FOR THESE RECORDS SHOULD  ST1174.2
007700*    BE IN ASCENDING ORDER.                                       ST1174.2
007800*                                                                 ST1174.2
007900*                                                                 ST1174.2
008000*    ************************************************************ ST1174.2
008100 ENVIRONMENT DIVISION.                                            ST1174.2
008200 CONFIGURATION SECTION.                                           ST1174.2
008300 SOURCE-COMPUTER.                                                 ST1174.2
008400     XXXXX082.                                                    ST1174.2
008500 OBJECT-COMPUTER.                                                 ST1174.2
008600     XXXXX083.                                                    ST1174.2
008700 INPUT-OUTPUT SECTION.                                            ST1174.2
008800 FILE-CONTROL.                                                    ST1174.2
008900     SELECT PRINT-FILE ASSIGN TO                                  ST1174.2
009000     XXXXX055.                                                    ST1174.2
009100     SELECT SQ-FS2 ASSIGN TO                                      ST1174.2
009200     XXXXD002                                                     ST1174.2
009300     ORGANIZATION IS SEQUENTIAL                                   ST1174.2
009400     ACCESS MODE IS SEQUENTIAL.                                   ST1174.2
009500 DATA DIVISION.                                                   ST1174.2
009600 FILE SECTION.                                                    ST1174.2
009700 FD  PRINT-FILE.                                                  ST1174.2
009800 01  PRINT-REC PICTURE X(120).                                    ST1174.2
009900 01  DUMMY-RECORD PICTURE X(120).                                 ST1174.2
010000 FD  SQ-FS2                                                       ST1174.2
010100     LABEL RECORDS STANDARD                                       ST1174.2
010200C    VALUE OF                                                     ST1174.2
010300C    XXXXX074                                                     ST1174.2
010400C    IS                                                           ST1174.2
010500C    XXXXX076                                                     ST1174.2
010600*    XXXXX069                                                     ST1174.2
010700     BLOCK CONTAINS 2 RECORDS                                     ST1174.2
010800     RECORD CONTAINS 507 CHARACTERS                               ST1174.2
010900     DATA RECORD SQ-FS2R1-F-G-507.                                ST1174.2
011000 01  SQ-FS2R1-F-G-507.                                            ST1174.2
011100     10 REC-PRE-2 PIC X(120).                                     ST1174.2
011200     10  LENGTH-2 PIC 999.                                        ST1174.2
011300     10  THOSE-LOVABLE-KEYS.                                      ST1174.2
011400     20 KEY-4.                                                    ST1174.2
011500         30 ALPHAN-KEY-K4 PIC X.                                  ST1174.2
011600         30 NUM-KEY-K4 PIC 999.                                   ST1174.2
011700     20 KEY-5.                                                    ST1174.2
011800         30 ALPHAN-KEY-K5 PIC X.                                  ST1174.2
011900         30 NUM-KEY-K5 PIC 999.                                   ST1174.2
012000     20 KEY-6.                                                    ST1174.2
012100         30 ALPHAN-KEY-K6 PIC X.                                  ST1174.2
012200         30 NUM-KEY-K6 PIC 999.                                   ST1174.2
012300     10  STUFF-FOR-FUN OCCURS                                     ST1174.2
012400             31 TIMES.                                            ST1174.2
012500         30  FILLER  PIC X(12).                                   ST1174.2
012600 WORKING-STORAGE SECTION.                                         ST1174.2
012700 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1174.2
012800 77  WRK-DU-9-2 PIC 9 VALUE 0.                                    ST1174.2
012900 77  WRK-DU-999-0001 PIC 999.                                     ST1174.2
013000 77  WRK-DU-999-2 PIC 999 VALUE 000.                              ST1174.2
013100 77  WRK-DU-999-3 PIC 999 VALUE ZERO.                             ST1174.2
013200 77  NUMBER-OF-SETS PIC 999 VALUE ZERO.                           ST1174.2
013300*77  COUNT-OF-RECS PIC 9(6) VALUE ZERO.                           ST1174.2
013400 01  WRK-XN-2    PIC X(51) VALUE                                  ST1174.2
013500     "                     * G BAIRD, USNAVY            ".        ST1174.2
013600 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1174.2
013700     02 ANSWER PIC X OCCURS 51 TIMES.                             ST1174.2
013800 01  FILE-RECORD-INFORMATION-REC.                                 ST1174.2
013900     03 FILE-RECORD-INFO-SKELETON.                                ST1174.2
014000        05 FILLER                 PICTURE X(48)       VALUE       ST1174.2
014100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1174.2
014200        05 FILLER                 PICTURE X(46)       VALUE       ST1174.2
014300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1174.2
014400        05 FILLER                 PICTURE X(26)       VALUE       ST1174.2
014500             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1174.2
014600        05 FILLER                 PICTURE X(37)       VALUE       ST1174.2
014700             ",RECKEY=                             ".             ST1174.2
014800        05 FILLER                 PICTURE X(38)       VALUE       ST1174.2
014900             ",ALTKEY1=                             ".            ST1174.2
015000        05 FILLER                 PICTURE X(38)       VALUE       ST1174.2
015100             ",ALTKEY2=                             ".            ST1174.2
015200        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1174.2
015300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1174.2
015400        05 FILE-RECORD-INFO-P1-120.                               ST1174.2
015500           07 FILLER              PIC X(5).                       ST1174.2
015600           07 XFILE-NAME           PIC X(6).                      ST1174.2
015700           07 FILLER              PIC X(8).                       ST1174.2
015800           07 XRECORD-NAME         PIC X(6).                      ST1174.2
015900           07 FILLER              PIC X(1).                       ST1174.2
016000           07 REELUNIT-NUMBER     PIC 9(1).                       ST1174.2
016100           07 FILLER              PIC X(7).                       ST1174.2
016200           07 XRECORD-NUMBER       PIC 9(6).                      ST1174.2
016300           07 FILLER              PIC X(6).                       ST1174.2
016400           07 UPDATE-NUMBER       PIC 9(2).                       ST1174.2
016500           07 FILLER              PIC X(5).                       ST1174.2
016600           07 ODO-NUMBER          PIC 9(4).                       ST1174.2
016700           07 FILLER              PIC X(5).                       ST1174.2
016800           07 XPROGRAM-NAME        PIC X(5).                      ST1174.2
016900           07 FILLER              PIC X(7).                       ST1174.2
017000           07 XRECORD-LENGTH       PIC 9(6).                      ST1174.2
017100           07 FILLER              PIC X(7).                       ST1174.2
017200           07 CHARS-OR-RECORDS    PIC X(2).                       ST1174.2
017300           07 FILLER              PIC X(1).                       ST1174.2
017400           07 XBLOCK-SIZE          PIC 9(4).                      ST1174.2
017500           07 FILLER              PIC X(6).                       ST1174.2
017600           07 RECORDS-IN-FILE     PIC 9(6).                       ST1174.2
017700           07 FILLER              PIC X(5).                       ST1174.2
017800           07 XFILE-ORGANIZATION   PIC X(2).                      ST1174.2
017900           07 FILLER              PIC X(6).                       ST1174.2
018000           07 XLABEL-TYPE          PIC X(1).                      ST1174.2
018100        05 FILE-RECORD-INFO-P121-240.                             ST1174.2
018200           07 FILLER              PIC X(8).                       ST1174.2
018300           07 XRECORD-KEY          PIC X(29).                     ST1174.2
018400           07 FILLER              PIC X(9).                       ST1174.2
018500           07 ALTERNATE-KEY1      PIC X(29).                      ST1174.2
018600           07 FILLER              PIC X(9).                       ST1174.2
018700           07 ALTERNATE-KEY2      PIC X(29).                      ST1174.2
018800           07 FILLER              PIC X(7).                       ST1174.2
018900 01  TEST-RESULTS.                                                ST1174.2
019000     02 FILLER                   PIC X      VALUE SPACE.          ST1174.2
019100     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1174.2
019200     02 FILLER                   PIC X      VALUE SPACE.          ST1174.2
019300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1174.2
019400     02 FILLER                   PIC X      VALUE SPACE.          ST1174.2
019500     02  PAR-NAME.                                                ST1174.2
019600       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1174.2
019700       03  PARDOT-X              PIC X      VALUE SPACE.          ST1174.2
019800       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1174.2
019900     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1174.2
020000     02 RE-MARK                  PIC X(61).                       ST1174.2
020100 01  TEST-COMPUTED.                                               ST1174.2
020200     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1174.2
020300     02 FILLER                   PIC X(17)  VALUE                 ST1174.2
020400            "       COMPUTED=".                                   ST1174.2
020500     02 COMPUTED-X.                                               ST1174.2
020600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1174.2
020700     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1174.2
020800                                 PIC -9(9).9(9).                  ST1174.2
020900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1174.2
021000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1174.2
021100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1174.2
021200     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1174.2
021300         04 COMPUTED-18V0                    PIC -9(18).          ST1174.2
021400         04 FILLER                           PIC X.               ST1174.2
021500     03 FILLER PIC X(50) VALUE SPACE.                             ST1174.2
021600 01  TEST-CORRECT.                                                ST1174.2
021700     02 FILLER PIC X(30) VALUE SPACE.                             ST1174.2
021800     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1174.2
021900     02 CORRECT-X.                                                ST1174.2
022000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1174.2
022100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1174.2
022200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1174.2
022300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1174.2
022400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1174.2
022500     03      CR-18V0 REDEFINES CORRECT-A.                         ST1174.2
022600         04 CORRECT-18V0                     PIC -9(18).          ST1174.2
022700         04 FILLER                           PIC X.               ST1174.2
022800     03 FILLER PIC X(2) VALUE SPACE.                              ST1174.2
022900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1174.2
023000 01  CCVS-C-1.                                                    ST1174.2
023100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1174.2
023200-    "SS  PARAGRAPH-NAME                                          ST1174.2
023300-    "       REMARKS".                                            ST1174.2
023400     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1174.2
023500 01  CCVS-C-2.                                                    ST1174.2
023600     02 FILLER                     PIC X        VALUE SPACE.      ST1174.2
023700     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1174.2
023800     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1174.2
023900     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1174.2
024000     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1174.2
024100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1174.2
024200 01  REC-CT                        PIC 99       VALUE ZERO.       ST1174.2
024300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1174.2
024400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1174.2
024500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1174.2
024600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1174.2
024700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1174.2
024800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1174.2
024900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1174.2
025000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1174.2
025100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1174.2
025200 01  CCVS-H-1.                                                    ST1174.2
025300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1174.2
025400     02  FILLER                    PIC X(42)    VALUE             ST1174.2
025500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1174.2
025600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1174.2
025700 01  CCVS-H-2A.                                                   ST1174.2
025800   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1174.2
025900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1174.2
026000   02  FILLER                        PIC XXXX   VALUE             ST1174.2
026100     "4.2 ".                                                      ST1174.2
026200   02  FILLER                        PIC X(28)  VALUE             ST1174.2
026300            " COPY - NOT FOR DISTRIBUTION".                       ST1174.2
026400   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1174.2
026500                                                                  ST1174.2
026600 01  CCVS-H-2B.                                                   ST1174.2
026700   02  FILLER                        PIC X(15)  VALUE             ST1174.2
026800            "TEST RESULT OF ".                                    ST1174.2
026900   02  TEST-ID                       PIC X(9).                    ST1174.2
027000   02  FILLER                        PIC X(4)   VALUE             ST1174.2
027100            " IN ".                                               ST1174.2
027200   02  FILLER                        PIC X(12)  VALUE             ST1174.2
027300     " HIGH       ".                                              ST1174.2
027400   02  FILLER                        PIC X(22)  VALUE             ST1174.2
027500            " LEVEL VALIDATION FOR ".                             ST1174.2
027600   02  FILLER                        PIC X(58)  VALUE             ST1174.2
027700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1174.2
027800 01  CCVS-H-3.                                                    ST1174.2
027900     02  FILLER                      PIC X(34)  VALUE             ST1174.2
028000            " FOR OFFICIAL USE ONLY    ".                         ST1174.2
028100     02  FILLER                      PIC X(58)  VALUE             ST1174.2
028200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1174.2
028300     02  FILLER                      PIC X(28)  VALUE             ST1174.2
028400            "  COPYRIGHT   1985 ".                                ST1174.2
028500 01  CCVS-E-1.                                                    ST1174.2
028600     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1174.2
028700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1174.2
028800     02 ID-AGAIN                     PIC X(9).                    ST1174.2
028900     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1174.2
029000 01  CCVS-E-2.                                                    ST1174.2
029100     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1174.2
029200     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1174.2
029300     02 CCVS-E-2-2.                                               ST1174.2
029400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1174.2
029500         03 FILLER                   PIC X      VALUE SPACE.      ST1174.2
029600         03 ENDER-DESC               PIC X(44)  VALUE             ST1174.2
029700            "ERRORS ENCOUNTERED".                                 ST1174.2
029800 01  CCVS-E-3.                                                    ST1174.2
029900     02  FILLER                      PIC X(22)  VALUE             ST1174.2
030000            " FOR OFFICIAL USE ONLY".                             ST1174.2
030100     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1174.2
030200     02  FILLER                      PIC X(58)  VALUE             ST1174.2
030300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1174.2
030400     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1174.2
030500     02 FILLER                       PIC X(15)  VALUE             ST1174.2
030600             " COPYRIGHT 1985".                                   ST1174.2
030700 01  CCVS-E-4.                                                    ST1174.2
030800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1174.2
030900     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1174.2
031000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1174.2
031100     02 FILLER                       PIC X(40)  VALUE             ST1174.2
031200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1174.2
031300 01  XXINFO.                                                      ST1174.2
031400     02 FILLER                       PIC X(19)  VALUE             ST1174.2
031500            "*** INFORMATION ***".                                ST1174.2
031600     02 INFO-TEXT.                                                ST1174.2
031700       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1174.2
031800       04 XXCOMPUTED                 PIC X(20).                   ST1174.2
031900       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1174.2
032000       04 XXCORRECT                  PIC X(20).                   ST1174.2
032100     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1174.2
032200 01  HYPHEN-LINE.                                                 ST1174.2
032300     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1174.2
032400     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1174.2
032500-    "*****************************************".                 ST1174.2
032600     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1174.2
032700-    "******************************".                            ST1174.2
032800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1174.2
032900     "ST117A".                                                    ST1174.2
033000 PROCEDURE DIVISION.                                              ST1174.2
033100 CCVS1 SECTION.                                                   ST1174.2
033200 OPEN-FILES.                                                      ST1174.2
033300     OPEN    OUTPUT PRINT-FILE.                                   ST1174.2
033400     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1174.2
033500     MOVE    SPACE TO TEST-RESULTS.                               ST1174.2
033600     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1174.2
033700     MOVE    ZERO TO REC-SKL-SUB.                                 ST1174.2
033800     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1174.2
033900 CCVS-INIT-FILE.                                                  ST1174.2
034000     ADD     1 TO REC-SKL-SUB.                                    ST1174.2
034100     MOVE    FILE-RECORD-INFO-SKELETON                            ST1174.2
034200          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1174.2
034300 CCVS-INIT-EXIT.                                                  ST1174.2
034400     GO TO CCVS1-EXIT.                                            ST1174.2
034500 CLOSE-FILES.                                                     ST1174.2
034600     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1174.2
034700 TERMINATE-CCVS.                                                  ST1174.2
034800*    EXIT PROGRAM.                                                ST1174.2
034900*TERMINATE-CALL.                                                  ST1174.2
035000     STOP     RUN.                                                ST1174.2
035100 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1174.2
035200 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1174.2
035300 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1174.2
035400 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1174.2
035500     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1174.2
035600 PRINT-DETAIL.                                                    ST1174.2
035700     IF REC-CT NOT EQUAL TO ZERO                                  ST1174.2
035800             MOVE "." TO PARDOT-X                                 ST1174.2
035900             MOVE REC-CT TO DOTVALUE.                             ST1174.2
036000     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1174.2
036100     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1174.2
036200        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1174.2
036300          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1174.2
036400     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1174.2
036500     MOVE SPACE TO CORRECT-X.                                     ST1174.2
036600     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1174.2
036700     MOVE     SPACE TO RE-MARK.                                   ST1174.2
036800 HEAD-ROUTINE.                                                    ST1174.2
036900     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1174.2
037000     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1174.2
037100     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1174.2
037200     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1174.2
037300 COLUMN-NAMES-ROUTINE.                                            ST1174.2
037400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1174.2
037500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1174.2
037600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1174.2
037700 END-ROUTINE.                                                     ST1174.2
037800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1174.2
037900 END-RTN-EXIT.                                                    ST1174.2
038000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1174.2
038100 END-ROUTINE-1.                                                   ST1174.2
038200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1174.2
038300      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1174.2
038400      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1174.2
038500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1174.2
038600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1174.2
038700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1174.2
038800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1174.2
038900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1174.2
039000  END-ROUTINE-12.                                                 ST1174.2
039100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1174.2
039200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1174.2
039300         MOVE "NO " TO ERROR-TOTAL                                ST1174.2
039400         ELSE                                                     ST1174.2
039500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1174.2
039600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1174.2
039700     PERFORM WRITE-LINE.                                          ST1174.2
039800 END-ROUTINE-13.                                                  ST1174.2
039900     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1174.2
040000         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1174.2
040100         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1174.2
040200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1174.2
040300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1174.2
040400      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1174.2
040500          MOVE "NO " TO ERROR-TOTAL                               ST1174.2
040600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1174.2
040700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1174.2
040800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1174.2
040900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1174.2
041000 WRITE-LINE.                                                      ST1174.2
041100     ADD 1 TO RECORD-COUNT.                                       ST1174.2
041200*    IF RECORD-COUNT GREATER 42                                   ST1174.2
041300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1174.2
041400*        MOVE SPACE TO DUMMY-RECORD                               ST1174.2
041500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1174.2
041600*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1174.2
041700*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1174.2
041800*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1174.2
041900*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1174.2
042000*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1174.2
042100*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1174.2
042200*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1174.2
042300*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1174.2
042400*        MOVE ZERO TO RECORD-COUNT.                               ST1174.2
042500     PERFORM WRT-LN.                                              ST1174.2
042600 WRT-LN.                                                          ST1174.2
042700     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1174.2
042800     MOVE SPACE TO DUMMY-RECORD.                                  ST1174.2
042900 BLANK-LINE-PRINT.                                                ST1174.2
043000     PERFORM WRT-LN.                                              ST1174.2
043100 FAIL-ROUTINE.                                                    ST1174.2
043200     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1174.2
043300            GO TO   FAIL-ROUTINE-WRITE.                           ST1174.2
043400     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1174.2
043500     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1174.2
043600     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1174.2
043700     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1174.2
043800     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1174.2
043900     GO TO  FAIL-ROUTINE-EX.                                      ST1174.2
044000 FAIL-ROUTINE-WRITE.                                              ST1174.2
044100     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1174.2
044200     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1174.2
044300     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1174.2
044400     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1174.2
044500 FAIL-ROUTINE-EX. EXIT.                                           ST1174.2
044600 BAIL-OUT.                                                        ST1174.2
044700     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1174.2
044800     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1174.2
044900 BAIL-OUT-WRITE.                                                  ST1174.2
045000     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1174.2
045100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1174.2
045200     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1174.2
045300     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1174.2
045400 BAIL-OUT-EX. EXIT.                                               ST1174.2
045500 CCVS1-EXIT.                                                      ST1174.2
045600     EXIT.                                                        ST1174.2
045700 SECT-ST117-0001 SECTION.                                         ST1174.2
045800 SRT-INIT.                                                        ST1174.2
045900     OPEN INPUT SQ-FS2.                                           ST1174.2
046000     MOVE    "BIG-SORT" TO PAR-NAME.                              ST1174.2
046100     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1174.2
046200     MOVE    0 TO WRK-DU-9-0001.                                  ST1174.2
046300     MOVE    0 TO WRK-DU-9-2.                                     ST1174.2
046400     MOVE    0 TO WRK-DU-999-3.                                   ST1174.2
046500     DIVIDE                                                       ST1174.2
046600     XXXXX065                                                     ST1174.2
046700         BY 51 GIVING NUMBER-OF-SETS.                             ST1174.2
046800*                                                                 ST1174.2
046900*    FOR EVERY SET OF 51 RECORDS CREATED AS THE ORIGINAL INPUT    ST1174.2
047000*        FILE FOR THE SORT ( X-65 / 51 ), THEN THERE SHOULD BE AT ST1174.2
047100*        LEAST THAT NUMBER OF DUPLICATE ALPHANUMERIC KEYS AS THE  ST1174.2
047200*        FIRST N RECORDS IN THE SORTED FILE SQ-FS2.  THAT MANY    ST1174.2
047300*        RECORDS WILL BE READ AND THE KEYS SHOULD BE THE LOWEST   ST1174.2
047400*        CHARACTER IN THE NATIVE COLLATING SEQUENCE. THE NUMERIC  ST1174.2
047500*        KEYS SHOULD ALWAYS BE ASCENDING.                         ST1174.2
047600*                                                                 ST1174.2
047700     MOVE 1 TO WRK-DU-999-0001.                                   ST1174.2
047800 SRT-INIT-01.                                                     ST1174.2
047900     PERFORM RD-2 THRU R2-EXIT.                                   ST1174.2
048000     ADD 1 TO WRK-DU-999-0001.                                    ST1174.2
048100     IF  WRK-DU-999-0001 IS NOT GREATER THAN NUMBER-OF-SETS       ST1174.2
048200         GO TO SRT-INIT-01.                                       ST1174.2
048300     IF      WRK-DU-9-2 IS EQUAL TO 0                             ST1174.2
048400         PERFORM PASS                                             ST1174.2
048500         GO TO SRT-WRITE                                          ST1174.2
048600     ELSE                                                         ST1174.2
048700         PERFORM FAIL                                             ST1174.2
048800         MOVE "ERROR AT RECORD" TO COMPUTED-A                     ST1174.2
048900         MOVE WRK-DU-999-2 TO CORRECT-18V0                        ST1174.2
049000         MOVE "FILE SQ-FS2 PASSED FROM ST116" TO RE-MARK          ST1174.2
049100         GO TO SRT-WRITE.                                         ST1174.2
049200 SRT-DELETE.                                                      ST1174.2
049300     PERFORM DE-LETE.                                             ST1174.2
049400 SRT-WRITE.                                                       ST1174.2
049500     PERFORM PRINT-DETAIL.                                        ST1174.2
049600     CLOSE   SQ-FS2.                                              ST1174.2
049700     GO      TO ST117-END.                                        ST1174.2
049800 RD-2.                                                            ST1174.2
049900     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1174.2
050000         GO TO R2-EXIT.                                           ST1174.2
050100     READ SQ-FS2 AT END GO TO PREMATURE-EOF.                      ST1174.2
050200     IF      ALPHAN-KEY-K6 IS NOT EQUAL TO ANSWER (1)             ST1174.2
050300         MOVE 1 TO WRK-DU-9-2                                     ST1174.2
050400         MOVE WRK-DU-999-0001 TO WRK-DU-999-2                     ST1174.2
050500         MOVE 1 TO WRK-DU-9-0001.                                 ST1174.2
050600     IF      NUM-KEY-K6 IS NOT GREATER THAN WRK-DU-999-3          ST1174.2
050700         MOVE 1 TO WRK-DU-9-2                                     ST1174.2
050800         MOVE WRK-DU-999-0001 TO WRK-DU-999-2                     ST1174.2
050900         MOVE 1 TO WRK-DU-9-0001                                  ST1174.2
051000     ELSE                                                         ST1174.2
051100         MOVE NUM-KEY-K6 TO WRK-DU-999-3                          ST1174.2
051200         GO TO R2-EXIT.                                           ST1174.2
051300 PREMATURE-EOF.                                                   ST1174.2
051400     MOVE 1 TO WRK-DU-9-0001.                                     ST1174.2
051500     PERFORM FAIL.                                                ST1174.2
051600     MOVE    "AT RECORD" TO COMPUTED-A.                           ST1174.2
051700     MOVE    WRK-DU-999-0001 TO CORRECT-18V0.                     ST1174.2
051800     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1174.2
051900 R2-EXIT.                                                         ST1174.2
052000     EXIT.                                                        ST1174.2
052100 ST117-END.                                                       ST1174.2
052200     EXIT.                                                        ST1174.2
052300*FILEDUMP SECTION.                                                ST1174.2
052400*FILE-2-DUMP-INIT.                                                ST1174.2
052500*    OPEN INPUT SQ-FS2.                                           ST1174.2
052600*    MOVE ZERO TO COUNT-OF-RECS.                                  ST1174.2
052700*FILE-2-DUMP.                                                     ST1174.2
052800*    READ SQ-FS2 RECORD                                           ST1174.2
052900*        AT END GO TO FILE-2-DUMP-END.                            ST1174.2
053000*    ADD 1 TO COUNT-OF-RECS.                                      ST1174.2
053100*    IF COUNT-OF-RECS GREATER THAN                                ST1174.2
053200*    XXXXX065                                                     ST1174.2
053300*        MOVE " TOO MANY RECORDS" TO DUMMY-RECORD                 ST1174.2
053400*        WRITE DUMMY-RECORD AFTER ADVANCING 1 LINES               ST1174.2
053500*        GO TO FILE-2-DUMP-END.                                   ST1174.2
053600*    PERFORM FILE-2-DUMP-WRITE.                                   ST1174.2
053700*    GO TO FILE-2-DUMP.                                           ST1174.2
053800*FILE-2-DUMP-WRITE.                                               ST1174.2
053900*    MOVE SQ-FS2R1-F-G-507 TO DUMMY-RECORD.                       ST1174.2
054000*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1174.2
054100*FILE-2-DUMP-END.                                                 ST1174.2
054200*    MOVE    " NUMBER OF SORTED RECORDS ON SQ-FS2 SHOWN BELOW"    ST1174.2
054300*        TO DUMMY-RECORD.                                         ST1174.2
054400*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1174.2
054500*    MOVE    COUNT-OF-RECS TO DUMMY-RECORD.                       ST1174.2
054600*    WRITE   DUMMY-RECORD AFTER ADVANCING 1 LINES.                ST1174.2
054700*    CLOSE   SQ-FS2.                                              ST1174.2
054800 CCVS-EXIT SECTION.                                               ST1174.2
054900 CCVS-999999.                                                     ST1174.2
055000     GO TO CLOSE-FILES.                                           ST1174.2
      *END-OF,ST117A                                                                  
