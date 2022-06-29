      *HEADER,COBOL,DB203A                                                            
000100 IDENTIFICATION DIVISION.                                         DB2034.2
000200 PROGRAM-ID.                                                      DB2034.2
000300     DB203A.                                                      DB2034.2
000400 AUTHOR.                                                          DB2034.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB2034.2
000600 INSTALLATION.                                                    DB2034.2
000700     GENERAL SERVICES ADMINISTRATION                              DB2034.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB2034.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB2034.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB2034.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB2034.2
001200                                                                  DB2034.2
001300     PHONE   (703) 756-6153                                       DB2034.2
001400                                                                  DB2034.2
001500     " HIGH       ".                                              DB2034.2
001600 DATE-WRITTEN.                                                    DB2034.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB2034.2
001800     CREATION DATE     /    VALIDATION DATE                       DB2034.2
001900     "4.2 ".                                                      DB2034.2
002000 SECURITY.                                                        DB2034.2
002100     NONE.                                                        DB2034.2
002200*                                                                 DB2034.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB2034.2
002400*                                                                 DB2034.2
002500*                       PROGRAM ABSTRACT                          DB2034.2
002600*                                                                 DB2034.2
002700*    DB203A TESTS THE CAPABILITY OF THE DEBUG MODULE TO HANDLE    DB2034.2
002800*    DEBUGGING PROCEDURES WHICH ARE MONITORING I-O FUNCTIONS      DB2034.2
002900*    OF THE RELATIVE I-O OR INDEXED I-O MODULES.  THIS PROGRAM    DB2034.2
003000*    IS TO BE COMPILED AND EXECUTED WITH BOTH COMPILE AND OBJECT  DB2034.2
003100*    TIME DEBUGGING SWITCHES ON.  THE DEBUGGING PROCEDURES        DB2034.2
003200*    SHOULD BE INCLUDED IN COMPILATION AND GENERATE CODE.         DB2034.2
003300*    DURING EXECUTION, A FILE IS ASSIGNED IN DYNAMIC MODE,        DB2034.2
003400*    CREATED SEQUENTIALLY, AND ACCESSED BOTH SEQUENTIALLY         DB2034.2
003500*    AND RANDOMLY.  ITS RECORDS ARE 80 CHARACTERS IN LENGTH.      DB2034.2
003600*    EXECUTION OF "OPEN", "READ", "WRITE", "REWRITE", "START",    DB2034.2
003700*    AND "DELETE" FUNCTIONS SHOULD TRIGGER THE APPROPRIATE        DB2034.2
003800*    DEBUGGING PROCEDURES.                                        DB2034.2
003900*                                                                 DB2034.2
004000*                                                                 DB2034.2
004100*                                                                 DB2034.2
004200 ENVIRONMENT DIVISION.                                            DB2034.2
004300 CONFIGURATION SECTION.                                           DB2034.2
004400 SOURCE-COMPUTER.                                                 DB2034.2
004500     XXXXX082                                                     DB2034.2
004600         WITH DEBUGGING MODE.                                     DB2034.2
004700 OBJECT-COMPUTER.                                                 DB2034.2
004800     XXXXX083.                                                    DB2034.2
004900 SPECIAL-NAMES.                                                   DB2034.2
005000     XXXXX056                                                     DB2034.2
005100*      XXXXX056  REPLACE WITH DISPLAY IMPLEMENTOR NAME            DB2034.2
005200         IS THE-SYSTEM-PRINTER.                                   DB2034.2
005300 INPUT-OUTPUT SECTION.                                            DB2034.2
005400 FILE-CONTROL.                                                    DB2034.2
005500     SELECT PRINT-FILE ASSIGN TO                                  DB2034.2
005600     XXXXX055.                                                    DB2034.2
005700     SELECT IND-FILE ASSIGN TO                                    DB2034.2
005800     XXXXX024                                                     DB2034.2
005900*    XXXXX044                                                     DB2034.2
006000*      XXXXX044  REPLACE WITH INDEX-FILE-NAME (*OPT J ONLY)       DB2034.2
006100     ORGANIZATION IS INDEXED                                      DB2034.2
006200     ACCESS MODE IS DYNAMIC                                       DB2034.2
006300     RECORD KEY IS IND-KEY.                                       DB2034.2
006400 DATA DIVISION.                                                   DB2034.2
006500 FILE SECTION.                                                    DB2034.2
006600 FD  PRINT-FILE                                                   DB2034.2
006700     LABEL RECORDS                                                DB2034.2
006800     XXXXX084                                                     DB2034.2
006900     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB2034.2
007000 01  PRINT-REC PICTURE X(120).                                    DB2034.2
007100 01  DUMMY-RECORD PICTURE X(120).                                 DB2034.2
007200 FD  IND-FILE                                                     DB2034.2
007300C    VALUE OF                                                     DB2034.2
007400C    XXXXX074                                                     DB2034.2
007500*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB2034.2
007600C    IS                                                           DB2034.2
007700C    XXXXX075                                                     DB2034.2
007800*      XXXXX075  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB2034.2
007900*    XXXXX069                                                     DB2034.2
008000*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB2034.2
008100     LABEL RECORDS ARE STANDARD.                                  DB2034.2
008200 01  IND-REC-1.                                                   DB2034.2
008300     02  FILLER PIC X(128).                                       DB2034.2
008400     02  IND-KEY PIC XX.                                          DB2034.2
008500     02  FILLER PIC X(110).                                       DB2034.2
008600 01  IND-REC-2.                                                   DB2034.2
008700     02  IND-REC-1H PIC X(120).                                   DB2034.2
008800     02  IND-REC-2H PIC X(120).                                   DB2034.2
008900 WORKING-STORAGE SECTION.                                         DB2034.2
009000 01  ITEM-1.                                                      DB2034.2
009100     02  KEY-1 PIC 99.                                            DB2034.2
009200     02  LINE-1 PIC X(6).                                         DB2034.2
009300     02  NAME-1 PIC X(30).                                        DB2034.2
009400     02  UNQUAL-NAME-1 PIC X(30).                                 DB2034.2
009500     02  CONTENTS-1.                                              DB2034.2
009600         03  CONTENTS-1-1H PIC X(120).                            DB2034.2
009700         03  CONTENTS-1-2H PIC X(120).                            DB2034.2
009800     02  CONTENTS-REC.                                            DB2034.2
009900         03  CONTENTS-REC-1H PIC X(120).                          DB2034.2
010000         03  CONTENTS-REC-2H PIC X(120).                          DB2034.2
010100 01  FILE-RECORD-INFORMATION-REC.                                 DB2034.2
010200     03 FILE-RECORD-INFO-SKELETON.                                DB2034.2
010300        05 FILLER                 PICTURE X(48)       VALUE       DB2034.2
010400             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  DB2034.2
010500        05 FILLER                 PICTURE X(46)       VALUE       DB2034.2
010600             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    DB2034.2
010700        05 FILLER                 PICTURE X(26)       VALUE       DB2034.2
010800             ",LFIL=000000,ORG=  ,LBLR= ".                        DB2034.2
010900        05 FILLER                 PICTURE X(37)       VALUE       DB2034.2
011000             ",RECKEY=                             ".             DB2034.2
011100        05 FILLER                 PICTURE X(38)       VALUE       DB2034.2
011200             ",ALTKEY1=                             ".            DB2034.2
011300        05 FILLER                 PICTURE X(38)       VALUE       DB2034.2
011400             ",ALTKEY2=                             ".            DB2034.2
011500        05 FILLER                 PICTURE X(7)        VALUE SPACE.DB2034.2
011600     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              DB2034.2
011700        05 FILE-RECORD-INFO-P1-120.                               DB2034.2
011800           07 FILLER              PIC X(5).                       DB2034.2
011900           07 XFILE-NAME           PIC X(6).                      DB2034.2
012000           07 FILLER              PIC X(8).                       DB2034.2
012100           07 XRECORD-NAME         PIC X(6).                      DB2034.2
012200           07 FILLER              PIC X(1).                       DB2034.2
012300           07 REELUNIT-NUMBER     PIC 9(1).                       DB2034.2
012400           07 FILLER              PIC X(7).                       DB2034.2
012500           07 XRECORD-NUMBER       PIC 9(6).                      DB2034.2
012600           07 FILLER              PIC X(6).                       DB2034.2
012700           07 UPDATE-NUMBER       PIC 9(2).                       DB2034.2
012800           07 FILLER              PIC X(5).                       DB2034.2
012900           07 ODO-NUMBER          PIC 9(4).                       DB2034.2
013000           07 FILLER              PIC X(5).                       DB2034.2
013100           07 XPROGRAM-NAME        PIC X(5).                      DB2034.2
013200           07 FILLER              PIC X(7).                       DB2034.2
013300           07 XRECORD-LENGTH       PIC 9(6).                      DB2034.2
013400           07 FILLER              PIC X(7).                       DB2034.2
013500           07 CHARS-OR-RECORDS    PIC X(2).                       DB2034.2
013600           07 FILLER              PIC X(1).                       DB2034.2
013700           07 XBLOCK-SIZE          PIC 9(4).                      DB2034.2
013800           07 FILLER              PIC X(6).                       DB2034.2
013900           07 RECORDS-IN-FILE     PIC 9(6).                       DB2034.2
014000           07 FILLER              PIC X(5).                       DB2034.2
014100           07 XFILE-ORGANIZATION   PIC X(2).                      DB2034.2
014200           07 FILLER              PIC X(6).                       DB2034.2
014300           07 XLABEL-TYPE          PIC X(1).                      DB2034.2
014400        05 FILE-RECORD-INFO-P121-240.                             DB2034.2
014500           07 FILLER              PIC X(8).                       DB2034.2
014600           07 XRECORD-KEY          PIC X(29).                     DB2034.2
014700           07 FILLER              PIC X(9).                       DB2034.2
014800           07 ALTERNATE-KEY1      PIC X(29).                      DB2034.2
014900           07 FILLER              PIC X(9).                       DB2034.2
015000           07 ALTERNATE-KEY2      PIC X(29).                      DB2034.2
015100           07 FILLER              PIC X(7).                       DB2034.2
015200 01  TEST-RESULTS.                                                DB2034.2
015300     02 FILLER                    PICTURE X VALUE SPACE.          DB2034.2
015400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB2034.2
015500     02 FILLER                    PICTURE X VALUE SPACE.          DB2034.2
015600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB2034.2
015700     02 FILLER                    PICTURE X  VALUE SPACE.         DB2034.2
015800     02  PAR-NAME.                                                DB2034.2
015900       03 FILLER PICTURE X(12) VALUE SPACE.                       DB2034.2
016000       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB2034.2
016100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB2034.2
016200       03 FILLER PIC X(5) VALUE SPACE.                            DB2034.2
016300     02 FILLER PIC X(10) VALUE SPACE.                             DB2034.2
016400     02 RE-MARK PIC X(61).                                        DB2034.2
016500 01  TEST-COMPUTED.                                               DB2034.2
016600     02 FILLER PIC X(30) VALUE SPACE.                             DB2034.2
016700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB2034.2
016800     02 COMPUTED-X.                                               DB2034.2
016900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB2034.2
017000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB2034.2
017100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB2034.2
017200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB2034.2
017300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB2034.2
017400     03       CM-18V0 REDEFINES COMPUTED-A.                       DB2034.2
017500         04 COMPUTED-18V0                   PICTURE -9(18).       DB2034.2
017600         04 FILLER                          PICTURE X.            DB2034.2
017700     03 FILLER PIC X(50) VALUE SPACE.                             DB2034.2
017800 01  TEST-CORRECT.                                                DB2034.2
017900     02 FILLER PIC X(30) VALUE SPACE.                             DB2034.2
018000     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB2034.2
018100     02 CORRECT-X.                                                DB2034.2
018200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB2034.2
018300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB2034.2
018400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB2034.2
018500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB2034.2
018600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB2034.2
018700     03      CR-18V0 REDEFINES CORRECT-A.                         DB2034.2
018800         04 CORRECT-18V0                    PICTURE -9(18).       DB2034.2
018900         04 FILLER                          PICTURE X.            DB2034.2
019000     03 FILLER PIC X(50) VALUE SPACE.                             DB2034.2
019100 01  CCVS-C-1.                                                    DB2034.2
019200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB2034.2
019300-    "SS  PARAGRAPH-NAME                                          DB2034.2
019400-    "        REMARKS".                                           DB2034.2
019500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB2034.2
019600 01  CCVS-C-2.                                                    DB2034.2
019700     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2034.2
019800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB2034.2
019900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB2034.2
020000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB2034.2
020100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB2034.2
020200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB2034.2
020300 01  REC-CT PICTURE 99 VALUE ZERO.                                DB2034.2
020400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB2034.2
020500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB2034.2
020600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB2034.2
020700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB2034.2
020800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB2034.2
020900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB2034.2
021000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB2034.2
021100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB2034.2
021200 01  CCVS-H-1.                                                    DB2034.2
021300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB2034.2
021400     02 FILLER PICTURE X(67) VALUE                                DB2034.2
021500     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB2034.2
021600-    " SYSTEM".                                                   DB2034.2
021700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB2034.2
021800 01  CCVS-H-2.                                                    DB2034.2
021900     02 FILLER PICTURE X(52) VALUE IS                             DB2034.2
022000     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB2034.2
022100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB2034.2
022200     02 TEST-ID PICTURE IS X(9).                                  DB2034.2
022300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB2034.2
022400 01  CCVS-H-3.                                                    DB2034.2
022500     02  FILLER PICTURE X(34) VALUE                               DB2034.2
022600     " FOR OFFICIAL USE ONLY    ".                                DB2034.2
022700     02  FILLER PICTURE X(58) VALUE                               DB2034.2
022800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB2034.2
022900     02  FILLER PICTURE X(28) VALUE                               DB2034.2
023000     "  COPYRIGHT   1974 ".                                       DB2034.2
023100 01  CCVS-E-1.                                                    DB2034.2
023200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB2034.2
023300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB2034.2
023400     02 ID-AGAIN PICTURE IS X(9).                                 DB2034.2
023500     02 FILLER PICTURE X(45) VALUE IS                             DB2034.2
023600     " NTIS DISTRIBUTION COBOL 74".                               DB2034.2
023700 01  CCVS-E-2.                                                    DB2034.2
023800     02  FILLER                   PICTURE X(31)  VALUE            DB2034.2
023900     SPACE.                                                       DB2034.2
024000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB2034.2
024100     02 CCVS-E-2-2.                                               DB2034.2
024200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB2034.2
024300         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB2034.2
024400         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB2034.2
024500 01  CCVS-E-3.                                                    DB2034.2
024600     02  FILLER PICTURE X(22) VALUE                               DB2034.2
024700     " FOR OFFICIAL USE ONLY".                                    DB2034.2
024800     02  FILLER PICTURE X(12) VALUE SPACE.                        DB2034.2
024900     02  FILLER PICTURE X(58) VALUE                               DB2034.2
025000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB2034.2
025100     02  FILLER PICTURE X(13) VALUE SPACE.                        DB2034.2
025200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB2034.2
025300 01  CCVS-E-4.                                                    DB2034.2
025400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB2034.2
025500     02 FILLER PIC XXXX VALUE " OF ".                             DB2034.2
025600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB2034.2
025700     02 FILLER PIC X(40) VALUE                                    DB2034.2
025800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB2034.2
025900 01  XXINFO.                                                      DB2034.2
026000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB2034.2
026100     02 INFO-TEXT.                                                DB2034.2
026200     04 FILLER PIC X(20) VALUE SPACE.                             DB2034.2
026300     04 XXCOMPUTED PIC X(20).                                     DB2034.2
026400     04 FILLER PIC X(5) VALUE SPACE.                              DB2034.2
026500     04 XXCORRECT PIC X(20).                                      DB2034.2
026600 01  HYPHEN-LINE.                                                 DB2034.2
026700     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB2034.2
026800     02 FILLER PICTURE IS X(65) VALUE IS "************************DB2034.2
026900-    "*****************************************".                 DB2034.2
027000     02 FILLER PICTURE IS X(54) VALUE IS "************************DB2034.2
027100-    "******************************".                            DB2034.2
027200 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB2034.2
027300     "DB203A".                                                    DB2034.2
027400 PROCEDURE DIVISION.                                              DB2034.2
027500 DECLARATIVES.                                                    DB2034.2
027600 FILENAME-PROC SECTION.                                           DB2034.2
027700     USE FOR DEBUGGING ON IND-FILE  IND-REC-1.                    DB2034.2
027800 FILENAME-1.                                                      DB2034.2
027900     MOVE 1 TO KEY-1.                                             DB2034.2
028000     MOVE DEBUG-LINE TO LINE-1.                                   DB2034.2
028100     MOVE DEBUG-NAME TO  NAME-1  UNQUAL-NAME-1.                   DB2034.2
028200     MOVE DEBUG-CONTENTS TO CONTENTS-1.                           DB2034.2
028300     INSPECT UNQUAL-NAME-1  REPLACING CHARACTERS                  DB2034.2
028400         BY SPACES AFTER INITIAL SPACE.                           DB2034.2
028500 END DECLARATIVES.                                                DB2034.2
028600 CCVS1 SECTION.                                                   DB2034.2
028700 OPEN-FILES.                                                      DB2034.2
028800     OPEN     OUTPUT PRINT-FILE.                                  DB2034.2
028900     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB2034.2
029000     MOVE    SPACE TO TEST-RESULTS.                               DB2034.2
029100     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB2034.2
029200     GO TO CCVS1-EXIT.                                            DB2034.2
029300 CLOSE-FILES.                                                     DB2034.2
029400     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB2034.2
029500 TERMINATE-CCVS.                                                  DB2034.2
029600*    EXIT PROGRAM.                                                DB2034.2
029700*TERMINATE-CALL.                                                  DB2034.2
029800     STOP     RUN.                                                DB2034.2
029900 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB2034.2
030000 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB2034.2
030100 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB2034.2
030200 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB2034.2
030300     MOVE "****TEST DELETED****" TO RE-MARK.                      DB2034.2
030400 PRINT-DETAIL.                                                    DB2034.2
030500     IF REC-CT NOT EQUAL TO ZERO                                  DB2034.2
030600             MOVE "." TO PARDOT-X                                 DB2034.2
030700             MOVE REC-CT TO DOTVALUE.                             DB2034.2
030800     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB2034.2
030900     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB2034.2
031000        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB2034.2
031100          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB2034.2
031200     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB2034.2
031300     MOVE SPACE TO CORRECT-X.                                     DB2034.2
031400     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB2034.2
031500     MOVE     SPACE TO RE-MARK.                                   DB2034.2
031600 HEAD-ROUTINE.                                                    DB2034.2
031700     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2034.2
031800     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB2034.2
031900     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB2034.2
032000 COLUMN-NAMES-ROUTINE.                                            DB2034.2
032100     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2034.2
032200     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2034.2
032300     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB2034.2
032400 END-ROUTINE.                                                     DB2034.2
032500     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB2034.2
032600 END-RTN-EXIT.                                                    DB2034.2
032700     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB2034.2
032800 END-ROUTINE-1.                                                   DB2034.2
032900      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB2034.2
033000      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB2034.2
033100      ADD PASS-COUNTER TO ERROR-HOLD.                             DB2034.2
033200*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB2034.2
033300      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB2034.2
033400      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB2034.2
033500      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB2034.2
033600      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB2034.2
033700  END-ROUTINE-12.                                                 DB2034.2
033800      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB2034.2
033900     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB2034.2
034000         MOVE "NO " TO ERROR-TOTAL                                DB2034.2
034100         ELSE                                                     DB2034.2
034200         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB2034.2
034300     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB2034.2
034400     PERFORM WRITE-LINE.                                          DB2034.2
034500 END-ROUTINE-13.                                                  DB2034.2
034600     IF DELETE-CNT IS EQUAL TO ZERO                               DB2034.2
034700         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB2034.2
034800         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB2034.2
034900     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB2034.2
035000     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2034.2
035100      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB2034.2
035200          MOVE "NO " TO ERROR-TOTAL                               DB2034.2
035300      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB2034.2
035400      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB2034.2
035500      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB2034.2
035600     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB2034.2
035700 WRITE-LINE.                                                      DB2034.2
035800     ADD 1 TO RECORD-COUNT.                                       DB2034.2
035900*    IF RECORD-COUNT GREATER 50                                   DB2034.2
036000*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB2034.2
036100*        MOVE SPACE TO DUMMY-RECORD                               DB2034.2
036200*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB2034.2
036300*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB2034.2
036400*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB2034.2
036500*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB2034.2
036600*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB2034.2
036700*        MOVE ZERO TO RECORD-COUNT.                               DB2034.2
036800     PERFORM WRT-LN.                                              DB2034.2
036900 WRT-LN.                                                          DB2034.2
037000     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB2034.2
037100     MOVE SPACE TO DUMMY-RECORD.                                  DB2034.2
037200 BLANK-LINE-PRINT.                                                DB2034.2
037300     PERFORM WRT-LN.                                              DB2034.2
037400 FAIL-ROUTINE.                                                    DB2034.2
037500     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB2034.2
037600     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB2034.2
037700     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB2034.2
037800     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2034.2
037900     GO TO FAIL-ROUTINE-EX.                                       DB2034.2
038000 FAIL-ROUTINE-WRITE.                                              DB2034.2
038100     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB2034.2
038200     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB2034.2
038300 FAIL-ROUTINE-EX. EXIT.                                           DB2034.2
038400 BAIL-OUT.                                                        DB2034.2
038500     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB2034.2
038600     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB2034.2
038700 BAIL-OUT-WRITE.                                                  DB2034.2
038800     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB2034.2
038900     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB2034.2
039000 BAIL-OUT-EX. EXIT.                                               DB2034.2
039100 CCVS1-EXIT.                                                      DB2034.2
039200     EXIT.                                                        DB2034.2
039300 BEGIN-DB203A-TESTS SECTION.                                      DB2034.2
039400 SET-UP-REC-AREA.                                                 DB2034.2
039500     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO (1).      DB2034.2
039600     MOVE "IX-FD1" TO XFILE-NAME (1).                             DB2034.2
039700     MOVE "REC-1" TO XRECORD-NAME (1).                            DB2034.2
039800     MOVE ".XXX." TO XPROGRAM-NAME (1).                           DB2034.2
039900     MOVE 240 TO XRECORD-LENGTH (1).                              DB2034.2
040000     MOVE "RC" TO CHARS-OR-RECORDS (1).                           DB2034.2
040100     MOVE 1 TO XBLOCK-SIZE (1).                                   DB2034.2
040200     MOVE 5 TO RECORDS-IN-FILE (1).                               DB2034.2
040300     MOVE "IX" TO XFILE-ORGANIZATION (1).                         DB2034.2
040400     MOVE "S" TO XLABEL-TYPE (1).                                 DB2034.2
040500 OPEN-TEST-1-INIT.                                                DB2034.2
040600     MOVE 0 TO KEY-1.                                             DB2034.2
040700     MOVE "OPEN-TEST-1" TO PAR-NAME.                              DB2034.2
040800     MOVE "DEBUG OPEN OUTPUT" TO FEATURE.                         DB2034.2
040900 OPEN-TEST-1.                                                     DB2034.2
041000     OPEN OUTPUT IND-FILE.                                        DB2034.2
041100     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
041200         PERFORM PASS                                             DB2034.2
041300         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
041400     ELSE PERFORM FAIL                                            DB2034.2
041500         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
041600     PERFORM GEN-WRITE.                                           DB2034.2
041700     GO TO WRITE-TEST-1-INIT.                                     DB2034.2
041800 OPEN-TEST-1-DELETE.                                              DB2034.2
041900     PERFORM DE-LETE.                                             DB2034.2
042000     PERFORM GEN-WRITE.                                           DB2034.2
042100 WRITE-TEST-1-INIT.                                               DB2034.2
042200     MOVE 0 TO KEY-1.                                             DB2034.2
042300     MOVE "WRITE-TEST-1" TO PAR-NAME.                             DB2034.2
042400     MOVE "DEBUG WRITE RECORD" TO FEATURE.                        DB2034.2
042500 WRITE-TEST-1.                                                    DB2034.2
042600     MOVE 1 TO XRECORD-NUMBER (1).                                DB2034.2
042700     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
042800     MOVE "02" TO IND-KEY.                                        DB2034.2
042900     WRITE IND-REC-2 INVALID KEY GO TO ABORT-PGM.                 DB2034.2
043000     MOVE 2 TO XRECORD-NUMBER (1).                                DB2034.2
043100     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
043200     MOVE "04" TO IND-KEY.                                        DB2034.2
043300     WRITE IND-REC-2 INVALID KEY GO TO ABORT-PGM.                 DB2034.2
043400     MOVE 3 TO XRECORD-NUMBER (1).                                DB2034.2
043500     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
043600     MOVE "06" TO IND-KEY.                                        DB2034.2
043700     WRITE IND-REC-2 INVALID KEY GO TO ABORT-PGM.                 DB2034.2
043800     MOVE 4 TO XRECORD-NUMBER (1).                                DB2034.2
043900     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
044000     MOVE "08" TO IND-KEY.                                        DB2034.2
044100     WRITE IND-REC-2 INVALID KEY GO TO ABORT-PGM.                 DB2034.2
044200     MOVE 5 TO XRECORD-NUMBER (1).                                DB2034.2
044300     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
044400     MOVE "10" TO IND-KEY.                                        DB2034.2
044500     WRITE IND-REC-1 INVALID KEY GO TO ABORT-PGM.                 DB2034.2
044600     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
044700         PERFORM PASS                                             DB2034.2
044800         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
044900     ELSE PERFORM FAIL                                            DB2034.2
045000         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
045100     PERFORM GEN-WRITE.                                           DB2034.2
045200     GO TO CLOSE-TEST-1-INIT.                                     DB2034.2
045300 WRITE-TEST-1-DELETE.                                             DB2034.2
045400     PERFORM DE-LETE.                                             DB2034.2
045500     PERFORM GEN-WRITE.                                           DB2034.2
045600 CLOSE-TEST-1-INIT.                                               DB2034.2
045700     MOVE 0 TO KEY-1.                                             DB2034.2
045800     MOVE "CLOSE-TEST-1" TO PAR-NAME.                             DB2034.2
045900     MOVE "DEBUG CLOSE FILE" TO FEATURE.                          DB2034.2
046000 CLOSE-TEST-1.                                                    DB2034.2
046100     CLOSE IND-FILE.                                              DB2034.2
046200     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
046300         PERFORM PASS                                             DB2034.2
046400         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
046500     ELSE PERFORM FAIL                                            DB2034.2
046600         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
046700     PERFORM GEN-WRITE.                                           DB2034.2
046800     GO TO OPEN-TEST-2-INIT.                                      DB2034.2
046900 CLOSE-TEST-1-DELETE.                                             DB2034.2
047000     PERFORM DE-LETE.                                             DB2034.2
047100     PERFORM GEN-WRITE.                                           DB2034.2
047200 OPEN-TEST-2-INIT.                                                DB2034.2
047300     MOVE 0 TO KEY-1.                                             DB2034.2
047400     MOVE "OPEN-TEST-2" TO PAR-NAME.                              DB2034.2
047500     MOVE "DEBUG OPEN I-O" TO FEATURE.                            DB2034.2
047600 OPEN-TEST-2.                                                     DB2034.2
047700     OPEN I-O IND-FILE.                                           DB2034.2
047800     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
047900         PERFORM PASS                                             DB2034.2
048000         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
048100     ELSE  PERFORM FAIL                                           DB2034.2
048200         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
048300     PERFORM GEN-WRITE.                                           DB2034.2
048400     GO TO READ-TEST-1-INIT.                                      DB2034.2
048500 OPEN-TEST-2-DELETE.                                              DB2034.2
048600     PERFORM DE-LETE.                                             DB2034.2
048700     PERFORM GEN-WRITE.                                           DB2034.2
048800 READ-TEST-1-INIT.                                                DB2034.2
048900     MOVE SPACES TO ITEM-1.                                       DB2034.2
049000     MOVE 0 TO KEY-1.                                             DB2034.2
049100     MOVE "READ-TEST-1" TO PAR-NAME.                              DB2034.2
049200     MOVE "DEBUG READ RANDOM" TO FEATURE.                         DB2034.2
049300 READ-TEST-1.                                                     DB2034.2
049400     MOVE "04" TO IND-KEY.                                        DB2034.2
049500******************************************************************DB2034.2
049600*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2034.2
049700*    "READ-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT     *DB2034.2
049800*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS, "READ       *DB2034.2
049900*    IND-FILE KEY IS IND-KEY INVALID KEY GO TO ABORT-PGM.".      *DB2034.2
050000******************************************************************DB2034.2
050100     READ IND-FILE  KEY IS IND-KEY                                DB2034.2
050200         INVALID KEY GO TO ABORT-PGM.                             DB2034.2
050300     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
050400         PERFORM PASS                                             DB2034.2
050500         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
050600         PERFORM GEN-WRITE                                        DB2034.2
050700         GO TO READ-TEST-1A                                       DB2034.2
050800     ELSE  PERFORM FAIL                                           DB2034.2
050900         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2034.2
051000         PERFORM GEN-WRITE                                        DB2034.2
051100         PERFORM DELETE-READ-TEST-1-SUBTESTS                      DB2034.2
051200         GO TO REWRITE-TEST-1-INIT.                               DB2034.2
051300 READ-TEST-1-DELETE.                                              DB2034.2
051400     PERFORM DE-LETE.                                             DB2034.2
051500     PERFORM GEN-WRITE.                                           DB2034.2
051600     PERFORM DELETE-READ-TEST-1-SUBTESTS.                         DB2034.2
051700     GO TO READ-TEST-2-INIT.                                      DB2034.2
051800 DELETE-READ-TEST-1-SUBTESTS.                                     DB2034.2
051900     MOVE "READ-TEST-1A" TO PAR-NAME.                             DB2034.2
052000     PERFORM DE-LETE.                                             DB2034.2
052100     PERFORM GEN-WRITE.                                           DB2034.2
052200     MOVE "READ-TEST-1B" TO PAR-NAME.                             DB2034.2
052300     PERFORM DE-LETE.                                             DB2034.2
052400     PERFORM GEN-WRITE.                                           DB2034.2
052500     MOVE "READ-TEST-1C" TO PAR-NAME.                             DB2034.2
052600     PERFORM DE-LETE.                                             DB2034.2
052700     PERFORM GEN-WRITE.                                           DB2034.2
052800 READ-TEST-1A.                                                    DB2034.2
052900     MOVE "READ-TEST-1A" TO PAR-NAME.                             DB2034.2
053000     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2034.2
053100     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2034.2
053200     MOVE LINE-1 TO COMPUTED-A.                                   DB2034.2
053300     PERFORM INSPT.                                               DB2034.2
053400     PERFORM GEN-WRITE.                                           DB2034.2
053500 READ-TEST-1B.                                                    DB2034.2
053600     MOVE "READ-TEST-1B" TO PAR-NAME.                             DB2034.2
053700     IF UNQUAL-NAME-1 IS EQUAL TO "IND-FILE"                      DB2034.2
053800         PERFORM PASS                                             DB2034.2
053900     ELSE  PERFORM FAIL                                           DB2034.2
054000         MOVE "IND-FILE" TO CORRECT-A                             DB2034.2
054100         MOVE NAME-1 TO COMPUTED-A.                               DB2034.2
054200     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2034.2
054300     PERFORM GEN-WRITE.                                           DB2034.2
054400 READ-TEST-1C.                                                    DB2034.2
054500     MOVE "READ-TEST-1C" TO PAR-NAME.                             DB2034.2
054600     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2034.2
054700     IF CONTENTS-1 IS EQUAL TO IND-REC-2                          DB2034.2
054800         PERFORM PASS                                             DB2034.2
054900         PERFORM GEN-WRITE                                        DB2034.2
055000         GO TO READ-TEST-2-INIT                                   DB2034.2
055100     ELSE  PERFORM FAIL                                           DB2034.2
055200         MOVE "LINES 1 AND 3 BELOW" TO COMPUTED-A                 DB2034.2
055300         MOVE "LINES 2 AND 4 BELOW" TO CORRECT-A.                 DB2034.2
055400     PERFORM GEN-WRITE.                                           DB2034.2
055500     MOVE CONTENTS-1-1H TO PRINT-REC.                             DB2034.2
055600     PERFORM WRITE-LINE.                                          DB2034.2
055700     MOVE IND-REC-1H TO PRINT-REC.                                DB2034.2
055800     PERFORM WRITE-LINE.                                          DB2034.2
055900     MOVE CONTENTS-1-2H TO PRINT-REC.                             DB2034.2
056000     PERFORM WRITE-LINE.                                          DB2034.2
056100     MOVE IND-REC-2H TO PRINT-REC.                                DB2034.2
056200     PERFORM WRITE-LINE.                                          DB2034.2
056300 READ-TEST-2-INIT.                                                DB2034.2
056400     MOVE SPACES TO ITEM-1.                                       DB2034.2
056500     MOVE 0 TO KEY-1.                                             DB2034.2
056600     MOVE "READ-TEST-2" TO PAR-NAME.                              DB2034.2
056700     MOVE "DEBUG READ INV KEY" TO FEATURE.                        DB2034.2
056800 READ-TEST-2.                                                     DB2034.2
056900     MOVE "05" TO IND-KEY.                                        DB2034.2
057000     READ IND-FILE  KEY IS IND-KEY                                DB2034.2
057100         INVALID KEY  GO TO READ-TEST-2-CONT.                     DB2034.2
057200 READ-TEST-2-DELETE.                                              DB2034.2
057300     PERFORM DE-LETE.                                             DB2034.2
057400     PERFORM GEN-WRITE.                                           DB2034.2
057500     GO TO REWRITE-TEST-1-INIT.                                   DB2034.2
057600 READ-TEST-2-CONT.                                                DB2034.2
057700     IF KEY-1 IS EQUAL TO 0                                       DB2034.2
057800         PERFORM PASS                                             DB2034.2
057900         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2034.2
058000     ELSE  PERFORM FAIL                                           DB2034.2
058100         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK.               DB2034.2
058200     PERFORM GEN-WRITE.                                           DB2034.2
058300 REWRITE-TEST-1-INIT.                                             DB2034.2
058400     MOVE SPACES TO ITEM-1.                                       DB2034.2
058500     MOVE 0 TO KEY-1.                                             DB2034.2
058600     MOVE "REWRITE-TEST-1" TO PAR-NAME.                           DB2034.2
058700     MOVE "DEBUG REWRITE RECORD" TO FEATURE.                      DB2034.2
058800 REWRITE-TEST-1.                                                  DB2034.2
058900     MOVE 2 TO XRECORD-NUMBER (1).                                DB2034.2
059000     MOVE 1 TO UPDATE-NUMBER (1).                                 DB2034.2
059100     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
059200     MOVE "04" TO IND-KEY.                                        DB2034.2
059300     MOVE IND-REC-2 TO CONTENTS-REC.                              DB2034.2
059400******************************************************************DB2034.2
059500*    THE DEBUG-LINE (INSPT) TEST NAMED IN THE OUTPUT REPORT AS   *DB2034.2
059600*    "REWRITE-TEST-1A" SHOULD POINT TO THE EXECUTABLE STATEMENT  *DB2034.2
059700*    WHICH FOLLOWS THIS COMMENT SET AND WHICH READS,             *DB2034.2
059800*    "REWRITE IND-REC-1  INVALID KEY  GO TO ABORT-PGM.".         *DB2034.2
059900******************************************************************DB2034.2
060000     REWRITE IND-REC-1  INVALID KEY  GO TO ABORT-PGM.             DB2034.2
060100     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
060200         PERFORM PASS                                             DB2034.2
060300         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
060400         PERFORM GEN-WRITE                                        DB2034.2
060500         GO TO REWRITE-TEST-1A                                    DB2034.2
060600     ELSE  PERFORM FAIL                                           DB2034.2
060700         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2034.2
060800         PERFORM GEN-WRITE                                        DB2034.2
060900         PERFORM DELETE-REWRITE-TEST-1-SUBTESTS                   DB2034.2
061000         GO TO REWRITE-TEST-2-INIT.                               DB2034.2
061100 REWRITE-TEST-1-DELETE.                                           DB2034.2
061200     PERFORM DE-LETE.                                             DB2034.2
061300     PERFORM GEN-WRITE.                                           DB2034.2
061400     PERFORM DELETE-REWRITE-TEST-1-SUBTESTS.                      DB2034.2
061500     GO TO REWRITE-TEST-2-INIT.                                   DB2034.2
061600 DELETE-REWRITE-TEST-1-SUBTESTS.                                  DB2034.2
061700     MOVE "REWRITE-TEST-1A" TO PAR-NAME.                          DB2034.2
061800     PERFORM DE-LETE.                                             DB2034.2
061900     PERFORM GEN-WRITE.                                           DB2034.2
062000     MOVE "REWRITE-TEST-1B" TO PAR-NAME.                          DB2034.2
062100     PERFORM DE-LETE.                                             DB2034.2
062200     PERFORM GEN-WRITE.                                           DB2034.2
062300     MOVE "REWRITE-TEST-1C" TO PAR-NAME.                          DB2034.2
062400     PERFORM DE-LETE.                                             DB2034.2
062500     PERFORM GEN-WRITE.                                           DB2034.2
062600 REWRITE-TEST-1A.                                                 DB2034.2
062700     MOVE "REWRITE-TEST-1A" TO PAR-NAME.                          DB2034.2
062800     MOVE "DEBUG-LINE; SEE NEXT LINE" TO RE-MARK.                 DB2034.2
062900     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB2034.2
063000     MOVE LINE-1 TO COMPUTED-A.                                   DB2034.2
063100     PERFORM INSPT.                                               DB2034.2
063200     PERFORM GEN-WRITE.                                           DB2034.2
063300 REWRITE-TEST-1B.                                                 DB2034.2
063400     MOVE "REWRITE-TEST-1B" TO PAR-NAME.                          DB2034.2
063500     IF UNQUAL-NAME-1 IS EQUAL TO "IND-REC-1"                     DB2034.2
063600         PERFORM PASS                                             DB2034.2
063700     ELSE  PERFORM FAIL                                           DB2034.2
063800         MOVE "IND-REC-1" TO CORRECT-A                            DB2034.2
063900         MOVE NAME-1 TO COMPUTED-A.                               DB2034.2
064000     MOVE "DEBUG-NAME" TO RE-MARK.                                DB2034.2
064100     PERFORM GEN-WRITE.                                           DB2034.2
064200 REWRITE-TEST-1C.                                                 DB2034.2
064300     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2034.2
064400     MOVE "REWRITE-TEST-1C" TO PAR-NAME.                          DB2034.2
064500     IF CONTENTS-1 IS EQUAL TO CONTENTS-REC                       DB2034.2
064600         PERFORM PASS                                             DB2034.2
064700         PERFORM GEN-WRITE                                        DB2034.2
064800         GO TO REWRITE-TEST-2-INIT                                DB2034.2
064900     ELSE  PERFORM FAIL                                           DB2034.2
065000         MOVE "LINES 1 AND 3 BELOW" TO COMPUTED-A                 DB2034.2
065100         MOVE "LINES 2 AND 4 BELOW" TO CORRECT-A                  DB2034.2
065200         PERFORM GEN-WRITE.                                       DB2034.2
065300     MOVE CONTENTS-1-1H TO PRINT-REC.                             DB2034.2
065400     PERFORM WRITE-LINE.                                          DB2034.2
065500     MOVE CONTENTS-REC-1H TO PRINT-REC.                           DB2034.2
065600     PERFORM WRITE-LINE.                                          DB2034.2
065700     MOVE CONTENTS-1-2H TO PRINT-REC.                             DB2034.2
065800     PERFORM WRITE-LINE.                                          DB2034.2
065900     MOVE CONTENTS-REC-2H TO PRINT-REC.                           DB2034.2
066000     PERFORM WRITE-LINE.                                          DB2034.2
066100 REWRITE-TEST-2-INIT.                                             DB2034.2
066200     MOVE SPACES TO ITEM-1.                                       DB2034.2
066300     MOVE 0 TO KEY-1.                                             DB2034.2
066400     MOVE "REWRITE-TEST-2" TO PAR-NAME.                           DB2034.2
066500     MOVE "DEBUG REWRITE INVLID" TO FEATURE.                      DB2034.2
066600 REWRITE-TEST-2.                                                  DB2034.2
066700     MOVE 6 TO XRECORD-NUMBER (1).                                DB2034.2
066800     MOVE 1 TO UPDATE-NUMBER (1).                                 DB2034.2
066900     MOVE FILE-RECORD-INFO (1) TO IND-REC-2.                      DB2034.2
067000     MOVE "03" TO IND-KEY.                                        DB2034.2
067100     REWRITE IND-REC-1  INVALID KEY  GO TO REWRITE-TEST-2-CONT.   DB2034.2
067200 REWRITE-TEST-2-DELETE.                                           DB2034.2
067300     PERFORM DE-LETE.                                             DB2034.2
067400     PERFORM GEN-WRITE.                                           DB2034.2
067500     GO TO START-TEST-1-INIT.                                     DB2034.2
067600 REWRITE-TEST-2-CONT.                                             DB2034.2
067700     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
067800         PERFORM PASS                                             DB2034.2
067900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
068000     ELSE  PERFORM FAIL                                           DB2034.2
068100         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
068200     PERFORM GEN-WRITE.                                           DB2034.2
068300 START-TEST-1-INIT.                                               DB2034.2
068400     MOVE SPACES TO ITEM-1.                                       DB2034.2
068500     MOVE 0 TO KEY-1.                                             DB2034.2
068600     MOVE "START-TEST-1" TO PAR-NAME.                             DB2034.2
068700     MOVE "DEBUG START FILENAME" TO FEATURE.                      DB2034.2
068800 START-TEST-1.                                                    DB2034.2
068900     MOVE "05" TO IND-KEY.                                        DB2034.2
069000     START IND-FILE  KEY IS GREATER THAN IND-KEY                  DB2034.2
069100         INVALID KEY  GO TO ABORT-PGM.                            DB2034.2
069200     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
069300         PERFORM PASS                                             DB2034.2
069400         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
069500         PERFORM GEN-WRITE                                        DB2034.2
069600         GO TO START-TEST-1A                                      DB2034.2
069700     ELSE  PERFORM FAIL                                           DB2034.2
069800         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2034.2
069900         PERFORM GEN-WRITE                                        DB2034.2
070000         PERFORM DELETE-START-TEST-1-SUBTEST                      DB2034.2
070100         GO TO START-TEST-2-INIT.                                 DB2034.2
070200 START-TEST-1-DELETE.                                             DB2034.2
070300     PERFORM DE-LETE.                                             DB2034.2
070400     PERFORM GEN-WRITE.                                           DB2034.2
070500     PERFORM DELETE-START-TEST-1-SUBTEST.                         DB2034.2
070600     GO TO START-TEST-2-INIT.                                     DB2034.2
070700 DELETE-START-TEST-1-SUBTEST.                                     DB2034.2
070800     MOVE "START-TEST-1A" TO PAR-NAME.                            DB2034.2
070900     PERFORM DE-LETE.                                             DB2034.2
071000     PERFORM GEN-WRITE.                                           DB2034.2
071100 START-TEST-1A.                                                   DB2034.2
071200     MOVE "START-TEST-1A" TO PAR-NAME.                            DB2034.2
071300     IF CONTENTS-1 IS EQUAL TO SPACES                             DB2034.2
071400         PERFORM PASS                                             DB2034.2
071500     ELSE  PERFORM FAIL                                           DB2034.2
071600         MOVE CONTENTS-1 TO COMPUTED-A                            DB2034.2
071700         MOVE "(SPACES)" TO CORRECT-A.                            DB2034.2
071800     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2034.2
071900     PERFORM GEN-WRITE.                                           DB2034.2
072000 START-TEST-2-INIT.                                               DB2034.2
072100     MOVE 0 TO KEY-1.                                             DB2034.2
072200     MOVE "START-TEST-2" TO PAR-NAME.                             DB2034.2
072300     MOVE "DEBUG START INV KEY" TO FEATURE.                       DB2034.2
072400 START-TEST-2.                                                    DB2034.2
072500     MOVE "12" TO IND-KEY.                                        DB2034.2
072600     START IND-FILE  KEY IS GREATER THAN IND-KEY                  DB2034.2
072700         INVALID KEY  GO TO START-TEST-2-CONT.                    DB2034.2
072800 START-TEST-2-DELETE.                                             DB2034.2
072900     PERFORM DE-LETE.                                             DB2034.2
073000     PERFORM GEN-WRITE.                                           DB2034.2
073100     GO TO DELETE-TEST-1-INIT.                                    DB2034.2
073200 START-TEST-2-CONT.                                               DB2034.2
073300     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
073400         PERFORM PASS                                             DB2034.2
073500         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
073600     ELSE  PERFORM FAIL                                           DB2034.2
073700         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
073800     PERFORM GEN-WRITE.                                           DB2034.2
073900 DELETE-TEST-1-INIT.                                              DB2034.2
074000     MOVE SPACES TO ITEM-1.                                       DB2034.2
074100     MOVE 0 TO KEY-1.                                             DB2034.2
074200     MOVE "DELETE-TEST-1" TO PAR-NAME.                            DB2034.2
074300     MOVE "DEBUG DELETE FILE" TO FEATURE.                         DB2034.2
074400 DELETE-TEST-1.                                                   DB2034.2
074500     MOVE "06" TO IND-KEY.                                        DB2034.2
074600     DELETE IND-FILE INVALID KEY GO TO ABORT-PGM.                 DB2034.2
074700     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
074800         PERFORM PASS                                             DB2034.2
074900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
075000         PERFORM GEN-WRITE                                        DB2034.2
075100         GO TO DELETE-TEST-1A                                     DB2034.2
075200     ELSE  PERFORM FAIL                                           DB2034.2
075300         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK                DB2034.2
075400         PERFORM GEN-WRITE                                        DB2034.2
075500         PERFORM DELETE-DELETE-TEST-1-SUBTEST                     DB2034.2
075600         GO TO DELETE-TEST-2-INIT.                                DB2034.2
075700 DELETE-TEST-1-DELETE.                                            DB2034.2
075800     PERFORM DE-LETE.                                             DB2034.2
075900     PERFORM GEN-WRITE.                                           DB2034.2
076000     PERFORM DELETE-DELETE-TEST-1-SUBTEST.                        DB2034.2
076100     GO TO DELETE-TEST-2-INIT.                                    DB2034.2
076200 DELETE-DELETE-TEST-1-SUBTEST.                                    DB2034.2
076300     MOVE "DELETE-TEST-1A" TO PAR-NAME.                           DB2034.2
076400     PERFORM DE-LETE.                                             DB2034.2
076500     PERFORM GEN-WRITE.                                           DB2034.2
076600 DELETE-TEST-1A.                                                  DB2034.2
076700     MOVE "DELETE-TEST-1A" TO PAR-NAME.                           DB2034.2
076800     IF CONTENTS-1 IS EQUAL TO SPACES                             DB2034.2
076900         PERFORM PASS                                             DB2034.2
077000     ELSE  PERFORM FAIL                                           DB2034.2
077100         MOVE "(SPACES)" TO CORRECT-A                             DB2034.2
077200         MOVE CONTENTS-1 TO COMPUTED-A.                           DB2034.2
077300     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB2034.2
077400     PERFORM GEN-WRITE.                                           DB2034.2
077500 DELETE-TEST-2-INIT.                                              DB2034.2
077600     MOVE "DELETE-TEST-2" TO PAR-NAME.                            DB2034.2
077700     MOVE "DEBUG DELETE INV KEY" TO FEATURE.                      DB2034.2
077800     MOVE 0 TO KEY-1.                                             DB2034.2
077900 DELETE-TEST-2.                                                   DB2034.2
078000     MOVE "07" TO IND-KEY.                                        DB2034.2
078100     DELETE IND-FILE INVALID KEY GO TO DELETE-TEST-2-CONT.        DB2034.2
078200 DELETE-TEST-2-DELETE.                                            DB2034.2
078300     PERFORM DE-LETE.                                             DB2034.2
078400     PERFORM GEN-WRITE.                                           DB2034.2
078500     GO TO CLOSE-IND-FILE.                                        DB2034.2
078600 DELETE-TEST-2-CONT.                                              DB2034.2
078700     IF KEY-1 IS EQUAL TO 1                                       DB2034.2
078800         PERFORM PASS                                             DB2034.2
078900         MOVE "DEBUG PROC WAS EXECUTED" TO RE-MARK                DB2034.2
079000     ELSE  PERFORM FAIL                                           DB2034.2
079100         MOVE "DEBUG PROC NOT EXECUTED" TO RE-MARK.               DB2034.2
079200     PERFORM GEN-WRITE.                                           DB2034.2
079300     GO TO CLOSE-IND-FILE.                                        DB2034.2
079400 GEN-WRITE.                                                       DB2034.2
079500     PERFORM PRINT-DETAIL.                                        DB2034.2
079600 ABORT-PGM.                                                       DB2034.2
079700     DISPLAY "INDEXED I-O MODULE - FILE HANDLING ERROR - PROGRAM ADB2034.2
079800-            "BORTED."  UPON THE-SYSTEM-PRINTER.                  DB2034.2
079900 CLOSE-IND-FILE.                                                  DB2034.2
080000     CLOSE IND-FILE.                                              DB2034.2
080100*OPEN-IND-FILE.                                                   DB2034.2
080200*    OPEN INPUT IND-FILE.                                         DB2034.2
080300*    MOVE SPACES TO PRINT-REC.                                    DB2034.2
080400*    PERFORM WRITE-LINE.                                          DB2034.2
080500*    MOVE " DUMP OF IND-FILE FOLLOWS" TO PRINT-REC.               DB2034.2
080600*    PERFORM WRITE-LINE.                                          DB2034.2
080700*READ-IND-FILE.                                                   DB2034.2
080800*    READ IND-FILE NEXT RECORD  AT END GO TO CLOSE-FILE-DUMP.     DB2034.2
080900*    MOVE IND-REC-1H TO PRINT-REC.                                DB2034.2
081000*    PERFORM WRITE-LINE.                                          DB2034.2
081100*    MOVE IND-REC-2H TO PRINT-REC.                                DB2034.2
081200*    PERFORM WRITE-LINE.                                          DB2034.2
081300*    GO TO READ-IND-FILE.                                         DB2034.2
081400*CLOSE-FILE-DUMP.                                                 DB2034.2
081500*    CLOSE IND-FILE.                                              DB2034.2
081600 CCVS-EXIT SECTION.                                               DB2034.2
081700 CCVS-999999.                                                     DB2034.2
081800     GO TO CLOSE-FILES.                                           DB2034.2
      *END-OF,DB203A                                                                  
