      *HEADER,COBOL,ST140A                                                    TES00010
000100 IDENTIFICATION DIVISION.                                         ST1404.2
000200 PROGRAM-ID.                                                      ST1404.2
000300     ST140A.                                                      ST1404.2
000400****************************************************************  ST1404.2
000500*                                                              *  ST1404.2
000600*    VALIDATION FOR:-                                          *  ST1404.2
000700*                                                              *  ST1404.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1404.2
000900*                                                              *  ST1404.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1404.2
001100*                                                              *  ST1404.2
001200****************************************************************  ST1404.2
001300*                                                                 ST1404.2
001400*    OBJECTIVE -                                                  ST1404.2
001500*        ROUTINE ST210  IS A TEST OF THE MERGE STATEMENT USING    ST1404.2
001600*    THE ASCII COLLATING SEQUENCE AND MULTIPLE FILE TAPE.         ST1404.2
001700*    THIS ROUTINE IS A TEST OF THE COMPILERS ABILITY TO MERGE     ST1404.2
001800*    THE SECOND FILE OF A MULTI-FILE REEL WITH A MASS-STORAGE     ST1404.2
001900*    FILE TO PRODUCE A MASS-STORAGE FILE.                         ST1404.2
002000*                                                                 ST1404.2
002100*                                                                 ST1404.2
002200*    FEATURES TESTED -                                            ST1404.2
002300*    *   ALPHABET-NAME IS STANDARD-1 (THE ASCII COLLATING SEQ.)   ST1404.2
002400*    *   COLLATING SEQUENCE IS ALPHABET-NAME                      ST1404.2
002500*    *   MULTIPLE FILE TAPE                                       ST1404.2
002600*    *   FIXED LENGTH RECORDS                                     ST1404.2
002700*    *   SAME SORT AREA IN THE I-O-CONTROL PARAGRAPH              ST1404.2
002800*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1404.2
002900*    *   USING FILE-NAME SERIES                                   ST1404.2
003000*    *   OUTPUT PROCEDURE IS SECTION-NAME                         ST1404.2
003100*    *   RETURN RECORD INTO     PART OF THE OUTPUT PROCEDURE      ST1404.2
003200*                                                                 ST1404.2
003300*    *   MERGE MERGE-FILE-NAME                                    ST1404.2
003400*        ON DESCENDING KEY KEY-1 OF DATA-NAME-1                   ST1404.2
003500*        ASCENDING KEY-2 OF DATA-NAME-2                           ST1404.2
003600*        COLLATING SEQUENCE IS ALPHABET-NAME                      ST1404.2
003700*        OUTPUT PROCEDURE IS SECTION-NAME.                        ST1404.2
003800*                                                                 ST1404.2
003900*                                                                 ST1404.2
004000*                                                                 ST1404.2
004100*                                                                 ST1404.2
004200*    FILES USED -                                                 ST1404.2
004300*    *   FILES SQ-FS1 AND SQ-FS3 ARE WRITTEN ONTO A MULTIPLE      ST1404.2
004400*    FILE TAPE.  FILE SQ-FS2 IS WRITTEN ONTO MASS-STORAGE.        ST1404.2
004500*    THEN THE MERGE STATEMENT USES SQ-FS3 AND SQ-FS2 TO CREATE  A ST1404.2
004600*    NEW MASS-STORAGE FILE SQ-FS4.  ALL FILES HAVE FIXED LENGTH   ST1404.2
004700*    RECORDS AND 132 CHARACTERS PER RECORD.                       ST1404.2
004800*                                                                 ST1404.2
004900*        SQ-FS1                                                   ST1404.2
005000*    51 RECORDS                                                   ST1404.2
005100*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1404.2
005200*    BLOCKED 1                                                    ST1404.2
005300*    RESERVE 2 AREAS                                              ST1404.2
005400*                                                                 ST1404.2
005500*        SQ-FS2                                                   ST1404.2
005600*    51 RECORDS                                                   ST1404.2
005700*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1404.2
005800*    BLOCKED 2                                                    ST1404.2
005900*    RESERVE 4 AREAS                                              ST1404.2
006000*                                                                 ST1404.2
006100*        SQ-FS3                                                   ST1404.2
006200*    51 RECORDS                                                   ST1404.2
006300*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1404.2
006400*    BLOCKED 1                                                    ST1404.2
006500*    RESERVE 6 AREAS                                              ST1404.2
006600*                                                                 ST1404.2
006700*        NOTE THAT FILE SQ-FS3 IS THE SECOND POSITION             ST1404.2
006800*    ON A MULTIPLE FILE TAPE.  BOTH FILES SQ-FS1 AND SQ-FS3 ARE   ST1404.2
006900*    ON THE SAME MULTIPLE FILE TAPE.                              ST1404.2
007000*                                                                 ST1404.2
007100*        SQ-FS4                                                   ST1404.2
007200*    FINAL TOTAL OF 102 RECORDS AS A RESULT OF THE MERGE          ST1404.2
007300*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1404.2
007400*    BLOCKED 3                                                    ST1404.2
007500*    RESERVE 4 AREAS                                              ST1404.2
007600*                                                                 ST1404.2
007700*                                                                 ST1404.2
007800*    X-CARDS USED  -                                              ST1404.2
007900*    X-XXX008     SQ-FS1                                          ST1404.2
008000*    X-XXX014     SQ-FS2                                          ST1404.2
008100*    X-XXX009     SQ-FS3                                          ST1404.2
008200*    X-XXX015     SQ-FS4                                          ST1404.2
008300*    X-XXX027     MERGE FILE ST-FS1                               ST1404.2
008400*    X-55         SYSTEM PRINTER NAME.                            ST1404.2
008500*    X-82         SOURCE COMPUTER NAME.                           ST1404.2
008600*    X-83         OBJECT COMPUTER NAME.                           ST1404.2
008700*                                                                 ST1404.2
008800*                                                                 ST1404.2
008900*    OPTIONS RECOMMENDED -                                        ST1404.2
009000*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1404.2
009100*                  FILES AS THEY ARE CREATED AND READ DURING      ST1404.2
009200*                  MRG-TESTS 3 THRU 8.                            ST1404.2
009300*                                                                 ST1404.2
009400*                                                                 ST1404.2
009500*    TEST DESCRIPTIONS -                                          ST1404.2
009600*    BLD-TEST-001     CHECKS THE CREATION OF SQ-FS1               ST1404.2
009700*    BLD-TEST-002     CHECKS THE CREATION OF SQ-FS2               ST1404.2
009800*    BLD-TEST-003     CHECKS THE CREATION OF SQ-FS3               ST1404.2
009900*    MRG-TEST-003     TESTS RECORDS 1-20 ON MERGE RESULT SQ-FS4   ST1404.2
010000*    MRG-TEST-004     TESTS RECORDS 21-40 ON MERGE RESULT SQ-FS4  ST1404.2
010100*    MRG-TEST-005     TESTS RECORDS 41-60 ON MERGE RESULT SQ-FS4  ST1404.2
010200*    MRG-TEST-006     TESTS RECORDS 61-80 ON MERGE RESULT SQ-FS4  ST1404.2
010300*    MRG-TEST-007     TESTS RECORDS 81-100 ON MERGE RESULT SQ-FS4 ST1404.2
010400*    MRG-TEST-008     TESTS RECORDS 101-102 ON MERGE RESULT SQ-FS4ST1404.2
010500*    MRG-TEST-009     AN EOF CHECK ON SQ-FS4                      ST1404.2
010600*    MRG-TEST-010     CHECK THAT THE NUMERIC KEY ON THE LAST      ST1404.2
010700*                     RECORD ON SQ-FS4 EQUALS 102                 ST1404.2
010800*                                                                 ST1404.2
010900*                                                                 ST1404.2
011000*    ************************************************************ ST1404.2
011100 ENVIRONMENT DIVISION.                                            ST1404.2
011200 CONFIGURATION SECTION.                                           ST1404.2
011300 SOURCE-COMPUTER.                                                 ST1404.2
011400     XXXXX082.                                                    ST1404.2
011500 OBJECT-COMPUTER.                                                 ST1404.2
011600     XXXXX083.                                                    ST1404.2
011700 SPECIAL-NAMES.                                                   ST1404.2
011800     ALPHABET MY-FAVORITE-ALPHABET IS STANDARD-1.                 ST1404.2
011900 INPUT-OUTPUT SECTION.                                            ST1404.2
012000 FILE-CONTROL.                                                    ST1404.2
012100     SELECT PRINT-FILE ASSIGN TO                                  ST1404.2
012200     XXXXX055.                                                    ST1404.2
012300     SELECT SQ-FS1 ASSIGN                                         ST1404.2
012400     XXXXX008                                                     ST1404.2
012500     ; ORGANIZATION IS SEQUENTIAL                                 ST1404.2
012600     ACCESS MODE SEQUENTIAL                                       ST1404.2
012700     RESERVE 2 AREAS.                                             ST1404.2
012800     SELECT SQ-FS2 ASSIGN TO                                      ST1404.2
012900     XXXXX014                                                     ST1404.2
013000     ORGANIZATION IS SEQUENTIAL                                   ST1404.2
013100     ACCESS MODE IS SEQUENTIAL                                    ST1404.2
013200     RESERVE 4 AREAS.                                             ST1404.2
013300     SELECT SQ-FS3 ASSIGN TO                                      ST1404.2
013400     XXXXX009                                                     ST1404.2
013500     ORGANIZATION SEQUENTIAL                                      ST1404.2
013600     ; ACCESS MODE IS SEQUENTIAL                                  ST1404.2
013700     RESERVE 6 AREAS.                                             ST1404.2
013800     SELECT SQ-FS4 ASSIGN TO                                      ST1404.2
013900     XXXXX015                                                     ST1404.2
014000     ORGANIZATION IS SEQUENTIAL                                   ST1404.2
014100     ; ACCESS MODE IS SEQUENTIAL                                  ST1404.2
014200     RESERVE 4 AREAS.                                             ST1404.2
014300     SELECT ST-FS1 ASSIGN TO                                      ST1404.2
014400     XXXXX027.                                                    ST1404.2
014500 I-O-CONTROL.                                                     ST1404.2
014600     SAME SORT AREA FOR SQ-FS1 ST-FS1,                            ST1404.2
014700     MULTIPLE FILE TAPE CONTAINS SQ-FS1 POSITION 1                ST1404.2
014800     SQ-FS3 POSITION 2.                                           ST1404.2
014900 DATA DIVISION.                                                   ST1404.2
015000 FILE SECTION.                                                    ST1404.2
015100 FD  PRINT-FILE.                                                  ST1404.2
015200 01  PRINT-REC PICTURE X(120).                                    ST1404.2
015300 01  DUMMY-RECORD PICTURE X(120).                                 ST1404.2
015400 FD  SQ-FS1                                                       ST1404.2
015500     LABEL RECORDS STANDARD                                       ST1404.2
015600C    VALUE OF                                                     ST1404.2
015700C    XXXXX074                                                     ST1404.2
015800C    IS                                                           ST1404.2
015900C    XXXXX075                                                     ST1404.2
016000*    XXXXX069                                                     ST1404.2
016100     BLOCK CONTAINS 1 RECORDS                                     ST1404.2
016200     RECORD CONTAINS 132 CHARACTERS                               ST1404.2
016300     DATA RECORDS SQ-FS1R1-F-G-132, SQ-FS1R2-F-G-132.             ST1404.2
016400 01  SQ-FS1R1-F-G-132.                                            ST1404.2
016500     10 REC-PREAMBLE PIC X(120).                                  ST1404.2
016600     10 REST-OF-1.                                                ST1404.2
016700     20 KEY-1.                                                    ST1404.2
016800         30 ALPHAN-KEY PIC X.                                     ST1404.2
016900         30 NUM-KEY PIC 999.                                      ST1404.2
017000     20 KEY-2.                                                    ST1404.2
017100         30 ALPHAN-KEY PIC X.                                     ST1404.2
017200         30 NUM-KEY PIC 999.                                      ST1404.2
017300     20 KEY-3.                                                    ST1404.2
017400         30 ALPHAN-KEY PIC X.                                     ST1404.2
017500         30 NUM-KEY PIC 999.                                      ST1404.2
017600 01  SQ-FS1R2-F-G-132.                                            ST1404.2
017700     02 FILLER PIC X(120).                                        ST1404.2
017800     02 GARBAGE PIC X(12).                                        ST1404.2
017900 FD  SQ-FS2                                                       ST1404.2
018000     LABEL RECORD STANDARD                                        ST1404.2
018100C    VALUE OF                                                     ST1404.2
018200C    XXXXX074                                                     ST1404.2
018300C    IS                                                           ST1404.2
018400C    XXXXX076                                                     ST1404.2
018500*    XXXXX069                                                     ST1404.2
018600     BLOCK CONTAINS 2 RECORDS                                     ST1404.2
018700     RECORD CONTAINS 132 CHARACTERS                               ST1404.2
018800     DATA RECORD SQ-FS2R1-F-G-132.                                ST1404.2
018900 01  SQ-FS2R1-F-G-132.                                            ST1404.2
019000     10 REC-PRE-2 PIC X(120).                                     ST1404.2
019100     10 REST-OF-2.                                                ST1404.2
019200     20 KEY-4.                                                    ST1404.2
019300         30 ALPHAN-KEY PIC X.                                     ST1404.2
019400         30 NUM-KEY PIC 999.                                      ST1404.2
019500     20 KEY-5.                                                    ST1404.2
019600         30 ALPHAN-KEY PIC X.                                     ST1404.2
019700         30 NUM-KEY PIC 999.                                      ST1404.2
019800     20 KEY-6.                                                    ST1404.2
019900         30 ALPHAN-KEY PIC X.                                     ST1404.2
020000         30 NUM-KEY PIC 999.                                      ST1404.2
020100 FD  SQ-FS3                                                       ST1404.2
020200     LABEL RECORDS STANDARD                                       ST1404.2
020300C    VALUE OF                                                     ST1404.2
020400C    XXXXX074                                                     ST1404.2
020500C    IS                                                           ST1404.2
020600C    XXXXX077                                                     ST1404.2
020700*    XXXXX069                                                     ST1404.2
020800     BLOCK CONTAINS 1 RECORDS                                     ST1404.2
020900     RECORD CONTAINS 132 CHARACTERS                               ST1404.2
021000     DATA RECORD SQ-FS3R1-F-G-132.                                ST1404.2
021100 01  SQ-FS3R1-F-G-132.                                            ST1404.2
021200     10 REC-PRE-3 PIC X(120).                                     ST1404.2
021300     10 REST-OF-3.                                                ST1404.2
021400     20 KEY-7.                                                    ST1404.2
021500         30 ALPHAN-KEY PIC X.                                     ST1404.2
021600         30 NUM-KEY PIC 999.                                      ST1404.2
021700     20 KEY-8.                                                    ST1404.2
021800         30 ALPHAN-KEY PIC X.                                     ST1404.2
021900         30 NUM-KEY PIC 999.                                      ST1404.2
022000     20 KEY-9.                                                    ST1404.2
022100         30 ALPHAN-KEY PIC X.                                     ST1404.2
022200         30 NUM-KEY PIC 999.                                      ST1404.2
022300 FD  SQ-FS4                                                       ST1404.2
022400     LABEL RECORD IS STANDARD                                     ST1404.2
022500C    ; VALUE OF                                                   ST1404.2
022600C    XXXXX074                                                     ST1404.2
022700C    IS                                                           ST1404.2
022800C    XXXXX078                                                     ST1404.2
022900*    XXXXX069                                                     ST1404.2
023000     ; BLOCK CONTAINS 3 RECORDS                                   ST1404.2
023100     RECORD CONTAINS 132 CHARACTERS                               ST1404.2
023200     DATA RECORD SQ-FS4R1-F-G-132.                                ST1404.2
023300 01  SQ-FS4R1-F-G-132.                                            ST1404.2
023400     10  REC-PRE-4 PIC X(120).                                    ST1404.2
023500     10  REST-OF-4.                                               ST1404.2
023600     20  KEY-10.                                                  ST1404.2
023700          30  ALPHAN-KEY PIC X.                                   ST1404.2
023800          30  NUM-KEY PIC 999.                                    ST1404.2
023900     20  KEY-11.                                                  ST1404.2
024000          30  ALPHAN-KEY PIC X.                                   ST1404.2
024100          30  NUM-KEY PIC 999.                                    ST1404.2
024200     20  KEY-12.                                                  ST1404.2
024300          30  ALPHAN-KEY PIC X.                                   ST1404.2
024400          30  NUM-KEY PIC 999.                                    ST1404.2
024500 SD  ST-FS1                                                       ST1404.2
024600     RECORD CONTAINS 132 CHARACTERS                               ST1404.2
024700     DATA RECORD IS ST-FS1R1-F-G-132.                             ST1404.2
024800 01  ST-FS1R1-F-G-132.                                            ST1404.2
024900     02 FILLER PIC X(120).                                        ST1404.2
025000     02 NON-KEY-1.                                                ST1404.2
025100         03 A-KEY PIC X.                                          ST1404.2
025200         03 N-KEY PIC 999.                                        ST1404.2
025300     02 SORT-KEY.                                                 ST1404.2
025400         03 A-KEY PIC X.                                          ST1404.2
025500         03 N-KEY PIC 999.                                        ST1404.2
025600     02 NON-KEY-2.                                                ST1404.2
025700         03 A-KEY PIC X.                                          ST1404.2
025800         03 N-KEY PIC 999.                                        ST1404.2
025900 WORKING-STORAGE SECTION.                                         ST1404.2
026000 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1404.2
026100 77  WRK-DU-999-0001 PIC 999.                                     ST1404.2
026200 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1404.2
026300 77  LAST-REC-NUM  PIC 999 VALUE ZERO.                            ST1404.2
026400 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1404.2
026500     "ZYXWVUTSRQPONMLKJIHGFEDCBA>=<;9876543210/.-,+*)($$ ".       ST1404.2
026600 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1404.2
026700     02 CHAR PIC X OCCURS 51 TIMES.                               ST1404.2
026800 01  WRK-XN-2    PIC X(51) VALUE                                  ST1404.2
026900     "ZYXWVUTSRQPONMLKJIHGFEDCBA>=<;9876543210/.-,+*)($$ ".       ST1404.2
027000 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1404.2
027100     02 ASCIIS PIC X OCCURS 51 TIMES.                             ST1404.2
027200 01  WRK-XN-O020F-0001.                                           ST1404.2
027300     02 COMPU PIC X OCCURS 20 TIMES.                              ST1404.2
027400 01  WRK-XN-X-0001 REDEFINES WRK-XN-O020F-0001.                   ST1404.2
027500     02 FILLER PIC X(20).                                         ST1404.2
027600 01  WRK-XN-O120F-1.                                              ST1404.2
027700     02 COLLS PIC X OCCURS 120 TIMES.                             ST1404.2
027800 01  WRK-XN-X-2 REDEFINES WRK-XN-O120F-1.                         ST1404.2
027900     02 WRK-XN-0002 PIC X(20).                                    ST1404.2
028000     02 WRK-XN-0003 PIC X(20).                                    ST1404.2
028100     02 WRK-XN-0004 PIC X(20).                                    ST1404.2
028200     02 WRK-XN-0005 PIC X(20).                                    ST1404.2
028300     02 WRK-XN-0006 PIC X(20).                                    ST1404.2
028400     02 WRK-XN-0007 PIC X(20).                                    ST1404.2
028500 01  FILE-RECORD-INFORMATION-REC.                                 ST1404.2
028600     03 FILE-RECORD-INFO-SKELETON.                                ST1404.2
028700        05 FILLER                 PICTURE X(48)       VALUE       ST1404.2
028800             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1404.2
028900        05 FILLER                 PICTURE X(46)       VALUE       ST1404.2
029000             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1404.2
029100        05 FILLER                 PICTURE X(26)       VALUE       ST1404.2
029200             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1404.2
029300        05 FILLER                 PICTURE X(37)       VALUE       ST1404.2
029400             ",RECKEY=                             ".             ST1404.2
029500        05 FILLER                 PICTURE X(38)       VALUE       ST1404.2
029600             ",ALTKEY1=                             ".            ST1404.2
029700        05 FILLER                 PICTURE X(38)       VALUE       ST1404.2
029800             ",ALTKEY2=                             ".            ST1404.2
029900        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1404.2
030000     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1404.2
030100        05 FILE-RECORD-INFO-P1-120.                               ST1404.2
030200           07 FILLER              PIC X(5).                       ST1404.2
030300           07 XFILE-NAME           PIC X(6).                      ST1404.2
030400           07 FILLER              PIC X(8).                       ST1404.2
030500           07 XRECORD-NAME         PIC X(6).                      ST1404.2
030600           07 FILLER              PIC X(1).                       ST1404.2
030700           07 REELUNIT-NUMBER     PIC 9(1).                       ST1404.2
030800           07 FILLER              PIC X(7).                       ST1404.2
030900           07 XRECORD-NUMBER       PIC 9(6).                      ST1404.2
031000           07 FILLER              PIC X(6).                       ST1404.2
031100           07 UPDATE-NUMBER       PIC 9(2).                       ST1404.2
031200           07 FILLER              PIC X(5).                       ST1404.2
031300           07 ODO-NUMBER          PIC 9(4).                       ST1404.2
031400           07 FILLER              PIC X(5).                       ST1404.2
031500           07 XPROGRAM-NAME        PIC X(5).                      ST1404.2
031600           07 FILLER              PIC X(7).                       ST1404.2
031700           07 XRECORD-LENGTH       PIC 9(6).                      ST1404.2
031800           07 FILLER              PIC X(7).                       ST1404.2
031900           07 CHARS-OR-RECORDS    PIC X(2).                       ST1404.2
032000           07 FILLER              PIC X(1).                       ST1404.2
032100           07 XBLOCK-SIZE          PIC 9(4).                      ST1404.2
032200           07 FILLER              PIC X(6).                       ST1404.2
032300           07 RECORDS-IN-FILE     PIC 9(6).                       ST1404.2
032400           07 FILLER              PIC X(5).                       ST1404.2
032500           07 XFILE-ORGANIZATION   PIC X(2).                      ST1404.2
032600           07 FILLER              PIC X(6).                       ST1404.2
032700           07 XLABEL-TYPE          PIC X(1).                      ST1404.2
032800        05 FILE-RECORD-INFO-P121-240.                             ST1404.2
032900           07 FILLER              PIC X(8).                       ST1404.2
033000           07 XRECORD-KEY          PIC X(29).                     ST1404.2
033100           07 FILLER              PIC X(9).                       ST1404.2
033200           07 ALTERNATE-KEY1      PIC X(29).                      ST1404.2
033300           07 FILLER              PIC X(9).                       ST1404.2
033400           07 ALTERNATE-KEY2      PIC X(29).                      ST1404.2
033500           07 FILLER              PIC X(7).                       ST1404.2
033600 01  TEST-RESULTS.                                                ST1404.2
033700     02 FILLER                   PIC X      VALUE SPACE.          ST1404.2
033800     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1404.2
033900     02 FILLER                   PIC X      VALUE SPACE.          ST1404.2
034000     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1404.2
034100     02 FILLER                   PIC X      VALUE SPACE.          ST1404.2
034200     02  PAR-NAME.                                                ST1404.2
034300       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1404.2
034400       03  PARDOT-X              PIC X      VALUE SPACE.          ST1404.2
034500       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1404.2
034600     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1404.2
034700     02 RE-MARK                  PIC X(61).                       ST1404.2
034800 01  TEST-COMPUTED.                                               ST1404.2
034900     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1404.2
035000     02 FILLER                   PIC X(17)  VALUE                 ST1404.2
035100            "       COMPUTED=".                                   ST1404.2
035200     02 COMPUTED-X.                                               ST1404.2
035300     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1404.2
035400     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1404.2
035500                                 PIC -9(9).9(9).                  ST1404.2
035600     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1404.2
035700     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1404.2
035800     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1404.2
035900     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1404.2
036000         04 COMPUTED-18V0                    PIC -9(18).          ST1404.2
036100         04 FILLER                           PIC X.               ST1404.2
036200     03 FILLER PIC X(50) VALUE SPACE.                             ST1404.2
036300 01  TEST-CORRECT.                                                ST1404.2
036400     02 FILLER PIC X(30) VALUE SPACE.                             ST1404.2
036500     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1404.2
036600     02 CORRECT-X.                                                ST1404.2
036700     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1404.2
036800     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1404.2
036900     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1404.2
037000     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1404.2
037100     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1404.2
037200     03      CR-18V0 REDEFINES CORRECT-A.                         ST1404.2
037300         04 CORRECT-18V0                     PIC -9(18).          ST1404.2
037400         04 FILLER                           PIC X.               ST1404.2
037500     03 FILLER PIC X(2) VALUE SPACE.                              ST1404.2
037600     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1404.2
037700 01  CCVS-C-1.                                                    ST1404.2
037800     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1404.2
037900-    "SS  PARAGRAPH-NAME                                          ST1404.2
038000-    "       REMARKS".                                            ST1404.2
038100     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1404.2
038200 01  CCVS-C-2.                                                    ST1404.2
038300     02 FILLER                     PIC X        VALUE SPACE.      ST1404.2
038400     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1404.2
038500     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1404.2
038600     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1404.2
038700     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1404.2
038800 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1404.2
038900 01  REC-CT                        PIC 99       VALUE ZERO.       ST1404.2
039000 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1404.2
039100 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1404.2
039200 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1404.2
039300 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1404.2
039400 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1404.2
039500 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1404.2
039600 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1404.2
039700 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1404.2
039800 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1404.2
039900 01  CCVS-H-1.                                                    ST1404.2
040000     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1404.2
040100     02  FILLER                    PIC X(42)    VALUE             ST1404.2
040200     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1404.2
040300     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1404.2
040400 01  CCVS-H-2A.                                                   ST1404.2
040500   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1404.2
040600   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1404.2
040700   02  FILLER                        PIC XXXX   VALUE             ST1404.2
040800     "4.2 ".                                                      ST1404.2
040900   02  FILLER                        PIC X(28)  VALUE             ST1404.2
041000            " COPY - NOT FOR DISTRIBUTION".                       ST1404.2
041100   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1404.2
041200                                                                  ST1404.2
041300 01  CCVS-H-2B.                                                   ST1404.2
041400   02  FILLER                        PIC X(15)  VALUE             ST1404.2
041500            "TEST RESULT OF ".                                    ST1404.2
041600   02  TEST-ID                       PIC X(9).                    ST1404.2
041700   02  FILLER                        PIC X(4)   VALUE             ST1404.2
041800            " IN ".                                               ST1404.2
041900   02  FILLER                        PIC X(12)  VALUE             ST1404.2
042000     " HIGH       ".                                              ST1404.2
042100   02  FILLER                        PIC X(22)  VALUE             ST1404.2
042200            " LEVEL VALIDATION FOR ".                             ST1404.2
042300   02  FILLER                        PIC X(58)  VALUE             ST1404.2
042400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1404.2
042500 01  CCVS-H-3.                                                    ST1404.2
042600     02  FILLER                      PIC X(34)  VALUE             ST1404.2
042700            " FOR OFFICIAL USE ONLY    ".                         ST1404.2
042800     02  FILLER                      PIC X(58)  VALUE             ST1404.2
042900     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1404.2
043000     02  FILLER                      PIC X(28)  VALUE             ST1404.2
043100            "  COPYRIGHT   1985 ".                                ST1404.2
043200 01  CCVS-E-1.                                                    ST1404.2
043300     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1404.2
043400     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1404.2
043500     02 ID-AGAIN                     PIC X(9).                    ST1404.2
043600     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1404.2
043700 01  CCVS-E-2.                                                    ST1404.2
043800     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1404.2
043900     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1404.2
044000     02 CCVS-E-2-2.                                               ST1404.2
044100         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1404.2
044200         03 FILLER                   PIC X      VALUE SPACE.      ST1404.2
044300         03 ENDER-DESC               PIC X(44)  VALUE             ST1404.2
044400            "ERRORS ENCOUNTERED".                                 ST1404.2
044500 01  CCVS-E-3.                                                    ST1404.2
044600     02  FILLER                      PIC X(22)  VALUE             ST1404.2
044700            " FOR OFFICIAL USE ONLY".                             ST1404.2
044800     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1404.2
044900     02  FILLER                      PIC X(58)  VALUE             ST1404.2
045000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1404.2
045100     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1404.2
045200     02 FILLER                       PIC X(15)  VALUE             ST1404.2
045300             " COPYRIGHT 1985".                                   ST1404.2
045400 01  CCVS-E-4.                                                    ST1404.2
045500     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1404.2
045600     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1404.2
045700     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1404.2
045800     02 FILLER                       PIC X(40)  VALUE             ST1404.2
045900      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1404.2
046000 01  XXINFO.                                                      ST1404.2
046100     02 FILLER                       PIC X(19)  VALUE             ST1404.2
046200            "*** INFORMATION ***".                                ST1404.2
046300     02 INFO-TEXT.                                                ST1404.2
046400       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1404.2
046500       04 XXCOMPUTED                 PIC X(20).                   ST1404.2
046600       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1404.2
046700       04 XXCORRECT                  PIC X(20).                   ST1404.2
046800     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1404.2
046900 01  HYPHEN-LINE.                                                 ST1404.2
047000     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1404.2
047100     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1404.2
047200-    "*****************************************".                 ST1404.2
047300     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1404.2
047400-    "******************************".                            ST1404.2
047500 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1404.2
047600     "ST140A".                                                    ST1404.2
047700 PROCEDURE DIVISION.                                              ST1404.2
047800 DECLARATIVES.                                                    ST1404.2
047900 SECT-ST210-DEC SECTION.                                          ST1404.2
048000     USE AFTER STANDARD EXCEPTION PROCEDURE ON INPUT.             ST1404.2
048100 SRT-WRITE-DEC.                                                   ST1404.2
048200     MOVE "ERROR ON OUTPUT DECL." TO FEATURE.                     ST1404.2
048300     MOVE "SRT-TEST-DEC" TO PAR-NAME.                             ST1404.2
048400     WRITE   PRINT-REC FROM TEST-RESULTS AFTER ADVANCING 2 LINES. ST1404.2
048500     STOP RUN.                                                    ST1404.2
048600 END DECLARATIVES.                                                ST1404.2
048700 CCVS1 SECTION.                                                   ST1404.2
048800 OPEN-FILES.                                                      ST1404.2
048900     OPEN    OUTPUT PRINT-FILE.                                   ST1404.2
049000     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1404.2
049100     MOVE    SPACE TO TEST-RESULTS.                               ST1404.2
049200     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1404.2
049300     MOVE    ZERO TO REC-SKL-SUB.                                 ST1404.2
049400     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1404.2
049500 CCVS-INIT-FILE.                                                  ST1404.2
049600     ADD     1 TO REC-SKL-SUB.                                    ST1404.2
049700     MOVE    FILE-RECORD-INFO-SKELETON                            ST1404.2
049800          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1404.2
049900 CCVS-INIT-EXIT.                                                  ST1404.2
050000     GO TO CCVS1-EXIT.                                            ST1404.2
050100 CLOSE-FILES.                                                     ST1404.2
050200     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1404.2
050300 TERMINATE-CCVS.                                                  ST1404.2
050400*    EXIT PROGRAM.                                                ST1404.2
050500*TERMINATE-CALL.                                                  ST1404.2
050600     STOP     RUN.                                                ST1404.2
050700 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1404.2
050800 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1404.2
050900 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1404.2
051000 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1404.2
051100     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1404.2
051200 PRINT-DETAIL.                                                    ST1404.2
051300     IF REC-CT NOT EQUAL TO ZERO                                  ST1404.2
051400             MOVE "." TO PARDOT-X                                 ST1404.2
051500             MOVE REC-CT TO DOTVALUE.                             ST1404.2
051600     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1404.2
051700     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1404.2
051800        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1404.2
051900          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1404.2
052000     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1404.2
052100     MOVE SPACE TO CORRECT-X.                                     ST1404.2
052200     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1404.2
052300     MOVE     SPACE TO RE-MARK.                                   ST1404.2
052400 HEAD-ROUTINE.                                                    ST1404.2
052500     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1404.2
052600     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1404.2
052700     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1404.2
052800     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1404.2
052900 COLUMN-NAMES-ROUTINE.                                            ST1404.2
053000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1404.2
053100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1404.2
053200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1404.2
053300 END-ROUTINE.                                                     ST1404.2
053400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1404.2
053500 END-RTN-EXIT.                                                    ST1404.2
053600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1404.2
053700 END-ROUTINE-1.                                                   ST1404.2
053800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1404.2
053900      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1404.2
054000      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1404.2
054100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1404.2
054200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1404.2
054300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1404.2
054400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1404.2
054500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1404.2
054600  END-ROUTINE-12.                                                 ST1404.2
054700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1404.2
054800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1404.2
054900         MOVE "NO " TO ERROR-TOTAL                                ST1404.2
055000         ELSE                                                     ST1404.2
055100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1404.2
055200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1404.2
055300     PERFORM WRITE-LINE.                                          ST1404.2
055400 END-ROUTINE-13.                                                  ST1404.2
055500     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1404.2
055600         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1404.2
055700         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1404.2
055800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1404.2
055900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1404.2
056000      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1404.2
056100          MOVE "NO " TO ERROR-TOTAL                               ST1404.2
056200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1404.2
056300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1404.2
056400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1404.2
056500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1404.2
056600 WRITE-LINE.                                                      ST1404.2
056700     ADD 1 TO RECORD-COUNT.                                       ST1404.2
056800*    IF RECORD-COUNT GREATER 42                                   ST1404.2
056900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1404.2
057000*        MOVE SPACE TO DUMMY-RECORD                               ST1404.2
057100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1404.2
057200*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1404.2
057300*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1404.2
057400*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1404.2
057500*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1404.2
057600*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1404.2
057700*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1404.2
057800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1404.2
057900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1404.2
058000*        MOVE ZERO TO RECORD-COUNT.                               ST1404.2
058100     PERFORM WRT-LN.                                              ST1404.2
058200 WRT-LN.                                                          ST1404.2
058300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1404.2
058400     MOVE SPACE TO DUMMY-RECORD.                                  ST1404.2
058500 BLANK-LINE-PRINT.                                                ST1404.2
058600     PERFORM WRT-LN.                                              ST1404.2
058700 FAIL-ROUTINE.                                                    ST1404.2
058800     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1404.2
058900            GO TO   FAIL-ROUTINE-WRITE.                           ST1404.2
059000     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1404.2
059100     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1404.2
059200     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1404.2
059300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1404.2
059400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1404.2
059500     GO TO  FAIL-ROUTINE-EX.                                      ST1404.2
059600 FAIL-ROUTINE-WRITE.                                              ST1404.2
059700     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1404.2
059800     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1404.2
059900     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1404.2
060000     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1404.2
060100 FAIL-ROUTINE-EX. EXIT.                                           ST1404.2
060200 BAIL-OUT.                                                        ST1404.2
060300     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1404.2
060400     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1404.2
060500 BAIL-OUT-WRITE.                                                  ST1404.2
060600     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1404.2
060700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1404.2
060800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1404.2
060900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1404.2
061000 BAIL-OUT-EX. EXIT.                                               ST1404.2
061100 CCVS1-EXIT.                                                      ST1404.2
061200     EXIT.                                                        ST1404.2
061300 SECT-ST210-0001 SECTION.                                         ST1404.2
061400 BLD-INIT-001.                                                    ST1404.2
061500     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1404.2
061600     OPEN OUTPUT SQ-FS1.                                          ST1404.2
061700     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1404.2
061800     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1404.2
061900     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       ST1404.2
062000     MOVE 000132 TO XRECORD-LENGTH (1).                           ST1404.2
062100     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1404.2
062200     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1404.2
062300     MOVE 000051 TO RECORDS-IN-FILE (1).                          ST1404.2
062400     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1404.2
062500     MOVE "S" TO XLABEL-TYPE (1).                                 ST1404.2
062600     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1404.2
062700     MOVE SPACES TO WRK-XN-O120F-1.                               ST1404.2
062800 BLD-TEST-001.                                                    ST1404.2
062900     PERFORM BLD-TEST-001-BUILD VARYING WRK-DU-999-0001           ST1404.2
063000         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1404.2
063100*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
063200*    WRITE   PRINT-REC.                                           ST1404.2
063300     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1404.2
063400         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1404.2
063500         ELSE                                                     ST1404.2
063600         PERFORM PASS.                                            ST1404.2
063700     GO TO BLD-WRITE-001.                                         ST1404.2
063800 BLD-TEST-001-BUILD.                                              ST1404.2
063900     MOVE "JUNKSLOPJUNK" TO GARBAGE.                              ST1404.2
064000     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1404.2
064100         NUM-KEY OF KEY-3.                                        ST1404.2
064200     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1404.2
064300     ADD 1 TO XRECORD-NUMBER (1).                                 ST1404.2
064400     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1404.2
064500     ADD 1 TO WRK-DU-999-2.                                       ST1404.2
064600     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1404.2
064700     ADD 1 TO WRK-DU-999-2.                                       ST1404.2
064800*    WRITE PRINT-REC FROM SQ-FS1R1-F-G-132.                       ST1404.2
064900*    WRITE PRINT-REC FROM REST-OF-1.                              ST1404.2
065000*    MOVE SPACES TO PRINT-REC.                                    ST1404.2
065100     WRITE   SQ-FS1R1-F-G-132.                                    ST1404.2
065200 BLD-DELETE-001.                                                  ST1404.2
065300     PERFORM DE-LETE.                                             ST1404.2
065400 BLD-WRITE-001.                                                   ST1404.2
065500     MOVE "BLD-TEST-001" TO PAR-NAME.                             ST1404.2
065600     MOVE "FIRST FILE CREATED" TO COMPUTED-A.                     ST1404.2
065700     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1404.2
065800     PERFORM PRINT-DETAIL.                                        ST1404.2
065900*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
066000*    WRITE   PRINT-REC.                                           ST1404.2
066100     CLOSE SQ-FS1 WITH NO REWIND.                                 ST1404.2
066200 BLD-INIT-002.                                                    ST1404.2
066300     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        ST1404.2
066400     OPEN OUTPUT SQ-FS2.                                          ST1404.2
066500     MOVE  "SQ-FS2" TO XFILE-NAME (1).                            ST1404.2
066600     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1404.2
066700     MOVE 0002 TO XBLOCK-SIZE (1).                                ST1404.2
066800 BLD-TEST-002.                                                    ST1404.2
066900     PERFORM BLD-TEST-002-BUILD VARYING WRK-DU-999-0001 FROM      ST1404.2
067000         1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.         ST1404.2
067100*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
067200*    WRITE   PRINT-REC.                                           ST1404.2
067300     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1404.2
067400         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1404.2
067500         ELSE                                                     ST1404.2
067600         PERFORM PASS.                                            ST1404.2
067700     GO TO BLD-WRITE-002.                                         ST1404.2
067800 BLD-TEST-002-BUILD.                                              ST1404.2
067900     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-4           ST1404.2
068000         ALPHAN-KEY OF KEY-5 ALPHAN-KEY OF KEY-6.                 ST1404.2
068100     ADD 51 WRK-DU-999-0001 GIVING NUM-KEY OF KEY-4               ST1404.2
068200         NUM-KEY OF KEY-5 NUM-KEY OF KEY-6.                       ST1404.2
068300     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-2.               ST1404.2
068400     ADD 000001 TO XRECORD-NUMBER (1).                            ST1404.2
068500*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1404.2
068600*    WRITE PRINT-REC FROM REST-OF-2.                              ST1404.2
068700*    MOVE SPACES TO PRINT-REC.                                    ST1404.2
068800     WRITE   SQ-FS2R1-F-G-132.                                    ST1404.2
068900 BLD-DELETE-002.                                                  ST1404.2
069000     PERFORM DE-LETE.                                             ST1404.2
069100 BLD-WRITE-002.                                                   ST1404.2
069200     MOVE "BLD-TEST-002" TO PAR-NAME.                             ST1404.2
069300     MOVE "2ND FILE CREATED" TO COMPUTED-A.                       ST1404.2
069400     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1404.2
069500     PERFORM PRINT-DETAIL.                                        ST1404.2
069600*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
069700*    WRITE   PRINT-REC.                                           ST1404.2
069800     CLOSE SQ-FS2.                                                ST1404.2
069900 BLD-INIT-003.                                                    ST1404.2
070000     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        ST1404.2
070100     MOVE  "SQ-FS3" TO XFILE-NAME (1).                            ST1404.2
070200     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1404.2
070300     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1404.2
070400     OPEN OUTPUT SQ-FS3.                                          ST1404.2
070500 BLD-TEST-003.                                                    ST1404.2
070600     PERFORM BLD-TEST-003-BUILD VARYING WRK-DU-999-0001 FROM      ST1404.2
070700         1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.         ST1404.2
070800*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
070900*    WRITE   PRINT-REC.                                           ST1404.2
071000     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 52                     ST1404.2
071100         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1404.2
071200         ELSE                                                     ST1404.2
071300         PERFORM PASS.                                            ST1404.2
071400     GO TO BLD-WRITE-003.                                         ST1404.2
071500 BLD-TEST-003-BUILD.                                              ST1404.2
071600     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-7           ST1404.2
071700         ALPHAN-KEY OF KEY-8 ALPHAN-KEY OF KEY-9.                 ST1404.2
071800     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-7 NUM-KEY OF KEY-8    ST1404.2
071900         NUM-KEY OF KEY-9.                                        ST1404.2
072000     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-3.               ST1404.2
072100     ADD 000001 TO XRECORD-NUMBER (1).                            ST1404.2
072200*    WRITE PRINT-REC FROM SQ-FS3R1-F-G-132.                       ST1404.2
072300*    WRITE PRINT-REC FROM REST-OF-3.                              ST1404.2
072400*    MOVE SPACES TO PRINT-REC.                                    ST1404.2
072500     WRITE   SQ-FS3R1-F-G-132.                                    ST1404.2
072600 BLD-DELETE-003.                                                  ST1404.2
072700     PERFORM DE-LETE.                                             ST1404.2
072800 BLD-WRITE-003.                                                   ST1404.2
072900     MOVE "BLD-TEST-003" TO PAR-NAME.                             ST1404.2
073000     MOVE "3RD FILE CREATED" TO COMPUTED-A.                       ST1404.2
073100     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1404.2
073200     PERFORM PRINT-DETAIL.                                        ST1404.2
073300*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
073400*    WRITE   PRINT-REC.                                           ST1404.2
073500     CLOSE SQ-FS3.                                                ST1404.2
073600 MRG-INIT-001.                                                    ST1404.2
073700     MERGE ST-FS1                                                 ST1404.2
073800         ON DESCENDING KEY A-KEY OF SORT-KEY                      ST1404.2
073900         ASCENDING N-KEY OF NON-KEY-2                             ST1404.2
074000     COLLATING SEQUENCE IS MY-FAVORITE-ALPHABET                   ST1404.2
074100         USING SQ-FS2, SQ-FS3                                     ST1404.2
074200     OUTPUT PROCEDURE IS SECT-ST210-0002.                         ST1404.2
074300 SRT-TEST-003.                                                    ST1404.2
074400     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
074500     OPEN INPUT SQ-FS4.                                           ST1404.2
074600     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
074700         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1404.2
074800*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
074900*    WRITE   PRINT-REC.                                           ST1404.2
075000     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1404.2
075100         PERFORM FAIL GO TO SRT-FAIL-003                          ST1404.2
075200         ELSE                                                     ST1404.2
075300         PERFORM PASS.                                            ST1404.2
075400     GO TO SRT-WRITE-003.                                         ST1404.2
075500 SRT-DELETE-003.                                                  ST1404.2
075600     PERFORM DE-LETE.                                             ST1404.2
075700     GO TO SRT-WRITE-003.                                         ST1404.2
075800 SRT-FAIL-003.                                                    ST1404.2
075900     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
076000     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1404.2
076100 SRT-WRITE-003.                                                   ST1404.2
076200     MOVE "MRG-TEST-003" TO PAR-NAME.                             ST1404.2
076300     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
076400     PERFORM PRINT-DETAIL.                                        ST1404.2
076500*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
076600*    WRITE   PRINT-REC.                                           ST1404.2
076700 SRT-INIT-004.                                                    ST1404.2
076800     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
076900 SRT-TEST-004.                                                    ST1404.2
077000     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
077100         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1404.2
077200*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
077300*    WRITE   PRINT-REC.                                           ST1404.2
077400     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1404.2
077500         PERFORM FAIL GO TO SRT-FAIL-004                          ST1404.2
077600         ELSE                                                     ST1404.2
077700         PERFORM PASS.                                            ST1404.2
077800     GO TO SRT-WRITE-004.                                         ST1404.2
077900 SRT-DELETE-004.                                                  ST1404.2
078000     PERFORM DE-LETE.                                             ST1404.2
078100     GO TO SRT-WRITE-004.                                         ST1404.2
078200 SRT-FAIL-004.                                                    ST1404.2
078300     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
078400     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1404.2
078500 SRT-WRITE-004.                                                   ST1404.2
078600     MOVE "MRG-TEST-004" TO PAR-NAME.                             ST1404.2
078700     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
078800     PERFORM PRINT-DETAIL.                                        ST1404.2
078900*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
079000*    WRITE   PRINT-REC.                                           ST1404.2
079100 SRT-INIT-005.                                                    ST1404.2
079200     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
079300 SRT-TEST-005.                                                    ST1404.2
079400     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
079500         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1404.2
079600*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
079700*    WRITE   PRINT-REC.                                           ST1404.2
079800     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1404.2
079900         PERFORM FAIL GO TO SRT-FAIL-005                          ST1404.2
080000         ELSE                                                     ST1404.2
080100         PERFORM PASS.                                            ST1404.2
080200     GO TO SRT-WRITE-005.                                         ST1404.2
080300 SRT-DELETE-005.                                                  ST1404.2
080400     PERFORM DE-LETE.                                             ST1404.2
080500     GO TO SRT-WRITE-005.                                         ST1404.2
080600 SRT-FAIL-005.                                                    ST1404.2
080700     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
080800     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1404.2
080900 SRT-WRITE-005.                                                   ST1404.2
081000     MOVE "MRG-TEST-005" TO PAR-NAME.                             ST1404.2
081100     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
081200     PERFORM PRINT-DETAIL.                                        ST1404.2
081300*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
081400*    WRITE   PRINT-REC.                                           ST1404.2
081500 SRT-INIT-006.                                                    ST1404.2
081600     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
081700 SRT-TEST-006.                                                    ST1404.2
081800     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
081900         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1404.2
082000*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
082100*    WRITE   PRINT-REC.                                           ST1404.2
082200     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1404.2
082300         PERFORM FAIL GO TO SRT-FAIL-006                          ST1404.2
082400         ELSE                                                     ST1404.2
082500         PERFORM PASS.                                            ST1404.2
082600     GO TO SRT-WRITE-006.                                         ST1404.2
082700 SRT-DELETE-006.                                                  ST1404.2
082800     PERFORM DE-LETE.                                             ST1404.2
082900     GO TO SRT-WRITE-006.                                         ST1404.2
083000 SRT-FAIL-006.                                                    ST1404.2
083100     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
083200     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1404.2
083300 SRT-WRITE-006.                                                   ST1404.2
083400     MOVE "MRG-TEST-006" TO PAR-NAME.                             ST1404.2
083500     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
083600     PERFORM PRINT-DETAIL.                                        ST1404.2
083700*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
083800*    WRITE   PRINT-REC.                                           ST1404.2
083900 SRT-INIT-007.                                                    ST1404.2
084000     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
084100 SRT-TEST-007.                                                    ST1404.2
084200     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
084300         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1404.2
084400*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
084500*    WRITE   PRINT-REC.                                           ST1404.2
084600     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1404.2
084700         PERFORM FAIL GO TO SRT-FAIL-007                          ST1404.2
084800         ELSE                                                     ST1404.2
084900         PERFORM PASS.                                            ST1404.2
085000     GO TO SRT-WRITE-007.                                         ST1404.2
085100 SRT-DELETE-007.                                                  ST1404.2
085200     PERFORM DE-LETE.                                             ST1404.2
085300     GO TO SRT-WRITE-007.                                         ST1404.2
085400 SRT-FAIL-007.                                                    ST1404.2
085500     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
085600     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1404.2
085700 SRT-WRITE-007.                                                   ST1404.2
085800     MOVE "MRG-TEST-007" TO PAR-NAME.                             ST1404.2
085900     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
086000     PERFORM PRINT-DETAIL.                                        ST1404.2
086100*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
086200*    WRITE   PRINT-REC.                                           ST1404.2
086300 SRT-INIT-008.                                                    ST1404.2
086400     MOVE SPACES TO WRK-XN-X-0001.                                ST1404.2
086500 SRT-TEST-008.                                                    ST1404.2
086600     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1404.2
086700         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1404.2
086800*    MOVE    SPACES TO PRINT-REC.                                 ST1404.2
086900*    WRITE   PRINT-REC.                                           ST1404.2
087000     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1404.2
087100         PERFORM FAIL GO TO SRT-FAIL-008                          ST1404.2
087200         ELSE                                                     ST1404.2
087300         PERFORM PASS.                                            ST1404.2
087400     GO TO SRT-WRITE-008.                                         ST1404.2
087500 SRT-DELETE-008.                                                  ST1404.2
087600     PERFORM DE-LETE.                                             ST1404.2
087700     GO TO SRT-WRITE-008.                                         ST1404.2
087800 SRT-FAIL-008.                                                    ST1404.2
087900     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1404.2
088000     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1404.2
088100 SRT-WRITE-008.                                                   ST1404.2
088200     MOVE "MRG-TEST-008" TO PAR-NAME.                             ST1404.2
088300     MOVE "STAN. COLLATING SEQ." TO FEATURE.                      ST1404.2
088400     PERFORM PRINT-DETAIL.                                        ST1404.2
088500     MOVE    NUM-KEY OF KEY-11 TO LAST-REC-NUM.                   ST1404.2
088600 SRT-TEST-009.                                                    ST1404.2
088700     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1404.2
088800         GO TO SRT-FAIL-009.                                      ST1404.2
088900     READ SQ-FS4 AT END PERFORM PASS                              ST1404.2
089000         GO TO SRT-WRITE-009.                                     ST1404.2
089100     GO TO SRT-FAIL-009.                                          ST1404.2
089200 SRT-DELETE-009.                                                  ST1404.2
089300     PERFORM DE-LETE.                                             ST1404.2
089400     GO TO SRT-WRITE-009.                                         ST1404.2
089500 SRT-FAIL-009.                                                    ST1404.2
089600     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1404.2
089700     PERFORM FAIL .                                               ST1404.2
089800 SRT-WRITE-009.                                                   ST1404.2
089900     MOVE "EOF CHECK SQ-FS4" TO FEATURE.                          ST1404.2
090000     MOVE "MRG-TEST-009" TO PAR-NAME.                             ST1404.2
090100     PERFORM PRINT-DETAIL.                                        ST1404.2
090200 SRT-TEST-010.                                                    ST1404.2
090300     IF LAST-REC-NUM IS NOT EQUAL TO 102                          ST1404.2
090400         PERFORM FAIL GO TO SRT-FAIL-010                          ST1404.2
090500         ELSE                                                     ST1404.2
090600         PERFORM PASS.                                            ST1404.2
090700     GO TO SRT-WRITE-010.                                         ST1404.2
090800 SRT-DELETE-010.                                                  ST1404.2
090900     PERFORM DE-LETE.                                             ST1404.2
091000     GO TO SRT-WRITE-010.                                         ST1404.2
091100 SRT-FAIL-010.                                                    ST1404.2
091200     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1404.2
091300     MOVE 102 TO CR-18V0.                                         ST1404.2
091400 SRT-WRITE-010.                                                   ST1404.2
091500     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1404.2
091600     MOVE "MRG-TEST-010" TO PAR-NAME.                             ST1404.2
091700     PERFORM PRINT-DETAIL.                                        ST1404.2
091800     CLOSE   SQ-FS4.                                              ST1404.2
091900     GO TO CCVS-999999.                                           ST1404.2
092000 READ-SQ-FS1 SECTION.                                             ST1404.2
092100 RD-1.                                                            ST1404.2
092200     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1404.2
092300         GO TO R1-EXIT.                                           ST1404.2
092400     READ SQ-FS4 AT END GO TO PREMATURE-EOF.                      ST1404.2
092500     MOVE ALPHAN-KEY OF KEY-12 TO COMPU (WRK-DU-999-0001).        ST1404.2
092600     GO TO R1-EXIT.                                               ST1404.2
092700 PREMATURE-EOF.                                                   ST1404.2
092800     MOVE 1 TO WRK-DU-9-0001.                                     ST1404.2
092900     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1404.2
093000 R1-EXIT.                                                         ST1404.2
093100     EXIT.                                                        ST1404.2
093200 SECT-ST210-0002 SECTION.                                         ST1404.2
093300 SORT-OUTPUT-PROC.                                                ST1404.2
093400     OPEN OUTPUT SQ-FS4.                                          ST1404.2
093500 RETURN-THE-OLD-RECORDS.                                          ST1404.2
093600     RETURN ST-FS1 RECORD INTO SQ-FS4R1-F-G-132                   ST1404.2
093700         AT END GO TO CLOSE-AFTER-SORT.                           ST1404.2
093800*    WRITE PRINT-REC FROM SQ-FS4R1-F-G-132.                       ST1404.2
093900*    WRITE PRINT-REC FROM REST-OF-4.                              ST1404.2
094000*    MOVE SPACES TO PRINT-REC.                                    ST1404.2
094100     WRITE   SQ-FS4R1-F-G-132.                                    ST1404.2
094200     GO TO RETURN-THE-OLD-RECORDS.                                ST1404.2
094300 CLOSE-AFTER-SORT.                                                ST1404.2
094400     CLOSE SQ-FS4.                                                ST1404.2
094500 CCVS-EXIT SECTION.                                               ST1404.2
094600 CCVS-999999.                                                     ST1404.2
094700     GO TO CLOSE-FILES.                                           ST1404.2
      *END-OF,ST140A                                                          TES10060
