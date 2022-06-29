      *HEADER,COBOL,ST139A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1394.2
000200 PROGRAM-ID.                                                      ST1394.2
000300     ST139A.                                                      ST1394.2
000400****************************************************************  ST1394.2
000500*                                                              *  ST1394.2
000600*    VALIDATION FOR:-                                          *  ST1394.2
000700*                                                              *  ST1394.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1394.2
000900*                                                              *  ST1394.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1394.2
001100*                                                              *  ST1394.2
001200****************************************************************  ST1394.2
001300*                                                                 ST1394.2
001400*    OBJECTIVE -                                                  ST1394.2
001500*        ROUTINE ST209 IS A TEST OF THE MERGE STATEMENT USING     ST1394.2
001600*    THE ASCII COLLATING SEQUENCE AND FIXED LENGTH RECORDS.       ST1394.2
001700*                                                                 ST1394.2
001800*        TWO FILES ARE FIRST CREATED BY THE ROUTINE IN ASCENDING  ST1394.2
001900*    ASCII ORDER.  THE ALPHABET NAME CLAUSE AND MERGE STATEMENT   ST1394.2
002000*    WITH THE COLLATING SEQUENCE PHRASE ARE USED TO TEST THE      ST1394.2
002100*    ABILITY OF THE COMPILER TO MERGE THE TWO FILES INTO A THIRD  ST1394.2
002200*    FILE IN ASCENDING ASCII ORDER.                               ST1394.2
002300*                                                                 ST1394.2
002400*                                                                 ST1394.2
002500*    FEATURES TESTED -                                            ST1394.2
002600*    *   ALPHABET-NAME IS STANDARD-1 (THE ASCII COLLATING SEQ.)   ST1394.2
002700*    *   COLLATING SEQUENCE IS ALPHABET-NAME                      ST1394.2
002800*    *   FIXED LENGTH RECORDS                                     ST1394.2
002900*    *   SAME SORT-MERGE AREA IN THE I-O-CONTROL PARAGRAPH        ST1394.2
003000*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1394.2
003100*    *   USING FILE-NAME SERIES                                   ST1394.2
003200*                                                                 ST1394.2
003300*    *   MERGE MERGE-FILE-NAME                                    ST1394.2
003400*        ASCENDING KEY-1 OF DATA-NAME-1                           ST1394.2
003500*        ON ASCENDING KEY KEY-2 OF DATA-NAME-2                    ST1394.2
003600*        SEQUENCE ALPHABET-ASCII-NAME                             ST1394.2
003700*        USING FILE-NAME-2 FILE-NAME-1                            ST1394.2
003800*        GIVING FILE-NAME-3.                                      ST1394.2
003900*                                                                 ST1394.2
004000*                                                                 ST1394.2
004100*    FILES USED -                                                 ST1394.2
004200*    *   FILES SQ-FS1 AND SQ-FS2 ON MAGNETIC TAPE OR MASS-STORAGE ST1394.2
004300*    ARE FIRST CREATED BY ROUTINE ST209.  THE MERGE STATEMENT     ST1394.2
004400*    USES BOTH OF THE FILES AND CREATES A THIRD FILE SQ-FS3.      ST1394.2
004500*                                                                 ST1394.2
004600*        SQ-FS1                                                   ST1394.2
004700*    51 RECORDS                                                   ST1394.2
004800*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1394.2
004900*    BLOCKED 1                                                    ST1394.2
005000*    RESERVE 2 AREAS                                              ST1394.2
005100*                                                                 ST1394.2
005200*        SQ-FS2                                                   ST1394.2
005300*    51 RECORDS                                                   ST1394.2
005400*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1394.2
005500*    BLOCKED 2                                                    ST1394.2
005600*    RESERVE 4 AREAS                                              ST1394.2
005700*                                                                 ST1394.2
005800*        SQ-FS3                                                   ST1394.2
005900*    FINAL TOTAL OF 102 RECORDS                                   ST1394.2
006000*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1394.2
006100*    BLOCKED 3                                                    ST1394.2
006200*    RESERVE 4 AREAS                                              ST1394.2
006300*                                                                 ST1394.2
006400*        NOTE THAT SQ-FS3 IS THE RESULT OF MERGING SQ-FS1 AND     ST1394.2
006500*    SQ-FS2.  THE RECORDS IN SQ-FS3 SHOULD ALTERNATE BETWEEN      ST1394.2
006600*    SQ-FS1 AND SQ-FS2 BECAUSE THE ALPHANUMERIC KEYS ARE THE SAME ST1394.2
006700*    FOR BOTH FILES AND THE NUMERIC KEYS WERE MERGED INTO         ST1394.2
006800*    ASCENDING ORDER.                                             ST1394.2
006900*                                                                 ST1394.2
007000*                                                                 ST1394.2
007100*    X-CARDS USED  -                                              ST1394.2
007200*    X-XXX014     SQ-FS1                                          ST1394.2
007300*    X-XXX015     SQ-FS2                                          ST1394.2
007400*    X-XXX016     SQ-FS3                                          ST1394.2
007500*    X-XXX060     SQ-FS4                                          ST1394.2
007600*    X-XXX027     MERGE FILE ST-FS1                               ST1394.2
007700*    X-55         SYSTEM PRINTER NAME.                            ST1394.2
007800*    X-82         SOURCE COMPUTER NAME.                           ST1394.2
007900*    X-83         OBJECT COMPUTER NAME.                           ST1394.2
008000*                                                                 ST1394.2
008100*                                                                 ST1394.2
008200*    OPTIONS RECOMMENDED -                                        ST1394.2
008300*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1394.2
008400*                  FILES AS THEY ARE CREATED AND READ DURING      ST1394.2
008500*                  TESTS 3 THRU 8.                                ST1394.2
008600*                                                                 ST1394.2
008700*                                                                 ST1394.2
008800*    TEST DESCRIPTIONS -                                          ST1394.2
008900*    MRG-TEST-001     CHECKS THE CREATION OF SQ-FS1               ST1394.2
009000*    MRG-TEST-002     CHECKS THE CREATION OF SQ-FS2               ST1394.2
009100*    MRG-TEST-003     TESTS RECORDS 1-20 ON MERGE RESULT SQ-FS3   ST1394.2
009200*    MRG-TEST-004     TESTS RECORDS 21-40 ON MERGE RESULT SQ-FS3  ST1394.2
009300*    MRG-TEST-005     TESTS RECORDS 41-60 ON MERGE RESULT SQ-FS3  ST1394.2
009400*    MRG-TEST-006     TESTS RECORDS 61-80 ON MERGE RESULT SQ-FS3  ST1394.2
009500*    MRG-TEST-007     TESTS RECORDS 81-100 ON MERGE RESULT SQ-FS3 ST1394.2
009600*    MRG-TEST-008     TESTS RECORDS 101-102 ON MERGE RESULT SQ-FS3ST1394.2
009700*    MRG-TEST-009     AN EOF CHECK ON SQ-FS3                      ST1394.2
009800*    MRG-TEST-010     CHECK THAT THE NUMERIC KEY ON THE LAST      ST1394.2
009900*                     RECORD ON SQ-FS3 EQUALS 102                 ST1394.2
010000*                                                                 ST1394.2
010100*                                                                 ST1394.2
010200*    ************************************************************ ST1394.2
010300 ENVIRONMENT DIVISION.                                            ST1394.2
010400 CONFIGURATION SECTION.                                           ST1394.2
010500 SOURCE-COMPUTER.                                                 ST1394.2
010600     XXXXX082.                                                    ST1394.2
010700 OBJECT-COMPUTER.                                                 ST1394.2
010800     XXXXX083.                                                    ST1394.2
010900 SPECIAL-NAMES.                                                   ST1394.2
011000     ALPHABET MY-FAVORITE-ALPHABET IS STANDARD-1.                 ST1394.2
011100 INPUT-OUTPUT SECTION.                                            ST1394.2
011200 FILE-CONTROL.                                                    ST1394.2
011300     SELECT PRINT-FILE ASSIGN TO                                  ST1394.2
011400     XXXXX055.                                                    ST1394.2
011500     SELECT SQ-FS1 ASSIGN                                         ST1394.2
011600     XXXXX014                                                     ST1394.2
011700     ; ORGANIZATION IS SEQUENTIAL                                 ST1394.2
011800     ; ACCESS MODE SEQUENTIAL                                     ST1394.2
011900     ; RESERVE 2 AREAS.                                           ST1394.2
012000     SELECT SQ-FS2 ASSIGN TO                                      ST1394.2
012100     XXXXX015                                                     ST1394.2
012200     ORGANIZATION IS SEQUENTIAL                                   ST1394.2
012300     ACCESS MODE IS SEQUENTIAL                                    ST1394.2
012400     RESERVE 4 AREAS.                                             ST1394.2
012500     SELECT SQ-FS3 ASSIGN TO                                      ST1394.2
012600     XXXXX016                                                     ST1394.2
012700     ORGANIZATION IS SEQUENTIAL                                   ST1394.2
012800     ; ACCESS MODE IS SEQUENTIAL                                  ST1394.2
012900     RESERVE 4 AREAS.                                             ST1394.2
013000     SELECT SQ-FS4 ASSIGN                                         ST1394.2
013100     XXXXX060.                                                    ST1394.2
013200     SELECT ST-FS1 ASSIGN TO                                      ST1394.2
013300     XXXXX027.                                                    ST1394.2
013400 I-O-CONTROL.                                                     ST1394.2
013500     SAME SORT-MERGE AREA FOR SQ-FS4, ST-FS1.                     ST1394.2
013600 DATA DIVISION.                                                   ST1394.2
013700 FILE SECTION.                                                    ST1394.2
013800 FD  PRINT-FILE.                                                  ST1394.2
013900 01  PRINT-REC PICTURE X(120).                                    ST1394.2
014000 01  DUMMY-RECORD PICTURE X(120).                                 ST1394.2
014100 FD  SQ-FS1                                                       ST1394.2
014200     LABEL RECORDS STANDARD                                       ST1394.2
014300C    VALUE OF                                                     ST1394.2
014400C    XXXXX074                                                     ST1394.2
014500C    XXXXX075                                                     ST1394.2
014600C    BLOCK CONTAINS 1 RECORDS                                     ST1394.2
014700*    XXXXX069                                                     ST1394.2
014800     RECORD CONTAINS 132 CHARACTERS.                              ST1394.2
014900 01  SQ-FS1R1-F-G-132.                                            ST1394.2
015000     10 REC-PREAMBLE PIC X(120).                                  ST1394.2
015100     10 REST-OF-1.                                                ST1394.2
015200     20 KEY-1.                                                    ST1394.2
015300         30 ALPHAN-KEY PIC X.                                     ST1394.2
015400         30 NUM-KEY PIC 999.                                      ST1394.2
015500     20 KEY-2.                                                    ST1394.2
015600         30 ALPHAN-KEY PIC X.                                     ST1394.2
015700         30 NUM-KEY PIC 999.                                      ST1394.2
015800     20 KEY-3.                                                    ST1394.2
015900         30 ALPHAN-KEY PIC X.                                     ST1394.2
016000         30 NUM-KEY PIC 999.                                      ST1394.2
016100 FD  SQ-FS2                                                       ST1394.2
016200     LABEL RECORD IS STANDARD                                     ST1394.2
016300C    ; VALUE OF                                                   ST1394.2
016400C    XXXXX074                                                     ST1394.2
016500C    IS                                                           ST1394.2
016600C    XXXXX076                                                     ST1394.2
016700*    XXXXX069                                                     ST1394.2
016800     ; BLOCK CONTAINS 2 RECORDS                                   ST1394.2
016900     ; RECORD CONTAINS 132 CHARACTERS                             ST1394.2
017000     DATA RECORD SQ-FS2R1-F-G-132.                                ST1394.2
017100 01  SQ-FS2R1-F-G-132.                                            ST1394.2
017200     10 REC-PRE-2 PIC X(120).                                     ST1394.2
017300     10 REST-OF-2.                                                ST1394.2
017400     20 KEY-4.                                                    ST1394.2
017500         30 ALPHAN-KEY PIC X.                                     ST1394.2
017600         30 NUM-KEY PIC 999.                                      ST1394.2
017700     20 KEY-5.                                                    ST1394.2
017800         30 ALPHAN-KEY PIC X.                                     ST1394.2
017900         30 NUM-KEY PIC 999.                                      ST1394.2
018000     20 KEY-6.                                                    ST1394.2
018100         30 ALPHAN-KEY PIC X.                                     ST1394.2
018200         30 NUM-KEY PIC 999.                                      ST1394.2
018300 FD  SQ-FS3                                                       ST1394.2
018400     LABEL RECORD IS STANDARD                                     ST1394.2
018500C    ; VALUE OF                                                   ST1394.2
018600C    XXXXX074                                                     ST1394.2
018700C    IS                                                           ST1394.2
018800C    XXXXX077                                                     ST1394.2
018900*    XXXXX069                                                     ST1394.2
019000     ; BLOCK CONTAINS 3 RECORDS                                   ST1394.2
019100     RECORD CONTAINS 132 CHARACTERS                               ST1394.2
019200     DATA RECORD SQ-FS3R1-F-G-132.                                ST1394.2
019300 01  SQ-FS3R1-F-G-132.                                            ST1394.2
019400     10  REC-PRE-3 PIC X(120).                                    ST1394.2
019500     10  REST-OF-3.                                               ST1394.2
019600     20  KEY-7.                                                   ST1394.2
019700          30  ALPHAN-KEY PIC X.                                   ST1394.2
019800          30  NUM-KEY PIC 999.                                    ST1394.2
019900     20  KEY-8.                                                   ST1394.2
020000          30  ALPHAN-KEY PIC X.                                   ST1394.2
020100          30  NUM-KEY PIC 999.                                    ST1394.2
020200     20 KEY-9.                                                    ST1394.2
020300          30  ALPHAN-KEY PIC X.                                   ST1394.2
020400          30  NUM-KEY PIC 999.                                    ST1394.2
020500 FD  SQ-FS4.                                                      ST1394.2
020600 01  SQ-FS4R1-F-6-132.                                            ST1394.2
020700     02  REC-2  PIC X(132).                                       ST1394.2
020800 SD  ST-FS1                                                       ST1394.2
020900     RECORD CONTAINS 132 CHARACTERS                               ST1394.2
021000     DATA RECORD IS ST-FS1R1-F-G-132.                             ST1394.2
021100 01  ST-FS1R1-F-G-132.                                            ST1394.2
021200     02 FILLER PIC X(120).                                        ST1394.2
021300     02 NON-KEY-1.                                                ST1394.2
021400         03 A-KEY PIC X.                                          ST1394.2
021500         03 N-KEY PIC 999.                                        ST1394.2
021600     02 SORT-KEY.                                                 ST1394.2
021700         03 A-KEY PIC X.                                          ST1394.2
021800         03 N-KEY PIC 999.                                        ST1394.2
021900     02 NON-KEY-2.                                                ST1394.2
022000         03 A-KEY PIC X.                                          ST1394.2
022100         03 N-KEY PIC 999.                                        ST1394.2
022200 WORKING-STORAGE SECTION.                                         ST1394.2
022300 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1394.2
022400 77  WRK-DU-999-0001 PIC 999.                                     ST1394.2
022500 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1394.2
022600 77  LAST-REC-NUM  PIC 999 VALUE ZERO.                            ST1394.2
022700 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1394.2
022800     " $$()*+,-./0123456789;<=>ABCDEFGHIJKLMNOPQRSTUVWXYZ".       ST1394.2
022900 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1394.2
023000     02 CHAR PIC X OCCURS 51 TIMES.                               ST1394.2
023100 01  WRK-XN-2    PIC X(51) VALUE                                  ST1394.2
023200     " $$()*+,-./0123456789;<=>ABCDEFGHIJKLMNOPQRSTUVWXYZ".       ST1394.2
023300 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1394.2
023400     02 ASCIIS PIC X OCCURS 51 TIMES.                             ST1394.2
023500 01  WRK-XN-O020F-0001.                                           ST1394.2
023600     02 COMPU PIC X OCCURS 20 TIMES.                              ST1394.2
023700 01  WRK-XN-X-0001 REDEFINES WRK-XN-O020F-0001.                   ST1394.2
023800     02 FILLER PIC X(20).                                         ST1394.2
023900 01  WRK-XN-O120F-1.                                              ST1394.2
024000     02 COLLS PIC X OCCURS 120 TIMES.                             ST1394.2
024100 01  WRK-XN-X-2 REDEFINES WRK-XN-O120F-1.                         ST1394.2
024200     02 WRK-XN-0002 PIC X(20).                                    ST1394.2
024300     02 WRK-XN-0003 PIC X(20).                                    ST1394.2
024400     02 WRK-XN-0004 PIC X(20).                                    ST1394.2
024500     02 WRK-XN-0005 PIC X(20).                                    ST1394.2
024600     02 WRK-XN-0006 PIC X(20).                                    ST1394.2
024700     02 WRK-XN-0007 PIC X(20).                                    ST1394.2
024800 01  FILE-RECORD-INFORMATION-REC.                                 ST1394.2
024900     03 FILE-RECORD-INFO-SKELETON.                                ST1394.2
025000        05 FILLER                 PICTURE X(48)       VALUE       ST1394.2
025100             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1394.2
025200        05 FILLER                 PICTURE X(46)       VALUE       ST1394.2
025300             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1394.2
025400        05 FILLER                 PICTURE X(26)       VALUE       ST1394.2
025500             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1394.2
025600        05 FILLER                 PICTURE X(37)       VALUE       ST1394.2
025700             ",RECKEY=                             ".             ST1394.2
025800        05 FILLER                 PICTURE X(38)       VALUE       ST1394.2
025900             ",ALTKEY1=                             ".            ST1394.2
026000        05 FILLER                 PICTURE X(38)       VALUE       ST1394.2
026100             ",ALTKEY2=                             ".            ST1394.2
026200        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1394.2
026300     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1394.2
026400        05 FILE-RECORD-INFO-P1-120.                               ST1394.2
026500           07 FILLER              PIC X(5).                       ST1394.2
026600           07 XFILE-NAME           PIC X(6).                      ST1394.2
026700           07 FILLER              PIC X(8).                       ST1394.2
026800           07 XRECORD-NAME         PIC X(6).                      ST1394.2
026900           07 FILLER              PIC X(1).                       ST1394.2
027000           07 REELUNIT-NUMBER     PIC 9(1).                       ST1394.2
027100           07 FILLER              PIC X(7).                       ST1394.2
027200           07 XRECORD-NUMBER       PIC 9(6).                      ST1394.2
027300           07 FILLER              PIC X(6).                       ST1394.2
027400           07 UPDATE-NUMBER       PIC 9(2).                       ST1394.2
027500           07 FILLER              PIC X(5).                       ST1394.2
027600           07 ODO-NUMBER          PIC 9(4).                       ST1394.2
027700           07 FILLER              PIC X(5).                       ST1394.2
027800           07 XPROGRAM-NAME        PIC X(5).                      ST1394.2
027900           07 FILLER              PIC X(7).                       ST1394.2
028000           07 XRECORD-LENGTH       PIC 9(6).                      ST1394.2
028100           07 FILLER              PIC X(7).                       ST1394.2
028200           07 CHARS-OR-RECORDS    PIC X(2).                       ST1394.2
028300           07 FILLER              PIC X(1).                       ST1394.2
028400           07 XBLOCK-SIZE          PIC 9(4).                      ST1394.2
028500           07 FILLER              PIC X(6).                       ST1394.2
028600           07 RECORDS-IN-FILE     PIC 9(6).                       ST1394.2
028700           07 FILLER              PIC X(5).                       ST1394.2
028800           07 XFILE-ORGANIZATION   PIC X(2).                      ST1394.2
028900           07 FILLER              PIC X(6).                       ST1394.2
029000           07 XLABEL-TYPE          PIC X(1).                      ST1394.2
029100        05 FILE-RECORD-INFO-P121-240.                             ST1394.2
029200           07 FILLER              PIC X(8).                       ST1394.2
029300           07 XRECORD-KEY          PIC X(29).                     ST1394.2
029400           07 FILLER              PIC X(9).                       ST1394.2
029500           07 ALTERNATE-KEY1      PIC X(29).                      ST1394.2
029600           07 FILLER              PIC X(9).                       ST1394.2
029700           07 ALTERNATE-KEY2      PIC X(29).                      ST1394.2
029800           07 FILLER              PIC X(7).                       ST1394.2
029900 01  TEST-RESULTS.                                                ST1394.2
030000     02 FILLER                   PIC X      VALUE SPACE.          ST1394.2
030100     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1394.2
030200     02 FILLER                   PIC X      VALUE SPACE.          ST1394.2
030300     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1394.2
030400     02 FILLER                   PIC X      VALUE SPACE.          ST1394.2
030500     02  PAR-NAME.                                                ST1394.2
030600       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1394.2
030700       03  PARDOT-X              PIC X      VALUE SPACE.          ST1394.2
030800       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1394.2
030900     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1394.2
031000     02 RE-MARK                  PIC X(61).                       ST1394.2
031100 01  TEST-COMPUTED.                                               ST1394.2
031200     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1394.2
031300     02 FILLER                   PIC X(17)  VALUE                 ST1394.2
031400            "       COMPUTED=".                                   ST1394.2
031500     02 COMPUTED-X.                                               ST1394.2
031600     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1394.2
031700     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1394.2
031800                                 PIC -9(9).9(9).                  ST1394.2
031900     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1394.2
032000     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1394.2
032100     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1394.2
032200     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1394.2
032300         04 COMPUTED-18V0                    PIC -9(18).          ST1394.2
032400         04 FILLER                           PIC X.               ST1394.2
032500     03 FILLER PIC X(50) VALUE SPACE.                             ST1394.2
032600 01  TEST-CORRECT.                                                ST1394.2
032700     02 FILLER PIC X(30) VALUE SPACE.                             ST1394.2
032800     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1394.2
032900     02 CORRECT-X.                                                ST1394.2
033000     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1394.2
033100     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1394.2
033200     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1394.2
033300     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1394.2
033400     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1394.2
033500     03      CR-18V0 REDEFINES CORRECT-A.                         ST1394.2
033600         04 CORRECT-18V0                     PIC -9(18).          ST1394.2
033700         04 FILLER                           PIC X.               ST1394.2
033800     03 FILLER PIC X(2) VALUE SPACE.                              ST1394.2
033900     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1394.2
034000 01  CCVS-C-1.                                                    ST1394.2
034100     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1394.2
034200-    "SS  PARAGRAPH-NAME                                          ST1394.2
034300-    "       REMARKS".                                            ST1394.2
034400     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1394.2
034500 01  CCVS-C-2.                                                    ST1394.2
034600     02 FILLER                     PIC X        VALUE SPACE.      ST1394.2
034700     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1394.2
034800     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1394.2
034900     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1394.2
035000     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1394.2
035100 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1394.2
035200 01  REC-CT                        PIC 99       VALUE ZERO.       ST1394.2
035300 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1394.2
035400 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1394.2
035500 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1394.2
035600 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1394.2
035700 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1394.2
035800 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1394.2
035900 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1394.2
036000 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1394.2
036100 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1394.2
036200 01  CCVS-H-1.                                                    ST1394.2
036300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1394.2
036400     02  FILLER                    PIC X(42)    VALUE             ST1394.2
036500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1394.2
036600     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1394.2
036700 01  CCVS-H-2A.                                                   ST1394.2
036800   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1394.2
036900   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1394.2
037000   02  FILLER                        PIC XXXX   VALUE             ST1394.2
037100     "4.2 ".                                                      ST1394.2
037200   02  FILLER                        PIC X(28)  VALUE             ST1394.2
037300            " COPY - NOT FOR DISTRIBUTION".                       ST1394.2
037400   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1394.2
037500                                                                  ST1394.2
037600 01  CCVS-H-2B.                                                   ST1394.2
037700   02  FILLER                        PIC X(15)  VALUE             ST1394.2
037800            "TEST RESULT OF ".                                    ST1394.2
037900   02  TEST-ID                       PIC X(9).                    ST1394.2
038000   02  FILLER                        PIC X(4)   VALUE             ST1394.2
038100            " IN ".                                               ST1394.2
038200   02  FILLER                        PIC X(12)  VALUE             ST1394.2
038300     " HIGH       ".                                              ST1394.2
038400   02  FILLER                        PIC X(22)  VALUE             ST1394.2
038500            " LEVEL VALIDATION FOR ".                             ST1394.2
038600   02  FILLER                        PIC X(58)  VALUE             ST1394.2
038700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1394.2
038800 01  CCVS-H-3.                                                    ST1394.2
038900     02  FILLER                      PIC X(34)  VALUE             ST1394.2
039000            " FOR OFFICIAL USE ONLY    ".                         ST1394.2
039100     02  FILLER                      PIC X(58)  VALUE             ST1394.2
039200     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1394.2
039300     02  FILLER                      PIC X(28)  VALUE             ST1394.2
039400            "  COPYRIGHT   1985 ".                                ST1394.2
039500 01  CCVS-E-1.                                                    ST1394.2
039600     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1394.2
039700     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1394.2
039800     02 ID-AGAIN                     PIC X(9).                    ST1394.2
039900     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1394.2
040000 01  CCVS-E-2.                                                    ST1394.2
040100     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1394.2
040200     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1394.2
040300     02 CCVS-E-2-2.                                               ST1394.2
040400         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1394.2
040500         03 FILLER                   PIC X      VALUE SPACE.      ST1394.2
040600         03 ENDER-DESC               PIC X(44)  VALUE             ST1394.2
040700            "ERRORS ENCOUNTERED".                                 ST1394.2
040800 01  CCVS-E-3.                                                    ST1394.2
040900     02  FILLER                      PIC X(22)  VALUE             ST1394.2
041000            " FOR OFFICIAL USE ONLY".                             ST1394.2
041100     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1394.2
041200     02  FILLER                      PIC X(58)  VALUE             ST1394.2
041300     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1394.2
041400     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1394.2
041500     02 FILLER                       PIC X(15)  VALUE             ST1394.2
041600             " COPYRIGHT 1985".                                   ST1394.2
041700 01  CCVS-E-4.                                                    ST1394.2
041800     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1394.2
041900     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1394.2
042000     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1394.2
042100     02 FILLER                       PIC X(40)  VALUE             ST1394.2
042200      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1394.2
042300 01  XXINFO.                                                      ST1394.2
042400     02 FILLER                       PIC X(19)  VALUE             ST1394.2
042500            "*** INFORMATION ***".                                ST1394.2
042600     02 INFO-TEXT.                                                ST1394.2
042700       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1394.2
042800       04 XXCOMPUTED                 PIC X(20).                   ST1394.2
042900       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1394.2
043000       04 XXCORRECT                  PIC X(20).                   ST1394.2
043100     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1394.2
043200 01  HYPHEN-LINE.                                                 ST1394.2
043300     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1394.2
043400     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1394.2
043500-    "*****************************************".                 ST1394.2
043600     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1394.2
043700-    "******************************".                            ST1394.2
043800 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1394.2
043900     "ST139A".                                                    ST1394.2
044000 PROCEDURE DIVISION.                                              ST1394.2
044100 DECLARATIVES.                                                    ST1394.2
044200 SECT-ST209-DEC SECTION.                                          ST1394.2
044300     USE AFTER STANDARD ERROR PROCEDURE ON OUTPUT.                ST1394.2
044400 MRG-WRITE-DEC.                                                   ST1394.2
044500     MOVE "ERROR ON OUTPUT DECL." TO FEATURE.                     ST1394.2
044600     MOVE "MRG-TEST-DEC" TO PAR-NAME.                             ST1394.2
044700     WRITE   PRINT-REC FROM TEST-RESULTS AFTER ADVANCING 2 LINES. ST1394.2
044800     STOP RUN.                                                    ST1394.2
044900 END DECLARATIVES.                                                ST1394.2
045000 CCVS1 SECTION.                                                   ST1394.2
045100 OPEN-FILES.                                                      ST1394.2
045200     OPEN    OUTPUT PRINT-FILE.                                   ST1394.2
045300     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1394.2
045400     MOVE    SPACE TO TEST-RESULTS.                               ST1394.2
045500     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1394.2
045600     MOVE    ZERO TO REC-SKL-SUB.                                 ST1394.2
045700     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1394.2
045800 CCVS-INIT-FILE.                                                  ST1394.2
045900     ADD     1 TO REC-SKL-SUB.                                    ST1394.2
046000     MOVE    FILE-RECORD-INFO-SKELETON                            ST1394.2
046100          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1394.2
046200 CCVS-INIT-EXIT.                                                  ST1394.2
046300     GO TO CCVS1-EXIT.                                            ST1394.2
046400 CLOSE-FILES.                                                     ST1394.2
046500     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1394.2
046600 TERMINATE-CCVS.                                                  ST1394.2
046700*    EXIT PROGRAM.                                                ST1394.2
046800*TERMINATE-CALL.                                                  ST1394.2
046900     STOP     RUN.                                                ST1394.2
047000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1394.2
047100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1394.2
047200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1394.2
047300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1394.2
047400     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1394.2
047500 PRINT-DETAIL.                                                    ST1394.2
047600     IF REC-CT NOT EQUAL TO ZERO                                  ST1394.2
047700             MOVE "." TO PARDOT-X                                 ST1394.2
047800             MOVE REC-CT TO DOTVALUE.                             ST1394.2
047900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1394.2
048000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1394.2
048100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1394.2
048200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1394.2
048300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1394.2
048400     MOVE SPACE TO CORRECT-X.                                     ST1394.2
048500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1394.2
048600     MOVE     SPACE TO RE-MARK.                                   ST1394.2
048700 HEAD-ROUTINE.                                                    ST1394.2
048800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1394.2
048900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1394.2
049000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1394.2
049100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1394.2
049200 COLUMN-NAMES-ROUTINE.                                            ST1394.2
049300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1394.2
049400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1394.2
049500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1394.2
049600 END-ROUTINE.                                                     ST1394.2
049700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1394.2
049800 END-RTN-EXIT.                                                    ST1394.2
049900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1394.2
050000 END-ROUTINE-1.                                                   ST1394.2
050100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1394.2
050200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1394.2
050300      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1394.2
050400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1394.2
050500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1394.2
050600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1394.2
050700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1394.2
050800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1394.2
050900  END-ROUTINE-12.                                                 ST1394.2
051000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1394.2
051100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1394.2
051200         MOVE "NO " TO ERROR-TOTAL                                ST1394.2
051300         ELSE                                                     ST1394.2
051400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1394.2
051500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1394.2
051600     PERFORM WRITE-LINE.                                          ST1394.2
051700 END-ROUTINE-13.                                                  ST1394.2
051800     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1394.2
051900         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1394.2
052000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1394.2
052100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1394.2
052200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1394.2
052300      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1394.2
052400          MOVE "NO " TO ERROR-TOTAL                               ST1394.2
052500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1394.2
052600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1394.2
052700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1394.2
052800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1394.2
052900 WRITE-LINE.                                                      ST1394.2
053000     ADD 1 TO RECORD-COUNT.                                       ST1394.2
053100*    IF RECORD-COUNT GREATER 42                                   ST1394.2
053200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1394.2
053300*        MOVE SPACE TO DUMMY-RECORD                               ST1394.2
053400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1394.2
053500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1394.2
053600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1394.2
053700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1394.2
053800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1394.2
053900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1394.2
054000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1394.2
054100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1394.2
054200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1394.2
054300*        MOVE ZERO TO RECORD-COUNT.                               ST1394.2
054400     PERFORM WRT-LN.                                              ST1394.2
054500 WRT-LN.                                                          ST1394.2
054600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1394.2
054700     MOVE SPACE TO DUMMY-RECORD.                                  ST1394.2
054800 BLANK-LINE-PRINT.                                                ST1394.2
054900     PERFORM WRT-LN.                                              ST1394.2
055000 FAIL-ROUTINE.                                                    ST1394.2
055100     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1394.2
055200            GO TO   FAIL-ROUTINE-WRITE.                           ST1394.2
055300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1394.2
055400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1394.2
055500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1394.2
055600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1394.2
055700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1394.2
055800     GO TO  FAIL-ROUTINE-EX.                                      ST1394.2
055900 FAIL-ROUTINE-WRITE.                                              ST1394.2
056000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1394.2
056100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1394.2
056200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1394.2
056300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1394.2
056400 FAIL-ROUTINE-EX. EXIT.                                           ST1394.2
056500 BAIL-OUT.                                                        ST1394.2
056600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1394.2
056700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1394.2
056800 BAIL-OUT-WRITE.                                                  ST1394.2
056900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1394.2
057000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1394.2
057100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1394.2
057200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1394.2
057300 BAIL-OUT-EX. EXIT.                                               ST1394.2
057400 CCVS1-EXIT.                                                      ST1394.2
057500     EXIT.                                                        ST1394.2
057600 SECT-ST209-0001 SECTION.                                         ST1394.2
057700 MRG-INIT-001.                                                    ST1394.2
057800     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1394.2
057900     OPEN OUTPUT SQ-FS1.                                          ST1394.2
058000     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1394.2
058100     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1394.2
058200     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       ST1394.2
058300     MOVE 000132 TO XRECORD-LENGTH (1).                           ST1394.2
058400     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1394.2
058500     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1394.2
058600     MOVE 000051 TO RECORDS-IN-FILE (1).                          ST1394.2
058700     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1394.2
058800     MOVE "S" TO XLABEL-TYPE (1).                                 ST1394.2
058900     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1394.2
059000             MOVE SPACES TO WRK-XN-O120F-1.                       ST1394.2
059100 MRG-TEST-001.                                                    ST1394.2
059200     PERFORM MRG-TEST-001-BUILD VARYING WRK-DU-999-0001           ST1394.2
059300         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1394.2
059400*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
059500*    WRITE   PRINT-REC.                                           ST1394.2
059600     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1394.2
059700         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1394.2
059800         ELSE                                                     ST1394.2
059900         PERFORM PASS.                                            ST1394.2
060000     GO TO MRG-WRITE-001.                                         ST1394.2
060100 MRG-TEST-001-BUILD.                                              ST1394.2
060200     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-1           ST1394.2
060300         ALPHAN-KEY OF KEY-2 ALPHAN-KEY OF KEY-3.                 ST1394.2
060400     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1394.2
060500         NUM-KEY OF KEY-3.                                        ST1394.2
060600     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1394.2
060700     ADD 1 TO XRECORD-NUMBER (1).                                 ST1394.2
060800     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1394.2
060900     ADD 1 TO WRK-DU-999-2.                                       ST1394.2
061000     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1394.2
061100     ADD 1 TO WRK-DU-999-2.                                       ST1394.2
061200*    WRITE PRINT-REC FROM SQ-FS1R1-F-G-132.                       ST1394.2
061300*    WRITE PRINT-REC FROM REST-OF-1.                              ST1394.2
061400*    MOVE SPACES TO PRINT-REC.                                    ST1394.2
061500     WRITE   SQ-FS1R1-F-G-132.                                    ST1394.2
061600 MRG-DELETE-001.                                                  ST1394.2
061700     PERFORM DE-LETE.                                             ST1394.2
061800 MRG-WRITE-001.                                                   ST1394.2
061900     MOVE "MRG-TEST-001" TO PAR-NAME.                             ST1394.2
062000     MOVE "FIRST FILE CREATED" TO COMPUTED-A.                     ST1394.2
062100     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1394.2
062200     PERFORM PRINT-DETAIL.                                        ST1394.2
062300*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
062400*    WRITE   PRINT-REC.                                           ST1394.2
062500     CLOSE SQ-FS1.                                                ST1394.2
062600 MRG-INIT-002.                                                    ST1394.2
062700     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        ST1394.2
062800     OPEN OUTPUT SQ-FS2.                                          ST1394.2
062900     MOVE  "SQ-FS2" TO XFILE-NAME (1).                            ST1394.2
063000     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1394.2
063100     MOVE 0002 TO XBLOCK-SIZE (1).                                ST1394.2
063200 MRG-TEST-002.                                                    ST1394.2
063300     PERFORM MRG-TEST-002-BUILD VARYING WRK-DU-999-0001           ST1394.2
063400         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1394.2
063500*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
063600*    WRITE   PRINT-REC.                                           ST1394.2
063700     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 52                     ST1394.2
063800         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1394.2
063900         ELSE                                                     ST1394.2
064000         PERFORM PASS.                                            ST1394.2
064100     GO TO MRG-WRITE-002.                                         ST1394.2
064200 MRG-TEST-002-BUILD.                                              ST1394.2
064300     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-4           ST1394.2
064400         ALPHAN-KEY OF KEY-5 ALPHAN-KEY OF KEY-6.                 ST1394.2
064500     ADD 51 WRK-DU-999-0001 GIVING NUM-KEY OF KEY-4               ST1394.2
064600         NUM-KEY OF KEY-5 NUM-KEY OF KEY-6.                       ST1394.2
064700     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-2.               ST1394.2
064800     ADD 000001 TO XRECORD-NUMBER (1).                            ST1394.2
064900*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1394.2
065000*    WRITE PRINT-REC FROM REST-OF-2.                              ST1394.2
065100*    MOVE SPACES TO PRINT-REC.                                    ST1394.2
065200     WRITE   SQ-FS2R1-F-G-132.                                    ST1394.2
065300 MRG-DELETE-002.                                                  ST1394.2
065400     PERFORM DE-LETE.                                             ST1394.2
065500 MRG-WRITE-002.                                                   ST1394.2
065600     MOVE "MRG-TEST-002" TO PAR-NAME.                             ST1394.2
065700     MOVE "2ND FILE CREATED" TO COMPUTED-A.                       ST1394.2
065800     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1394.2
065900     PERFORM PRINT-DETAIL.                                        ST1394.2
066000*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
066100*    WRITE   PRINT-REC.                                           ST1394.2
066200     CLOSE SQ-FS2.                                                ST1394.2
066300 MRG-INIT-003.                                                    ST1394.2
066400     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
066500     MERGE ST-FS1                                                 ST1394.2
066600         ASCENDING A-KEY OF SORT-KEY                              ST1394.2
066700         ON ASCENDING KEY N-KEY OF NON-KEY-1                      ST1394.2
066800         SEQUENCE MY-FAVORITE-ALPHABET                            ST1394.2
066900         USING SQ-FS2 SQ-FS1                                      ST1394.2
067000         GIVING SQ-FS3.                                           ST1394.2
067100 MRG-TEST-003.                                                    ST1394.2
067200     OPEN INPUT SQ-FS3.                                           ST1394.2
067300     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
067400         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1394.2
067500*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
067600*    WRITE   PRINT-REC.                                           ST1394.2
067700     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1394.2
067800         PERFORM FAIL GO TO MRG-FAIL-003                          ST1394.2
067900         ELSE                                                     ST1394.2
068000         PERFORM PASS.                                            ST1394.2
068100     GO TO MRG-WRITE-003.                                         ST1394.2
068200 MRG-DELETE-003.                                                  ST1394.2
068300     PERFORM DE-LETE.                                             ST1394.2
068400     GO TO MRG-WRITE-003.                                         ST1394.2
068500 MRG-FAIL-003.                                                    ST1394.2
068600     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
068700     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1394.2
068800 MRG-WRITE-003.                                                   ST1394.2
068900     MOVE "MRG-TEST-003" TO PAR-NAME.                             ST1394.2
069000     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
069100     PERFORM PRINT-DETAIL.                                        ST1394.2
069200*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
069300*    WRITE   PRINT-REC.                                           ST1394.2
069400 MRG-INIT-004.                                                    ST1394.2
069500     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
069600 MRG-TEST-004.                                                    ST1394.2
069700     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
069800         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1394.2
069900*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
070000*    WRITE   PRINT-REC.                                           ST1394.2
070100     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1394.2
070200         PERFORM FAIL GO TO MRG-FAIL-004                          ST1394.2
070300         ELSE                                                     ST1394.2
070400         PERFORM PASS.                                            ST1394.2
070500     GO TO MRG-WRITE-004.                                         ST1394.2
070600 MRG-DELETE-004.                                                  ST1394.2
070700     PERFORM DE-LETE.                                             ST1394.2
070800     GO TO MRG-WRITE-004.                                         ST1394.2
070900 MRG-FAIL-004.                                                    ST1394.2
071000     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
071100     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1394.2
071200 MRG-WRITE-004.                                                   ST1394.2
071300     MOVE "MRG-TEST-004" TO PAR-NAME.                             ST1394.2
071400     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
071500     PERFORM PRINT-DETAIL.                                        ST1394.2
071600*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
071700*    WRITE   PRINT-REC.                                           ST1394.2
071800 MRG-INIT-005.                                                    ST1394.2
071900     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
072000 MRG-TEST-005.                                                    ST1394.2
072100     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
072200         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1394.2
072300*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
072400*    WRITE   PRINT-REC.                                           ST1394.2
072500     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1394.2
072600         PERFORM FAIL GO TO MRG-FAIL-005                          ST1394.2
072700         ELSE                                                     ST1394.2
072800         PERFORM PASS.                                            ST1394.2
072900     GO TO MRG-WRITE-005.                                         ST1394.2
073000 MRG-DELETE-005.                                                  ST1394.2
073100     PERFORM DE-LETE.                                             ST1394.2
073200     GO TO MRG-WRITE-005.                                         ST1394.2
073300 MRG-FAIL-005.                                                    ST1394.2
073400     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
073500     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1394.2
073600 MRG-WRITE-005.                                                   ST1394.2
073700     MOVE "MRG-TEST-005" TO PAR-NAME.                             ST1394.2
073800     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
073900     PERFORM PRINT-DETAIL.                                        ST1394.2
074000*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
074100*    WRITE   PRINT-REC.                                           ST1394.2
074200 MRG-INIT-006.                                                    ST1394.2
074300     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
074400 MRG-TEST-006.                                                    ST1394.2
074500     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
074600         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1394.2
074700*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
074800*    WRITE   PRINT-REC.                                           ST1394.2
074900     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1394.2
075000         PERFORM FAIL GO TO MRG-FAIL-006                          ST1394.2
075100         ELSE                                                     ST1394.2
075200         PERFORM PASS.                                            ST1394.2
075300     GO TO MRG-WRITE-006.                                         ST1394.2
075400 MRG-DELETE-006.                                                  ST1394.2
075500     PERFORM DE-LETE.                                             ST1394.2
075600     GO TO MRG-WRITE-006.                                         ST1394.2
075700 MRG-FAIL-006.                                                    ST1394.2
075800     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
075900     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1394.2
076000 MRG-WRITE-006.                                                   ST1394.2
076100     MOVE "MRG-TEST-006" TO PAR-NAME.                             ST1394.2
076200     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
076300     PERFORM PRINT-DETAIL.                                        ST1394.2
076400*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
076500*    WRITE   PRINT-REC.                                           ST1394.2
076600 MRG-INIT-007.                                                    ST1394.2
076700     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
076800 MRG-TEST-007.                                                    ST1394.2
076900     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
077000         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1394.2
077100*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
077200*    WRITE   PRINT-REC.                                           ST1394.2
077300     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1394.2
077400         PERFORM FAIL GO TO MRG-FAIL-007                          ST1394.2
077500         ELSE                                                     ST1394.2
077600         PERFORM PASS.                                            ST1394.2
077700     GO TO MRG-WRITE-007.                                         ST1394.2
077800 MRG-DELETE-007.                                                  ST1394.2
077900     PERFORM DE-LETE.                                             ST1394.2
078000     GO TO MRG-WRITE-007.                                         ST1394.2
078100 MRG-FAIL-007.                                                    ST1394.2
078200     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
078300     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1394.2
078400 MRG-WRITE-007.                                                   ST1394.2
078500     MOVE "MRG-TEST-007" TO PAR-NAME.                             ST1394.2
078600     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
078700     PERFORM PRINT-DETAIL.                                        ST1394.2
078800*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
078900*    WRITE   PRINT-REC.                                           ST1394.2
079000 MRG-INIT-008.                                                    ST1394.2
079100     MOVE SPACES TO WRK-XN-X-0001.                                ST1394.2
079200 MRG-TEST-008.                                                    ST1394.2
079300     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1394.2
079400         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1394.2
079500*    MOVE    SPACES TO PRINT-REC.                                 ST1394.2
079600*    WRITE   PRINT-REC.                                           ST1394.2
079700     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1394.2
079800         PERFORM FAIL GO TO MRG-FAIL-008                          ST1394.2
079900         ELSE                                                     ST1394.2
080000         PERFORM PASS.                                            ST1394.2
080100     GO TO MRG-WRITE-008.                                         ST1394.2
080200 MRG-DELETE-008.                                                  ST1394.2
080300     PERFORM DE-LETE.                                             ST1394.2
080400     GO TO MRG-WRITE-008.                                         ST1394.2
080500 MRG-FAIL-008.                                                    ST1394.2
080600     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1394.2
080700     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1394.2
080800 MRG-WRITE-008.                                                   ST1394.2
080900     MOVE "MRG-TEST-008" TO PAR-NAME.                             ST1394.2
081000     MOVE "STAND. COLLATING SEQ." TO FEATURE.                     ST1394.2
081100     PERFORM PRINT-DETAIL.                                        ST1394.2
081200     MOVE    NUM-KEY OF KEY-7 TO LAST-REC-NUM.                    ST1394.2
081300 MRG-TEST-009.                                                    ST1394.2
081400     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1394.2
081500         GO TO MRG-FAIL-009.                                      ST1394.2
081600     READ SQ-FS3 END PERFORM PASS                                 ST1394.2
081700         GO TO MRG-WRITE-009.                                     ST1394.2
081800     GO TO MRG-FAIL-009.                                          ST1394.2
081900 MRG-DELETE-009.                                                  ST1394.2
082000     PERFORM DE-LETE.                                             ST1394.2
082100     GO TO MRG-WRITE-009.                                         ST1394.2
082200 MRG-FAIL-009.                                                    ST1394.2
082300     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1394.2
082400     PERFORM FAIL .                                               ST1394.2
082500 MRG-WRITE-009.                                                   ST1394.2
082600     MOVE "EOF CHECK SQ-FS3" TO FEATURE.                          ST1394.2
082700     MOVE "MRG-TEST-009" TO PAR-NAME.                             ST1394.2
082800     PERFORM PRINT-DETAIL.                                        ST1394.2
082900 MRG-TEST-010.                                                    ST1394.2
083000     IF LAST-REC-NUM IS NOT EQUAL TO 102                          ST1394.2
083100         PERFORM FAIL GO TO MRG-FAIL-010                          ST1394.2
083200         ELSE                                                     ST1394.2
083300         PERFORM PASS.                                            ST1394.2
083400     GO TO MRG-WRITE-010.                                         ST1394.2
083500 MRG-DELETE-010.                                                  ST1394.2
083600     PERFORM DE-LETE.                                             ST1394.2
083700     GO TO MRG-WRITE-010.                                         ST1394.2
083800 MRG-FAIL-010.                                                    ST1394.2
083900     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1394.2
084000     MOVE 102 TO CR-18V0.                                         ST1394.2
084100 MRG-WRITE-010.                                                   ST1394.2
084200     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1394.2
084300     MOVE "MRG-TEST-010" TO PAR-NAME.                             ST1394.2
084400     PERFORM PRINT-DETAIL.                                        ST1394.2
084500     CLOSE   SQ-FS3.                                              ST1394.2
084600     GO TO CCVS-999999.                                           ST1394.2
084700 READ-SQ-FS1 SECTION.                                             ST1394.2
084800 RD-1.                                                            ST1394.2
084900     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1394.2
085000         GO TO R1-EXIT.                                           ST1394.2
085100     READ SQ-FS3 AT END GO TO PREMATURE-EOF.                      ST1394.2
085200*    WRITE PRINT-REC FROM SQ-FS3R1-F-G-132.                       ST1394.2
085300*    WRITE PRINT-REC FROM REST-OF-3.                              ST1394.2
085400*    MOVE SPACES TO PRINT-REC.                                    ST1394.2
085500     MOVE ALPHAN-KEY OF KEY-8 TO COMPU (WRK-DU-999-0001).         ST1394.2
085600     GO TO R1-EXIT.                                               ST1394.2
085700 PREMATURE-EOF.                                                   ST1394.2
085800     MOVE 1 TO WRK-DU-9-0001.                                     ST1394.2
085900     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1394.2
086000 R1-EXIT.                                                         ST1394.2
086100     EXIT.                                                        ST1394.2
086200 CCVS-EXIT SECTION.                                               ST1394.2
086300 CCVS-999999.                                                     ST1394.2
086400     GO TO CLOSE-FILES.                                           ST1394.2
      *END-OF,ST139A                                                                  
