      *HEADER,COBOL,NC113M                                                            
000100    IDENTIFICATION DIVISION.                                      NC1134.2
000200    PROGRAM-ID.                                                   NC1134.2
000300     NC113M.                                                      NC1134.2
000400****************************************************************  NC1134.2
000500*                                                              *  NC1134.2
000600*    VALIDATION FOR:-                                          *  NC1134.2
000700*                                                              *  NC1134.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".NC1134.2
000900*                                                              *  NC1134.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".NC1134.2
001100*                                                              *  NC1134.2
001200****************************************************************  NC1134.2
001300*                                                              *  NC1134.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  NC1134.2
001500*                                                              *  NC1134.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  NC1134.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  NC1134.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  NC1134.2
001900*                                                              *  NC1134.2
002000****************************************************************  NC1134.2
002100*                                                                 NC1134.2
002200*   PROGRAM NC113M VERIFIES CORRECT USE OF AREA A WITHIN          NC1134.2
002300*                      A PROGRAM.                                 NC1134.2
002400*                                                                 NC1134.2
002500*                                                                 NC1134.2
002600    ENVIRONMENT DIVISION.                                         NC1134.2
002700    CONFIGURATION SECTION.                                        NC1134.2
002800    SOURCE-COMPUTER.                                              NC1134.2
002900     XXXXX082.                                                    NC1134.2
003000    OBJECT-COMPUTER.                                              NC1134.2
003100     XXXXX083.                                                    NC1134.2
003200    INPUT-OUTPUT SECTION.                                         NC1134.2
003300    FILE-CONTROL.                                                 NC1134.2
003400     SELECT PRINT-FILE ASSIGN TO                                  NC1134.2
003500     XXXXX055.                                                    NC1134.2
003600    DATA                                                          NC1134.2
003700     DIVISION.                                                    NC1134.2
003800    FILE SECTION.                                                 NC1134.2
003900    FD PRINT-FILE.                                                NC1134.2
004000    01 PRINT-REC PICTURE X(120).                                  NC1134.2
004100    01  DUMMY-RECORD PICTURE X(120).                              NC1134.2
004200 WORKING-STORAGE SECTION.                                         NC1134.2
004300    77  ERROR-COUNTER PICTURE IS 999 VALUE IS ZERO.               NC1134.2
004400    77  DELETE-CNT                PICTURE 999  VALUE ZERO.        NC1134.2
004500    01  TEST-RESULTS.                                             NC1134.2
004600    02 FILLER                     PICTURE X VALUE SPACE.          NC1134.2
004700  02  FEATURE                PICTURE X(20).                       NC1134.2
004800  02 FILLER                       PICTURE X VALUE SPACE.          NC1134.2
004900  02  P-OR-F                      PICTURE XXXXX.                  NC1134.2
005000   02 FILLER                      PICTURE X  VALUE SPACE.         NC1134.2
005100    02 PAR-NAME                   PICTURE X(19).                  NC1134.2
005200   02 FILLER                      PICTURE X VALUE SPACE.          NC1134.2
005300  02 COMPUTED-A                   PICTURE X(20).                  NC1134.2
005400   02 COMPUTED-N REDEFINES COMPUTED-A PICTURE -9(9).9(9).         NC1134.2
005500     02 FILLER                    PICTURE XX VALUE SPACE.         NC1134.2
005600     02 CORRECT-A                 PICTURE X(20).                  NC1134.2
005700  02 CORRECT-N REDEFINES CORRECT-A PICTURE -9(9).9(9).            NC1134.2
005800       02 FILLER                  PICTURE XX VALUE SPACE.         NC1134.2
005900  02 RE-MARK                      PICTURE X(27).                  NC1134.2
006000   01  HEADER-LINE-1.                                             NC1134.2
006100       02  PAGE-CONTROL-4 PICTURE IS X   VALUE IS "1".            NC1134.2
006200       02  FILLER         PICTURE X(42)  VALUE                    NC1134.2
006300     SPACE.                                                       NC1134.2
006400 02 FILLER PICTURE X(42) VALUE                                    NC1134.2
006500     "OFFICIAL COBOL COMPILER VALIDATION SYSTEM ".                NC1134.2
006600 02 FILLER                        PICTURE X(35)  VALUE            NC1134.2
006700     SPACE.                                                       NC1134.2
006800    01 HEADER-LINE-2.                                             NC1134.2
006900  02 FILLER PICTURE IS X(52) VALUE IS SPACE.                      NC1134.2
007000   02 FILLER PICTURE IS X(19) VALUE IS "TEST RESULTS SET-  ".     NC1134.2
007100    02 TEST-ID PICTURE IS X(9).                                   NC1134.2
007200   02 FILLER PICTURE IS X(40) VALUE IS SPACE.                     NC1134.2
007300   01 HEADER-LINE-3.                                              NC1134.2
007400  02 FILLER PICTURE X(34) VALUE                                   NC1134.2
007500     " FOR OFFICIAL USE ONLY    ".                                NC1134.2
007600     02  FILLER PICTURE X(58) VALUE                               NC1134.2
007700     "                                                         ". NC1134.2
007800       02 FILLER PICTURE X(28) VALUE                              NC1134.2
007900     "          COPYRIGHT 1985    ".                              NC1134.2
008000    01  COLUMNS-LINE-1.                                           NC1134.2
008100  02 PAGE-CONTROL-C PICTURE IS X VALUE IS SPACE.                  NC1134.2
008200  02  FILLER   PICTURE IS X(99) VALUE IS "FEATURE              PASNC1134.2
008300-    "S  PARAGRAPH NAME                                           NC1134.2
008400-    "        REMARKS".                                           NC1134.2
008500  02 FILLER PICTURE IS X(20) VALUE IS SPACE.                      NC1134.2
008600   01  COLUMNS-LINE-2.                                            NC1134.2
008700   02 FILLER PICTURE IS X VALUE IS SPACE.                         NC1134.2
008800   02 FILLER PICTURE IS X(6) VALUE IS "TESTED".                   NC1134.2
008900   02 FILLER PICTURE IS X(15) VALUE IS SPACE.                     NC1134.2
009000   02 FILLER PICTURE IS X(4) VALUE IS "FAIL".                     NC1134.2
009100   02 FILLER PICTURE IS X(94) VALUE IS SPACE.                     NC1134.2
009200    01        ENDER-LINE-1.                                       NC1134.2
009300    02      FILLER PICTURE IS X(52) VALUE IS SPACE.               NC1134.2
009400    02        FILLER PICTURE IS X(14) VALUE IS "END OF TEST-  ".  NC1134.2
009500    02         ID-AGAIN   PICTURE IS X(9).                        NC1134.2
009600    02         FILLER PICTURE IS X(45) VALUE IS SPACE.            NC1134.2
009700  01 ENDER-LINE-2.                                                NC1134.2
009800  02 FILLER                       PICTURE X(31)  VALUE            NC1134.2
009900     SPACE.                                                       NC1134.2
010000  02 FILLER     PICTURE     X(21)    VALUE   IS   SPACE.          NC1134.2
010100   02 ERROR-TOTAL PICTURE IS         XXX     VALUE IS SPACE.      NC1134.2
010200  02 FILLER PICTURE IS X VALUE IS SPACE.                          NC1134.2
010300    02      ENDER-DESC           PICTURE X(31) VALUE IS "ERRORS ENNC1134.2
010400-    "COUNTERED".                                                 NC1134.2
010500     02 FILLER PICTURE IS X(33) VALUE IS SPACE.                   NC1134.2
010600    01 ENDER-LINE-3.                                              NC1134.2
010700   02 FILLER PICTURE X(22) VALUE                                  NC1134.2
010800     " FOR OFFICIAL USE ONLY".                                    NC1134.2
010900  02 FILLER PICTURE X(12) VALUE SPACE.                            NC1134.2
011000   02 FILLER PICTURE X(58) VALUE SPACE.                           NC1134.2
011100    02 FILLER PICTURE X(13) VALUE SPACE.                          NC1134.2
011200     02  FILLER PICTURE X(15) VALUE " COPYRIGHT 1985".            NC1134.2
011300  01  HYPHEN-LINE.                                                NC1134.2
011400     02 FILLER PICTURE IS X VALUE IS SPACE.                       NC1134.2
011500     02 FILLER PICTURE IS X(65) VALUE IS "------------------------NC1134.2
011600-    "-----------------------------------------".                 NC1134.2
011700     02 FILLER PICTURE IS X(54) VALUE IS "------------------------NC1134.2
011800-    "------------------------------".                            NC1134.2
011900    PROCEDURE                                                     NC1134.2
012000      DIVISION                                                    NC1134.2
012100               .                                                  NC1134.2
012200    OPEN-FILES.                                                   NC1134.2
012300     OPEN     OUTPUT PRINT-FILE.                                  NC1134.2
012400     MOVE "  NC113M" TO TEST-ID.                                  NC1134.2
012500     MOVE     TEST-ID TO ID-AGAIN.                                NC1134.2
012600     PERFORM  HEAD-ROUTINE THRU COLUMN-NAMES-ROUTINE.             NC1134.2
012700     MOVE     SPACE TO TEST-RESULTS.                              NC1134.2
012800     MOVE "MARGIN TESTING" TO FEATURE.                            NC1134.2
012900     PERFORM MAR-TEST-1.                                          NC1134.2
013000    MAR-TEST-2.                                                   NC1134.2
013100     MOVE "VISUALLY CHECKED" TO RE-MARK.                          NC1134.2
013200     MOVE "MAR-TEST-2"     TO PAR-NAME.                           NC1134.2
013300     PERFORM PRINT-DETAIL.                                        NC1134.2
013400     GO TO  MAR-TEST-3.                                           NC1134.2
013500    MAR-TEST-15.                                                  NC1134.2
013600     MOVE "MAR-TEST-15"    TO PAR-NAME.                           NC1134.2
013700     PERFORM PRINT-DETAIL.                                        NC1134.2
013800    MAR-TEST-7.                                                   NC1134.2
013900     MOVE "MAR-TEST-7"     TO PAR-NAME.                           NC1134.2
014000     PERFORM PRINT-DETAIL.                                        NC1134.2
014100*    GO TO MAR-TEST-8.                                            NC1134.2
014200    MAR-TEST-12.                                                  NC1134.2
014300     MOVE "MAR-TEST-12"    TO PAR-NAME.                           NC1134.2
014400     PERFORM PRINT-DETAIL.                                        NC1134.2
014500     GO TO MAR-TEST-13.                                           NC1134.2
014600    MAR-TEST-4.                                                   NC1134.2
014700     MOVE "MAR-TEST-4"     TO PAR-NAME.                           NC1134.2
014800     PERFORM PRINT-DETAIL.                                        NC1134.2
014900  MAR-TEST-9.                                                     NC1134.2
015000     MOVE "MAR-TEST-9"     TO PAR-NAME.                           NC1134.2
015100     PERFORM PRINT-DETAIL.                                        NC1134.2
015200 MAR-TEST-8.                                                      NC1134.2
015300     MOVE "MAR-TEST-8"     TO PAR-NAME.                           NC1134.2
015400     PERFORM PRINT-DETAIL.                                        NC1134.2
015500     PERFORM MAR-TEST-9.                                          NC1134.2
015600     GO TO MAR-TEST-10.                                           NC1134.2
015700    MAR-TEST-1.                                                   NC1134.2
015800     MOVE "ANSWERS MUST BE"  TO RE-MARK.                          NC1134.2
015900     MOVE "MAR-TEST-1"     TO PAR-NAME.                           NC1134.2
016000     PERFORM PRINT-DETAIL.                                        NC1134.2
016100    MAR-TEST-5.                                                   NC1134.2
016200     MOVE "TESTS MUST BE SEQUENTIAL" TO RE-MARK.                  NC1134.2
016300     MOVE "MAR-TEST-5"     TO PAR-NAME.                           NC1134.2
016400     PERFORM PRINT-DETAIL.                                        NC1134.2
016500     GO TO   MAR-TEST-6.                                          NC1134.2
016600    MAR-TEST-13.                                                  NC1134.2
016700     MOVE "MAR-TEST-13"    TO PAR-NAME.                           NC1134.2
016800     PERFORM PRINT-DETAIL.                                        NC1134.2
016900     GO TO MAR-TEST-14.                                           NC1134.2
017000    MAR-TEST-3.                                                   NC1134.2
017100     MOVE "FOR CORRECTNESS"  TO RE-MARK.                          NC1134.2
017200     MOVE "MAR-TEST-3"     TO PAR-NAME.                           NC1134.2
017300     PERFORM PRINT-DETAIL.                                        NC1134.2
017400     PERFORM MAR-TEST-4.                                          NC1134.2
017500     GO TO MAR-TEST-5.                                            NC1134.2
017600    MAR-TEST-14.                                                  NC1134.2
017700     MOVE "MAR-TEST-14"    TO PAR-NAME.                           NC1134.2
017800     PERFORM PRINT-DETAIL.                                        NC1134.2
017900     PERFORM MAR-TEST-15.                                         NC1134.2
018000     GO TO CLOSE-FILES.                                           NC1134.2
018100    MAR-TEST-11.                                                  NC1134.2
018200     MOVE "MAR-TEST-11"    TO PAR-NAME.                           NC1134.2
018300     PERFORM PRINT-DETAIL.                                        NC1134.2
018400     GO TO MAR-TEST-12.                                           NC1134.2
018500   MAR-TEST-10.                                                   NC1134.2
018600     MOVE "MAR-TEST-10"    TO PAR-NAME.                           NC1134.2
018700     PERFORM PRINT-DETAIL.                                        NC1134.2
018800     GO TO MAR-TEST-11.                                           NC1134.2
018900    MAR-TEST-6.                                                   NC1134.2
019000     MOVE "MAR-TEST-6"     TO PAR-NAME.                           NC1134.2
019100     PERFORM PRINT-DETAIL.                                        NC1134.2
019200     PERFORM MAR-TEST-7.                                          NC1134.2
019300     GO TO MAR-TEST-8.                                            NC1134.2
019400    CLOSE-FILES.                                                  NC1134.2
019500     PERFORM END-ROUTINE THRU END-ROUTINE-3.                      NC1134.2
019600     CLOSE    PRINT-FILE.                                         NC1134.2
019700     STOP     RUN.                                                NC1134.2
019800    PASS.                                                         NC1134.2
019900     MOVE "PASS" TO P-OR-F.                                       NC1134.2
020000    FAIL.                                                         NC1134.2
020100     ADD      1 TO ERROR-COUNTER.                                 NC1134.2
020200     MOVE "FAIL*" TO P-OR-F.                                      NC1134.2
020300    DE-LETE.                                                      NC1134.2
020400     MOVE     SPACE TO P-OR-F.                                    NC1134.2
020500     MOVE     "    ************    " TO COMPUTED-A.               NC1134.2
020600     MOVE     "    ************    " TO CORRECT-A.                NC1134.2
020700     MOVE "****TEST DELETED****" TO RE-MARK.                      NC1134.2
020800     ADD 1 TO DELETE-CNT.                                         NC1134.2
020900    PRINT-DETAIL.                                                 NC1134.2
021000     MOVE     TEST-RESULTS TO PRINT-REC.                          NC1134.2
021100     WRITE    PRINT-REC                             AFTER 1.      NC1134.2
021200     MOVE     SPACE TO P-OR-F.                                    NC1134.2
021300     MOVE     SPACE TO PAR-NAME.                                  NC1134.2
021400     MOVE     SPACE TO COMPUTED-A.                                NC1134.2
021500     MOVE SPACE TO CORRECT-A.                                     NC1134.2
021600     MOVE     SPACE TO RE-MARK.                                   NC1134.2
021700    HEAD-ROUTINE.                                                 NC1134.2
021800     PERFORM  BLANK-LINE-PRINT 15 TIMES.                          NC1134.2
021900     MOVE     HEADER-LINE-1 TO DUMMY-RECORD.                      NC1134.2
022000     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
022100     PERFORM  BLANK-LINE-PRINT.                                   NC1134.2
022200     MOVE     HEADER-LINE-2 TO DUMMY-RECORD.                      NC1134.2
022300     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
022400     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           NC1134.2
022500     MOVE HEADER-LINE-3 TO DUMMY-RECORD.                          NC1134.2
022600     WRITE DUMMY-RECORD                             AFTER 1.      NC1134.2
022700     PERFORM BLANK-LINE-PRINT 2 TIMES.                            NC1134.2
022800    COLUMN-NAMES-ROUTINE.                                         NC1134.2
022900     MOVE     COLUMNS-LINE-1 TO DUMMY-RECORD.                     NC1134.2
023000     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
023100     MOVE     SPACE TO PAGE-CONTROL-C.                            NC1134.2
023200     MOVE     COLUMNS-LINE-2 TO DUMMY-RECORD.                     NC1134.2
023300     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
023400     PERFORM  BLANK-LINE-PRINT.                                   NC1134.2
023500     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        NC1134.2
023600     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
023700    END-ROUTINE.                                                  NC1134.2
023800     MOVE     HYPHEN-LINE TO DUMMY-RECORD.                        NC1134.2
023900     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
024000    PARA-Z.                                                       NC1134.2
024100     PERFORM  BLANK-LINE-PRINT 4 TIMES.                           NC1134.2
024200     MOVE     ENDER-LINE-1 TO DUMMY-RECORD.                       NC1134.2
024300     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
024400    END-ROUTINE-1.                                                NC1134.2
024500     PERFORM  BLANK-LINE-PRINT.                                   NC1134.2
024600     MOVE   "TESTS REQUIRE VISUAL INSPECTION" TO ENDER-DESC.      NC1134.2
024700    END-ROUTINE-2.                                                NC1134.2
024800     MOVE " 15" TO ERROR-TOTAL.                                   NC1134.2
024900    END-ROUTINE-3.                                                NC1134.2
025000     MOVE     ENDER-LINE-2 TO DUMMY-RECORD.                       NC1134.2
025100     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
025200     IF DELETE-CNT IS EQUAL TO ZERO                               NC1134.2
025300         MOVE " NO" TO ERROR-TOTAL  ELSE                          NC1134.2
025400     MOVE DELETE-CNT TO ERROR-TOTAL.                              NC1134.2
025500     MOVE "TESTS DELETED     " TO ENDER-DESC.                     NC1134.2
025600     MOVE ENDER-LINE-2 TO DUMMY-RECORD.                           NC1134.2
025700     WRITE DUMMY-RECORD                             AFTER 1.      NC1134.2
025800     MOVE ENDER-LINE-3 TO DUMMY-RECORD.                           NC1134.2
025900     WRITE DUMMY-RECORD                             AFTER 1.      NC1134.2
026000    BLANK-LINE-PRINT.                                             NC1134.2
026100     MOVE     SPACE TO DUMMY-RECORD.                              NC1134.2
026200     WRITE    DUMMY-RECORD                          AFTER 1.      NC1134.2
      *END-OF,NC113M                                                                  
