      *HEADER,COBOL,ST137A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1374.2
000200 PROGRAM-ID.                                                      ST1374.2
000300     ST137A.                                                      ST1374.2
000400****************************************************************  ST1374.2
000500*                                                              *  ST1374.2
000600*    VALIDATION FOR:-                                          *  ST1374.2
000700*                                                              *  ST1374.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1374.2
000900*                                                              *  ST1374.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1374.2
001100*                                                              *  ST1374.2
001200****************************************************************  ST1374.2
001300*    OBJECTIVE -                                                  ST1374.2
001400*        ROUTINE ST207 IS A TEST OF THE SORT STATEMENT USING      ST1374.2
001500*    VARIABLE LENGTH RECORDS WHICH CONTAIN ODO (OCCURS DEPENDING  ST1374.2
001600*    ON) CLAUSES IN THEIR RECORD DESCRIPTIONS.                    ST1374.2
001700*                                                                 ST1374.2
001800*                                                                 ST1374.2
001900*    FEATURES TESTED -                                            ST1374.2
002000*    *   COLLATING SEQUENCE IS NATIVE.  NO COLLATING SEQUENCE     ST1374.2
002100*            STATEMENT IS USED IN THE ACTUAL SORT STATEMENT.      ST1374.2
002200*    *   VARIABLE LENGTH RECORDS                                  ST1374.2
002300*    *   OCCURS DEPENDING ON CLAUSES                              ST1374.2
002400*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1374.2
002500*                                                                 ST1374.2
002600*    *   SORT  SORT-FILE-NAME                                     ST1374.2
002700*        ON ASCENDING KEY KEY-1 OF DATA-NAME-1                    ST1374.2
002800*        ASCENDING KEY-2 OF DATA-NAME-2                           ST1374.2
002900*        USING FILE-NAME-1                                        ST1374.2
003000*        GIVING FILE-NAME-2.                                      ST1374.2
003100*                                                                 ST1374.2
003200*                                                                 ST1374.2
003300*    ANSI X3.23-1974 REFERENCES -                                 ST1374.2
003400*    *   SECTION 2.1  OCCURS DEPENDING ON  PAGE III-2             ST1374.2
003500*    *   SECTION 4.4  THE SORT STATEMENT  PAGE VII-14             ST1374.2
003600*                                                                 ST1374.2
003700*                                                                 ST1374.2
003800*    FILES USED -                                                 ST1374.2
003900*    *   FILES SQ-FS1 AND SQ-FS2 ON MAGNETIC TAPE OR MASS-STORAGE ST1374.2
004000*    ARE FIRST CREATED BY ROUTINE ST207.  THE FIRST FILE (SQ-FS1) ST1374.2
004100*    IS THEN SORTED GIVING THE SECOND FILE (SQ-FS2).              ST1374.2
004200*                                                                 ST1374.2
004300*        SQ-FS1 -                                                 ST1374.2
004400*    51 RECORDS                                                   ST1374.2
004500*    VARIABLE LENGTH RECORDS (148 TO 1435 CHARACTERS) USING ODO   ST1374.2
004600*    BLOCKED 1                                                    ST1374.2
004700*    RESERVE 2 AREAS                                              ST1374.2
004800*                                                                 ST1374.2
004900*        SQ-FS2 -                                                 ST1374.2
005000*    51 RECORDS                                                   ST1374.2
005100*    VARIABLE LENGTH RECORDS FORMAT WITH ODO BUT ACTUALLY ALL     ST1374.2
005200*        RECORDS ARE FIXED LENGTH 148 CHARACTERS.                 ST1374.2
005300*    BLOCKED 2                                                    ST1374.2
005400*    RESERVE 4 AREAS                                              ST1374.2
005500*                                                                 ST1374.2
005600*        NOTE THAT SQ-FS2 IS OVERWRITTEN AS A RESULT OF THE SORT  ST1374.2
005700*    AND SHOULD CONTAIN A FINAL TOTAL OF 51 RECORDS.              ST1374.2
005800*                                                                 ST1374.2
005900*                                                                 ST1374.2
006000*    X-CARDS USED  -                                              ST1374.2
006100*    X-XXX014     SQ-FS1                                          ST1374.2
006200*    X-XXX015     SQ-FS2                                          ST1374.2
006300*    X-XXX027     SORT FILE ST-FS1                                ST1374.2
006400*    X-XXX063     NATIVE COLLATING SEQUENCE ASCENDING ORDER (NOTE ST1374.2
006500*        THAT THE QUOTE CHARACTER IS NOT TO APPEAR IN THE X-63    ST1374.2
006600*        CARD AND THE DOLLAR SIGN $ IS TO APPEAR TWICE WHEREVER   ST1374.2
006700*        THE $ BELONGS IN THE NATIVE COLLATING SEQUENCE).  IF     ST1374.2
006800*        THE NATIVE COLLATING SEQUENCE IS ACTUALLY THE ASCII      ST1374.2
006900*        COLLATING SEQUENCE SEE BELOW FOR A SAMPLE X-63 CARD..... ST1374.2
007000*                                                                 ST1374.2
007100*    X-63  " $$()*+,-./0123456789;<=>ABCDEFGHIJKLMNOPQRSTUVWXYZ". ST1374.2
007200*                                                                 ST1374.2
007300*                                                                 ST1374.2
007400*    OPTIONS RECOMMENDED -                                        ST1374.2
007500*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1374.2
007600*                  FILES AS THEY ARE CREATED AND READ DURING      ST1374.2
007700*                  TESTS 3 THRU 6.                                ST1374.2
007800*                                                                 ST1374.2
007900*                                                                 ST1374.2
008000*    TEST DESCRIPTIONS -                                          ST1374.2
008100*    SRT-TEST-001     CHECKS THE CREATION OF SQ-FS1               ST1374.2
008200*    SRT-TEST-002     CHECKS THE CREATION OF SQ-FS2               ST1374.2
008300*    SRT-TEST-003     TESTS RECORDS 1-20 ON SORTED SQ-FS2         ST1374.2
008400*    SRT-TEST-004     TESTS RECORDS 21-40 ON SORTED SQ-FS2        ST1374.2
008500*    SRT-TEST-005     TESTS RECORDS 41-51 ON SORTED SQ-FS2        ST1374.2
008600*    SRT-TEST-006     AN EOF CHECK ON SQ-FS2                      ST1374.2
008700*                                                                 ST1374.2
008800*                                                                 ST1374.2
008900*    ************************************************************ ST1374.2
009000 ENVIRONMENT DIVISION.                                            ST1374.2
009100 CONFIGURATION SECTION.                                           ST1374.2
009200 SOURCE-COMPUTER.                                                 ST1374.2
009300     XXXXX082.                                                    ST1374.2
009400 OBJECT-COMPUTER.                                                 ST1374.2
009500     XXXXX083.                                                    ST1374.2
009600 INPUT-OUTPUT SECTION.                                            ST1374.2
009700 FILE-CONTROL.                                                    ST1374.2
009800     SELECT PRINT-FILE ASSIGN TO                                  ST1374.2
009900     XXXXX055.                                                    ST1374.2
010000     SELECT SQ-FS1 ASSIGN TO                                      ST1374.2
010100     XXXXX014                                                     ST1374.2
010200     ORGANIZATION IS SEQUENTIAL                                   ST1374.2
010300     ACCESS MODE IS SEQUENTIAL                                    ST1374.2
010400     RESERVE 2 AREAS.                                             ST1374.2
010500     SELECT SQ-FS2 ASSIGN TO                                      ST1374.2
010600     XXXXX015                                                     ST1374.2
010700     ORGANIZATION IS SEQUENTIAL                                   ST1374.2
010800     ACCESS MODE IS SEQUENTIAL                                    ST1374.2
010900     RESERVE 4 AREAS.                                             ST1374.2
011000     SELECT ST-FS1 ASSIGN TO                                      ST1374.2
011100     XXXXX027.                                                    ST1374.2
011200 DATA DIVISION.                                                   ST1374.2
011300 FILE SECTION.                                                    ST1374.2
011400 FD  PRINT-FILE.                                                  ST1374.2
011500 01  PRINT-REC PICTURE X(120).                                    ST1374.2
011600 01  DUMMY-RECORD PICTURE X(120).                                 ST1374.2
011700 FD  SQ-FS1                                                       ST1374.2
011800     LABEL RECORDS STANDARD                                       ST1374.2
011900C    VALUE OF                                                     ST1374.2
012000C    XXXXX074                                                     ST1374.2
012100C    IS                                                           ST1374.2
012200C    XXXXX075                                                     ST1374.2
012300*    XXXXX069                                                     ST1374.2
012400     BLOCK CONTAINS 1 RECORDS                                     ST1374.2
012500     RECORD CONTAINS 148 TO 1435 CHARACTERS.                      ST1374.2
012600 01  SQ-FS1R1-F-G-132.                                            ST1374.2
012700     10 REC-PREAMBLE PIC X(120).                                  ST1374.2
012800     10 REST-OF-1.                                                ST1374.2
012900     20  LENGTH-1 PIC 999.                                        ST1374.2
013000     20 KEY-1.                                                    ST1374.2
013100         30 ALPHAN-KEY PIC X.                                     ST1374.2
013200         30 NUM-KEY PIC 999.                                      ST1374.2
013300     20 KEY-2.                                                    ST1374.2
013400         30 ALPHAN-KEY PIC X.                                     ST1374.2
013500         30 NUM-KEY PIC 999.                                      ST1374.2
013600     20 KEY-3.                                                    ST1374.2
013700         30 ALPHAN-KEY PIC X.                                     ST1374.2
013800         30 NUM-KEY PIC 999.                                      ST1374.2
013900     20  STUFF-1 OCCURS 1 TO 100 TIMES DEPENDING ON LENGTH-1.     ST1374.2
014000         30 FILLER PIC X(13).                                     ST1374.2
014100 FD  SQ-FS2                                                       ST1374.2
014200     LABEL RECORDS STANDARD                                       ST1374.2
014300C    VALUE OF                                                     ST1374.2
014400C    XXXXX074                                                     ST1374.2
014500C    IS                                                           ST1374.2
014600C    XXXXX076                                                     ST1374.2
014700*    XXXXX069                                                     ST1374.2
014800     BLOCK CONTAINS 2 RECORDS                                     ST1374.2
014900     RECORD CONTAINS 148 TO 1435 CHARACTERS                       ST1374.2
015000     DATA RECORD SQ-FS2R1-F-G-132.                                ST1374.2
015100 01  SQ-FS2R1-F-G-132.                                            ST1374.2
015200     10 REC-PRE-2 PIC X(120).                                     ST1374.2
015300     10  REST-OF-2.                                               ST1374.2
015400     20  LENGTH-2 PIC 999.                                        ST1374.2
015500     20 KEY-4.                                                    ST1374.2
015600         30 ALPHAN-KEY PIC X.                                     ST1374.2
015700         30 NUM-KEY PIC 999.                                      ST1374.2
015800     20 KEY-5.                                                    ST1374.2
015900         30 ALPHAN-KEY PIC X.                                     ST1374.2
016000         30 NUM-KEY PIC 999.                                      ST1374.2
016100     20 KEY-6.                                                    ST1374.2
016200         30 ALPHAN-KEY PIC X.                                     ST1374.2
016300         30 NUM-KEY PIC 999.                                      ST1374.2
016400     20 STUFF-2 OCCURS 1 TO 100 TIMES DEPENDING ON LENGTH-100.    ST1374.2
016500         30 FILLER PIC X(13).                                     ST1374.2
016600 SD  ST-FS1                                                       ST1374.2
016700     RECORD CONTAINS 148 TO 1435 CHARACTERS                       ST1374.2
016800     DATA RECORD IS ST-FS1R1-F-G-132.                             ST1374.2
016900 01  ST-FS1R1-F-G-132.                                            ST1374.2
017000     02 FILLER PIC X(120).                                        ST1374.2
017100     02 LENGTH-3 PIC 999.                                         ST1374.2
017200     02 NON-KEY-1.                                                ST1374.2
017300         03 A-KEY PIC X.                                          ST1374.2
017400         03 N-KEY PIC 999.                                        ST1374.2
017500     02 SORT-KEY.                                                 ST1374.2
017600         03 A-KEY PIC X.                                          ST1374.2
017700         03 N-KEY PIC 999.                                        ST1374.2
017800     02 NON-KEY-2.                                                ST1374.2
017900         03 A-KEY PIC X.                                          ST1374.2
018000         03 N-KEY PIC 999.                                        ST1374.2
018100     02 STUFF-3 OCCURS 1 TO 100 TIMES DEPENDING ON LENGTH-100.    ST1374.2
018200         03 FILLER PIC X(13).                                     ST1374.2
018300 WORKING-STORAGE SECTION.                                         ST1374.2
018400 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1374.2
018500 77  WRK-DU-999-0001 PIC 999.                                     ST1374.2
018600 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1374.2
018700 77  WRK-DU-999-0002 PIC 999 VALUE 0.                             ST1374.2
018800 77  LENGTH-100 PIC 999 VALUE 100.                                ST1374.2
018900 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1374.2
019000     "/A.Z-B,Y+C*X)D(W$E$V F0U1G2T3H4S5I6R7J8Q9K;P<L=O>MN".       ST1374.2
019100 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1374.2
019200     02 CHAR PIC X OCCURS 51 TIMES.                               ST1374.2
019300 01  WRK-XN-2    PIC X(51) VALUE                                  ST1374.2
019400     XXXXX063.                                                    ST1374.2
019500 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1374.2
019600     02 ASCIIS PIC X OCCURS 51 TIMES.                             ST1374.2
019700 01  WRK-XN-O020F-0001.                                           ST1374.2
019800     02 COMPU PIC X OCCURS 20 TIMES.                              ST1374.2
019900 01  WRK-XN-X-0001 REDEFINES WRK-XN-O020F-0001.                   ST1374.2
020000     02 FILLER PIC X(20).                                         ST1374.2
020100 01  WRK-XN-O120F-1.                                              ST1374.2
020200     02 COLLS PIC X OCCURS 120 TIMES.                             ST1374.2
020300 01  WRK-XN-X-2 REDEFINES WRK-XN-O120F-1.                         ST1374.2
020400     02 WRK-XN-0002 PIC X(20).                                    ST1374.2
020500     02 WRK-XN-0003 PIC X(20).                                    ST1374.2
020600     02 WRK-XN-0004 PIC X(20).                                    ST1374.2
020700     02 WRK-XN-0005 PIC X(20).                                    ST1374.2
020800     02 WRK-XN-0006 PIC X(20).                                    ST1374.2
020900     02 WRK-XN-0007 PIC X(20).                                    ST1374.2
021000 01  FILE-RECORD-INFORMATION-REC.                                 ST1374.2
021100     03 FILE-RECORD-INFO-SKELETON.                                ST1374.2
021200        05 FILLER                 PICTURE X(48)       VALUE       ST1374.2
021300             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1374.2
021400        05 FILLER                 PICTURE X(46)       VALUE       ST1374.2
021500             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1374.2
021600        05 FILLER                 PICTURE X(26)       VALUE       ST1374.2
021700             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1374.2
021800        05 FILLER                 PICTURE X(37)       VALUE       ST1374.2
021900             ",RECKEY=                             ".             ST1374.2
022000        05 FILLER                 PICTURE X(38)       VALUE       ST1374.2
022100             ",ALTKEY1=                             ".            ST1374.2
022200        05 FILLER                 PICTURE X(38)       VALUE       ST1374.2
022300             ",ALTKEY2=                             ".            ST1374.2
022400        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1374.2
022500     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1374.2
022600        05 FILE-RECORD-INFO-P1-120.                               ST1374.2
022700           07 FILLER              PIC X(5).                       ST1374.2
022800           07 XFILE-NAME           PIC X(6).                      ST1374.2
022900           07 FILLER              PIC X(8).                       ST1374.2
023000           07 XRECORD-NAME         PIC X(6).                      ST1374.2
023100           07 FILLER              PIC X(1).                       ST1374.2
023200           07 REELUNIT-NUMBER     PIC 9(1).                       ST1374.2
023300           07 FILLER              PIC X(7).                       ST1374.2
023400           07 XRECORD-NUMBER       PIC 9(6).                      ST1374.2
023500           07 FILLER              PIC X(6).                       ST1374.2
023600           07 UPDATE-NUMBER       PIC 9(2).                       ST1374.2
023700           07 FILLER              PIC X(5).                       ST1374.2
023800           07 ODO-NUMBER          PIC 9(4).                       ST1374.2
023900           07 FILLER              PIC X(5).                       ST1374.2
024000           07 XPROGRAM-NAME        PIC X(5).                      ST1374.2
024100           07 FILLER              PIC X(7).                       ST1374.2
024200           07 XRECORD-LENGTH       PIC 9(6).                      ST1374.2
024300           07 FILLER              PIC X(7).                       ST1374.2
024400           07 CHARS-OR-RECORDS    PIC X(2).                       ST1374.2
024500           07 FILLER              PIC X(1).                       ST1374.2
024600           07 XBLOCK-SIZE          PIC 9(4).                      ST1374.2
024700           07 FILLER              PIC X(6).                       ST1374.2
024800           07 RECORDS-IN-FILE     PIC 9(6).                       ST1374.2
024900           07 FILLER              PIC X(5).                       ST1374.2
025000           07 XFILE-ORGANIZATION   PIC X(2).                      ST1374.2
025100           07 FILLER              PIC X(6).                       ST1374.2
025200           07 XLABEL-TYPE          PIC X(1).                      ST1374.2
025300        05 FILE-RECORD-INFO-P121-240.                             ST1374.2
025400           07 FILLER              PIC X(8).                       ST1374.2
025500           07 XRECORD-KEY          PIC X(29).                     ST1374.2
025600           07 FILLER              PIC X(9).                       ST1374.2
025700           07 ALTERNATE-KEY1      PIC X(29).                      ST1374.2
025800           07 FILLER              PIC X(9).                       ST1374.2
025900           07 ALTERNATE-KEY2      PIC X(29).                      ST1374.2
026000           07 FILLER              PIC X(7).                       ST1374.2
026100 01  TEST-RESULTS.                                                ST1374.2
026200     02 FILLER                   PIC X      VALUE SPACE.          ST1374.2
026300     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1374.2
026400     02 FILLER                   PIC X      VALUE SPACE.          ST1374.2
026500     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1374.2
026600     02 FILLER                   PIC X      VALUE SPACE.          ST1374.2
026700     02  PAR-NAME.                                                ST1374.2
026800       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1374.2
026900       03  PARDOT-X              PIC X      VALUE SPACE.          ST1374.2
027000       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1374.2
027100     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1374.2
027200     02 RE-MARK                  PIC X(61).                       ST1374.2
027300 01  TEST-COMPUTED.                                               ST1374.2
027400     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1374.2
027500     02 FILLER                   PIC X(17)  VALUE                 ST1374.2
027600            "       COMPUTED=".                                   ST1374.2
027700     02 COMPUTED-X.                                               ST1374.2
027800     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1374.2
027900     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1374.2
028000                                 PIC -9(9).9(9).                  ST1374.2
028100     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1374.2
028200     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1374.2
028300     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1374.2
028400     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1374.2
028500         04 COMPUTED-18V0                    PIC -9(18).          ST1374.2
028600         04 FILLER                           PIC X.               ST1374.2
028700     03 FILLER PIC X(50) VALUE SPACE.                             ST1374.2
028800 01  TEST-CORRECT.                                                ST1374.2
028900     02 FILLER PIC X(30) VALUE SPACE.                             ST1374.2
029000     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1374.2
029100     02 CORRECT-X.                                                ST1374.2
029200     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1374.2
029300     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1374.2
029400     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1374.2
029500     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1374.2
029600     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1374.2
029700     03      CR-18V0 REDEFINES CORRECT-A.                         ST1374.2
029800         04 CORRECT-18V0                     PIC -9(18).          ST1374.2
029900         04 FILLER                           PIC X.               ST1374.2
030000     03 FILLER PIC X(2) VALUE SPACE.                              ST1374.2
030100     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1374.2
030200 01  CCVS-C-1.                                                    ST1374.2
030300     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1374.2
030400-    "SS  PARAGRAPH-NAME                                          ST1374.2
030500-    "       REMARKS".                                            ST1374.2
030600     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1374.2
030700 01  CCVS-C-2.                                                    ST1374.2
030800     02 FILLER                     PIC X        VALUE SPACE.      ST1374.2
030900     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1374.2
031000     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1374.2
031100     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1374.2
031200     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1374.2
031300 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1374.2
031400 01  REC-CT                        PIC 99       VALUE ZERO.       ST1374.2
031500 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1374.2
031600 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1374.2
031700 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1374.2
031800 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1374.2
031900 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1374.2
032000 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1374.2
032100 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1374.2
032200 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1374.2
032300 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1374.2
032400 01  CCVS-H-1.                                                    ST1374.2
032500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1374.2
032600     02  FILLER                    PIC X(42)    VALUE             ST1374.2
032700     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1374.2
032800     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1374.2
032900 01  CCVS-H-2A.                                                   ST1374.2
033000   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1374.2
033100   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1374.2
033200   02  FILLER                        PIC XXXX   VALUE             ST1374.2
033300     "4.2 ".                                                      ST1374.2
033400   02  FILLER                        PIC X(28)  VALUE             ST1374.2
033500            " COPY - NOT FOR DISTRIBUTION".                       ST1374.2
033600   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1374.2
033700                                                                  ST1374.2
033800 01  CCVS-H-2B.                                                   ST1374.2
033900   02  FILLER                        PIC X(15)  VALUE             ST1374.2
034000            "TEST RESULT OF ".                                    ST1374.2
034100   02  TEST-ID                       PIC X(9).                    ST1374.2
034200   02  FILLER                        PIC X(4)   VALUE             ST1374.2
034300            " IN ".                                               ST1374.2
034400   02  FILLER                        PIC X(12)  VALUE             ST1374.2
034500     " HIGH       ".                                              ST1374.2
034600   02  FILLER                        PIC X(22)  VALUE             ST1374.2
034700            " LEVEL VALIDATION FOR ".                             ST1374.2
034800   02  FILLER                        PIC X(58)  VALUE             ST1374.2
034900     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1374.2
035000 01  CCVS-H-3.                                                    ST1374.2
035100     02  FILLER                      PIC X(34)  VALUE             ST1374.2
035200            " FOR OFFICIAL USE ONLY    ".                         ST1374.2
035300     02  FILLER                      PIC X(58)  VALUE             ST1374.2
035400     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1374.2
035500     02  FILLER                      PIC X(28)  VALUE             ST1374.2
035600            "  COPYRIGHT   1985 ".                                ST1374.2
035700 01  CCVS-E-1.                                                    ST1374.2
035800     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1374.2
035900     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1374.2
036000     02 ID-AGAIN                     PIC X(9).                    ST1374.2
036100     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1374.2
036200 01  CCVS-E-2.                                                    ST1374.2
036300     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1374.2
036400     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1374.2
036500     02 CCVS-E-2-2.                                               ST1374.2
036600         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1374.2
036700         03 FILLER                   PIC X      VALUE SPACE.      ST1374.2
036800         03 ENDER-DESC               PIC X(44)  VALUE             ST1374.2
036900            "ERRORS ENCOUNTERED".                                 ST1374.2
037000 01  CCVS-E-3.                                                    ST1374.2
037100     02  FILLER                      PIC X(22)  VALUE             ST1374.2
037200            " FOR OFFICIAL USE ONLY".                             ST1374.2
037300     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1374.2
037400     02  FILLER                      PIC X(58)  VALUE             ST1374.2
037500     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1374.2
037600     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1374.2
037700     02 FILLER                       PIC X(15)  VALUE             ST1374.2
037800             " COPYRIGHT 1985".                                   ST1374.2
037900 01  CCVS-E-4.                                                    ST1374.2
038000     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1374.2
038100     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1374.2
038200     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1374.2
038300     02 FILLER                       PIC X(40)  VALUE             ST1374.2
038400      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1374.2
038500 01  XXINFO.                                                      ST1374.2
038600     02 FILLER                       PIC X(19)  VALUE             ST1374.2
038700            "*** INFORMATION ***".                                ST1374.2
038800     02 INFO-TEXT.                                                ST1374.2
038900       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1374.2
039000       04 XXCOMPUTED                 PIC X(20).                   ST1374.2
039100       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1374.2
039200       04 XXCORRECT                  PIC X(20).                   ST1374.2
039300     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1374.2
039400 01  HYPHEN-LINE.                                                 ST1374.2
039500     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1374.2
039600     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1374.2
039700-    "*****************************************".                 ST1374.2
039800     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1374.2
039900-    "******************************".                            ST1374.2
040000 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1374.2
040100     "ST137A".                                                    ST1374.2
040200 PROCEDURE DIVISION.                                              ST1374.2
040300 DECLARATIVES.                                                    ST1374.2
040400 SECT-ST216-DEC SECTION.                                          ST1374.2
040500     USE AFTER STANDARD ERROR PROCEDURE ON OUTPUT.                ST1374.2
040600 SRT-WRITE-DEC.                                                   ST1374.2
040700     MOVE "ERROR ON OUTPUT DECL." TO FEATURE.                     ST1374.2
040800     MOVE "SRT-TEST-DEC" TO PAR-NAME.                             ST1374.2
040900     WRITE   PRINT-REC FROM TEST-RESULTS AFTER ADVANCING 2 LINES. ST1374.2
041000     STOP RUN.                                                    ST1374.2
041100 END DECLARATIVES.                                                ST1374.2
041200 CCVS1 SECTION.                                                   ST1374.2
041300 OPEN-FILES.                                                      ST1374.2
041400     OPEN    OUTPUT PRINT-FILE.                                   ST1374.2
041500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1374.2
041600     MOVE    SPACE TO TEST-RESULTS.                               ST1374.2
041700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1374.2
041800     MOVE    ZERO TO REC-SKL-SUB.                                 ST1374.2
041900     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1374.2
042000 CCVS-INIT-FILE.                                                  ST1374.2
042100     ADD     1 TO REC-SKL-SUB.                                    ST1374.2
042200     MOVE    FILE-RECORD-INFO-SKELETON                            ST1374.2
042300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1374.2
042400 CCVS-INIT-EXIT.                                                  ST1374.2
042500     GO TO CCVS1-EXIT.                                            ST1374.2
042600 CLOSE-FILES.                                                     ST1374.2
042700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1374.2
042800 TERMINATE-CCVS.                                                  ST1374.2
042900*    EXIT PROGRAM.                                                ST1374.2
043000*TERMINATE-CALL.                                                  ST1374.2
043100     STOP     RUN.                                                ST1374.2
043200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1374.2
043300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1374.2
043400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1374.2
043500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1374.2
043600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1374.2
043700 PRINT-DETAIL.                                                    ST1374.2
043800     IF REC-CT NOT EQUAL TO ZERO                                  ST1374.2
043900             MOVE "." TO PARDOT-X                                 ST1374.2
044000             MOVE REC-CT TO DOTVALUE.                             ST1374.2
044100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1374.2
044200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1374.2
044300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1374.2
044400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1374.2
044500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1374.2
044600     MOVE SPACE TO CORRECT-X.                                     ST1374.2
044700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1374.2
044800     MOVE     SPACE TO RE-MARK.                                   ST1374.2
044900 HEAD-ROUTINE.                                                    ST1374.2
045000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1374.2
045100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1374.2
045200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1374.2
045300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1374.2
045400 COLUMN-NAMES-ROUTINE.                                            ST1374.2
045500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1374.2
045600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1374.2
045700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1374.2
045800 END-ROUTINE.                                                     ST1374.2
045900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1374.2
046000 END-RTN-EXIT.                                                    ST1374.2
046100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1374.2
046200 END-ROUTINE-1.                                                   ST1374.2
046300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1374.2
046400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1374.2
046500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1374.2
046600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1374.2
046700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1374.2
046800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1374.2
046900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1374.2
047000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1374.2
047100  END-ROUTINE-12.                                                 ST1374.2
047200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1374.2
047300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1374.2
047400         MOVE "NO " TO ERROR-TOTAL                                ST1374.2
047500         ELSE                                                     ST1374.2
047600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1374.2
047700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1374.2
047800     PERFORM WRITE-LINE.                                          ST1374.2
047900 END-ROUTINE-13.                                                  ST1374.2
048000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1374.2
048100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1374.2
048200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1374.2
048300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1374.2
048400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1374.2
048500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1374.2
048600          MOVE "NO " TO ERROR-TOTAL                               ST1374.2
048700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1374.2
048800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1374.2
048900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1374.2
049000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1374.2
049100 WRITE-LINE.                                                      ST1374.2
049200     ADD 1 TO RECORD-COUNT.                                       ST1374.2
049300*    IF RECORD-COUNT GREATER 42                                   ST1374.2
049400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1374.2
049500*        MOVE SPACE TO DUMMY-RECORD                               ST1374.2
049600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1374.2
049700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1374.2
049800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1374.2
049900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1374.2
050000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1374.2
050100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1374.2
050200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1374.2
050300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1374.2
050400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1374.2
050500*        MOVE ZERO TO RECORD-COUNT.                               ST1374.2
050600     PERFORM WRT-LN.                                              ST1374.2
050700 WRT-LN.                                                          ST1374.2
050800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1374.2
050900     MOVE SPACE TO DUMMY-RECORD.                                  ST1374.2
051000 BLANK-LINE-PRINT.                                                ST1374.2
051100     PERFORM WRT-LN.                                              ST1374.2
051200 FAIL-ROUTINE.                                                    ST1374.2
051300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1374.2
051400            GO TO   FAIL-ROUTINE-WRITE.                           ST1374.2
051500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1374.2
051600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1374.2
051700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1374.2
051800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1374.2
051900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1374.2
052000     GO TO  FAIL-ROUTINE-EX.                                      ST1374.2
052100 FAIL-ROUTINE-WRITE.                                              ST1374.2
052200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1374.2
052300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1374.2
052400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1374.2
052500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1374.2
052600 FAIL-ROUTINE-EX. EXIT.                                           ST1374.2
052700 BAIL-OUT.                                                        ST1374.2
052800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1374.2
052900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1374.2
053000 BAIL-OUT-WRITE.                                                  ST1374.2
053100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1374.2
053200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1374.2
053300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1374.2
053400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1374.2
053500 BAIL-OUT-EX. EXIT.                                               ST1374.2
053600 CCVS1-EXIT.                                                      ST1374.2
053700     EXIT.                                                        ST1374.2
053800 SECT-ST216-0001 SECTION.                                         ST1374.2
053900 SRT-INIT-001.                                                    ST1374.2
054000     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1374.2
054100     OPEN OUTPUT SQ-FS1.                                          ST1374.2
054200     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1374.2
054300     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1374.2
054400     MOVE ".XXX." TO XPROGRAM-NAME (1).                           ST1374.2
054500     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1374.2
054600     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1374.2
054700     MOVE 000051 TO RECORDS-IN-FILE (1).                          ST1374.2
054800     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1374.2
054900     MOVE "S" TO XLABEL-TYPE (1).                                 ST1374.2
055000     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1374.2
055100             MOVE SPACES TO WRK-XN-O120F-1.                       ST1374.2
055200 SRT-TEST-001.                                                    ST1374.2
055300     PERFORM SRT-TEST-001-BUILD VARYING WRK-DU-999-0001           ST1374.2
055400         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1374.2
055500*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
055600*    WRITE   PRINT-REC.                                           ST1374.2
055700     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1374.2
055800         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1374.2
055900         ELSE                                                     ST1374.2
056000         PERFORM PASS.                                            ST1374.2
056100     GO TO SRT-WRITE-001.                                         ST1374.2
056200 SRT-TEST-001-BUILD.                                              ST1374.2
056300     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-1           ST1374.2
056400         ALPHAN-KEY OF KEY-2 ALPHAN-KEY OF KEY-3.                 ST1374.2
056500     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1374.2
056600         NUM-KEY OF KEY-3.                                        ST1374.2
056700     MULTIPLY  WRK-DU-999-0001 BY 13                              ST1374.2
056800             GIVING XRECORD-LENGTH (1) ROUNDED.                   ST1374.2
056900     ADD     135 TO XRECORD-LENGTH (1).                           ST1374.2
057000     MOVE WRK-DU-999-0001 TO LENGTH-1.                            ST1374.2
057100     PERFORM STUFF-IT VARYING WRK-DU-999-0002                     ST1374.2
057200         FROM 1 BY 1 UNTIL WRK-DU-999-0002 IS GREATER THAN        ST1374.2
057300             WRK-DU-999-0001.                                     ST1374.2
057400     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1374.2
057500     ADD 1 TO XRECORD-NUMBER (1).                                 ST1374.2
057600     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1374.2
057700     ADD 1 TO WRK-DU-999-2.                                       ST1374.2
057800*    WRITE PRINT-REC FROM SQ-FS1R1-F-G-132.                       ST1374.2
057900*    WRITE PRINT-REC FROM REST-OF-1.                              ST1374.2
058000*    MOVE SPACES TO PRINT-REC.                                    ST1374.2
058100     WRITE   SQ-FS1R1-F-G-132.                                    ST1374.2
058200 STUFF-IT.                                                        ST1374.2
058300     MOVE WRK-DU-999-0002 TO STUFF-1 (WRK-DU-999-0002).           ST1374.2
058400 SRT-DELETE-001.                                                  ST1374.2
058500     PERFORM DE-LETE.                                             ST1374.2
058600 SRT-WRITE-001.                                                   ST1374.2
058700     MOVE "SRT-TEST-001" TO PAR-NAME.                             ST1374.2
058800     MOVE "FIRST FILE CREATED" TO COMPUTED-A.                     ST1374.2
058900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1374.2
059000     PERFORM PRINT-DETAIL.                                        ST1374.2
059100*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
059200*    WRITE   PRINT-REC.                                           ST1374.2
059300     CLOSE SQ-FS1.                                                ST1374.2
059400 SRT-INIT-002.                                                    ST1374.2
059500     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        ST1374.2
059600     OPEN OUTPUT SQ-FS2.                                          ST1374.2
059700     MOVE  "SQ-FS2" TO XFILE-NAME (1).                            ST1374.2
059800     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1374.2
059900     MOVE    000148 TO XRECORD-LENGTH (1).                        ST1374.2
060000     MOVE 0002 TO XBLOCK-SIZE (1).                                ST1374.2
060100 SRT-TEST-002.                                                    ST1374.2
060200     PERFORM SRT-TEST-002-BUILD VARYING WRK-DU-999-0001           ST1374.2
060300         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1374.2
060400*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
060500*    WRITE   PRINT-REC.                                           ST1374.2
060600     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1374.2
060700         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1374.2
060800         ELSE                                                     ST1374.2
060900         PERFORM PASS.                                            ST1374.2
061000     GO TO SRT-WRITE-002.                                         ST1374.2
061100 SRT-TEST-002-BUILD.                                              ST1374.2
061200     MOVE  100 TO LENGTH-2.                                       ST1374.2
061300     MOVE    SPACES TO STUFF-2 (1).                               ST1374.2
061400     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-4           ST1374.2
061500         ALPHAN-KEY OF KEY-5 ALPHAN-KEY OF KEY-6.                 ST1374.2
061600     ADD 51 WRK-DU-999-0001 GIVING NUM-KEY OF KEY-4               ST1374.2
061700         NUM-KEY OF KEY-5 NUM-KEY OF KEY-6.                       ST1374.2
061800     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-2.               ST1374.2
061900     ADD 000001 TO XRECORD-NUMBER (1).                            ST1374.2
062000*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1374.2
062100*    WRITE PRINT-REC FROM REST-OF-2.                              ST1374.2
062200*    MOVE SPACES TO PRINT-REC.                                    ST1374.2
062300     WRITE   SQ-FS2R1-F-G-132.                                    ST1374.2
062400 SRT-DELETE-002.                                                  ST1374.2
062500     PERFORM DE-LETE.                                             ST1374.2
062600 SRT-WRITE-002.                                                   ST1374.2
062700     MOVE "SRT-TEST-002" TO PAR-NAME.                             ST1374.2
062800     MOVE "2ND FILE CREATED" TO COMPUTED-A.                       ST1374.2
062900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1374.2
063000     PERFORM PRINT-DETAIL.                                        ST1374.2
063100*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
063200*    WRITE   PRINT-REC.                                           ST1374.2
063300     CLOSE SQ-FS2.                                                ST1374.2
063400 SRT-INIT-003.                                                    ST1374.2
063500     MOVE 100 TO LENGTH-100.                                      ST1374.2
063600     SORT ST-FS1                                                  ST1374.2
063700         ON ASCENDING KEY A-KEY OF SORT-KEY                       ST1374.2
063800         ASCENDING N-KEY OF NON-KEY-2                             ST1374.2
063900         USING SQ-FS1                                             ST1374.2
064000         GIVING SQ-FS2.                                           ST1374.2
064100 SRT-TEST-003.                                                    ST1374.2
064200     MOVE SPACES TO WRK-XN-X-0001.                                ST1374.2
064300     OPEN INPUT SQ-FS2.                                           ST1374.2
064400     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1374.2
064500         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1374.2
064600*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
064700*    WRITE   PRINT-REC.                                           ST1374.2
064800     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1374.2
064900         PERFORM FAIL GO TO SRT-FAIL-003                          ST1374.2
065000         ELSE                                                     ST1374.2
065100         PERFORM PASS.                                            ST1374.2
065200     GO TO SRT-WRITE-003.                                         ST1374.2
065300 SRT-DELETE-003.                                                  ST1374.2
065400     PERFORM DE-LETE.                                             ST1374.2
065500     GO TO SRT-WRITE-003.                                         ST1374.2
065600 SRT-FAIL-003.                                                    ST1374.2
065700     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1374.2
065800     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1374.2
065900 SRT-WRITE-003.                                                   ST1374.2
066000     MOVE "SRT-TEST-003" TO PAR-NAME.                             ST1374.2
066100     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1374.2
066200     PERFORM PRINT-DETAIL.                                        ST1374.2
066300*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
066400*    WRITE   PRINT-REC.                                           ST1374.2
066500 SRT-INIT-004.                                                    ST1374.2
066600     MOVE SPACES TO WRK-XN-X-0001.                                ST1374.2
066700 SRT-TEST-004.                                                    ST1374.2
066800     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1374.2
066900         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1374.2
067000*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
067100*    WRITE   PRINT-REC.                                           ST1374.2
067200     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1374.2
067300         PERFORM FAIL GO TO SRT-FAIL-004                          ST1374.2
067400         ELSE                                                     ST1374.2
067500         PERFORM PASS.                                            ST1374.2
067600     GO TO SRT-WRITE-004.                                         ST1374.2
067700 SRT-DELETE-004.                                                  ST1374.2
067800     PERFORM DE-LETE.                                             ST1374.2
067900     GO TO SRT-WRITE-004.                                         ST1374.2
068000 SRT-FAIL-004.                                                    ST1374.2
068100     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1374.2
068200     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1374.2
068300 SRT-WRITE-004.                                                   ST1374.2
068400     MOVE "SRT-TEST-004" TO PAR-NAME.                             ST1374.2
068500     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1374.2
068600     PERFORM PRINT-DETAIL.                                        ST1374.2
068700*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
068800*    WRITE   PRINT-REC.                                           ST1374.2
068900 SRT-INIT-005.                                                    ST1374.2
069000     MOVE SPACES TO WRK-XN-X-0001.                                ST1374.2
069100 SRT-TEST-005.                                                    ST1374.2
069200     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1374.2
069300         UNTIL WRK-DU-999-0001 IS GREATER THAN 11.                ST1374.2
069400*    MOVE    SPACES TO PRINT-REC.                                 ST1374.2
069500*    WRITE   PRINT-REC.                                           ST1374.2
069600     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1374.2
069700         PERFORM FAIL GO TO SRT-FAIL-005                          ST1374.2
069800         ELSE                                                     ST1374.2
069900         PERFORM PASS.                                            ST1374.2
070000     GO TO SRT-WRITE-005.                                         ST1374.2
070100 SRT-DELETE-005.                                                  ST1374.2
070200     PERFORM DE-LETE.                                             ST1374.2
070300     GO TO SRT-WRITE-005.                                         ST1374.2
070400 SRT-FAIL-005.                                                    ST1374.2
070500     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1374.2
070600     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1374.2
070700 SRT-WRITE-005.                                                   ST1374.2
070800     MOVE "SRT-TEST-005" TO PAR-NAME.                             ST1374.2
070900     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1374.2
071000     PERFORM PRINT-DETAIL.                                        ST1374.2
071100 SRT-TEST-006.                                                    ST1374.2
071200     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1374.2
071300         GO TO SRT-FAIL-006.                                      ST1374.2
071400     READ SQ-FS2 AT END PERFORM PASS                              ST1374.2
071500     GO TO SRT-WRITE-006.                                         ST1374.2
071600     GO TO SRT-FAIL-006.                                          ST1374.2
071700 SRT-DELETE-006.                                                  ST1374.2
071800     PERFORM DE-LETE.                                             ST1374.2
071900 SRT-FAIL-006.                                                    ST1374.2
072000     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1374.2
072100     PERFORM FAIL .                                               ST1374.2
072200 SRT-WRITE-006.                                                   ST1374.2
072300     MOVE "EOF CHECK SQ-FS2" TO FEATURE.                          ST1374.2
072400     MOVE "SRT-TEST-006" TO PAR-NAME.                             ST1374.2
072500     PERFORM PRINT-DETAIL.                                        ST1374.2
072600     CLOSE   SQ-FS2.                                              ST1374.2
072700     GO TO CCVS-999999.                                           ST1374.2
072800 READ-SQ-FS1 SECTION.                                             ST1374.2
072900 RD-1.                                                            ST1374.2
073000     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1374.2
073100         GO TO R1-EXIT.                                           ST1374.2
073200     READ SQ-FS2 AT END GO TO PREMATURE-EOF.                      ST1374.2
073300*    MOVE LENGTH-2 TO LENGTH-100.                                 ST1374.2
073400*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1374.2
073500*    WRITE PRINT-REC FROM REST-OF-2.                              ST1374.2
073600*    MOVE 100 TO LENGTH-100.                                      ST1374.2
073700*    MOVE SPACES TO PRINT-REC.                                    ST1374.2
073800     MOVE ALPHAN-KEY OF KEY-6 TO COMPU (WRK-DU-999-0001).         ST1374.2
073900     GO TO R1-EXIT.                                               ST1374.2
074000 PREMATURE-EOF.                                                   ST1374.2
074100     MOVE 1 TO WRK-DU-9-0001.                                     ST1374.2
074200     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1374.2
074300 R1-EXIT.                                                         ST1374.2
074400     EXIT.                                                        ST1374.2
074500 CCVS-EXIT SECTION.                                               ST1374.2
074600 CCVS-999999.                                                     ST1374.2
074700     GO TO CLOSE-FILES.                                           ST1374.2
      *END-OF,ST137A                                                                  
