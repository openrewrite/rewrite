      *HEADER,COBOL,DB305M                                                            
000100 IDENTIFICATION DIVISION.                                         DB3054.2
000200 PROGRAM-ID.                                                      DB3054.2
000300     DB305M.                                                      DB3054.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF LEVEL 1              DB3054.2
000500*OBSOLETE FEATURES THAT ARE USED IN DEBUGGING.                    DB3054.2
000600 ENVIRONMENT DIVISION.                                            DB3054.2
000700 CONFIGURATION SECTION.                                           DB3054.2
000800 SOURCE-COMPUTER.                                                 DB3054.2
000900     XXXXX082                                                     DB3054.2
001000     WITH DEBUGGING MODE.                                         DB3054.2
001100 OBJECT-COMPUTER.                                                 DB3054.2
001200     XXXXX083.                                                    DB3054.2
001300                                                                  DB3054.2
001400 INPUT-OUTPUT SECTION.                                            DB3054.2
001500 FILE-CONTROL.                                                    DB3054.2
001600     SELECT TFIL ASSIGN                                           DB3054.2
001700     XXXXX014                                                     DB3054.2
001800         ORGANIZATION IS SEQUENTIAL                               DB3054.2
001900         ACCESS MODE IS SEQUENTIAL.                               DB3054.2
002000                                                                  DB3054.2
002100 DATA DIVISION.                                                   DB3054.2
002200 FILE SECTION.                                                    DB3054.2
002300 FD TFIL.                                                         DB3054.2
002400 01 FREC.                                                         DB3054.2
002500     03 RKEY  PIC 9(8).                                           DB3054.2
002600                                                                  DB3054.2
002700 PROCEDURE DIVISION.                                              DB3054.2
002800                                                                  DB3054.2
002900 DECLARATIVES.                                                    DB3054.2
003000                                                                  DB3054.2
003100 BUGGING SECTION.                                                 DB3054.2
003200                                                                  DB3054.2
003300     USE FOR DEBUGGING ON ALL PROCEDURES.                         DB3054.2
003400*Message expected for above statement: OBSOLETE                   DB3054.2
003500                                                                  DB3054.2
003600 END DECLARATIVES.                                                DB3054.2
003700                                                                  DB3054.2
003800 DB305-FLAGS SECTION.                                             DB3054.2
003900                                                                  DB3054.2
004000 DB305M-CONTROL.                                                  DB3054.2
004100     DISPLAY "THIS IS A DUMMY PROCEDURE".                         DB3054.2
004200     STOP RUN.                                                    DB3054.2
004300                                                                  DB3054.2
004400*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              DB3054.2
      *END-OF,DB305M                                                                  
