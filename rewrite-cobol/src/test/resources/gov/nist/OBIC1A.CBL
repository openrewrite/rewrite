      *HEADER,COBOL,OBIC1A                                                            
000100 IDENTIFICATION DIVISION.                                         OBIC14.2
000200 PROGRAM-ID.                                                      OBIC14.2
000300     OBIC1A.                                                      OBIC14.2
000400****************************************************************  OBIC14.2
000500*                                                              *  OBIC14.2
000600*    VALIDATION FOR:-                                          *  OBIC14.2
000700*                                                              *  OBIC14.2
000800*    "ON-SITE VALIDATION, NATIONAL INSTITUTE OF STD & TECH.     ".OBIC14.2
000900*                                                              *  OBIC14.2
001000*    "COBOL 85 VERSION 4.2, Apr  1993 SSVG                      ".OBIC14.2
001100*                                                              *  OBIC14.2
001200****************************************************************  OBIC14.2
001300*                                                              *  OBIC14.2
001400*      X-CARDS USED BY THIS PROGRAM ARE :-                     *  OBIC14.2
001500*                                                              *  OBIC14.2
001600*        X-55  - SYSTEM PRINTER NAME.                          *  OBIC14.2
001700*        X-82  - SOURCE COMPUTER NAME.                         *  OBIC14.2
001800*        X-83  - OBJECT COMPUTER NAME.                         *  OBIC14.2
001900*                                                              *  OBIC14.2
002000****************************************************************  OBIC14.2
002100*                                                                 OBIC14.2
002200*        THE MAIN PROGRAM IC218 CALLS THE SUBPROGRAM IC219 WHICH  OBIC14.2
002300*    CONTAINS A SORT STATEMENT AND A STOP RUN STATEMENT.  THE     OBIC14.2
002400*    PURPOSE OF THESE PROGRAMS IS TO VERIFY THAT A SORT STATEMENT OBIC14.2
002500*    FUNCTIONS CORRECTLY IN A SUBPROGRAM.  THE FIRST NON-DECLARA- OBIC14.2
002600*    TIVE PORTION OF THE SUBPROGRAM, THE SORT INPUT PROCEDURE AND OBIC14.2
002700*    THE SORT OUTPUT PROCEDURE ARE CONTAINED IN DIFFERENT SUBPRO- OBIC14.2
002800*    GRAM SEGMENTS.                                               OBIC14.2
002900*                                                                 OBIC14.2
003000*        A CALL IS MADE TO THE SUBPROGRAM IC219.  CONTROL SHOULD  OBIC14.2
003100*    NOT BE RETURNED TO THIS PROGRAM SINCE IC219 CONTAINS A STOP  OBIC14.2
003200*    RUN STATEMENT.  THE SUBPROGRAM IC220 CONTAINS THE PRINTER FD OBIC14.2
003300*    AND PRINTS OUT THE REPORT SHOWING THE TEST RESULTS.          OBIC14.2
003400*                                                                 OBIC14.2
003500*    REFERENCE - AMERICAN NATIONAL STANDARD PROGRAMMING LANGUAGE  OBIC14.2
003600*                COBOL, X3.23-1985                                OBIC14.2
003700*                                                                 OBIC14.2
003800******************************************************************OBIC14.2
003900 ENVIRONMENT DIVISION.                                            OBIC14.2
004000 CONFIGURATION SECTION.                                           OBIC14.2
004100 SOURCE-COMPUTER.                                                 OBIC14.2
004200     XXXXX082.                                                    OBIC14.2
004300 OBJECT-COMPUTER.                                                 OBIC14.2
004400     XXXXX083.                                                    OBIC14.2
004500 DATA DIVISION.                                                   OBIC14.2
004600 WORKING-STORAGE SECTION.                                         OBIC14.2
004700 01  SORT-LINK               PICTURE 9.                           OBIC14.2
004800 01  PRINT-LINE-VALUES.                                           OBIC14.2
004900     02  PASS-OR-FAIL        PICTURE X(5).                        OBIC14.2
005000     02  R-COUNT             PICTURE 99.                          OBIC14.2
005100     02  FEATURE-TESTED      PICTURE X(20).                       OBIC14.2
005200     02  COMPUTED-SORT-KEY   PICTURE X(20).                       OBIC14.2
005300     02  CORRECT-SORT-KEY    PICTURE X(20).                       OBIC14.2
005400     02  PARAGRAPH-NAME      PICTURE X(12).                       OBIC14.2
005500 01  PRINT-FLAG              PICTURE 9.                           OBIC14.2
005600 PROCEDURE DIVISION.                                              OBIC14.2
005700 SECT-IC218-0001 SECTION.                                         OBIC14.2
005800 CALL-IC219.                                                      OBIC14.2
005900     MOVE 0 TO SORT-LINK.                                         OBIC14.2
006000     CALL "OBIC2A" USING SORT-LINK.                               OBIC14.2
006100 CALL-FAIL.                                                       OBIC14.2
006200*                                                                 OBIC14.2
006300*    CONTROL SHOULD NOT RETURN TO THE MAIN PROGRAM FROM THE SUB-  OBIC14.2
006400*    PROGRAM SINCE THE SUBPROGRAM CONTAINS A STOP RUN STATEMENT.  OBIC14.2
006500*                                                                 OBIC14.2
006600     MOVE 2 TO PRINT-FLAG.                                        OBIC14.2
006700     MOVE "CALL-MAIN-IC" TO PARAGRAPH-NAME.                       OBIC14.2
006800     MOVE "CONTROL RETURNED" TO FEATURE-TESTED.                   OBIC14.2
006900     MOVE "FAIL " TO PASS-OR-FAIL.                                OBIC14.2
007000     MOVE 0 TO R-COUNT.                                           OBIC14.2
007100     MOVE SORT-LINK TO COMPUTED-SORT-KEY.                         OBIC14.2
007200     MOVE SPACE TO CORRECT-SORT-KEY.                              OBIC14.2
007300     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC14.2
007400     MOVE 3 TO PRINT-FLAG.                                        OBIC14.2
007500     CALL "OBIC3A" USING PRINT-LINE-VALUES  PRINT-FLAG.           OBIC14.2
007600*                                                                 OBIC14.2
007700*        NO STOP RUN STATEMENT APPEARS IN THIS PROGRAM.           OBIC14.2
007800*                                                                 OBIC14.2
007900 END-OF-PROGRAM.                                                  OBIC14.2
008000     EXIT PROGRAM.                                                OBIC14.2
      *END-OF,OBIC1A                                                                  
