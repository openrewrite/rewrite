      *HEADER,COBOL,DB302M                                                            
000100 IDENTIFICATION DIVISION.                                         DB3024.2
000200 PROGRAM-ID.                                                      DB3024.2
000300     DB302M.                                                      DB3024.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF LEVEL 1              DB3024.2
000500*OBSOLETE FEATURES THAT ARE USED IN DEBUGGING.                    DB3024.2
000600 ENVIRONMENT DIVISION.                                            DB3024.2
000700 CONFIGURATION SECTION.                                           DB3024.2
000800 SOURCE-COMPUTER.                                                 DB3024.2
000900     XXXXX082                                                     DB3024.2
001000     WITH DEBUGGING MODE.                                         DB3024.2
001100 OBJECT-COMPUTER.                                                 DB3024.2
001200     XXXXX083.                                                    DB3024.2
001300                                                                  DB3024.2
001400 INPUT-OUTPUT SECTION.                                            DB3024.2
001500 FILE-CONTROL.                                                    DB3024.2
001600     SELECT TFIL ASSIGN                                           DB3024.2
001700     XXXXX014                                                     DB3024.2
001800         ORGANIZATION IS SEQUENTIAL                               DB3024.2
001900         ACCESS MODE IS SEQUENTIAL.                               DB3024.2
002000                                                                  DB3024.2
002100 DATA DIVISION.                                                   DB3024.2
002200 FILE SECTION.                                                    DB3024.2
002300 FD TFIL.                                                         DB3024.2
002400 01 FREC.                                                         DB3024.2
002500     03 RKEY  PIC 9(8).                                           DB3024.2
002600                                                                  DB3024.2
002700 PROCEDURE DIVISION.                                              DB3024.2
002800                                                                  DB3024.2
002900 DECLARATIVES.                                                    DB3024.2
003000                                                                  DB3024.2
003100 BUGGING SECTION.                                                 DB3024.2
003200                                                                  DB3024.2
003300     USE FOR DEBUGGING ON DB302M-CONTROL.                         DB3024.2
003400*Message expected for above statement: OBSOLETE                   DB3024.2
003500                                                                  DB3024.2
003600 END DECLARATIVES.                                                DB3024.2
003700                                                                  DB3024.2
003800 DB302-FLAGS SECTION.                                             DB3024.2
003900                                                                  DB3024.2
004000 DB302M-CONTROL.                                                  DB3024.2
004100     DISPLAY "THIS IS A DUMMY PROCEDURE".                         DB3024.2
004200     STOP RUN.                                                    DB3024.2
004300                                                                  DB3024.2
004400*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              DB3024.2
      *END-OF,DB302M                                                                  
