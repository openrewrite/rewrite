      *HEADER,COBOL,DB202A                                                            
000100 IDENTIFICATION DIVISION.                                         DB2024.2
000200 PROGRAM-ID.                                                      DB2024.2
000300     DB202A.                                                      DB2024.2
000400 AUTHOR.                                                          DB2024.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB2024.2
000600 INSTALLATION.                                                    DB2024.2
000700     GENERAL SERVICES ADMINISTRATION                              DB2024.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB2024.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB2024.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB2024.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB2024.2
001200                                                                  DB2024.2
001300     PHONE   (703) 756-6153                                       DB2024.2
001400                                                                  DB2024.2
001500     " HIGH       ".                                              DB2024.2
001600 DATE-WRITTEN.                                                    DB2024.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB2024.2
001800     CREATION DATE     /    VALIDATION DATE                       DB2024.2
001900     "4.2 ".                                                      DB2024.2
002000 SECURITY.                                                        DB2024.2
002100     NONE.                                                        DB2024.2
002200*                                                                 DB2024.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB2024.2
002400*                                                                 DB2024.2
002500*                       PROGRAM ABSTRACT                          DB2024.2
002600*                                                                 DB2024.2
002700*    DB202A TESTS THE CPABILITY OF THE DEBUG MODULE TO HANDLE     DB2024.2
002800*    DEBUGGING PROCEDURES WHICH ARE MONITORING I-O FUNCTIONS      DB2024.2
002900*    OF THE SEQUENTIAL I-O MODULE.  THIS PROGRAM IS TO BE         DB2024.2
003000*    COMPILED AND EXECUTED WITH BOTH COMPILE AND OBJECT TIME      DB2024.2
003100*    DEBUGGING SWITCHES ON.  THE DEBUGGING PROCEDURES SHOULD      DB2024.2
003200*    BE INCLUDED IN COMPILATION AND GENERATE CODE.                DB2024.2
003300*                                                                 DB2024.2
003400*    DURING EXECUTION, A SEQUENTIAL FILE IS CREATED CONTAINING    DB2024.2
003500*    80-CHARACTER RECORDS.  THE FILE US THEN READ.  EXECUTION     DB2024.2
003600*    OF "OPEN", "READ", AND "WRITE" FUNCTION SHOULD TRIGGER THE   DB2024.2
003700*    APPROPRIATE DEBUGGING PROCEDURES.                            DB2024.2
003800*                                                                 DB2024.2
003900*                                                                 DB2024.2
004000*                                                                 DB2024.2
004100 ENVIRONMENT DIVISION.                                            DB2024.2
004200 CONFIGURATION SECTION.                                           DB2024.2
004300 SOURCE-COMPUTER.                                                 DB2024.2
004400     XXXXX082                                                     DB2024.2
004500     WITH DEBUGGING MODE.                                         DB2024.2
004600 OBJECT-COMPUTER.                                                 DB2024.2
004700     XXXXX083.                                                    DB2024.2
004800 INPUT-OUTPUT SECTION.                                            DB2024.2
004900 FILE-CONTROL.                                                    DB2024.2
005000     SELECT PRINT-FILE ASSIGN TO                                  DB2024.2
005100     XXXXX055.                                                    DB2024.2
005200     SELECT SEQ-FILE ASSIGN TO                                    DB2024.2
005300     XXXXX014.                                                    DB2024.2
005400 DATA DIVISION.                                                   DB2024.2
005500 FILE SECTION.                                                    DB2024.2
005600 FD  PRINT-FILE                                                   DB2024.2
005700     LABEL RECORDS                                                DB2024.2
005800     XXXXX084                                                     DB2024.2
005900     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB2024.2
006000 01  PRINT-REC PICTURE X(120).                                    DB2024.2
006100 01  DUMMY-RECORD PICTURE X(120).                                 DB2024.2
006200 FD  SEQ-FILE                                                     DB2024.2
006300C    VALUE OF                                                     DB2024.2
006400C    XXXXX074                                                     DB2024.2
006500*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB2024.2
006600C    IS                                                           DB2024.2
006700C    XXXXX075                                                     DB2024.2
006800*      XXXXX075  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB2024.2
006900*    XXXXX069                                                     DB2024.2
007000*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB2024.2
007100     LABEL RECORDS ARE STANDARD.                                  DB2024.2
007200 01  SEQ-REC-1 PIC X(120).                                        DB2024.2
007300 01  SEQ-REC-2 PIC X(120).                                        DB2024.2
007400 01  SEQ-REC-3 PIC X(120).                                        DB2024.2
007500 WORKING-STORAGE SECTION.                                         DB2024.2
007600 01  ITEM-1.                                                      DB2024.2
007700     02  KEY-1 PIC 99.                                            DB2024.2
007800     02  LINE-1 PIC X(6).                                         DB2024.2
007900     02  NAME-1 PIC X(30).                                        DB2024.2
008000     02  UNQUAL-NAME-1 PIC X(30).                                 DB2024.2
008100     02  CONTENTS-1 PIC X(120).                                   DB2024.2
008200     02  CONTENTS-REC PIC X(120).                                 DB2024.2
008300 01  FILE-RECORD-INFORMATION-REC.                                 DB2024.2
008400     03 FILE-RECORD-INFO-SKELETON.                                DB2024.2
008500        05 FILLER                 PICTURE X(48)       VALUE       DB2024.2
008600             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  DB2024.2
008700        05 FILLER                 PICTURE X(46)       VALUE       DB2024.2
008800             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    DB2024.2
008900        05 FILLER                 PICTURE X(26)       VALUE       DB2024.2
009000             ",LFIL=000000,ORG=  ,LBLR= ".                        DB2024.2
009100        05 FILLER                 PICTURE X(37)       VALUE       DB2024.2
009200             ",RECKEY=                             ".             DB2024.2
009300        05 FILLER                 PICTURE X(38)       VALUE       DB2024.2
009400             ",ALTKEY1=                             ".            DB2024.2
009500        05 FILLER                 PICTURE X(38)       VALUE       DB2024.2
009600             ",ALTKEY2=                             ".            DB2024.2
009700        05 FILLER                 PICTURE X(7)        VALUE SPACE.DB2024.2
009800     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              DB2024.2
009900        05 FILE-RECORD-INFO-P1-120.                               DB2024.2
010000           07 FILLER              PIC X(5).                       DB2024.2
010100           07 XFILE-NAME           PIC X(6).                      DB2024.2
010200           07 FILLER              PIC X(8).                       DB2024.2
010300           07 XRECORD-NAME         PIC X(6).                      DB2024.2
010400           07 FILLER              PIC X(1).                       DB2024.2
010500           07 REELUNIT-NUMBER     PIC 9(1).                       DB2024.2
010600           07 FILLER              PIC X(7).                       DB2024.2
010700           07 XRECORD-NUMBER       PIC 9(6).                      DB2024.2
010800           07 FILLER              PIC X(6).                       DB2024.2
010900           07 UPDATE-NUMBER       PIC 9(2).                       DB2024.2
011000           07 FILLER              PIC X(5).                       DB2024.2
011100           07 ODO-NUMBER          PIC 9(4).                       DB2024.2
011200           07 FILLER              PIC X(5).                       DB2024.2
011300           07 XPROGRAM-NAME        PIC X(5).                      DB2024.2
011400           07 FILLER              PIC X(7).                       DB2024.2
011500           07 XRECORD-LENGTH       PIC 9(6).                      DB2024.2
011600           07 FILLER              PIC X(7).                       DB2024.2
011700           07 CHARS-OR-RECORDS    PIC X(2).                       DB2024.2
011800           07 FILLER              PIC X(1).                       DB2024.2
011900           07 XBLOCK-SIZE          PIC 9(4).                      DB2024.2
012000           07 FILLER              PIC X(6).                       DB2024.2
012100           07 RECORDS-IN-FILE     PIC 9(6).                       DB2024.2
012200           07 FILLER              PIC X(5).                       DB2024.2
012300           07 XFILE-ORGANIZATION   PIC X(2).                      DB2024.2
012400           07 FILLER              PIC X(6).                       DB2024.2
012500           07 XLABEL-TYPE          PIC X(1).                      DB2024.2
012600        05 FILE-RECORD-INFO-P121-240.                             DB2024.2
012700           07 FILLER              PIC X(8).                       DB2024.2
012800           07 XRECORD-KEY          PIC X(29).                     DB2024.2
012900           07 FILLER              PIC X(9).                       DB2024.2
013000           07 ALTERNATE-KEY1      PIC X(29).                      DB2024.2
013100           07 FILLER              PIC X(9).                       DB2024.2
013200           07 ALTERNATE-KEY2      PIC X(29).                      DB2024.2
013300           07 FILLER              PIC X(7).                       DB2024.2
013400 01  TEST-RESULTS.                                                DB2024.2
013500     02 FILLER                    PICTURE X VALUE SPACE.          DB2024.2
013600     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB2024.2
013700     02 FILLER                    PICTURE X VALUE SPACE.          DB2024.2
013800     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB2024.2
013900     02 FILLER                    PICTURE X  VALUE SPACE.         DB2024.2
014000     02  PAR-NAME.                                                DB2024.2
014100       03 FILLER PICTURE X(12) VALUE SPACE.                       DB2024.2
014200       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB2024.2
014300       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB2024.2
014400       03 FILLER PIC X(5) VALUE SPACE.                            DB2024.2
014500     02 FILLER PIC X(10) VALUE SPACE.                             DB2024.2
014600     02 RE-MARK PIC X(61).                                        DB2024.2
014700 01  TEST-COMPUTED.                                               DB2024.2
014800     02 FILLER PIC X(30) VALUE SPACE.                             DB2024.2
014900     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB2024.2
015000     02 COMPUTED-X.                                               DB2024.2
015100     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB2024.2
015200     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB2024.2
015300     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB2024.2
015400     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB2024.2
015500     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB2024.2
015600     03       CM-18V0 REDEFINES COMPUTED-A.                       DB2024.2
015700         04 COMPUTED-18V0                   PICTURE -9(18).       DB2024.2
015800         04 FILLER                          PICTURE X.            DB2024.2
015900     03 FILLER PIC X(50) VALUE SPACE.                             DB2024.2
016000 01  TEST-CORRECT.                                                DB2024.2
016100     02 FILLER PIC X(30) VALUE SPACE.                             DB2024.2
016200     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB2024.2
016300     02 CORRECT-X.                                                DB2024.2
016400     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB2024.2
016500     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB2024.2
016600     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB2024.2
016700     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB2024.2
016800     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB2024.2
016900     03      CR-18V0 REDEFINES CORRECT-A.                         DB2024.2
017000         04 CORRECT-18V0                    PICTURE -9(18).       DB2024.2
017100         04 FILLER                          PICTURE X.            DB2024.2
017200     03 FILLER PIC X(50) VALUE SPACE.                             DB2024.2
017300 01  CCVS-C-1.                                                    DB2024.2
017400     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB2024.2
017500-    "SS  PARAGRAPH-NAME                                          DB2024.2
017600-    "        REMARKS".                                           DB2024.2
017700     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB2024.2
017800 01  CCVS-C-2.                                                    DB2024.2
017900     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2024.2
018000     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB2024.2
018100     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB2024.2
018200     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB2024.2
018300     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB2024.2
018400 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB2024.2
018500 01  REC-CT PICTURE 99 VALUE ZERO.                                DB2024.2
018600 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB2024.2
018700 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB2024.2
018800 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB2024.2
018900 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB2024.2
019000 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB2024.2
019100 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB2024.2
019200 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB2024.2
019300 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB2024.2
019400 01  CCVS-H-1.                                                    DB2024.2
019500     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB2024.2
019600     02 FILLER PICTURE X(67) VALUE                                DB2024.2
019700     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB2024.2
019800-    " SYSTEM".                                                   DB2024.2
019900     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB2024.2
020000 01  CCVS-H-2.                                                    DB2024.2
020100     02 FILLER PICTURE X(52) VALUE IS                             DB2024.2
020200     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB2024.2
020300     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB2024.2
020400     02 TEST-ID PICTURE IS X(9).                                  DB2024.2
020500     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB2024.2
020600 01  CCVS-H-3.                                                    DB2024.2
020700     02  FILLER PICTURE X(34) VALUE                               DB2024.2
020800     " FOR OFFICIAL USE ONLY    ".                                DB2024.2
020900     02  FILLER PICTURE X(58) VALUE                               DB2024.2
021000     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB2024.2
021100     02  FILLER PICTURE X(28) VALUE                               DB2024.2
021200     "  COPYRIGHT   1974 ".                                       DB2024.2
021300 01  CCVS-E-1.                                                    DB2024.2
021400     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB2024.2
021500     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB2024.2
021600     02 ID-AGAIN PICTURE IS X(9).                                 DB2024.2
021700     02 FILLER PICTURE X(45) VALUE IS                             DB2024.2
021800     " NTIS DISTRIBUTION COBOL 74".                               DB2024.2
021900 01  CCVS-E-2.                                                    DB2024.2
022000     02  FILLER                   PICTURE X(31)  VALUE            DB2024.2
022100     SPACE.                                                       DB2024.2
022200     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB2024.2
022300     02 CCVS-E-2-2.                                               DB2024.2
022400         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB2024.2
022500         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB2024.2
022600         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB2024.2
022700 01  CCVS-E-3.                                                    DB2024.2
022800     02  FILLER PICTURE X(22) VALUE                               DB2024.2
022900     " FOR OFFICIAL USE ONLY".                                    DB2024.2
023000     02  FILLER PICTURE X(12) VALUE SPACE.                        DB2024.2
023100     02  FILLER PICTURE X(58) VALUE                               DB2024.2
023200     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB2024.2
023300     02  FILLER PICTURE X(13) VALUE SPACE.                        DB2024.2
023400     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB2024.2
023500 01  CCVS-E-4.                                                    DB2024.2
023600     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB2024.2
023700     02 FILLER PIC XXXX VALUE " OF ".                             DB2024.2
023800     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB2024.2
023900     02 FILLER PIC X(40) VALUE                                    DB2024.2
024000      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB2024.2
024100 01  XXINFO.                                                      DB2024.2
024200     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB2024.2
024300     02 INFO-TEXT.                                                DB2024.2
024400     04 FILLER PIC X(20) VALUE SPACE.                             DB2024.2
024500     04 XXCOMPUTED PIC X(20).                                     DB2024.2
024600     04 FILLER PIC X(5) VALUE SPACE.                              DB2024.2
024700     04 XXCORRECT PIC X(20).                                      DB2024.2
024800 01  HYPHEN-LINE.                                                 DB2024.2
024900     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2024.2
025000     02 FILLER PICTURE IS X(65) VALUE IS "************************DB2024.2
025100-    "*****************************************".                 DB2024.2
025200     02 FILLER PICTURE IS X(54) VALUE IS "************************DB2024.2
025300-    "******************************".                            DB2024.2
025400 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB2024.2
025500     "DB202A".                                                    DB2024.2
025600 PROCEDURE DIVISION.                                              DB2024.2
025700 DECLARATIVES.                                                    DB2024.2
025800 FILENAME-PROC SECTION.                                           DB2024.2
025900     USE FOR DEBUGGING ON SEQ-FILE.                               DB2024.2
026000 FILENAME-1.                                                      DB2024.2
026100     MOVE 1 TO KEY-1.                                             DB2024.2
026200 DB-COMMON.                                                       DB2024.2
026300     MOVE DEBUG-LINE TO LINE-1.                                   DB2024.2
026400     MOVE DEBUG-NAME TO NAME-1 UNQUAL-NAME-1.                     DB2024.2
026500     MOVE DEBUG-CONTENTS TO CONTENTS-1.                           DB2024.2
026600     INSPECT UNQUAL-NAME-1 REPLACING CHARACTERS BY SPACES         DB2024.2
026700         AFTER INITIAL SPACE.                                     DB2024.2
026800 WRITE-PROC SECTION.                                              DB2024.2
026900     USE FOR DEBUGGING ON ALL REFERENCES OF SEQ-REC-1  SEQ-REC-2. DB2024.2
027000 WRITE-1.                                                         DB2024.2
027100     ADD 1 TO KEY-1.                                              DB2024.2
027200     PERFORM DB-COMMON.                                           DB2024.2
027300     MOVE SEQ-REC-3 TO CONTENTS-REC.                              DB2024.2
027400 END DECLARATIVES.                                                DB2024.2
027500 CCVS1 SECTION.                                                   DB2024.2
027600 OPEN-FILES.                                                      DB2024.2
027700     OPEN     OUTPUT PRINT-FILE.                                  DB2024.2
027800     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB2024.2
027900     MOVE    SPACE TO TEST-RESULTS.                               DB2024.2
028000     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB2024.2
028100     GO TO CCVS1-EXIT.                                            DB2024.2
028200 CLOSE-FILES.                                                     DB2024.2
028300     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB2024.2
028400 TERMINATE-CCVS.                                                  DB2024.2
028500*    EXIT PROGRAM.                                                DB2024.2
028600*TERMINATE-CALL.                                                  DB2024.2
028700     STOP     RUN.                                                DB2024.2
028800 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB2024.2
028900 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB2024.2
029000 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB2024.2
029100 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB2024.2
029200     MOVE "****TEST DELETED****" TO RE-MARK.                      DB2024.2
029300 PRINT-DETAIL.                                                    DB2024.2
029400     IF REC-CT NOT EQUAL TO ZERO                                  DB2024.2
029500             MOVE "." TO PARDOT-X                                 DB2024.2
029600             MOVE REC-CT TO DOTVALUE.                             DB2024.2
029700     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB2024.2
029800     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB2024.2
029900        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB2024.2
030000          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB2024.2
030100     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB2024.2
030200     MOVE SPACE TO CORRECT-X.                                     DB2024.2
030300     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB2024.2
030400     MOVE     SPACE TO RE-MARK.                                   DB2024.2
030500 HEAD-ROUTINE.                                                    DB2024.2
030600     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2024.2
030700     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB2024.2
030800     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB2024.2
030900 COLUMN-NAMES-ROUTINE.                                            DB2024.2
031000     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2024.2
031100     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2024.2
031200     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB2024.2
031300 END-ROUTINE.                                                     DB2024.2
031400     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB2024.2
031500 END-RTN-EXIT.                                                    DB2024.2
031600     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2024.2
031700 END-ROUTINE-1.                                                   DB2024.2
031800      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB2024.2
031900      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB2024.2
032000      ADD PASS-COUNTER TO ERROR-HOLD.                             DB2024.2
032100*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB2024.2
032200      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB2024.2
032300      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB2024.2
032400      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB2024.2
032500      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB2024.2
032600  END-ROUTINE-12.                                                 DB2024.2
032700      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB2024.2
032800     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB2024.2
032900         MOVE "NO " TO ERROR-TOTAL                                DB2024.2
033000         ELSE                                                     DB2024.2
033100         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB2024.2
033200     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB2024.2
033300     PERFORM WRITE-LINE.                                          DB2024.2
033400 END-ROUTINE-13.                                                  DB2024.2
033500     IF DELETE-CNT IS EQUAL TO ZERO                               DB2024.2
033600         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB2024.2
033700         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB2024.2
033800     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB2024.2
033900     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2024.2
034000      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB2024.2
034100          MOVE "NO " TO ERROR-TOTAL                               DB2024.2
034200      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB2024.2
034300      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB2024.2
034400      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB2024.2
034500     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2024.2
034600 WRITE-LINE.                                                      DB2024.2
034700     ADD 1 TO RECORD-COUNT.                                       DB2024.2
034800*    IF RECORD-COUNT GREATER 50                                   DB2024.2
034900*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB2024.2
035000*        MOVE SPACE TO DUMMY-RECORD                               DB2024.2
035100*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB2024.2
035200*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB2024.2
035300*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB2024.2
035400*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB2024.2
035500*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB2024.2
035600*        MOVE ZERO TO RECORD-COUNT.                               DB2024.2
035700     PERFORM WRT-LN.                                              DB2024.2
035800 WRT-LN.                                                          DB2024.2
035900     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB2024.2
036000     MOVE SPACE TO DUMMY-RECORD.                                  DB2024.2
036100 BLANK-LINE-PRINT.                                                DB2024.2
036200     PERFORM WRT-LN.                                              DB2024.2
036300 FAIL-ROUTINE.                                                    DB2024.2
036400     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB2024.2
036500     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB2024.2
036600     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB2024.2
036700     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2024.2
036800     GO TO FAIL-ROUTINE-EX.                                       DB2024.2
036900 FAIL-ROUTINE-WRITE.                                              DB2024.2
037000     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB2024.2
037100     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB2024.2
037200 FAIL-ROUTINE-EX. EXIT.                                           DB2024.2
037300 BAIL-OUT.                                                        DB2024.2
037400     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB2024.2
037500     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB2024.2
037600 BAIL-OUT-WRITE.                                                  DB2024.2
037700     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB2024.2
037800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2024.2
037900 BAIL-OUT-EX. EXIT.                                               DB2024.2
038000 CCVS1-EXIT.                                                      DB2024.2
038100     EXIT.                                                        DB2024.2
038200 BEGIN-DB202A-TEST SECTION.                                       DB2024.2
038300 OPEN-TEST-1-INIT.                                                DB2024.2
038400     MOVE SPACES TO ITEM-1.                                       DB2024.2
038500     MOVE 0 TO KEY-1.                                             DB2024.2
038600     MOVE "OPEN-TEST-1" TO PAR-NAME.                              DB2024.2
038700     MOVE "DEBUG OPEN FILENAME" TO FEATURE.                       DB2024.2
038800******************************************************************DB2024.2
038900*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2024.2
039000*    "OPEN-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2024.2
039100*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "OPEN       *DB2024.2
039200*    OUTPUT SEQ-FILE.".                                          *DB2024.2
039300******************************************************************DB2024.2
039400 OPEN-TEST-1.                                                     DB2024.2
039500     OPEN OUTPUT SEQ-FILE.                                        DB2024.2
039600     IF KEY-1 IS EQUAL TO 1                                       DB2024.2
039700         PERFORM PASS                                             DB2024.2
039800         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2024.2
039900         PERFORM GEN-WRITE                                        DB2024.2
040000         GO TO OPEN-TEST-1A                                       DB2024.2
040100     ELSE PERFORM FAIL                                            DB2024.2
040200         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2024.2
040300         PERFORM GEN-WRITE                                        DB2024.2
040400         PERFORM DELETE-OPEN-TEST-1-SUBTESTS                      DB2024.2
040500         GO TO WRITE-TEST-1-INIT.                                 DB2024.2
040600 OPEN-TEST-1-DELETE.                                              DB2024.2
040700     PERFORM DE-LETE.                                             DB2024.2
040800     PERFORM GEN-WRITE.                                           DB2024.2
040900     PERFORM DELETE-OPEN-TEST-1-SUBTESTS.                         DB2024.2
041000     GO TO WRITE-TEST-1-INIT.                                     DB2024.2
041100 DELETE-OPEN-TEST-1-SUBTESTS.                                     DB2024.2
041200     MOVE "OPEN-TEST-1A" TO PAR-NAME.                             DB2024.2
041300     PERFORM DE-LETE.                                             DB2024.2
041400     PERFORM GEN-WRITE.                                           DB2024.2
041500     MOVE "OPEN-TEST-1B" TO PAR-NAME.                             DB2024.2
041600     PERFORM DE-LETE.                                             DB2024.2
041700     PERFORM GEN-WRITE.                                           DB2024.2
041800     MOVE "OPEN-TEST-1C" TO PAR-NAME.                             DB2024.2
041900     PERFORM DE-LETE.                                             DB2024.2
042000     PERFORM GEN-WRITE.                                           DB2024.2
042100 OPEN-TEST-1A.                                                    DB2024.2
042200     MOVE "OPEN-TEST-1A" TO PAR-NAME.                             DB2024.2
042300     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2024.2
042400     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2024.2
042500     MOVE LINE-1 TO COMPUTED-A.                                   DB2024.2
042600     PERFORM INSPT.                                               DB2024.2
042700     PERFORM GEN-WRITE.                                           DB2024.2
042800 OPEN-TEST-1B.                                                    DB2024.2
042900     MOVE "OPEN-TEST-1B" TO PAR-NAME.                             DB2024.2
043000     IF UNQUAL-NAME-1 IS EQUAL TO "SEQ-FILE"                      DB2024.2
043100         PERFORM PASS                                             DB2024.2
043200     ELSE  PERFORM FAIL                                           DB2024.2
043300         MOVE NAME-1 TO COMPUTED-A                                DB2024.2
043400     MOVE "SEQ-FILE" TO CORRECT-A.                                DB2024.2
043500     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2024.2
043600     PERFORM GEN-WRITE.                                           DB2024.2
043700 OPEN-TEST-1C.                                                    DB2024.2
043800     MOVE "OPEN-TEST-1C" TO PAR-NAME.                             DB2024.2
043900     IF CONTENTS-1 IS EQUAL TO SPACES                             DB2024.2
044000         PERFORM PASS                                             DB2024.2
044100     ELSE PERFORM FAIL                                            DB2024.2
044200         MOVE CONTENTS-1 TO COMPUTED-A                            DB2024.2
044300         MOVE "(SPACES)" TO CORRECT-A.                            DB2024.2
044400     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2024.2
044500     PERFORM GEN-WRITE.                                           DB2024.2
044600 WRITE-TEST-1-INIT.                                               DB2024.2
044700     MOVE "WRITE-TEST-1" TO PAR-NAME.                             DB2024.2
044800     MOVE SPACES TO ITEM-1.                                       DB2024.2
044900     MOVE 0 TO KEY-1.                                             DB2024.2
045000     MOVE "DEBUG WRITE/ALL REF" TO FEATURE.                       DB2024.2
045100     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO (1).      DB2024.2
045200     MOVE "SEQ-FI" TO XFILE-NAME (1).                             DB2024.2
045300     MOVE "REC-1" TO XRECORD-NAME (1).                            DB2024.2
045400     MOVE ".XXX." TO XPROGRAM-NAME (1).                           DB2024.2
045500     MOVE 120 TO XRECORD-LENGTH (1).                              DB2024.2
045600     MOVE "RC" TO CHARS-OR-RECORDS (1).                           DB2024.2
045700     MOVE 1 TO XBLOCK-SIZE (1).                                   DB2024.2
045800     MOVE 30 TO RECORDS-IN-FILE (1).                              DB2024.2
045900     MOVE "SQ" TO XFILE-ORGANIZATION (1).                         DB2024.2
046000     MOVE "S" TO XLABEL-TYPE (1).                                 DB2024.2
046100 WRITE-TEST-1.                                                    DB2024.2
046200     PERFORM WRITE-REC-1  10  TIMES.                              DB2024.2
046300     IF KEY-1 IS EQUAL TO 10                                      DB2024.2
046400         PERFORM PASS                                             DB2024.2
046500         MOVE "DEBUG PROC EXECUTED 10 TIMES" TO RE-MARK           DB2024.2
046600     ELSE PERFORM FAIL                                            DB2024.2
046700         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2024.2
046800         MOVE KEY-1 TO COMPUTED-18V0                              DB2024.2
046900         MOVE 10 TO CORRECT-18V0.                                 DB2024.2
047000     PERFORM GEN-WRITE.                                           DB2024.2
047100     IF KEY-1 IS EQUAL TO 0                                       DB2024.2
047200         PERFORM DELETE-WRITE-TEST-1-SUBTESTS                     DB2024.2
047300         GO TO WRITE-TEST-2-INIT                                  DB2024.2
047400     ELSE  GO TO WRITE-TEST-1A.                                   DB2024.2
047500 WRITE-TEST-1-DELETE.                                             DB2024.2
047600     PERFORM DE-LETE.                                             DB2024.2
047700     PERFORM GEN-WRITE.                                           DB2024.2
047800     PERFORM DELETE-WRITE-TEST-1-SUBTESTS                         DB2024.2
047900     GO TO WRITE-TEST-2-INIT.                                     DB2024.2
048000 DELETE-WRITE-TEST-1-SUBTESTS.                                    DB2024.2
048100     MOVE "WRITE-TEST-1A" TO PAR-NAME.                            DB2024.2
048200     PERFORM DE-LETE.                                             DB2024.2
048300     PERFORM GEN-WRITE.                                           DB2024.2
048400     MOVE "WRITE-TEST-1B" TO PAR-NAME.                            DB2024.2
048500     PERFORM DE-LETE.                                             DB2024.2
048600     PERFORM GEN-WRITE.                                           DB2024.2
048700     MOVE "WRITE-TEST-1C" TO PAR-NAME.                            DB2024.2
048800     PERFORM DE-LETE.                                             DB2024.2
048900     PERFORM GEN-WRITE.                                           DB2024.2
049000     MOVE "WRITE-TEST-1D" TO PAR-NAME.                            DB2024.2
049100     PERFORM DE-LETE.                                             DB2024.2
049200     PERFORM GEN-WRITE.                                           DB2024.2
049300 WRITE-REC-1.                                                     DB2024.2
049400     MOVE SPACES TO SEQ-REC-3.                                    DB2024.2
049500     ADD 1 TO XRECORD-NUMBER (1).                                 DB2024.2
049600******************************************************************DB2024.2
049700*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2024.2
049800*    "WRITE-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT    *DB2024.2
049900*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS,             *DB2024.2
050000*    "WRITE SEQ-REC-1 FROM FILE-RECORD-INFO-P1-120 (1).".        *DB2024.2
050100******************************************************************DB2024.2
050200     WRITE SEQ-REC-1 FROM FILE-RECORD-INFO-P1-120 (1).            DB2024.2
050300 WRITE-TEST-1A.                                                   DB2024.2
050400     MOVE "WRITE-TEST-1A" TO PAR-NAME.                            DB2024.2
050500     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2024.2
050600     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2024.2
050700     MOVE LINE-1 TO COMPUTED-A.                                   DB2024.2
050800     PERFORM INSPT.                                               DB2024.2
050900     PERFORM GEN-WRITE.                                           DB2024.2
051000 WRITE-TEST-1B.                                                   DB2024.2
051100     MOVE "WRITE-TEST-1B" TO PAR-NAME.                            DB2024.2
051200     IF UNQUAL-NAME-1 IS EQUAL TO "SEQ-REC-1"                     DB2024.2
051300         PERFORM PASS                                             DB2024.2
051400     ELSE  PERFORM FAIL                                           DB2024.2
051500         MOVE "SEQ-REC-1" TO CORRECT-A                            DB2024.2
051600         MOVE NAME-1 TO COMPUTED-A.                               DB2024.2
051700     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2024.2
051800     PERFORM GEN-WRITE.                                           DB2024.2
051900 WRITE-TEST-1C.                                                   DB2024.2
052000     MOVE "WRITE-TEST-1C" TO PAR-NAME.                            DB2024.2
052100     IF CONTENTS-REC IS EQUAL TO FILE-RECORD-INFO-P1-120 (1)      DB2024.2
052200         PERFORM PASS                                             DB2024.2
052300         MOVE "PROC EXECUTED AT PROPER TIME" TO RE-MARK           DB2024.2
052400         PERFORM GEN-WRITE                                        DB2024.2
052500         GO TO WRITE-TEST-1D                                      DB2024.2
052600     ELSE  PERFORM FAIL                                           DB2024.2
052700         MOVE "PROC NOT EXEC BETW MOVE / WRITE" TO RE-MARK        DB2024.2
052800         MOVE "1ST LINE = REC AREA" TO COMPUTED-A                 DB2024.2
052900         MOVE "2ND LINE = FROM FLD" TO CORRECT-A                  DB2024.2
053000         PERFORM GEN-WRITE.                                       DB2024.2
053100     MOVE CONTENTS-REC TO PRINT-REC.                              DB2024.2
053200     PERFORM WRITE-LINE.                                          DB2024.2
053300     MOVE FILE-RECORD-INFO-P1-120 (1) TO PRINT-REC.               DB2024.2
053400     PERFORM WRITE-LINE.                                          DB2024.2
053500 WRITE-TEST-1D.                                                   DB2024.2
053600     MOVE "WRITE-TEST-1D" TO PAR-NAME.                            DB2024.2
053700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2024.2
053800     IF CONTENTS-1 IS EQUAL TO FILE-RECORD-INFO-P1-120 (1)        DB2024.2
053900         PERFORM PASS                                             DB2024.2
054000         PERFORM GEN-WRITE                                        DB2024.2
054100         GO TO WRITE-TEST-2-INIT                                  DB2024.2
054200     ELSE  PERFORM FAIL                                           DB2024.2
054300         MOVE "SEE 1ST LINE FOLLOW" TO COMPUTED-A                 DB2024.2
054400         MOVE "SEE 2ND LINE FOLLOW" TO CORRECT-A                  DB2024.2
054500         PERFORM GEN-WRITE.                                       DB2024.2
054600     MOVE CONTENTS-1 TO PRINT-REC.                                DB2024.2
054700     PERFORM WRITE-LINE.                                          DB2024.2
054800     MOVE FILE-RECORD-INFO-P1-120 (1) TO PRINT-REC.               DB2024.2
054900     PERFORM WRITE-LINE.                                          DB2024.2
055000 WRITE-TEST-2-INIT.                                               DB2024.2
055100     MOVE "WRITE-TEST-2" TO PAR-NAME.                             DB2024.2
055200     MOVE SPACES TO ITEM-1.                                       DB2024.2
055300     MOVE 0 TO KEY-1.                                             DB2024.2
055400     MOVE "DEBUG WRITE/NOT ALL" TO FEATURE.                       DB2024.2
055500     MOVE 10 TO XRECORD-NUMBER (1).                               DB2024.2
055600     MOVE "REC-2" TO XRECORD-NAME (1).                            DB2024.2
055700 WRITE-TEST-2.                                                    DB2024.2
055800     PERFORM WRITE-REC-2 10 TIMES.                                DB2024.2
055900     IF KEY-1 IS EQUAL TO 10                                      DB2024.2
056000         PERFORM PASS                                             DB2024.2
056100         MOVE "DEBUG PROC EXECUTED 10 TIMES" TO RE-MARK           DB2024.2
056200     ELSE  PERFORM FAIL                                           DB2024.2
056300         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2024.2
056400         MOVE KEY-1 TO COMPUTED-18V0                              DB2024.2
056500         MOVE 10 TO CORRECT-18V0.                                 DB2024.2
056600     PERFORM GEN-WRITE.                                           DB2024.2
056700     IF KEY-1 IS EQUAL TO 0                                       DB2024.2
056800         PERFORM DELETE-WRITE-TEST-2-SUBTESTS                     DB2024.2
056900         GO TO WRITE-TEST-3-INIT                                  DB2024.2
057000     ELSE GO TO WRITE-TEST-2A.                                    DB2024.2
057100 WRITE-TEST-2-DELETE.                                             DB2024.2
057200     PERFORM DE-LETE.                                             DB2024.2
057300     PERFORM GEN-WRITE.                                           DB2024.2
057400     PERFORM DELETE-WRITE-TEST-2-SUBTESTS                         DB2024.2
057500     GO TO WRITE-TEST-3-INIT.                                     DB2024.2
057600 DELETE-WRITE-TEST-2-SUBTESTS.                                    DB2024.2
057700     MOVE "WRITE-TEST-2A" TO PAR-NAME.                            DB2024.2
057800     PERFORM DE-LETE.                                             DB2024.2
057900     PERFORM GEN-WRITE.                                           DB2024.2
058000     MOVE "WRITE-TEST-2B" TO PAR-NAME.                            DB2024.2
058100     PERFORM DE-LETE.                                             DB2024.2
058200     PERFORM GEN-WRITE.                                           DB2024.2
058300     MOVE "WRITE-TEST-2C" TO PAR-NAME.                            DB2024.2
058400     PERFORM DE-LETE.                                             DB2024.2
058500     PERFORM GEN-WRITE.                                           DB2024.2
058600 WRITE-REC-2.                                                     DB2024.2
058700     MOVE SPACES TO SEQ-REC-3.                                    DB2024.2
058800     ADD 1 TO XRECORD-NUMBER (1).                                 DB2024.2
058900     WRITE SEQ-REC-2 FROM FILE-RECORD-INFO-P1-120 (1).            DB2024.2
059000 WRITE-TEST-2A.                                                   DB2024.2
059100     MOVE "WRITE-TEST-2A" TO PAR-NAME.                            DB2024.2
059200     IF UNQUAL-NAME-1 IS EQUAL TO "SEQ-REC-2"                     DB2024.2
059300         PERFORM PASS                                             DB2024.2
059400     ELSE  PERFORM FAIL                                           DB2024.2
059500         MOVE "SEQ-REC-2" TO CORRECT-A                            DB2024.2
059600         MOVE NAME-1 TO COMPUTED-A.                               DB2024.2
059700     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2024.2
059800     PERFORM GEN-WRITE.                                           DB2024.2
059900 WRITE-TEST-2B.                                                   DB2024.2
060000     MOVE "WRITE-TEST-2B" TO PAR-NAME.                            DB2024.2
060100     IF CONTENTS-REC IS EQUAL TO FILE-RECORD-INFO-P1-120 (1)      DB2024.2
060200         PERFORM PASS                                             DB2024.2
060300         MOVE "PROC EXECUTED AT PROPER TIME" TO RE-MARK           DB2024.2
060400         PERFORM GEN-WRITE                                        DB2024.2
060500         GO TO WRITE-TEST-2C                                      DB2024.2
060600     ELSE  PERFORM FAIL                                           DB2024.2
060700         MOVE "PROC NOT EXEC BTWN MOVE / WRITE" TO RE-MARK        DB2024.2
060800         MOVE "1ST LINE = REC AREA" TO COMPUTED-A                 DB2024.2
060900         MOVE "2ND LINE = FROM FLD" TO CORRECT-A                  DB2024.2
061000         PERFORM GEN-WRITE.                                       DB2024.2
061100     MOVE CONTENTS-REC TO PRINT-REC.                              DB2024.2
061200     PERFORM WRITE-LINE.                                          DB2024.2
061300     MOVE FILE-RECORD-INFO-P1-120 (1) TO PRINT-REC.               DB2024.2
061400     PERFORM WRITE-LINE.                                          DB2024.2
061500 WRITE-TEST-2C.                                                   DB2024.2
061600     MOVE "WRITE-TEST-2C" TO PAR-NAME.                            DB2024.2
061700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2024.2
061800     IF CONTENTS-1 IS EQUAL TO FILE-RECORD-INFO-P1-120 (1)        DB2024.2
061900         PERFORM PASS                                             DB2024.2
062000         PERFORM GEN-WRITE                                        DB2024.2
062100         GO TO WRITE-TEST-3-INIT                                  DB2024.2
062200     ELSE  PERFORM FAIL                                           DB2024.2
062300         MOVE "SEE 1ST LINE FOLLOW" TO COMPUTED-A                 DB2024.2
062400         MOVE "SEE 2ND LINE FOLLOW" TO CORRECT-A                  DB2024.2
062500         PERFORM GEN-WRITE.                                       DB2024.2
062600     MOVE CONTENTS-1 TO PRINT-REC.                                DB2024.2
062700     PERFORM WRITE-LINE.                                          DB2024.2
062800     MOVE FILE-RECORD-INFO-P1-120 (1) TO PRINT-REC.               DB2024.2
062900     PERFORM WRITE-LINE.                                          DB2024.2
063000 WRITE-TEST-3-INIT.                                               DB2024.2
063100     MOVE SPACES TO ITEM-1                                        DB2024.2
063200     MOVE 0 TO KEY-1                                              DB2024.2
063300     MOVE "WRITE/NO DEBUG PROC" TO FEATURE.                       DB2024.2
063400     MOVE "WRITE-TEST-3" TO PAR-NAME.                             DB2024.2
063500     MOVE "REC-3" TO XRECORD-NAME (1)                             DB2024.2
063600     MOVE 20 TO XRECORD-NUMBER (1).                               DB2024.2
063700 WRITE-TEST-3.                                                    DB2024.2
063800     PERFORM WRITE-REC-3 10 TIMES.                                DB2024.2
063900     IF KEY-1 IS EQUAL TO 0                                       DB2024.2
064000         PERFORM PASS                                             DB2024.2
064100         MOVE  "DEBUG PROC NOT EXECUTED" TO RE-MARK               DB2024.2
064200     ELSE  PERFORM FAIL                                           DB2024.2
064300         MOVE "NO. TIMES DEBUG PROC EXECUTED" TO RE-MARK          DB2024.2
064400         MOVE KEY-1 TO COMPUTED-18V0                              DB2024.2
064500         MOVE 0 TO CORRECT-18V0.                                  DB2024.2
064600         PERFORM GEN-WRITE.                                       DB2024.2
064700     GO TO CLOSE-TEST-1-INIT.                                     DB2024.2
064800 WRITE-TEST-3-DELETE.                                             DB2024.2
064900     PERFORM DE-LETE.                                             DB2024.2
065000     PERFORM GEN-WRITE.                                           DB2024.2
065100     GO TO CLOSE-TEST-1-INIT.                                     DB2024.2
065200 WRITE-REC-3.                                                     DB2024.2
065300     MOVE SPACES TO SEQ-REC-3.                                    DB2024.2
065400     ADD 1 TO XRECORD-NUMBER (1).                                 DB2024.2
065500     WRITE SEQ-REC-3 FROM FILE-RECORD-INFO-P1-120 (1).            DB2024.2
065600 CLOSE-TEST-1-INIT.                                               DB2024.2
065700     MOVE SPACES TO ITEM-1.                                       DB2024.2
065800     MOVE 0 TO KEY-1.                                             DB2024.2
065900     MOVE "CLOSE-TEST-1" TO PAR-NAME.                             DB2024.2
066000     MOVE "DEBUG CLOSE FILENAME" TO FEATURE.                      DB2024.2
066100******************************************************************DB2024.2
066200*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2024.2
066300*    "CLOSE-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT    *DB2024.2
066400*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS,             *DB2024.2
066500*    "CLOSE SEQ-FILE.".                                          *DB2024.2
066600******************************************************************DB2024.2
066700 CLOSE-TEST-1.                                                    DB2024.2
066800     CLOSE SEQ-FILE.                                              DB2024.2
066900     IF KEY-1 IS EQUAL TO 1                                       DB2024.2
067000         PERFORM PASS                                             DB2024.2
067100         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2024.2
067200         PERFORM GEN-WRITE                                        DB2024.2
067300         GO TO CLOSE-TEST-1A                                      DB2024.2
067400     ELSE PERFORM  FAIL                                           DB2024.2
067500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2024.2
067600         PERFORM GEN-WRITE                                        DB2024.2
067700         PERFORM DELETE-CLOSE-TEST-1-SUBTESTS                     DB2024.2
067800         GO TO OPEN-TEST-2-INIT.                                  DB2024.2
067900 CLOSE-TEST-1-DELETE.                                             DB2024.2
068000     PERFORM DE-LETE.                                             DB2024.2
068100     PERFORM GEN-WRITE.                                           DB2024.2
068200     PERFORM DELETE-CLOSE-TEST-1-SUBTESTS.                        DB2024.2
068300     GO TO OPEN-TEST-2-INIT.                                      DB2024.2
068400 DELETE-CLOSE-TEST-1-SUBTESTS.                                    DB2024.2
068500     MOVE "CLOSE-TEST-1A" TO PAR-NAME.                            DB2024.2
068600     PERFORM DE-LETE.                                             DB2024.2
068700     PERFORM GEN-WRITE.                                           DB2024.2
068800     MOVE "CLOSE-TEST-1B" TO PAR-NAME.                            DB2024.2
068900     PERFORM DE-LETE.                                             DB2024.2
069000     PERFORM GEN-WRITE.                                           DB2024.2
069100     MOVE "CLOSE-TEST-1C" TO PAR-NAME.                            DB2024.2
069200     PERFORM DE-LETE.                                             DB2024.2
069300     PERFORM GEN-WRITE.                                           DB2024.2
069400 CLOSE-TEST-1A.                                                   DB2024.2
069500     MOVE "CLOSE-TEST-1A" TO PAR-NAME.                            DB2024.2
069600     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2024.2
069700     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2024.2
069800     MOVE LINE-1 TO COMPUTED-A.                                   DB2024.2
069900     PERFORM INSPT.                                               DB2024.2
070000     PERFORM GEN-WRITE.                                           DB2024.2
070100 CLOSE-TEST-1B.                                                   DB2024.2
070200     MOVE "CLOSE-TEST-1B" TO PAR-NAME.                            DB2024.2
070300     IF UNQUAL-NAME-1 IS EQUAL TO "SEQ-FILE"                      DB2024.2
070400         PERFORM PASS                                             DB2024.2
070500     ELSE  PERFORM FAIL                                           DB2024.2
070600         MOVE NAME-1 TO COMPUTED-A                                DB2024.2
070700         MOVE "SEQ-FILE" TO CORRECT-A.                            DB2024.2
070800     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2024.2
070900     PERFORM GEN-WRITE.                                           DB2024.2
071000 CLOSE-TEST-1C.                                                   DB2024.2
071100     MOVE "OPEN-TEST-1C" TO PAR-NAME.                             DB2024.2
071200     IF CONTENTS-1 IS EQUAL TO SPACES                             DB2024.2
071300         PERFORM PASS                                             DB2024.2
071400     ELSE PERFORM FAIL                                            DB2024.2
071500         MOVE CONTENTS-1 TO COMPUTED-A                            DB2024.2
071600         MOVE "(SPACES)" TO CORRECT-A.                            DB2024.2
071700     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2024.2
071800     PERFORM GEN-WRITE.                                           DB2024.2
071900 OPEN-TEST-2-INIT.                                                DB2024.2
072000     MOVE SPACES TO ITEM-1.                                       DB2024.2
072100     MOVE 0 TO KEY-1.                                             DB2024.2
072200     MOVE "OPEN-TEST-2" TO PAR-NAME.                              DB2024.2
072300     MOVE "DEBUG OPEN FILENAME" TO FEATURE.                       DB2024.2
072400 OPEN-TEST-2.                                                     DB2024.2
072500     OPEN INPUT SEQ-FILE.                                         DB2024.2
072600     IF KEY-1 IS EQUAL TO 1                                       DB2024.2
072700         PERFORM PASS                                             DB2024.2
072800         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2024.2
072900     ELSE PERFORM FAIL                                            DB2024.2
073000         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2024.2
073100     PERFORM GEN-WRITE.                                           DB2024.2
073200     GO TO READ-TEST-1-INIT.                                      DB2024.2
073300 OPEN-TEST-2-DELETE.                                              DB2024.2
073400     PERFORM DE-LETE.                                             DB2024.2
073500     PERFORM GEN-WRITE.                                           DB2024.2
073600 READ-TEST-1-INIT.                                                DB2024.2
073700     MOVE SPACES TO ITEM-1.                                       DB2024.2
073800     MOVE 0 TO KEY-1.                                             DB2024.2
073900     MOVE "READ-TEST-1" TO PAR-NAME.                              DB2024.2
074000     MOVE "DEBUG READ FILENAME" TO FEATURE.                       DB2024.2
074100******************************************************************DB2024.2
074200*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2024.2
074300*    "READ-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2024.2
074400*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "READ       *DB2024.2
074500*    SEQ-FILE AT END PERFORM READ-TEST-1-DELETE GO TO            *DB2024.2
074600*    READ-TEST-2.".                                              *DB2024.2
074700******************************************************************DB2024.2
074800 READ-TEST-1.                                                     DB2024.2
074900     READ SEQ-FILE  AT END                                        DB2024.2
075000         PERFORM READ-TEST-1-DELETE                               DB2024.2
075100         GO TO READ-TEST-2.                                       DB2024.2
075200     IF KEY-1 IS EQUAL TO 1                                       DB2024.2
075300         PERFORM PASS                                             DB2024.2
075400         MOVE "DEBUG PROC EXECUTED" TO RE-MARK                    DB2024.2
075500         PERFORM GEN-WRITE                                        DB2024.2
075600         GO TO READ-TEST-1A                                       DB2024.2
075700     ELSE  PERFORM FAIL                                           DB2024.2
075800         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2024.2
075900         PERFORM GEN-WRITE                                        DB2024.2
076000         PERFORM DELETE-READ-TEST-1-SUBTESTS                      DB2024.2
076100          GO TO READ-TEST-2-INIT.                                 DB2024.2
076200 READ-TEST-1-DELETE.                                              DB2024.2
076300     PERFORM DE-LETE.                                             DB2024.2
076400     PERFORM GEN-WRITE.                                           DB2024.2
076500     PERFORM DELETE-READ-TEST-1-SUBTESTS.                         DB2024.2
076600 READ-TEST-1-DELETE-A.                                            DB2024.2
076700     GO TO READ-TEST-2-DELETE.                                    DB2024.2
076800 DELETE-READ-TEST-1-SUBTESTS.                                     DB2024.2
076900     MOVE "READ-TEST-1A" TO PAR-NAME.                             DB2024.2
077000     PERFORM DE-LETE.                                             DB2024.2
077100     PERFORM GEN-WRITE.                                           DB2024.2
077200     MOVE "READ-TEST-1B" TO PAR-NAME.                             DB2024.2
077300     PERFORM DE-LETE.                                             DB2024.2
077400     PERFORM GEN-WRITE.                                           DB2024.2
077500     MOVE "READ-TEST-1C" TO PAR-NAME.                             DB2024.2
077600     PERFORM DE-LETE.                                             DB2024.2
077700     PERFORM GEN-WRITE.                                           DB2024.2
077800 READ-TEST-1A.                                                    DB2024.2
077900     MOVE "READ-TEST-1A" TO PAR-NAME.                             DB2024.2
078000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2024.2
078100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2024.2
078200     MOVE LINE-1 TO COMPUTED-A.                                   DB2024.2
078300     PERFORM INSPT.                                               DB2024.2
078400     PERFORM GEN-WRITE.                                           DB2024.2
078500 READ-TEST-1B.                                                    DB2024.2
078600     MOVE "READ-TEST-1B" TO PAR-NAME.                             DB2024.2
078700     IF UNQUAL-NAME-1 IS EQUAL TO "SEQ-FILE"                      DB2024.2
078800         PERFORM PASS                                             DB2024.2
078900     ELSE PERFORM FAIL                                            DB2024.2
079000         MOVE "SEQ-FILE" TO CORRECT-A                             DB2024.2
079100         MOVE NAME-1 TO COMPUTED-A.                               DB2024.2
079200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2024.2
079300     PERFORM GEN-WRITE.                                           DB2024.2
079400 READ-TEST-1C.                                                    DB2024.2
079500     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2024.2
079600     MOVE "READ-TEST-1C" TO PAR-NAME.                             DB2024.2
079700     IF CONTENTS-1 IS EQUAL TO SEQ-REC-3                          DB2024.2
079800         PERFORM PASS                                             DB2024.2
079900         PERFORM GEN-WRITE                                        DB2024.2
080000         GO TO READ-TEST-2-INIT                                   DB2024.2
080100     ELSE PERFORM FAIL                                            DB2024.2
080200         MOVE "SEE 1ST LINE FOLLOW" TO COMPUTED-A                 DB2024.2
080300         MOVE "SEE 2ND LINE FOLLOW" TO CORRECT-A                  DB2024.2
080400         PERFORM GEN-WRITE.                                       DB2024.2
080500     MOVE CONTENTS-1 TO PRINT-REC.                                DB2024.2
080600     PERFORM WRITE-LINE.                                          DB2024.2
080700     MOVE SEQ-REC-3 TO PRINT-REC.                                 DB2024.2
080800     PERFORM WRITE-LINE.                                          DB2024.2
080900 READ-TEST-2-INIT.                                                DB2024.2
081000     MOVE SPACES TO ITEM-1.                                       DB2024.2
081100     MOVE 0 TO KEY-1.                                             DB2024.2
081200     READ SEQ-FILE  AT END GO TO READ-TEST-2.                     DB2024.2
081300     GO TO READ-TEST-2-INIT.                                      DB2024.2
081400 READ-TEST-2-DELETE.                                              DB2024.2
081500     MOVE "DEBUG READ AT END" TO FEATURE.                         DB2024.2
081600     MOVE "READ-TEST-2" TO PAR-NAME.                              DB2024.2
081700     PERFORM DE-LETE.                                             DB2024.2
081800     PERFORM GEN-WRITE.                                           DB2024.2
081900     GO TO CLOSE-SEQ-FILE.                                        DB2024.2
082000 READ-TEST-2.                                                     DB2024.2
082100     MOVE "DEBUG READ AT END" TO FEATURE.                         DB2024.2
082200     MOVE "READ-TEST-2" TO PAR-NAME.                              DB2024.2
082300     IF KEY-1 IS EQUAL TO 0                                       DB2024.2
082400         PERFORM PASS                                             DB2024.2
082500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2024.2
082600     ELSE  PERFORM FAIL                                           DB2024.2
082700         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK.               DB2024.2
082800 GEN-WRITE.                                                       DB2024.2
082900     PERFORM PRINT-DETAIL.                                        DB2024.2
083000 CLOSE-SEQ-FILE.                                                  DB2024.2
083100     CLOSE SEQ-FILE.                                              DB2024.2
083200*TAPE-DUMP SECTION.                                               DB2024.2
083300*OPEN-FILE.                                                       DB2024.2
083400*    OPEN INPUT SEQ-FILE.                                         DB2024.2
083500*READ-FILE.                                                       DB2024.2
083600*    READ SEQ-FILE AT END GO TO CLOSE-FILE.                       DB2024.2
083700*    MOVE SEQ-REC-3 TO PRINT-REC.                                 DB2024.2
083800*    PERFORM WRITE-LINE.                                          DB2024.2
083900*    GO TO READ-FILE.                                             DB2024.2
084000*CLOSE-FILE.                                                      DB2024.2
084100*    CLOSE SEQ-FILE.                                              DB2024.2
084200 CCVS-EXIT SECTION.                                               DB2024.2
084300 CCVS-999999.                                                     DB2024.2
084400     GO TO CLOSE-FILES.                                           DB2024.2
      *END-OF,DB202A                                                                  
