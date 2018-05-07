/*****************************************************************************

hls - program to get a directory listing using the client API

Command format:

  hls directory

  Where: -d  -  turns on debugging
         -v  -  turns on verbose output
         -x  -  output timing information

Examples:

    $ kinit username
    $ hput test .
    $ hput test Dir/test123

To compile:

  # make

Jeff Russ, UITS, Indiana University, November 2015

*****************************************************************************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <malloc.h>
#include <unistd.h>
#include <strings.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <ctype.h>
#include <rpc/types.h>
#include <rpc/xdr.h>

#include "hpss_api.h"
#include "hpss_errno.h"

#include "ns_Constants.h"

/****************************************************************************/

char *program;

int debug = 0;
int verbose = 0;
int xflag = 0;

/****************************************************************************/

ls(directory)
char *directory;
{
  int dirhandle, status;
  hpss_dirent_t dirbuf;
  hpss_stat_t statbuf;
  char name[2049];
  char fullpath[2049];
  time_t before, after, delta;
  int n = 0;

  dirhandle = hpss_Opendir(directory);
  if (dirhandle < 0) {
    fprintf(stderr,"%s: error opening directory \"%s\", error code = %d\n",program,directory,dirhandle);
    exit(1);
  }
  
  printf("\nPath name is: %s", directory);

  while (1) {
    status = hpss_Readdir(dirhandle,&dirbuf);
    if (status != 0) {
      fprintf(stderr,"%s: hpss_Readdir() returned %d\n",program,status);
      exit(1);
    }
	printf("\n Directory name: %s",dirbuf.d_name);
    if (dirbuf.d_namelen == 0) break;
    n++;
    strcpy(name,dirbuf.d_name);
    if (xflag) {
      sprintf(fullpath,"%s/%s",directory,name);
      time(&before);
      hpss_Stat(fullpath,&statbuf);
      time(&after);
      delta = after - before;
      printf("%6d %s\n",delta,fullpath);
    }
    else printf("%5d %s\n",n,name);
  }
}

/****************************************************************************/

char *findFileName(pathname)
char *pathname;
{
  char c, *q, *p;

  q = p = pathname;

  while (c = *p++) {
    if (c == '/') q = p;
  } 
  return q;
}

/****************************************************************************/

printUsage(status)
int status;
{
  fprintf(stderr,"\n");
  fprintf(stderr,"Usage:  hls -d -v directory\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Where: -d  debug\n");
  fprintf(stderr,"       -h  output command line usage information\n");
  fprintf(stderr,"       -v  turns on verbose output\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Examples: hls .\n");
  fprintf(stderr,"          hls /hpss/r/u/russ\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"\n");
  exit(status);
}

/****************************************************************************/

main(argc,argv)
int argc;
char **argv;
{
  char *argp, c, *string;
  char *directory;
  int l;

  program = argv[0];    /*save program name for error messages*/

  if (argc == 1) printUsage(1);

  argc--;    /*process command line arguments*/
  argv++;    
  while( (argc > 0) && (**argv == '-')) {
    argp = *argv++;
    argc--;
    argp++;
    switch (*argp) {
    case 'd':
      debug = 1;
      break;
    case 'h':
      printUsage();
      exit(0);
      break;
    case 'v':
      verbose = 1;
      break;
    case 'x':
      xflag = 1;
      break;
    default:
      fprintf(stderr,"%s: illegal option: %s\n",program,--argp);
      printUsage();
      exit(1);
    }
  }

  if (argc == 0) {
    fprintf(stderr,"%s: HPSS directory is not specified\n",program);
    printUsage(1);
  }

  directory = *argv++;
  argc--;
    
  if (argc > 0) {
    fprintf(stderr,"%s: too many arguments on command line\n",program);
    printUsage(1);
  }

  ls(directory);

  exit(0);
}
