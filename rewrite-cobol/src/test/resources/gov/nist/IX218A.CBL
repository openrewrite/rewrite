      *HEADER,COBOL,IX218A                                                            
000100 IDENTIFICATION DIVISION.                                         IX2184.2
000200 PROGRAM-ID.                                                      IX2184.2
000300     IX218A.                                                      IX2184.2
000400****************************************************************  IX2184.2
000500*                                                              *  IX2184.2
000600*    VALIDATION FOR:-                                          *  IX2184.2
000700*                                                              *  IX2184.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2184.2
000900*                                                              *  IX2184.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2184.2
001100*                                                              *  IX2184.2
001200****************************************************************  IX2184.2
001300*                                                                 IX2184.2
001400*  1.  THE  FUNCTION  OF THIS PROGRAM IS TO CHECK THE SEQUENTIAL  IX2184.2
001500*      READ  STATEMENT FOR A NOT EXISTING OPTIONAL INDEXED FILE.  IX2184.2
001600*      THE  READ  STATEMENT WITHOUT AN OPEN STATEMENT FOR SUCH A  IX2184.2
001700*      FILE  MUST CAUSE THE AT END CONDITION AND THE FILE STATUS  IX2184.2
001800*      CODE  10. THIS CODE IS CHECKED HERE. THE NAME OF THE FILE  IX2184.2
001900*      IS  IX-FS1.  THE  AT  END PHRASE IS SPECIFIED. THAT MEANS  IX2184.2
002000*      THAT  ANY USE AFTER STANDARD EXCEPTION PROCEDURE MUST NOT  IX2184.2
002100*      BE EXECUTED.                                               IX2184.2
002200*                                                                 IX2184.2
002300*  2.  ANOTHER  FUNCTION  OF  THIS PROGRAM IS TO CHECK THE START  IX2184.2
002400*      AND  THE  RANDOM  READ  STATEMENTS  FOR  A  NOT  EXISTING  IX2184.2
002500*      OPTIONAL  INDEXED  FILE.  BOTH  ATTEMPTS SHOULD CAUSE THE  IX2184.2
002600*      FILE  STATUS CODE 23. THE INVALID KEY PHRASE IS SPECIFIED  IX2184.2
002700*      AND  THE  USE AFTER STANDARD EXCEPTION PROCEDURE MUST NOT  IX2184.2
002800*      BE EXECUTED. THE NAME OF THE FILE IS IX-FS2.               IX2184.2
002900*                                                                 IX2184.2
003000*                                                                 IX2184.2
003100*                                                                 IX2184.2
003200*       X-CARDS  WHICH MUST BE REPLACED FOR THIS PROGRAM ARE      IX2184.2
003300*                                                                 IX2184.2
003400*             X-24   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2184.2
003500*                    CLAUSE FOR DATA FILE IX-FS1                  IX2184.2
003600*             X-25   INDEXED FILE IMPLEMENTOR-NAME IN ASSGN TO    IX2184.2
003700*                    CLAUSE FOR DATA FILE IX-FS1                  IX2184.2
003800*             X-55   IMPLEMENTOR-NAME FOR SYSTEM PRINTER          IX2184.2
003900*             X-62   IMPLEMENTOR-NAME FOR RAW-DATA (OPTIONAL)     IX2184.2
004000*             X-82   IMPLEMENTOR-NAME FOR SOURCE-COMPUTER         IX2184.2
004100*             X-83   IMPLEMENTOR-NAME FOR OBJECT-COMPUTER         IX2184.2
004200*             X-84   IMPLEMENTOR-NAME FOR PRINT-FILE              IX2184.2
004300*                                                                 IX2184.2
004400*         NOTE:  X-CARDS 44, 45 AND 62          ARE OPTIONAL      IX2184.2
004500*               AND NEED ONLY TO BE PRESENT IF THE COMPILER RE-   IX2184.2
004600*               QUIRES THIS CODE BE AVAILABLE FOR PROPER PROGRAM  IX2184.2
004700*               COMPILATION AND EXECUTION. IF THE VP-ROUTINE IS   IX2184.2
004800*               USED THE  X-CARDS MAY BE AUTOMATICALLY SELECTED   IX2184.2
004900*               FOR INCLUSION IN THE PROGRAM BY SPECIFYING THE    IX2184.2
005000*               APPROPRIATE LETTER IN THE "*OPT" VP-ROUTINE       IX2184.2
005100*               CONTROL CARD. THE LETTER  CORRESPONDS TO A        IX2184.2
005200*               CHARACTER IN POSITION 7 OF THE SOURCE LINE AND    IX2184.2
005300*               THEY ARE AS FOLLOWS                               IX2184.2
005400*                                                                 IX2184.2
005500*                  J  SELECTS X-CARD 44                           IX2184.2
005600*                  J  SELECTS X-CARD 45                           IX2184.2
005700*                  P  SELECTS X-CARD 62                           IX2184.2
005800*                  C  SELECTS X-CARD 84                           IX2184.2
005900*                                                                 IX2184.2
006000******************************************************            IX2184.2
006100 ENVIRONMENT DIVISION.                                            IX2184.2
006200 CONFIGURATION SECTION.                                           IX2184.2
006300 SOURCE-COMPUTER.                                                 IX2184.2
006400     XXXXX082.                                                    IX2184.2
006500 OBJECT-COMPUTER.                                                 IX2184.2
006600     XXXXX083.                                                    IX2184.2
006700 INPUT-OUTPUT SECTION.                                            IX2184.2
006800 FILE-CONTROL.                                                    IX2184.2
006900*    SELECT RAW-DATA   ASSIGN TO                                  IX2184.2
007000*    XXXXX062                                                     IX2184.2
007100*           ORGANIZATION IS INDEXED                               IX2184.2
007200*           ACCESS MODE IS RANDOM                                 IX2184.2
007300*           RECORD KEY IS RAW-DATA-KEY.                           IX2184.2
007400     SELECT PRINT-FILE ASSIGN TO                                  IX2184.2
007500     XXXXX055.                                                    IX2184.2
007600                                                                  IX2184.2
007700     SELECT OPTIONAL   IX-FS1 ASSIGN TO                           IX2184.2
007800*    SELECT            IX-FS1 ASSIGN TO                           IX2184.2
007900     XXXXP024                                                     IX2184.2
008000*    XXXXP044                                                     IX2184.2
008100     ORGANIZATION IS INDEXED                                      IX2184.2
008200     RECORD KEY IS IX-FS1-KEY                                     IX2184.2
008300     ACCESS MODE IS SEQUENTIAL                                    IX2184.2
008400     FILE STATUS IS IX-FS1-STATUS.                                IX2184.2
008500                                                                  IX2184.2
008600     SELECT OPTIONAL   IX-FS2 ASSIGN TO                           IX2184.2
008700*    SELECT            IX-FS2 ASSIGN TO                           IX2184.2
008800     XXXXP025                                                     IX2184.2
008900*    XXXXP045                                                     IX2184.2
009000     ORGANIZATION IS INDEXED                                      IX2184.2
009100     RECORD KEY IS IX-FS2-KEY                                     IX2184.2
009200     ACCESS MODE IS DYNAMIC                                       IX2184.2
009300     FILE STATUS IS IX-FS2-STATUS.                                IX2184.2
009400                                                                  IX2184.2
009500 DATA DIVISION.                                                   IX2184.2
009600 FILE SECTION.                                                    IX2184.2
009700*                                                                 IX2184.2
009800*FD  RAW-DATA.                                                    IX2184.2
009900*                                                                 IX2184.2
010000*01  RAW-DATA-SATZ.                                               IX2184.2
010100*    05  RAW-DATA-KEY        PIC X(6).                            IX2184.2
010200*    05  C-DATE              PIC 9(6).                            IX2184.2
010300*    05  C-TIME              PIC 9(8).                            IX2184.2
010400*    05  C-NO-OF-TESTS       PIC 99.                              IX2184.2
010500*    05  C-OK                PIC 999.                             IX2184.2
010600*    05  C-ALL               PIC 999.                             IX2184.2
010700*    05  C-FAIL              PIC 999.                             IX2184.2
010800*    05  C-DELETED           PIC 999.                             IX2184.2
010900*    05  C-INSPECT           PIC 999.                             IX2184.2
011000*    05  C-NOTE              PIC X(13).                           IX2184.2
011100*    05  C-INDENT            PIC X.                               IX2184.2
011200*    05  C-ABORT             PIC X(8).                            IX2184.2
011300 FD  PRINT-FILE.                                                  IX2184.2
011400 01  PRINT-REC PICTURE X(120).                                    IX2184.2
011500 01  DUMMY-RECORD PICTURE X(120).                                 IX2184.2
011600                                                                  IX2184.2
011700 FD  IX-FS1                                                       IX2184.2
011800C    LABEL RECORD IS STANDARD                                     IX2184.2
011900C    DATA RECORD IS IX-FS1R1-F-G-240                              IX2184.2
012000     BLOCK CONTAINS 1 RECORDS                                     IX2184.2
012100     RECORD CONTAINS 240 CHARACTERS.                              IX2184.2
012200 01  IX-FS1R1-F-G-240.                                            IX2184.2
012300     03 IX-FS1-WRK-120 PIC X(120).                                IX2184.2
012400     03 IX-FS1-GRP-120.                                           IX2184.2
012500     05 FILLER   PIC   X(8).                                      IX2184.2
012600     05 IX-FS1-KEY  PIC X(29).                                    IX2184.2
012700     05 FILLER PIC X(83).                                         IX2184.2
012800                                                                  IX2184.2
012900 FD  IX-FS2                                                       IX2184.2
013000C    LABEL RECORD IS STANDARD                                     IX2184.2
013100C    DATA RECORD IS IX-FS2R1-F-G-240                              IX2184.2
013200     BLOCK CONTAINS 1 RECORDS                                     IX2184.2
013300     RECORD CONTAINS 240 CHARACTERS.                              IX2184.2
013400 01  IX-FS2R1-F-G-240.                                            IX2184.2
013500     03 IX-FS2-WRK-120 PIC X(120).                                IX2184.2
013600     03 IX-FS2-GRP-120.                                           IX2184.2
013700     05 FILLER   PIC   X(8).                                      IX2184.2
013800     05 IX-FS2-KEY  PIC X(29).                                    IX2184.2
013900     05 FILLER PIC X(83).                                         IX2184.2
014000                                                                  IX2184.2
014100 WORKING-STORAGE SECTION.                                         IX2184.2
014200 01  EOF-FLAG                PIC 9 VALUE ZERO.                    IX2184.2
014300                                                                  IX2184.2
014400 01  IX-FS1-STATUS.                                               IX2184.2
014500     05  IX-FS1-STAT1        PIC X.                               IX2184.2
014600     05  IX-FS1-STAT2        PIC X.                               IX2184.2
014700                                                                  IX2184.2
014800 01  IX-FS2-STATUS.                                               IX2184.2
014900     05  IX-FS2-STAT1        PIC X.                               IX2184.2
015000     05  IX-FS2-STAT2        PIC X.                               IX2184.2
015100                                                                  IX2184.2
015200 01  TEST-RESULTS.                                                IX2184.2
015300     02 FILLER                   PIC X      VALUE SPACE.          IX2184.2
015400     02 FEATURE                  PIC X(20)  VALUE SPACE.          IX2184.2
015500     02 FILLER                   PIC X      VALUE SPACE.          IX2184.2
015600     02 P-OR-F                   PIC X(5)   VALUE SPACE.          IX2184.2
015700     02 FILLER                   PIC X      VALUE SPACE.          IX2184.2
015800     02  PAR-NAME.                                                IX2184.2
015900       03 FILLER                 PIC X(19)  VALUE SPACE.          IX2184.2
016000       03  PARDOT-X              PIC X      VALUE SPACE.          IX2184.2
016100       03 DOTVALUE               PIC 99     VALUE ZERO.           IX2184.2
016200     02 FILLER                   PIC X(8)   VALUE SPACE.          IX2184.2
016300     02 RE-MARK                  PIC X(61).                       IX2184.2
016400 01  TEST-COMPUTED.                                               IX2184.2
016500     02 FILLER                   PIC X(30)  VALUE SPACE.          IX2184.2
016600     02 FILLER                   PIC X(17)  VALUE                 IX2184.2
016700            "       COMPUTED=".                                   IX2184.2
016800     02 COMPUTED-X.                                               IX2184.2
016900     03 COMPUTED-A               PIC X(20)  VALUE SPACE.          IX2184.2
017000     03 COMPUTED-N               REDEFINES COMPUTED-A             IX2184.2
017100                                 PIC -9(9).9(9).                  IX2184.2
017200     03 COMPUTED-0V18 REDEFINES COMPUTED-A   PIC -.9(18).         IX2184.2
017300     03 COMPUTED-4V14 REDEFINES COMPUTED-A   PIC -9(4).9(14).     IX2184.2
017400     03 COMPUTED-14V4 REDEFINES COMPUTED-A   PIC -9(14).9(4).     IX2184.2
017500     03       CM-18V0 REDEFINES COMPUTED-A.                       IX2184.2
017600         04 COMPUTED-18V0                    PIC -9(18).          IX2184.2
017700         04 FILLER                           PIC X.               IX2184.2
017800     03 FILLER PIC X(50) VALUE SPACE.                             IX2184.2
017900 01  TEST-CORRECT.                                                IX2184.2
018000     02 FILLER PIC X(30) VALUE SPACE.                             IX2184.2
018100     02 FILLER PIC X(17) VALUE "       CORRECT =".                IX2184.2
018200     02 CORRECT-X.                                                IX2184.2
018300     03 CORRECT-A                  PIC X(20) VALUE SPACE.         IX2184.2
018400     03 CORRECT-N    REDEFINES CORRECT-A     PIC -9(9).9(9).      IX2184.2
018500     03 CORRECT-0V18 REDEFINES CORRECT-A     PIC -.9(18).         IX2184.2
018600     03 CORRECT-4V14 REDEFINES CORRECT-A     PIC -9(4).9(14).     IX2184.2
018700     03 CORRECT-14V4 REDEFINES CORRECT-A     PIC -9(14).9(4).     IX2184.2
018800     03      CR-18V0 REDEFINES CORRECT-A.                         IX2184.2
018900         04 CORRECT-18V0                     PIC -9(18).          IX2184.2
019000         04 FILLER                           PIC X.               IX2184.2
019100     03 FILLER PIC X(2) VALUE SPACE.                              IX2184.2
019200     03 COR-ANSI-REFERENCE             PIC X(48) VALUE SPACE.     IX2184.2
019300 01  CCVS-C-1.                                                    IX2184.2
019400     02 FILLER  PIC IS X(99)    VALUE IS " FEATURE              PAIX2184.2
019500-    "SS  PARAGRAPH-NAME                                          IX2184.2
019600-    "       REMARKS".                                            IX2184.2
019700     02 FILLER                     PIC X(20)    VALUE SPACE.      IX2184.2
019800 01  CCVS-C-2.                                                    IX2184.2
019900     02 FILLER                     PIC X        VALUE SPACE.      IX2184.2
020000     02 FILLER                     PIC X(6)     VALUE "TESTED".   IX2184.2
020100     02 FILLER                     PIC X(15)    VALUE SPACE.      IX2184.2
020200     02 FILLER                     PIC X(4)     VALUE "FAIL".     IX2184.2
020300     02 FILLER                     PIC X(94)    VALUE SPACE.      IX2184.2
020400 01  REC-SKL-SUB                   PIC 9(2)     VALUE ZERO.       IX2184.2
020500 01  REC-CT                        PIC 99       VALUE ZERO.       IX2184.2
020600 01  DELETE-COUNTER                PIC 999      VALUE ZERO.       IX2184.2
020700 01  ERROR-COUNTER                 PIC 999      VALUE ZERO.       IX2184.2
020800 01  INSPECT-COUNTER               PIC 999      VALUE ZERO.       IX2184.2
020900 01  PASS-COUNTER                  PIC 999      VALUE ZERO.       IX2184.2
021000 01  TOTAL-ERROR                   PIC 999      VALUE ZERO.       IX2184.2
021100 01  ERROR-HOLD                    PIC 999      VALUE ZERO.       IX2184.2
021200 01  DUMMY-HOLD                    PIC X(120)   VALUE SPACE.      IX2184.2
021300 01  RECORD-COUNT                  PIC 9(5)     VALUE ZERO.       IX2184.2
021400 01  ANSI-REFERENCE                PIC X(48)    VALUE SPACES.     IX2184.2
021500 01  CCVS-H-1.                                                    IX2184.2
021600     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2184.2
021700     02  FILLER                    PIC X(42)    VALUE             IX2184.2
021800     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM".                 IX2184.2
021900     02  FILLER                    PIC X(39)    VALUE SPACES.     IX2184.2
022000 01  CCVS-H-2A.                                                   IX2184.2
022100   02  FILLER                        PIC X(40)  VALUE SPACE.      IX2184.2
022200   02  FILLER                        PIC X(7)   VALUE "CCVS85 ".  IX2184.2
022300   02  FILLER                        PIC XXXX   VALUE             IX2184.2
022400     "4.2 ".                                                      IX2184.2
022500   02  FILLER                        PIC X(28)  VALUE             IX2184.2
022600            " COPY - NOT FOR DISTRIBUTION".                       IX2184.2
022700   02  FILLER                        PIC X(41)  VALUE SPACE.      IX2184.2
022800                                                                  IX2184.2
022900 01  CCVS-H-2B.                                                   IX2184.2
023000   02  FILLER                        PIC X(15)  VALUE             IX2184.2
023100            "TEST RESULT OF ".                                    IX2184.2
023200   02  TEST-ID                       PIC X(9).                    IX2184.2
023300   02  FILLER                        PIC X(4)   VALUE             IX2184.2
023400            " IN ".                                               IX2184.2
023500   02  FILLER                        PIC X(12)  VALUE             IX2184.2
023600     " HIGH       ".                                              IX2184.2
023700   02  FILLER                        PIC X(22)  VALUE             IX2184.2
023800            " LEVEL VALIDATION FOR ".                             IX2184.2
023900   02  FILLER                        PIC X(58)  VALUE             IX2184.2
024000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2184.2
024100 01  CCVS-H-3.                                                    IX2184.2
024200     02  FILLER                      PIC X(34)  VALUE             IX2184.2
024300            " FOR OFFICIAL USE ONLY    ".                         IX2184.2
024400     02  FILLER                      PIC X(58)  VALUE             IX2184.2
024500     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".IX2184.2
024600     02  FILLER                      PIC X(28)  VALUE             IX2184.2
024700            "  COPYRIGHT   1985 ".                                IX2184.2
024800 01  CCVS-E-1.                                                    IX2184.2
024900     02 FILLER                       PIC X(52)  VALUE SPACE.      IX2184.2
025000     02 FILLER  PIC X(14) VALUE IS "END OF TEST-  ".              IX2184.2
025100     02 ID-AGAIN                     PIC X(9).                    IX2184.2
025200     02 FILLER                       PIC X(45)  VALUE SPACES.     IX2184.2
025300 01  CCVS-E-2.                                                    IX2184.2
025400     02  FILLER                      PIC X(31)  VALUE SPACE.      IX2184.2
025500     02  FILLER                      PIC X(21)  VALUE SPACE.      IX2184.2
025600     02 CCVS-E-2-2.                                               IX2184.2
025700         03 ERROR-TOTAL              PIC XXX    VALUE SPACE.      IX2184.2
025800         03 FILLER                   PIC X      VALUE SPACE.      IX2184.2
025900         03 ENDER-DESC               PIC X(44)  VALUE             IX2184.2
026000            "ERRORS ENCOUNTERED".                                 IX2184.2
026100 01  CCVS-E-3.                                                    IX2184.2
026200     02  FILLER                      PIC X(22)  VALUE             IX2184.2
026300            " FOR OFFICIAL USE ONLY".                             IX2184.2
026400     02  FILLER                      PIC X(12)  VALUE SPACE.      IX2184.2
026500     02  FILLER                      PIC X(58)  VALUE             IX2184.2
026600     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".IX2184.2
026700     02  FILLER                      PIC X(13)  VALUE SPACE.      IX2184.2
026800     02 FILLER                       PIC X(15)  VALUE             IX2184.2
026900             " COPYRIGHT 1985".                                   IX2184.2
027000 01  CCVS-E-4.                                                    IX2184.2
027100     02 CCVS-E-4-1                   PIC XXX    VALUE SPACE.      IX2184.2
027200     02 FILLER                       PIC X(4)   VALUE " OF ".     IX2184.2
027300     02 CCVS-E-4-2                   PIC XXX    VALUE SPACE.      IX2184.2
027400     02 FILLER                       PIC X(40)  VALUE             IX2184.2
027500      "  TESTS WERE EXECUTED SUCCESSFULLY".                       IX2184.2
027600 01  XXINFO.                                                      IX2184.2
027700     02 FILLER                       PIC X(19)  VALUE             IX2184.2
027800            "*** INFORMATION ***".                                IX2184.2
027900     02 INFO-TEXT.                                                IX2184.2
028000       04 FILLER                     PIC X(8)   VALUE SPACE.      IX2184.2
028100       04 XXCOMPUTED                 PIC X(20).                   IX2184.2
028200       04 FILLER                     PIC X(5)   VALUE SPACE.      IX2184.2
028300       04 XXCORRECT                  PIC X(20).                   IX2184.2
028400     02 INF-ANSI-REFERENCE           PIC X(48).                   IX2184.2
028500 01  HYPHEN-LINE.                                                 IX2184.2
028600     02 FILLER  PIC IS X VALUE IS SPACE.                          IX2184.2
028700     02 FILLER  PIC IS X(65)    VALUE IS "************************IX2184.2
028800-    "*****************************************".                 IX2184.2
028900     02 FILLER  PIC IS X(54)    VALUE IS "************************IX2184.2
029000-    "******************************".                            IX2184.2
029100 01  CCVS-PGM-ID                     PIC X(9)   VALUE             IX2184.2
029200     "IX218A".                                                    IX2184.2
029300 PROCEDURE DIVISION.                                              IX2184.2
029400 DECLARATIVES.                                                    IX2184.2
029500                                                                  IX2184.2
029600 READ-OPTIONAL-10 SECTION.                                        IX2184.2
029700     USE AFTER EXCEPTION PROCEDURE ON IX-FS1.                     IX2184.2
029800 INPUT-PROCESS.                                                   IX2184.2
029900     MOVE 1 TO EOF-FLAG.                                          IX2184.2
030000                                                                  IX2184.2
030100 READ-OPTIONAL-23 SECTION.                                        IX2184.2
030200     USE AFTER EXCEPTION PROCEDURE ON IX-FS2.                     IX2184.2
030300 INPUT-PROCESS.                                                   IX2184.2
030400     MOVE 1 TO EOF-FLAG.                                          IX2184.2
030500                                                                  IX2184.2
030600 END DECLARATIVES.                                                IX2184.2
030700                                                                  IX2184.2
030800 CCVS1 SECTION.                                                   IX2184.2
030900 OPEN-FILES.                                                      IX2184.2
031000*    OPEN I-O RAW-DATA.                                           IX2184.2
031100*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2184.2
031200*    READ RAW-DATA INVALID KEY GO TO END-E-1.                     IX2184.2
031300*    MOVE "ABORTED " TO C-ABORT.                                  IX2184.2
031400*    ADD 1 TO C-NO-OF-TESTS.                                      IX2184.2
031500*    ACCEPT C-DATE  FROM DATE.                                    IX2184.2
031600*    ACCEPT C-TIME  FROM TIME.                                    IX2184.2
031700*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-1.             IX2184.2
031800*END-E-1.                                                         IX2184.2
031900*    CLOSE RAW-DATA.                                              IX2184.2
032000     OPEN    OUTPUT PRINT-FILE.                                   IX2184.2
032100     MOVE  CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.  IX2184.2
032200     MOVE    SPACE TO TEST-RESULTS.                               IX2184.2
032300     PERFORM HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.              IX2184.2
032400     GO TO CCVS1-EXIT.                                            IX2184.2
032500 CLOSE-FILES.                                                     IX2184.2
032600*    OPEN I-O RAW-DATA.                                           IX2184.2
032700*    MOVE CCVS-PGM-ID TO RAW-DATA-KEY.                            IX2184.2
032800*    READ RAW-DATA INVALID KEY GO TO END-E-2.                     IX2184.2
032900*    MOVE "OK.     " TO C-ABORT.                                  IX2184.2
033000*    MOVE PASS-COUNTER TO C-OK.                                   IX2184.2
033100*    MOVE ERROR-HOLD   TO C-ALL.                                  IX2184.2
033200*    MOVE ERROR-COUNTER TO C-FAIL.                                IX2184.2
033300*    MOVE DELETE-COUNTER TO C-DELETED.                            IX2184.2
033400*    MOVE INSPECT-COUNTER TO C-INSPECT.                           IX2184.2
033500*    REWRITE RAW-DATA-SATZ INVALID KEY GO TO END-E-2.             IX2184.2
033600*END-E-2.                                                         IX2184.2
033700*    CLOSE RAW-DATA IX-FS1 IX-FS2.                                IX2184.2
033800     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   IX2184.2
033900 TERMINATE-CCVS.                                                  IX2184.2
034000*    EXIT PROGRAM.                                                IX2184.2
034100*TERMINATE-CALL.                                                  IX2184.2
034200     STOP     RUN.                                                IX2184.2
034300 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         IX2184.2
034400 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           IX2184.2
034500 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          IX2184.2
034600 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-COUNTER.      IX2184.2
034700     MOVE "****TEST DELETED****" TO RE-MARK.                      IX2184.2
034800 PRINT-DETAIL.                                                    IX2184.2
034900     IF REC-CT NOT EQUAL TO ZERO                                  IX2184.2
035000             MOVE "." TO PARDOT-X                                 IX2184.2
035100             MOVE REC-CT TO DOTVALUE.                             IX2184.2
035200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      IX2184.2
035300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               IX2184.2
035400        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 IX2184.2
035500          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 IX2184.2
035600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              IX2184.2
035700     MOVE SPACE TO CORRECT-X.                                     IX2184.2
035800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         IX2184.2
035900     MOVE     SPACE TO RE-MARK.                                   IX2184.2
036000 HEAD-ROUTINE.                                                    IX2184.2
036100     MOVE CCVS-H-1  TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2184.2
036200     MOVE CCVS-H-2A TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.  IX2184.2
036300     MOVE CCVS-H-2B TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2184.2
036400     MOVE CCVS-H-3  TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.  IX2184.2
036500 COLUMN-NAMES-ROUTINE.                                            IX2184.2
036600     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2184.2
036700     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2184.2
036800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        IX2184.2
036900 END-ROUTINE.                                                     IX2184.2
037000     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.IX2184.2
037100 END-RTN-EXIT.                                                    IX2184.2
037200     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2184.2
037300 END-ROUTINE-1.                                                   IX2184.2
037400      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      IX2184.2
037500      ERROR-HOLD. ADD DELETE-COUNTER TO ERROR-HOLD.               IX2184.2
037600      ADD PASS-COUNTER TO ERROR-HOLD.                             IX2184.2
037700*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   IX2184.2
037800      MOVE PASS-COUNTER TO CCVS-E-4-1.                            IX2184.2
037900      MOVE ERROR-HOLD TO CCVS-E-4-2.                              IX2184.2
038000      MOVE CCVS-E-4 TO CCVS-E-2-2.                                IX2184.2
038100      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           IX2184.2
038200  END-ROUTINE-12.                                                 IX2184.2
038300      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        IX2184.2
038400     IF       ERROR-COUNTER IS EQUAL TO ZERO                      IX2184.2
038500         MOVE "NO " TO ERROR-TOTAL                                IX2184.2
038600         ELSE                                                     IX2184.2
038700         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       IX2184.2
038800     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           IX2184.2
038900     PERFORM WRITE-LINE.                                          IX2184.2
039000 END-ROUTINE-13.                                                  IX2184.2
039100     IF DELETE-COUNTER IS EQUAL TO ZERO                           IX2184.2
039200         MOVE "NO " TO ERROR-TOTAL  ELSE                          IX2184.2
039300         MOVE DELETE-COUNTER TO ERROR-TOTAL.                      IX2184.2
039400     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   IX2184.2
039500     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2184.2
039600      IF   INSPECT-COUNTER EQUAL TO ZERO                          IX2184.2
039700          MOVE "NO " TO ERROR-TOTAL                               IX2184.2
039800      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   IX2184.2
039900      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            IX2184.2
040000      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          IX2184.2
040100     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           IX2184.2
040200 WRITE-LINE.                                                      IX2184.2
040300     ADD 1 TO RECORD-COUNT.                                       IX2184.2
040400*    IF RECORD-COUNT GREATER 42                                   IX2184.2
040500*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          IX2184.2
040600*        MOVE SPACE TO DUMMY-RECORD                               IX2184.2
040700*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  IX2184.2
040800*        MOVE CCVS-H-1  TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2184.2
040900*        MOVE CCVS-H-2A TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES    IX2184.2
041000*        MOVE CCVS-H-2B TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2184.2
041100*        MOVE CCVS-H-3  TO DUMMY-RECORD PERFORM WRT-LN 3 TIMES    IX2184.2
041200*        MOVE CCVS-C-1  TO DUMMY-RECORD PERFORM WRT-LN            IX2184.2
041300*        MOVE CCVS-C-2  TO DUMMY-RECORD PERFORM WRT-LN            IX2184.2
041400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          IX2184.2
041500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          IX2184.2
041600*        MOVE ZERO TO RECORD-COUNT.                               IX2184.2
041700     PERFORM WRT-LN.                                              IX2184.2
041800 WRT-LN.                                                          IX2184.2
041900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               IX2184.2
042000     MOVE SPACE TO DUMMY-RECORD.                                  IX2184.2
042100 BLANK-LINE-PRINT.                                                IX2184.2
042200     PERFORM WRT-LN.                                              IX2184.2
042300 FAIL-ROUTINE.                                                    IX2184.2
042400     IF     COMPUTED-X NOT EQUAL TO SPACE                         IX2184.2
042500            GO TO   FAIL-ROUTINE-WRITE.                           IX2184.2
042600     IF     CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.IX2184.2
042700     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2184.2
042800     MOVE  "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.   IX2184.2
042900     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2184.2
043000     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2184.2
043100     GO TO  FAIL-ROUTINE-EX.                                      IX2184.2
043200 FAIL-ROUTINE-WRITE.                                              IX2184.2
043300     MOVE   TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE         IX2184.2
043400     MOVE   ANSI-REFERENCE TO COR-ANSI-REFERENCE.                 IX2184.2
043500     MOVE   TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES. IX2184.2
043600     MOVE   SPACES TO COR-ANSI-REFERENCE.                         IX2184.2
043700 FAIL-ROUTINE-EX. EXIT.                                           IX2184.2
043800 BAIL-OUT.                                                        IX2184.2
043900     IF     COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.   IX2184.2
044000     IF     CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.           IX2184.2
044100 BAIL-OUT-WRITE.                                                  IX2184.2
044200     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  IX2184.2
044300     MOVE   ANSI-REFERENCE TO INF-ANSI-REFERENCE.                 IX2184.2
044400     MOVE   XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   IX2184.2
044500     MOVE   SPACES TO INF-ANSI-REFERENCE.                         IX2184.2
044600 BAIL-OUT-EX. EXIT.                                               IX2184.2
044700 CCVS1-EXIT.                                                      IX2184.2
044800     EXIT.                                                        IX2184.2
044900                                                                  IX2184.2
045000******************************************************************IX2184.2
045100*   TEST  1                                                      *IX2184.2
045200*         READ   (ACCESS IS SEQUENTIAL) OPTIONAL NOT EXISTING FILEIX2184.2
045300*                                       10  EXPECTED             *IX2184.2
045400*         IX-4, 1.3.4 (2) A 2);                                  *IX2184.2
045500*         IX-29, 4.5.4 GR (4) B;                                 *IX2184.2
045600*         IX-30, 4.5.4 GR (10) A;                                *IX2184.2
045700*         IX-30, 4.5.4 GR (10) B;                                *IX2184.2
045800******************************************************************IX2184.2
045900 SECT-IX-01-001 SECTION.                                          IX2184.2
046000 REA-INIT-F1-01.                                                  IX2184.2
046100     MOVE ZERO TO EOF-FLAG.                                       IX2184.2
046200     MOVE SPACES TO IX-FS1-STATUS.                                IX2184.2
046300     MOVE "READ OPTION. 10 EXP." TO FEATURE.                      IX2184.2
046400     MOVE "REA-TEST-F1-01      " TO PAR-NAME.                     IX2184.2
046500     OPEN INPUT IX-FS1.                                           IX2184.2
046600 REA-TEST-F1-01.                                                  IX2184.2
046700     READ IX-FS1 AT END   GO TO REA-TEST-F1-01-1.                 IX2184.2
046800 REA-TEST-F1-01-1.                                                IX2184.2
046900     IF IX-FS1-STATUS EQUAL TO "10"                               IX2184.2
047000         GO TO REA-PASS-F1-01.                                    IX2184.2
047100 REA-FAIL-F1-01.                                                  IX2184.2
047200     MOVE "IX-4, 1.3.4, (2) A 2); IX-29 GR (4) B  IX-30 GR (10) A,IX2184.2
047300-         " B"                   TO RE-MARK.                      IX2184.2
047400     PERFORM FAIL.                                                IX2184.2
047500     MOVE IX-FS1-STATUS TO COMPUTED-A.                            IX2184.2
047600     MOVE "10" TO CORRECT-X.                                      IX2184.2
047700     GO TO REA-WRITE-F1-01.                                       IX2184.2
047800 REA-PASS-F1-01.                                                  IX2184.2
047900     PERFORM PASS.                                                IX2184.2
048000 REA-WRITE-F1-01.                                                 IX2184.2
048100     PERFORM PRINT-DETAIL.                                        IX2184.2
048200                                                                  IX2184.2
048300******************************************************************IX2184.2
048400*   TEST  2                                                      *IX2184.2
048500*         READ I-O  (ACCESS IS DYNAMIC) OPTIONAL NOT EXISTING FILEIX2184.2
048600*                                                                *IX2184.2
048700*         IX-30, GR (10) B                                       *IX2184.2
048800******************************************************************IX2184.2
048900 REA-INIT-GF-02.                                                  IX2184.2
049000     MOVE "REA-TEST-GF-02  " TO PAR-NAME.                         IX2184.2
049100     MOVE "NO USE MUST BE EXEC." TO FEATURE.                      IX2184.2
049200 REA-TEST-GF-02.                                                  IX2184.2
049300     IF EOF-FLAG EQUAL TO    0                                    IX2184.2
049400         GO TO REA-PASS-GF-02.                                    IX2184.2
049500 REA-FAIL-GF-02.                                                  IX2184.2
049600     MOVE "IX-30, GR (10) B; 1: USE PROCEDURE HAS BEEN  EXECUTED" IX2184.2
049700              TO RE-MARK.                                         IX2184.2
049800     PERFORM FAIL.                                                IX2184.2
049900     MOVE EOF-FLAG      TO COMPUTED-N.                            IX2184.2
050000     MOVE " 0" TO CORRECT-X.                                      IX2184.2
050100     GO TO REA-WRITE-GF-02.                                       IX2184.2
050200 REA-PASS-GF-02.                                                  IX2184.2
050300     PERFORM PASS.                                                IX2184.2
050400 REA-WRITE-GF-02.                                                 IX2184.2
050500     PERFORM PRINT-DETAIL.                                        IX2184.2
050600                                                                  IX2184.2
050700******************************************************************IX2184.2
050800*   TEST  3                                                      *IX2184.2
050900*         START   (FOR AN OPTIONAL FILE WHICH IS NOT PRESENT)    *IX2184.2
051000*         IX-4, 1.3.4 (3) C 2)          23  EXPECTED             *IX2184.2
051100******************************************************************IX2184.2
051200 STA-INIT-GF-01.                                                  IX2184.2
051300     MOVE ZERO TO EOF-FLAG.                                       IX2184.2
051400     MOVE SPACES TO IX-FS2-STATUS.                                IX2184.2
051500     MOVE "STA-TEST-GF-01  " TO PAR-NAME.                         IX2184.2
051600     MOVE "START OPT.   23 EXP." TO FEATURE.                      IX2184.2
051700     OPEN INPUT IX-FS2.                                           IX2184.2
051800 STA-TEST-GF-01.                                                  IX2184.2
051900     START IX-FS2        INVALID  KEY GO TO STA-TEST-GF-01-1.     IX2184.2
052000 STA-TEST-GF-01-1.                                                IX2184.2
052100     IF IX-FS2-STATUS EQUAL TO "23"                               IX2184.2
052200         GO TO STA-PASS-GF-01.                                    IX2184.2
052300 STA-FAIL-GF-01.                                                  IX2184.2
052400     MOVE "IX-4, 1.3.4,(3) C 2)" TO RE-MARK.                      IX2184.2
052500     PERFORM FAIL.                                                IX2184.2
052600     MOVE IX-FS2-STATUS TO COMPUTED-A.                            IX2184.2
052700     MOVE "23" TO CORRECT-X.                                      IX2184.2
052800     GO TO STA-WRITE-GF-01.                                       IX2184.2
052900 STA-PASS-GF-01.                                                  IX2184.2
053000     PERFORM PASS.                                                IX2184.2
053100 STA-WRITE-GF-01.                                                 IX2184.2
053200     PERFORM PRINT-DETAIL.                                        IX2184.2
053300                                                                  IX2184.2
053400******************************************************************IX2184.2
053500*   TEST  4                                                      *IX2184.2
053600*         START   (NO USE PROCEDURE MUST BE EXECUTED BECAUSE     *IX2184.2
053700*                  THE INVALID KEY PHRASE IS SPECIFIED)          *IX2184.2
053800*         IX-37, 4.7.4, (5), (6) AND (7)                         *IX2184.2
053900******************************************************************IX2184.2
054000 STA-INIT-GF-02.                                                  IX2184.2
054100     MOVE "STA-TEST-GF-02  " TO PAR-NAME.                         IX2184.2
054200     MOVE "START NO USE    EXP." TO FEATURE.                      IX2184.2
054300 STA-TEST-GF-02.                                                  IX2184.2
054400     IF EOF-FLAG  EQUAL TO ZERO                                   IX2184.2
054500         GO TO STA-PASS-GF-02.                                    IX2184.2
054600 STA-FAIL-GF-02.                                                  IX2184.2
054700     MOVE "IX-37 4.7.4,(5,6,7); 1:USE PROCEDURE HAS BEEN EXECUTED"IX2184.2
054800                                 TO RE-MARK.                      IX2184.2
054900     PERFORM FAIL.                                                IX2184.2
055000     MOVE EOF-FLAG      TO COMPUTED-N.                            IX2184.2
055100     MOVE  "0" TO CORRECT-X.                                      IX2184.2
055200     GO TO STA-WRITE-GF-02.                                       IX2184.2
055300 STA-PASS-GF-02.                                                  IX2184.2
055400     PERFORM PASS.                                                IX2184.2
055500 STA-WRITE-GF-02.                                                 IX2184.2
055600     PERFORM PRINT-DETAIL.                                        IX2184.2
055700                                                                  IX2184.2
055800******************************************************************IX2184.2
055900*   TEST  5                                                      *IX2184.2
056000*      READ (RANDOM) (FOR AN OPTIONAL FILE WHICH IS NOT PRESENT) *IX2184.2
056100*         IX-4, 1.3.4 (3) C 2)                                   *IX2184.2
056200******************************************************************IX2184.2
056300 REA-INIT-GF-03.                                                  IX2184.2
056400     MOVE ZERO TO EOF-FLAG.                                       IX2184.2
056500     MOVE SPACES TO IX-FS2-STATUS.                                IX2184.2
056600     MOVE "REA-TEST-GF-03  " TO PAR-NAME.                         IX2184.2
056700     MOVE "RANDOM READ  23 EXP." TO FEATURE.                      IX2184.2
056800 REA-TEST-GF-03.                                                  IX2184.2
056900     READ  IX-FS2        INVALID  KEY GO TO REA-TEST-GF-03-1.     IX2184.2
057000 REA-TEST-GF-03-1.                                                IX2184.2
057100     IF IX-FS2-STATUS EQUAL TO "23"                               IX2184.2
057200         GO TO REA-PASS-GF-03.                                    IX2184.2
057300 REA-FAIL-GF-03.                                                  IX2184.2
057400     MOVE "IX-4, 1.3.4,(3) C 2); IX-36 4.7.4,GR (1), (5), (6) AND IX2184.2
057500-         " (7)"                 TO RE-MARK.                      IX2184.2
057600     PERFORM FAIL.                                                IX2184.2
057700     MOVE IX-FS2-STATUS TO COMPUTED-A.                            IX2184.2
057800     MOVE "23" TO CORRECT-X.                                      IX2184.2
057900     GO TO REA-WRITE-GF-03.                                       IX2184.2
058000 REA-PASS-GF-03.                                                  IX2184.2
058100     PERFORM PASS.                                                IX2184.2
058200 REA-WRITE-GF-03.                                                 IX2184.2
058300     PERFORM PRINT-DETAIL.                                        IX2184.2
058400                                                                  IX2184.2
058500******************************************************************IX2184.2
058600*   TEST  6                                                      *IX2184.2
058700*         READ (RANDOM) NO USE PROCEDURE MUST BE EXECUTED BECAUSE*IX2184.2
058800*                  THE INVALID KEY PHRASE IS SPECIFIED)          *IX2184.2
058900*         IX-29, 4.5.4, GR (4) B, (17)                           *IX2184.2
059000******************************************************************IX2184.2
059100 REA-INIT-GF-04.                                                  IX2184.2
059200     MOVE "REA-TEST-GF-04  " TO PAR-NAME.                         IX2184.2
059300     MOVE "RANDOM READ (NO USE)" TO FEATURE.                      IX2184.2
059400 REA-TEST-GF-04.                                                  IX2184.2
059500     IF EOF-FLAG  EQUAL TO ZERO                                   IX2184.2
059600         GO TO REA-PASS-GF-04.                                    IX2184.2
059700 REA-FAIL-GF-04.                                                  IX2184.2
059800     MOVE "IX-29 4.5.4, GR (4) B, (17); 1: USE PROCEDURE HAS BEEN IX2184.2
059900-    "EXECUTED"             TO RE-MARK.                           IX2184.2
060000     PERFORM FAIL.                                                IX2184.2
060100     MOVE EOF-FLAG      TO COMPUTED-N.                            IX2184.2
060200     MOVE  "0" TO CORRECT-X.                                      IX2184.2
060300     GO TO REA-WRITE-GF-04.                                       IX2184.2
060400 REA-PASS-GF-04.                                                  IX2184.2
060500     PERFORM PASS.                                                IX2184.2
060600 REA-WRITE-GF-04.                                                 IX2184.2
060700     PERFORM PRINT-DETAIL.                                        IX2184.2
060800                                                                  IX2184.2
060900                                                                  IX2184.2
061000                                                                  IX2184.2
061100 CCVS-EXIT SECTION.                                               IX2184.2
061200 CCVS-999999.                                                     IX2184.2
061300     GO TO CLOSE-FILES.                                           IX2184.2
      *END-OF,IX218A                                                                  
