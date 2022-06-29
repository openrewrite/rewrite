      *HEADER,COBOL,DB304M                                                            
000100 IDENTIFICATION DIVISION.                                         DB3044.2
000200 PROGRAM-ID.                                                      DB3044.2
000300     DB304M.                                                      DB3044.2
000400*THE FOLLOWING PROGRAM TESTS THE FLAGGING OF OBSOLETE             DB3044.2
000500*LEVEL 2 COMMUNICATION FEATURES THAT ARE USED IN DEBUGGING.       DB3044.2
000600*THIS TEST NEED NOT BE RUN IF COMMUNICATIONS NOT IMPLEMTENTED.    DB3044.2
000700 ENVIRONMENT DIVISION.                                            DB3044.2
000800 CONFIGURATION SECTION.                                           DB3044.2
000900 SOURCE-COMPUTER.                                                 DB3044.2
001000     XXXXX082                                                     DB3044.2
001100     WITH DEBUGGING MODE.                                         DB3044.2
001200 OBJECT-COMPUTER.                                                 DB3044.2
001300     XXXXX083.                                                    DB3044.2
001400 DATA DIVISION.                                                   DB3044.2
001500 COMMUNICATION SECTION.                                           DB3044.2
001600                                                                  DB3044.2
001700 CD COMMNAME FOR INPUT.                                           DB3044.2
001800 01 CREC.                                                         DB3044.2
001900     03 CNAME1 PIC 9(8).                                          DB3044.2
002000     03 FILLER PIC X(79).                                         DB3044.2
002100                                                                  DB3044.2
002200 PROCEDURE DIVISION.                                              DB3044.2
002300                                                                  DB3044.2
002400 DECLARATIVES.                                                    DB3044.2
002500                                                                  DB3044.2
002600 BUGGING SECTION.                                                 DB3044.2
002700                                                                  DB3044.2
002800     USE FOR DEBUGGING ON COMMNAME.                               DB3044.2
002900*Message expected for above statement: OBSOLETE                   DB3044.2
003000                                                                  DB3044.2
003100                                                                  DB3044.2
003200                                                                  DB3044.2
003300 END DECLARATIVES.                                                DB3044.2
003400                                                                  DB3044.2
003500 DB304M-FLAGS SECTION.                                            DB3044.2
003600                                                                  DB3044.2
003700 DB304M-CONTROL.                                                  DB3044.2
003800     DISPLAY "THIS IS A DUMMY PROCEDURE".                         DB3044.2
003900     STOP RUN.                                                    DB3044.2
004000                                                                  DB3044.2
004100*TOTAL NUMBER OF FLAGS EXPECTED = 1.                              DB3044.2
      *END-OF,DB304M                                                                  
