      *HEADER,COBOL,IX105A                                                            
000100 IDENTIFICATION DIVISION.                                         IX1054.2
000200 PROGRAM-ID.                                                      IX1054.2
000300     IX105A.                                                      IX1054.2
000400****************************************************************  IX1054.2
000500*                                                              *  IX1054.2
000600*    VALIDATION FOR:-                                          *  IX1054.2
000700*                                                              *  IX1054.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1054.2
000900*                                                              *  IX1054.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1054.2
001100*                                                              *  IX1054.2
001200****************************************************************  IX1054.2
001300*GENERAL:    THIS PROGRAM PROCESSES THREE INDEXED I-O FILES       IX1054.2
001400*            IDENTIFIED AS IX-FR1,IX-FR2 AND IX-FR3.  THE FUNCTIONIX1054.2
001500*            OF THIS PROGRAM IS TO CREATE THREE INDEXED FILES     IX1054.2
001600*            RANDOMLLY (ACCESS MODE RANDOM) AND VERIFY THAT THEY  IX1054.2
001700*            WERE CREATED CORRECTLY.  THE FILES PROCESSED         IX1054.2
001800*            CONTAIN VARIABLE LENGTH RECORDS.                     IX1054.2
001900*                                                                 IX1054.2
002000*            X-CARD PARAMETERS WHICH MUST BE SUPPLIED FOR THIS    IX1054.2
002100*            PROGRAM ARE:                                         IX1054.2
002200*                                                                 IX1054.2
002300*                 X-24   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR IX1054.2
002400*                         INDEXED  I-O DATA FILE-1                IX1054.2
002500*                 X-25   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR IX1054.2
002600*                         INDEXED  I-O DATA FILE-2                IX1054.2
002700*                 X-26   IMPLEMENTOR-NAME IN ASSIGN TO CLAUSE FOR IX1054.2
002800*                         INDEXED  I-O DATA FILE-3                IX1054.2
002900*                 X-55   SYSTEM PRINTER                           IX1054.2
003000*                 X-62   FOR RAW-DATA                             IX1054.2
003100*                 X-82   SOURCE-COMPUTER                          IX1054.2
003200*                 X-83   OBJECT-COMPUTER.                         IX1054.2
003300*                                                                 IX1054.2
003400*  THIS PROGRAM SHOULD BE RUN ONLY WHEN AN IMPLEMENTATION   *     IX1054.2
003500*  PROVIDES VARIABLE-LENGTH RECORDS FOR THE RECORD CONTAINS *     IX1054.2
003600*  INTEGER TO INTEGER CLAUSE.                               *     IX1054.2
003700*                                                           *     IX1054.2
003800***************************************************               IX1054.2
003900 ENVIRONMENT DIVISION.                                            IX1054.2
004000 CONFIGURATION SECTION.                                           IX1054.2
004100 SOURCE-COMPUTER.                                                 IX1054.2
004200     XXXXX082.                                                    IX1054.2
004300 OBJECT-COMPUTER.                                                 IX1054.2
004400     XXXXX083.                                                    IX1054.2
004500 INPUT-OUTPUT SECTION.                                            IX1054.2
004600 FILE-CONTROL.                                                    IX1054.2
004700     SELECT PRINT-FILE ASSIGN TO                                  IX1054.2
004800     XXXXX055.                                                    IX1054.2
004900     SELECT IX-FR1 ASSIGN TO                                      IX1054.2
005000     XXXXX024                                                     IX1054.2
005100             ORGANIZATION IS INDEXED                              IX1054.2
005200             ACCESS  MODE IS RANDOM                               IX1054.2
005300              RECORD KEY IS IX-FR1-KEY.                           IX1054.2
005400     SELECT IX-FR2 ASSIGN TO                                      IX1054.2
005500     XXXXX025                                                     IX1054.2
005600             ORGANIZATION IS INDEXED                              IX1054.2
005700             ACCESS  MODE IS RANDOM                               IX1054.2
005800              RECORD KEY IS IX-FR2-KEY.                           IX1054.2
005900     SELECT IX-FR3 ASSIGN TO                                      IX1054.2
006000     XXXXX026                                                     IX1054.2
006100             ORGANIZATION IS INDEXED                              IX1054.2
006200             ACCESS  MODE IS RANDOM                               IX1054.2
006300              RECORD KEY IS IX-FR3-KEY.                           IX1054.2
006400 I-O-CONTROL.                                                     IX1054.2
006500     SAME IX-FR2 IX-FR3.                                          IX1054.2
006600 DATA DIVISION.                                                   IX1054.2
006700 FILE SECTION.                                                    IX1054.2
006800 FD  PRINT-FILE.                                                  IX1054.2
006900 01  PRINT-REC PICTURE X(120).                                    IX1054.2
007000 01  DUMMY-RECORD PICTURE X(120).                                 IX1054.2
007100 FD  IX-FR1                                                       IX1054.2
007200C    LABEL RECORDS ARE STANDARD                                   IX1054.2
007300C    DATA RECORDS ARE GRP-1SEQ-RECORD-1A  GRP-1SEQ-RECORD-1B      IX1054.2
007400     RECORD CONTAINS 56 TO 100 CHARACTERS.                        IX1054.2
007500 01  GRP-1SEQ-RECORD-1A.                                          IX1054.2
007600     02  IX-FR1-KEY              PICTURE X(8).                    IX1054.2
007700     02  FILLER-1A               PICTURE X(48).                   IX1054.2
007800 01  GRP-1SEQ-RECORD-1B.                                          IX1054.2
007900     02  FILLER-1B               PICTURE X(56).                   IX1054.2
008000     02  LONG-REC-1B.                                             IX1054.2
008100         03  FILLER              PICTURE X(15).                   IX1054.2
008200         03  REC-NUMBER-1B       PICTURE XXX.                     IX1054.2
008300         03  FILLER              PICTURE X(26).                   IX1054.2
008400 FD  IX-FR2                                                       IX1054.2
008500C    DATA RECORDS  GRP-1SEQ-RECORD-2A  GRP-1SEQ-RECORD-2B         IX1054.2
008600C    LABEL RECORDS ARE STANDARD                                   IX1054.2
008700     RECORD CONTAINS 56 TO 101 CHARACTERS.                        IX1054.2
008800 01  GRP-1SEQ-RECORD-2A.                                          IX1054.2
008900     02  IX-FR2-KEY              PICTURE X(8).                    IX1054.2
009000     02  FILLER-2A               PICTURE X(48).                   IX1054.2
009100 01  GRP-1SEQ-RECORD-2B.                                          IX1054.2
009200     02  FILLER-2B               PICTURE X(56).                   IX1054.2
009300     02  LONG-REC-2B.                                             IX1054.2
009400         03  FILLER              PICTURE X(15).                   IX1054.2
009500         03  REC-NUMBER-2B       PICTURE XXX.                     IX1054.2
009600         03  FILLER              PICTURE X(27).                   IX1054.2
009700 FD  IX-FR3                                                       IX1054.2
009800C    LABEL RECORD STANDARD                                        IX1054.2
009900C    DATA RECORD  GRP-1SEQ-RECORD-3A  GRP-1SEQ-RECORD-3B          IX1054.2
010000     BLOCK 3 RECORDS                                              IX1054.2
010100     RECORD CONTAINS 56 TO 102 CHARACTERS.                        IX1054.2
010200 01  GRP-1SEQ-RECORD-3A.                                          IX1054.2
010300     02  IX-FR3-KEY              PICTURE X(8).                    IX1054.2
010400     02  FILLER-3A               PICTURE X(48).                   IX1054.2
010500 01  GRP-1SEQ-RECORD-3B.                                          IX1054.2
010600     02  FILLER-3B               PICTURE X(56).                   IX1054.2
010700     02  LONG-REC-3B.                                             IX1054.2
010800         03  FILLER              PICTURE X(15).                   IX1054.2
010900         03  REC-NUMBER-3B       PICTURE XXX.                     IX1054.2
011000     02  FILLER                  PICTURE X(28).                   IX1054.2
011100 WORKING-STORAGE SECTION.                                         IX1054.2
011200 01  SHORT-SW                    PICTURE 9 VALUE ZERO.            IX1054.2
011300 01  RECORD-BUILD.                                                IX1054.2
011400     02  KEY-BUILD.                                               IX1054.2
011500         03  KEY-NAME            PICTURE X(3) VALUE "KEY".        IX1054.2
011600         03  KEY-VALUE           PICTURE 9(5) VALUE ZERO.         IX1054.2
011700     02  FILLER                  PICTURE X(6) VALUE " FILE ".     IX1054.2
011800     02  FILE-NO                 PICTURE 99 VALUE ZERO.           IX1054.2
011900     02  FILLER                  PICTURE X(14)  VALUE             IX1054.2
012000     "  RECORD TYPE ".                                            IX1054.2
012100     02  RECORD-LONG-OR-SHORT    PICTURE X(5) VALUE "SHORT".      IX1054.2
012200     02  FILLER                  PICTURE X(21) VALUE SPACE.       IX1054.2
012300     02  RECORD-LONG-ONLY.                                        IX1054.2
012400         03  FILLER              PICTURE X(15)  VALUE             IX1054.2
012500         " RECORD NUMBER ".                                       IX1054.2
012600         03  THREE-POS-NUM       PICTURE 999 VALUE ZERO.          IX1054.2
012700         03  FILLER-LONG         PICTURE X(28)  VALUE             IX1054.2
012800         " AREA USED FOR LONG RECORD  ".                          IX1054.2
012900 01  FILE-RECORD-INFORMATION-REC.                                 IX1054.2
013000     03 FILE-RECORD-INFO-SKELETON.                                IX1054.2
013100        05 FILLER                 PICTURE X(48)       VALUE       IX1054.2
013200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  IX1054.2
013300        05 FILLER                 PICTURE X(46)       VALUE       IX1054.2
013400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    IX1054.2
013500        05 FILLER                 PICTURE X(26)       VALUE       IX1054.2
013600             ",LFIL=000000,ORG=  ,LBLR= ".                        IX1054.2
013700        05 FILLER                 PICTURE X(37)       VALUE       IX1054.2
013800             ",RECKEY=                             ".             IX1054.2
013900        05 FILLER                 PICTURE X(38)       VALUE       IX1054.2
014000             ",ALTKEY1=                             ".            IX1054.2
014100        05 FILLER                 PICTURE X(38)       VALUE       IX1054.2
014200             ",ALTKEY2=                             ".            IX1054.2
014300        05 FILLER                 PICTURE X(7)        VALUE SPACE.IX1054.2
014400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              IX1054.2
014500        05 FILE-RECORD-INFO-P1-120.                               IX1054.2
014600           07 FILLER              PIC X(5).                       IX1054.2
014700           07 XFILE-NAME           PIC X(6).                      IX1054.2
014800           07 FILLER              PIC X(8).                       IX1054.2
014900           07 XRECORD-NAME         PIC X(6).                      IX1054.2
015000           07 FILLER              PIC X(1).                       IX1054.2
015100           07 REELUNIT-NUMBER     PIC 9(1).                       IX1054.2
015200           07 FILLER              PIC X(7).                       IX1054.2
015300           07 XRECORD-NUMBER       PIC 9(6).                      IX1054.2
015400           07 FILLER              PIC X(6).                       IX1054.2
015500           07 UPDATE-NUMBER       PIC 9(2).                       IX1054.2
015600           07 FILLER              PIC X(5).                       IX1054.2
015700           07 ODO-NUMBER          PIC 9(4).                       IX1054.2
015800           07 FILLER              PIC X(5).                       IX1054.2
015900           07 XPROGRAM-NAME        PIC X(5).                      IX1054.2
016000           07 FILLER              PIC X(7).                       IX1054.2
016100           07 XRECORD-LENGTH       PIC 9(6).                      IX1054.2
016200           07 FILLER              PIC X(7).                       IX1054.2
016300           07 CHARS-OR-RECORDS    PIC X(2).                       IX1054.2
016400           07 FILLER              PIC X(1).                       IX1054.2
016500           07 XBLOCK-SIZE          PIC 9(4).                      IX1054.2
016600           07 FILLER              PIC X(6).                       IX1054.2
016700           07 RECORDS-IN-FILE     PIC 9(6).                       IX1054.2
016800           07 FILLER              PIC X(5).                       IX1054.2
016900           07 XFILE-ORGANIZATION   PIC X(2).                      IX1054.2
017000           07 FILLER              PIC X(6).                       IX1054.2
017100           07 XLABEL-TYPE          PIC X(1).                      IX1054.2
017200        05 FILE-RECORD-INFO-P121-240.                             IX1054.2
017300           07 FILLER              PIC X(8).                       IX1054.2
017400           07 XRECORD-KEY          PIC X(29).                     IX1054.2
017500           07 FILLER              PIC X(9).                       IX1054.2
017600           07 ALTERNATE-KEY1      PIC X(29).                      IX1054.2
017700           07 FILLER              PIC X(9).                       IX1054.2
017800           07 ALTERNATE-KEY2      PIC X(29).                      IX1054.2
017900           07 FILLER              PIC X(7).                       IX1054.2
018000 01  TEST-RESULTS.                                                IX1054.2
018100     02 FILLER                   PIC X      VALUE SPACE.          IX1054.2
018200     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX1054.2
018300     02 FILLER                   PIC X      VALUE SPACE.          IX1054.2
018400     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX1054.2
018500     02 FILLER                   PIC X      VALUE SPACE.          IX1054.2
018600     02  PAR-NAME.                                                IX1054.2
018700       03 FILLER                 PIC X(19)  VALUE SPACE.          IX1054.2
018800       03  PARDOT-X              PIC X      VALUE SPACE.          IX1054.2
018900       03 DOTVALUE               PIC 99     VALUE ZERO.           IX1054.2
019000     02 FILLER                   PIC X(8)   VALUE SPACE.          IX1054.2
019100     02 RE-MARK                  PIC X(61).                       IX1054.2
019200 01  TEST-COMPUTED.                                               IX1054.2
019300     02 FILLER                   PIC X(30)  VALUE SPACE.          IX1054.2
019400     02 FILLER                   PIC X(17)  VALUE                 IX1054.2
019500            "       COMPUTED=".                                   IX1054.2
019600     02 COMPUTED-X.                                               IX1054.2
019700     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX1054.2
019800     03 COMPUTED-N               REDEFINES COMPUTED-A             IX1054.2
019900                                 PIC -9(9).9(9).                  IX1054.2
020000     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX1054.2
020100     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX1054.2
020200     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX1054.2
020300     03       CM-18V0 REDEFINES COMPUTED-A.                       IX1054.2
020400         04 COMPUTED-18V0                    PIC -9(18).          IX1054.2
020500         04 FILLER                           PIC X.               IX1054.2
020600     03 FILLER PIC X(50) VALUE SPACE.                             IX1054.2
020700 01  TEST-CORRECT.                                                IX1054.2
020800     02 FILLER PIC X(30) VALUE SPACE.                             IX1054.2
020900     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX1054.2
021000     02 CORRECT-X.                                                IX1054.2
021100     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX1054.2
021200     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX1054.2
021300     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX1054.2
021400     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX1054.2
021500     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX1054.2
021600     03      CR-18V0 REDEFINES CORRECT-A.                         IX1054.2
021700         04 CORRECT-18V0                     PIC -9(18).          IX1054.2
021800         04 FILLER                           PIC X.               IX1054.2
021900     03 FILLER PIC X(2) VALUE SPACE.                              IX1054.2
022000     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX1054.2
022100 01  CCVS-C-1.                                                    IX1054.2
022200     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX1054.2
022300-    "SS  PARAGRAPH-NAME                                          IX1054.2
022400-    "       REMARKS".                                            IX1054.2
022500     02 FILLER                     PIC X(20)    VALUE SPACE.      IX1054.2
022600 01  CCVS-C-2.                                                    IX1054.2
022700     02 FILLER                     PIC X        VALUE SPACE.      IX1054.2
022800     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX1054.2
022900     02 FILLER                     PIC X(15)    VALUE SPACE.      IX1054.2
023000     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX1054.2
023100     02 FILLER                     PIC X(94)    VALUE SPACE.      IX1054.2
023200 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX1054.2
023300 01  REC-CT                        PIC 99       VALUE ZERO.       IX1054.2
023400 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX1054.2
023500 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX1054.2
023600 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX1054.2
023700 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX1054.2
023800 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX1054.2
023900 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX1054.2
024000 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX1054.2
024100 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX1054.2
024200 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX1054.2
024300 01  CCVS-H-1.                                                    IX1054.2
024400     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1054.2
024500     02  FILLER                    PIC X(42)    VALUE             IX1054.2
024600     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX1054.2
024700     02  FILLER                    PIC X(39)    VALUE SPACES.     IX1054.2
024800 01  CCVS-H-2A.                                                   IX1054.2
024900   02  FILLER                        PIC X(40)  VALUE SPACE.      IX1054.2
025000   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX1054.2
025100   02  FILLER                        PIC XXXX   VALUE             IX1054.2
025200     "4.2 ".                                                      IX1054.2
025300   02  FILLER                        PIC X(28)  VALUE             IX1054.2
025400            " COPY - NOT FOR DISTRIBUTION".                       IX1054.2
025500   02  FILLER                        PIC X(41)  VALUE SPACE.      IX1054.2
025600                                                                  IX1054.2
025700 01  CCVS-H-2B.                                                   IX1054.2
025800   02  FILLER                        PIC X(15)  VALUE             IX1054.2
025900            "TEST RESULT OF ".                                    IX1054.2
026000   02  TEST-ID                       PIC X(9).                    IX1054.2
026100   02  FILLER                        PIC X(4)   VALUE             IX1054.2
026200            " IN ".                                               IX1054.2
026300   02  FILLER                        PIC X(12)  VALUE             IX1054.2
026400     " HIGH       ".                                              IX1054.2
026500   02  FILLER                        PIC X(22)  VALUE             IX1054.2
026600            " LEVEL VALIDATION FOR ".                             IX1054.2
026700   02  FILLER                        PIC X(58)  VALUE             IX1054.2
026800     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1054.2
026900 01  CCVS-H-3.                                                    IX1054.2
027000     02  FILLER                      PIC X(34)  VALUE             IX1054.2
027100            " FOR OFFICIAL USE ONLY    ".                         IX1054.2
027200     02  FILLER                      PIC X(58)  VALUE             IX1054.2
027300     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX1054.2
027400     02  FILLER                      PIC X(28)  VALUE             IX1054.2
027500            "  COPYRIGHT   1985 ".                                IX1054.2
027600 01  CCVS-E-1.                                                    IX1054.2
027700     02 FILLER                       PIC X(52)  VALUE SPACE.      IX1054.2
027800     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX1054.2
027900     02 ID-AGAIN                     PIC X(9).                    IX1054.2
028000     02 FILLER                       PIC X(45)  VALUE SPACES.     IX1054.2
028100 01  CCVS-E-2.                                                    IX1054.2
028200     02  FILLER                      PIC X(31)  VALUE SPACE.      IX1054.2
028300     02  FILLER                      PIC X(21)  VALUE SPACE.      IX1054.2
028400     02 CCVS-E-2-2.                                               IX1054.2
028500         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX1054.2
028600         03 FILLER                   PIC X      VALUE SPACE.      IX1054.2
028700         03 ENDER-DESC               PIC X(44)  VALUE             IX1054.2
028800            "ERRORS ENCOUNTERED".                                 IX1054.2
028900 01  CCVS-E-3.                                                    IX1054.2
029000     02  FILLER                      PIC X(22)  VALUE             IX1054.2
029100            " FOR OFFICIAL USE ONLY".                             IX1054.2
029200     02  FILLER                      PIC X(12)  VALUE SPACE.      IX1054.2
029300     02  FILLER                      PIC X(58)  VALUE             IX1054.2
029400     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX1054.2
029500     02  FILLER                      PIC X(13)  VALUE SPACE.      IX1054.2
029600     02 FILLER                       PIC X(15)  VALUE             IX1054.2
029700             " COPYRIGHT 1985".                                   IX1054.2
029800 01  CCVS-E-4.                                                    IX1054.2
029900     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX1054.2
030000     02 FILLER                       PIC X(4)   VALUE " OF ".     IX1054.2
030100     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX1054.2
030200     02 FILLER                       PIC X(40)  VALUE             IX1054.2
030300      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX1054.2
030400 01  XXINFO.                                                      IX1054.2
030500     02 FILLER                       PIC X(19)  VALUE             IX1054.2
030600            "*** INFORMATION ***".                                IX1054.2
030700     02 INFO-TEXT.                                                IX1054.2
030800       04 FILLER                     PIC X(8)   VALUE SPACE.      IX1054.2
030900       04 XXCOMPUTED                 PIC X(20).                   IX1054.2
031000       04 FILLER                     PIC X(5)   VALUE SPACE.      IX1054.2
031100       04 XXCORRECT                  PIC X(20).                   IX1054.2
031200     02 INF-ANSI-REFERENCE           PIC X(48).                   IX1054.2
031300 01  HYPHEN-LINE.                                                 IX1054.2
031400     02 FILLER  PIC IS X VALUE IS SPACE.                          IX1054.2
031500     02 FILLER  PIC IS X(65)    VALUE IS "************************IX1054.2
031600-    "*****************************************".                 IX1054.2
031700     02 FILLER  PIC IS X(54)    VALUE IS "************************IX1054.2
031800-    "******************************".                            IX1054.2
031900 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX1054.2
032000     "IX105A".                                                    IX1054.2
032100 PROCEDURE DIVISION.                                              IX1054.2
032200 CCVS1 SECTION.                                                   IX1054.2
032300 OPEN-FILES.                                                      IX1054.2
032400     OPEN    OUTPUT PRINT-FILE.                                   IX1054.2
032500     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX1054.2
032600     MOVE    SPACE TO TEST-RESULTS.                               IX1054.2
032700     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX1054.2
032800     MOVE    ZERO TO REC-SKL-SUB.                                 IX1054.2
032900     PERFORM CCVS-INIT-FILE 9 TIMES.                              IX1054.2
033000 CCVS-INIT-FILE.                                                  IX1054.2
033100     ADD     1 TO REC-SKL-SUB.                                    IX1054.2
033200     MOVE    FILE-RECORD-INFO-SKELETON                            IX1054.2
033300          TO FILE-RECORD-INFO (REC-SKL-SUB).                      IX1054.2
033400 CCVS-INIT-EXIT.                                                  IX1054.2
033500     GO TO CCVS1-EXIT.                                            IX1054.2
033600 CLOSE-FILES.                                                     IX1054.2
033700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX1054.2
033800 TERMINATE-CCVS.                                                  IX1054.2
033900     STOP     RUN.                                                IX1054.2
034000 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX1054.2
034100 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX1054.2
034200 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX1054.2
034300 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX1054.2
034400     MOVE "****TEST DELETED****" TO RE-MARK.                      IX1054.2
034500 PRINT-DETAIL.                                                    IX1054.2
034600     IF REC-CT NOT EQUAL TO ZERO                                  IX1054.2
034700             MOVE "." TO PARDOT-X                                 IX1054.2
034800             MOVE REC-CT TO DOTVALUE.                             IX1054.2
034900     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX1054.2
035000     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX1054.2
035100        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX1054.2
035200          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX1054.2
035300     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX1054.2
035400     MOVE SPACE TO CORRECT-X.                                     IX1054.2
035500     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX1054.2
035600     MOVE     SPACE TO RE-MARK.                                   IX1054.2
035700 HEAD-ROUTINE.                                                    IX1054.2
035800     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1054.2
035900     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX1054.2
036000     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1054.2
036100     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX1054.2
036200 COLUMN-NAMES-ROUTINE.                                            IX1054.2
036300     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1054.2
036400     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1054.2
036500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX1054.2
036600 END-ROUTINE.                                                     IX1054.2
036700     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX1054.2
036800 END-RTN-EXIT.                                                    IX1054.2
036900     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1054.2
037000 END-ROUTINE-1.                                                   IX1054.2
037100      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX1054.2
037200      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX1054.2
037300      ADD PASS-COUNTER TO ERROR-HOLD.                             IX1054.2
037400*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX1054.2
037500      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX1054.2
037600      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX1054.2
037700      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX1054.2
037800      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX1054.2
037900  END-ROUTINE-12.                                                 IX1054.2
038000      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX1054.2
038100     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX1054.2
038200         MOVE "NO " TO ERROR-TOTAL                                IX1054.2
038300         ELSE                                                     IX1054.2
038400         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX1054.2
038500     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX1054.2
038600     PERFORM WRITE-LINE.                                          IX1054.2
038700 END-ROUTINE-13.                                                  IX1054.2
038800     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX1054.2
038900         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX1054.2
039000         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX1054.2
039100     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX1054.2
039200     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1054.2
039300      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX1054.2
039400          MOVE "NO " TO ERROR-TOTAL                               IX1054.2
039500      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX1054.2
039600      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX1054.2
039700      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX1054.2
039800     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX1054.2
039900 WRITE-LINE.                                                      IX1054.2
040000     ADD 1 TO RECORD-COUNT.                                       IX1054.2
040100*    IF RECORD-COUNT GREATER 42                                   IX1054.2
040200*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX1054.2
040300*        MOVE SPACE TO DUMMY-RECORD                               IX1054.2
040400*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX1054.2
040500*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1054.2
040600*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX1054.2
040700*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1054.2
040800*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX1054.2
040900*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX1054.2
041000*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX1054.2
041100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX1054.2
041200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX1054.2
041300*        MOVE ZERO TO RECORD-COUNT.                               IX1054.2
041400     PERFORM WRT-LN.                                              IX1054.2
041500 WRT-LN.                                                          IX1054.2
041600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX1054.2
041700     MOVE SPACE TO DUMMY-RECORD.                                  IX1054.2
041800 BLANK-LINE-PRINT.                                                IX1054.2
041900     PERFORM WRT-LN.                                              IX1054.2
042000 FAIL-ROUTINE.                                                    IX1054.2
042100     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX1054.2
042200            GO TO   FAIL-ROUTINE-WRITE.                           IX1054.2
042300     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX1054.2
042400     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1054.2
042500     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX1054.2
042600     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1054.2
042700     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1054.2
042800     GO TO  FAIL-ROUTINE-EX.                                      IX1054.2
042900 FAIL-ROUTINE-WRITE.                                              IX1054.2
043000     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX1054.2
043100     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX1054.2
043200     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX1054.2
043300     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX1054.2
043400 FAIL-ROUTINE-EX. EXIT.                                           IX1054.2
043500 BAIL-OUT.                                                        IX1054.2
043600     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX1054.2
043700     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX1054.2
043800 BAIL-OUT-WRITE.                                                  IX1054.2
043900     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX1054.2
044000     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX1054.2
044100     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX1054.2
044200     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX1054.2
044300 BAIL-OUT-EX. EXIT.                                               IX1054.2
044400 CCVS1-EXIT.                                                      IX1054.2
044500     EXIT.                                                        IX1054.2
044600 SECT-RC-02-001 SECTION.                                          IX1054.2
044700 WRITE-INIT-GF-01.                                                IX1054.2
044800     MOVE "WRITE SHORT & LONG " TO FEATURE.                       IX1054.2
044900     MOVE "WRITE-TEST-GF-01" TO PAR-NAME.                         IX1054.2
045000 WRITE-TEST-GF-01.                                                IX1054.2
045100*    CREATE AN INDEXED FILE OF 180 RECORDS.  THE RECORDS SIZE IS  IX1054.2
045200*    VARIABLE LENGTH (056 TO 100 CHARACTERS).                     IX1054.2
045300     MOVE 1 TO FILE-NO.                                           IX1054.2
045400     OPEN OUTPUT IX-FR1.                                          IX1054.2
045500     PERFORM WRITE-TEST-GF-01-SHORT-REC 20 TIMES.                 IX1054.2
045600     PERFORM WRITE-TEST-GF-01-LONG-REC 45 TIMES.                  IX1054.2
045700     PERFORM WRITE-TEST-GF-01-SHORT-REC 50 TIMES.                 IX1054.2
045800     PERFORM WRITE-TEST-GF-01-LONG-REC 29 TIMES.                  IX1054.2
045900     PERFORM WRITE-TEST-GF-01-SHORT-REC 35 TIMES.                 IX1054.2
046000     PERFORM WRITE-TEST-GF-01-LONG-REC.                           IX1054.2
046100     IF THREE-POS-NUM EQUAL 180                                   IX1054.2
046200        PERFORM PASS                                              IX1054.2
046300        MOVE "FILE IX-FR1 CREATED (180 RECORDS)" TO RE-MARK       IX1054.2
046400        GO TO WRITE-TEST-GF-01-WRITE.                             IX1054.2
046500     MOVE "WRONG NUMBER OF RECORDS WRITTEN" TO RE-MARK.           IX1054.2
046600     GO TO WRITE-TEST-GF-01-FAIL.                                 IX1054.2
046700 WRITE-DELETE-GF-01.                                              IX1054.2
046800     PERFORM DE-LETE.                                             IX1054.2
046900*    NOTE IX-FR1 IS NOT CREATED  SO SKIP TO WRITE-TEST-5.         IX1054.2
047000     PERFORM PRINT-DETAIL.                                        IX1054.2
047100     GO TO WRITE-INIT-GF-02.                                      IX1054.2
047200 WRITE-TEST-GF-01-LONG-REC.                                       IX1054.2
047300     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
047400     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        IX1054.2
047500     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-1B.                     IX1054.2
047600     WRITE GRP-1SEQ-RECORD-1B INVALID KEY                         IX1054.2
047700        MOVE "INVALID KEY ON WRITE (LONG)" TO RE-MARK             IX1054.2
047800        GO TO WRITE-TEST-GF-01-FAIL.                              IX1054.2
047900 WRITE-TEST-GF-01-SHORT-REC.                                      IX1054.2
048000     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
048100     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        IX1054.2
048200     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-1B.                     IX1054.2
048300     WRITE GRP-1SEQ-RECORD-1A INVALID KEY                         IX1054.2
048400        MOVE "INVALID KEY ON WRITE (SHORT)" TO RE-MARK            IX1054.2
048500        GO TO WRITE-TEST-GF-01-FAIL.                              IX1054.2
048600 WRITE-TEST-GF-01-FAIL.                                           IX1054.2
048700     MOVE "IX-41 4.9.2 WRONG NUMBER OF RECORDS WRITTEN" TO RE-MARKIX1054.2
048800     PERFORM FAIL.                                                IX1054.2
048900     MOVE 180 TO CORRECT-18V0.                                    IX1054.2
049000     MOVE THREE-POS-NUM TO COMPUTED-18V0.                         IX1054.2
049100 WRITE-TEST-GF-01-WRITE.                                          IX1054.2
049200     MOVE SPACE TO GRP-1SEQ-RECORD-1B.                            IX1054.2
049300     PERFORM PRINT-DETAIL.                                        IX1054.2
049400     CLOSE IX-FR1.                                                IX1054.2
049500 READ-INIT-F2-01.                                                 IX1054.2
049600     MOVE "READ TO VERIFY     " TO FEATURE.                       IX1054.2
049700     MOVE "READ-TEST-F2-01    " TO PAR-NAME.                      IX1054.2
049800*    VERIFY NUMBER OF RECORDS IN FILE.                            IX1054.2
049900     OPEN INPUT IX-FR1.                                           IX1054.2
050000     MOVE 1 TO KEY-VALUE.                                         IX1054.2
050100     MOVE KEY-BUILD TO IX-FR1-KEY.                                IX1054.2
050200 READ-TEST-F2-01.                                                 IX1054.2
050300     READ IX-FR1 INVALID KEY                                      IX1054.2
050400        GO TO COMPARE-FOR-TEST-F2-01.                             IX1054.2
050500     ADD 1 TO KEY-VALUE.                                          IX1054.2
050600     MOVE KEY-BUILD TO IX-FR1-KEY.                                IX1054.2
050700     IF KEY-VALUE GREATER THAN 181                                IX1054.2
050800        GO TO READ-FAIL-F2-01.                                    IX1054.2
050900     GO TO READ-TEST-F2-01.                                       IX1054.2
051000 COMPARE-FOR-TEST-F2-01.                                          IX1054.2
051100     IF KEY-VALUE EQUAL 181                                       IX1054.2
051200         PERFORM PASS                                             IX1054.2
051300         MOVE "180 RECORDS VERIFIED" TO RE-MARK                   IX1054.2
051400         GO TO READ-WRITE-F2-01.                                  IX1054.2
051500 READ-FAIL-F2-01.                                                 IX1054.2
051600     MOVE "IX-28 4.5.2                               " TO RE-MARK.IX1054.2
051700     PERFORM FAIL.                                                IX1054.2
051800     MOVE 180 TO CORRECT-18V0.                                    IX1054.2
051900     SUBTRACT 1 FROM KEY-VALUE.                                   IX1054.2
052000     MOVE KEY-VALUE TO COMPUTED-18V0.                             IX1054.2
052100     MOVE "INCORRECT NUMBER OF RECORDS" TO RE-MARK.               IX1054.2
052200 READ-WRITE-F2-01.                                                IX1054.2
052300     MOVE "READ-TEST-F2-01" TO PAR-NAME.                          IX1054.2
052400     PERFORM PRINT-DETAIL.                                        IX1054.2
052500     CLOSE IX-FR1.                                                IX1054.2
052600 READ-INIT-F2-02.                                                 IX1054.2
052700     OPEN INPUT IX-FR1.                                           IX1054.2
052800     MOVE 10 TO KEY-VALUE.                                        IX1054.2
052900     MOVE KEY-BUILD TO IX-FR1-KEY.                                IX1054.2
053000     MOVE "READ  SHORT RECORDS" TO FEATURE.                       IX1054.2
053100     MOVE "READ-TEST-GF-02    " TO PAR-NAME.                      IX1054.2
053200 READ-TEST-F2-02.                                                 IX1054.2
053300*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
053400*    SHORT RECORD.                                                IX1054.2
053500     READ IX-FR1 INVALID KEY                                      IX1054.2
053600         PERFORM FAIL                                             IX1054.2
053700         MOVE "KEY00010" TO CORRECT-A                             IX1054.2
053800         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
053900         MOVE "INVALID KEY IX-FR1 IX-28 4.5.2" TO RE-MARK         IX1054.2
054000         GO TO READ-TEST-F2-02-WRITE.                             IX1054.2
054100*    NOTE  *** IF REC-NUMBER-1B CONTAINS THE RECORD NUMBER        IX1054.2
054200*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         IX1054.2
054300*          OF VARIABLE LENGTH RECORDS.                            IX1054.2
054400*    NOTE  CHECK LENGTH OF RECORD 10.                             IX1054.2
054500 COMPARE-FOR-TEST-F2-02.                                          IX1054.2
054600     IF REC-NUMBER-1B EQUAL TO "010"                              IX1054.2
054700        MOVE "LONG RECORD CREATED" TO COMPUTED-A                  IX1054.2
054800     ELSE MOVE "SHORT RECORD CREATED" TO COMPUTED-A               IX1054.2
054900          MOVE 1 TO SHORT-SW.                                     IX1054.2
055000     MOVE "EXPECT SHORT RECORD" TO CORRECT-A.                     IX1054.2
055100     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     IX1054.2
055200 READ-TEST-F2-02-WRITE.                                           IX1054.2
055300     PERFORM PRINT-DETAIL.                                        IX1054.2
055400     CLOSE IX-FR1.                                                IX1054.2
055500 READ-INIT-F2-03.                                                 IX1054.2
055600*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
055700*    LONG RECORD.                                                 IX1054.2
055800     OPEN INPUT IX-FR1.                                           IX1054.2
055900     MOVE 144 TO KEY-VALUE.                                       IX1054.2
056000     MOVE KEY-BUILD TO IX-FR1-KEY.                                IX1054.2
056100     MOVE "READ LONG   RECORDS" TO FEATURE.                       IX1054.2
056200     MOVE "READ-TEST-F2-03    " TO PAR-NAME.                      IX1054.2
056300 READ-TEST-F2-03.                                                 IX1054.2
056400     READ IX-FR1 INVALID KEY                                      IX1054.2
056500         PERFORM FAIL                                             IX1054.2
056600         MOVE "KEY00144" TO CORRECT-A                             IX1054.2
056700         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
056800         MOVE "INVAILD KEY IX-FR1" TO RE-MARK                     IX1054.2
056900         GO TO READ-WRITE-F2-03.                                  IX1054.2
057000*    NOTE  *** IF REC-NUMBER-1B CONTAINS THE RECORD NUMBER        IX1054.2
057100*              THEN LONG RECORDS WERE WRITTEN.                    IX1054.2
057200 COMPARE-FOR-TEST-F2-03.                                          IX1054.2
057300     IF REC-NUMBER-1B NOT EQUAL TO "144" GO TO READ-FAIL-F2-03.   IX1054.2
057400     PERFORM PASS.                                                IX1054.2
057500     PERFORM READ-WRITE-F2-03.                                    IX1054.2
057600     MOVE "EXPECT VARIABLE LTH" TO CORRECT-A.                     IX1054.2
057700     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     IX1054.2
057800     IF SHORT-SW EQUAL TO ZERO                                    IX1054.2
057900        MOVE "FIXED RECORD CREATED" TO COMPUTED-A                 IX1054.2
058000        ELSE MOVE "VARIABLE LTH CREATED" TO COMPUTED-A.           IX1054.2
058100     GO TO READ-WRITE-F2-03.                                      IX1054.2
058200 READ-FAIL-F2-03.                                                 IX1054.2
058300     PERFORM FAIL.                                                IX1054.2
058400     MOVE "KEY00144" TO CORRECT-A.                                IX1054.2
058500     MOVE IX-FR1-KEY TO COMPUTED-A.                               IX1054.2
058600     MOVE "WRONG LENGTH OR WRONG RECORD IX-28 4.5.2" TO RE-MARK.  IX1054.2
058700 READ-WRITE-F2-03.                                                IX1054.2
058800     PERFORM PRINT-DETAIL.                                        IX1054.2
058900 READ-TEST-F2-03-EXIT.                                            IX1054.2
059000     CLOSE  IX-FR1.                                               IX1054.2
059100 WRITE-INIT-GF-02.                                                IX1054.2
059200     MOVE "WRITE IX-FS2       " TO FEATURE.                       IX1054.2
059300     MOVE "WRITE-TEST-GF-02    " TO PAR-NAME.                     IX1054.2
059400*    CREATE AN INDEXED FILE OF 101 RECORDS.  THE RECORD SIZE IS   IX1054.2
059500*    VARIABLE LENGTH (056 TO 101 CHARACTERS).                     IX1054.2
059600     MOVE ZERO TO KEY-VALUE  THREE-POS-NUM  SHORT-SW.             IX1054.2
059700     MOVE 2 TO FILE-NO.                                           IX1054.2
059800     OPEN OUTPUT IX-FR2.                                          IX1054.2
059900 WRITE-TEST-GF-02.                                                IX1054.2
060000     PERFORM WRITE-TEST-GF-02-SHORT-REC 11 TIMES.                 IX1054.2
060100     PERFORM WRITE-TEST-GF-02-LONG-REC 29 TIMES.                  IX1054.2
060200     PERFORM WRITE-TEST-GF-02-SHORT-REC 20 TIMES.                 IX1054.2
060300     PERFORM WRITE-TEST-GF-02-LONG-REC 20 TIMES.                  IX1054.2
060400     PERFORM WRITE-TEST-GF-02-SHORT-REC 20 TIMES.                 IX1054.2
060500     PERFORM WRITE-TEST-GF-02-LONG-REC.                           IX1054.2
060600     IF THREE-POS-NUM EQUAL 101                                   IX1054.2
060700        PERFORM PASS                                              IX1054.2
060800        MOVE "FILE IX-FR2 CREATED (101 RECORDS)" TO RE-MARK       IX1054.2
060900        GO TO WRITE-TEST-GF-02-WRITE.                             IX1054.2
061000     MOVE "WRONG NUMBER OF RECORDS WRITTEN IX-41 4.9.2" TO RE-MARKIX1054.2
061100     GO TO WRITE-TEST-GF-02-FAIL.                                 IX1054.2
061200 WRITE-DELETE-GF-02.                                              IX1054.2
061300     PERFORM DE-LETE.                                             IX1054.2
061400*    NOTE IX-FR2 IS NOT CREATED  SO SKIP TO WRITE-TEST-9.         IX1054.2
061500     PERFORM PRINT-DETAIL.                                        IX1054.2
061600     GO TO WRITE-INIT-GF-03.                                      IX1054.2
061700 WRITE-TEST-GF-02-LONG-REC.                                       IX1054.2
061800     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
061900     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        IX1054.2
062000     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-2B.                     IX1054.2
062100     WRITE GRP-1SEQ-RECORD-2B INVALID KEY                         IX1054.2
062200        MOVE "INVALID KEY ON WRITE (LONG)" TO RE-MARK             IX1054.2
062300        GO TO WRITE-TEST-GF-02-FAIL.                              IX1054.2
062400 WRITE-TEST-GF-02-SHORT-REC.                                      IX1054.2
062500     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
062600     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        IX1054.2
062700     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-2B.                     IX1054.2
062800     WRITE GRP-1SEQ-RECORD-2A INVALID KEY                         IX1054.2
062900        MOVE "INVALID KEY ON WRITE (SHORT)" TO RE-MARK            IX1054.2
063000        GO TO WRITE-TEST-GF-02-FAIL.                              IX1054.2
063100 WRITE-TEST-GF-02-FAIL.                                           IX1054.2
063200     PERFORM FAIL.                                                IX1054.2
063300     MOVE 101 TO CORRECT-18V0.                                    IX1054.2
063400     MOVE THREE-POS-NUM TO COMPUTED-18V0.                         IX1054.2
063500 WRITE-TEST-GF-02-WRITE.                                          IX1054.2
063600     MOVE SPACE TO GRP-1SEQ-RECORD-2B.                            IX1054.2
063700     PERFORM PRINT-DETAIL.                                        IX1054.2
063800     CLOSE IX-FR2.                                                IX1054.2
063900 READ-INIT-F2-05.                                                 IX1054.2
064000*    VERIFY NUMBER OF RECORDS IN FILE.                            IX1054.2
064100     MOVE "READ IX-FS2 VERIFY " TO FEATURE.                       IX1054.2
064200     MOVE "READ-TEST-F2-05    " TO PAR-NAME.                      IX1054.2
064300     OPEN INPUT IX-FR2.                                           IX1054.2
064400     MOVE 1 TO KEY-VALUE.                                         IX1054.2
064500     MOVE KEY-BUILD TO IX-FR2-KEY.                                IX1054.2
064600 READ-TEST-F2-05.                                                 IX1054.2
064700     READ IX-FR2 INVALID KEY                                      IX1054.2
064800        GO TO COMPARE-FOR-TEST-F2-05.                             IX1054.2
064900     ADD 1 TO KEY-VALUE.                                          IX1054.2
065000     MOVE KEY-BUILD TO IX-FR2-KEY.                                IX1054.2
065100     IF KEY-VALUE GREATER THAN 102                                IX1054.2
065200        GO TO READ-TEST-F2-05-FAIL.                               IX1054.2
065300     GO TO READ-TEST-F2-05.                                       IX1054.2
065400 COMPARE-FOR-TEST-F2-05.                                          IX1054.2
065500     IF KEY-VALUE EQUAL 102                                       IX1054.2
065600         PERFORM PASS                                             IX1054.2
065700         MOVE "101 RECORDS VERIFIED" TO RE-MARK                   IX1054.2
065800         GO TO READ-TEST-F2-05-WRITE.                             IX1054.2
065900 READ-TEST-F2-05-FAIL.                                            IX1054.2
066000     PERFORM FAIL.                                                IX1054.2
066100     MOVE 101 TO CORRECT-18V0.                                    IX1054.2
066200     SUBTRACT 1 FROM KEY-VALUE.                                   IX1054.2
066300     MOVE KEY-VALUE TO COMPUTED-18V0.                             IX1054.2
066400     MOVE "INCORRECT NUMBER OF RECORDS IX-28  4.5.2" TO RE-MARK.  IX1054.2
066500 READ-TEST-F2-05-WRITE.                                           IX1054.2
066600     PERFORM PRINT-DETAIL.                                        IX1054.2
066700 READ-INIT-F2-06.                                                 IX1054.2
066800*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
066900*    SHORT RECORD.                                                IX1054.2
067000     MOVE "READ               " TO FEATURE.                       IX1054.2
067100     MOVE "READ-TEST-F2-06    " TO PAR-NAME.                      IX1054.2
067200     MOVE 100 TO KEY-VALUE.                                       IX1054.2
067300     MOVE KEY-BUILD TO IX-FR2-KEY.                                IX1054.2
067400 READ-TEST-F2-06.                                                 IX1054.2
067500     READ IX-FR2 INVALID KEY                                      IX1054.2
067600         PERFORM FAIL                                             IX1054.2
067700         MOVE "KEY00100" TO CORRECT-A                             IX1054.2
067800         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
067900         MOVE "INVAILD KEY IX-FR2 IX-28 4.5.2 " TO RE-MARK        IX1054.2
068000         GO TO READ-TEST-F2-06-WRITE.                             IX1054.2
068100*    NOTE  *** IF REC-NUMBER-2B CONTAINS THE RECORD NUMBER        IX1054.2
068200*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         IX1054.2
068300*          OF VARIABLE LENGTH RECORDS.                            IX1054.2
068400*    NOTE  CHECK LENGTH OF RECORD 100.                            IX1054.2
068500 COMPARE-FOR-TEST-F2-06.                                          IX1054.2
068600     IF REC-NUMBER-2B EQUAL TO "100"                              IX1054.2
068700        MOVE "LONG RECORD CREATED" TO COMPUTED-A                  IX1054.2
068800     ELSE MOVE "SHORT RECORD CREATED" TO COMPUTED-A               IX1054.2
068900          MOVE 1 TO SHORT-SW.                                     IX1054.2
069000     MOVE "EXPECT SHORT RECORD" TO CORRECT-A.                     IX1054.2
069100     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     IX1054.2
069200 READ-TEST-F2-06-WRITE.                                           IX1054.2
069300     PERFORM PRINT-DETAIL.                                        IX1054.2
069400 READ-INIT-F2-07.                                                 IX1054.2
069500*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
069600*    LONG RECORD.                                                 IX1054.2
069700     MOVE "READ               " TO FEATURE.                       IX1054.2
069800     MOVE "READ-TEST-F2-07    " TO PAR-NAME.                      IX1054.2
069900     MOVE 12 TO KEY-VALUE.                                        IX1054.2
070000     MOVE KEY-BUILD TO IX-FR2-KEY.                                IX1054.2
070100 READ-TEST-F2-07.                                                 IX1054.2
070200     READ IX-FR2 INVALID KEY                                      IX1054.2
070300         PERFORM FAIL                                             IX1054.2
070400         MOVE "KEY00012" TO CORRECT-A                             IX1054.2
070500         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
070600         MOVE "INVALID KEY IX-FR2 IX-28 4.5.2" TO RE-MARK         IX1054.2
070700         GO TO READ-TEST-F2-07-WRITE.                             IX1054.2
070800*    NOTE  ** IF REC-NUMBER-1B CONTAINS THE RECORD NUMBER         IX1054.2
070900*             THEN LONG RECORDS WERE WRITTEN.                     IX1054.2
071000 COMPARE-FOR-TEST-F2-07.                                          IX1054.2
071100     IF REC-NUMBER-2B EQUAL TO "012"                              IX1054.2
071200        PERFORM PASS                                              IX1054.2
071300        PERFORM READ-TEST-F2-07-WRITE                             IX1054.2
071400        MOVE "EXPECT VARIABLE LTH" TO CORRECT-A                   IX1054.2
071500        MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK                   IX1054.2
071600        IF SHORT-SW EQUAL TO ZERO                                 IX1054.2
071700           MOVE "FIXED RECORD CREATED" TO COMPUTED-A              IX1054.2
071800           GO TO READ-TEST-F2-07-WRITE                            IX1054.2
071900        ELSE MOVE "VARIABLE LTH CREATED" TO COMPUTED-A            IX1054.2
072000             GO TO READ-TEST-F2-07-WRITE.                         IX1054.2
072100     PERFORM FAIL.                                                IX1054.2
072200     MOVE "KEY00012" TO CORRECT-A.                                IX1054.2
072300     MOVE IX-FR2-KEY TO COMPUTED-A.                               IX1054.2
072400     MOVE "WRONG LENGTH OR WRONG RECORD" TO RE-MARK.              IX1054.2
072500 READ-TEST-F2-07-WRITE.                                           IX1054.2
072600     PERFORM PRINT-DETAIL.                                        IX1054.2
072700 READ-TEST-F2-07-EXIT.                                            IX1054.2
072800     CLOSE IX-FR2.                                                IX1054.2
072900 WRITE-INIT-GF-03.                                                IX1054.2
073000*    CREATE AN INDEXED FILE OF 120 RECORDS.  THE RECORD SIZE IS   IX1054.2
073100*    VARIABLE LENGTH (056-102 CHARACTERS).                        IX1054.2
073200     MOVE "WRITE IX-FS3       " TO FEATURE.                       IX1054.2
073300     MOVE "WRITE-TEST-GF-03   " TO PAR-NAME.                      IX1054.2
073400     MOVE ZERO TO KEY-VALUE  THREE-POS-NUM  SHORT-SW.             IX1054.2
073500     MOVE 3 TO FILE-NO.                                           IX1054.2
073600     OPEN OUTPUT IX-FR3.                                          IX1054.2
073700 WRITE-TEST-GF-03.                                                IX1054.2
073800     PERFORM WRITE-TEST-GF-03-SHORT-REC.                          IX1054.2
073900     PERFORM WRITE-TEST-GF-03-LONG-REC 15 TIMES.                  IX1054.2
074000     PERFORM WRITE-TEST-GF-03-SHORT-REC 20 TIMES.                 IX1054.2
074100     PERFORM WRITE-TEST-GF-03-LONG-REC 12 TIMES.                  IX1054.2
074200     PERFORM WRITE-TEST-GF-03-SHORT-REC 23 TIMES.                 IX1054.2
074300     PERFORM WRITE-TEST-GF-03-LONG-REC 23 TIMES.                  IX1054.2
074400     PERFORM WRITE-TEST-GF-03-SHORT-REC 25 TIMES.                 IX1054.2
074500     PERFORM WRITE-TEST-GF-03-LONG-REC.                           IX1054.2
074600     IF THREE-POS-NUM EQUAL 120                                   IX1054.2
074700        PERFORM PASS                                              IX1054.2
074800        MOVE "FILE IX-FR3 CREATED (120 RECORDS)" TO RE-MARK       IX1054.2
074900        GO TO WRITE-TEST-GF-03-WRITE.                             IX1054.2
075000     MOVE "WRONG NUMBER OF RECORDS WRITTEN IX-41 4.9.2" TO RE-MARKIX1054.2
075100     GO TO WRITE-TEST-GF-03-FAIL.                                 IX1054.2
075200 WRITE-DELETE-GF-03.                                              IX1054.2
075300     PERFORM DE-LETE.                                             IX1054.2
075400*    NOTE IX-FR3 IS NOT CREATED  SO SKIP TO END-PARAGRAPH.        IX1054.2
075500     PERFORM PRINT-DETAIL.                                        IX1054.2
075600     GO TO CCVS-EXIT.                                             IX1054.2
075700 WRITE-TEST-GF-03-LONG-REC.                                       IX1054.2
075800     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
075900     MOVE "LONG " TO RECORD-LONG-OR-SHORT.                        IX1054.2
076000     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-3B.                     IX1054.2
076100     WRITE GRP-1SEQ-RECORD-3B INVALID KEY                         IX1054.2
076200        MOVE "INVALID KEY ON WRITE (LONG)" TO RE-MARK             IX1054.2
076300        GO TO WRITE-TEST-GF-03-FAIL.                              IX1054.2
076400 WRITE-TEST-GF-03-SHORT-REC.                                      IX1054.2
076500     ADD 1 TO KEY-VALUE  THREE-POS-NUM.                           IX1054.2
076600     MOVE "SHORT" TO RECORD-LONG-OR-SHORT.                        IX1054.2
076700     MOVE RECORD-BUILD TO GRP-1SEQ-RECORD-3B.                     IX1054.2
076800     WRITE GRP-1SEQ-RECORD-3A INVALID KEY                         IX1054.2
076900        MOVE "INVALID KEY ON WRITE (SHORT)" TO RE-MARK            IX1054.2
077000        GO TO WRITE-TEST-GF-03-FAIL.                              IX1054.2
077100 WRITE-TEST-GF-03-FAIL.                                           IX1054.2
077200     PERFORM FAIL.                                                IX1054.2
077300     MOVE 120 TO CORRECT-18V0.                                    IX1054.2
077400     MOVE THREE-POS-NUM TO COMPUTED-18V0.                         IX1054.2
077500 WRITE-TEST-GF-03-WRITE.                                          IX1054.2
077600     MOVE SPACE TO GRP-1SEQ-RECORD-3B.                            IX1054.2
077700     PERFORM PRINT-DETAIL.                                        IX1054.2
077800     CLOSE IX-FR3.                                                IX1054.2
077900 READ-INIT-F2-08.                                                 IX1054.2
078000*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
078100*    LONG RECORD.                                                 IX1054.2
078200     MOVE "READ IX-FS3 VERIFY " TO FEATURE.                       IX1054.2
078300     MOVE "READ-TEST-F2-08    " TO PAR-NAME.                      IX1054.2
078400     OPEN INPUT IX-FR3.                                           IX1054.2
078500     MOVE 1 TO KEY-VALUE.                                         IX1054.2
078600     MOVE KEY-BUILD TO IX-FR3-KEY.                                IX1054.2
078700 READ-TEST-F2-08.                                                 IX1054.2
078800     READ IX-FR3 INVALID KEY                                      IX1054.2
078900         PERFORM FAIL                                             IX1054.2
079000         MOVE "KEY00016" TO CORRECT-A                             IX1054.2
079100         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
079200         MOVE "INVAILD KEY IX-FR3" TO RE-MARK                     IX1054.2
079300         GO TO READ-TEST-F2-08-WRITE.                             IX1054.2
079400     IF KEY-VALUE NOT EQUAL TO 16                                 IX1054.2
079500        ADD 1 TO KEY-VALUE                                        IX1054.2
079600        MOVE KEY-BUILD TO IX-FR3-KEY                              IX1054.2
079700        GO TO READ-TEST-F2-08.                                    IX1054.2
079800*    NOTE  *** IF REC-NUMBER-3B CONTAINS THE RECORD NUMBER        IX1054.2
079900*              THEN LONG RECORDS WERE WRITTEN.                    IX1054.2
080000 COMPARE-FOR-TEST-F2-08.                                          IX1054.2
080100     IF REC-NUMBER-3B EQUAL TO "016"                              IX1054.2
080200        PERFORM PASS                                              IX1054.2
080300        GO TO READ-TEST-F2-08-WRITE.                              IX1054.2
080400     PERFORM FAIL.                                                IX1054.2
080500     MOVE "KEY00016" TO CORRECT-A.                                IX1054.2
080600     MOVE IX-FR3-KEY TO COMPUTED-A.                               IX1054.2
080700     MOVE "WRONG LENGTH OR WRONG RECORD IX-28 4.5.2" TO RE-MARK.  IX1054.2
080800 READ-TEST-F2-08-WRITE.                                           IX1054.2
080900     PERFORM PRINT-DETAIL.                                        IX1054.2
081000 READ-INIT-09.                                                    IX1054.2
081100*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
081200*    SHORT RECORD.                                                IX1054.2
081300     MOVE "READ  SHORT RECORDS" TO FEATURE.                       IX1054.2
081400     MOVE "READ-TEST-F2-09    " TO PAR-NAME.                      IX1054.2
081500     MOVE 71 TO KEY-VALUE.                                        IX1054.2
081600     MOVE KEY-BUILD TO IX-FR3-KEY.                                IX1054.2
081700 READ-TEST-F2-09.                                                 IX1054.2
081800     READ IX-FR3 INVALID KEY                                      IX1054.2
081900         PERFORM FAIL                                             IX1054.2
082000         MOVE "KEY00071" TO CORRECT-A                             IX1054.2
082100         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
082200         MOVE "INVAILD KEY IX-FR3 IX-28 4.5.2" TO RE-MARK         IX1054.2
082300         GO TO READ-TEST-F2-09-WRITE.                             IX1054.2
082400*    NOTE  *** IF REC-NUMBER-1B CONTAINS THE RECORD NUMBER        IX1054.2
082500*          THEN FIXED LENGTH RECORDS WERE WRITTEN INSTEAD         IX1054.2
082600*          OF VARIABLE LENGTH RECORDS.                            IX1054.2
082700*    NOTE  CHECK LENGTH OF RECORD 71.                             IX1054.2
082800 COMPARE-FOR-TEST-F2-09.                                          IX1054.2
082900     IF REC-NUMBER-3B EQUAL TO "071"                              IX1054.2
083000        MOVE "LONG RECORD CREATED" TO COMPUTED-A                  IX1054.2
083100     ELSE MOVE "SHORT RECORD CREATED" TO COMPUTED-A               IX1054.2
083200          MOVE 1 TO SHORT-SW.                                     IX1054.2
083300     MOVE "EXPECT SHORT RECORD" TO CORRECT-A.                     IX1054.2
083400     MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK.                     IX1054.2
083500 READ-TEST-F2-09-WRITE.                                           IX1054.2
083600     PERFORM PRINT-DETAIL.                                        IX1054.2
083700 READ-INIT-F2-10.                                                 IX1054.2
083800*    VERIFY VARIABLE LENGTH RECORDS WERE CREATED BY TESTING       IX1054.2
083900*    LONG RECORD.                                                 IX1054.2
084000     MOVE "READ LONG   RECORDS" TO FEATURE.                       IX1054.2
084100     MOVE "READ-TEST-F2-10    " TO PAR-NAME.                      IX1054.2
084200     MOVE 120 TO KEY-VALUE.                                       IX1054.2
084300     MOVE KEY-BUILD TO IX-FR3-KEY.                                IX1054.2
084400 READ-TEST-F2-10.                                                 IX1054.2
084500     READ IX-FR3 INVALID KEY                                      IX1054.2
084600         PERFORM FAIL                                             IX1054.2
084700         MOVE "KEY00120" TO CORRECT-A                             IX1054.2
084800         MOVE "***INVALID KEY***" TO COMPUTED-A                   IX1054.2
084900         MOVE "INVAILD KEY IX-FR3 IX-28 4.5.2" TO RE-MARK         IX1054.2
085000         GO TO READ-TEST-F2-10-WRITE.                             IX1054.2
085100*    NOTE  *** IF REC-NUMBER-3B CONTAINS THE RECORD NUMBER        IX1054.2
085200*          THEN LONG RECORDS WERE WITTEN.                         IX1054.2
085300 COMPARE-FOR-TEST-F2-10.                                          IX1054.2
085400     IF REC-NUMBER-3B EQUAL TO "120"                              IX1054.2
085500        PERFORM PASS                                              IX1054.2
085600        PERFORM READ-TEST-F2-10-WRITE                             IX1054.2
085700        MOVE "EXPECT VARIABLE LTH" TO CORRECT-A                   IX1054.2
085800        MOVE "INFO ONLY-SEE PROGRAM" TO RE-MARK                   IX1054.2
085900        IF SHORT-SW EQUAL TO ZERO                                 IX1054.2
086000           MOVE "FIXED RECORD CREATED" TO COMPUTED-A              IX1054.2
086100           GO TO READ-TEST-F2-10-WRITE                            IX1054.2
086200        ELSE MOVE "VARIABLE LTH CREATED" TO COMPUTED-A            IX1054.2
086300             GO TO READ-TEST-F2-10-WRITE.                         IX1054.2
086400     PERFORM FAIL.                                                IX1054.2
086500     MOVE "KEY00120" TO CORRECT-A.                                IX1054.2
086600     MOVE IX-FR3-KEY TO COMPUTED-A.                               IX1054.2
086700     MOVE "WRONG LENGTH OR WRONG RECORD IX-28 4.5.2" TO RE-MARK.  IX1054.2
086800 READ-TEST-F2-10-WRITE.                                           IX1054.2
086900     MOVE "READ-TEST-012" TO PAR-NAME.                            IX1054.2
087000     PERFORM PRINT-DETAIL.                                        IX1054.2
087100 READ-TEST-F2-10-EXIT.                                            IX1054.2
087200     CLOSE IX-FR3.                                                IX1054.2
087300 CCVS-EXIT SECTION.                                               IX1054.2
087400 CCVS-999999.                                                     IX1054.2
087500     GO TO CLOSE-FILES.                                           IX1054.2
      *END-OF,IX105A                                                                  
