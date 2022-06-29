      *HEADER,COBOL,DB301M                                                            
000100 IDENTIFICATION DIVISION.                                         DB3014.2
000200 PROGRAM-ID.                                                      DB3014.2
000300     DB301M.                                                      DB3014.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF LEVEL 1              DB3014.2
000500*FEATURES OF THE DEBUGGING MODULE.                                DB3014.2
000600 ENVIRONMENT DIVISION.                                            DB3014.2
000700 CONFIGURATION SECTION.                                           DB3014.2
000800 SOURCE-COMPUTER.                                                 DB3014.2
000900     XXXXX082                                                     DB3014.2
001000     WITH DEBUGGING MODE.                                         DB3014.2
001100 OBJECT-COMPUTER.                                                 DB3014.2
001200     XXXXX083.                                                    DB3014.2
001300 INPUT-OUTPUT SECTION.                                            DB3014.2
001400 FILE-CONTROL.                                                    DB3014.2
001500     SELECT TFIL ASSIGN                                           DB3014.2
001600     XXXXX014                                                     DB3014.2
001700         ORGANIZATION IS SEQUENTIAL                               DB3014.2
001800         ACCESS MODE IS SEQUENTIAL.                               DB3014.2
001900 DATA DIVISION.                                                   DB3014.2
002000 FILE SECTION.                                                    DB3014.2
002100 FD TFIL.                                                         DB3014.2
002200 01 FREC.                                                         DB3014.2
002300     03 RKEY PIC 9(8).                                            DB3014.2
002400                                                                  DB3014.2
002500 PROCEDURE DIVISION.                                              DB3014.2
002600                                                                  DB3014.2
002700 DECLARATIVES.                                                    DB3014.2
002800                                                                  DB3014.2
002900                                                                  DB3014.2
003000 BUGGING-2 SECTION.                                               DB3014.2
003100                                                                  DB3014.2
003200                                                                  DB3014.2
003300     USE FOR DEBUGGING ON ALL PROCEDURES.                         DB3014.2
003400*Message expected for above statement: NON-CONFORMING STANDARD    DB3014.2
003500                                                                  DB3014.2
003600                                                                  DB3014.2
003700 END DECLARATIVES.                                                DB3014.2
003800                                                                  DB3014.2
003900 DB301M-FLAGS SECTION.                                            DB3014.2
004000 DB301M-CONTROL.                                                  DB3014.2
004100     DISPLAY "THIS IS A DUMMY PROCEDURE".                         DB3014.2
004200     STOP RUN.                                                    DB3014.2
004300                                                                  DB3014.2
004400                                                                  DB3014.2
004500*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              DB3014.2
      *END-OF,DB301M                                                                  
