/*****************************************************************************

hput - program to get a file from HPSS using the client API

Command format:

  hget -b buffer_size -c cos -d -v -r local_file hpss_destination

  Where: -b  -  buffer size
         -c  -  specifies cos, default = 1
         -d  -  turns on debugging
         -v  -  turns on verbose output
         -r  -  replace existing file

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
int replace = 0;

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

int putFile(localfile,destination,cos,buffersize)
char *localfile, *destination;
int cos, buffersize;
{
  hpss_cos_hints_t hints, hintsx;
  hpss_cos_priorities_t priority;
  hpss_fileattr_t attr;
  int status, i, n;
  int fd, hpssfd;
  struct stat sbuf;
  char *p, *q, *sp;
  char pathname[2048];
  char errmsg[3000];
  char *buffer;

  if (strcmp(localfile,"stdin") == 0) fd = 0;
  else {
    status = stat(localfile,&sbuf);     /*verify that the file exists*/
    if (status == 0) {
      if (S_ISDIR(sbuf.st_mode)) {
        fprintf(stderr,"%s: \"%s\" is a directory, you must specify a file\n",program,localfile);
        exit(1);
      }
    }
    else {
      sprintf(errmsg,"%s: Error opening \"%s\" for reading",program,localfile);
      perror(errmsg);
      exit(1);
    }
  
    fd = open(localfile,O_RDONLY);     /*attempt to open the file for reading*/
    if (fd < 0) {
      sprintf(errmsg,"%s: Error opening \"%s\" for reading",program,localfile);
      perror(errmsg);
      fprintf(stderr,"%s: %s\n",program,pathname);
      exit(1);
    }
  }

  hpssfd = -1;      /*indicates that the HPSS file has not been opened*/

  strcpy(pathname,destination);

  memset(&hints,0x0,sizeof(hints));
  memset(&hintsx,0x0,sizeof(hintsx));
  memset(&priority,0x0,sizeof(priority));
  priority.COSIdPriority = REQUIRED_PRIORITY;
  printf("COS: %d\n",cos);
  hints.COSId = cos;

  status = hpss_FileGetAttributes(pathname,&attr);      /*get the file information from HPSS*/
  if (status == 0) {                                    /*if the file/directory exists in HPSS*/
    if (attr.Attrs.Type == NS_OBJECT_TYPE_DIRECTORY) {  /*if the destination is a directory*/
      sprintf(pathname,"%s/%s",destination,findFileName(localfile));   /*form the new pathname*/
      status = hpss_FileGetAttributes(pathname,&attr);  /*get the file information from HPSS*/
    }
    if (status == 0) {
      if (attr.Attrs.Type != NS_OBJECT_TYPE_FILE) {
        fprintf(stderr,"%s: the specified HPSS name is not a file\n",program);
        exit(1);
      }
      if (replace == 0) { 
        fprintf(stderr,"%s: the HPSS file \"%s\" exists, specify -r to replace\n",program,pathname);
        exit(1);
      }
      hpssfd = hpss_Open(pathname,O_WRONLY,0,&hints,&priority,&hintsx);
      if (hpssfd < 0) {
        fprintf(stderr,"%s: error opening HPSS file \"%s\" for writing\n",program,pathname);
        exit(1);
      }
    }
  }
  if (hpssfd == -1) {
    hpssfd = hpss_Open(pathname,O_CREAT|O_WRONLY,0600,&hints,&priority,&hintsx);
    if (hpssfd < 0) {
      fprintf(stderr,"%s: error opening HPSS file \"%s\" for writing (2)\n",program,pathname);
      exit(1);
    }
  }

  if (debug) printf("Destination: %s\n",destination);

  buffer = malloc(buffersize);

  while (1) {
    n = read(fd,buffer,buffersize);
    if (debug) printf("%d\n",n);
    if (n <= 0) break;
    status = hpss_Write(hpssfd,buffer,n);
    if (status < 0) {
      fprintf(stderr,"%s: error writing to HPSS.\n",program);
      exit(1);
    }
    if (status != n) {
      fprintf(stderr,"%S: attempted to write %d bytes, wrote: %d\n",program,n,status);
      exit(1);
    }
    if (n < buffersize) break;
  }
}

/****************************************************************************/

printUsage(status)
int status;
{
  fprintf(stderr,"\n");
  fprintf(stderr,"Usage:  hput -b buffer_size -c cos -d -h -v local_pathname hpss_destination\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Where: -d  debug\n");
  fprintf(stderr,"       -h  output command line usage information\n");
  fprintf(stderr,"       -v  turns on verbose output\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Examples: hput test .\n");
  fprintf(stderr,"          hput hput.c hput.c.backup\n");
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
  char *localfile;
  char *hpssdestination;
  int l;
  int cos = 0;
  int buffersize = 0;

  program = argv[0];    /*save program name for error messages*/

  if (argc == 1) printUsage(1);

  argc--;    /*process command line arguments*/
  argv++;    
  while( (argc > 0) && (**argv == '-')) {
    argp = *argv++;
    argc--;
    argp++;
    switch (*argp) {
    case 'b':
      if (argc == 0) {
        fprintf(stderr,"%s: value missing for -b option\n",program);
        exit(1);
      }
      string = *argv++;
      argc--;
      l = strlen(string);
      c = string[l-1];
      buffersize = atoi(string);
      if ((c == 'k') || (c == 'K')) buffersize = buffersize * 1024;
      else if ((c == 'm') || (c == 'M')) buffersize = buffersize * 1024 * 1024;
      else if (!((c > '0') && (c <= '9'))) {
        fprintf(stderr,"%s: unrecognized multiplier, should be k, K, m or M\n",program);
        exit(1);
      }
      break;
    case 'c':
      if (argc == 0) {
        fprintf(stderr,"%s: value missing for -c option\n",program);
        exit(1);
      }
      cos = atoi(*argv++);
      argc--;
      break;
    case 'd':
      debug = 1;
      break;
    case 'h':
      printUsage();
      exit(0);
      break;
    case 'r':
      replace = 1;
      break;
    case 'v':
      verbose = 1;
      break;
    default:
      fprintf(stderr,"%s: illegal option: %s\n",program,--argp);
      printUsage();
      exit(1);
    }
  }

  if (argc == 0) {
    fprintf(stderr,"%s: HPSS file is not specified\n",program);
    printUsage(1);
  }

  localfile = *argv++;
  argc--;
    
  if (argc == 0) {
    fprintf(stderr,"%s: local destination is not specified\n",program);
    printUsage(1);
  }

  hpssdestination = *argv++;
  argc--;

  if (argc > 0) {
    fprintf(stderr,"%s: too many arguments on command line\n",program);
    printUsage(1);
  }

  if (cos == 0) cos = 1;
  if (buffersize == 0) buffersize = 1024 * 1024;

  if (verbose) {
    printf("%s: using a buffer size of %d bytes\n",program,buffersize);
    printf("%s: Placing the file in class of service %d\n",program,cos);
  }

  putFile(localfile,hpssdestination,cos,buffersize);

  exit(0);
}
