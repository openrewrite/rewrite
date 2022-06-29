      *HEADER,COBOL,DB104A                                                            
000100 IDENTIFICATION DIVISION.                                         DB1044.2
000200 PROGRAM-ID.                                                      DB1044.2
000300     DB104A.                                                      DB1044.2
000400 AUTHOR.                                                          DB1044.2
000500     FEDERAL COMPILER TESTING CENTER.                             DB1044.2
000600 INSTALLATION.                                                    DB1044.2
000700     GENERAL SERVICES ADMINISTRATION                              DB1044.2
000800*     AUTOMATED DATA AND TELECOMMUNICATION SERVICE.                DB1044.2
000900     SOFTWARE DEVELOPMENT OFFICE.                                 DB1044.2
001000     5203 LEESBURG PIKE  SUITE 1100                               DB1044.2
001100     FALLS CHURCH VIRGINIA 22041.                                 DB1044.2
001200                                                                  DB1044.2
001300     PHONE   (703) 756-6153                                       DB1044.2
001400                                                                  DB1044.2
001500     " HIGH       ".                                              DB1044.2
001600 DATE-WRITTEN.                                                    DB1044.2
001700     CCVS-74 VERSION 4.0 - 1980 JULY 1.                           DB1044.2
001800     CREATION DATE     /    VALIDATION DATE                       DB1044.2
001900     "4.2 ".                                                      DB1044.2
002000 SECURITY.                                                        DB1044.2
002100     NONE.                                                        DB1044.2
002200*                                                                 DB1044.2
002300*    *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *  *DB1044.2
002400*                                                                 DB1044.2
002500*                       PROGRAM ABSTRACT                          DB1044.2
002600*                                                                 DB1044.2
002700*    DB104A TESTS THE CAPABILITY OF THE DEBUG MODULE TO HANDLE    DB1044.2
002800*    PROCEDURES TIED TO SORT INPUT, SORT OUTPUT, AND FILE         DB1044.2
002900*    DECLARATIVE PROCEDURES.  THIS PROGRAM IS TO BE COMPILED AND  DB1044.2
003000*    EXECUTED WITH BOTH COMPILE AND OBJECT TIME DEBUGGING         DB1044.2
003100*    SWITCHES ENABLED.  THE PROGRAM FIRST BUILDS A SEQUENTIAL     DB1044.2
003200*    FILE CONTAINING 99 EIGHTY CHARACTER RECORDS.  THIS FILE      DB1044.2
003300*    IS THEN SORTED.                                              DB1044.2
003400*                                                                 DB1044.2
003500*    ALL DEBUGGING PROCEDURES SHOULD BE INCLUDED IN COMPILATION   DB1044.2
003600*    AND GENERATE CODE.  BEFORE BEGINNING EXECUTION OF THE OBJECT DB1044.2
003700*    PROGRAM, THE JOB CONTROL LANGUAGE NECESSARY TO ACTIVATE      DB1044.2
003800*    THE OBJECT TIME DEBUGGING SWITCH MUST BE SUBMITTED.          DB1044.2
003900*                                                                 DB1044.2
004000*    EXECUTION OF THE PROGRAM"S SORT SHOULD TRIGGER DEBUGGING     DB1044.2
004100*    PROCEDURES AT THE BEGINNING OF THE SORT INPUT AND SORT       DB1044.2
004200*    OUTPUT PROCEDURES.  DURING EXECUTION OF THE SORT INPUT       DB1044.2
004300*    PROCEDURE, END-OF-FILE CONDITION ON THE INPUT FILE SHOULD    DB1044.2
004400*    TRIGGER A DECLARATIVE PROCEDURE ASSOCIATED WITH THE FILE,    DB1044.2
004500*    AND THIS IN TURN SHOULD CAUSE EXECUTION OF A DEBUGGING       DB1044.2
004600*    PROCEDURE MONITORING THE FILE DECLARATIVE PROCEDURE.         DB1044.2
004700*                                                                 DB1044.2
004800*    THE PERFORMANCE OF THE SORT VERB IS NOT CHECKED IN DB104.    DB1044.2
004900*                                                                 DB1044.2
005000*                                                                 DB1044.2
005100*                                                                 DB1044.2
005200 ENVIRONMENT DIVISION.                                            DB1044.2
005300 CONFIGURATION SECTION.                                           DB1044.2
005400 SOURCE-COMPUTER.                                                 DB1044.2
005500     XXXXX082                                                     DB1044.2
005600         WITH DEBUGGING MODE.                                     DB1044.2
005700 OBJECT-COMPUTER.                                                 DB1044.2
005800     XXXXX083.                                                    DB1044.2
005900 INPUT-OUTPUT SECTION.                                            DB1044.2
006000 FILE-CONTROL.                                                    DB1044.2
006100     SELECT PRINT-FILE ASSIGN TO                                  DB1044.2
006200     XXXXX055.                                                    DB1044.2
006300     SELECT GEN-FILE ASSIGN TO                                    DB1044.2
006400     XXXXX014                                                     DB1044.2
006500     FILE STATUS IS GEN-STATUS.                                   DB1044.2
006600*      XXXXX014  REPLACE WITH SEQUENTIAL ACCESS SCRATCH FILE NAME DB1044.2
006700     SELECT SORT-FILE ASSIGN TO                                   DB1044.2
006800     XXXXX027.                                                    DB1044.2
006900*      XXXXX27  REPLACE WITH SORT FILE NAME                       DB1044.2
007000 DATA DIVISION.                                                   DB1044.2
007100 FILE SECTION.                                                    DB1044.2
007200 FD  PRINT-FILE                                                   DB1044.2
007300     LABEL RECORDS                                                DB1044.2
007400     XXXXX084                                                     DB1044.2
007500     DATA RECORD IS PRINT-REC DUMMY-RECORD.                       DB1044.2
007600 01  PRINT-REC PICTURE X(120).                                    DB1044.2
007700 01  DUMMY-RECORD PICTURE X(120).                                 DB1044.2
007800 FD  GEN-FILE                                                     DB1044.2
007900C    VALUE OF                                                     DB1044.2
008000C    XXXXX074                                                     DB1044.2
008100*      XXXXX074  REPLACE WITH IMPLEMENTOR NAME (*OPT C ONLY)      DB1044.2
008200C    IS                                                           DB1044.2
008300C    XXXXX075                                                     DB1044.2
008400*      XXXXX075  REPLACE WITH VALUE CLAUSE OBJECT (*OPT C ONLY)   DB1044.2
008500*    XXXXX069                                                     DB1044.2
008600*      XXXXX069  REPLACE WITH ADDITIONAL INFO (*OPT G ONLY)       DB1044.2
008700     LABEL RECORD IS STANDARD.                                    DB1044.2
008800 01  GEN-REC PIC X(80).                                           DB1044.2
008900 SD  SORT-FILE.                                                   DB1044.2
009000 01  SORT-REC.                                                    DB1044.2
009100     02  FILLER PIC X(34).                                        DB1044.2
009200     02  SORT-REC-NO PIC 9(6).                                    DB1044.2
009300     02  FILLER PIC X(40).                                        DB1044.2
009400 WORKING-STORAGE SECTION.                                         DB1044.2
009500 77  RESULT-FLAG PIC 99 COMP VALUE 0.                             DB1044.2
009600 77  DBLINE-HOLD PIC X(6).                                        DB1044.2
009700 77  DBNAME-HOLD PIC X(30).                                       DB1044.2
009800 77  DBCONT-HOLD PIC X(30).                                       DB1044.2
009900 01  FILE-RECORD-INFORMATION-REC.                                 DB1044.2
010000     03 FILE-RECORD-INFO-SKELETON.                                DB1044.2
010100        05 FILLER                 PICTURE X(48)       VALUE       DB1044.2
010200             "FILE=      ,RECORD=      /0,RECNO=000000,UPDT=00".  DB1044.2
010300        05 FILLER                 PICTURE X(46)       VALUE       DB1044.2
010400             ",ODO=0000,PGM=     ,LRECL=000000,BLKSIZ  =0000".    DB1044.2
010500        05 FILLER                 PICTURE X(26)       VALUE       DB1044.2
010600             ",LFIL=000000,ORG=  ,LBLR= ".                        DB1044.2
010700        05 FILLER                 PICTURE X(37)       VALUE       DB1044.2
010800             ",RECKEY=                             ".             DB1044.2
010900        05 FILLER                 PICTURE X(38)       VALUE       DB1044.2
011000             ",ALTKEY1=                             ".            DB1044.2
011100        05 FILLER                 PICTURE X(38)       VALUE       DB1044.2
011200             ",ALTKEY2=                             ".            DB1044.2
011300        05 FILLER                 PICTURE X(7)        VALUE SPACE.DB1044.2
011400     03 FILE-RECORD-INFO          OCCURS  10  TIMES.              DB1044.2
011500        05 FILE-RECORD-INFO-P1-120.                               DB1044.2
011600           07 FILLER              PIC X(5).                       DB1044.2
011700           07 XFILE-NAME           PIC X(6).                      DB1044.2
011800           07 FILLER              PIC X(8).                       DB1044.2
011900           07 XRECORD-NAME         PIC X(6).                      DB1044.2
012000           07 FILLER              PIC X(1).                       DB1044.2
012100           07 REELUNIT-NUMBER     PIC 9(1).                       DB1044.2
012200           07 FILLER              PIC X(7).                       DB1044.2
012300           07 XRECORD-NUMBER       PIC 9(6).                      DB1044.2
012400           07 FILLER              PIC X(6).                       DB1044.2
012500           07 UPDATE-NUMBER       PIC 9(2).                       DB1044.2
012600           07 FILLER              PIC X(5).                       DB1044.2
012700           07 ODO-NUMBER          PIC 9(4).                       DB1044.2
012800           07 FILLER              PIC X(5).                       DB1044.2
012900           07 XPROGRAM-NAME        PIC X(5).                      DB1044.2
013000           07 FILLER              PIC X(7).                       DB1044.2
013100           07 XRECORD-LENGTH       PIC 9(6).                      DB1044.2
013200           07 FILLER              PIC X(7).                       DB1044.2
013300           07 CHARS-OR-RECORDS    PIC X(2).                       DB1044.2
013400           07 FILLER              PIC X(1).                       DB1044.2
013500           07 XBLOCK-SIZE          PIC 9(4).                      DB1044.2
013600           07 FILLER              PIC X(6).                       DB1044.2
013700           07 RECORDS-IN-FILE     PIC 9(6).                       DB1044.2
013800           07 FILLER              PIC X(5).                       DB1044.2
013900           07 XFILE-ORGANIZATION   PIC X(2).                      DB1044.2
014000           07 FILLER              PIC X(6).                       DB1044.2
014100           07 XLABEL-TYPE          PIC X(1).                      DB1044.2
014200        05 FILE-RECORD-INFO-P121-240.                             DB1044.2
014300           07 FILLER              PIC X(8).                       DB1044.2
014400           07 XRECORD-KEY          PIC X(29).                     DB1044.2
014500           07 FILLER              PIC X(9).                       DB1044.2
014600           07 ALTERNATE-KEY1      PIC X(29).                      DB1044.2
014700           07 FILLER              PIC X(9).                       DB1044.2
014800           07 ALTERNATE-KEY2      PIC X(29).                      DB1044.2
014900           07 FILLER              PIC X(7).                       DB1044.2
015000 01  GEN-STATUS.                                                  DB1044.2
015100     02  END-FLAG PIC X.                                          DB1044.2
015200     02  FILLER PIC X.                                            DB1044.2
015300 01  SIZE-13.                                                     DB1044.2
015400     02  FILLER PIC XX.                                           DB1044.2
015500     02  SIZE-11.                                                 DB1044.2
015600         03  FILLER PIC X.                                        DB1044.2
015700         03  SIZE-10.                                             DB1044.2
015800             04  FILLER PIC XX.                                   DB1044.2
015900             04  SIZE-8.                                          DB1044.2
016000                 05  FILLER PIC X.                                DB1044.2
016100                 05  SIZE-7 PIC X(7).                             DB1044.2
016200 01  TEST-RESULTS.                                                DB1044.2
016300     02 FILLER                    PICTURE X VALUE SPACE.          DB1044.2
016400     02 FEATURE                   PICTURE X(20) VALUE SPACE.      DB1044.2
016500     02 FILLER                    PICTURE X VALUE SPACE.          DB1044.2
016600     02 P-OR-F                    PICTURE X(5) VALUE SPACE.       DB1044.2
016700     02 FILLER                    PICTURE X  VALUE SPACE.         DB1044.2
016800     02  PAR-NAME.                                                DB1044.2
016900       03 FILLER PICTURE X(12) VALUE SPACE.                       DB1044.2
017000       03  PARDOT-X PICTURE X  VALUE SPACE.                       DB1044.2
017100       03 DOTVALUE PICTURE 99  VALUE ZERO.                        DB1044.2
017200       03 FILLER PIC X(5) VALUE SPACE.                            DB1044.2
017300     02 FILLER PIC X(10) VALUE SPACE.                             DB1044.2
017400     02 RE-MARK PIC X(61).                                        DB1044.2
017500 01  TEST-COMPUTED.                                               DB1044.2
017600     02 FILLER PIC X(30) VALUE SPACE.                             DB1044.2
017700     02 FILLER PIC X(17) VALUE "       COMPUTED=".                DB1044.2
017800     02 COMPUTED-X.                                               DB1044.2
017900     03 COMPUTED-A                PICTURE X(20) VALUE SPACE.      DB1044.2
018000     03 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).       DB1044.2
018100     03 COMPUTED-0V18 REDEFINES COMPUTED-A  PICTURE -.9(18).      DB1044.2
018200     03 COMPUTED-4V14 REDEFINES COMPUTED-A  PICTURE -9(4).9(14).  DB1044.2
018300     03 COMPUTED-14V4 REDEFINES COMPUTED-A  PICTURE -9(14).9(4).  DB1044.2
018400     03       CM-18V0 REDEFINES COMPUTED-A.                       DB1044.2
018500         04 COMPUTED-18V0                   PICTURE -9(18).       DB1044.2
018600         04 FILLER                          PICTURE X.            DB1044.2
018700     03 FILLER PIC X(50) VALUE SPACE.                             DB1044.2
018800 01  TEST-CORRECT.                                                DB1044.2
018900     02 FILLER PIC X(30) VALUE SPACE.                             DB1044.2
019000     02 FILLER PIC X(17) VALUE "       CORRECT =".                DB1044.2
019100     02 CORRECT-X.                                                DB1044.2
019200     03 CORRECT-A                 PICTURE X(20) VALUE SPACE.      DB1044.2
019300     03 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).         DB1044.2
019400     03 CORRECT-0V18 REDEFINES CORRECT-A    PICTURE -.9(18).      DB1044.2
019500     03 CORRECT-4V14 REDEFINES CORRECT-A    PICTURE -9(4).9(14).  DB1044.2
019600     03 CORRECT-14V4 REDEFINES CORRECT-A    PICTURE -9(14).9(4).  DB1044.2
019700     03      CR-18V0 REDEFINES CORRECT-A.                         DB1044.2
019800         04 CORRECT-18V0                    PICTURE -9(18).       DB1044.2
019900         04 FILLER                          PICTURE X.            DB1044.2
020000     03 FILLER PIC X(50) VALUE SPACE.                             DB1044.2
020100 01  CCVS-C-1.                                                    DB1044.2
020200     02 FILLER PICTURE IS X(99) VALUE IS " FEATURE              PADB1044.2
020300-    "SS  PARAGRAPH-NAME                                          DB1044.2
020400-    "        REMARKS".                                           DB1044.2
020500     02 FILLER PICTURE IS X(20) VALUE IS SPACE.                   DB1044.2
020600 01  CCVS-C-2.                                                    DB1044.2
020700     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1044.2
020800     02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                 DB1044.2
020900     02 FILLER PICTURE IS X(15) VALUE IS SPACE.                   DB1044.2
021000     02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                   DB1044.2
021100     02 FILLER PICTURE IS X(94) VALUE IS SPACE.                   DB1044.2
021200 01  REC-SKL-SUB PICTURE 9(2) VALUE ZERO.                         DB1044.2
021300 01  REC-CT PICTURE 99 VALUE ZERO.                                DB1044.2
021400 01  DELETE-CNT                   PICTURE 999  VALUE ZERO.        DB1044.2
021500 01  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.                  DB1044.2
021600 01  INSPECT-COUNTER PIC 999 VALUE ZERO.                          DB1044.2
021700 01  PASS-COUNTER PIC 999 VALUE ZERO.                             DB1044.2
021800 01  TOTAL-ERROR PIC 999 VALUE ZERO.                              DB1044.2
021900 01  ERROR-HOLD PIC 999 VALUE ZERO.                               DB1044.2
022000 01  DUMMY-HOLD PIC X(120) VALUE SPACE.                           DB1044.2
022100 01  RECORD-COUNT PIC 9(5) VALUE ZERO.                            DB1044.2
022200 01  CCVS-H-1.                                                    DB1044.2
022300     02  FILLER   PICTURE X(27)  VALUE SPACE.                     DB1044.2
022400     02 FILLER PICTURE X(67) VALUE                                DB1044.2
022500     " FEDERAL COMPILER TESTING CENTER COBOL COMPILER VALIDATION  DB1044.2
022600-    " SYSTEM".                                                   DB1044.2
022700     02  FILLER     PICTURE X(26)  VALUE SPACE.                   DB1044.2
022800 01  CCVS-H-2.                                                    DB1044.2
022900     02 FILLER PICTURE X(52) VALUE IS                             DB1044.2
023000     "CCVS74 NCC  COPY, NOT FOR DISTRIBUTION.".                   DB1044.2
023100     02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".   DB1044.2
023200     02 TEST-ID PICTURE IS X(9).                                  DB1044.2
023300     02 FILLER PICTURE IS X(40) VALUE IS SPACE.                   DB1044.2
023400 01  CCVS-H-3.                                                    DB1044.2
023500     02  FILLER PICTURE X(34) VALUE                               DB1044.2
023600     " FOR OFFICIAL USE ONLY    ".                                DB1044.2
023700     02  FILLER PICTURE X(58) VALUE                               DB1044.2
023800     "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".DB1044.2
023900     02  FILLER PICTURE X(28) VALUE                               DB1044.2
024000     "  COPYRIGHT   1974 ".                                       DB1044.2
024100 01  CCVS-E-1.                                                    DB1044.2
024200     02 FILLER PICTURE IS X(52) VALUE IS SPACE.                   DB1044.2
024300     02 FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".        DB1044.2
024400     02 ID-AGAIN PICTURE IS X(9).                                 DB1044.2
024500     02 FILLER PICTURE X(45) VALUE IS                             DB1044.2
024600     " NTIS DISTRIBUTION COBOL 74".                               DB1044.2
024700 01  CCVS-E-2.                                                    DB1044.2
024800     02  FILLER                   PICTURE X(31)  VALUE            DB1044.2
024900     SPACE.                                                       DB1044.2
025000     02  FILLER                   PICTURE X(21)  VALUE SPACE.     DB1044.2
025100     02 CCVS-E-2-2.                                               DB1044.2
025200         03 ERROR-TOTAL PICTURE IS XXX VALUE IS SPACE.            DB1044.2
025300         03 FILLER PICTURE IS X VALUE IS SPACE.                   DB1044.2
025400         03 ENDER-DESC PIC X(44) VALUE "ERRORS ENCOUNTERED".      DB1044.2
025500 01  CCVS-E-3.                                                    DB1044.2
025600     02  FILLER PICTURE X(22) VALUE                               DB1044.2
025700     " FOR OFFICIAL USE ONLY".                                    DB1044.2
025800     02  FILLER PICTURE X(12) VALUE SPACE.                        DB1044.2
025900     02  FILLER PICTURE X(58) VALUE                               DB1044.2
026000     "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".DB1044.2
026100     02  FILLER PICTURE X(13) VALUE SPACE.                        DB1044.2
026200     02 FILLER PIC X(15) VALUE " COPYRIGHT 1974".                 DB1044.2
026300 01  CCVS-E-4.                                                    DB1044.2
026400     02 CCVS-E-4-1 PIC XXX VALUE SPACE.                           DB1044.2
026500     02 FILLER PIC XXXX VALUE " OF ".                             DB1044.2
026600     02 CCVS-E-4-2 PIC XXX VALUE SPACE.                           DB1044.2
026700     02 FILLER PIC X(40) VALUE                                    DB1044.2
026800      "  TESTS WERE EXECUTED SUCCESSFULLY".                       DB1044.2
026900 01  XXINFO.                                                      DB1044.2
027000     02 FILLER PIC X(30) VALUE "        *** INFORMATION  ***".    DB1044.2
027100     02 INFO-TEXT.                                                DB1044.2
027200     04 FILLER PIC X(20) VALUE SPACE.                             DB1044.2
027300     04 XXCOMPUTED PIC X(20).                                     DB1044.2
027400     04 FILLER PIC X(5) VALUE SPACE.                              DB1044.2
027500     04 XXCORRECT PIC X(20).                                      DB1044.2
027600 01  HYPHEN-LINE.                                                 DB1044.2
027700     02 FILLER PICTURE IS X VALUE IS SPACE.                       DB1044.2
027800     02 FILLER PICTURE IS X(65) VALUE IS "************************DB1044.2
027900-    "*****************************************".                 DB1044.2
028000     02 FILLER PICTURE IS X(54) VALUE IS "************************DB1044.2
028100-    "******************************".                            DB1044.2
028200 01  CCVS-PGM-ID PIC X(6) VALUE                                   DB1044.2
028300     "DB104A".                                                    DB1044.2
028400 PROCEDURE DIVISION.                                              DB1044.2
028500 DECLARATIVES.                                                    DB1044.2
028600 SORT-IN-PROC SECTION.                                            DB1044.2
028700     USE FOR DEBUGGING ON SORT-IN.                                DB1044.2
028800 BEGIN-SORT-IN-PROC.                                              DB1044.2
028900     MOVE 1 TO RESULT-FLAG.                                       DB1044.2
029000 DB-COMMON.                                                       DB1044.2
029100     MOVE DEBUG-LINE TO DBLINE-HOLD.                              DB1044.2
029200     MOVE DEBUG-NAME TO DBNAME-HOLD.                              DB1044.2
029300     MOVE DEBUG-CONTENTS TO DBCONT-HOLD.                          DB1044.2
029400 SORT-OUT-PROC SECTION.                                           DB1044.2
029500     USE FOR DEBUGGING ON SORT-OUT.                               DB1044.2
029600 BEGIN-SORT-OUT-PROC.                                             DB1044.2
029700     MOVE 2 TO RESULT-FLAG.                                       DB1044.2
029800     PERFORM DB-COMMON.                                           DB1044.2
029900 USE-PROC SECTION.                                                DB1044.2
030000     USE FOR DEBUGGING ON AT-END-PROC.                            DB1044.2
030100 BEGIN-USE-PROC.                                                  DB1044.2
030200     ADD 3 TO RESULT-FLAG.                                        DB1044.2
030300     PERFORM DB-COMMON.                                           DB1044.2
030400 AT-END-PROC SECTION.                                             DB1044.2
030500     USE AFTER ERROR PROCEDURE ON GEN-FILE.                       DB1044.2
030600 BEGIN-AT-END-PROC.                                               DB1044.2
030700     ADD 4 TO RESULT-FLAG.                                        DB1044.2
030800 END DECLARATIVES.                                                DB1044.2
030900 CCVS1 SECTION.                                                   DB1044.2
031000 OPEN-FILES.                                                      DB1044.2
031100     OPEN     OUTPUT PRINT-FILE.                                  DB1044.2
031200     MOVE CCVS-PGM-ID TO TEST-ID. MOVE CCVS-PGM-ID TO ID-AGAIN.   DB1044.2
031300     MOVE    SPACE TO TEST-RESULTS.                               DB1044.2
031400     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             DB1044.2
031500     GO TO CCVS1-EXIT.                                            DB1044.2
031600 CLOSE-FILES.                                                     DB1044.2
031700     PERFORM END-ROUTINE THRU END-ROUTINE-13. CLOSE PRINT-FILE.   DB1044.2
031800 TERMINATE-CCVS.                                                  DB1044.2
031900*    EXIT PROGRAM.                                                DB1044.2
032000*TERMINATE-CALL.                                                  DB1044.2
032100     STOP     RUN.                                                DB1044.2
032200 INSPT. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.         DB1044.2
032300 PASS.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.           DB1044.2
032400 FAIL.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.          DB1044.2
032500 DE-LETE.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.          DB1044.2
032600     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1044.2
032700 PRINT-DETAIL.                                                    DB1044.2
032800     IF REC-CT NOT EQUAL TO ZERO                                  DB1044.2
032900             MOVE "." TO PARDOT-X                                 DB1044.2
033000             MOVE REC-CT TO DOTVALUE.                             DB1044.2
033100     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE.      DB1044.2
033200     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE               DB1044.2
033300        PERFORM FAIL-ROUTINE THRU FAIL-ROUTINE-EX                 DB1044.2
033400          ELSE PERFORM BAIL-OUT THRU BAIL-OUT-EX.                 DB1044.2
033500     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1044.2
033600     MOVE SPACE TO CORRECT-X.                                     DB1044.2
033700     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1044.2
033800     MOVE     SPACE TO RE-MARK.                                   DB1044.2
033900 HEAD-ROUTINE.                                                    DB1044.2
034000     MOVE CCVS-H-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1044.2
034100     MOVE CCVS-H-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.   DB1044.2
034200     MOVE CCVS-H-3 TO DUMMY-RECORD. PERFORM WRITE-LINE 3 TIMES.   DB1044.2
034300 COLUMN-NAMES-ROUTINE.                                            DB1044.2
034400     MOVE CCVS-C-1 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1044.2
034500     MOVE CCVS-C-2 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1044.2
034600     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE.        DB1044.2
034700 END-ROUTINE.                                                     DB1044.2
034800     MOVE HYPHEN-LINE TO DUMMY-RECORD. PERFORM WRITE-LINE 5 TIMES.DB1044.2
034900 END-RTN-EXIT.                                                    DB1044.2
035000     MOVE CCVS-E-1 TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.   DB1044.2
035100 END-ROUTINE-1.                                                   DB1044.2
035200      ADD ERROR-COUNTER TO ERROR-HOLD ADD INSPECT-COUNTER TO      DB1044.2
035300      ERROR-HOLD. ADD DELETE-CNT TO ERROR-HOLD.                   DB1044.2
035400      ADD PASS-COUNTER TO ERROR-HOLD.                             DB1044.2
035500*     IF PASS-COUNTER EQUAL TO ERROR-HOLD GO TO END-ROUTINE-12.   DB1044.2
035600      MOVE PASS-COUNTER TO CCVS-E-4-1.                            DB1044.2
035700      MOVE ERROR-HOLD TO CCVS-E-4-2.                              DB1044.2
035800      MOVE CCVS-E-4 TO CCVS-E-2-2.                                DB1044.2
035900      MOVE CCVS-E-2 TO DUMMY-RECORD PERFORM WRITE-LINE.           DB1044.2
036000  END-ROUTINE-12.                                                 DB1044.2
036100      MOVE "TEST(S) FAILED" TO ENDER-DESC.                        DB1044.2
036200     IF       ERROR-COUNTER IS EQUAL TO ZERO                      DB1044.2
036300         MOVE "NO " TO ERROR-TOTAL                                DB1044.2
036400         ELSE                                                     DB1044.2
036500         MOVE ERROR-COUNTER TO ERROR-TOTAL.                       DB1044.2
036600     MOVE     CCVS-E-2 TO DUMMY-RECORD.                           DB1044.2
036700     PERFORM WRITE-LINE.                                          DB1044.2
036800 END-ROUTINE-13.                                                  DB1044.2
036900     IF DELETE-CNT IS EQUAL TO ZERO                               DB1044.2
037000         MOVE "NO " TO ERROR-TOTAL  ELSE                          DB1044.2
037100         MOVE DELETE-CNT TO ERROR-TOTAL.                          DB1044.2
037200     MOVE "TEST(S) DELETED     " TO ENDER-DESC.                   DB1044.2
037300     MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1044.2
037400      IF   INSPECT-COUNTER EQUAL TO ZERO                          DB1044.2
037500          MOVE "NO " TO ERROR-TOTAL                               DB1044.2
037600      ELSE MOVE INSPECT-COUNTER TO ERROR-TOTAL.                   DB1044.2
037700      MOVE "TEST(S) REQUIRE INSPECTION" TO ENDER-DESC.            DB1044.2
037800      MOVE CCVS-E-2 TO DUMMY-RECORD. PERFORM WRITE-LINE.          DB1044.2
037900     MOVE CCVS-E-3 TO DUMMY-RECORD. PERFORM WRITE-LINE.           DB1044.2
038000 WRITE-LINE.                                                      DB1044.2
038100     ADD 1 TO RECORD-COUNT.                                       DB1044.2
038200*    IF RECORD-COUNT GREATER 50                                   DB1044.2
038300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1044.2
038400*        MOVE SPACE TO DUMMY-RECORD                               DB1044.2
038500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1044.2
038600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN             DB1044.2
038700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN 2 TIMES     DB1044.2
038800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN          DB1044.2
038900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1044.2
039000*        MOVE ZERO TO RECORD-COUNT.                               DB1044.2
039100     PERFORM WRT-LN.                                              DB1044.2
039200 WRT-LN.                                                          DB1044.2
039300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1044.2
039400     MOVE SPACE TO DUMMY-RECORD.                                  DB1044.2
039500 BLANK-LINE-PRINT.                                                DB1044.2
039600     PERFORM WRT-LN.                                              DB1044.2
039700 FAIL-ROUTINE.                                                    DB1044.2
039800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.   DB1044.2
039900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-ROUTINE-WRITE.    DB1044.2
040000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1044.2
040100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1044.2
040200     GO TO FAIL-ROUTINE-EX.                                       DB1044.2
040300 FAIL-ROUTINE-WRITE.                                              DB1044.2
040400     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE           DB1044.2
040500     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE 2 TIMES.   DB1044.2
040600 FAIL-ROUTINE-EX. EXIT.                                           DB1044.2
040700 BAIL-OUT.                                                        DB1044.2
040800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE.       DB1044.2
040900     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX.               DB1044.2
041000 BAIL-OUT-WRITE.                                                  DB1044.2
041100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1044.2
041200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE 2 TIMES.     DB1044.2
041300 BAIL-OUT-EX. EXIT.                                               DB1044.2
041400 CCVS1-EXIT.                                                      DB1044.2
041500     EXIT.                                                        DB1044.2
041600 BEGIN-FILE-GENERATION.                                           DB1044.2
041700     MOVE FILE-RECORD-INFO-SKELETON TO FILE-RECORD-INFO (1).      DB1044.2
041800     MOVE "GEN-FI" TO XFILE-NAME (1).                             DB1044.2
041900     MOVE "GEN-RE" TO XRECORD-NAME (1).                           DB1044.2
042000     MOVE "DB104A" TO XPROGRAM-NAME (1).                          DB1044.2
042100     MOVE 80 TO XRECORD-LENGTH (1).                               DB1044.2
042200     OPEN OUTPUT GEN-FILE.                                        DB1044.2
042300     MOVE 99 TO XRECORD-NUMBER (1).                               DB1044.2
042400 GEN-LOOP.                                                        DB1044.2
042500     MOVE FILE-RECORD-INFO (1) TO GEN-REC.                        DB1044.2
042600     WRITE GEN-REC.                                               DB1044.2
042700     IF XRECORD-NUMBER (1) IS GREATER THAN 5                      DB1044.2
042800         SUBTRACT 5 FROM XRECORD-NUMBER (1)                       DB1044.2
042900         GO TO GEN-LOOP.                                          DB1044.2
043000 END-OF-GEN-LOOP.                                                 DB1044.2
043100     MOVE 98 TO XRECORD-NUMBER (1).                               DB1044.2
043200     PERFORM GEN-LOOP.                                            DB1044.2
043300     MOVE 97 TO XRECORD-NUMBER (1).                               DB1044.2
043400     PERFORM GEN-LOOP.                                            DB1044.2
043500     MOVE 96 TO XRECORD-NUMBER (1).                               DB1044.2
043600     PERFORM GEN-LOOP.                                            DB1044.2
043700     MOVE 95 TO XRECORD-NUMBER (1).                               DB1044.2
043800     PERFORM GEN-LOOP.                                            DB1044.2
043900     CLOSE GEN-FILE.                                              DB1044.2
044000******************************************************************DB1044.2
044100*    THE DEBUG-LINE (INSPT) SUBTESTS FOR THE TESTS NAMED IN THE  *DB1044.2
044200*    OUTPUT REPORT AS "SORT-IN-2" AND "SORT-OUT-2" SHOULD POINT  *DB1044.2
044300*    TO THE "SORT" STATEMENT WHICH APPEARS IN THE PARAGRAPH      *DB1044.2
044400*    BELOW NAMED "BEGIN-TESTS".                                  *DB1044.2
044500******************************************************************DB1044.2
044600 BEGIN-TESTS.                                                     DB1044.2
044700     MOVE 0 TO RESULT-FLAG.                                       DB1044.2
044800     SORT SORT-FILE ON ASCENDING KEY SORT-REC-NO                  DB1044.2
044900         INPUT PROCEDURE IS SORT-IN                               DB1044.2
045000         OUTPUT PROCEDURE IS SORT-OUT.                            DB1044.2
045100     GO TO AFTER-SORT.                                            DB1044.2
045200 SORT-IN SECTION.                                                 DB1044.2
045300 SORT-IN-1.                                                       DB1044.2
045400     MOVE "SORT-IN-1" TO PAR-NAME.                                DB1044.2
045500     IF RESULT-FLAG IS NOT EQUAL TO 1                             DB1044.2
045600         MOVE "DEBUG PROCEDURE NOT EXECUTED" TO RE-MARK           DB1044.2
045700         PERFORM FAIL-1                                           DB1044.2
045800         PERFORM SORT-IN-WRITE                                    DB1044.2
045900         GO TO SORT-IN-5                                          DB1044.2
046000         ELSE  PERFORM PASS-1                                     DB1044.2
046100         MOVE "DEBUG PROCEDURE EXECUTED" TO RE-MARK.              DB1044.2
046200     PERFORM SORT-IN-WRITE.                                       DB1044.2
046300     GO TO SORT-IN-2.                                             DB1044.2
046400 SORT-IN-DELETE.                                                  DB1044.2
046500     MOVE "SORT-IN" TO PAR-NAME.                                  DB1044.2
046600     PERFORM DE-LETE-1.                                           DB1044.2
046700     PERFORM SORT-IN-WRITE.                                       DB1044.2
046800     GO TO SORT-IN-5.                                             DB1044.2
046900 SORT-IN-WRITE.                                                   DB1044.2
047000     MOVE "DEBUG SORT INPUT" TO FEATURE.                          DB1044.2
047100     PERFORM PRINT-DETAIL-1.                                      DB1044.2
047200 SORT-IN-2.                                                       DB1044.2
047300     MOVE "SORT-IN-2" TO PAR-NAME.                                DB1044.2
047400     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1044.2
047500     MOVE "DEBUG-LINE, SEE NEXT LINE" TO RE-MARK.                 DB1044.2
047600     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1044.2
047700     PERFORM   INSPT-1.                                           DB1044.2
047800     PERFORM SORT-IN-WRITE.                                       DB1044.2
047900 SORT-IN-3.                                                       DB1044.2
048000     MOVE DBNAME-HOLD TO SIZE-7.                                  DB1044.2
048100     IF SIZE-7 IS EQUAL TO "SORT-IN"                              DB1044.2
048200         PERFORM PASS-1 ELSE                                      DB1044.2
048300         MOVE "SORT-IN" TO CORRECT-A                              DB1044.2
048400         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1044.2
048500         PERFORM FAIL-1.                                          DB1044.2
048600     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1044.2
048700     MOVE "SORT-IN-3" TO PAR-NAME.                                DB1044.2
048800     PERFORM SORT-IN-WRITE.                                       DB1044.2
048900 SORT-IN-4.                                                       DB1044.2
049000     MOVE DBCONT-HOLD TO SIZE-10.                                 DB1044.2
049100     IF SIZE-10 IS EQUAL TO "SORT INPUT"                          DB1044.2
049200         PERFORM PASS-1 ELSE                                      DB1044.2
049300         MOVE "SORT INPUT" TO CORRECT-A                           DB1044.2
049400         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1044.2
049500         PERFORM FAIL-1.                                          DB1044.2
049600     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1044.2
049700     MOVE "SORT-IN-4" TO PAR-NAME.                                DB1044.2
049800     PERFORM SORT-IN-WRITE.                                       DB1044.2
049900 SORT-IN-5.                                                       DB1044.2
050000     OPEN INPUT GEN-FILE.                                         DB1044.2
050100     MOVE 0 TO RESULT-FLAG.                                       DB1044.2
050200******************************************************************DB1044.2
050300*    THE DEBUG-LINE (INSPT) SUBTEST FOR THE TEST NAMED IN THE    *DB1044.2
050400*    OUTPUT REPORT AS "SORT-USE-TEST" SHOULD POINT TO THE        *DB1044.2
050500*    EXECUTABLE STATEMENT WHICH FOLLOWS THIS COMMENT SET AND     *DB1044.2
050600*    WHICH READS, "READ GEN-FILE".                               *DB1044.2
050700******************************************************************DB1044.2
050800 SORT-USE-TEST.                                                   DB1044.2
050900     READ GEN-FILE                                                DB1044.2
051000*        AT END GO TO SORT-USE-DELETE.                            DB1044.2
051100*                                                                 DB1044.2
051200*    IN CASE IMPLEMENTATION FAILS TO NOTIFY PROGRAM OF            DB1044.2
051300*    END-OF-FILE VIA STATUS OR DECLARATIVE PROC, REMOVE ASTERISK  DB1044.2
051400*    FROM THE FIRST OF THESE COMMENT LINES AND PERMIT THE AT END  DB1044.2
051500*    CLAUSE TO BE COMPILED; THIS WILL RESULT IN TEST DELETION.    DB1044.2
051600*                                                                 DB1044.2
051700     IF RESULT-FLAG IS EQUAL TO 3                                 DB1044.2
051800         CLOSE GEN-FILE GO TO SORT-USE-1.                         DB1044.2
051900     IF RESULT-FLAG IS EQUAL TO 4                                 DB1044.2
052000         CLOSE GEN-FILE GO TO SORT-USE-3.                         DB1044.2
052100     IF RESULT-FLAG IS EQUAL TO 7                                 DB1044.2
052200         CLOSE GEN-FILE                                           DB1044.2
052300         PERFORM PASS-1                                           DB1044.2
052400         MOVE "BOTH PROCEDURES EXECUTED" TO RE-MARK               DB1044.2
052500         PERFORM SORT-USE-WRITE                                   DB1044.2
052600         GO TO SORT-USE-2.                                        DB1044.2
052700     IF END-FLAG IS EQUAL TO "1"                                  DB1044.2
052800         CLOSE GEN-FILE GO TO SORT-USE-4.                         DB1044.2
052900     RELEASE SORT-REC FROM GEN-REC.                               DB1044.2
053000     GO TO SORT-USE-TEST.                                         DB1044.2
053100 SORT-USE-DELETE.                                                 DB1044.2
053200     CLOSE GEN-FILE.                                              DB1044.2
053300     PERFORM DE-LETE-1.                                           DB1044.2
053400     GO TO SORT-USE-WRITE.                                        DB1044.2
053500 SORT-USE-1.                                                      DB1044.2
053600     MOVE "ERROR PROCEDURE NOT COMPLETED" TO RE-MARK.             DB1044.2
053700     PERFORM SORT-USE-WRITE.                                      DB1044.2
053800 SORT-USE-2.                                                      DB1044.2
053900     MOVE "DEBUG-LINE, SEE NEXT LINE" TO RE-MARK.                 DB1044.2
054000     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1044.2
054100     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1044.2
054200     PERFORM   INSPT-1.                                           DB1044.2
054300     PERFORM SORT-USE-WRITE.                                      DB1044.2
054400     MOVE DBNAME-HOLD TO SIZE-11.                                 DB1044.2
054500     IF SIZE-11 IS EQUAL TO "AT-END-PROC"                         DB1044.2
054600         PERFORM PASS-1 ELSE                                      DB1044.2
054700         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1044.2
054800         MOVE "AT-END-PROC" TO CORRECT-A                          DB1044.2
054900         PERFORM FAIL-1.                                          DB1044.2
055000     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1044.2
055100     PERFORM SORT-USE-WRITE.                                      DB1044.2
055200     MOVE DBCONT-HOLD TO SIZE-13.                                 DB1044.2
055300     IF SIZE-13 IS EQUAL TO "USE PROCEDURE"                       DB1044.2
055400         PERFORM PASS-1 ELSE                                      DB1044.2
055500         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1044.2
055600         MOVE "USE PROCEDURE" TO CORRECT-A                        DB1044.2
055700         PERFORM FAIL-1.                                          DB1044.2
055800     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1044.2
055900     GO TO SORT-USE-WRITE.                                        DB1044.2
056000 SORT-USE-3.                                                      DB1044.2
056100     MOVE "DEBUG ON USE PROC NOT EXECUTED" TO RE-MARK.            DB1044.2
056200     PERFORM FAIL-1.                                              DB1044.2
056300     GO TO SORT-USE-WRITE.                                        DB1044.2
056400 SORT-USE-4.                                                      DB1044.2
056500     MOVE "DEBUG AND USE PROCS BOTH FAIL" TO RE-MARK.             DB1044.2
056600     PERFORM FAIL-1.                                              DB1044.2
056700 SORT-USE-WRITE.                                                  DB1044.2
056800     MOVE "SORT-USE-TEST" TO PAR-NAME.                            DB1044.2
056900     MOVE "DEBUG USE PROC" TO FEATURE.                            DB1044.2
057000     PERFORM PRINT-DETAIL-1.                                      DB1044.2
057100 SORT-USE-DONE.                                                   DB1044.2
057200     GO TO SORT-IN-EXIT.                                          DB1044.2
057300 INSPT-1. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       DB1044.2
057400 PASS-1.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         DB1044.2
057500 FAIL-1.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        DB1044.2
057600 DE-LETE-1.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.        DB1044.2
057700     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1044.2
057800 PRINT-DETAIL-1.                                                  DB1044.2
057900     IF REC-CT NOT EQUAL TO ZERO                                  DB1044.2
058000             MOVE "." TO PARDOT-X                                 DB1044.2
058100             MOVE REC-CT TO DOTVALUE.                             DB1044.2
058200     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-1.    DB1044.2
058300     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-1             DB1044.2
058400        PERFORM FAIL-ROUTINE-1 THRU FAIL-ROUTINE-EX-1             DB1044.2
058500          ELSE PERFORM BAIL-OUT-1 THRU BAIL-OUT-EX-1.             DB1044.2
058600     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1044.2
058700     MOVE SPACE TO CORRECT-X.                                     DB1044.2
058800     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1044.2
058900     MOVE     SPACE TO RE-MARK.                                   DB1044.2
059000 WRITE-LINE-1.                                                    DB1044.2
059100     ADD 1 TO RECORD-COUNT.                                       DB1044.2
059200*    IF RECORD-COUNT GREATER 50                                   DB1044.2
059300*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1044.2
059400*        MOVE SPACE TO DUMMY-RECORD                               DB1044.2
059500*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1044.2
059600*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-1           DB1044.2
059700*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-1 2 TIMES   DB1044.2
059800*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-1        DB1044.2
059900*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1044.2
060000*        MOVE ZERO TO RECORD-COUNT.                               DB1044.2
060100     PERFORM WRT-LN-1.                                            DB1044.2
060200 WRT-LN-1.                                                        DB1044.2
060300     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1044.2
060400     MOVE SPACE TO DUMMY-RECORD.                                  DB1044.2
060500 BLANK-LINE-PRINT-1.                                              DB1044.2
060600     PERFORM WRT-LN-1.                                            DB1044.2
060700 FAIL-ROUTINE-1.                                                  DB1044.2
060800     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.     DB1044.2
060900     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-1.      DB1044.2
061000     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1044.2
061100     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   DB1044.2
061200     GO TO FAIL-ROUTINE-EX-1.                                     DB1044.2
061300 FAIL-RTN-WRITE-1.                                                DB1044.2
061400     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-1         DB1044.2
061500     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-1 2 TIMES. DB1044.2
061600 FAIL-ROUTINE-EX-1. EXIT.                                         DB1044.2
061700 BAIL-OUT-1.                                                      DB1044.2
061800     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-1.     DB1044.2
061900     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-1.             DB1044.2
062000 BAIL-OUT-WRITE-1.                                                DB1044.2
062100     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1044.2
062200     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-1 2 TIMES.   DB1044.2
062300 BAIL-OUT-EX-1. EXIT.                                             DB1044.2
062400 SORT-IN-EXIT.                                                    DB1044.2
062500     MOVE 0 TO RESULT-FLAG.                                       DB1044.2
062600 SORT-OUT SECTION.                                                DB1044.2
062700 SORT-OUT-1.                                                      DB1044.2
062800     MOVE "SORT-OUT-1" TO PAR-NAME.                               DB1044.2
062900     IF RESULT-FLAG IS NOT EQUAL TO 2                             DB1044.2
063000         PERFORM FAIL-2                                           DB1044.2
063100         MOVE "DEBUG PROCEDURE NOT EXECUTED" TO RE-MARK           DB1044.2
063200         GO TO SORT-OUT-WRITE.                                    DB1044.2
063300     PERFORM PASS-2.                                              DB1044.2
063400     MOVE "DEBUG PROCEDURE EXECUTED" TO RE-MARK.                  DB1044.2
063500     PERFORM SORT-OUT-WRITE.                                      DB1044.2
063600 SORT-OUT-2.                                                      DB1044.2
063700     MOVE "SORT-OUT-2" TO PAR-NAME.                               DB1044.2
063800     MOVE DBLINE-HOLD TO COMPUTED-A.                              DB1044.2
063900     MOVE "DEBUG-LINE, SEE NEXT LINE" TO RE-MARK.                 DB1044.2
064000     MOVE "<===  DEBUG-LINE" TO CORRECT-A.                        DB1044.2
064100     PERFORM   INSPT-2.                                           DB1044.2
064200     PERFORM SORT-OUT-WRITE.                                      DB1044.2
064300 SORT-OUT-3.                                                      DB1044.2
064400     MOVE "SORT-OUT-3" TO PAR-NAME.                               DB1044.2
064500     MOVE DBNAME-HOLD TO SIZE-8.                                  DB1044.2
064600     IF SIZE-8 IS EQUAL TO "SORT-OUT"                             DB1044.2
064700         PERFORM PASS-2 ELSE                                      DB1044.2
064800         MOVE "SORT-OUT" TO CORRECT-A                             DB1044.2
064900         MOVE DBNAME-HOLD TO COMPUTED-A                           DB1044.2
065000         PERFORM FAIL-2.                                          DB1044.2
065100     MOVE "DEBUG-NAME" TO RE-MARK.                                DB1044.2
065200     PERFORM SORT-OUT-WRITE.                                      DB1044.2
065300 SORT-OUT-4.                                                      DB1044.2
065400     MOVE "SORT-OUT-4" TO PAR-NAME.                               DB1044.2
065500     MOVE DBCONT-HOLD TO SIZE-11.                                 DB1044.2
065600     IF SIZE-11 IS EQUAL TO "SORT OUTPUT"                         DB1044.2
065700         PERFORM PASS-2 ELSE                                      DB1044.2
065800         MOVE "SORT OUTPUT" TO CORRECT-A                          DB1044.2
065900         MOVE DBCONT-HOLD TO COMPUTED-A                           DB1044.2
066000     PERFORM FAIL-2.                                              DB1044.2
066100     MOVE "DEBUG-CONTENTS" TO RE-MARK.                            DB1044.2
066200     GO TO SORT-OUT-WRITE.                                        DB1044.2
066300 SORT-OUT-DELETE.                                                 DB1044.2
066400     MOVE "SORT-OUT" TO PAR-NAME.                                 DB1044.2
066500     PERFORM DE-LETE-2.                                           DB1044.2
066600 SORT-OUT-WRITE.                                                  DB1044.2
066700     MOVE "DEBUG SORT OUTPUT" TO FEATURE.                         DB1044.2
066800     PERFORM PRINT-DETAIL-2.                                      DB1044.2
066900 SORT-OUT-5.                                                      DB1044.2
067000     OPEN OUTPUT GEN-FILE.                                        DB1044.2
067100 SORT-OUT-6.                                                      DB1044.2
067200     RETURN SORT-FILE INTO GEN-REC                                DB1044.2
067300         AT END GO TO SORT-OUT-EXIT.                              DB1044.2
067400     WRITE GEN-REC.                                               DB1044.2
067500     GO TO SORT-OUT-6.                                            DB1044.2
067600 INSPT-2. MOVE "INSPT" TO P-OR-F. ADD 1 TO INSPECT-COUNTER.       DB1044.2
067700 PASS-2.  MOVE "PASS " TO P-OR-F.  ADD 1 TO PASS-COUNTER.         DB1044.2
067800 FAIL-2.  MOVE "FAIL*" TO P-OR-F.  ADD 1 TO ERROR-COUNTER.        DB1044.2
067900 DE-LETE-2.  MOVE "*****" TO P-OR-F.  ADD 1 TO DELETE-CNT.        DB1044.2
068000     MOVE "****TEST DELETED****" TO RE-MARK.                      DB1044.2
068100 PRINT-DETAIL-2.                                                  DB1044.2
068200     IF REC-CT NOT EQUAL TO ZERO                                  DB1044.2
068300             MOVE "." TO PARDOT-X                                 DB1044.2
068400             MOVE REC-CT TO DOTVALUE.                             DB1044.2
068500     MOVE     TEST-RESULTS TO PRINT-REC. PERFORM WRITE-LINE-2.    DB1044.2
068600     IF P-OR-F EQUAL TO "FAIL*"  PERFORM WRITE-LINE-2             DB1044.2
068700        PERFORM FAIL-ROUTINE-2 THRU FAIL-ROUTINE-EX-2             DB1044.2
068800          ELSE PERFORM BAIL-OUT-2 THRU BAIL-OUT-EX-2.             DB1044.2
068900     MOVE SPACE TO P-OR-F. MOVE SPACE TO COMPUTED-X.              DB1044.2
069000     MOVE SPACE TO CORRECT-X.                                     DB1044.2
069100     IF     REC-CT EQUAL TO ZERO  MOVE SPACE TO PAR-NAME.         DB1044.2
069200     MOVE     SPACE TO RE-MARK.                                   DB1044.2
069300 WRITE-LINE-2.                                                    DB1044.2
069400     ADD 1 TO RECORD-COUNT.                                       DB1044.2
069500*    IF RECORD-COUNT GREATER 50                                   DB1044.2
069600*        MOVE DUMMY-RECORD TO DUMMY-HOLD                          DB1044.2
069700*        MOVE SPACE TO DUMMY-RECORD                               DB1044.2
069800*        WRITE DUMMY-RECORD AFTER ADVANCING PAGE                  DB1044.2
069900*        MOVE CCVS-C-1 TO DUMMY-RECORD PERFORM WRT-LN-2           DB1044.2
070000*        MOVE CCVS-C-2 TO DUMMY-RECORD PERFORM WRT-LN-2 2 TIMES   DB1044.2
070100*        MOVE HYPHEN-LINE TO DUMMY-RECORD PERFORM WRT-LN-2        DB1044.2
070200*        MOVE DUMMY-HOLD TO DUMMY-RECORD                          DB1044.2
070300*        MOVE ZERO TO RECORD-COUNT.                               DB1044.2
070400     PERFORM WRT-LN-2.                                            DB1044.2
070500 WRT-LN-2.                                                        DB1044.2
070600     WRITE    DUMMY-RECORD AFTER ADVANCING 1 LINES.               DB1044.2
070700     MOVE SPACE TO DUMMY-RECORD.                                  DB1044.2
070800 BLANK-LINE-PRINT-2.                                              DB1044.2
070900     PERFORM WRT-LN-2.                                            DB1044.2
071000 FAIL-ROUTINE-2.                                                  DB1044.2
071100     IF COMPUTED-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.     DB1044.2
071200     IF CORRECT-X NOT EQUAL TO SPACE GO TO FAIL-RTN-WRITE-2.      DB1044.2
071300     MOVE "NO FURTHER INFORMATION, SEE PROGRAM." TO INFO-TEXT.    DB1044.2
071400     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   DB1044.2
071500     GO TO FAIL-ROUTINE-EX-2.                                     DB1044.2
071600 FAIL-RTN-WRITE-2.                                                DB1044.2
071700     MOVE TEST-COMPUTED TO PRINT-REC PERFORM WRITE-LINE-2         DB1044.2
071800     MOVE TEST-CORRECT TO PRINT-REC PERFORM WRITE-LINE-2 2 TIMES. DB1044.2
071900 FAIL-ROUTINE-EX-2. EXIT.                                         DB1044.2
072000 BAIL-OUT-2.                                                      DB1044.2
072100     IF COMPUTED-A NOT EQUAL TO SPACE GO TO BAIL-OUT-WRITE-2.     DB1044.2
072200     IF CORRECT-A EQUAL TO SPACE GO TO BAIL-OUT-EX-2.             DB1044.2
072300 BAIL-OUT-WRITE-2.                                                DB1044.2
072400     MOVE CORRECT-A TO XXCORRECT. MOVE COMPUTED-A TO XXCOMPUTED.  DB1044.2
072500     MOVE XXINFO TO DUMMY-RECORD. PERFORM WRITE-LINE-2 2 TIMES.   DB1044.2
072600 BAIL-OUT-EX-2. EXIT.                                             DB1044.2
072700 SORT-OUT-EXIT.                                                   DB1044.2
072800     CLOSE GEN-FILE.                                              DB1044.2
072900     MOVE 0 TO RESULT-FLAG.                                       DB1044.2
073000 END-OF-SORT SECTION.                                             DB1044.2
073100 AFTER-SORT.                                                      DB1044.2
073200     EXIT.                                                        DB1044.2
073300*DUMP-CODING SECTION.                                             DB1044.2
073400*BEGIN-DUMP.                                                      DB1044.2
073500*    OPEN INPUT GEN-FILE.                                         DB1044.2
073600*    PERFORM BLANK-LINE-PRINT.                                    DB1044.2
073700*    MOVE " DUMP OF GEN-FILE FOLLOWS:" TO PRINT-REC.              DB1044.2
073800*    PERFORM WRITE-LINE.                                          DB1044.2
073900*DUMP-FILE-1.                                                     DB1044.2
074000*    READ GEN-FILE AT END GO TO DUMP-FILE-2.                      DB1044.2
074100*    MOVE GEN-REC TO PRINT-REC.                                   DB1044.2
074200*    PERFORM WRITE-LINE.                                          DB1044.2
074300*    GO TO DUMP-FILE-1.                                           DB1044.2
074400*DUMP-FILE-2.                                                     DB1044.2
074500*    CLOSE GEN-FILE.                                              DB1044.2
074600 CCVS-EXIT SECTION.                                               DB1044.2
074700 CCVS-999999.                                                     DB1044.2
074800     GO TO CLOSE-FILES.                                           DB1044.2
      *END-OF,DB104A                                                                  
