      *HEADER,COBOL,ST144A                                                            
000100 IDENTIFICATION DIVISION.                                         ST1444.2
000200 PROGRAM-ID.                                                      ST1444.2
000300     ST144A.                                                      ST1444.2
000400****************************************************************  ST1444.2
000500*                                                              *  ST1444.2
000600*    VALIDATION FOR:-                                          *  ST1444.2
000700*                                                              *  ST1444.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1444.2
000900*                                                              *  ST1444.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1444.2
001100*                                                              *  ST1444.2
001200****************************************************************  ST1444.2
001300*                                                                 ST1444.2
001400*    OBJECTIVE -                                                  ST1444.2
001500*        ROUTINE ST144A IS A TEST OF THE MERGE STATEMENT USING    ST1444.2
001600*    A NATIVE  COLLATING SEQUENCE AND MULTIPLE FILE TAPE.         ST1444.2
001700*    THIS ROUTINE IS A TEST OF THE COMPILERS ABILITY TO MERGE     ST1444.2
001800*    THE SECOND FILE OF A MULTI-FILE REEL WITH A MASS-STORAGE     ST1444.2
001900*    FILE TO PRODUCE A MASS-STORAGE FILE.                         ST1444.2
002000*                                                                 ST1444.2
002100*                                                                 ST1444.2
002200*    FEATURES TESTED -                                            ST1444.2
002300*    *   MULTIPLE FILE TAPE                                       ST1444.2
002400*    *   FIXED LENGTH RECORDS                                     ST1444.2
002500*    *   SAME SORT AREA IN THE I-O-CONTROL PARAGRAPH              ST1444.2
002600*    *   QUALIFIED ALPHANUMERIC AND NUMERIC SORT KEYS             ST1444.2
002700*    *   USING FILE-NAME SERIES                                   ST1444.2
002800*    *   OUTPUT PROCEDURE IS SECTION-NAME                         ST1444.2
002900*    *   RETURN RECORD INTO     PART OF THE OUTPUT PROCEDURE      ST1444.2
003000*                                                                 ST1444.2
003100*    *   MERGE MERGE-FILE-NAME                                    ST1444.2
003200*        ON DESCENDING KEY KEY-1 OF DATA-NAME-1                   ST1444.2
003300*        ASCENDING KEY-2 OF DATA-NAME-2                           ST1444.2
003400*        USING  FILE-NAME-2, FILE-NAME-3                          ST1444.2
003500*        OUTPUT PROCEDURE IS SECTION-NAME.                        ST1444.2
003600*                                                                 ST1444.2
003700*                                                                 ST1444.2
003800*    FILES USED -                                                 ST1444.2
003900*    *   FILES SQ-FS1 AND SQ-FS3 ARE WRITTEN ONTO A MULTIPLE      ST1444.2
004000*    FILE TAPE.  FILE SQ-FS2 IS WRITTEN ONTO MASS-STORAGE.        ST1444.2
004100*    THEN THE MERGE STATEMENT USES SQ-FS3 AND SQ-FS2 TO CREATE  A ST1444.2
004200*    NEW MASS-STORAGE FILE SQ-FS4.  ALL FILES HAVE FIXED LENGTH   ST1444.2
004300*    RECORDS AND 132 CHARACTERS PER RECORD.                       ST1444.2
004400*                                                                 ST1444.2
004500*        SQ-FS1                                                   ST1444.2
004600*    51 RECORDS                                                   ST1444.2
004700*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1444.2
004800*    BLOCKED 1                                                    ST1444.2
004900*    RESERVE 2 AREAS                                              ST1444.2
005000*                                                                 ST1444.2
005100*        SQ-FS2                                                   ST1444.2
005200*    51 RECORDS                                                   ST1444.2
005300*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1444.2
005400*    BLOCKED 2                                                    ST1444.2
005500*    RESERVE 4 AREAS                                              ST1444.2
005600*                                                                 ST1444.2
005700*        SQ-FS3                                                   ST1444.2
005800*    51 RECORDS                                                   ST1444.2
005900*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1444.2
006000*    BLOCKED 1                                                    ST1444.2
006100*    RESERVE 6 AREAS                                              ST1444.2
006200*                                                                 ST1444.2
006300*        NOTE THAT FILE SQ-FS3 IS THE SECOND POSITION             ST1444.2
006400*    ON A MULTIPLE FILE TAPE.  BOTH FILES SQ-FS1 AND SQ-FS3 ARE   ST1444.2
006500*    ON THE SAME MULTIPLE FILE TAPE.                              ST1444.2
006600*                                                                 ST1444.2
006700*        SQ-FS4                                                   ST1444.2
006800*    FINAL TOTAL OF 102 RECORDS AS A RESULT OF THE MERGE          ST1444.2
006900*    FIXED LENGTH RECORDS 132 CHARACTERS                          ST1444.2
007000*    BLOCKED 3                                                    ST1444.2
007100*    RESERVE 4 AREAS                                              ST1444.2
007200*                                                                 ST1444.2
007300*                                                                 ST1444.2
007400*    X-CARDS USED  -                                              ST1444.2
007500*    X-XXX008     SQ-FS1                                          ST1444.2
007600*    X-XXX014     SQ-FS2                                          ST1444.2
007700*    X-XXX009     SQ-FS3                                          ST1444.2
007800*    X-XXX015     SQ-FS4                                          ST1444.2
007900*    X-XXX027     MERGE FILE ST-FS1                               ST1444.2
008000*    X-55         SYSTEM PRINTER NAME.                            ST1444.2
008100*    X-XXX064     NATIVE COLLATING SEQUENCE DESCENDING ORDER-NOTE ST1444.2
008200*        THAT THE QUOTE CHARACTER IS NOT TO APPEAR IN THE X-64    ST1444.2
008300*        CARD AND THE DOLLAR SIGN $ IS TO APPEAR TWICE WHEREVER   ST1444.2
008400*        THE $ BELONGS IN THE NATIVE COLLATING SEQUENCE).  IF     ST1444.2
008500*        THE NATIVE COLLATING SEQUENCE IS ACTUALLY THE ASCII      ST1444.2
008600*        COLLATING SEQUENCE SEE BELOW FOR A SAMPLE X-64 CARD..... ST1444.2
008700*                                                                 ST1444.2
008800*    X-64  "ZYXWVUTSRQPONMLKJIHGFEDCBA>=<;9876543210/.-,+*)($$ ". ST1444.2
008900*    X-82         SOURCE COMPUTER NAME.                           ST1444.2
009000*    X-83         OBJECT COMPUTER NAME.                           ST1444.2
009100*                                                                 ST1444.2
009200*                                                                 ST1444.2
009300*    OPTIONS RECOMMENDED -                                        ST1444.2
009400*    *   *OPT8  X  TO BE USED IF NECESSARY TO DUMP THE            ST1444.2
009500*                  FILES AS THEY ARE CREATED AND READ DURING      ST1444.2
009600*                  MRG-TESTS 3 THRU 8.                            ST1444.2
009700*                                                                 ST1444.2
009800*                                                                 ST1444.2
009900*    TEST DESCRIPTIONS -                                          ST1444.2
010000*    BLD-TEST-001     CHECKS THE CREATION OF SQ-FS1               ST1444.2
010100*    BLD-TEST-002     CHECKS THE CREATION OF SQ-FS2               ST1444.2
010200*    BLD-TEST-003     CHECKS THE CREATION OF SQ-FS3               ST1444.2
010300*    MRG-TEST-003     TESTS RECORDS 1-20 ON MERGE RESULT SQ-FS4   ST1444.2
010400*    MRG-TEST-004     TESTS RECORDS 21-40 ON MERGE RESULT SQ-FS4  ST1444.2
010500*    MRG-TEST-005     TESTS RECORDS 41-60 ON MERGE RESULT SQ-FS4  ST1444.2
010600*    MRG-TEST-006     TESTS RECORDS 61-80 ON MERGE RESULT SQ-FS4  ST1444.2
010700*    MRG-TEST-007     TESTS RECORDS 81-100 ON MERGE RESULT SQ-FS4 ST1444.2
010800*    MRG-TEST-008     TESTS RECORDS 101-102 ON MERGE RESULT SQ-FS4ST1444.2
010900*    MRG-TEST-009     AN EOF CHECK ON SQ-FS4                      ST1444.2
011000*    MRG-TEST-010     CHECK THAT THE NUMERIC KEY ON THE LAST      ST1444.2
011100*                     RECORD ON SQ-FS4 EQUALS 102                 ST1444.2
011200*                                                                 ST1444.2
011300*                                                                 ST1444.2
011400*    ************************************************************ ST1444.2
011500 ENVIRONMENT DIVISION.                                            ST1444.2
011600 CONFIGURATION SECTION.                                           ST1444.2
011700 SOURCE-COMPUTER.                                                 ST1444.2
011800     XXXXX082.                                                    ST1444.2
011900 OBJECT-COMPUTER.                                                 ST1444.2
012000     XXXXX083.                                                    ST1444.2
012100 INPUT-OUTPUT SECTION.                                            ST1444.2
012200 FILE-CONTROL.                                                    ST1444.2
012300     SELECT PRINT-FILE ASSIGN TO                                  ST1444.2
012400     XXXXX055.                                                    ST1444.2
012500     SELECT SQ-FS1 ASSIGN                                         ST1444.2
012600     XXXXX008                                                     ST1444.2
012700     ; ORGANIZATION IS SEQUENTIAL                                 ST1444.2
012800     ACCESS MODE SEQUENTIAL                                       ST1444.2
012900     RESERVE 2 AREAS.                                             ST1444.2
013000     SELECT SQ-FS2 ASSIGN TO                                      ST1444.2
013100     XXXXX014                                                     ST1444.2
013200     ORGANIZATION IS SEQUENTIAL                                   ST1444.2
013300     ACCESS MODE IS SEQUENTIAL                                    ST1444.2
013400     RESERVE 4 AREAS.                                             ST1444.2
013500     SELECT SQ-FS3 ASSIGN TO                                      ST1444.2
013600     XXXXX009                                                     ST1444.2
013700     ORGANIZATION SEQUENTIAL                                      ST1444.2
013800     ; ACCESS MODE IS SEQUENTIAL                                  ST1444.2
013900     RESERVE 6 AREAS.                                             ST1444.2
014000     SELECT SQ-FS4 ASSIGN TO                                      ST1444.2
014100     XXXXX015                                                     ST1444.2
014200     ORGANIZATION IS SEQUENTIAL                                   ST1444.2
014300     ; ACCESS MODE IS SEQUENTIAL                                  ST1444.2
014400     RESERVE 4 AREAS.                                             ST1444.2
014500     SELECT ST-FS1 ASSIGN TO                                      ST1444.2
014600     XXXXX027.                                                    ST1444.2
014700 I-O-CONTROL.                                                     ST1444.2
014800     SAME SORT AREA FOR SQ-FS1 ST-FS1,                            ST1444.2
014900     MULTIPLE FILE TAPE CONTAINS SQ-FS1 POSITION 1                ST1444.2
015000     SQ-FS3 POSITION 2.                                           ST1444.2
015100 DATA DIVISION.                                                   ST1444.2
015200 FILE SECTION.                                                    ST1444.2
015300 FD  PRINT-FILE.                                                  ST1444.2
015400 01  PRINT-REC PICTURE X(120).                                    ST1444.2
015500 01  DUMMY-RECORD PICTURE X(120).                                 ST1444.2
015600 FD  SQ-FS1                                                       ST1444.2
015700     LABEL RECORDS STANDARD                                       ST1444.2
015800C    VALUE OF                                                     ST1444.2
015900C    XXXXX074                                                     ST1444.2
016000C    IS                                                           ST1444.2
016100C    XXXXX075                                                     ST1444.2
016200*    XXXXX069                                                     ST1444.2
016300     BLOCK CONTAINS 1 RECORDS                                     ST1444.2
016400     RECORD CONTAINS 132 CHARACTERS                               ST1444.2
016500     DATA RECORDS SQ-FS1R1-F-G-132, SQ-FS1R2-F-G-132.             ST1444.2
016600 01  SQ-FS1R1-F-G-132.                                            ST1444.2
016700     10 REC-PREAMBLE PIC X(120).                                  ST1444.2
016800     10 REST-OF-1.                                                ST1444.2
016900     20 KEY-1.                                                    ST1444.2
017000         30 ALPHAN-KEY PIC X.                                     ST1444.2
017100         30 NUM-KEY PIC 999.                                      ST1444.2
017200     20 KEY-2.                                                    ST1444.2
017300         30 ALPHAN-KEY PIC X.                                     ST1444.2
017400         30 NUM-KEY PIC 999.                                      ST1444.2
017500     20 KEY-3.                                                    ST1444.2
017600         30 ALPHAN-KEY PIC X.                                     ST1444.2
017700         30 NUM-KEY PIC 999.                                      ST1444.2
017800 01  SQ-FS1R2-F-G-132.                                            ST1444.2
017900     02 FILLER PIC X(120).                                        ST1444.2
018000     02 GARBAGE PIC X(12).                                        ST1444.2
018100 FD  SQ-FS2                                                       ST1444.2
018200     LABEL RECORD STANDARD                                        ST1444.2
018300C    VALUE OF                                                     ST1444.2
018400C    XXXXX074                                                     ST1444.2
018500C    IS                                                           ST1444.2
018600C    XXXXX076                                                     ST1444.2
018700*    XXXXX069                                                     ST1444.2
018800     BLOCK CONTAINS 2 RECORDS                                     ST1444.2
018900     RECORD CONTAINS 132 CHARACTERS                               ST1444.2
019000     DATA RECORD SQ-FS2R1-F-G-132.                                ST1444.2
019100 01  SQ-FS2R1-F-G-132.                                            ST1444.2
019200     10 REC-PRE-2 PIC X(120).                                     ST1444.2
019300     10 REST-OF-2.                                                ST1444.2
019400     20 KEY-4.                                                    ST1444.2
019500         30 ALPHAN-KEY PIC X.                                     ST1444.2
019600         30 NUM-KEY PIC 999.                                      ST1444.2
019700     20 KEY-5.                                                    ST1444.2
019800         30 ALPHAN-KEY PIC X.                                     ST1444.2
019900         30 NUM-KEY PIC 999.                                      ST1444.2
020000     20 KEY-6.                                                    ST1444.2
020100         30 ALPHAN-KEY PIC X.                                     ST1444.2
020200         30 NUM-KEY PIC 999.                                      ST1444.2
020300 FD  SQ-FS3                                                       ST1444.2
020400     LABEL RECORDS STANDARD                                       ST1444.2
020500C    VALUE OF                                                     ST1444.2
020600C    XXXXX074                                                     ST1444.2
020700C    IS                                                           ST1444.2
020800C    XXXXX077                                                     ST1444.2
020900*    XXXXX069                                                     ST1444.2
021000     BLOCK CONTAINS 1 RECORDS                                     ST1444.2
021100     RECORD CONTAINS 132 CHARACTERS                               ST1444.2
021200     DATA RECORD SQ-FS3R1-F-G-132.                                ST1444.2
021300 01  SQ-FS3R1-F-G-132.                                            ST1444.2
021400     10 REC-PRE-3 PIC X(120).                                     ST1444.2
021500     10 REST-OF-3.                                                ST1444.2
021600     20 KEY-7.                                                    ST1444.2
021700         30 ALPHAN-KEY PIC X.                                     ST1444.2
021800         30 NUM-KEY PIC 999.                                      ST1444.2
021900     20 KEY-8.                                                    ST1444.2
022000         30 ALPHAN-KEY PIC X.                                     ST1444.2
022100         30 NUM-KEY PIC 999.                                      ST1444.2
022200     20 KEY-9.                                                    ST1444.2
022300         30 ALPHAN-KEY PIC X.                                     ST1444.2
022400         30 NUM-KEY PIC 999.                                      ST1444.2
022500 FD  SQ-FS4                                                       ST1444.2
022600     LABEL RECORD IS STANDARD                                     ST1444.2
022700C    ; VALUE OF                                                   ST1444.2
022800C    XXXXX074                                                     ST1444.2
022900C    IS                                                           ST1444.2
023000C    XXXXX078                                                     ST1444.2
023100*    XXXXX069                                                     ST1444.2
023200     ; BLOCK CONTAINS 3 RECORDS                                   ST1444.2
023300     RECORD CONTAINS 132 CHARACTERS                               ST1444.2
023400     DATA RECORD SQ-FS4R1-F-G-132.                                ST1444.2
023500 01  SQ-FS4R1-F-G-132.                                            ST1444.2
023600     10  REC-PRE-4 PIC X(120).                                    ST1444.2
023700     10  REST-OF-4.                                               ST1444.2
023800     20  KEY-10.                                                  ST1444.2
023900          30  ALPHAN-KEY PIC X.                                   ST1444.2
024000          30  NUM-KEY PIC 999.                                    ST1444.2
024100     20  KEY-11.                                                  ST1444.2
024200          30  ALPHAN-KEY PIC X.                                   ST1444.2
024300          30  NUM-KEY PIC 999.                                    ST1444.2
024400     20  KEY-12.                                                  ST1444.2
024500          30  ALPHAN-KEY PIC X.                                   ST1444.2
024600          30  NUM-KEY PIC 999.                                    ST1444.2
024700 SD  ST-FS1                                                       ST1444.2
024800     RECORD CONTAINS 132 CHARACTERS                               ST1444.2
024900     DATA RECORD IS ST-FS1R1-F-G-132.                             ST1444.2
025000 01  ST-FS1R1-F-G-132.                                            ST1444.2
025100     02 FILLER PIC X(120).                                        ST1444.2
025200     02 NON-KEY-1.                                                ST1444.2
025300         03 A-KEY PIC X.                                          ST1444.2
025400         03 N-KEY PIC 999.                                        ST1444.2
025500     02 SORT-KEY.                                                 ST1444.2
025600         03 A-KEY PIC X.                                          ST1444.2
025700         03 N-KEY PIC 999.                                        ST1444.2
025800     02 NON-KEY-2.                                                ST1444.2
025900         03 A-KEY PIC X.                                          ST1444.2
026000         03 N-KEY PIC 999.                                        ST1444.2
026100 WORKING-STORAGE SECTION.                                         ST1444.2
026200 77  WRK-DU-9-0001 PIC 9 VALUE 0.                                 ST1444.2
026300 77  WRK-DU-999-0001 PIC 999.                                     ST1444.2
026400 77  WRK-DU-999-2 PIC 999 VALUE 001.                              ST1444.2
026500 77  LAST-REC-NUM  PIC 999 VALUE ZERO.                            ST1444.2
026600 01  WRK-XN-0001 PIC X(51) VALUE                                  ST1444.2
026700     XXXXX064.                                                    ST1444.2
026800 01  WRK-XN-O051F-X-0001 REDEFINES WRK-XN-0001.                   ST1444.2
026900     02 CHAR PIC X OCCURS 51 TIMES.                               ST1444.2
027000 01  WRK-XN-2    PIC X(51) VALUE                                  ST1444.2
027100     XXXXX064.                                                    ST1444.2
027200 01  WRK-XN-0051F-X-0002 REDEFINES WRK-XN-2.                      ST1444.2
027300     02 ASCIIS PIC X OCCURS 51 TIMES.                             ST1444.2
027400 01  WRK-XN-O020F-0001.                                           ST1444.2
027500     02 COMPU PIC X OCCURS 20 TIMES.                              ST1444.2
027600 01  WRK-XN-X-0001 REDEFINES WRK-XN-O020F-0001.                   ST1444.2
027700     02 FILLER PIC X(20).                                         ST1444.2
027800 01  WRK-XN-O120F-1.                                              ST1444.2
027900     02 COLLS PIC X OCCURS 120 TIMES.                             ST1444.2
028000 01  WRK-XN-X-2 REDEFINES WRK-XN-O120F-1.                         ST1444.2
028100     02 WRK-XN-0002 PIC X(20).                                    ST1444.2
028200     02 WRK-XN-0003 PIC X(20).                                    ST1444.2
028300     02 WRK-XN-0004 PIC X(20).                                    ST1444.2
028400     02 WRK-XN-0005 PIC X(20).                                    ST1444.2
028500     02 WRK-XN-0006 PIC X(20).                                    ST1444.2
028600     02 WRK-XN-0007 PIC X(20).                                    ST1444.2
028700 01  FILE-RECORD-INFORMATION-REC.                                 ST1444.2
028800     03 FILE-RECORD-INFO-SKELETON.                                ST1444.2
028900        05 FILLER                 PICTURE X(48)       VALUE       ST1444.2
029000             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1444.2
029100        05 FILLER                 PICTURE X(46)       VALUE       ST1444.2
029200             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1444.2
029300        05 FILLER                 PICTURE X(26)       VALUE       ST1444.2
029400             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1444.2
029500        05 FILLER                 PICTURE X(37)       VALUE       ST1444.2
029600             ",RECKEY=                             ".             ST1444.2
029700        05 FILLER                 PICTURE X(38)       VALUE       ST1444.2
029800             ",ALTKEY1=                             ".            ST1444.2
029900        05 FILLER                 PICTURE X(38)       VALUE       ST1444.2
030000             ",ALTKEY2=                             ".            ST1444.2
030100        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1444.2
030200     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1444.2
030300        05 FILE-RECORD-INFO-P1-120.                               ST1444.2
030400           07 FILLER              PIC X(5).                       ST1444.2
030500           07 XFILE-NAME           PIC X(6).                      ST1444.2
030600           07 FILLER              PIC X(8).                       ST1444.2
030700           07 XRECORD-NAME         PIC X(6).                      ST1444.2
030800           07 FILLER              PIC X(1).                       ST1444.2
030900           07 REELUNIT-NUMBER     PIC 9(1).                       ST1444.2
031000           07 FILLER              PIC X(7).                       ST1444.2
031100           07 XRECORD-NUMBER       PIC 9(6).                      ST1444.2
031200           07 FILLER              PIC X(6).                       ST1444.2
031300           07 UPDATE-NUMBER       PIC 9(2).                       ST1444.2
031400           07 FILLER              PIC X(5).                       ST1444.2
031500           07 ODO-NUMBER          PIC 9(4).                       ST1444.2
031600           07 FILLER              PIC X(5).                       ST1444.2
031700           07 XPROGRAM-NAME        PIC X(5).                      ST1444.2
031800           07 FILLER              PIC X(7).                       ST1444.2
031900           07 XRECORD-LENGTH       PIC 9(6).                      ST1444.2
032000           07 FILLER              PIC X(7).                       ST1444.2
032100           07 CHARS-OR-RECORDS    PIC X(2).                       ST1444.2
032200           07 FILLER              PIC X(1).                       ST1444.2
032300           07 XBLOCK-SIZE          PIC 9(4).                      ST1444.2
032400           07 FILLER              PIC X(6).                       ST1444.2
032500           07 RECORDS-IN-FILE     PIC 9(6).                       ST1444.2
032600           07 FILLER              PIC X(5).                       ST1444.2
032700           07 XFILE-ORGANIZATION   PIC X(2).                      ST1444.2
032800           07 FILLER              PIC X(6).                       ST1444.2
032900           07 XLABEL-TYPE          PIC X(1).                      ST1444.2
033000        05 FILE-RECORD-INFO-P121-240.                             ST1444.2
033100           07 FILLER              PIC X(8).                       ST1444.2
033200           07 XRECORD-KEY          PIC X(29).                     ST1444.2
033300           07 FILLER              PIC X(9).                       ST1444.2
033400           07 ALTERNATE-KEY1      PIC X(29).                      ST1444.2
033500           07 FILLER              PIC X(9).                       ST1444.2
033600           07 ALTERNATE-KEY2      PIC X(29).                      ST1444.2
033700           07 FILLER              PIC X(7).                       ST1444.2
033800 01  TEST-RESULTS.                                                ST1444.2
033900     02 FILLER                   PIC X      VALUE SPACE.          ST1444.2
034000     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1444.2
034100     02 FILLER                   PIC X      VALUE SPACE.          ST1444.2
034200     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1444.2
034300     02 FILLER                   PIC X      VALUE SPACE.          ST1444.2
034400     02  PAR-NAME.                                                ST1444.2
034500       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1444.2
034600       03  PARDOT-X              PIC X      VALUE SPACE.          ST1444.2
034700       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1444.2
034800     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1444.2
034900     02 RE-MARK                  PIC X(61).                       ST1444.2
035000 01  TEST-COMPUTED.                                               ST1444.2
035100     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1444.2
035200     02 FILLER                   PIC X(17)  VALUE                 ST1444.2
035300            "       COMPUTED=".                                   ST1444.2
035400     02 COMPUTED-X.                                               ST1444.2
035500     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1444.2
035600     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1444.2
035700                                 PIC -9(9).9(9).                  ST1444.2
035800     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1444.2
035900     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1444.2
036000     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1444.2
036100     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1444.2
036200         04 COMPUTED-18V0                    PIC -9(18).          ST1444.2
036300         04 FILLER                           PIC X.               ST1444.2
036400     03 FILLER PIC X(50) VALUE SPACE.                             ST1444.2
036500 01  TEST-CORRECT.                                                ST1444.2
036600     02 FILLER PIC X(30) VALUE SPACE.                             ST1444.2
036700     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1444.2
036800     02 CORRECT-X.                                                ST1444.2
036900     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1444.2
037000     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1444.2
037100     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1444.2
037200     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1444.2
037300     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1444.2
037400     03      CR-18V0 REDEFINES CORRECT-A.                         ST1444.2
037500         04 CORRECT-18V0                     PIC -9(18).          ST1444.2
037600         04 FILLER                           PIC X.               ST1444.2
037700     03 FILLER PIC X(2) VALUE SPACE.                              ST1444.2
037800     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1444.2
037900 01  CCVS-C-1.                                                    ST1444.2
038000     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1444.2
038100-    "SS  PARAGRAPH-NAME                                          ST1444.2
038200-    "       REMARKS".                                            ST1444.2
038300     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1444.2
038400 01  CCVS-C-2.                                                    ST1444.2
038500     02 FILLER                     PIC X        VALUE SPACE.      ST1444.2
038600     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1444.2
038700     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1444.2
038800     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1444.2
038900     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1444.2
039000 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1444.2
039100 01  REC-CT                        PIC 99       VALUE ZERO.       ST1444.2
039200 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1444.2
039300 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1444.2
039400 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1444.2
039500 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1444.2
039600 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1444.2
039700 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1444.2
039800 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1444.2
039900 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1444.2
040000 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1444.2
040100 01  CCVS-H-1.                                                    ST1444.2
040200     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1444.2
040300     02  FILLER                    PIC X(42)    VALUE             ST1444.2
040400     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1444.2
040500     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1444.2
040600 01  CCVS-H-2A.                                                   ST1444.2
040700   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1444.2
040800   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1444.2
040900   02  FILLER                        PIC XXXX   VALUE             ST1444.2
041000     "4.2 ".                                                      ST1444.2
041100   02  FILLER                        PIC X(28)  VALUE             ST1444.2
041200            " COPY - NOT FOR DISTRIBUTION".                       ST1444.2
041300   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1444.2
041400                                                                  ST1444.2
041500 01  CCVS-H-2B.                                                   ST1444.2
041600   02  FILLER                        PIC X(15)  VALUE             ST1444.2
041700            "TEST RESULT OF ".                                    ST1444.2
041800   02  TEST-ID                       PIC X(9).                    ST1444.2
041900   02  FILLER                        PIC X(4)   VALUE             ST1444.2
042000            " IN ".                                               ST1444.2
042100   02  FILLER                        PIC X(12)  VALUE             ST1444.2
042200     " HIGH       ".                                              ST1444.2
042300   02  FILLER                        PIC X(22)  VALUE             ST1444.2
042400            " LEVEL VALIDATION FOR ".                             ST1444.2
042500   02  FILLER                        PIC X(58)  VALUE             ST1444.2
042600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1444.2
042700 01  CCVS-H-3.                                                    ST1444.2
042800     02  FILLER                      PIC X(34)  VALUE             ST1444.2
042900            " FOR OFFICIAL USE ONLY    ".                         ST1444.2
043000     02  FILLER                      PIC X(58)  VALUE             ST1444.2
043100     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1444.2
043200     02  FILLER                      PIC X(28)  VALUE             ST1444.2
043300            "  COPYRIGHT   1985 ".                                ST1444.2
043400 01  CCVS-E-1.                                                    ST1444.2
043500     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1444.2
043600     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1444.2
043700     02 ID-AGAIN                     PIC X(9).                    ST1444.2
043800     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1444.2
043900 01  CCVS-E-2.                                                    ST1444.2
044000     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1444.2
044100     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1444.2
044200     02 CCVS-E-2-2.                                               ST1444.2
044300         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1444.2
044400         03 FILLER                   PIC X      VALUE SPACE.      ST1444.2
044500         03 ENDER-DESC               PIC X(44)  VALUE             ST1444.2
044600            "ERRORS ENCOUNTERED".                                 ST1444.2
044700 01  CCVS-E-3.                                                    ST1444.2
044800     02  FILLER                      PIC X(22)  VALUE             ST1444.2
044900            " FOR OFFICIAL USE ONLY".                             ST1444.2
045000     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1444.2
045100     02  FILLER                      PIC X(58)  VALUE             ST1444.2
045200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1444.2
045300     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1444.2
045400     02 FILLER                       PIC X(15)  VALUE             ST1444.2
045500             " COPYRIGHT 1985".                                   ST1444.2
045600 01  CCVS-E-4.                                                    ST1444.2
045700     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1444.2
045800     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1444.2
045900     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1444.2
046000     02 FILLER                       PIC X(40)  VALUE             ST1444.2
046100      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1444.2
046200 01  XXINFO.                                                      ST1444.2
046300     02 FILLER                       PIC X(19)  VALUE             ST1444.2
046400            "*** INFORMATION ***".                                ST1444.2
046500     02 INFO-TEXT.                                                ST1444.2
046600       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1444.2
046700       04 XXCOMPUTED                 PIC X(20).                   ST1444.2
046800       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1444.2
046900       04 XXCORRECT                  PIC X(20).                   ST1444.2
047000     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1444.2
047100 01  HYPHEN-LINE.                                                 ST1444.2
047200     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1444.2
047300     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1444.2
047400-    "*****************************************".                 ST1444.2
047500     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1444.2
047600-    "******************************".                            ST1444.2
047700 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1444.2
047800     "ST144A".                                                    ST1444.2
047900 PROCEDURE DIVISION.                                              ST1444.2
048000 DECLARATIVES.                                                    ST1444.2
048100 SECT-ST214-DEC SECTION.                                          ST1444.2
048200     USE AFTER STANDARD EXCEPTION PROCEDURE ON INPUT.             ST1444.2
048300 SRT-WRITE-DEC.                                                   ST1444.2
048400     MOVE "ERROR ON OUTPUT DECL." TO FEATURE.                     ST1444.2
048500     MOVE "SRT-TEST-DEC" TO PAR-NAME.                             ST1444.2
048600     WRITE   PRINT-REC FROM TEST-RESULTS AFTER ADVANCING 2 LINES. ST1444.2
048700     STOP RUN.                                                    ST1444.2
048800 END DECLARATIVES.                                                ST1444.2
048900 CCVS1 SECTION.                                                   ST1444.2
049000 OPEN-FILES.                                                      ST1444.2
049100     OPEN    OUTPUT PRINT-FILE.                                   ST1444.2
049200     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1444.2
049300     MOVE    SPACE TO TEST-RESULTS.                               ST1444.2
049400     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1444.2
049500     MOVE    ZERO TO REC-SKL-SUB.                                 ST1444.2
049600     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1444.2
049700 CCVS-INIT-FILE.                                                  ST1444.2
049800     ADD     1 TO REC-SKL-SUB.                                    ST1444.2
049900     MOVE    FILE-RECORD-INFO-SKELETON                            ST1444.2
050000          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1444.2
050100 CCVS-INIT-EXIT.                                                  ST1444.2
050200     GO TO CCVS1-EXIT.                                            ST1444.2
050300 CLOSE-FILES.                                                     ST1444.2
050400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1444.2
050500 TERMINATE-CCVS.                                                  ST1444.2
050600*    EXIT PROGRAM.                                                ST1444.2
050700*TERMINATE-CALL.                                                  ST1444.2
050800     STOP     RUN.                                                ST1444.2
050900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1444.2
051000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1444.2
051100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1444.2
051200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1444.2
051300     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1444.2
051400 PRINT-DETAIL.                                                    ST1444.2
051500     IF REC-CT NOT EQUAL TO ZERO                                  ST1444.2
051600             MOVE "." TO PARDOT-X                                 ST1444.2
051700             MOVE REC-CT TO DOTVALUE.                             ST1444.2
051800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1444.2
051900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1444.2
052000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1444.2
052100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1444.2
052200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1444.2
052300     MOVE SPACE TO CORRECT-X.                                     ST1444.2
052400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1444.2
052500     MOVE     SPACE TO RE-MARK.                                   ST1444.2
052600 HEAD-ROUTINE.                                                    ST1444.2
052700     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1444.2
052800     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1444.2
052900     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1444.2
053000     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1444.2
053100 COLUMN-NAMES-ROUTINE.                                            ST1444.2
053200     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1444.2
053300     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1444.2
053400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1444.2
053500 END-ROUTINE.                                                     ST1444.2
053600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1444.2
053700 END-RTN-EXIT.                                                    ST1444.2
053800     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1444.2
053900 END-ROUTINE-1.                                                   ST1444.2
054000      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1444.2
054100      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1444.2
054200      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1444.2
054300*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1444.2
054400      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1444.2
054500      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1444.2
054600      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1444.2
054700      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1444.2
054800  END-ROUTINE-12.                                                 ST1444.2
054900      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1444.2
055000     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1444.2
055100         MOVE "NO " TO ERROR-TOTAL                                ST1444.2
055200         ELSE                                                     ST1444.2
055300         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1444.2
055400     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1444.2
055500     PERFORM WRITE-LINE.                                          ST1444.2
055600 END-ROUTINE-13.                                                  ST1444.2
055700     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1444.2
055800         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1444.2
055900         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1444.2
056000     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1444.2
056100     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1444.2
056200      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1444.2
056300          MOVE "NO " TO ERROR-TOTAL                               ST1444.2
056400      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1444.2
056500      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1444.2
056600      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1444.2
056700     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1444.2
056800 WRITE-LINE.                                                      ST1444.2
056900     ADD 1 TO RECORD-COUNT.                                       ST1444.2
057000*    IF RECORD-COUNT GREATER 42                                   ST1444.2
057100*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1444.2
057200*        MOVE SPACE TO DUMMY-RECORD                               ST1444.2
057300*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1444.2
057400*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1444.2
057500*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1444.2
057600*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1444.2
057700*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1444.2
057800*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1444.2
057900*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1444.2
058000*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1444.2
058100*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1444.2
058200*        MOVE ZERO TO RECORD-COUNT.                               ST1444.2
058300     PERFORM WRT-LN.                                              ST1444.2
058400 WRT-LN.                                                          ST1444.2
058500     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1444.2
058600     MOVE SPACE TO DUMMY-RECORD.                                  ST1444.2
058700 BLANK-LINE-PRINT.                                                ST1444.2
058800     PERFORM WRT-LN.                                              ST1444.2
058900 FAIL-ROUTINE.                                                    ST1444.2
059000     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1444.2
059100            GO TO   FAIL-ROUTINE-WRITE.                           ST1444.2
059200     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1444.2
059300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1444.2
059400     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1444.2
059500     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1444.2
059600     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1444.2
059700     GO TO  FAIL-ROUTINE-EX.                                      ST1444.2
059800 FAIL-ROUTINE-WRITE.                                              ST1444.2
059900     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1444.2
060000     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1444.2
060100     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1444.2
060200     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1444.2
060300 FAIL-ROUTINE-EX. EXIT.                                           ST1444.2
060400 BAIL-OUT.                                                        ST1444.2
060500     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1444.2
060600     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1444.2
060700 BAIL-OUT-WRITE.                                                  ST1444.2
060800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1444.2
060900     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1444.2
061000     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1444.2
061100     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1444.2
061200 BAIL-OUT-EX. EXIT.                                               ST1444.2
061300 CCVS1-EXIT.                                                      ST1444.2
061400     EXIT.                                                        ST1444.2
061500 SECT-ST214-0001 SECTION.                                         ST1444.2
061600 BLD-INIT-001.                                                    ST1444.2
061700     MOVE "CREATE FILE SQ-FS1" TO FEATURE.                        ST1444.2
061800     OPEN OUTPUT SQ-FS1.                                          ST1444.2
061900     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1444.2
062000     MOVE "R1-F-G" TO XRECORD-NAME (1).                           ST1444.2
062100     MOVE CCVS-PGM-ID TO XPROGRAM-NAME (1).                       ST1444.2
062200     MOVE 000132 TO XRECORD-LENGTH (1).                           ST1444.2
062300     MOVE "RC" TO CHARS-OR-RECORDS (1).                           ST1444.2
062400     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1444.2
062500     MOVE 000051 TO RECORDS-IN-FILE (1).                          ST1444.2
062600     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1444.2
062700     MOVE "S" TO XLABEL-TYPE (1).                                 ST1444.2
062800     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1444.2
062900             MOVE SPACES TO WRK-XN-O120F-1.                       ST1444.2
063000 BLD-TEST-001.                                                    ST1444.2
063100     PERFORM BLD-TEST-001-BUILD VARYING WRK-DU-999-0001           ST1444.2
063200         FROM 1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.    ST1444.2
063300*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
063400*    WRITE   PRINT-REC.                                           ST1444.2
063500     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1444.2
063600         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1444.2
063700         ELSE                                                     ST1444.2
063800         PERFORM PASS.                                            ST1444.2
063900     GO TO BLD-WRITE-001.                                         ST1444.2
064000 BLD-TEST-001-BUILD.                                              ST1444.2
064100     MOVE "JUNKSLOPJUNK" TO GARBAGE.                              ST1444.2
064200     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-1 NUM-KEY OF KEY-2    ST1444.2
064300         NUM-KEY OF KEY-3.                                        ST1444.2
064400     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PREAMBLE.            ST1444.2
064500     ADD 1 TO XRECORD-NUMBER (1).                                 ST1444.2
064600     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1444.2
064700     ADD 1 TO WRK-DU-999-2.                                       ST1444.2
064800     MOVE ASCIIS (WRK-DU-999-0001) TO COLLS (WRK-DU-999-2).       ST1444.2
064900     ADD 1 TO WRK-DU-999-2.                                       ST1444.2
065000*    WRITE PRINT-REC FROM SQ-FS1R1-F-G-132.                       ST1444.2
065100*    WRITE PRINT-REC FROM REST-OF-1.                              ST1444.2
065200*    MOVE SPACES TO PRINT-REC.                                    ST1444.2
065300     WRITE   SQ-FS1R1-F-G-132.                                    ST1444.2
065400 BLD-DELETE-001.                                                  ST1444.2
065500     PERFORM DE-LETE.                                             ST1444.2
065600 BLD-WRITE-001.                                                   ST1444.2
065700     MOVE "BLD-TEST-001" TO PAR-NAME.                             ST1444.2
065800     MOVE "FIRST FILE CREATED" TO COMPUTED-A.                     ST1444.2
065900     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1444.2
066000     PERFORM PRINT-DETAIL.                                        ST1444.2
066100*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
066200*    WRITE   PRINT-REC.                                           ST1444.2
066300     CLOSE SQ-FS1 WITH NO REWIND.                                 ST1444.2
066400 BLD-INIT-002.                                                    ST1444.2
066500     MOVE "CREATE FILE SQ-FS2" TO FEATURE.                        ST1444.2
066600     OPEN OUTPUT SQ-FS2.                                          ST1444.2
066700     MOVE  "SQ-FS2" TO XFILE-NAME (1).                            ST1444.2
066800     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1444.2
066900     MOVE 0002 TO XBLOCK-SIZE (1).                                ST1444.2
067000 BLD-TEST-002.                                                    ST1444.2
067100     PERFORM BLD-TEST-002-BUILD VARYING WRK-DU-999-0001 FROM      ST1444.2
067200         1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.         ST1444.2
067300*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
067400*    WRITE   PRINT-REC.                                           ST1444.2
067500     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 000052                 ST1444.2
067600         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1444.2
067700         ELSE                                                     ST1444.2
067800         PERFORM PASS.                                            ST1444.2
067900     GO TO BLD-WRITE-002.                                         ST1444.2
068000 BLD-TEST-002-BUILD.                                              ST1444.2
068100     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-4           ST1444.2
068200         ALPHAN-KEY OF KEY-5 ALPHAN-KEY OF KEY-6.                 ST1444.2
068300     ADD 51 WRK-DU-999-0001 GIVING NUM-KEY OF KEY-4               ST1444.2
068400         NUM-KEY OF KEY-5 NUM-KEY OF KEY-6.                       ST1444.2
068500     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-2.               ST1444.2
068600     ADD 000001 TO XRECORD-NUMBER (1).                            ST1444.2
068700*    WRITE PRINT-REC FROM SQ-FS2R1-F-G-132.                       ST1444.2
068800*    WRITE PRINT-REC FROM REST-OF-2.                              ST1444.2
068900*    MOVE SPACES TO PRINT-REC.                                    ST1444.2
069000     WRITE   SQ-FS2R1-F-G-132.                                    ST1444.2
069100 BLD-DELETE-002.                                                  ST1444.2
069200     PERFORM DE-LETE.                                             ST1444.2
069300 BLD-WRITE-002.                                                   ST1444.2
069400     MOVE "BLD-TEST-002" TO PAR-NAME.                             ST1444.2
069500     MOVE "2ND FILE CREATED" TO COMPUTED-A.                       ST1444.2
069600     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1444.2
069700     PERFORM PRINT-DETAIL.                                        ST1444.2
069800*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
069900*    WRITE   PRINT-REC.                                           ST1444.2
070000     CLOSE SQ-FS2.                                                ST1444.2
070100 BLD-INIT-003.                                                    ST1444.2
070200     MOVE "CREATE FILE SQ-FS3" TO FEATURE.                        ST1444.2
070300     OPEN    OUTPUT SQ-FS3.                                       ST1444.2
070400     MOVE  "SQ-FS3" TO XFILE-NAME (1).                            ST1444.2
070500     MOVE 000001 TO XRECORD-NUMBER (1).                           ST1444.2
070600     MOVE 0001 TO XBLOCK-SIZE (1).                                ST1444.2
070700 BLD-TEST-003.                                                    ST1444.2
070800     PERFORM BLD-TEST-003-BUILD VARYING WRK-DU-999-0001 FROM      ST1444.2
070900         1 BY 1 UNTIL WRK-DU-999-0001 IS GREATER THAN 51.         ST1444.2
071000*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
071100*    WRITE   PRINT-REC.                                           ST1444.2
071200     IF XRECORD-NUMBER (1) IS NOT EQUAL TO 52                     ST1444.2
071300         PERFORM FAIL MOVE "INCORR. NO. OF RECS." TO RE-MARK      ST1444.2
071400         ELSE                                                     ST1444.2
071500         PERFORM PASS.                                            ST1444.2
071600     GO TO BLD-WRITE-003.                                         ST1444.2
071700 BLD-TEST-003-BUILD.                                              ST1444.2
071800     MOVE CHAR (WRK-DU-999-0001) TO ALPHAN-KEY OF KEY-7           ST1444.2
071900         ALPHAN-KEY OF KEY-8 ALPHAN-KEY OF KEY-9.                 ST1444.2
072000     MOVE WRK-DU-999-0001 TO NUM-KEY OF KEY-7 NUM-KEY OF KEY-8    ST1444.2
072100         NUM-KEY OF KEY-9.                                        ST1444.2
072200     MOVE FILE-RECORD-INFO-P1-120 (1) TO REC-PRE-3.               ST1444.2
072300     ADD 000001 TO XRECORD-NUMBER (1).                            ST1444.2
072400*    WRITE PRINT-REC FROM SQ-FS3R1-F-G-132.                       ST1444.2
072500*    WRITE PRINT-REC FROM REST-OF-3.                              ST1444.2
072600*    MOVE SPACES TO PRINT-REC.                                    ST1444.2
072700     WRITE   SQ-FS3R1-F-G-132.                                    ST1444.2
072800 BLD-DELETE-003.                                                  ST1444.2
072900     PERFORM DE-LETE.                                             ST1444.2
073000 BLD-WRITE-003.                                                   ST1444.2
073100     MOVE "BLD-TEST-003" TO PAR-NAME.                             ST1444.2
073200     MOVE "3RD FILE CREATED" TO COMPUTED-A.                       ST1444.2
073300     MOVE XRECORD-NUMBER (1) TO CORRECT-18V0.                     ST1444.2
073400     PERFORM PRINT-DETAIL.                                        ST1444.2
073500*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
073600*    WRITE   PRINT-REC.                                           ST1444.2
073700     CLOSE SQ-FS3.                                                ST1444.2
073800 MRG-INIT-001.                                                    ST1444.2
073900     MERGE ST-FS1                                                 ST1444.2
074000         ON DESCENDING KEY A-KEY OF SORT-KEY                      ST1444.2
074100         ASCENDING N-KEY OF NON-KEY-2                             ST1444.2
074200         USING SQ-FS2, SQ-FS3                                     ST1444.2
074300     OUTPUT PROCEDURE IS SECT-ST214-0002.                         ST1444.2
074400 SRT-TEST-003.                                                    ST1444.2
074500     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
074600     OPEN INPUT SQ-FS4.                                           ST1444.2
074700     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
074800         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1444.2
074900*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
075000*    WRITE   PRINT-REC.                                           ST1444.2
075100     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0002                 ST1444.2
075200         PERFORM FAIL GO TO SRT-FAIL-003                          ST1444.2
075300         ELSE                                                     ST1444.2
075400         PERFORM PASS.                                            ST1444.2
075500     GO TO SRT-WRITE-003.                                         ST1444.2
075600 SRT-DELETE-003.                                                  ST1444.2
075700     PERFORM DE-LETE.                                             ST1444.2
075800     GO TO SRT-WRITE-003.                                         ST1444.2
075900 SRT-FAIL-003.                                                    ST1444.2
076000     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
076100     MOVE WRK-XN-0002 TO CORRECT-A.                               ST1444.2
076200 SRT-WRITE-003.                                                   ST1444.2
076300     MOVE "MRG-TEST-003" TO PAR-NAME.                             ST1444.2
076400     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
076500     PERFORM PRINT-DETAIL.                                        ST1444.2
076600*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
076700*    WRITE   PRINT-REC.                                           ST1444.2
076800 SRT-INIT-004.                                                    ST1444.2
076900     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
077000 SRT-TEST-004.                                                    ST1444.2
077100     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
077200         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1444.2
077300*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
077400*    WRITE   PRINT-REC.                                           ST1444.2
077500     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0003                 ST1444.2
077600         PERFORM FAIL GO TO SRT-FAIL-004                          ST1444.2
077700         ELSE                                                     ST1444.2
077800         PERFORM PASS.                                            ST1444.2
077900     GO TO SRT-WRITE-004.                                         ST1444.2
078000 SRT-DELETE-004.                                                  ST1444.2
078100     PERFORM DE-LETE.                                             ST1444.2
078200     GO TO SRT-WRITE-004.                                         ST1444.2
078300 SRT-FAIL-004.                                                    ST1444.2
078400     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
078500     MOVE WRK-XN-0003 TO CORRECT-A.                               ST1444.2
078600 SRT-WRITE-004.                                                   ST1444.2
078700     MOVE "MRG-TEST-004" TO PAR-NAME.                             ST1444.2
078800     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
078900     PERFORM PRINT-DETAIL.                                        ST1444.2
079000*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
079100*    WRITE   PRINT-REC.                                           ST1444.2
079200 SRT-INIT-005.                                                    ST1444.2
079300     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
079400 SRT-TEST-005.                                                    ST1444.2
079500     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
079600         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1444.2
079700*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
079800*    WRITE   PRINT-REC.                                           ST1444.2
079900     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0004                 ST1444.2
080000         PERFORM FAIL GO TO SRT-FAIL-005                          ST1444.2
080100         ELSE                                                     ST1444.2
080200         PERFORM PASS.                                            ST1444.2
080300     GO TO SRT-WRITE-005.                                         ST1444.2
080400 SRT-DELETE-005.                                                  ST1444.2
080500     PERFORM DE-LETE.                                             ST1444.2
080600     GO TO SRT-WRITE-005.                                         ST1444.2
080700 SRT-FAIL-005.                                                    ST1444.2
080800     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
080900     MOVE WRK-XN-0004 TO CORRECT-A.                               ST1444.2
081000 SRT-WRITE-005.                                                   ST1444.2
081100     MOVE "MRG-TEST-005" TO PAR-NAME.                             ST1444.2
081200     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
081300     PERFORM PRINT-DETAIL.                                        ST1444.2
081400*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
081500*    WRITE   PRINT-REC.                                           ST1444.2
081600 SRT-INIT-006.                                                    ST1444.2
081700     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
081800 SRT-TEST-006.                                                    ST1444.2
081900     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
082000         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1444.2
082100*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
082200*    WRITE   PRINT-REC.                                           ST1444.2
082300     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0005                 ST1444.2
082400         PERFORM FAIL GO TO SRT-FAIL-006                          ST1444.2
082500         ELSE                                                     ST1444.2
082600         PERFORM PASS.                                            ST1444.2
082700     GO TO SRT-WRITE-006.                                         ST1444.2
082800 SRT-DELETE-006.                                                  ST1444.2
082900     PERFORM DE-LETE.                                             ST1444.2
083000     GO TO SRT-WRITE-006.                                         ST1444.2
083100 SRT-FAIL-006.                                                    ST1444.2
083200     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
083300     MOVE WRK-XN-0005 TO CORRECT-A.                               ST1444.2
083400 SRT-WRITE-006.                                                   ST1444.2
083500     MOVE "MRG-TEST-006" TO PAR-NAME.                             ST1444.2
083600     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
083700     PERFORM PRINT-DETAIL.                                        ST1444.2
083800*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
083900*    WRITE   PRINT-REC.                                           ST1444.2
084000 SRT-INIT-007.                                                    ST1444.2
084100     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
084200 SRT-TEST-007.                                                    ST1444.2
084300     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
084400         UNTIL WRK-DU-999-0001 IS GREATER THAN 20.                ST1444.2
084500*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
084600*    WRITE   PRINT-REC.                                           ST1444.2
084700     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0006                 ST1444.2
084800         PERFORM FAIL GO TO SRT-FAIL-007                          ST1444.2
084900         ELSE                                                     ST1444.2
085000         PERFORM PASS.                                            ST1444.2
085100     GO TO SRT-WRITE-007.                                         ST1444.2
085200 SRT-DELETE-007.                                                  ST1444.2
085300     PERFORM DE-LETE.                                             ST1444.2
085400     GO TO SRT-WRITE-007.                                         ST1444.2
085500 SRT-FAIL-007.                                                    ST1444.2
085600     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
085700     MOVE WRK-XN-0006 TO CORRECT-A.                               ST1444.2
085800 SRT-WRITE-007.                                                   ST1444.2
085900     MOVE "MRG-TEST-007" TO PAR-NAME.                             ST1444.2
086000     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
086100     PERFORM PRINT-DETAIL.                                        ST1444.2
086200*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
086300*    WRITE   PRINT-REC.                                           ST1444.2
086400 SRT-INIT-008.                                                    ST1444.2
086500     MOVE SPACES TO WRK-XN-X-0001.                                ST1444.2
086600 SRT-TEST-008.                                                    ST1444.2
086700     PERFORM RD-1 THRU R1-EXIT VARYING WRK-DU-999-0001 FROM 1 BY 1ST1444.2
086800         UNTIL WRK-DU-999-0001 IS GREATER THAN 2.                 ST1444.2
086900*    MOVE    SPACES TO PRINT-REC.                                 ST1444.2
087000*    WRITE   PRINT-REC.                                           ST1444.2
087100     IF WRK-XN-X-0001 IS NOT EQUAL TO WRK-XN-0007                 ST1444.2
087200         PERFORM FAIL GO TO SRT-FAIL-008                          ST1444.2
087300         ELSE                                                     ST1444.2
087400         PERFORM PASS.                                            ST1444.2
087500     GO TO SRT-WRITE-008.                                         ST1444.2
087600 SRT-DELETE-008.                                                  ST1444.2
087700     PERFORM DE-LETE.                                             ST1444.2
087800     GO TO SRT-WRITE-008.                                         ST1444.2
087900 SRT-FAIL-008.                                                    ST1444.2
088000     MOVE WRK-XN-X-0001 TO COMPUTED-A.                            ST1444.2
088100     MOVE WRK-XN-0007 TO CORRECT-A.                               ST1444.2
088200 SRT-WRITE-008.                                                   ST1444.2
088300     MOVE "MRG-TEST-008" TO PAR-NAME.                             ST1444.2
088400     MOVE "NATIVE COLL.SEQUENCE " TO FEATURE.                     ST1444.2
088500     PERFORM PRINT-DETAIL.                                        ST1444.2
088600     MOVE    NUM-KEY OF KEY-11 TO LAST-REC-NUM.                   ST1444.2
088700 SRT-TEST-009.                                                    ST1444.2
088800     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1444.2
088900         GO TO SRT-FAIL-009.                                      ST1444.2
089000     READ SQ-FS4 AT END PERFORM PASS                              ST1444.2
089100         GO TO SRT-WRITE-009.                                     ST1444.2
089200     GO TO SRT-FAIL-009.                                          ST1444.2
089300 SRT-DELETE-009.                                                  ST1444.2
089400     PERFORM DE-LETE.                                             ST1444.2
089500     GO TO SRT-WRITE-009.                                         ST1444.2
089600 SRT-FAIL-009.                                                    ST1444.2
089700     MOVE "EOF NOT FOUND" TO RE-MARK.                             ST1444.2
089800     PERFORM FAIL .                                               ST1444.2
089900 SRT-WRITE-009.                                                   ST1444.2
090000     MOVE "EOF CHECK SQ-FS4" TO FEATURE.                          ST1444.2
090100     MOVE "MRG-TEST-009" TO PAR-NAME.                             ST1444.2
090200     PERFORM PRINT-DETAIL.                                        ST1444.2
090300 SRT-TEST-010.                                                    ST1444.2
090400     IF LAST-REC-NUM IS NOT EQUAL TO 102                          ST1444.2
090500         PERFORM FAIL GO TO SRT-FAIL-010                          ST1444.2
090600         ELSE                                                     ST1444.2
090700         PERFORM PASS.                                            ST1444.2
090800     GO TO SRT-WRITE-010.                                         ST1444.2
090900 SRT-DELETE-010.                                                  ST1444.2
091000     PERFORM DE-LETE.                                             ST1444.2
091100     GO TO SRT-WRITE-010.                                         ST1444.2
091200 SRT-FAIL-010.                                                    ST1444.2
091300     MOVE LAST-REC-NUM TO COMPUTED-18V0.                          ST1444.2
091400     MOVE 102 TO CR-18V0.                                         ST1444.2
091500 SRT-WRITE-010.                                                   ST1444.2
091600     MOVE "NUMER. SEQ. CHECK" TO FEATURE.                         ST1444.2
091700     MOVE "MRG-TEST-010" TO PAR-NAME.                             ST1444.2
091800     PERFORM PRINT-DETAIL.                                        ST1444.2
091900     CLOSE   SQ-FS4.                                              ST1444.2
092000     GO TO CCVS-999999.                                           ST1444.2
092100 READ-SQ-FS1 SECTION.                                             ST1444.2
092200 RD-1.                                                            ST1444.2
092300     IF WRK-DU-9-0001 IS NOT EQUAL TO ZERO                        ST1444.2
092400         GO TO R1-EXIT.                                           ST1444.2
092500     READ SQ-FS4 AT END GO TO PREMATURE-EOF.                      ST1444.2
092600*    WRITE PRINT-REC FROM SQ-FS4R1-F-G-132.                       ST1444.2
092700*    WRITE PRINT-REC FROM REST-OF-4.                              ST1444.2
092800*    MOVE SPACES TO PRINT-REC.                                    ST1444.2
092900     MOVE ALPHAN-KEY OF KEY-12 TO COMPU (WRK-DU-999-0001).        ST1444.2
093000     GO TO R1-EXIT.                                               ST1444.2
093100 PREMATURE-EOF.                                                   ST1444.2
093200     MOVE 1 TO WRK-DU-9-0001.                                     ST1444.2
093300     MOVE "PREMATURE EOF FOUND" TO RE-MARK.                       ST1444.2
093400 R1-EXIT.                                                         ST1444.2
093500     EXIT.                                                        ST1444.2
093600 SECT-ST214-0002 SECTION.                                         ST1444.2
093700 SORT-OUTPUT-PROC.                                                ST1444.2
093800     OPEN OUTPUT SQ-FS4.                                          ST1444.2
093900 RETURN-THE-OLD-RECORDS.                                          ST1444.2
094000     RETURN ST-FS1 RECORD INTO SQ-FS4R1-F-G-132                   ST1444.2
094100         AT END GO TO CLOSE-AFTER-SORT.                           ST1444.2
094200*    WRITE PRINT-REC FROM SQ-FS4R1-F-G-132.                       ST1444.2
094300*    WRITE PRINT-REC FROM REST-OF-4.                              ST1444.2
094400*    MOVE SPACES TO PRINT-REC.                                    ST1444.2
094500     WRITE   SQ-FS4R1-F-G-132.                                    ST1444.2
094600     GO TO RETURN-THE-OLD-RECORDS.                                ST1444.2
094700 CLOSE-AFTER-SORT.                                                ST1444.2
094800     CLOSE SQ-FS4.                                                ST1444.2
094900 CCVS-EXIT SECTION.                                               ST1444.2
095000 CCVS-999999.                                                     ST1444.2
095100     GO TO CLOSE-FILES.                                           ST1444.2
      *END-OF,ST144A                                                                  
