      *HEADER,COBOL,ST146A                                                    TES00010
000100 IDENTIFICATION DIVISION.                                         ST1464.2
000200 PROGRAM-ID.                                                      ST1464.2
000300     ST146A.                                                      ST1464.2
000400****************************************************************  ST1464.2
000500*                                                              *  ST1464.2
000600*    VALIDATION FOR:-                                          *  ST1464.2
000700*                                                              *  ST1464.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1464.2
000900*                                                              *  ST1464.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1464.2
001100*                                                              *  ST1464.2
001200****************************************************************  ST1464.2
001300*                                                              *  ST1464.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  ST1464.2
001500*                                                              *  ST1464.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  ST1464.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  ST1464.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  ST1464.2
001900*                                                              *  ST1464.2
002000****************************************************************  ST1464.2
002100*                                                                 ST1464.2
002200*    ST146A TESTS OPERATIONS INVOLVING FORMAT 2  OCCURS CLAUSES,  ST1464.2
002300*    I.E.  ...OCCURS INTEGER-1 TO INTEGER-2 TIMES DEPENDING ON    ST1464.2
002400*          DATA-NAME-1 ....                                       ST1464.2
002500*    X3.23-1976, PAGE III-4, 2.1.4(3) STATES, IN PART, THAT       ST1464.2
002600*    INTEGER-2 REPRESENTS THE MAXIMUM NUMBER OF OCCURRENCES AND   ST1464.2
002700*    THAT ONLY THE NUMBER OF OCCURRENCES, AND NOT THE ITEM LENGTH,ST1464.2
002800*    IS VARIABLE.  WHENEVER THE PARENT GROUP ITEM IS REFERENCED,  ST1464.2
002900*    ONLY THE PORTION OF THE TABLE SPECIFIED BY THE CURRENT VALUE ST1464.2
003000*    OF DATA-NAME-1 WILL BE USED IN THE OPERATION.                ST1464.2
003100*                                                                 ST1464.2
003200*    THE SORT VERB IS EXERCIZED BUT NOT CHECKED FOR ITS           ST1464.2
003300*    CAPABILITY TO MEANINGFULLY SORT A FILE.  INSTEAD, THE        ST1464.2
003400*    OBJECT OF ST146A IS TO CHECK THE BEHAVIOR OF VARIABLE        ST1464.2
003500*    LENGTH TABLES BEING HANDLED IN THE IMPLICIT MOVES            ST1464.2
003600*    RESULTING FROM                                               ST1464.2
003700*        RELEASE ... FROM ...                                     ST1464.2
003800*            AND                                                  ST1464.2
003900*        RETURN ... INTO ...                                      ST1464.2
004000*            STATEMENTS.                                          ST1464.2
004100*                                                                 ST1464.2
004200*                                                                 ST1464.2
004300 ENVIRONMENT DIVISION.                                            ST1464.2
004400 CONFIGURATION SECTION.                                           ST1464.2
004500 SOURCE-COMPUTER.                                                 ST1464.2
004600     XXXXX082.                                                    ST1464.2
004700 OBJECT-COMPUTER.                                                 ST1464.2
004800     XXXXX083.                                                    ST1464.2
004900 INPUT-OUTPUT SECTION.                                            ST1464.2
005000 FILE-CONTROL.                                                    ST1464.2
005100     SELECT PRINT-FILE ASSIGN TO                                  ST1464.2
005200     XXXXX055.                                                    ST1464.2
005300     SELECT  SQ-FS1  ASSIGN TO                                    ST1464.2
005400     XXXXX014.                                                    ST1464.2
005500     SELECT SQ-FS2 ASSIGN TO                                      ST1464.2
005600     XXXXX015.                                                    ST1464.2
005700     SELECT ST-FR1 ASSIGN TO                                      ST1464.2
005800     XXXXX027.                                                    ST1464.2
005900 DATA DIVISION.                                                   ST1464.2
006000 FILE SECTION.                                                    ST1464.2
006100 FD  PRINT-FILE.                                                  ST1464.2
006200 01  PRINT-REC PICTURE X(120).                                    ST1464.2
006300 01  DUMMY-RECORD PICTURE X(120).                                 ST1464.2
006400 FD  SQ-FS1                                                       ST1464.2
006500C    VALUE OF                                                     ST1464.2
006600C    XXXXX074                                                     ST1464.2
006700C    IS                                                           ST1464.2
006800C    XXXXX075                                                     ST1464.2
006900*    XXXXX069                                                     ST1464.2
007000     LABEL RECORD IS STANDARD.                                    ST1464.2
007100 01  SQ-FS1R1-F-G-140.                                            ST1464.2
007200     02 FS1R1-XN-120 PIC X(120).                                  ST1464.2
007300     02 FS1R1-XN-20  PIC X(20).                                   ST1464.2
007400 FD  SQ-FS2                                                       ST1464.2
007500C    VALUE OF                                                     ST1464.2
007600C    XXXXX074                                                     ST1464.2
007700C    IS                                                           ST1464.2
007800C    XXXXX076                                                     ST1464.2
007900*    XXXXX069                                                     ST1464.2
008000     LABEL RECORD IS STANDARD.                                    ST1464.2
008100 01  SQ-FS2R1-F-G-140.                                            ST1464.2
008200     02  FS2R1-XN-120 PIC X(120).                                 ST1464.2
008300     02  FS2R1-XN-20  PIC X(20).                                  ST1464.2
008400 SD  ST-FR1.                                                      ST1464.2
008500 01  ST-FR1R1-F-G-140.                                            ST1464.2
008600     02  FILLER PIC X(34).                                        ST1464.2
008700     02  SORT-KEY-FIELD-XN-00006 PIC X(6).                        ST1464.2
008800     02  FILLER PIC X(100).                                       ST1464.2
008900 WORKING-STORAGE SECTION.                                         ST1464.2
009000 01  ODO-RECORD.                                                  ST1464.2
009100     02  FILLER PIC X(5).                                         ST1464.2
009200     02  SO-FILE-NAME PIC X(6).                                   ST1464.2
009300     02  FILLER PIC X(23).                                        ST1464.2
009400     02  SO-RECNO PIC X(6).                                       ST1464.2
009500     02  FILLER PIC X(80).                                        ST1464.2
009600     02  GRP-ODO.                                                 ST1464.2
009700         03  DOI-DU-01V00 PIC 9.                                  ST1464.2
009800         03  ODO-XN-00009 PIC X(9).                               ST1464.2
009900         03  ODO-GRP-00009.                                       ST1464.2
010000         04  ODO-XN-00001-O009D OCCURS 1 TO 9 TIMES DEPENDING ON  ST1464.2
010100                 DOI-DU-01V00 ASCENDING KEY ODO-XN-00001-O009D    ST1464.2
010200                 INDEXED BY ODO-IX PIC X.                         ST1464.2
010300 01  STATIC-VALUE.                                                ST1464.2
010400     02  FILLER PIC 9 VALUE 9.                                    ST1464.2
010500     02  FILLER PIC X(18) VALUE " ACTIVE: 123456789".             ST1464.2
010600 01  WRK-GRP-00019.                                               ST1464.2
010700     02  WRK-DU-01V00 PIC 9.                                      ST1464.2
010800     02  WRK-XN-00009-1 PIC X(9).                                 ST1464.2
010900     02  WRK-XN-00009-2 PIC X(9).                                 ST1464.2
011000 01  WRK-GRP-00009.                                               ST1464.2
011100     02  ODO-XN-00003  PIC X(3).                                  ST1464.2
011200     02  ODO-XN-00006  PIC X(6).                                  ST1464.2
011300 01  WRK-GRP-0009A  REDEFINES  WRK-GRP-00009.                     ST1464.2
011400     02  ODO-XN-00005  PIC X(5).                                  ST1464.2
011500     02  ODO-XN-00004  PIC X(4).                                  ST1464.2
011600 01  WRK-DU-05V00 PIC 9(5).                                       ST1464.2
011700 01  WRK-XN-00020 PIC X(20).                                      ST1464.2
011800 01  WRK-XN-00010 PIC X(10).                                      ST1464.2
011900 01  FILE-RECORD-INFORMATION-REC.                                 ST1464.2
012000     03 FILE-RECORD-INFO-SKELETON.                                ST1464.2
012100        05 FILLER                 PICTURE X(48)       VALUE       ST1464.2
012200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  ST1464.2
012300        05 FILLER                 PICTURE X(46)       VALUE       ST1464.2
012400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    ST1464.2
012500        05 FILLER                 PICTURE X(26)       VALUE       ST1464.2
012600             ",LFIL=000000,ORG=  ,LBLR= ".                        ST1464.2
012700        05 FILLER                 PICTURE X(37)       VALUE       ST1464.2
012800             ",RECKEY=                             ".             ST1464.2
012900        05 FILLER                 PICTURE X(38)       VALUE       ST1464.2
013000             ",ALTKEY1=                             ".            ST1464.2
013100        05 FILLER                 PICTURE X(38)       VALUE       ST1464.2
013200             ",ALTKEY2=                             ".            ST1464.2
013300        05 FILLER                 PICTURE X(7)        VALUE SPACE.ST1464.2
013400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              ST1464.2
013500        05 FILE-RECORD-INFO-P1-120.                               ST1464.2
013600           07 FILLER              PIC X(5).                       ST1464.2
013700           07 XFILE-NAME           PIC X(6).                      ST1464.2
013800           07 FILLER              PIC X(8).                       ST1464.2
013900           07 XRECORD-NAME         PIC X(6).                      ST1464.2
014000           07 FILLER              PIC X(1).                       ST1464.2
014100           07 REELUNIT-NUMBER     PIC 9(1).                       ST1464.2
014200           07 FILLER              PIC X(7).                       ST1464.2
014300           07 XRECORD-NUMBER       PIC 9(6).                      ST1464.2
014400           07 FILLER              PIC X(6).                       ST1464.2
014500           07 UPDATE-NUMBER       PIC 9(2).                       ST1464.2
014600           07 FILLER              PIC X(5).                       ST1464.2
014700           07 ODO-NUMBER          PIC 9(4).                       ST1464.2
014800           07 FILLER              PIC X(5).                       ST1464.2
014900           07 XPROGRAM-NAME        PIC X(5).                      ST1464.2
015000           07 FILLER              PIC X(7).                       ST1464.2
015100           07 XRECORD-LENGTH       PIC 9(6).                      ST1464.2
015200           07 FILLER              PIC X(7).                       ST1464.2
015300           07 CHARS-OR-RECORDS    PIC X(2).                       ST1464.2
015400           07 FILLER              PIC X(1).                       ST1464.2
015500           07 XBLOCK-SIZE          PIC 9(4).                      ST1464.2
015600           07 FILLER              PIC X(6).                       ST1464.2
015700           07 RECORDS-IN-FILE     PIC 9(6).                       ST1464.2
015800           07 FILLER              PIC X(5).                       ST1464.2
015900           07 XFILE-ORGANIZATION   PIC X(2).                      ST1464.2
016000           07 FILLER              PIC X(6).                       ST1464.2
016100           07 XLABEL-TYPE          PIC X(1).                      ST1464.2
016200        05 FILE-RECORD-INFO-P121-240.                             ST1464.2
016300           07 FILLER              PIC X(8).                       ST1464.2
016400           07 XRECORD-KEY          PIC X(29).                     ST1464.2
016500           07 FILLER              PIC X(9).                       ST1464.2
016600           07 ALTERNATE-KEY1      PIC X(29).                      ST1464.2
016700           07 FILLER              PIC X(9).                       ST1464.2
016800           07 ALTERNATE-KEY2      PIC X(29).                      ST1464.2
016900           07 FILLER              PIC X(7).                       ST1464.2
017000 01  TEST-RESULTS.                                                ST1464.2
017100     02 FILLER                   PIC X      VALUE SPACE.          ST1464.2
017200     02 FEATURE                  PIC X(20)  VALUE SPACE.          ST1464.2
017300     02 FILLER                   PIC X      VALUE SPACE.          ST1464.2
017400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          ST1464.2
017500     02 FILLER                   PIC X      VALUE SPACE.          ST1464.2
017600     02  PAR-NAME.                                                ST1464.2
017700       03 FILLER                 PIC X(19)  VALUE SPACE.          ST1464.2
017800       03  PARDOT-X              PIC X      VALUE SPACE.          ST1464.2
017900       03 DOTVALUE               PIC 99     VALUE ZERO.           ST1464.2
018000     02 FILLER                   PIC X(8)   VALUE SPACE.          ST1464.2
018100     02 RE-MARK                  PIC X(61).                       ST1464.2
018200 01  TEST-COMPUTED.                                               ST1464.2
018300     02 FILLER                   PIC X(30)  VALUE SPACE.          ST1464.2
018400     02 FILLER                   PIC X(17)  VALUE                 ST1464.2
018500            "       COMPUTED=".                                   ST1464.2
018600     02 COMPUTED-X.                                               ST1464.2
018700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          ST1464.2
018800     03 COMPUTED-N               REDEFINES COMPUTED-A             ST1464.2
018900                                 PIC -9(9).9(9).                  ST1464.2
019000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         ST1464.2
019100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     ST1464.2
019200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     ST1464.2
019300     03       CM-18V0 REDEFINES COMPUTED-A.                       ST1464.2
019400         04 COMPUTED-18V0                    PIC -9(18).          ST1464.2
019500         04 FILLER                           PIC X.               ST1464.2
019600     03 FILLER PIC X(50) VALUE SPACE.                             ST1464.2
019700 01  TEST-CORRECT.                                                ST1464.2
019800     02 FILLER PIC X(30) VALUE SPACE.                             ST1464.2
019900     02 FILLER PIC X(17) VALUE "       CORRECT =".                ST1464.2
020000     02 CORRECT-X.                                                ST1464.2
020100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         ST1464.2
020200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      ST1464.2
020300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         ST1464.2
020400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     ST1464.2
020500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     ST1464.2
020600     03      CR-18V0 REDEFINES CORRECT-A.                         ST1464.2
020700         04 CORRECT-18V0                     PIC -9(18).          ST1464.2
020800         04 FILLER                           PIC X.               ST1464.2
020900     03 FILLER PIC X(2) VALUE SPACE.                              ST1464.2
021000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     ST1464.2
021100 01  CCVS-C-1.                                                    ST1464.2
021200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAST1464.2
021300-    "SS  PARAGRAPH-NAME                                          ST1464.2
021400-    "       REMARKS".                                            ST1464.2
021500     02 FILLER                     PIC X(20)    VALUE SPACE.      ST1464.2
021600 01  CCVS-C-2.                                                    ST1464.2
021700     02 FILLER                     PIC X        VALUE SPACE.      ST1464.2
021800     02 FILLER                     PIC X(6)     VALUE "TESTED".   ST1464.2
021900     02 FILLER                     PIC X(15)    VALUE SPACE.      ST1464.2
022000     02 FILLER                     PIC X(4)     VALUE "FAIL".     ST1464.2
022100     02 FILLER                     PIC X(94)    VALUE SPACE.      ST1464.2
022200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       ST1464.2
022300 01  REC-CT                        PIC 99       VALUE ZERO.       ST1464.2
022400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       ST1464.2
022500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       ST1464.2
022600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       ST1464.2
022700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       ST1464.2
022800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       ST1464.2
022900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       ST1464.2
023000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      ST1464.2
023100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       ST1464.2
023200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     ST1464.2
023300 01  CCVS-H-1.                                                    ST1464.2
023400     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1464.2
023500     02  FILLER                    PIC X(42)    VALUE             ST1464.2
023600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 ST1464.2
023700     02  FILLER                    PIC X(39)    VALUE SPACES.     ST1464.2
023800 01  CCVS-H-2A.                                                   ST1464.2
023900   02  FILLER                        PIC X(40)  VALUE SPACE.      ST1464.2
024000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  ST1464.2
024100   02  FILLER                        PIC XXXX   VALUE             ST1464.2
024200     "4.2 ".                                                      ST1464.2
024300   02  FILLER                        PIC X(28)  VALUE             ST1464.2
024400            " COPY - NOT FOR DISTRIBUTION".                       ST1464.2
024500   02  FILLER                        PIC X(41)  VALUE SPACE.      ST1464.2
024600                                                                  ST1464.2
024700 01  CCVS-H-2B.                                                   ST1464.2
024800   02  FILLER                        PIC X(15)  VALUE             ST1464.2
024900            "TEST RESULT OF ".                                    ST1464.2
025000   02  TEST-ID                       PIC X(9).                    ST1464.2
025100   02  FILLER                        PIC X(4)   VALUE             ST1464.2
025200            " IN ".                                               ST1464.2
025300   02  FILLER                        PIC X(12)  VALUE             ST1464.2
025400     " HIGH       ".                                              ST1464.2
025500   02  FILLER                        PIC X(22)  VALUE             ST1464.2
025600            " LEVEL VALIDATION FOR ".                             ST1464.2
025700   02  FILLER                        PIC X(58)  VALUE             ST1464.2
025800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1464.2
025900 01  CCVS-H-3.                                                    ST1464.2
026000     02  FILLER                      PIC X(34)  VALUE             ST1464.2
026100            " FOR OFFICIAL USE ONLY    ".                         ST1464.2
026200     02  FILLER                      PIC X(58)  VALUE             ST1464.2
026300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".ST1464.2
026400     02  FILLER                      PIC X(28)  VALUE             ST1464.2
026500            "  COPYRIGHT   1985 ".                                ST1464.2
026600 01  CCVS-E-1.                                                    ST1464.2
026700     02 FILLER                       PIC X(52)  VALUE SPACE.      ST1464.2
026800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              ST1464.2
026900     02 ID-AGAIN                     PIC X(9).                    ST1464.2
027000     02 FILLER                       PIC X(45)  VALUE SPACES.     ST1464.2
027100 01  CCVS-E-2.                                                    ST1464.2
027200     02  FILLER                      PIC X(31)  VALUE SPACE.      ST1464.2
027300     02  FILLER                      PIC X(21)  VALUE SPACE.      ST1464.2
027400     02 CCVS-E-2-2.                                               ST1464.2
027500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      ST1464.2
027600         03 FILLER                   PIC X      VALUE SPACE.      ST1464.2
027700         03 ENDER-DESC               PIC X(44)  VALUE             ST1464.2
027800            "ERRORS ENCOUNTERED".                                 ST1464.2
027900 01  CCVS-E-3.                                                    ST1464.2
028000     02  FILLER                      PIC X(22)  VALUE             ST1464.2
028100            " FOR OFFICIAL USE ONLY".                             ST1464.2
028200     02  FILLER                      PIC X(12)  VALUE SPACE.      ST1464.2
028300     02  FILLER                      PIC X(58)  VALUE             ST1464.2
028400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".ST1464.2
028500     02  FILLER                      PIC X(13)  VALUE SPACE.      ST1464.2
028600     02 FILLER                       PIC X(15)  VALUE             ST1464.2
028700             " COPYRIGHT 1985".                                   ST1464.2
028800 01  CCVS-E-4.                                                    ST1464.2
028900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      ST1464.2
029000     02 FILLER                       PIC X(4)   VALUE " OF ".     ST1464.2
029100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      ST1464.2
029200     02 FILLER                       PIC X(40)  VALUE             ST1464.2
029300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       ST1464.2
029400 01  XXINFO.                                                      ST1464.2
029500     02 FILLER                       PIC X(19)  VALUE             ST1464.2
029600            "*** INFORMATION ***".                                ST1464.2
029700     02 INFO-TEXT.                                                ST1464.2
029800       04 FILLER                     PIC X(8)   VALUE SPACE.      ST1464.2
029900       04 XXCOMPUTED                 PIC X(20).                   ST1464.2
030000       04 FILLER                     PIC X(5)   VALUE SPACE.      ST1464.2
030100       04 XXCORRECT                  PIC X(20).                   ST1464.2
030200     02 INF-ANSI-REFERENCE           PIC X(48).                   ST1464.2
030300 01  HYPHEN-LINE.                                                 ST1464.2
030400     02 FILLER  PIC IS X VALUE IS SPACE.                          ST1464.2
030500     02 FILLER  PIC IS X(65)    VALUE IS "************************ST1464.2
030600-    "*****************************************".                 ST1464.2
030700     02 FILLER  PIC IS X(54)    VALUE IS "************************ST1464.2
030800-    "******************************".                            ST1464.2
030900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             ST1464.2
031000     "ST146A".                                                    ST1464.2
031100 PROCEDURE DIVISION.                                              ST1464.2
031200 CCVS1 SECTION.                                                   ST1464.2
031300 OPEN-FILES.                                                      ST1464.2
031400     OPEN    OUTPUT PRINT-FILE.                                   ST1464.2
031500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  ST1464.2
031600     MOVE    SPACE TO TEST-RESULTS.                               ST1464.2
031700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              ST1464.2
031800     MOVE    ZERO TO REC-SKL-SUB.                                 ST1464.2
031900     PERFORM CCVS-INIT-FILE 9 TIMES.                              ST1464.2
032000 CCVS-INIT-FILE.                                                  ST1464.2
032100     ADD     1 TO REC-SKL-SUB.                                    ST1464.2
032200     MOVE    FILE-RECORD-INFO-SKELETON                            ST1464.2
032300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      ST1464.2
032400 CCVS-INIT-EXIT.                                                  ST1464.2
032500     GO TO CCVS1-EXIT.                                            ST1464.2
032600 CLOSE-FILES.                                                     ST1464.2
032700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   ST1464.2
032800 TERMINATE-CCVS.                                                  ST1464.2
032900*    EXIT PROGRAM.                                                ST1464.2
033000*TERMINATE-CALL.                                                  ST1464.2
033100     STOP     RUN.                                                ST1464.2
033200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         ST1464.2
033300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           ST1464.2
033400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          ST1464.2
033500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      ST1464.2
033600     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1464.2
033700 PRINT-DETAIL.                                                    ST1464.2
033800     IF REC-CT NOT EQUAL TO ZERO                                  ST1464.2
033900             MOVE "." TO PARDOT-X                                 ST1464.2
034000             MOVE REC-CT TO DOTVALUE.                             ST1464.2
034100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      ST1464.2
034200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               ST1464.2
034300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 ST1464.2
034400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 ST1464.2
034500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1464.2
034600     MOVE SPACE TO CORRECT-X.                                     ST1464.2
034700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1464.2
034800     MOVE     SPACE TO RE-MARK.                                   ST1464.2
034900 HEAD-ROUTINE.                                                    ST1464.2
035000     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1464.2
035100     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  ST1464.2
035200     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1464.2
035300     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  ST1464.2
035400 COLUMN-NAMES-ROUTINE.                                            ST1464.2
035500     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1464.2
035600     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1464.2
035700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        ST1464.2
035800 END-ROUTINE.                                                     ST1464.2
035900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.ST1464.2
036000 END-RTN-EXIT.                                                    ST1464.2
036100     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1464.2
036200 END-ROUTINE-1.                                                   ST1464.2
036300      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      ST1464.2
036400      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               ST1464.2
036500      ADD PASS-COUNTER TO ERROR-HOLD.                             ST1464.2
036600*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   ST1464.2
036700      MOVE PASS-COUNTER TO CCVS-E-4-1.                            ST1464.2
036800      MOVE ERROR-HOLD TO CCVS-E-4-2.                              ST1464.2
036900      MOVE CCVS-E-4 TO CCVS-E-2-2.                                ST1464.2
037000      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           ST1464.2
037100  END-ROUTINE-12.                                                 ST1464.2
037200      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        ST1464.2
037300     IF       ERROR-COUNTER IS EQUAL TO ZERO                      ST1464.2
037400         MOVE "NO " TO ERROR-TOTAL                                ST1464.2
037500         ELSE                                                     ST1464.2
037600         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       ST1464.2
037700     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           ST1464.2
037800     PERFORM WRITE-LINE.                                          ST1464.2
037900 END-ROUTINE-13.                                                  ST1464.2
038000     IF DELETE-COUNTER IS EQUAL TO ZERO                           ST1464.2
038100         MOVE "NO " TO ERROR-TOTAL  ELSE                          ST1464.2
038200         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      ST1464.2
038300     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   ST1464.2
038400     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1464.2
038500      IF   INSPECT-COUNTER EQUAL TO ZERO                          ST1464.2
038600          MOVE "NO " TO ERROR-TOTAL                               ST1464.2
038700      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   ST1464.2
038800      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            ST1464.2
038900      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          ST1464.2
039000     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           ST1464.2
039100 WRITE-LINE.                                                      ST1464.2
039200     ADD 1 TO RECORD-COUNT.                                       ST1464.2
039300*    IF RECORD-COUNT GREATER 42                                   ST1464.2
039400*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1464.2
039500*        MOVE SPACE TO DUMMY-RECORD                               ST1464.2
039600*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1464.2
039700*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1464.2
039800*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    ST1464.2
039900*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1464.2
040000*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    ST1464.2
040100*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            ST1464.2
040200*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            ST1464.2
040300*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          ST1464.2
040400*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1464.2
040500*        MOVE ZERO TO RECORD-COUNT.                               ST1464.2
040600     PERFORM WRT-LN.                                              ST1464.2
040700 WRT-LN.                                                          ST1464.2
040800     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1464.2
040900     MOVE SPACE TO DUMMY-RECORD.                                  ST1464.2
041000 BLANK-LINE-PRINT.                                                ST1464.2
041100     PERFORM WRT-LN.                                              ST1464.2
041200 FAIL-ROUTINE.                                                    ST1464.2
041300     IF     COMPUTED-X NOT EQUAL TO SPACE                         ST1464.2
041400            GO TO   FAIL-ROUTINE-WRITE.                           ST1464.2
041500     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.ST1464.2
041600     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1464.2
041700     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   ST1464.2
041800     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1464.2
041900     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1464.2
042000     GO TO  FAIL-ROUTINE-EX.                                      ST1464.2
042100 FAIL-ROUTINE-WRITE.                                              ST1464.2
042200     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         ST1464.2
042300     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 ST1464.2
042400     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. ST1464.2
042500     MOVE   SPACES TO COR-ANSI-REFERENCE.                         ST1464.2
042600 FAIL-ROUTINE-EX. EXIT.                                           ST1464.2
042700 BAIL-OUT.                                                        ST1464.2
042800     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   ST1464.2
042900     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           ST1464.2
043000 BAIL-OUT-WRITE.                                                  ST1464.2
043100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1464.2
043200     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 ST1464.2
043300     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   ST1464.2
043400     MOVE   SPACES TO INF-ANSI-REFERENCE.                         ST1464.2
043500 BAIL-OUT-EX. EXIT.                                               ST1464.2
043600 CCVS1-EXIT.                                                      ST1464.2
043700     EXIT.                                                        ST1464.2
043800 BEGIN-ST216-TESTS SECTION.                                       ST1464.2
043900 INIT-WRK-AREA.                                                   ST1464.2
044000     MOVE STATIC-VALUE TO WRK-GRP-00019.                          ST1464.2
044100     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
044200     MOVE " ACTIVE: " TO ODO-XN-00009.                            ST1464.2
044300     MOVE "1" TO ODO-XN-00001-O009D (1).                          ST1464.2
044400     MOVE "2" TO ODO-XN-00001-O009D (2).                          ST1464.2
044500     MOVE "3" TO ODO-XN-00001-O009D (3).                          ST1464.2
044600     MOVE "4" TO ODO-XN-00001-O009D (4).                          ST1464.2
044700     MOVE "5" TO ODO-XN-00001-O009D (5).                          ST1464.2
044800     MOVE "6" TO ODO-XN-00001-O009D (6).                          ST1464.2
044900     MOVE "7" TO ODO-XN-00001-O009D (7).                          ST1464.2
045000     MOVE "8" TO ODO-XN-00001-O009D (8).                          ST1464.2
045100     MOVE "9" TO ODO-XN-00001-O009D (9).                          ST1464.2
045200 BUILD-SQ-FS1 SECTION.                                            ST1464.2
045300 BUILD-SQ-FS1-PARA1.                                              ST1464.2
045400     OPEN OUTPUT SQ-FS1.                                          ST1464.2
045500     MOVE "SQ-FS1" TO XFILE-NAME (1).                             ST1464.2
045600     MOVE "FS1R1 " TO XRECORD-NAME (1).                           ST1464.2
045700     MOVE "ST216" TO XPROGRAM-NAME (1).                           ST1464.2
045800     MOVE 140 TO XRECORD-LENGTH (1).                              ST1464.2
045900     MOVE "1R" TO CHARS-OR-RECORDS (1).                           ST1464.2
046000     MOVE 4000 TO RECORDS-IN-FILE (1).                            ST1464.2
046100     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         ST1464.2
046200     MOVE "S" TO XLABEL-TYPE (1).                                 ST1464.2
046300     PERFORM BUILD-SQ-FS1-PARA2 VARYING ODO-IX FROM 1 BY 1        ST1464.2
046400         UNTIL ODO-IX IS GREATER THAN 1000.                       ST1464.2
046500     GO TO BUILD-SQ-FS1-PARA3.                                    ST1464.2
046600 BUILD-SQ-FS1-PARA2.                                              ST1464.2
046700     SET XRECORD-NUMBER (1) TO ODO-IX.                            ST1464.2
046800     MOVE 9 TO ODO-NUMBER (1).                                    ST1464.2
046900     MOVE FILE-RECORD-INFO-P1-120 (1) TO ODO-RECORD.              ST1464.2
047000     PERFORM INIT-WRK-AREA.                                       ST1464.2
047100     WRITE SQ-FS1R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
047200 BUILD-SQ-FS1-PARA3.                                              ST1464.2
047300     CLOSE SQ-FS1.                                                ST1464.2
047400 END-OF-BUILD-SQ-FS1 SECTION.                                     ST1464.2
047500 EXECUTE-THE-SORT.                                                ST1464.2
047600     SORT ST-FR1 ON ASCENDING KEY SORT-KEY-FIELD-XN-00006         ST1464.2
047700         INPUT PROCEDURE IS SORT-INPUT-PROCEDURES                 ST1464.2
047800         OUTPUT PROCEDURE IS SORT-OUTPUT-PROCEDURES.              ST1464.2
047900*FILE-DUMP SECTION.                                               ST1464.2
048000*F-D-1.                                                           ST1464.2
048100*    PERFORM END-ROUTINE.                                         ST1464.2
048200*    MOVE " DUMP OF FIRST 10 (OF 1000) RECORDS FROM SQ-FS1:"      ST1464.2
048300*        TO PRINT-REC.                                            ST1464.2
048400*    PERFORM WRITE-LINE.                                          ST1464.2
048500*    PERFORM F-D-2 10 TIMES.                                      ST1464.2
048600*    GO TO F-D-3.                                                 ST1464.2
048700*F-D-2.                                                           ST1464.2
048800*    READ SQ-FS1 AT END GO TO F-D-3.                              ST1464.2
048900*    MOVE FS1R1-XN-120 TO PRINT-REC.                              ST1464.2
049000*    PERFORM WRITE-LINE.                                          ST1464.2
049100*    MOVE FS1R1-XN-20 TO PRINT-REC.                               ST1464.2
049200*    PERFORM WRITE-LINE.                                          ST1464.2
049300*F-D-3.                                                           ST1464.2
049400*    CLOSE SQ-FS1.                                                ST1464.2
049500*    OPEN INPUT SQ-FS2.                                           ST1464.2
049600*    PERFORM END-ROUTINE.                                         ST1464.2
049700*    MOVE " DUMP OF FIRST 10 (OF 1000) RECORDS FROM SQ-FS2:"      ST1464.2
049800*        TO PRINT-REC.                                            ST1464.2
049900*    PERFORM WRITE-LINE.                                          ST1464.2
050000*    PERFORM F-D-4 10 TIMES.                                      ST1464.2
050100*    GO TO F-D-5.                                                 ST1464.2
050200*F-D-4.                                                           ST1464.2
050300*    READ SQ-FS2 AT END GO TO F-D-5.                              ST1464.2
050400*    MOVE FS2R1-XN-120 TO PRINT-REC.                              ST1464.2
050500*    PERFORM WRITE-LINE.                                          ST1464.2
050600*    MOVE FS2R1-XN-20 TO PRINT-REC.                               ST1464.2
050700*    PERFORM WRITE-LINE.                                          ST1464.2
050800*F-D-5.                                                           ST1464.2
050900*    CLOSE SQ-FS2.                                                ST1464.2
051000 CCVS-EXIT SECTION.                                               ST1464.2
051100 CCVS-999999.                                                     ST1464.2
051200     GO TO CLOSE-FILES.                                           ST1464.2
051300 SORT-INPUT-PROCEDURES SECTION.                                   ST1464.2
051400 S-I-P-1.                                                         ST1464.2
051500     OPEN INPUT SQ-FS1.                                           ST1464.2
051600     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
051700     READ SQ-FS1 INTO ODO-RECORD AT END GO TO S-I-P-3.            ST1464.2
051800     MOVE 3 TO ODO-NUMBER (1).                                    ST1464.2
051900     MOVE 3 TO DOI-DU-01V00.                                      ST1464.2
052000     RELEASE ST-FR1R1-F-G-140 FROM ODO-RECORD.                    ST1464.2
052100     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
052200     READ SQ-FS1 INTO ODO-RECORD AT END GO TO S-I-P-3.            ST1464.2
052300     MOVE 7 TO ODO-NUMBER (1).                                    ST1464.2
052400     MOVE 7 TO DOI-DU-01V00.                                      ST1464.2
052500     RELEASE ST-FR1R1-F-G-140 FROM ODO-RECORD.                    ST1464.2
052600 S-I-P-2.                                                         ST1464.2
052700     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
052800     READ SQ-FS1 INTO ODO-RECORD AT END GO TO S-I-P-3.            ST1464.2
052900     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
053000     RELEASE ST-FR1R1-F-G-140 FROM ODO-RECORD.                    ST1464.2
053100     GO TO S-I-P-2.                                               ST1464.2
053200 S-I-P-3.                                                         ST1464.2
053300     CLOSE SQ-FS1.                                                ST1464.2
053400 SORT-OUTPUT-PROCEDURES SECTION.                                  ST1464.2
053500 S-O-P-1.                                                         ST1464.2
053600     OPEN OUTPUT SQ-FS2.                                          ST1464.2
053700     MOVE "OCCURS DEPENDING ON" TO FEATURE.                       ST1464.2
053800 CLEAR-ODO-RECORD.                                                ST1464.2
053900     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
054000     MOVE SPACES TO ODO-RECORD.                                   ST1464.2
054100     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
054200 RELEASE-TEST-1.                                                  ST1464.2
054300     RETURN ST-FR1 INTO ODO-RECORD AT END GO TO RELEASE-DELETE-1. ST1464.2
054400     IF SO-RECNO NOT EQUAL TO "000001" GO TO RELEASE-DELETE-1.    ST1464.2
054500     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
054600     MOVE ODO-GRP-00009 TO WRK-GRP-00009.                         ST1464.2
054700     IF ODO-XN-00003 IS EQUAL TO "123" AND                        ST1464.2
054800        ODO-XN-00006 IS NOT EQUAL TO "456789"                     ST1464.2
054900         PERFORM PASS-1                                           ST1464.2
055000         ELSE                                                     ST1464.2
055100         PERFORM FAIL-1                                           ST1464.2
055200         MOVE "3 ACTIVE: 123" TO CORRECT-A                        ST1464.2
055300         MOVE 9 TO DOI-DU-01V00                                   ST1464.2
055400         MOVE GRP-ODO TO COMPUTED-A.                              ST1464.2
055500     MOVE "SQ-FS2" TO SO-FILE-NAME.                               ST1464.2
055600     WRITE SQ-FS2R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
055700     GO TO RELEASE-WRITE-1.                                       ST1464.2
055800 RELEASE-DELETE-1.                                                ST1464.2
055900     PERFORM DE-LETE-1.                                           ST1464.2
056000 RELEASE-WRITE-1.                                                 ST1464.2
056100     MOVE "RELEASE-TEST-1" TO PAR-NAME.                           ST1464.2
056200     MOVE "RELEASE 3 ODO - RETURN 9 ODO" TO RE-MARK.              ST1464.2
056300     PERFORM PRINT-DETAIL-1.                                      ST1464.2
056400 RETURN-TEST-1.                                                   ST1464.2
056500     PERFORM CLEAR-ODO-RECORD.                                    ST1464.2
056600     MOVE 5 TO DOI-DU-01V00.                                      ST1464.2
056700     RETURN ST-FR1 INTO ODO-RECORD AT END GO TO RETURN-DELETE-1.  ST1464.2
056800     IF SO-RECNO NOT EQUAL TO "000002" GO TO RETURN-DELETE-1.     ST1464.2
056900     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
057000     MOVE ODO-GRP-00009 TO WRK-GRP-00009.                         ST1464.2
057100     IF ODO-XN-00005 IS EQUAL TO "12345" AND                      ST1464.2
057200        ODO-XN-00004 IS NOT EQUAL TO "6789"                       ST1464.2
057300         PERFORM PASS-1                                           ST1464.2
057400         ELSE                                                     ST1464.2
057500         PERFORM FAIL-1                                           ST1464.2
057600         MOVE "7 ACTIVE: 12345" TO CORRECT-A                      ST1464.2
057700         MOVE 7 TO DOI-DU-01V00                                   ST1464.2
057800         MOVE GRP-ODO TO COMPUTED-A.                              ST1464.2
057900     MOVE "SQ-FS2" TO SO-FILE-NAME.                               ST1464.2
058000     WRITE SQ-FS2R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
058100     GO TO RETURN-WRITE-1.                                        ST1464.2
058200 RETURN-DELETE-1.                                                 ST1464.2
058300     PERFORM DE-LETE-1.                                           ST1464.2
058400 RETURN-WRITE-1.                                                  ST1464.2
058500     MOVE "RETURN-TEST-1" TO PAR-NAME.                            ST1464.2
058600     MOVE "RELEASE 7 ODO - RETURN 5 ODO" TO RE-MARK.              ST1464.2
058700     PERFORM PRINT-DETAIL-1.                                      ST1464.2
058800 RELEASE-TEST-2.                                                  ST1464.2
058900     PERFORM CLEAR-ODO-RECORD.                                    ST1464.2
059000     RETURN ST-FR1 INTO ODO-RECORD AT END GO TO RELEASE-DELETE-2. ST1464.2
059100     IF SO-RECNO NOT EQUAL TO "000003" GO TO RELEASE-DELETE-2.    ST1464.2
059200     IF GRP-ODO IS EQUAL TO "9 ACTIVE: 123456789"                 ST1464.2
059300         PERFORM PASS-1                                           ST1464.2
059400         ELSE                                                     ST1464.2
059500         PERFORM FAIL-1                                           ST1464.2
059600         MOVE "9 ACTIVE: 123456789" TO CORRECT-A                  ST1464.2
059700         MOVE GRP-ODO TO COMPUTED-A.                              ST1464.2
059800     MOVE "SQ-FS2" TO SO-FILE-NAME.                               ST1464.2
059900     WRITE SQ-FS2R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
060000     GO TO RELEASE-WRITE-2.                                       ST1464.2
060100 RELEASE-DELETE-2.                                                ST1464.2
060200     PERFORM DE-LETE-1.                                           ST1464.2
060300 RELEASE-WRITE-2.                                                 ST1464.2
060400     MOVE "RELEASE-TEST-2" TO PAR-NAME.                           ST1464.2
060500     MOVE "RELEASE 9 ODO - RETURN 9 ODO" TO RE-MARK.              ST1464.2
060600     PERFORM PRINT-DETAIL-1.                                      ST1464.2
060700 RETURN-TEST-2.                                                   ST1464.2
060800     PERFORM CLEAR-ODO-RECORD.                                    ST1464.2
060900     MOVE 3 TO DOI-DU-01V00.                                      ST1464.2
061000     RETURN ST-FR1 INTO ODO-RECORD AT END GO TO RETURN-DELETE-2.  ST1464.2
061100     IF SO-RECNO NOT EQUAL TO "000004" GO TO RETURN-DELETE-2.     ST1464.2
061200     MOVE 9 TO DOI-DU-01V00.                                      ST1464.2
061300     MOVE ODO-GRP-00009 TO WRK-GRP-00009.                         ST1464.2
061400     IF ODO-XN-00003 IS EQUAL TO "123" AND                        ST1464.2
061500        ODO-XN-00006 IS EQUAL TO "456789"                         ST1464.2
061600         PERFORM PASS-1                                           ST1464.2
061700         ELSE                                                     ST1464.2
061800         PERFORM FAIL-1                                           ST1464.2
061900         MOVE "9 ACTIVE: 123456789" TO CORRECT-A                  ST1464.2
062000         MOVE GRP-ODO TO COMPUTED-A.                              ST1464.2
062100     MOVE "SQ-FS2" TO SO-FILE-NAME.                               ST1464.2
062200     WRITE SQ-FS2R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
062300     GO TO RETURN-WRITE-2.                                        ST1464.2
062400 RETURN-DELETE-2.                                                 ST1464.2
062500     PERFORM DE-LETE-1.                                           ST1464.2
062600 RETURN-WRITE-2.                                                  ST1464.2
062700     MOVE "RETURN-TEST-2" TO PAR-NAME.                            ST1464.2
062800     MOVE "RELEASE 9 ODO - RETURN 6 ODO" TO RE-MARK.              ST1464.2
062900     PERFORM PRINT-DETAIL-1.                                      ST1464.2
063000 S-O-P-2.                                                         ST1464.2
063100     PERFORM CLEAR-ODO-RECORD.                                    ST1464.2
063200     RETURN ST-FR1 INTO ODO-RECORD AT END GO TO S-O-P-3.          ST1464.2
063300     MOVE "SQ-FS2" TO SO-FILE-NAME.                               ST1464.2
063400     WRITE SQ-FS2R1-F-G-140 FROM ODO-RECORD.                      ST1464.2
063500     GO TO S-O-P-2.                                               ST1464.2
063600 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       ST1464.2
063700 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         ST1464.2
063800 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        ST1464.2
063900 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.    ST1464.2
064000     MOVE "****TEST DELETED****" TO RE-MARK.                      ST1464.2
064100 PRINT-DETAIL-1.                                                  ST1464.2
064200     IF REC-CT NOT EQUAL TO ZERO                                  ST1464.2
064300             MOVE "." TO PARDOT-X                                 ST1464.2
064400             MOVE REC-CT TO DOTVALUE.                             ST1464.2
064500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    ST1464.2
064600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             ST1464.2
064700        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             ST1464.2
064800          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             ST1464.2
064900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              ST1464.2
065000     MOVE SPACE TO CORRECT-X.                                     ST1464.2
065100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         ST1464.2
065200     MOVE     SPACE TO RE-MARK.                                   ST1464.2
065300 WRITE-LINE-1.                                                    ST1464.2
065400     ADD 1 TO RECORD-COUNT.                                       ST1464.2
065500*    IF RECORD-COUNT GREATER 50                                   ST1464.2
065600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          ST1464.2
065700*        MOVE SPACE TO DUMMY-RECORD                               ST1464.2
065800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  ST1464.2
065900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           ST1464.2
066000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   ST1464.2
066100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        ST1464.2
066200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          ST1464.2
066300*        MOVE ZERO TO RECORD-COUNT.                               ST1464.2
066400     PERFORM WRT-LN-1.                                            ST1464.2
066500 WRT-LN-1.                                                        ST1464.2
066600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               ST1464.2
066700     MOVE SPACE TO DUMMY-RECORD.                                  ST1464.2
066800 BLANK-LINE-PRINT-1.                                              ST1464.2
066900     PERFORM WRT-LN-1.                                            ST1464.2
067000 FAIL-ROUTINE-1.                                                  ST1464.2
067100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     ST1464.2
067200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      ST1464.2
067300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    ST1464.2
067400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1464.2
067500     GO TO FAIL-ROUTINE-EX-1.                                     ST1464.2
067600 FAIL-RTN-WRITE-1.                                                ST1464.2
067700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         ST1464.2
067800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. ST1464.2
067900 FAIL-ROUTINE-EX-1. EXIT.                                         ST1464.2
068000 BAIL-OUT-1.                                                      ST1464.2
068100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     ST1464.2
068200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             ST1464.2
068300 BAIL-OUT-WRITE-1.                                                ST1464.2
068400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  ST1464.2
068500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   ST1464.2
068600 BAIL-OUT-EX-1. EXIT.                                             ST1464.2
068700 S-O-P-3.                                                         ST1464.2
068800     CLOSE SQ-FS2.                                                ST1464.2
      *END-OF,ST146A                                                          TES07470
