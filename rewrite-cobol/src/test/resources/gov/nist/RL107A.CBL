      *HEADER,COBOL,RL107A                                                            
000100 IDENTIFICATION DIVISION.                                         RL1074.2
000200 PROGRAM-ID.                                                      RL1074.2
000300     RL107A.                                                      RL1074.2
000400****************************************************************  RL1074.2
000500*                                                              *  RL1074.2
000600*    VALIDATION FOR:-                                          *  RL1074.2
000700*                                                              *  RL1074.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1074.2
000900*                                                              *  RL1074.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1074.2
001100*                                                              *  RL1074.2
001200****************************************************************  RL1074.2
001300*                                                              *  RL1074.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  RL1074.2
001500*                                                              *  RL1074.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  RL1074.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  RL1074.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  RL1074.2
001900*                                                              *  RL1074.2
002000****************************************************************  RL1074.2
002100*GENERAL:    THIS PROGRAM PROCESSES TWO RELATIVE I-O FILES.  THE  RL1074.2
002200*            THE FUNCTION OF THIS PROGRAM IS TO CREATE TWO        RL1074.2
002300*            RELATIVES FILES RANDOMLY (ACCESS MODE RANDOM) AND    RL1074.2
002400*            VERIFY THAT THEY WERE CREATED CORRECTLY.  THE FILES  RL1074.2
002500*            ARE IDENTIFIED AS "RL-FR7" AND "RL-FR8".  THE FILES  RL1074.2
002600*            ARE CREATED PARTIALLY (NOT ALL VALUES FOR RELATIVE   RL1074.2
002700*            KEY ARE USED) IN THE OUTPUT MODE AND SUBSEQUENTLY    RL1074.2
002800*            COMPLETED IN THE I-O MODE.  THE END RESULT IS THAT   RL1074.2
002900*            THERE ARE NO NULL RECORDS IN ANY OF THE FILES.       RL1074.2
003000*                                                                 RL1074.2
003100*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    RL1074.2
003200*            PROGRAM ARE:                                         RL1074.2
003300*                                                                 RL1074.2
003400*                 X-21   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1074.2
003500*                         RELATIVE  I-O DATA FILE-1               RL1074.2
003600*                 X-22   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR RL1074.2
003700*                         RELATIVE  I-O DATA FILE-2               RL1074.2
003800*                 X-55   SYSTEM PRINTER                           RL1074.2
003900*                 X-69   ADDITIONAL VALUE OF CLAUSES              RL1074.2
004000*                 X-74   VALUE OF IMPLEMENTOR-NAME                RL1074.2
004100*                 X-75   OBJECT OF VALUE OF CLAUSE FOR FILE-1     RL1074.2
004200*                 X-76   OBJECT OF VALUE OF CLAUSE FOR FILE-2     RL1074.2
004300*                 X-82   SOURCE-COMPUTER                          RL1074.2
004400*                 X-83   OBJECT-COMPUTER.                         RL1074.2
004500*                                                                 RL1074.2
004600****************************************************************  RL1074.2
004700 ENVIRONMENT DIVISION.                                            RL1074.2
004800 CONFIGURATION SECTION.                                           RL1074.2
004900 SOURCE-COMPUTER.                                                 RL1074.2
005000     XXXXX082.                                                    RL1074.2
005100 OBJECT-COMPUTER.                                                 RL1074.2
005200     XXXXX083.                                                    RL1074.2
005300 INPUT-OUTPUT SECTION.                                            RL1074.2
005400 FILE-CONTROL.                                                    RL1074.2
005500     SELECT PRINT-FILE ASSIGN TO                                  RL1074.2
005600     XXXXX055.                                                    RL1074.2
005700     SELECT RL-FR7 ASSIGN TO                                      RL1074.2
005800     XXXXX021                                                     RL1074.2
005900             ORGANIZATION IS RELATIVE                             RL1074.2
006000             ACCESS  MODE IS RANDOM                               RL1074.2
006100              RELATIVE KEY ACTUAL-KEY-1.                          RL1074.2
006200     SELECT RL-FR8 ASSIGN TO                                      RL1074.2
006300     XXXXX022                                                     RL1074.2
006400             ORGANIZATION IS RELATIVE                             RL1074.2
006500             ACCESS  MODE IS RANDOM                               RL1074.2
006600              RELATIVE KEY IS ACTUAL-KEY-2.                       RL1074.2
006700 DATA DIVISION.                                                   RL1074.2
006800 FILE SECTION.                                                    RL1074.2
006900 FD  PRINT-FILE.                                                  RL1074.2
007000 01  PRINT-REC PICTURE X(120).                                    RL1074.2
007100 01  DUMMY-RECORD PICTURE X(120).                                 RL1074.2
007200 FD  RL-FR7                                                       RL1074.2
007300     LABEL RECORDS ARE STANDARD                                   RL1074.2
007400C    VALUE OF                                                     RL1074.2
007500C    XXXXX074                                                     RL1074.2
007600C    IS                                                           RL1074.2
007700C    XXXXX075                                                     RL1074.2
007800*    XXXXX069                                                     RL1074.2
007900     DATA RECORD IS RAC-REC-1.                                    RL1074.2
008000 01  RAC-REC-1.                                                   RL1074.2
008100     03  FILLER                  PICTURE IS X(24).                RL1074.2
008200     03  RECORD-NO-1             PICTURE IS 9999.                 RL1074.2
008300     03  FILLER                  PICTURE IS XXXX.                 RL1074.2
008400     03  UPDATE-FIELD            PICTURE IS X(7).                 RL1074.2
008500     03  FILLER                  PICTURE IS X(81).                RL1074.2
008600 FD  RL-FR8                                                       RL1074.2
008700     LABEL RECORDS ARE STANDARD                                   RL1074.2
008800C    VALUE OF                                                     RL1074.2
008900C    XXXXX074                                                     RL1074.2
009000C    IS                                                           RL1074.2
009100C    XXXXX076                                                     RL1074.2
009200*    XXXXX069                                                     RL1074.2
009300     DATA RECORDS ARE RAC-REC-2 RAC-REC-3.                        RL1074.2
009400 01  RAC-REC-2.                                                   RL1074.2
009500     03  FILLER                  PICTURE IS X(24).                RL1074.2
009600     03  RECORD-NO-2             PICTURE IS 9999.                 RL1074.2
009700     03  FILLER                  PICTURE IS X(92).                RL1074.2
009800 01  RAC-REC-3.                                                   RL1074.2
009900     03  FILLER                  PICTURE IS X(24).                RL1074.2
010000     03  RECORD-NO-3             PICTURE IS 9999.                 RL1074.2
010100     03  FILLER                  PICTURE IS X(92).                RL1074.2
010200 WORKING-STORAGE SECTION.                                         RL1074.2
010300 01  RECORD-SKELTON.                                              RL1074.2
010400     03  FILLER PICTURE IS X(24) VALUE "  THIS IS RECORD NUMBER ".RL1074.2
010500     03  RECORD-NUMXXX           PICTURE IS  9999 VALUE IS ZERO.  RL1074.2
010600     03  FILLER                  PICTURE IS X(92) VALUE SPACE.    RL1074.2
010700 01  ACTUAL-KEY-1                                                 RL1074.2
010800         PICTURE   9(5).                                          RL1074.2
010900 01  ACTUAL-KEY-2                                                 RL1074.2
011000         PICTURE   9(5).                                          RL1074.2
011100 01  FILE-RECORD-INFORMATION-REC.                                 RL1074.2
011200     03 FILE-RECORD-INFO-SKELETON.                                RL1074.2
011300        05 FILLER                 PICTURE X(48)       VALUE       RL1074.2
011400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  RL1074.2
011500        05 FILLER                 PICTURE X(46)       VALUE       RL1074.2
011600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    RL1074.2
011700        05 FILLER                 PICTURE X(26)       VALUE       RL1074.2
011800             ",LFIL=000000,ORG=  ,LBLR= ".                        RL1074.2
011900        05 FILLER                 PICTURE X(37)       VALUE       RL1074.2
012000             ",RECKEY=                             ".             RL1074.2
012100        05 FILLER                 PICTURE X(38)       VALUE       RL1074.2
012200             ",ALTKEY1=                             ".            RL1074.2
012300        05 FILLER                 PICTURE X(38)       VALUE       RL1074.2
012400             ",ALTKEY2=                             ".            RL1074.2
012500        05 FILLER                 PICTURE X(7)        VALUE SPACE.RL1074.2
012600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              RL1074.2
012700        05 FILE-RECORD-INFO-P1-120.                               RL1074.2
012800           07 FILLER              PIC X(5).                       RL1074.2
012900           07 XFILE-NAME           PIC X(6).                      RL1074.2
013000           07 FILLER              PIC X(8).                       RL1074.2
013100           07 XRECORD-NAME         PIC X(6).                      RL1074.2
013200           07 FILLER              PIC X(1).                       RL1074.2
013300           07 REELUNIT-NUMBER     PIC 9(1).                       RL1074.2
013400           07 FILLER              PIC X(7).                       RL1074.2
013500           07 XRECORD-NUMBER       PIC 9(6).                      RL1074.2
013600           07 FILLER              PIC X(6).                       RL1074.2
013700           07 UPDATE-NUMBER       PIC 9(2).                       RL1074.2
013800           07 FILLER              PIC X(5).                       RL1074.2
013900           07 ODO-NUMBER          PIC 9(4).                       RL1074.2
014000           07 FILLER              PIC X(5).                       RL1074.2
014100           07 XPROGRAM-NAME        PIC X(5).                      RL1074.2
014200           07 FILLER              PIC X(7).                       RL1074.2
014300           07 XRECORD-LENGTH       PIC 9(6).                      RL1074.2
014400           07 FILLER              PIC X(7).                       RL1074.2
014500           07 CHARS-OR-RECORDS    PIC X(2).                       RL1074.2
014600           07 FILLER              PIC X(1).                       RL1074.2
014700           07 XBLOCK-SIZE          PIC 9(4).                      RL1074.2
014800           07 FILLER              PIC X(6).                       RL1074.2
014900           07 RECORDS-IN-FILE     PIC 9(6).                       RL1074.2
015000           07 FILLER              PIC X(5).                       RL1074.2
015100           07 XFILE-ORGANIZATION   PIC X(2).                      RL1074.2
015200           07 FILLER              PIC X(6).                       RL1074.2
015300           07 XLABEL-TYPE          PIC X(1).                      RL1074.2
015400        05 FILE-RECORD-INFO-P121-240.                             RL1074.2
015500           07 FILLER              PIC X(8).                       RL1074.2
015600           07 XRECORD-KEY          PIC X(29).                     RL1074.2
015700           07 FILLER              PIC X(9).                       RL1074.2
015800           07 ALTERNATE-KEY1      PIC X(29).                      RL1074.2
015900           07 FILLER              PIC X(9).                       RL1074.2
016000           07 ALTERNATE-KEY2      PIC X(29).                      RL1074.2
016100           07 FILLER              PIC X(7).                       RL1074.2
016200 01  TEST-RESULTS.                                                RL1074.2
016300     02 FILLER                   PIC X      VALUE SPACE.          RL1074.2
016400     02 FEATURE                  PIC X(20)  VALUE SPACE.          RL1074.2
016500     02 FILLER                   PIC X      VALUE SPACE.          RL1074.2
016600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          RL1074.2
016700     02 FILLER                   PIC X      VALUE SPACE.          RL1074.2
016800     02  PAR-NAME.                                                RL1074.2
016900       03 FILLER                 PIC X(19)  VALUE SPACE.          RL1074.2
017000       03  PARDOT-X              PIC X      VALUE SPACE.          RL1074.2
017100       03 DOTVALUE               PIC 99     VALUE ZERO.           RL1074.2
017200     02 FILLER                   PIC X(8)   VALUE SPACE.          RL1074.2
017300     02 RE-MARK                  PIC X(61).                       RL1074.2
017400 01  TEST-COMPUTED.                                               RL1074.2
017500     02 FILLER                   PIC X(30)  VALUE SPACE.          RL1074.2
017600     02 FILLER                   PIC X(17)  VALUE                 RL1074.2
017700            "       COMPUTED=".                                   RL1074.2
017800     02 COMPUTED-X.                                               RL1074.2
017900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          RL1074.2
018000     03 COMPUTED-N               REDEFINES COMPUTED-A             RL1074.2
018100                                 PIC -9(9).9(9).                  RL1074.2
018200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         RL1074.2
018300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     RL1074.2
018400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     RL1074.2
018500     03       CM-18V0 REDEFINES COMPUTED-A.                       RL1074.2
018600         04 COMPUTED-18V0                    PIC -9(18).          RL1074.2
018700         04 FILLER                           PIC X.               RL1074.2
018800     03 FILLER PIC X(50) VALUE SPACE.                             RL1074.2
018900 01  TEST-CORRECT.                                                RL1074.2
019000     02 FILLER PIC X(30) VALUE SPACE.                             RL1074.2
019100     02 FILLER PIC X(17) VALUE "       CORRECT =".                RL1074.2
019200     02 CORRECT-X.                                                RL1074.2
019300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         RL1074.2
019400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      RL1074.2
019500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         RL1074.2
019600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     RL1074.2
019700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     RL1074.2
019800     03      CR-18V0 REDEFINES CORRECT-A.                         RL1074.2
019900         04 CORRECT-18V0                     PIC -9(18).          RL1074.2
020000         04 FILLER                           PIC X.               RL1074.2
020100     03 FILLER PIC X(2) VALUE SPACE.                              RL1074.2
020200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     RL1074.2
020300 01  CCVS-C-1.                                                    RL1074.2
020400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PARL1074.2
020500-    "SS  PARAGRAPH-NAME                                          RL1074.2
020600-    "       REMARKS".                                            RL1074.2
020700     02 FILLER                     PIC X(20)    VALUE SPACE.      RL1074.2
020800 01  CCVS-C-2.                                                    RL1074.2
020900     02 FILLER                     PIC X        VALUE SPACE.      RL1074.2
021000     02 FILLER                     PIC X(6)     VALUE "TESTED".   RL1074.2
021100     02 FILLER                     PIC X(15)    VALUE SPACE.      RL1074.2
021200     02 FILLER                     PIC X(4)     VALUE "FAIL".     RL1074.2
021300     02 FILLER                     PIC X(94)    VALUE SPACE.      RL1074.2
021400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       RL1074.2
021500 01  REC-CT                        PIC 99       VALUE ZERO.       RL1074.2
021600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       RL1074.2
021700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       RL1074.2
021800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       RL1074.2
021900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       RL1074.2
022000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       RL1074.2
022100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       RL1074.2
022200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      RL1074.2
022300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       RL1074.2
022400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     RL1074.2
022500 01  CCVS-H-1.                                                    RL1074.2
022600     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1074.2
022700     02  FILLER                    PIC X(42)    VALUE             RL1074.2
022800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 RL1074.2
022900     02  FILLER                    PIC X(39)    VALUE SPACES.     RL1074.2
023000 01  CCVS-H-2A.                                                   RL1074.2
023100   02  FILLER                        PIC X(40)  VALUE SPACE.      RL1074.2
023200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  RL1074.2
023300   02  FILLER                        PIC XXXX   VALUE             RL1074.2
023400     "4.2 ".                                                      RL1074.2
023500   02  FILLER                        PIC X(28)  VALUE             RL1074.2
023600            " COPY - NOT FOR DISTRIBUTION".                       RL1074.2
023700   02  FILLER                        PIC X(41)  VALUE SPACE.      RL1074.2
023800                                                                  RL1074.2
023900 01  CCVS-H-2B.                                                   RL1074.2
024000   02  FILLER                        PIC X(15)  VALUE             RL1074.2
024100            "TEST RESULT OF ".                                    RL1074.2
024200   02  TEST-ID                       PIC X(9).                    RL1074.2
024300   02  FILLER                        PIC X(4)   VALUE             RL1074.2
024400            " IN ".                                               RL1074.2
024500   02  FILLER                        PIC X(12)  VALUE             RL1074.2
024600     " HIGH       ".                                              RL1074.2
024700   02  FILLER                        PIC X(22)  VALUE             RL1074.2
024800            " LEVEL VALIDATION FOR ".                             RL1074.2
024900   02  FILLER                        PIC X(58)  VALUE             RL1074.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1074.2
025100 01  CCVS-H-3.                                                    RL1074.2
025200     02  FILLER                      PIC X(34)  VALUE             RL1074.2
025300            " FOR OFFICIAL USE ONLY    ".                         RL1074.2
025400     02  FILLER                      PIC X(58)  VALUE             RL1074.2
025500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".RL1074.2
025600     02  FILLER                      PIC X(28)  VALUE             RL1074.2
025700            "  COPYRIGHT   1985 ".                                RL1074.2
025800 01  CCVS-E-1.                                                    RL1074.2
025900     02 FILLER                       PIC X(52)  VALUE SPACE.      RL1074.2
026000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              RL1074.2
026100     02 ID-AGAIN                     PIC X(9).                    RL1074.2
026200     02 FILLER                       PIC X(45)  VALUE SPACES.     RL1074.2
026300 01  CCVS-E-2.                                                    RL1074.2
026400     02  FILLER                      PIC X(31)  VALUE SPACE.      RL1074.2
026500     02  FILLER                      PIC X(21)  VALUE SPACE.      RL1074.2
026600     02 CCVS-E-2-2.                                               RL1074.2
026700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      RL1074.2
026800         03 FILLER                   PIC X      VALUE SPACE.      RL1074.2
026900         03 ENDER-DESC               PIC X(44)  VALUE             RL1074.2
027000            "ERRORS ENCOUNTERED".                                 RL1074.2
027100 01  CCVS-E-3.                                                    RL1074.2
027200     02  FILLER                      PIC X(22)  VALUE             RL1074.2
027300            " FOR OFFICIAL USE ONLY".                             RL1074.2
027400     02  FILLER                      PIC X(12)  VALUE SPACE.      RL1074.2
027500     02  FILLER                      PIC X(58)  VALUE             RL1074.2
027600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".RL1074.2
027700     02  FILLER                      PIC X(13)  VALUE SPACE.      RL1074.2
027800     02 FILLER                       PIC X(15)  VALUE             RL1074.2
027900             " COPYRIGHT 1985".                                   RL1074.2
028000 01  CCVS-E-4.                                                    RL1074.2
028100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      RL1074.2
028200     02 FILLER                       PIC X(4)   VALUE " OF ".     RL1074.2
028300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      RL1074.2
028400     02 FILLER                       PIC X(40)  VALUE             RL1074.2
028500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       RL1074.2
028600 01  XXINFO.                                                      RL1074.2
028700     02 FILLER                       PIC X(19)  VALUE             RL1074.2
028800            "*** INFORMATION ***".                                RL1074.2
028900     02 INFO-TEXT.                                                RL1074.2
029000       04 FILLER                     PIC X(8)   VALUE SPACE.      RL1074.2
029100       04 XXCOMPUTED                 PIC X(20).                   RL1074.2
029200       04 FILLER                     PIC X(5)   VALUE SPACE.      RL1074.2
029300       04 XXCORRECT                  PIC X(20).                   RL1074.2
029400     02 INF-ANSI-REFERENCE           PIC X(48).                   RL1074.2
029500 01  HYPHEN-LINE.                                                 RL1074.2
029600     02 FILLER  PIC IS X VALUE IS SPACE.                          RL1074.2
029700     02 FILLER  PIC IS X(65)    VALUE IS "************************RL1074.2
029800-    "*****************************************".                 RL1074.2
029900     02 FILLER  PIC IS X(54)    VALUE IS "************************RL1074.2
030000-    "******************************".                            RL1074.2
030100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             RL1074.2
030200     "RL107A".                                                    RL1074.2
030300 PROCEDURE DIVISION.                                              RL1074.2
030400 CCVS1 SECTION.                                                   RL1074.2
030500 OPEN-FILES.                                                      RL1074.2
030600     OPEN    OUTPUT PRINT-FILE.                                   RL1074.2
030700     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  RL1074.2
030800     MOVE    SPACE TO TEST-RESULTS.                               RL1074.2
030900     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              RL1074.2
031000     MOVE    ZERO TO REC-SKL-SUB.                                 RL1074.2
031100     PERFORM CCVS-INIT-FILE 9 TIMES.                              RL1074.2
031200 CCVS-INIT-FILE.                                                  RL1074.2
031300     ADD     1 TO REC-SKL-SUB.                                    RL1074.2
031400     MOVE    FILE-RECORD-INFO-SKELETON                            RL1074.2
031500          TO FILE-RECORD-INFO (REC-SKL-SUB).                      RL1074.2
031600 CCVS-INIT-EXIT.                                                  RL1074.2
031700     GO TO CCVS1-EXIT.                                            RL1074.2
031800 CLOSE-FILES.                                                     RL1074.2
031900     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   RL1074.2
032000 TERMINATE-CCVS.                                                  RL1074.2
032100*    EXIT PROGRAM.                                                RL1074.2
032200*TERMINATE-CALL.                                                  RL1074.2
032300     STOP     RUN.                                                RL1074.2
032400 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         RL1074.2
032500 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           RL1074.2
032600 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          RL1074.2
032700 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      RL1074.2
032800     MOVE "****TEST DELETED****" TO RE-MARK.                      RL1074.2
032900 PRINT-DETAIL.                                                    RL1074.2
033000     IF REC-CT NOT EQUAL TO ZERO                                  RL1074.2
033100             MOVE "." TO PARDOT-X                                 RL1074.2
033200             MOVE REC-CT TO DOTVALUE.                             RL1074.2
033300     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      RL1074.2
033400     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               RL1074.2
033500        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 RL1074.2
033600          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 RL1074.2
033700     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              RL1074.2
033800     MOVE SPACE TO CORRECT-X.                                     RL1074.2
033900     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         RL1074.2
034000     MOVE     SPACE TO RE-MARK.                                   RL1074.2
034100 HEAD-ROUTINE.                                                    RL1074.2
034200     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1074.2
034300     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  RL1074.2
034400     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1074.2
034500     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  RL1074.2
034600 COLUMN-NAMES-ROUTINE.                                            RL1074.2
034700     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1074.2
034800     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1074.2
034900     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        RL1074.2
035000 END-ROUTINE.                                                     RL1074.2
035100     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.RL1074.2
035200 END-RTN-EXIT.                                                    RL1074.2
035300     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1074.2
035400 END-ROUTINE-1.                                                   RL1074.2
035500      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      RL1074.2
035600      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               RL1074.2
035700      ADD PASS-COUNTER TO ERROR-HOLD.                             RL1074.2
035800*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   RL1074.2
035900      MOVE PASS-COUNTER TO CCVS-E-4-1.                            RL1074.2
036000      MOVE ERROR-HOLD TO CCVS-E-4-2.                              RL1074.2
036100      MOVE CCVS-E-4 TO CCVS-E-2-2.                                RL1074.2
036200      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           RL1074.2
036300  END-ROUTINE-12.                                                 RL1074.2
036400      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        RL1074.2
036500     IF       ERROR-COUNTER IS EQUAL TO ZERO                      RL1074.2
036600         MOVE "NO " TO ERROR-TOTAL                                RL1074.2
036700         ELSE                                                     RL1074.2
036800         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       RL1074.2
036900     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           RL1074.2
037000     PERFORM WRITE-LINE.                                          RL1074.2
037100 END-ROUTINE-13.                                                  RL1074.2
037200     IF DELETE-COUNTER IS EQUAL TO ZERO                           RL1074.2
037300         MOVE "NO " TO ERROR-TOTAL  ELSE                          RL1074.2
037400         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      RL1074.2
037500     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   RL1074.2
037600     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1074.2
037700      IF   INSPECT-COUNTER EQUAL TO ZERO                          RL1074.2
037800          MOVE "NO " TO ERROR-TOTAL                               RL1074.2
037900      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   RL1074.2
038000      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            RL1074.2
038100      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          RL1074.2
038200     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           RL1074.2
038300 WRITE-LINE.                                                      RL1074.2
038400     ADD 1 TO RECORD-COUNT.                                       RL1074.2
038500*    IF RECORD-COUNT GREATER 50                                   RL1074.2
038600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          RL1074.2
038700*        MOVE SPACE TO DUMMY-RECORD                               RL1074.2
038800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  RL1074.2
038900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             RL1074.2
039000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     RL1074.2
039100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          RL1074.2
039200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          RL1074.2
039300*        MOVE ZERO TO RECORD-COUNT.                               RL1074.2
039400     PERFORM WRT-LN.                                              RL1074.2
039500 WRT-LN.                                                          RL1074.2
039600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               RL1074.2
039700     MOVE SPACE TO DUMMY-RECORD.                                  RL1074.2
039800 BLANK-LINE-PRINT.                                                RL1074.2
039900     PERFORM WRT-LN.                                              RL1074.2
040000 FAIL-ROUTINE.                                                    RL1074.2
040100     IF     COMPUTED-X NOT EQUAL TO SPACE                         RL1074.2
040200            GO TO   FAIL-ROUTINE-WRITE.                           RL1074.2
040300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.RL1074.2
040400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1074.2
040500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   RL1074.2
040600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1074.2
040700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1074.2
040800     GO TO  FAIL-ROUTINE-EX.                                      RL1074.2
040900 FAIL-ROUTINE-WRITE.                                              RL1074.2
041000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         RL1074.2
041100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 RL1074.2
041200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. RL1074.2
041300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         RL1074.2
041400 FAIL-ROUTINE-EX. EXIT.                                           RL1074.2
041500 BAIL-OUT.                                                        RL1074.2
041600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   RL1074.2
041700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           RL1074.2
041800 BAIL-OUT-WRITE.                                                  RL1074.2
041900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  RL1074.2
042000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 RL1074.2
042100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   RL1074.2
042200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         RL1074.2
042300 BAIL-OUT-EX. EXIT.                                               RL1074.2
042400 CCVS1-EXIT.                                                      RL1074.2
042500     EXIT.                                                        RL1074.2
042600 SECT-RL107-001 SECTION.                                          RL1074.2
042700 TEST-1.                                                          RL1074.2
042800     MOVE  "OPEN OUTPUT RL-FR7" TO FEATURE.                       RL1074.2
042900     MOVE  "REL-TEST-001" TO PAR-NAME.                            RL1074.2
043000     PERFORM PRINT-DETAIL.                                        RL1074.2
043100     OPEN OUTPUT RL-FR7.                                          RL1074.2
043200 TEST-2.                                                          RL1074.2
043300     MOVE "OPEN OUTPUT RL-FR8" TO FEATURE.                        RL1074.2
043400     MOVE  "REL-TEST-002" TO PAR-NAME.                            RL1074.2
043500     PERFORM PRINT-DETAIL.                                        RL1074.2
043600     OPEN OUTPUT RL-FR8.                                          RL1074.2
043700 TEST-3-INIT.                                                     RL1074.2
043800     MOVE "WRITE RL-FR7" TO FEATURE.                              RL1074.2
043900     MOVE  "REL-TEST-003" TO PAR-NAME.                            RL1074.2
044000     MOVE 0 TO ACTUAL-KEY-1.                                      RL1074.2
044100     MOVE ZERO TO RECORD-NUMXXX.                                  RL1074.2
044200 TEST-3.                                                          RL1074.2
044300     ADD 1 TO RECORD-NUMXXX.                                      RL1074.2
044400     ADD 1 TO ACTUAL-KEY-1.                                       RL1074.2
044500     IF RECORD-NUMXXX IS GREATER THAN 25 GO TO TEST-3-EXIT.       RL1074.2
044600     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
044700     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
044800     GO TO TEST-3.                                                RL1074.2
044900 TEST-3-EXIT.                                                     RL1074.2
045000     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
045100        PERFORM PRINT-DETAIL.                                     RL1074.2
045200 TEST-4-INIT.                                                     RL1074.2
045300     MOVE "REL-TEST-004" TO PAR-NAME.                             RL1074.2
045400     MOVE ZERO TO REC-CT.                                         RL1074.2
045500     MOVE 51 TO RECORD-NUMXXX.                                    RL1074.2
045600     MOVE 51 TO ACTUAL-KEY-1.                                     RL1074.2
045700 TEST-4.                                                          RL1074.2
045800     SUBTRACT 1 FROM RECORD-NUMXXX.                               RL1074.2
045900     SUBTRACT 1 FROM ACTUAL-KEY-1.                                RL1074.2
046000     IF RECORD-NUMXXX IS LESS THAN 26 GO TO TEST-4-EXIT.          RL1074.2
046100     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
046200     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
046300     GO TO TEST-4.                                                RL1074.2
046400 TEST-4-EXIT.                                                     RL1074.2
046500     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
046600        PERFORM PRINT-DETAIL.                                     RL1074.2
046700 TEST-5-INIT.                                                     RL1074.2
046800     MOVE "REL-TEST-005" TO PAR-NAME.                             RL1074.2
046900     MOVE ZERO TO REC-CT.                                         RL1074.2
047000     MOVE 48 TO ACTUAL-KEY-1.                                     RL1074.2
047100     MOVE 48 TO RECORD-NUMXXX.                                    RL1074.2
047200 TEST-5.                                                          RL1074.2
047300     ADD 3 TO RECORD-NUMXXX.                                      RL1074.2
047400     ADD 3 TO ACTUAL-KEY-1.                                       RL1074.2
047500     IF RECORD-NUMXXX IS GREATER THAN 125 GO TO TEST-5-EXIT.      RL1074.2
047600     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
047700     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
047800     GO TO TEST-5.                                                RL1074.2
047900 TEST-5-EXIT.                                                     RL1074.2
048000     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
048100        PERFORM PRINT-DETAIL.                                     RL1074.2
048200 TEST-6-INIT.                                                     RL1074.2
048300     MOVE "REL-TEST-006" TO PAR-NAME.                             RL1074.2
048400     MOVE ZERO TO REC-CT.                                         RL1074.2
048500     MOVE 49 TO ACTUAL-KEY-1.                                     RL1074.2
048600     MOVE 49 TO RECORD-NUMXXX.                                    RL1074.2
048700 TEST-6.                                                          RL1074.2
048800     ADD 3 TO RECORD-NUMXXX.                                      RL1074.2
048900     ADD 3 TO ACTUAL-KEY-1.                                       RL1074.2
049000     IF RECORD-NUMXXX IS GREATER THAN 125 GO TO TEST-6-EXIT.      RL1074.2
049100     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
049200     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
049300     GO TO TEST-6.                                                RL1074.2
049400 TEST-6-EXIT.                                                     RL1074.2
049500     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
049600        PERFORM PRINT-DETAIL.                                     RL1074.2
049700 TEST-7-INIT.                                                     RL1074.2
049800     MOVE "REL-TEST-007" TO PAR-NAME.                             RL1074.2
049900     MOVE ZERO TO REC-CT.                                         RL1074.2
050000     MOVE 128 TO ACTUAL-KEY-1.                                    RL1074.2
050100     MOVE 128 TO RECORD-NUMXXX.                                   RL1074.2
050200 TEST-7.                                                          RL1074.2
050300     SUBTRACT 3 FROM RECORD-NUMXXX.                               RL1074.2
050400     SUBTRACT 3 FROM ACTUAL-KEY-1.                                RL1074.2
050500     IF RECORD-NUMXXX IS LESS THAN 53 GO TO TEST-7-EXIT.          RL1074.2
050600     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
050700     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
050800     GO TO TEST-7.                                                RL1074.2
050900 TEST-7-EXIT.                                                     RL1074.2
051000     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
051100        PERFORM PRINT-DETAIL.                                     RL1074.2
051200 TEST-8-INIT.                                                     RL1074.2
051300     MOVE "WRITE RL-FR8" TO FEATURE.                              RL1074.2
051400     MOVE "REL-TEST-008" TO PAR-NAME.                             RL1074.2
051500     MOVE ZERO TO REC-CT.                                         RL1074.2
051600     MOVE 0 TO ACTUAL-KEY-2.                                      RL1074.2
051700     MOVE 0 TO RECORD-NUMXXX.                                     RL1074.2
051800 TEST-8.                                                          RL1074.2
051900     ADD 1 TO RECORD-NUMXXX.                                      RL1074.2
052000     ADD 1 TO ACTUAL-KEY-2.                                       RL1074.2
052100     IF RECORD-NUMXXX IS GREATER THAN 25 GO TO TEST-8-EXIT.       RL1074.2
052200     MOVE RECORD-SKELTON TO RAC-REC-2.                            RL1074.2
052300     WRITE RAC-REC-2 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
052400     GO TO TEST-8.                                                RL1074.2
052500 TEST-8-EXIT.                                                     RL1074.2
052600     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
052700        PERFORM PRINT-DETAIL.                                     RL1074.2
052800 TEST-9.                                                          RL1074.2
052900     MOVE "CLOSE RL-FR7" TO FEATURE.                              RL1074.2
053000     MOVE "REL-TEST-009" TO PAR-NAME.                             RL1074.2
053100     MOVE ZERO TO REC-CT.                                         RL1074.2
053200     PERFORM PRINT-DETAIL.                                        RL1074.2
053300     CLOSE RL-FR7.                                                RL1074.2
053400 TEST-10.                                                         RL1074.2
053500     MOVE "CLOSE RL-FR8" TO FEATURE.                              RL1074.2
053600     MOVE "REL-TEST-010" TO PAR-NAME.                             RL1074.2
053700     PERFORM PRINT-DETAIL.                                        RL1074.2
053800     CLOSE RL-FR8.                                                RL1074.2
053900 TEST-11.                                                         RL1074.2
054000     MOVE "OPEN INPUT RL-FR8" TO FEATURE.                         RL1074.2
054100     MOVE "REL-TEST-011" TO PAR-NAME.                             RL1074.2
054200     PERFORM PRINT-DETAIL.                                        RL1074.2
054300     OPEN INPUT RL-FR8.                                           RL1074.2
054400 TEST-12.                                                         RL1074.2
054500     MOVE "OPEN I-O RL-FR7" TO FEATURE.                           RL1074.2
054600     MOVE "REL-TEST-012" TO PAR-NAME.                             RL1074.2
054700     PERFORM PRINT-DETAIL.                                        RL1074.2
054800     OPEN I-O RL-FR7.                                             RL1074.2
054900 TEST-13-INIT.                                                    RL1074.2
055000     MOVE "READ RL-FR7" TO FEATURE.                               RL1074.2
055100     MOVE "REL-TEST-013" TO PAR-NAME.                             RL1074.2
055200     MOVE 9 TO ACTUAL-KEY-1.                                      RL1074.2
055300     MOVE 9 TO RECORD-NUMXXX.                                     RL1074.2
055400 TEST-13.                                                         RL1074.2
055500     ADD 1 TO ACTUAL-KEY-1.                                       RL1074.2
055600     ADD 1 TO RECORD-NUMXXX.                                      RL1074.2
055700     IF RECORD-NUMXXX IS GREATER THAN 20 GO TO TEST-13-EXIT.      RL1074.2
055800     READ RL-FR7 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
055900        GO TO TEST-13.                                            RL1074.2
056000     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
056100        PERFORM ERROR-WRONG-RECORD.                               RL1074.2
056200     GO TO TEST-13.                                               RL1074.2
056300 TEST-13-EXIT.                                                    RL1074.2
056400     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
056500        PERFORM PRINT-DETAIL.                                     RL1074.2
056600 TEST-14-INIT.                                                    RL1074.2
056700     MOVE "READ RL-FR8" TO FEATURE.                               RL1074.2
056800     MOVE "REL-TEST-014" TO PAR-NAME.                             RL1074.2
056900     MOVE ZERO TO REC-CT.                                         RL1074.2
057000     MOVE 0 TO ACTUAL-KEY-2.                                      RL1074.2
057100     MOVE 0 TO RECORD-NUMXXX.                                     RL1074.2
057200 TEST-14.                                                         RL1074.2
057300     ADD 3 TO ACTUAL-KEY-2.                                       RL1074.2
057400     ADD 3 TO RECORD-NUMXXX.                                      RL1074.2
057500     IF RECORD-NUMXXX IS GREATER THAN 25 GO TO TEST-14-EXIT.      RL1074.2
057600     READ RL-FR8 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
057700        GO TO TEST-14.                                            RL1074.2
057800     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-2                 RL1074.2
057900        MOVE RECORD-NO-2 TO RECORD-NO-1                           RL1074.2
058000        PERFORM ERROR-WRONG-RECORD.                               RL1074.2
058100     GO TO TEST-14.                                               RL1074.2
058200 TEST-14-EXIT.                                                    RL1074.2
058300     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
058400        PERFORM PRINT-DETAIL.                                     RL1074.2
058500 TEST-15-INIT.                                                    RL1074.2
058600     MOVE "READ RL-FR7" TO FEATURE.                               RL1074.2
058700     MOVE "REL-TEST-015" TO PAR-NAME.                             RL1074.2
058800     MOVE ZERO TO REC-CT.                                         RL1074.2
058900     MOVE 14 TO ACTUAL-KEY-1.                                     RL1074.2
059000     MOVE 14 TO RECORD-NUMXXX.                                    RL1074.2
059100 TEST-15.                                                         RL1074.2
059200     ADD  14 TO ACTUAL-KEY-1.                                     RL1074.2
059300     ADD  14 TO RECORD-NUMXXX.                                    RL1074.2
059400     IF RECORD-NUMXXX IS GREATER THAN 125 GO TO TEST-15-EXIT.     RL1074.2
059500     READ RL-FR7 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
059600        GO TO TEST-14.                                            RL1074.2
059700     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
059800        PERFORM ERROR-WRONG-RECORD.                               RL1074.2
059900     GO TO TEST-15.                                               RL1074.2
060000 TEST-15-EXIT.                                                    RL1074.2
060100     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
060200        PERFORM PRINT-DETAIL.                                     RL1074.2
060300 TEST-16-INIT.                                                    RL1074.2
060400     MOVE "REL-TEST-016" TO PAR-NAME.                             RL1074.2
060500     MOVE ZERO TO REC-CT.                                         RL1074.2
060600     MOVE 11 TO ACTUAL-KEY-1.                                     RL1074.2
060700     MOVE 11 TO RECORD-NUMXXX.                                    RL1074.2
060800 TEST-16.                                                         RL1074.2
060900     ADD 17 TO ACTUAL-KEY-1.                                      RL1074.2
061000     ADD 17 TO RECORD-NUMXXX.                                     RL1074.2
061100     IF RECORD-NUMXXX IS GREATER THAN 125 GO TO TEST-16-EXIT.     RL1074.2
061200     READ RL-FR7 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
061300        GO TO TEST-16.                                            RL1074.2
061400     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
061500        PERFORM ERROR-WRONG-RECORD.                               RL1074.2
061600     GO TO TEST-16.                                               RL1074.2
061700 TEST-16-EXIT.                                                    RL1074.2
061800     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
061900        PERFORM PRINT-DETAIL.                                     RL1074.2
062000 TEST-17-INIT.                                                    RL1074.2
062100     MOVE "WRITE RL-FR7" TO FEATURE.                              RL1074.2
062200     MOVE "REL-TEST-017" TO PAR-NAME.                             RL1074.2
062300     MOVE ZERO TO REC-CT.                                         RL1074.2
062400     MOVE 125 TO ACTUAL-KEY-1.                                    RL1074.2
062500     MOVE 125 TO RECORD-NUMXXX.                                   RL1074.2
062600 TEST-17.                                                         RL1074.2
062700     ADD 5 TO ACTUAL-KEY-1.                                       RL1074.2
062800     ADD 5 TO RECORD-NUMXXX.                                      RL1074.2
062900     IF RECORD-NUMXXX IS GREATER THAN 200 GO TO TEST-17-EXIT.     RL1074.2
063000     MOVE RECORD-SKELTON TO RAC-REC-1.                            RL1074.2
063100     WRITE RAC-REC-1 INVALID KEY PERFORM ERROR-INVALID-KEY.       RL1074.2
063200     GO TO TEST-17.                                               RL1074.2
063300 TEST-17-EXIT.                                                    RL1074.2
063400     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
063500        PERFORM PRINT-DETAIL.                                     RL1074.2
063600 TEST-18.                                                         RL1074.2
063700     MOVE "READ RL-FR7" TO FEATURE.                               RL1074.2
063800     MOVE "REL-TEST-018" TO PAR-NAME.                             RL1074.2
063900     MOVE ZERO TO REC-CT.                                         RL1074.2
064000     MOVE 121 TO ACTUAL-KEY-1.                                    RL1074.2
064100     MOVE 121 TO RECORD-NUMXXX.                                   RL1074.2
064200     READ RL-FR7 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
064300        GO TO TEST-19.                                            RL1074.2
064400     IF RECORD-NO-1 IS NOT EQUAL TO RECORD-NUMXXX                 RL1074.2
064500        PERFORM ERROR-WRONG-RECORD                                RL1074.2
064600        GO TO TEST-19.                                            RL1074.2
064700     PERFORM PASS.                                                RL1074.2
064800     PERFORM PRINT-DETAIL.                                        RL1074.2
064900 TEST-19.                                                         RL1074.2
065000     MOVE "REL-TEST-019" TO PAR-NAME.                             RL1074.2
065100     MOVE ZERO TO REC-CT.                                         RL1074.2
065200     MOVE 57 TO ACTUAL-KEY-1.                                     RL1074.2
065300     MOVE 57 TO RECORD-NUMXXX.                                    RL1074.2
065400     READ RL-FR7 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
065500        GO TO TEST-20.                                            RL1074.2
065600     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
065700        PERFORM ERROR-WRONG-RECORD                                RL1074.2
065800        GO TO TEST-20.                                            RL1074.2
065900     PERFORM PASS.                                                RL1074.2
066000     PERFORM PRINT-DETAIL.                                        RL1074.2
066100 TEST-20.                                                         RL1074.2
066200     MOVE "READ RL-FR8" TO FEATURE.                               RL1074.2
066300     MOVE "REL-TEST-020" TO PAR-NAME.                             RL1074.2
066400     MOVE ZERO TO REC-CT.                                         RL1074.2
066500     MOVE 12 TO RECORD-NUMXXX.                                    RL1074.2
066600     MOVE 12 TO ACTUAL-KEY-2.                                     RL1074.2
066700     READ RL-FR8 INVALID KEY PERFORM ERROR-INVALID-KEY            RL1074.2
066800        GO TO TEST-21.                                            RL1074.2
066900     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-2                 RL1074.2
067000        MOVE RECORD-NO-2 TO RECORD-NO-1                           RL1074.2
067100        PERFORM ERROR-WRONG-RECORD                                RL1074.2
067200        GO TO TEST-21.                                            RL1074.2
067300     PERFORM PASS                                                 RL1074.2
067400     PERFORM PRINT-DETAIL.                                        RL1074.2
067500 TEST-21.                                                         RL1074.2
067600     MOVE "INVALID KEY RL-FR7" TO FEATURE.                        RL1074.2
067700     MOVE "REL-TEST-021" TO PAR-NAME.                             RL1074.2
067800     MOVE ZERO TO REC-CT.                                         RL1074.2
067900     MOVE 237 TO ACTUAL-KEY-1.                                    RL1074.2
068000     READ RL-FR7 INVALID KEY PERFORM PASS                         RL1074.2
068100        PERFORM PRINT-DETAIL                                      RL1074.2
068200        GO TO TEST-22.                                            RL1074.2
068300     PERFORM ERROR-INVALID-KEY-EXPECTED.                          RL1074.2
068400 TEST-22.                                                         RL1074.2
068500     MOVE "REL-TEST-022" TO PAR-NAME.                             RL1074.2
068600     MOVE 250 TO ACTUAL-KEY-1.                                    RL1074.2
068700     READ RL-FR7 INVALID KEY PERFORM PASS                         RL1074.2
068800        PERFORM PRINT-DETAIL                                      RL1074.2
068900        GO TO TEST-23.                                            RL1074.2
069000     PERFORM ERROR-INVALID-KEY-EXPECTED.                          RL1074.2
069100 TEST-23.                                                         RL1074.2
069200     MOVE "INVALID KEY RL-FR8" TO FEATURE.                        RL1074.2
069300     MOVE "REL-TEST-023" TO PAR-NAME.                             RL1074.2
069400     MOVE 150 TO ACTUAL-KEY-2.                                    RL1074.2
069500     READ RL-FR8 INVALID KEY PERFORM PASS                         RL1074.2
069600        PERFORM PRINT-DETAIL                                      RL1074.2
069700        GO TO TEST-24.                                            RL1074.2
069800     MOVE ACTUAL-KEY-2 TO ACTUAL-KEY-1.                           RL1074.2
069900     PERFORM ERROR-INVALID-KEY-EXPECTED.                          RL1074.2
070000 TEST-24.                                                         RL1074.2
070100     MOVE "INVALID KEY RL-FR7" TO FEATURE.                        RL1074.2
070200     MOVE "REL-TEST-024" TO PAR-NAME.                             RL1074.2
070300     MOVE 230 TO ACTUAL-KEY-1.                                    RL1074.2
070400     READ RL-FR7 INVALID KEY PERFORM PASS                         RL1074.2
070500        PERFORM PRINT-DETAIL                                      RL1074.2
070600        GO TO TEST-25-INIT.                                       RL1074.2
070700     PERFORM ERROR-INVALID-KEY-EXPECTED.                          RL1074.2
070800 TEST-25-INIT.                                                    RL1074.2
070900     MOVE "REWRITE RL-FR7" TO FEATURE.                            RL1074.2
071000     MOVE "REL-TEST-025" TO PAR-NAME.                             RL1074.2
071100     MOVE  0 TO ACTUAL-KEY-1.                                     RL1074.2
071200     MOVE  0 TO RECORD-NUMXXX.                                    RL1074.2
071300     MOVE ZERO TO REC-CT.                                         RL1074.2
071400 TEST-25.                                                         RL1074.2
071500     ADD 10 TO ACTUAL-KEY-1.                                      RL1074.2
071600     ADD 10 TO RECORD-NUMXXX.                                     RL1074.2
071700     IF RECORD-NUMXXX IS GREATER THAN 100 GO TO TEST-25-RESET.    RL1074.2
071800     READ RL-FR7 INVALID KEY                                      RL1074.2
071900        MOVE "INVALID KEY ON READ BEFORE REWRITE" TO RE-MARK      RL1074.2
072000        PERFORM ERROR-WRONG-RECORD                                RL1074.2
072100        GO TO TEST-25.                                            RL1074.2
072200     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
072300        PERFORM ERROR-WRONG-RECORD                                RL1074.2
072400        GO TO TEST-25.                                            RL1074.2
072500     MOVE "UPDATED" TO UPDATE-FIELD.                              RL1074.2
072600     REWRITE RAC-REC-1 INVALID KEY                                RL1074.2
072700        MOVE "INVALID KEY ON REWRITE" TO RE-MARK                  RL1074.2
072800        PERFORM ERROR-INVALID-KEY.                                RL1074.2
072900     GO TO TEST-25.                                               RL1074.2
073000 TEST-25-RESET.                                                   RL1074.2
073100     MOVE 0  TO ACTUAL-KEY-1.                                     RL1074.2
073200     MOVE 0  TO RECORD-NUMXXX.                                    RL1074.2
073300 TEST-25-READ.                                                    RL1074.2
073400     ADD 10 TO ACTUAL-KEY-1.                                      RL1074.2
073500     ADD 10 TO RECORD-NUMXXX.                                     RL1074.2
073600     IF RECORD-NUMXXX IS GREATER THAN 100 GO TO TEST-25-EXIT.     RL1074.2
073700     READ RL-FR7 INVALID KEY                                      RL1074.2
073800        MOVE "INVALID KEY ON READ AFTER REWRITE" TO RE-MARK       RL1074.2
073900        PERFORM ERROR-INVALID-KEY                                 RL1074.2
074000        GO TO TEST-25-READ.                                       RL1074.2
074100     IF RECORD-NUMXXX IS NOT EQUAL TO RECORD-NO-1                 RL1074.2
074200        PERFORM ERROR-WRONG-RECORD                                RL1074.2
074300        GO TO TEST-25-READ.                                       RL1074.2
074400     IF UPDATE-FIELD IS EQUAL TO "UPDATED" GO TO TEST-25-READ.    RL1074.2
074500     IF REC-CT IS EQUAL TO ZERO PERFORM FAIL.                     RL1074.2
074600     ADD 1 TO REC-CT.                                             RL1074.2
074700     MOVE UPDATE-FIELD TO COMPUTED-A.                             RL1074.2
074800     MOVE "UPDATED" TO CORRECT-A.                                 RL1074.2
074900     MOVE "RECORD NOT UPDATED PROPERLY" TO RE-MARK.               RL1074.2
075000     PERFORM PRINT-DETAIL.                                        RL1074.2
075100     GO TO TEST-25-READ.                                          RL1074.2
075200 TEST-25-EXIT.                                                    RL1074.2
075300     IF REC-CT IS EQUAL TO ZERO PERFORM PASS                      RL1074.2
075400        PERFORM PRINT-DETAIL.                                     RL1074.2
075500 TEST-26.                                                         RL1074.2
075600     MOVE "CLOSE RL-FR8" TO FEATURE.                              RL1074.2
075700     MOVE "REL-TEST-026" TO PAR-NAME.                             RL1074.2
075800     MOVE ZERO TO REC-CT.                                         RL1074.2
075900     PERFORM PRINT-DETAIL.                                        RL1074.2
076000     CLOSE RL-FR8.                                                RL1074.2
076100 TEST-27.                                                         RL1074.2
076200     MOVE "CLOSE RL-FR7" TO FEATURE.                              RL1074.2
076300     MOVE "REL-TEST-027" TO PAR-NAME.                             RL1074.2
076400     PERFORM PRINT-DETAIL.                                        RL1074.2
076500     CLOSE RL-FR7.                                                RL1074.2
076600     GO TO SECT-RC-03-001-EXIT.                                   RL1074.2
076700 ERROR-INVALID-KEY.                                               RL1074.2
076800     IF REC-CT IS EQUAL TO ZERO PERFORM FAIL.                     RL1074.2
076900     ADD 1 TO REC-CT.                                             RL1074.2
077000     MOVE RECORD-NUMXXX TO CORRECT-18V0.                          RL1074.2
077100     MOVE "INVALID KEY" TO COMPUTED-A.                            RL1074.2
077200     PERFORM PRINT-DETAIL.                                        RL1074.2
077300 ERROR-INVALID-KEY-EXPECTED.                                      RL1074.2
077400     PERFORM FAIL.                                                RL1074.2
077500     ADD 1 TO REC-CT.                                             RL1074.2
077600     MOVE ACTUAL-KEY-1 TO COMPUTED-18V0.                          RL1074.2
077700     MOVE "INVALID KEY" TO CORRECT-A.                             RL1074.2
077800     MOVE "INVALID KEY EXPECTED ON READ" TO RE-MARK.              RL1074.2
077900     PERFORM PRINT-DETAIL.                                        RL1074.2
078000 ERROR-WRONG-RECORD.                                              RL1074.2
078100     IF REC-CT IS EQUAL TO ZERO PERFORM FAIL.                     RL1074.2
078200     ADD 1 TO REC-CT.                                             RL1074.2
078300     MOVE RECORD-NO-1 TO COMPUTED-18V0.                           RL1074.2
078400     MOVE RECORD-NUMXXX TO CORRECT-18V0.                          RL1074.2
078500     MOVE "WRONG RECORD FOUND" TO RE-MARK.                        RL1074.2
078600     PERFORM PRINT-DETAIL.                                        RL1074.2
078700 SECT-RC-03-001-EXIT.                                             RL1074.2
078800     EXIT.                                                        RL1074.2
078900 CCVS-EXIT SECTION.                                               RL1074.2
079000 CCVS-999999.                                                     RL1074.2
079100     GO TO CLOSE-FILES.                                           RL1074.2
      *END-OF,RL107A                                                                  
