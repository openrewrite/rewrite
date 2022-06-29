      *HEADER,COBOL,IC401M                                                            
000100 IDENTIFICATION DIVISION.                                         IC4014.2
000200 PROGRAM-ID.                                                      IC4014.2
000300      IC401M IS INITIAL.                                          IC4014.2
000400*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
000500                                                                  IC4014.2
000600*The following program tests the flagging of high                 IC4014.2
000700*subset Features that are used in inter-program                   IC4014.2
000800*communication.                                                   IC4014.2
000900 ENVIRONMENT DIVISION.                                            IC4014.2
001000 CONFIGURATION SECTION.                                           IC4014.2
001100 SOURCE-COMPUTER.                                                 IC4014.2
001200     XXXXX082.                                                    IC4014.2
001300 OBJECT-COMPUTER.                                                 IC4014.2
001400     XXXXX083.                                                    IC4014.2
001500 DATA DIVISION.                                                   IC4014.2
001600 WORKING-STORAGE SECTION.                                         IC4014.2
001700                                                                  IC4014.2
001800 01 GLOB IS GLOBAL   PIC IS X(2) VALUE IS "HI".                   IC4014.2
001900*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
002000                                                                  IC4014.2
002100 01 EXTE IS EXTERNAL PIC IS X(5).                                 IC4014.2
002200*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
002300                                                                  IC4014.2
002400 PROCEDURE DIVISION.                                              IC4014.2
002500                                                                  IC4014.2
002600 DECLARATIVES.                                                    IC4014.2
002700                                                                  IC4014.2
002800 IC401M-USE SECTION.                                              IC4014.2
002900     USE GLOBAL AFTER STANDARD ERROR PROCEDURE ON I-O.            IC4014.2
003000*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
003100                                                                  IC4014.2
003200 END DECLARATIVES.                                                IC4014.2
003300 IC401M-NONDECL SECTION.                                          IC4014.2
003400 IC401M-CONTROL.                                                  IC4014.2
003500     PERFORM IC401M-CANCEL THRU IC401M-BYCONT.                    IC4014.2
003600     STOP RUN.                                                    IC4014.2
003700                                                                  IC4014.2
003800 IC401M-CANCEL.                                                   IC4014.2
003900     CANCEL "NESTEDPROG".                                         IC4014.2
004000*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
004100                                                                  IC4014.2
004200                                                                  IC4014.2
004300 IC401M-BYREF.                                                    IC4014.2
004400     CALL "NESTEDPROG" USING BY REFERENCE GLOB.                   IC4014.2
004500*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
004600                                                                  IC4014.2
004700                                                                  IC4014.2
004800 IC401M-BYCONT.                                                   IC4014.2
004900     CALL "FIC401M" USING BY CONTENT GLOB.                        IC4014.2
005000*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
005100                                                                  IC4014.2
005200                                                                  IC4014.2
005300 IDENTIFICATION DIVISION.                                         IC4014.2
005400*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
005500                                                                  IC4014.2
005600 PROGRAM-ID.                                                      IC4014.2
005700     NESTEDPROG IS COMMON.                                        IC4014.2
005800*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
005900                                                                  IC4014.2
006000 ENVIRONMENT DIVISION.                                            IC4014.2
006100 DATA DIVISION.                                                   IC4014.2
006200 LINKAGE SECTION.                                                 IC4014.2
006300 01  GLOB-2  PIC X(2).                                            IC4014.2
006400                                                                  IC4014.2
006500 PROCEDURE DIVISION USING GLOB-2.                                 IC4014.2
006600 DUMMY-PARA.                                                      IC4014.2
006700     DISPLAY "HELLO".                                             IC4014.2
006800                                                                  IC4014.2
006900 END-PARA.                                                        IC4014.2
007000*TOTAL NUMBER OF FLAGS EXPECTED = 11.                             IC4014.2
007100 END PROGRAM NESTEDPROG.                                          IC4014.2
007200*Message expected for above statement: NON-CONFORMING STANDARD    IC4014.2
007300*Message expected for following statement: NON-CONFORMING STANDARDIC4014.2
007400 END PROGRAM IC401M.                                              IC4014.2
      *END-OF,IC401M                                                                  
