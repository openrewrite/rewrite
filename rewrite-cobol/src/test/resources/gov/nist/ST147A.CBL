      *HEADER,COBOL,ST147A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1474.2
000200 PROGRAM-ID.                                                      ST1474.2
000300     ST147A.                                                      ST1474.2
000400****************************************************************  ST1474.2
000500*                                                              *  ST1474.2
000600*    VALIDATION FOR:-                                          *  ST1474.2
000700*                                                              *  ST1474.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1474.2
000900*                                                              *  ST1474.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1474.2
001100*                                                              *  ST1474.2
001200****************************************************************  ST1474.2
001300*                                                                 ST1474.2
001400*    OBJECTIVE -                                                  ST1474.2
001500*        ROUTINE ST147A IS A TEST OF THE MERGE STATEMENT USING    ST1474.2
001600*    A NATIVE  COLLATING SEQUENCE AND FIXED LENGTH RECORDS.       ST1474.2
001700*                                                                 ST1474.2
001800*        TWO FILES ARE FIRST CREATED BY THE ROUTINE IN DESCENDING ST1474.2
001900*    NATIVE ORDER.  THE MERGE STATEMENT IS USED TO MERGE THE TWO  ST1474.2
002000*    FILES AND PRODUCE, IN DESCENDING NATIVE COLLATING            ST1474.2
002100*    SEQUENCE ORDER, 3 OUTPUT FILES FROM A SINGLE "MERGE"         ST1474.2
002200*    STATEMENT.                                                   ST1474.2
002300*                                                                 ST1474.2
002400*    FEATURES TESTED -                                            ST1474.2
002500*    *   FIXED LENGTH RECORDS                                     ST1474.2
002600*    *   SAME SORT-MERGE AREA IN THE I-O-CONTROL PARAGRAPH        ST1474.2
002700*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1474.2
002800*    *   USING FILE-NAME SERIES                                   ST1474.2
002900*                                                                 ST1474.2
003000*    *   MERGE MERGE-FILE-NAME                                    ST1474.2
003100*        DESCENDING KEY-1 OF DATA-NAME-1                          ST1474.2
003200*        ON DESCENDING KEY KEY-2 OF DATA-NAME-2                   ST1474.2
003300*        USING FILE-NAME-2 FILE-NAME-1                            ST1474.2
003400*        GIVING FILE-NAME-3, FILE-NAME-4, FILE-NAME-5.            ST1474.2
003500*                                                                 ST1474.2
003600*    FILES USED -                                                 ST1474.2
003700*    *   FILES SQ-FS1 AND SQ-FS2 ON MAGNETIC TAPE OR MASS-STORAGE ST1474.2
003800*    ARE FIRST CREATED.  THE MERGE STATEMENT                      ST1474.2
003900*    USES BOTH OF THESE FILES AND CREATES OUTPUT FILES            ST1474.2
004000*    SQ-FS3, SQ-FS4 AND SQ-FS5.                                   ST1474.2
004100*                                                                 ST1474.2
004200*        SQ-FS1                                                   ST1474.2
004300*    51 RECORDS                                                   ST1474.2
004400*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1474.2
004500*    BLOCKED 1                                                    ST1474.2
004600*    RESERVE 2 AREAS                                              ST1474.2
004700*                                                                 ST1474.2
004800*        SQ-FS2                                                   ST1474.2
004900*    51 RECORDS                                                   ST1474.2
005000*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1474.2
005100*    BLOCKED 2                                                    ST1474.2
005200*    RESERVE 4 AREAS                                              ST1474.2
005300*                                                                 ST1474.2
005400*        SQ-FS3, SQ-FS4 AND SQ-FS5                                ST1474.2
005500*    FINAL TOTAL OF 102 RECORDS                                   ST1474.2
005600*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1474.2
005700*    BLOCKED 3                                                    ST1474.2
005800*    RESERVE 4 AREAS                                              ST1474.2
005900*                                                                 ST1474.2
006000*        NOTE THAT SQ-FS3 IS THE RESULT OF MERGING SQ-FS1 AND     ST1474.2
006100*    SQ-FS2.  THE RECORDS IN SQ-FS3 SHOULD ALTERNATE BETWEEN      ST1474.2
006200*    SQ-FS1 AND SQ-FS2 BECAUSE THE ALPHANUMERIC KEYS ARE THE SAME ST1474.2
006300*    FOR BOTH FILES AND THE NUMERIC KEYS WERE MERGED INTO         ST1474.2
006400*    DESCENDING ORDER.  FILES SQ-FS4 AND SQ-FS5 ARE               ST1474.2
006500*    IDENTICAL TO SQ-FS3.                                         ST1474.2
006600*                                                                 ST1474.2
006700*    X-CARDS USED  -                                              ST1474.2
006800*    X-XXX014     SQ-FS1                                          ST1474.2
006900*    X-XXX015     SQ-FS2                                          ST1474.2
007000*    X-XXX016     SQ-FS3                                          ST1474.2
007100*    X-XXX018     SQ-FS5                                          ST1474.2
007200*    X-XXX027     MERGE FILE ST-FS1                               ST1474.2
007300*    X-55         SYSTEM PRINTER NAME.                            ST1474.2
007400*    X-60         SQ-FS4                                          ST1474.2
007500*    X-XXX063     NATIVE COLLATING SEQUENCE ASCENDING ORDER-NOTE  ST1474.2
007600*        THAT THE QUOTE CHARACTER IS NOT TO APPEAR IN THE X-64    ST1474.2
007700*        CARD AND THE DOLLAR SIGN $ IS TO APPEAR TWICE WHEREVER   ST1474.2
007800*        THE $ BELONGS IN THE NATIVE COLLATING SEQUENCE).  IF     ST1474.2
007900*        THE NATIVE COLLATING SEQUENCE IS ACTUALLY THE ASCII      ST1474.2
008000*        COLLATING SEQUENCE SEE BELOW FOR A SAMPLE X-64 CARD..... ST1474.2
008100*                                                                 ST1474.2
008200*    X-63  " $$()*+,./0123456789;<=>ABCDEFGHIJKLMNOPQRSTUVWXYZ".  ST1474.2
008300*    X-69         OPTIONAL VALUE OF CLAUSE                        ST1474.2
008400*    X-74         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
008500*    X-75         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
008600*    X-76         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
008700*    X-77         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
008800*    X-78         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
008900*    X-79         VALUE OF CLAUSE NAME PHRASES                    ST1474.2
009000*    X-82         SOURCE COMPUTER NAME.                           ST1474.2
009100*    X-83         OBJECT COMPUTER NAME.                           ST1474.2
009200*                                                                 ST1474.2
009300*                                                                 ST1474.2
009400*    OPTIONS RECOMMENDED -                                        ST1474.2
009500*    *   OPT SW6 - X   TO BE USED IF NECESSARY TO DUMP THE        ST1474.2
009600*                      FILES AS THEY ARE CREATED AND READ         ST1474.2
009700*                      DURING TESTS 3 THRU 8, 11 THRU 16,         ST1474.2
009800*                      AND 19 THRU 24.                            ST1474.2
009900*                                                                 ST1474.2
010000*    TEST DESCRIPTIONS -                                          ST1474.2
010100*    MRG-TEST-001     CHECKS THE CREATION OF SQ-FS1               ST1474.2
010200*    MRG-TEST-002     CHECKS THE CREATION OF SQ-FS2               ST1474.2
010300*    MRG-TEST-003     TESTS RECORDS 1-20 ON MERGE RESULT SQ-FS3   ST1474.2
010400*    MRG-TEST-004     TESTS RECORDS 21-40 ON MERGE RESULT SQ-FS3  ST1474.2
010500*    MRG-TEST-005     TESTS RECORDS 41-60 ON MERGE RESULT SQ-FS3  ST1474.2
010600*    MRG-TEST-006     TESTS RECORDS 61-80 ON MERGE RESULT SQ-FS3  ST1474.2
010700*    MRG-TEST-007     TESTS RECORDS 81-100 ON MERGE RESULT SQ-FS3 ST1474.2
010800*    MRG-TEST-008     TESTS RECORDS 101-102 ON MERGE RESULT SQ-FS3ST1474.2
010900*    MRG-TEST-009     AN EOF CHECK ON SQ-FS3                      ST1474.2
011000*    MRG-TEST-010     CHECK THAT THE NUMERIC KEY ON THE LAST      ST1474.2
011100*                     RECORD ON SQ-FS3 EQUALS 51                  ST1474.2
011200*    MRG-TEST-011                                                 ST1474.2
011300*    TO                                                           ST1474.2
011400*    MRG-TEST-018     SAME TESTS ON SQ-FS4                        ST1474.2
011500*    MRG-TEST-019                                                 ST1474.2
011600*    TO                                                           ST1474.2
011700*    MRG-TEST-026     SAME TESTS ON SQ-FS5                        ST1474.2
011800*                                                                 ST1474.2
011900*    ************************************************************ ST1474.2
012000 ENVIRONMENT DIVISION.                                            ST1474.2
012100 CONFIGURATION SECTION.                                           ST1474.2
012200 SOURCE-COMPUTER.                                                 ST1474.2
012300     XXXXX082.                                                    ST1474.2
012400 OBJECT-COMPUTER.                                                 ST1474.2
012500     XXXXX083.                                                    ST1474.2
012600 INPUT-OUTPUT SECTION.                                            ST1474.2
012700 FILE-CONTROL.                                                    ST1474.2
012800     SELECT PRINT-FILE ASSIGN TO                                  ST1474.2
012900     XXXXX055.                                                    ST1474.2
013000     SELECT SQ-FS1 ASSIGN                                         ST1474.2
013100     XXXXX014                                                     ST1474.2
013200     ; ORGANIZATION IS SEQUENTIAL                                 ST1474.2
013300     ; ACCESS MODE SEQUENTIAL                                     ST1474.2
013400     ; RESERVE 2 AREAS.                                           ST1474.2
013500     SELECT SQ-FS2 ASSIGN TO                                      ST1474.2
013600     XXXXX015                                                     ST1474.2
013700     ORGANIZATION IS SEQUENTIAL                                   ST1474.2
013800     ACCESS MODE IS SEQUENTIAL                                    ST1474.2
013900     RESERVE 4 AREAS.                                             ST1474.2
014000     SELECT SQ-FS3 ASSIGN TO                                      ST1474.2
014100     XXXXX016                                                     ST1474.2
014200     ORGANIZATION IS SEQUENTIAL                                   ST1474.2
014300     ; ACCESS MODE IS SEQUENTIAL                                  ST1474.2
014400     RESERVE 4 AREAS.                                             ST1474.2
014500     SELECT SQ-FS4 ASSIGN TO                                      ST1474.2
014600     XXXXX060                                                     ST1474.2
014700     ORGANIZATION IS SEQUENTIAL                                   ST1474.2
014800     ; ACCESS MODE IS SEQUENTIAL                                  ST1474.2
014900     RESERVE 4 AREAS.                                             ST1474.2
015000     SELECT SQ-FS5 ASSIGN TO                                      ST1474.2
015100     XXXXX018                                                     ST1474.2
015200     ORGANIZATION IS SEQUENTIAL                                   ST1474.2
015300     ; ACCESS MODE IS SEQUENTIAL                                  ST1474.2
015400     RESERVE 4 AREAS.                                             ST1474.2
015500     SELECT ST-FS1 ASSIGN TO                                      ST1474.2
015600     XXXXX027.                                                    ST1474.2
015700 I-O-CONTROL.                                                     ST1474.2
015800*    SAME SORT-MERGE AREA FOR SQ-FS1, ST-FS1.                     ST1474.2
015900 DATA DIVISION.                                                   ST1474.2
016000 FILE SECTION.                                                    ST1474.2
016100 FD  PRINT-FILE.                                                  ST1474.2
016200 01  PRINT-REC PICTURE X(120).                                    ST1474.2
016300 01  DUMMY-RECORD PICTURE X(120).                                 ST1474.2
016400 FD  SQ-FS1                                                       ST1474.2
016500     LABEL RECORDS STANDARD                                       ST1474.2
016600C    VALUE OF                                                     ST1474.2
016700C    XXXXX074                                                     ST1474.2
016800C    XXXXX075                                                     ST1474.2
016900C    BLOCK CONTAINS 1 RECORDS                                     ST1474.2
017000*    XXXXX069                                                     ST1474.2
017100     RECORD CONTAINS 132 CHARACTERS.                              ST1474.2
017200 01  SQ-FS1R1-F-G-132.                                            ST1474.2
017300     10 REC-PREAMBLE PIC X(120).                                  ST1474.2
017400     10 REST-OF-1.                                                ST1474.2
017500     20 KEY-1.                                                    ST1474.2
017600         30 ALPHAN-KEY PIC X.                                     ST1474.2
017700         30 NUM-KEY PIC 999.                                      ST1474.2
017800     20 KEY-2.                                                    ST1474.2
017900         30 ALPHAN-KEY PIC X.                                     ST1474.2
018000         30 NUM-KEY PIC 999.                                      ST1474.2
018100     20 KEY-3.                                                    ST1474.2
018200         30 ALPHAN-KEY PIC X.                                     ST1474.2
018300         30 NUM-KEY PIC 999.                                      ST1474.2
018400 FD  SQ-FS2                                                       ST1474.2
018500     LABEL RECORD IS STANDARD                                     ST1474.2
018600C    ; VALUE OF                                                   ST1474.2
018700C    XXXXX074                                                     ST1474.2
018800C    IS                                                           ST1474.2
018900C    XXXXX076                                                     ST1474.2
019000*    XXXXX069                                                     ST1474.2
019100     ; BLOCK CONTAINS 2 RECORDS                                   ST1474.2
019200     ; RECORD CONTAINS 132 CHARACTERS                             ST1474.2
019300     DATA RECORD SQ-FS2R1-F-G-132.                                ST1474.2
019400 01  SQ-FS2R1-F-G-132.                                            ST1474.2
019500     10 REC-PRE-2 PIC X(120).                                     ST1474.2
019600     10 REST-OF-2.                                                ST1474.2
019700     20 KEY-4.                                                    ST1474.2
019800         30 ALPHAN-KEY PIC X.                                     ST1474.2
019900         30 NUM-KEY PIC 999.                                      ST1474.2
020000     20 KEY-5.                                                    ST1474.2
020100         30 ALPHAN-KEY PIC X.                                     ST1474.2
020200         30 NUM-KEY PIC 999.                                      ST1474.2
020300     20 KEY-6.                                                    ST1474.2
020400         30 ALPHAN-KEY PIC X.                                     ST1474.2
020500         30 NUM-KEY PIC 999.                                      ST1474.2
020600 FD  SQ-FS3                                                       ST1474.2
020700     LABEL RECORD IS STANDARD                                     ST1474.2
020800C    ; VALUE OF                                                   ST1474.2
020900C    XXXXX074                                                     ST1474.2
021000C    IS                                                           ST1474.2
021100C    XXXXX077                                                     ST1474.2
021200*    XXXXX069                                                     ST1474.2
021300     ; BLOCK CONTAINS 3 RECORDS                                   ST1474.2
021400     RECORD CONTAINS 132 CHARACTERS                               ST1474.2
021500     DATA RECORD SQ-FS3R1-F-G-132.                                ST1474.2
021600 01  SQ-FS3R1-F-G-132.                                            ST1474.2
021700     10  REC-PRE-3 PIC X(120).                                    ST1474.2
021800     10  REST-OF-3.                                               ST1474.2
021900     20  KEY-7.                                                   ST1474.2
022000          30  ALPHAN-KEY PIC X.                                   ST1474.2
022100          30  NUM-KEY PIC 999.                                    ST1474.2
022200     20  KEY-8.                                                   ST1474.2
022300          30  ALPHAN-KEY PIC X.                                   ST1474.2
022400          30  NUM-KEY PIC 999.                                    ST1474.2
022500     20  KEY-9.                                                   ST1474.2
022600          30  ALPHAN-KEY PIC X.                                   ST1474.2
022700          30  NUM-KEY PIC 999.                                    ST1474.2
022800 FD  SQ-FS4                                                       ST1474.2
022900     LABEL RECORD IS STANDARD                                     ST1474.2
023000C    ; VALUE OF                                                   ST1474.2
023100C    XXXXX074                                                     ST1474.2
023200C    IS                                                           ST1474.2
023300C    XXXXX078                                                     ST1474.2
023400*    XXXXX069                                                     ST1474.2
023500     ; BLOCK CONTAINS 3 RECORDS                                   ST1474.2
023600     RECORD CONTAINS 132 CHARACTERS                               ST1474.2
023700     DATA RECORD SQ-FS4R1-F-G-132.                                ST1474.2
023800 01  SQ-FS4R1-F-G-132.                                            ST1474.2
023900     10  REC-PRE-4 PIC X(120).                                    ST1474.2
024000     10  REST-OF-4.                                               ST1474.2
024100     20  KEY-10.                                                  ST1474.2
024200          30  ALPHAN-KEY PIC X.                                   ST1474.2
024300          30  NUM-KEY PIC 999.                                    ST1474.2
024400     20  KEY-11.                                                  ST1474.2
024500          30  ALPHAN-KEY PIC X.                                   ST1474.2
024600          30  NUM-KEY PIC 999.                                    ST1474.2
024700     20  KEY-12.                                                  ST1474.2
024800          30  ALPHAN-KEY PIC X.                                   ST1474.2
024900          30  NUM-KEY PIC 999.                                    ST1474.2
025000 FD  SQ-FS5                                                       ST1474.2
025100     LABEL RECORD IS STANDARD                                     ST1474.2
025200C    ; VALUE OF                                                   ST1474.2
025300C    XXXXX074                                                     ST1474.2
025400C    IS                                                           ST1474.2
025500C    XXXXX079                                                     ST1474.2
025600*    XXXXX069                                                     ST1474.2
025700     ; BLOCK CONTAINS 3 RECORDS                                   ST1474.2
025800     RECORD CONTAINS 132 CHARACTERS                               ST1474.2
025900     DATA RECORD SQ-FS5R1-F-G-132.                                ST1474.2
026000 01  SQ-FS5R1-F-G-132.                                            ST1474.2
026100     10  REC-PRE-5 PIC X(120).                                    ST1474.2
026200     10  REST-OF-5.                                               ST1474.2
026300     20  KEY-13.                                                  ST1474.2
026400          30  ALPHAN-KEY PIC X.                                   ST1474.2
026500          30  NUM-KEY PIC 999.                                    ST1474.2
026600     20  KEY-14.                                                  ST1474.2
026700          30  ALPHAN-KEY PIC X.                                   ST1474.2
026800          30  NUM-KEY PIC 999.                                    ST1474.2
026900     20  KEY-15.                                                  ST1474.2
027000          30  ALPHAN-KEY PIC X.                                   ST1474.2
027100          30  NUM-KEY PIC 999.                                    ST1474.2
027200 SD  ST-FS1                                                       ST1474.2
027300     RECORD CONTAINS 132 CHARACTERS                               ST1474.2
027400     DATA RECORD IS ST-FS1R1-F-G-132.                             ST1474.2
027500 01  ST-FS1R1-F-G-132.                                            ST1474.2
027600     02 FILLER PIC X(120).                                        ST1474.2
027700     02 NON-KEY-1.                                                ST1474.2
027800         03 A-KEY PIC X.                                          ST1474.2
027900         03 N-KEY PIC 999.                                        ST1474.2
028000     02 SORT-KEY.                                                 ST1474.2
028100         03 A-KEY PIC X.                                          ST1474.2
028200         03 N-KEY PIC 999.                                        ST1474.2
028300     02 NON-KEY-2.                                                ST1474.2
028400         03 A-KEY PIC X.                                          ST1474.2
028500         03 N-KEY PIC 999.                                        ST1474.2
028600 WORKING-STORAGE SECTION.                                         ST1474.2
028700 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1474.2
028800 77  WRK-DU-999-0001 PIC 999.                                     ST1474.2
028900 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1474.2
029000 77  LAST-REC-NUM  PIC 999 VALUE ZERO.                            ST1474.2
029100 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1474.2
029200     XXXXX063.                                                    ST1474.2
029300 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1474.2
029400     02 CHAR PIC X OCCURS 51 TIMES.                               ST1474.2
029500 01  WRK-XN-2    PIC X(51) VALUE                                  ST1474.2
029600     XXXXX063.                                                    ST1474.2
029700 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1474.2
029800     02 ASCIIS PIC X OCCURS 51 TIMES.                             ST1474.2
029900 01  WRK-XN-O020F-0001.                                           ST1474.2
030000     02 COMPU PIC X OCCURS 20 TIMES.                              ST1474.2
030100 01  WRK-XN-X-0001 REDEFINES WRK-XN-O020F-0001.                   ST1474.2
030200     02 FILLER PIC X(20).                                         ST1474.2
030300 01  WRK-XN-O120F-1.                                              ST1474.2
030400     02 COLLS PIC X OCCURS 120 TIMES.                             ST1474.2
030500 01  WRK-XN-X-2 REDEFINES WRK-XN-O120F-1.                         ST1474.2
030600     02 WRK-XN-0002 PIC X(20).                                    ST1474.2
030700     02 WRK-XN-0003 PIC X(20).                                    ST1474.2
030800     02 WRK-XN-0004 PIC X(20).                                    ST1474.2
030900     02 WRK-XN-0005 PIC X(20).                                    ST1474.2
031000     02 WRK-XN-0006 PIC X(20).                                    ST1474.2
031100     02 WRK-XN-0007 PIC X(20).                                    ST1474.2
031200 01  FILE-RECORD-INFORMATION-REC.                                 ST1474.2
031300     03 FILE-RECORD-INFO-SKELETON.                                ST1474.2
031400        05 FILLER                 PICTURE X(48)       VALUE       ST1474.2
031500             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1474.2
031600        05 FILLER                 PICTURE X(46)       VALUE       ST1474.2
031700             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1474.2
031800        05 FILLER                 PICTURE X(26)       VALUE       ST1474.2
031900             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1474.2
032000        05 FILLER                 PICTURE X(37)       VALUE       ST1474.2
032100             ",RECKEY=                             ".             ST1474.2
032200        05 FILLER                 PICTURE X(38)       VALUE       ST1474.2
032300             ",ALTKEY1=                             ".            ST1474.2
032400        05 FILLER                 PICTURE X(38)       VALUE       ST1474.2
032500             ",ALTKEY2=                             ".            ST1474.2
032600        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1474.2
032700     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1474.2
032800        05 FILE-RECORD-INFO-P1-120.                               ST1474.2
032900           07 FILLER              PIC X(5).                       ST1474.2
033000           07 XFILE-NAME           PIC X(6).                      ST1474.2
033100           07 FILLER              PIC X(8).                       ST1474.2
033200           07 XRECORD-NAME         PIC X(6).                      ST1474.2
033300           07 FILLER              PIC X(1).                       ST1474.2
033400           07 REELUNIT-NUMBER     PIC 9(1).                       ST1474.2
033500           07 FILLER              PIC X(7).                       ST1474.2
033600           07 XRECORD-NUMBER       PIC 9(6).                      ST1474.2
033700           07 FILLER              PIC X(6).                       ST1474.2
033800           07 UPDATE-NUMBER       PIC 9(2).                       ST1474.2
033900           07 FILLER              PIC X(5).                       ST1474.2
034000           07 ODO-NUMBER          PIC 9(4).                       ST1474.2
034100           07 FILLER              PIC X(5).                       ST1474.2
034200           07 XPROGRAM-NAME        PIC X(5).                      ST1474.2
034300           07 FILLER              PIC X(7).                       ST1474.2
034400           07 XRECORD-LENGTH       PIC 9(6).                      ST1474.2
034500           07 FILLER              PIC X(7).                       ST1474.2
034600           07 CHARS-OR-RECORDS    PIC X(2).                       ST1474.2
034700           07 FILLER              PIC X(1).                       ST1474.2
034800           07 XBLOCK-SIZE          PIC 9(4).                      ST1474.2
034900           07 FILLER              PIC X(6).                       ST1474.2
035000           07 RECORDS-IN-FILE     PIC 9(6).                       ST1474.2
035100           07 FILLER              PIC X(5).                       ST1474.2
035200           07 XFILE-ORGANIZATION   PIC X(2).                      ST1474.2
035300           07 FILLER              PIC X(6).                       ST1474.2
035400           07 XLABEL-TYPE          PIC X(1).                      ST1474.2
035500        05 FILE-RECORD-INFO-P121-240.                             ST1474.2
035600           07 FILLER              PIC X(8).                       ST1474.2
035700           07 XRECORD-KEY          PIC X(29).                     ST1474.2
035800           07 FILLER              PIC X(9).                       ST1474.2
035900           07 ALTERNATE-KEY1      PIC X(29).                      ST1474.2
036000           07 FILLER              PIC X(9).                       ST1474.2
036100           07 ALTERNATE-KEY2      PIC X(29).                      ST1474.2
036200           07 FILLER              PIC X(7).                       ST1474.2
036300 01  TEST-RESULTS.                                                ST1474.2
036400     02 FILLER                   PIC X      VALUE SPACE.          ST1474.2
036500     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1474.2
036600     02 FILLER                   PIC X      VALUE SPACE.          ST1474.2
036700     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1474.2
036800     02 FILLER                   PIC X      VALUE SPACE.          ST1474.2
036900     02  PAR-NAME.                                                ST1474.2
037000       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1474.2
037100       03  PARDOT-X              PIC X      VALUE SPACE.          ST1474.2
037200       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1474.2
037300     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1474.2
037400     02 RE-MARK                  PIC X(61).                       ST1474.2
037500 01  TEST-COMPUTED.                                               ST1474.2
037600     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1474.2
037700     02 FILLER                   PIC X(17)  VALUE                 ST1474.2
037800            "       COMPUTED=".                                   ST1474.2
037900     02 COMPUTED-X.                                               ST1474.2
038000     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1474.2
038100     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1474.2
038200                                 PIC -9(9).9(9).                  ST1474.2
038300     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1474.2
038400     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1474.2
038500     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1474.2
038600     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1474.2
038700         04 COMPUTED-18V0                    PIC -9(18).          ST1474.2
038800         04 FILLER                           PIC X.               ST1474.2
038900     03 FILLER PIC X(50) VALUE SPACE.                             ST1474.2
039000 01  TEST-CORRECT.                                                ST1474.2
039100     02 FILLER PIC X(30) VALUE SPACE.                             ST1474.2
039200     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1474.2
039300     02 CORRECT-X.                                                ST1474.2
039400     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1474.2
039500     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1474.2
039600     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1474.2
039700     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1474.2
039800     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1474.2
039900     03      CR-18V0 REDEFINES CORRECT-A.                         ST1474.2
040000         04 CORRECT-18V0                     PIC -9(18).          ST1474.2
040100         04 FILLER                           PIC X.               ST1474.2
040200     03 FILLER PIC X(2) VALUE SPACE.                              ST1474.2
040300     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1474.2
040400 01  CCVS-C-1.                                                    ST1474.2
040500     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1474.2
040600-    "SS  PARAGRAPH-NAME                                          ST1474.2
040700-    "       REMARKS".                                            ST1474.2
040800     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1474.2
040900 01  CCVS-C-2.                                                    ST1474.2
041000     02 FILLER                     PIC X        VALUE SPACE.      ST1474.2
041100     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1474.2
041200     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1474.2
041300     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1474.2
041400     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1474.2
041500 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1474.2
041600 01  REC-CT                        PIC 99       VALUE ZERO.       ST1474.2
041700 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1474.2
041800 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1474.2
041900 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1474.2
042000 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1474.2
042100 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1474.2
042200 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1474.2
042300 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1474.2
042400 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1474.2
042500 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1474.2
042600 01  CCVS-H-1.                                                    ST1474.2
042700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1474.2
042800     02  FILLER                    PIC X(42)    VALUE             ST1474.2
042900     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1474.2
043000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1474.2
043100 01  CCVS-H-2A.                                                   ST1474.2
043200   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1474.2
043300   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1474.2
043400   02  FILLER                        PIC XXXX   VALUE             ST1474.2
043500     "4.2 ".                                                      ST1474.2
043600   02  FILLER                        PIC X(28)  VALUE             ST1474.2
043700            " COPY - NOT FOR DISTRIBUTION".                       ST1474.2
043800   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1474.2
043900                                                                  ST1474.2
044000 01  CCVS-H-2B.                                                   ST1474.2
044100   02  FILLER                        PIC X(15)  VALUE             ST1474.2
044200            "TEST RESULT OF ".                                    ST1474.2
044300   02  TEST-ID                       PIC X(9).                    ST1474.2
044400   02  FILLER                        PIC X(4)   VALUE             ST1474.2
044500            " IN ".                                               ST1474.2
044600   02  FILLER                        PIC X(12)  VALUE             ST1474.2
044700     " HIGH       ".                                              ST1474.2
044800   02  FILLER                        PIC X(22)  VALUE             ST1474.2
044900            " LEVEL VALIDATION FOR ".                             ST1474.2
045000   02  FILLER                        PIC X(58)  VALUE             ST1474.2
045100     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1474.2
045200 01  CCVS-H-3.                                                    ST1474.2
045300     02  FILLER                      PIC X(34)  VALUE             ST1474.2
045400            " FOR OFFICIAL USE ONLY    ".                         ST1474.2
045500     02  FILLER                      PIC X(58)  VALUE             ST1474.2
045600     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1474.2
045700     02  FILLER                      PIC X(28)  VALUE             ST1474.2
045800            "  COPYRIGHT   1985 ".                                ST1474.2
045900 01  CCVS-E-1.                                                    ST1474.2
046000     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1474.2
046100     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1474.2
046200     02 ID-AGAIN                     PIC X(9).                    ST1474.2
046300     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1474.2
046400 01  CCVS-E-2.                                                    ST1474.2
046500     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1474.2
046600     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1474.2
046700     02 CCVS-E-2-2.                                               ST1474.2
046800         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1474.2
046900         03 FILLER                   PIC X      VALUE SPACE.      ST1474.2
047000         03 ENDER-DESC               PIC X(44)  VALUE             ST1474.2
047100            "ERRORS ENCOUNTERED".                                 ST1474.2
047200 01  CCVS-E-3.                                                    ST1474.2
047300     02  FILLER                      PIC X(22)  VALUE             ST1474.2
047400            " FOR OFFICIAL USE ONLY".                             ST1474.2
047500     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1474.2
047600     02  FILLER                      PIC X(58)  VALUE             ST1474.2
047700     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1474.2
047800     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1474.2
047900     02 FILLER                       PIC X(15)  VALUE             ST1474.2
048000             " COPYRIGHT 1985".                                   ST1474.2
048100 01  CCVS-E-4.                                                    ST1474.2
048200     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1474.2
048300     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1474.2
048400     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1474.2
048500     02 FILLER                       PIC X(40)  VALUE             ST1474.2
048600      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1474.2
048700 01  XXINFO.                                                      ST1474.2
048800     02 FILLER                       PIC X(19)  VALUE             ST1474.2
048900            "*** INFORMATION ***".                                ST1474.2
049000     02 INFO-TEXT.                                                ST1474.2
049100       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1474.2
049200       04 XXCOMPUTED                 PIC X(20).                   ST1474.2
049300       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1474.2
049400       04 XXCORRECT                  PIC X(20).                   ST1474.2
049500     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1474.2
049600 01  HYPHEN-LINE.                                                 ST1474.2
049700     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1474.2
049800     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1474.2
049900-    "*****************************************".                 ST1474.2
050000     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1474.2
050100-    "******************************".                            ST1474.2
050200 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1474.2
050300     "ST147A".                                                    ST1474.2
050400 PROCEDURE DIVISION.                                              ST1474.2
050500 DECLARATIVES.                                                    ST1474.2
050600 SECT-ST209-DEC SECTION.                                          ST1474.2
050700     USE AFTER STANDARD ERROR PROCEDURE ON OUTPUT.                ST1474.2
050800 MRG-WRITE-DEC.                                                   ST1474.2
050900     MOVE "ERROR ON OUTPUT DECL." TO FEATURE.                     ST1474.2
051000     MOVE "MRG-TEST-DEC" TO PAR-NAME.                             ST1474.2
051100     WRITE   PRINT-REC FROM TEST-RESULTS AFTER ADVANCING 2 LINES. ST1474.2
051200     STOP RUN.                                                    ST1474.2
051300 END DECLARATIVES.                                                ST1474.2
051400 CCVS1 SECTION.                                                   ST1474.2
051500 OPEN-FILES.                                                      ST1474.2
051600     OPEN    OUTPUT PRINT-FILE.                                   ST1474.2
051700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1474.2
051800     MOVE    SPACE TO TEST-RESULTS.                               ST1474.2
051900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1474.2
052000     MOVE    ZERO TO REC-SKL-SUB.                                 ST1474.2
052100     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1474.2
052200 CCVS-INIT-FILE.                                                  ST1474.2
052300     ADD     1 TO REC-SKL-SUB.                                    ST1474.2
052400     MOVE    FILE-RECORD-INFO-SKELETON                            ST1474.2
052500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1474.2
052600 CCVS-INIT-EXIT.                                                  ST1474.2
052700     GO TO CCVS1-EXIT.                                            ST1474.2
052800 CLOSE-FILES.                                                     ST1474.2
052900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1474.2
053000 TERMINATE-CCVS.                                                  ST1474.2
053100     STOP     RUN.                                                ST1474.2
053200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1474.2
053300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1474.2
053400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1474.2
053500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1474.2
053600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1474.2
053700 PRINT-DETAIL.                                                    ST1474.2
053800     IF REC-CT NOT EQUAL TO ZERO                                  ST1474.2
053900             MOVE "." TO PARDOT-X                                 ST1474.2
054000             MOVE REC-CT TO DOTVALUE.                             ST1474.2
054100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1474.2
054200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1474.2
054300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1474.2
054400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1474.2
054500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1474.2
054600     MOVE SPACE TO CORRECT-X.                                     ST1474.2
054700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1474.2
054800     MOVE     SPACE TO RE-MARK.                                   ST1474.2
054900 HEAD-ROUTINE.                                                    ST1474.2
055000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1474.2
055100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1474.2
055200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1474.2
055300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1474.2
055400 COLUMN-NAMES-ROUTINE.                                            ST1474.2
055500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1474.2
055600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1474.2
055700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1474.2
055800 END-ROUTINE.                                                     ST1474.2
055900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1474.2
056000 END-RTN-EXIT.                                                    ST1474.2
056100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1474.2
056200 END-ROUTINE-1.                                                   ST1474.2
056300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1474.2
056400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1474.2
056500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1474.2
056600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1474.2
056700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1474.2
056800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1474.2
056900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1474.2
057000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1474.2
057100  END-ROUTINE-12.                                                 ST1474.2
057200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1474.2
057300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1474.2
057400         MOVE "NO " TO ERROR-TOTAL                                ST1474.2
057500         ELSE                                                     ST1474.2
057600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1474.2
057700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1474.2
057800     PERFORM WRITE-LINE.                                          ST1474.2
057900 END-ROUTINE-13.                                                  ST1474.2
058000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1474.2
058100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1474.2
058200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1474.2
058300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1474.2
058400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1474.2
058500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1474.2
058600          MOVE "NO " TO ERROR-TOTAL                               ST1474.2
058700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1474.2
058800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1474.2
058900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1474.2
059000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1474.2
059100 WRITE-LINE.                                                      ST1474.2
059200     ADD 1 TO RECORD-COUNT.                                       ST1474.2
059300*    IF RECORD-COUNT GREATER 42                                   ST1474.2
059400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1474.2
059500*        MOVE SPACE TO DUMMY-RECORD                               ST1474.2
059600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1474.2
059700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1474.2
059800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1474.2
059900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1474.2
060000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1474.2
060100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1474.2
060200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1474.2
060300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1474.2
060400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1474.2
060500*        MOVE ZERO TO RECORD-COUNT.                               ST1474.2
060600     PERFORM WRT-LN.                                              ST1474.2
060700 WRT-LN.                                                          ST1474.2
060800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1474.2
060900     MOVE SPACE TO DUMMY-RECORD.                                  ST1474.2
061000 BLANK-LINE-PRINT.                                                ST1474.2
061100     PERFORM WRT-LN.                                              ST1474.2
061200 FAIL-ROUTINE.                                                    ST1474.2
061300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1474.2
061400            GO TO   FAIL-ROUTINE-WRITE.                           ST1474.2
061500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1474.2
061600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1474.2
061700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1474.2
061800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1474.2
061900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1474.2
062000     GO TO  FAIL-ROUTINE-EX.                                      ST1474.2
062100 FAIL-ROUTINE-WRITE.                                              ST1474.2
062200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1474.2
062300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1474.2
062400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1474.2
062500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1474.2
062600 FAIL-ROUTINE-EX. EXIT.                                           ST1474.2
062700 BAIL-OUT.                                                        ST1474.2
062800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1474.2
062900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1474.2
063000 BAIL-OUT-WRITE.                                                  ST1474.2
063100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1474.2
063200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1474.2
063300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1474.2
063400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1474.2
063500 BAIL-OUT-EX. EXIT.                                               ST1474.2
063600 CCVS1-EXIT.                                                      ST1474.2
063700     EXIT.                                                        ST1474.2
063800 SECT-ST417-001 SECTION.                                          ST1474.2
063900 MRG-INIT-001.                                                    ST1474.2
064000     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1474.2
064100     OPEN OUTPUT SQ-FS1.                                          ST1474.2
064200     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1474.2
064300     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1474.2
064400     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       ST1474.2
064500     MOVE 000132 TO XRECORD-LENGTH (1).                           ST1474.2
064600     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1474.2
064700     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1474.2
064800     MOVE 000051 TO RECORDS-IN-FILE (1).                          ST1474.2
064900     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1474.2
065000     MOVE "S" TO XLABEL-TYPE (1).                                 ST1474.2
065100     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1474.2
065200             MOVE SPACES TO WRK-XN-O120F-1.                       ST1474.2
065300 MRG-TEST-001.                                                    ST1474.2
065400     PERFORM MRG-TEST-001-BUILD VARYING WRK-DU-999-0001           ST1474.2
065500         FROM 51 BY -1 UNTIL WRK-DU-999-0001 IS LESS THAN 1.      ST1474.2
065600*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
065700*    WRITE   PRINT-REC.                                           ST1474.2
065800     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1474.2
065900         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1474.2
066000         ELSE                                                     ST1474.2
066100         PERFORM PASS.                                            ST1474.2
066200     GO TO MRG-WRITE-001.                                         ST1474.2
066300 MRG-TEST-001-BUILD.                                              ST1474.2
066400     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-1           ST1474.2
066500         ALPHAN-KEY OF KEY-2 ALPHAN-KEY OF KEY-3.                 ST1474.2
066600     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1474.2
066700         NUM-KEY OF KEY-3.                                        ST1474.2
066800     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1474.2
066900     ADD 1 TO XRECORD-NUMBER (1).                                 ST1474.2
067000     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1474.2
067100     ADD 1 TO WRK-DU-999-2.                                       ST1474.2
067200     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1474.2
067300     ADD 1 TO WRK-DU-999-2.                                       ST1474.2
067400*    WRITE PRINT-REC FROM SQ-FS1R1-F-G-132.                       ST1474.2
067500*    WRITE PRINT-REC FROM REST-OF-1.                              ST1474.2
067600*    MOVE SPACES TO PRINT-REC.                                    ST1474.2
067700     WRITE   SQ-FS1R1-F-G-132.                                    ST1474.2
067800 MRG-DELETE-001.                                                  ST1474.2
067900     PERFORM DE-LETE.                                             ST1474.2
068000 MRG-WRITE-001.                                                   ST1474.2
068100     MOVE "MRG-TEST-001" TO PAR-NAME.                             ST1474.2
068200     MOVE "FIRST FILE CREATED" TO COMPUTED-A.                     ST1474.2
068300     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1474.2
068400     PERFORM PRINT-DETAIL.                                        ST1474.2
068500*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
068600*    WRITE   PRINT-REC.                                           ST1474.2
068700     CLOSE SQ-FS1.                                                ST1474.2
068800 MRG-INIT-002.                                                    ST1474.2
068900     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        ST1474.2
069000     OPEN OUTPUT SQ-FS2.                                          ST1474.2
069100     MOVE  "SQ-FS2" TO XFILE-NAME (1).                            ST1474.2
069200     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1474.2
069300     MOVE 0002 TO XBLOCK-SIZE (1).                                ST1474.2
069400 MRG-TEST-002.                                                    ST1474.2
069500     PERFORM MRG-TEST-002-BUILD VARYING WRK-DU-999-0001           ST1474.2
069600         FROM 51 BY -1 UNTIL WRK-DU-999-0001 IS LESS THAN 1.      ST1474.2
069700*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
069800*    WRITE   PRINT-REC.                                           ST1474.2
069900     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 52                     ST1474.2
070000         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1474.2
070100         ELSE                                                     ST1474.2
070200         PERFORM PASS.                                            ST1474.2
070300     GO TO MRG-WRITE-002.                                         ST1474.2
070400 MRG-TEST-002-BUILD.                                              ST1474.2
070500     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-4           ST1474.2
070600         ALPHAN-KEY OF KEY-5 ALPHAN-KEY OF KEY-6.                 ST1474.2
070700     ADD 51 WRK-DU-999-0001 GIVING NUM-KEY OF KEY-4               ST1474.2
070800         NUM-KEY OF KEY-5 NUM-KEY OF KEY-6.                       ST1474.2
070900     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-2.               ST1474.2
071000     ADD 000001 TO XRECORD-NUMBER (1).                            ST1474.2
071100*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1474.2
071200*    WRITE PRINT-REC FROM REST-OF-2.                              ST1474.2
071300*    MOVE SPACES TO PRINT-REC.                                    ST1474.2
071400     WRITE   SQ-FS2R1-F-G-132.                                    ST1474.2
071500 MRG-DELETE-002.                                                  ST1474.2
071600     PERFORM DE-LETE.                                             ST1474.2
071700 MRG-WRITE-002.                                                   ST1474.2
071800     MOVE "MRG-TEST-002" TO PAR-NAME.                             ST1474.2
071900     MOVE "2ND FILE CREATED" TO COMPUTED-A.                       ST1474.2
072000     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1474.2
072100     PERFORM PRINT-DETAIL.                                        ST1474.2
072200*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
072300*    WRITE   PRINT-REC.                                           ST1474.2
072400     CLOSE SQ-FS2.                                                ST1474.2
072500 MRG-INIT-003.                                                    ST1474.2
072600*    ==-->  MULTIPLE "GIVING" FILES  <--==                        ST1474.2
072700     MOVE   "XI-11 4.1.4 GR (11)" TO ANSI-REFERENCE.              ST1474.2
072800     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
072900     MERGE ST-FS1                                                 ST1474.2
073000         DESCENDING A-KEY OF SORT-KEY                             ST1474.2
073100         ON DESCENDING KEY N-KEY OF NON-KEY-1                     ST1474.2
073200         USING  SQ-FS2 SQ-FS1                                     ST1474.2
073300         GIVING SQ-FS3 SQ-FS4 SQ-FS5.                             ST1474.2
073400 MRG-TEST-003.                                                    ST1474.2
073500     OPEN INPUT SQ-FS3.                                           ST1474.2
073600     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
073700         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
073800*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
073900*    WRITE   PRINT-REC.                                           ST1474.2
074000     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1474.2
074100         PERFORM FAIL GO TO MRG-FAIL-003                          ST1474.2
074200         ELSE                                                     ST1474.2
074300         PERFORM PASS.                                            ST1474.2
074400     GO TO MRG-WRITE-003.                                         ST1474.2
074500 MRG-DELETE-003.                                                  ST1474.2
074600     PERFORM DE-LETE.                                             ST1474.2
074700     GO TO MRG-WRITE-003.                                         ST1474.2
074800 MRG-FAIL-003.                                                    ST1474.2
074900     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
075000     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1474.2
075100 MRG-WRITE-003.                                                   ST1474.2
075200     MOVE "MRG-TEST-003" TO PAR-NAME.                             ST1474.2
075300     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
075400     PERFORM PRINT-DETAIL.                                        ST1474.2
075500*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
075600*    WRITE   PRINT-REC.                                           ST1474.2
075700 MRG-INIT-004.                                                    ST1474.2
075800     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
075900 MRG-TEST-004.                                                    ST1474.2
076000     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
076100         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
076200*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
076300*    WRITE   PRINT-REC.                                           ST1474.2
076400     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1474.2
076500         PERFORM FAIL GO TO MRG-FAIL-004                          ST1474.2
076600         ELSE                                                     ST1474.2
076700         PERFORM PASS.                                            ST1474.2
076800     GO TO MRG-WRITE-004.                                         ST1474.2
076900 MRG-DELETE-004.                                                  ST1474.2
077000     PERFORM DE-LETE.                                             ST1474.2
077100     GO TO MRG-WRITE-004.                                         ST1474.2
077200 MRG-FAIL-004.                                                    ST1474.2
077300     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
077400     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1474.2
077500 MRG-WRITE-004.                                                   ST1474.2
077600     MOVE "MRG-TEST-004" TO PAR-NAME.                             ST1474.2
077700     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
077800     PERFORM PRINT-DETAIL.                                        ST1474.2
077900*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
078000*    WRITE   PRINT-REC.                                           ST1474.2
078100 MRG-INIT-005.                                                    ST1474.2
078200     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
078300 MRG-TEST-005.                                                    ST1474.2
078400     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
078500         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
078600*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
078700*    WRITE   PRINT-REC.                                           ST1474.2
078800     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1474.2
078900         PERFORM FAIL GO TO MRG-FAIL-005                          ST1474.2
079000         ELSE                                                     ST1474.2
079100         PERFORM PASS.                                            ST1474.2
079200     GO TO MRG-WRITE-005.                                         ST1474.2
079300 MRG-DELETE-005.                                                  ST1474.2
079400     PERFORM DE-LETE.                                             ST1474.2
079500     GO TO MRG-WRITE-005.                                         ST1474.2
079600 MRG-FAIL-005.                                                    ST1474.2
079700     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
079800     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1474.2
079900 MRG-WRITE-005.                                                   ST1474.2
080000     MOVE "MRG-TEST-005" TO PAR-NAME.                             ST1474.2
080100     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
080200     PERFORM PRINT-DETAIL.                                        ST1474.2
080300*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
080400*    WRITE   PRINT-REC.                                           ST1474.2
080500 MRG-INIT-006.                                                    ST1474.2
080600     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
080700 MRG-TEST-006.                                                    ST1474.2
080800     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
080900         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
081000*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
081100*    WRITE   PRINT-REC.                                           ST1474.2
081200     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1474.2
081300         PERFORM FAIL GO TO MRG-FAIL-006                          ST1474.2
081400         ELSE                                                     ST1474.2
081500         PERFORM PASS.                                            ST1474.2
081600     GO TO MRG-WRITE-006.                                         ST1474.2
081700 MRG-DELETE-006.                                                  ST1474.2
081800     PERFORM DE-LETE.                                             ST1474.2
081900     GO TO MRG-WRITE-006.                                         ST1474.2
082000 MRG-FAIL-006.                                                    ST1474.2
082100     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
082200     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1474.2
082300 MRG-WRITE-006.                                                   ST1474.2
082400     MOVE "MRG-TEST-006" TO PAR-NAME.                             ST1474.2
082500     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
082600     PERFORM PRINT-DETAIL.                                        ST1474.2
082700*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
082800*    WRITE   PRINT-REC.                                           ST1474.2
082900 MRG-INIT-007.                                                    ST1474.2
083000     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
083100 MRG-TEST-007.                                                    ST1474.2
083200     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
083300         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
083400*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
083500*    WRITE   PRINT-REC.                                           ST1474.2
083600     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1474.2
083700         PERFORM FAIL GO TO MRG-FAIL-007                          ST1474.2
083800         ELSE                                                     ST1474.2
083900         PERFORM PASS.                                            ST1474.2
084000     GO TO MRG-WRITE-007.                                         ST1474.2
084100 MRG-DELETE-007.                                                  ST1474.2
084200     PERFORM DE-LETE.                                             ST1474.2
084300     GO TO MRG-WRITE-007.                                         ST1474.2
084400 MRG-FAIL-007.                                                    ST1474.2
084500     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
084600     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1474.2
084700 MRG-WRITE-007.                                                   ST1474.2
084800     MOVE "MRG-TEST-007" TO PAR-NAME.                             ST1474.2
084900     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
085000     PERFORM PRINT-DETAIL.                                        ST1474.2
085100*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
085200*    WRITE   PRINT-REC.                                           ST1474.2
085300 MRG-INIT-008.                                                    ST1474.2
085400     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
085500 MRG-TEST-008.                                                    ST1474.2
085600     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
085700         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1474.2
085800*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
085900*    WRITE   PRINT-REC.                                           ST1474.2
086000     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1474.2
086100         PERFORM FAIL GO TO MRG-FAIL-008                          ST1474.2
086200         ELSE                                                     ST1474.2
086300         PERFORM PASS.                                            ST1474.2
086400     GO TO MRG-WRITE-008.                                         ST1474.2
086500 MRG-DELETE-008.                                                  ST1474.2
086600     PERFORM DE-LETE.                                             ST1474.2
086700     GO TO MRG-WRITE-008.                                         ST1474.2
086800 MRG-FAIL-008.                                                    ST1474.2
086900     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
087000     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1474.2
087100 MRG-WRITE-008.                                                   ST1474.2
087200     MOVE "MRG-TEST-008" TO PAR-NAME.                             ST1474.2
087300     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
087400     PERFORM PRINT-DETAIL.                                        ST1474.2
087500     MOVE    NUM-KEY OF KEY-7 TO LAST-REC-NUM.                    ST1474.2
087600 MRG-TEST-009.                                                    ST1474.2
087700     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
087800         GO TO MRG-FAIL-009.                                      ST1474.2
087900     READ SQ-FS3 END PERFORM PASS                                 ST1474.2
088000         GO TO MRG-WRITE-009.                                     ST1474.2
088100     GO TO MRG-FAIL-009.                                          ST1474.2
088200 MRG-DELETE-009.                                                  ST1474.2
088300     PERFORM DE-LETE.                                             ST1474.2
088400     GO TO MRG-WRITE-009.                                         ST1474.2
088500 MRG-FAIL-009.                                                    ST1474.2
088600     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1474.2
088700     PERFORM FAIL .                                               ST1474.2
088800 MRG-WRITE-009.                                                   ST1474.2
088900     MOVE "MRG-TEST-009" TO PAR-NAME.                             ST1474.2
089000     MOVE "EOF CHECK SQ-FS3" TO FEATURE.                          ST1474.2
089100     PERFORM PRINT-DETAIL.                                        ST1474.2
089200 MRG-TEST-010.                                                    ST1474.2
089300     IF LAST-REC-NUM IS NOT EQUAL TO 1                            ST1474.2
089400         PERFORM FAIL GO TO MRG-FAIL-010                          ST1474.2
089500         ELSE                                                     ST1474.2
089600         PERFORM PASS.                                            ST1474.2
089700     GO TO MRG-WRITE-010.                                         ST1474.2
089800 MRG-DELETE-010.                                                  ST1474.2
089900     PERFORM DE-LETE.                                             ST1474.2
090000     GO TO MRG-WRITE-010.                                         ST1474.2
090100 MRG-FAIL-010.                                                    ST1474.2
090200     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1474.2
090300     MOVE 1 TO CR-18V0.                                           ST1474.2
090400 MRG-WRITE-010.                                                   ST1474.2
090500     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1474.2
090600     MOVE "MRG-TEST-010" TO PAR-NAME.                             ST1474.2
090700     PERFORM PRINT-DETAIL.                                        ST1474.2
090800     CLOSE   SQ-FS3.                                              ST1474.2
090900     GO TO   MRG-TEST-011.                                        ST1474.2
091000 READ-SQ-FS3 SECTION.                                             ST1474.2
091100 RD-1.                                                            ST1474.2
091200     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
091300         GO TO R1-EXIT.                                           ST1474.2
091400     READ SQ-FS3 AT END GO TO PREMATURE-EOF-1.                    ST1474.2
091500*    WRITE PRINT-REC FROM SQ-FS3R1-F-G-132.                       ST1474.2
091600*    WRITE PRINT-REC FROM REST-OF-3.                              ST1474.2
091700*    MOVE SPACES TO PRINT-REC.                                    ST1474.2
091800     MOVE ALPHAN-KEY OF KEY-8 TO COMPU (WRK-DU-999-0001).         ST1474.2
091900     GO TO R1-EXIT.                                               ST1474.2
092000 PREMATURE-EOF-1.                                                 ST1474.2
092100     MOVE 1 TO WRK-DU-9-0001.                                     ST1474.2
092200     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1474.2
092300 R1-EXIT.                                                         ST1474.2
092400     EXIT.                                                        ST1474.2
092500*                                                                 ST1474.2
092600*                                                                 ST1474.2
092700 MRG-TEST-011.                                                    ST1474.2
092800     OPEN INPUT SQ-FS4.                                           ST1474.2
092900     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
093000         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
093100*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
093200*    WRITE   PRINT-REC.                                           ST1474.2
093300     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1474.2
093400         PERFORM FAIL GO TO MRG-FAIL-011                          ST1474.2
093500         ELSE                                                     ST1474.2
093600         PERFORM PASS.                                            ST1474.2
093700     GO TO MRG-WRITE-011.                                         ST1474.2
093800 MRG-DELETE-011.                                                  ST1474.2
093900     PERFORM DE-LETE.                                             ST1474.2
094000     GO TO MRG-WRITE-011.                                         ST1474.2
094100 MRG-FAIL-011.                                                    ST1474.2
094200     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
094300     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1474.2
094400 MRG-WRITE-011.                                                   ST1474.2
094500     MOVE "MRG-TEST-011" TO PAR-NAME.                             ST1474.2
094600     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
094700     PERFORM PRINT-DETAIL.                                        ST1474.2
094800*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
094900*    WRITE   PRINT-REC.                                           ST1474.2
095000 MRG-INIT-012.                                                    ST1474.2
095100     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
095200 MRG-TEST-012.                                                    ST1474.2
095300     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
095400         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
095500*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
095600*    WRITE   PRINT-REC.                                           ST1474.2
095700     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1474.2
095800         PERFORM FAIL GO TO MRG-FAIL-012                          ST1474.2
095900         ELSE                                                     ST1474.2
096000         PERFORM PASS.                                            ST1474.2
096100     GO TO MRG-WRITE-012.                                         ST1474.2
096200 MRG-DELETE-012.                                                  ST1474.2
096300     PERFORM DE-LETE.                                             ST1474.2
096400     GO TO MRG-WRITE-012.                                         ST1474.2
096500 MRG-FAIL-012.                                                    ST1474.2
096600     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
096700     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1474.2
096800 MRG-WRITE-012.                                                   ST1474.2
096900     MOVE "MRG-TEST-012" TO PAR-NAME.                             ST1474.2
097000     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
097100     PERFORM PRINT-DETAIL.                                        ST1474.2
097200*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
097300*    WRITE   PRINT-REC.                                           ST1474.2
097400 MRG-INIT-013.                                                    ST1474.2
097500     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
097600 MRG-TEST-013.                                                    ST1474.2
097700     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
097800         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
097900*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
098000*    WRITE   PRINT-REC.                                           ST1474.2
098100     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1474.2
098200         PERFORM FAIL GO TO MRG-FAIL-013                          ST1474.2
098300         ELSE                                                     ST1474.2
098400         PERFORM PASS.                                            ST1474.2
098500     GO TO MRG-WRITE-013.                                         ST1474.2
098600 MRG-DELETE-013.                                                  ST1474.2
098700     PERFORM DE-LETE.                                             ST1474.2
098800     GO TO MRG-WRITE-013.                                         ST1474.2
098900 MRG-FAIL-013.                                                    ST1474.2
099000     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
099100     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1474.2
099200 MRG-WRITE-013.                                                   ST1474.2
099300     MOVE "MRG-TEST-013" TO PAR-NAME.                             ST1474.2
099400     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
099500     PERFORM PRINT-DETAIL.                                        ST1474.2
099600*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
099700*    WRITE   PRINT-REC.                                           ST1474.2
099800 MRG-INIT-014.                                                    ST1474.2
099900     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
100000 MRG-TEST-014.                                                    ST1474.2
100100     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
100200         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
100300*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
100400*    WRITE   PRINT-REC.                                           ST1474.2
100500     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1474.2
100600         PERFORM FAIL GO TO MRG-FAIL-014                          ST1474.2
100700         ELSE                                                     ST1474.2
100800         PERFORM PASS.                                            ST1474.2
100900     GO TO MRG-WRITE-014.                                         ST1474.2
101000 MRG-DELETE-014.                                                  ST1474.2
101100     PERFORM DE-LETE.                                             ST1474.2
101200     GO TO MRG-WRITE-014.                                         ST1474.2
101300 MRG-FAIL-014.                                                    ST1474.2
101400     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
101500     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1474.2
101600 MRG-WRITE-014.                                                   ST1474.2
101700     MOVE "MRG-TEST-014" TO PAR-NAME.                             ST1474.2
101800     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
101900     PERFORM PRINT-DETAIL.                                        ST1474.2
102000*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
102100*    WRITE   PRINT-REC.                                           ST1474.2
102200 MRG-INIT-015.                                                    ST1474.2
102300     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
102400 MRG-TEST-015.                                                    ST1474.2
102500     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
102600         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
102700*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
102800*    WRITE   PRINT-REC.                                           ST1474.2
102900     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1474.2
103000         PERFORM FAIL GO TO MRG-FAIL-015                          ST1474.2
103100         ELSE                                                     ST1474.2
103200         PERFORM PASS.                                            ST1474.2
103300     GO TO MRG-WRITE-015.                                         ST1474.2
103400 MRG-DELETE-015.                                                  ST1474.2
103500     PERFORM DE-LETE.                                             ST1474.2
103600     GO TO MRG-WRITE-015.                                         ST1474.2
103700 MRG-FAIL-015.                                                    ST1474.2
103800     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
103900     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1474.2
104000 MRG-WRITE-015.                                                   ST1474.2
104100     MOVE "MRG-TEST-015" TO PAR-NAME.                             ST1474.2
104200     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
104300     PERFORM PRINT-DETAIL.                                        ST1474.2
104400*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
104500*    WRITE   PRINT-REC.                                           ST1474.2
104600 MRG-INIT-016.                                                    ST1474.2
104700     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
104800 MRG-TEST-016.                                                    ST1474.2
104900     PERFORM RD-2 THRU R2-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
105000         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1474.2
105100*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
105200*    WRITE   PRINT-REC.                                           ST1474.2
105300     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1474.2
105400         PERFORM FAIL GO TO MRG-FAIL-016                          ST1474.2
105500         ELSE                                                     ST1474.2
105600         PERFORM PASS.                                            ST1474.2
105700     GO TO MRG-WRITE-016.                                         ST1474.2
105800 MRG-DELETE-016.                                                  ST1474.2
105900     PERFORM DE-LETE.                                             ST1474.2
106000     GO TO MRG-WRITE-016.                                         ST1474.2
106100 MRG-FAIL-016.                                                    ST1474.2
106200     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
106300     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1474.2
106400 MRG-WRITE-016.                                                   ST1474.2
106500     MOVE "MRG-TEST-016" TO PAR-NAME.                             ST1474.2
106600     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
106700     PERFORM PRINT-DETAIL.                                        ST1474.2
106800     MOVE    NUM-KEY OF KEY-10 TO LAST-REC-NUM.                   ST1474.2
106900 MRG-TEST-017.                                                    ST1474.2
107000     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
107100         GO TO MRG-FAIL-017.                                      ST1474.2
107200     READ SQ-FS4 END PERFORM PASS                                 ST1474.2
107300         GO TO MRG-WRITE-017.                                     ST1474.2
107400     GO TO MRG-FAIL-017.                                          ST1474.2
107500 MRG-DELETE-017.                                                  ST1474.2
107600     PERFORM DE-LETE.                                             ST1474.2
107700     GO TO MRG-WRITE-017.                                         ST1474.2
107800 MRG-FAIL-017.                                                    ST1474.2
107900     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1474.2
108000     PERFORM FAIL .                                               ST1474.2
108100 MRG-WRITE-017.                                                   ST1474.2
108200     MOVE "MRG-TEST-017" TO PAR-NAME.                             ST1474.2
108300     MOVE "EOF CHECK SQ-FS4" TO FEATURE.                          ST1474.2
108400     PERFORM PRINT-DETAIL.                                        ST1474.2
108500 MRG-TEST-018.                                                    ST1474.2
108600     IF LAST-REC-NUM IS NOT EQUAL TO 1                            ST1474.2
108700         PERFORM FAIL GO TO MRG-FAIL-018                          ST1474.2
108800         ELSE                                                     ST1474.2
108900         PERFORM PASS.                                            ST1474.2
109000     GO TO MRG-WRITE-018.                                         ST1474.2
109100 MRG-DELETE-018.                                                  ST1474.2
109200     PERFORM DE-LETE.                                             ST1474.2
109300     GO TO MRG-WRITE-018.                                         ST1474.2
109400 MRG-FAIL-018.                                                    ST1474.2
109500     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1474.2
109600     MOVE 1 TO CR-18V0.                                           ST1474.2
109700 MRG-WRITE-018.                                                   ST1474.2
109800     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1474.2
109900     MOVE "MRG-TEST-018" TO PAR-NAME.                             ST1474.2
110000     PERFORM PRINT-DETAIL.                                        ST1474.2
110100     CLOSE   SQ-FS4.                                              ST1474.2
110200     GO TO MRG-TEST-019.                                          ST1474.2
110300 READ-SQ-FS4 SECTION.                                             ST1474.2
110400 RD-2.                                                            ST1474.2
110500     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
110600         GO TO R2-EXIT.                                           ST1474.2
110700     READ SQ-FS4 AT END GO TO PREMATURE-EOF-2.                    ST1474.2
110800*    WRITE PRINT-REC FROM SQ-FS4R1-F-G-132.                       ST1474.2
110900*    WRITE PRINT-REC FROM REST-OF-4.                              ST1474.2
111000*    MOVE SPACES TO PRINT-REC.                                    ST1474.2
111100     MOVE ALPHAN-KEY OF KEY-11 TO COMPU (WRK-DU-999-0001).        ST1474.2
111200     GO TO R2-EXIT.                                               ST1474.2
111300 PREMATURE-EOF-2.                                                 ST1474.2
111400     MOVE 1 TO WRK-DU-9-0001.                                     ST1474.2
111500     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1474.2
111600 R2-EXIT.                                                         ST1474.2
111700     EXIT.                                                        ST1474.2
111800*                                                                 ST1474.2
111900*                                                                 ST1474.2
112000 MRG-TEST-019.                                                    ST1474.2
112100     OPEN INPUT SQ-FS5.                                           ST1474.2
112200     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
112300         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
112400*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
112500*    WRITE   PRINT-REC.                                           ST1474.2
112600     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1474.2
112700         PERFORM FAIL GO TO MRG-FAIL-019                          ST1474.2
112800         ELSE                                                     ST1474.2
112900         PERFORM PASS.                                            ST1474.2
113000     GO TO MRG-WRITE-019.                                         ST1474.2
113100 MRG-DELETE-019.                                                  ST1474.2
113200     PERFORM DE-LETE.                                             ST1474.2
113300     GO TO MRG-WRITE-019.                                         ST1474.2
113400 MRG-FAIL-019.                                                    ST1474.2
113500     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
113600     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1474.2
113700 MRG-WRITE-019.                                                   ST1474.2
113800     MOVE "MRG-TEST-019" TO PAR-NAME.                             ST1474.2
113900     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
114000     PERFORM PRINT-DETAIL.                                        ST1474.2
114100*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
114200*    WRITE   PRINT-REC.                                           ST1474.2
114300 MRG-INIT-020.                                                    ST1474.2
114400     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
114500 MRG-TEST-020.                                                    ST1474.2
114600     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
114700         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
114800*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
114900*    WRITE   PRINT-REC.                                           ST1474.2
115000     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1474.2
115100         PERFORM FAIL GO TO MRG-FAIL-020                          ST1474.2
115200         ELSE                                                     ST1474.2
115300         PERFORM PASS.                                            ST1474.2
115400     GO TO MRG-WRITE-020.                                         ST1474.2
115500 MRG-DELETE-020.                                                  ST1474.2
115600     PERFORM DE-LETE.                                             ST1474.2
115700     GO TO MRG-WRITE-020.                                         ST1474.2
115800 MRG-FAIL-020.                                                    ST1474.2
115900     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
116000     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1474.2
116100 MRG-WRITE-020.                                                   ST1474.2
116200     MOVE "MRG-TEST-020" TO PAR-NAME.                             ST1474.2
116300     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
116400     PERFORM PRINT-DETAIL.                                        ST1474.2
116500*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
116600*    WRITE   PRINT-REC.                                           ST1474.2
116700 MRG-INIT-021.                                                    ST1474.2
116800     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
116900 MRG-TEST-021.                                                    ST1474.2
117000     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
117100         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
117200*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
117300*    WRITE   PRINT-REC.                                           ST1474.2
117400     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1474.2
117500         PERFORM FAIL GO TO MRG-FAIL-021                          ST1474.2
117600         ELSE                                                     ST1474.2
117700         PERFORM PASS.                                            ST1474.2
117800     GO TO MRG-WRITE-021.                                         ST1474.2
117900 MRG-DELETE-021.                                                  ST1474.2
118000     PERFORM DE-LETE.                                             ST1474.2
118100     GO TO MRG-WRITE-021.                                         ST1474.2
118200 MRG-FAIL-021.                                                    ST1474.2
118300     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
118400     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1474.2
118500 MRG-WRITE-021.                                                   ST1474.2
118600     MOVE "MRG-TEST-021" TO PAR-NAME.                             ST1474.2
118700     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
118800     PERFORM PRINT-DETAIL.                                        ST1474.2
118900*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
119000*    WRITE   PRINT-REC.                                           ST1474.2
119100 MRG-INIT-022.                                                    ST1474.2
119200     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
119300 MRG-TEST-022.                                                    ST1474.2
119400     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
119500         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
119600*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
119700*    WRITE   PRINT-REC.                                           ST1474.2
119800     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1474.2
119900         PERFORM FAIL GO TO MRG-FAIL-022                          ST1474.2
120000         ELSE                                                     ST1474.2
120100         PERFORM PASS.                                            ST1474.2
120200     GO TO MRG-WRITE-022.                                         ST1474.2
120300 MRG-DELETE-022.                                                  ST1474.2
120400     PERFORM DE-LETE.                                             ST1474.2
120500     GO TO MRG-WRITE-022.                                         ST1474.2
120600 MRG-FAIL-022.                                                    ST1474.2
120700     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
120800     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1474.2
120900 MRG-WRITE-022.                                                   ST1474.2
121000     MOVE "MRG-TEST-022" TO PAR-NAME.                             ST1474.2
121100     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
121200     PERFORM PRINT-DETAIL.                                        ST1474.2
121300*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
121400*    WRITE   PRINT-REC.                                           ST1474.2
121500 MRG-INIT-023.                                                    ST1474.2
121600     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
121700 MRG-TEST-023.                                                    ST1474.2
121800     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
121900         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1474.2
122000*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
122100*    WRITE   PRINT-REC.                                           ST1474.2
122200     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1474.2
122300         PERFORM FAIL GO TO MRG-FAIL-023                          ST1474.2
122400         ELSE                                                     ST1474.2
122500         PERFORM PASS.                                            ST1474.2
122600     GO TO MRG-WRITE-023.                                         ST1474.2
122700 MRG-DELETE-023.                                                  ST1474.2
122800     PERFORM DE-LETE.                                             ST1474.2
122900     GO TO MRG-WRITE-023.                                         ST1474.2
123000 MRG-FAIL-023.                                                    ST1474.2
123100     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
123200     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1474.2
123300 MRG-WRITE-023.                                                   ST1474.2
123400     MOVE "MRG-TEST-023" TO PAR-NAME.                             ST1474.2
123500     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
123600     PERFORM PRINT-DETAIL.                                        ST1474.2
123700*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
123800*    WRITE   PRINT-REC.                                           ST1474.2
123900 MRG-INIT-024.                                                    ST1474.2
124000     MOVE SPACES TO WRK-XN-X-0001.                                ST1474.2
124100 MRG-TEST-024.                                                    ST1474.2
124200     PERFORM RD-3 THRU R3-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1474.2
124300         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1474.2
124400*    MOVE    SPACES TO PRINT-REC.                                 ST1474.2
124500*    WRITE   PRINT-REC.                                           ST1474.2
124600     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1474.2
124700         PERFORM FAIL GO TO MRG-FAIL-024                          ST1474.2
124800         ELSE                                                     ST1474.2
124900         PERFORM PASS.                                            ST1474.2
125000     GO TO MRG-WRITE-024.                                         ST1474.2
125100 MRG-DELETE-024.                                                  ST1474.2
125200     PERFORM DE-LETE.                                             ST1474.2
125300     GO TO MRG-WRITE-024.                                         ST1474.2
125400 MRG-FAIL-024.                                                    ST1474.2
125500     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1474.2
125600     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1474.2
125700 MRG-WRITE-024.                                                   ST1474.2
125800     MOVE "MRG-TEST-024" TO PAR-NAME.                             ST1474.2
125900     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1474.2
126000     PERFORM PRINT-DETAIL.                                        ST1474.2
126100     MOVE    NUM-KEY OF KEY-13 TO LAST-REC-NUM.                   ST1474.2
126200 MRG-TEST-025.                                                    ST1474.2
126300     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
126400         GO TO MRG-FAIL-025.                                      ST1474.2
126500     READ SQ-FS5 END PERFORM PASS                                 ST1474.2
126600         GO TO MRG-WRITE-025.                                     ST1474.2
126700     GO TO MRG-FAIL-025.                                          ST1474.2
126800 MRG-DELETE-025.                                                  ST1474.2
126900     PERFORM DE-LETE.                                             ST1474.2
127000     GO TO MRG-WRITE-025.                                         ST1474.2
127100 MRG-FAIL-025.                                                    ST1474.2
127200     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1474.2
127300     PERFORM FAIL .                                               ST1474.2
127400 MRG-WRITE-025.                                                   ST1474.2
127500     MOVE "MRG-TEST-025" TO PAR-NAME.                             ST1474.2
127600     MOVE "EOF CHECK SQ-FS5" TO FEATURE.                          ST1474.2
127700     PERFORM PRINT-DETAIL.                                        ST1474.2
127800 MRG-TEST-026.                                                    ST1474.2
127900     IF LAST-REC-NUM IS NOT EQUAL TO 1                            ST1474.2
128000         PERFORM FAIL GO TO MRG-FAIL-026                          ST1474.2
128100         ELSE                                                     ST1474.2
128200         PERFORM PASS.                                            ST1474.2
128300     GO TO MRG-WRITE-026.                                         ST1474.2
128400 MRG-DELETE-026.                                                  ST1474.2
128500     PERFORM DE-LETE.                                             ST1474.2
128600     GO TO MRG-WRITE-026.                                         ST1474.2
128700 MRG-FAIL-026.                                                    ST1474.2
128800     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1474.2
128900     MOVE 1 TO CR-18V0.                                           ST1474.2
129000 MRG-WRITE-026.                                                   ST1474.2
129100     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1474.2
129200     MOVE "MRG-TEST-026" TO PAR-NAME.                             ST1474.2
129300     PERFORM PRINT-DETAIL.                                        ST1474.2
129400     CLOSE   SQ-FS5.                                              ST1474.2
129500     GO TO CLOSE-FILES.                                           ST1474.2
129600                                                                  ST1474.2
129700 READ-SQ-FS5 SECTION.                                             ST1474.2
129800 RD-3.                                                            ST1474.2
129900     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1474.2
130000         GO TO R3-EXIT.                                           ST1474.2
130100     READ SQ-FS5 AT END GO TO PREMATURE-EOF-3.                    ST1474.2
130200*    WRITE PRINT-REC FROM SQ-FS5R1-F-G-132.                       ST1474.2
130300*    WRITE PRINT-REC FROM REST-OF-5.                              ST1474.2
130400*    MOVE SPACES TO PRINT-REC.                                    ST1474.2
130500     MOVE ALPHAN-KEY OF KEY-14 TO COMPU (WRK-DU-999-0001).        ST1474.2
130600     GO TO R3-EXIT.                                               ST1474.2
130700 PREMATURE-EOF-3.                                                 ST1474.2
130800     MOVE 1 TO WRK-DU-9-0001.                                     ST1474.2
130900     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1474.2
131000 R3-EXIT.                                                         ST1474.2
131100     EXIT.                                                        ST1474.2
131200*                                                                 ST1474.2
131300 CCVS-EXIT SECTION.                                               ST1474.2
131400 CCVS-999999.                                                     ST1474.2
131500     GO TO CLOSE-FILES.                                           ST1474.2
      *END-OF,ST147A                                                                  
