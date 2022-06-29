      *HEADER,CLBRY,KP001                                                             
000100 PST-TEST-001.                                                    KP0014.2
000200     MOVE    "PSEUDO-TEXT" TO FEATURE.                            KP0014.2
000300*    THIS        TEXT IS COPIED INTO A SOURCE PROGRAM THE PSEUDO  KP0014.2
000400*    TEXT CONTAINING PERFORM FAIL  IS REPLACED WITH A NULL        KP0014.2
000500*    PSEUDO TEXT.                                                 KP0014.2
000600     MOVE    "PST-TEST-001" TO PAR-NAME                           KP0014.2
000700     PERFORM PASS.                                                KP0014.2
000800     PERFORM FAIL.                                                KP0014.2
000900 PST-WRITE-001.                                                   KP0014.2
001000     PERFORM PRINT-DETAIL.                                        KP0014.2
      *END-OF,KP001                                                                   
