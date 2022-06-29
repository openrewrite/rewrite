      *HEADER,COBOL,DB303M                                                            
000100 IDENTIFICATION DIVISION.                                         DB3034.2
000200 PROGRAM-ID.                                                      DB3034.2
000300     DB303M.                                                      DB3034.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF LEVEL 2              DB3034.2
000500*OBSOLETE FEATURES THAT ARE USED IN DEBUGGING.                    DB3034.2
000600 ENVIRONMENT DIVISION.                                            DB3034.2
000700 CONFIGURATION SECTION.                                           DB3034.2
000800 SOURCE-COMPUTER.                                                 DB3034.2
000900     XXXXX082                                                     DB3034.2
001000     WITH DEBUGGING MODE.                                         DB3034.2
001100 OBJECT-COMPUTER.                                                 DB3034.2
001200     XXXXX083.                                                    DB3034.2
001300 INPUT-OUTPUT SECTION.                                            DB3034.2
001400 FILE-CONTROL.                                                    DB3034.2
001500     SELECT TFIL ASSIGN                                           DB3034.2
001600     XXXXX014                                                     DB3034.2
001700         ORGANIZATION IS SEQUENTIAL                               DB3034.2
001800         ACCESS MODE IS SEQUENTIAL.                               DB3034.2
001900 DATA DIVISION.                                                   DB3034.2
002000 FILE SECTION.                                                    DB3034.2
002100 FD TFIL.                                                         DB3034.2
002200 01 FREC.                                                         DB3034.2
002300     03 RKEY PIC 9(8).                                            DB3034.2
002400                                                                  DB3034.2
002500                                                                  DB3034.2
002600 PROCEDURE DIVISION.                                              DB3034.2
002700                                                                  DB3034.2
002800 DECLARATIVES.                                                    DB3034.2
002900                                                                  DB3034.2
003000                                                                  DB3034.2
003100 BUGGING-2 SECTION.                                               DB3034.2
003200                                                                  DB3034.2
003300     USE FOR DEBUGGING ON ALL REFERENCES OF FREC.                 DB3034.2
003400*Message expected for above statement: OBSOLETE                   DB3034.2
003500                                                                  DB3034.2
003600                                                                  DB3034.2
003700                                                                  DB3034.2
003800 BUGGING-3 SECTION.                                               DB3034.2
003900                                                                  DB3034.2
004000                                                                  DB3034.2
004100     USE FOR DEBUGGING ON TFIL.                                   DB3034.2
004200*Message expected for above statement: OBSOLETE                   DB3034.2
004300                                                                  DB3034.2
004400 END DECLARATIVES.                                                DB3034.2
004500                                                                  DB3034.2
004600 DB303M-FLAGS SECTION.                                            DB3034.2
004700                                                                  DB3034.2
004800 DB303M-CONTROL.                                                  DB3034.2
004900     DISPLAY "THIS IS A DUMMY PROCEDURE".                         DB3034.2
005000     STOP RUN.                                                    DB3034.2
005100                                                                  DB3034.2
005200*TOTAL NUMBER OF FLAGS EXPECTED = 2.                              DB3034.2
      *END-OF,DB303M                                                                  
