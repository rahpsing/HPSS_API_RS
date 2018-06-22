/*****************************************************************************

hget - program to get a file from HPSS using the client API

Command format:

  hget -b buffer_size -d -v -r hpss_pathname local_destination

  Where: -d  -  turns on debugging
         -v  -  turns on verbose output
         -r  -  replace existing file

Examples:

    $ kinit username
    $ hget test /tmp
    $ hget test /tmp/test123

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
int buffersize = 4096 * 4096;

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

int getFile(hpssfile,destination)
char *hpssfile, *destination;
{
  hpssoid_t oid;
  hpss_fileattr_t attr;
  int status, i, n;
  int fd, hpssfd;
  struct stat sbuf;
  char *p, *q, *sp;
  char pathname[2048];
  char *buffer;
 
   printf("\nHpssfile value: %s", hpssfile);
   printf("\nDestination value: %s", destination);
  status = hpss_FileGetAttributes(hpssfile,&attr);   /*get the file information from HPSS*/
  printf("\nStatus is: %d\n", status);
  if (status != 0) {
    printf("%s: hpss_FileGetAttributes() returned %d, pathname = %s\n",program,status,hpssfile);
    exit(1);
  }

  if (attr.Attrs.Type == NS_OBJECT_TYPE_DIRECTORY) {
    fprintf(stderr,"%s: the specified HPSS name is a directory\n",program);
    return 1;
  }
  
  if (attr.Attrs.Type != NS_OBJECT_TYPE_FILE) {
    fprintf(stderr,"%s: the specified HPSS name is not a file\n",program);
    return 1;
  }
  
  if ((strcmp(destination,"stdout") == 0) || (strcmp(destination,"-") == 0)) fd = 1;
  else {
    strcpy(pathname,destination);
  
    if (debug) printf("Destination: %s\n",destination);
  
    status = stat(destination,&sbuf);
    if (status == 0) {
      if (S_ISDIR(sbuf.st_mode)) {
        sprintf(pathname,"%s/%s",destination,findFileName(hpssfile));
        status = stat(pathname,&sbuf);
      }
    }
    if (status == 0) {
      if (S_ISDIR(sbuf.st_mode)) {
        printf("destination is a directory\n");
      }
      else if (S_ISREG(sbuf.st_mode)) {
        printf("destination is an existing file\n");
        fd = open(pathname,O_WRONLY);
        if (fd < 0) {
          perror("open");
          exit(1);
        }
      }
      else {
        fprintf(stderr,"%s: invalid destination\n",program);
        return 1;
      }
    } 
    else {
      fd = open(pathname,O_CREAT|O_WRONLY,0600);
      if (fd < 0) {
        perror("open");
        fprintf(stderr,"%s: %s\n",program,pathname);
        exit(1);
      }
    }
  }
 
  hpssfd = hpss_Open(hpssfile,O_RDONLY,0,NULL,NULL,NULL);
  if (hpssfd < 0) {
    fprintf(stderr,"%s: error opening HPSS file %s for reading\n",program,hpssfile);
    exit(1);
  }

  buffer = malloc(buffersize);

  while (1) {
    n = hpss_Read(hpssfd,buffer,buffersize);
    if (debug) printf("%d\n",n);
    if (n <= 0) break;
    status = write(fd,buffer,n);
    if (status < 0) {
      perror("write");
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
  fprintf(stderr,"Usage:  hget -d -h -v hpss_pathname local_destination\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Where: -d  debug\n");
  fprintf(stderr,"       -h  output command line usage information\n");
  fprintf(stderr,"       -v  turns on verbose output\n");
  fprintf(stderr,"\n");
  fprintf(stderr,"Examples: hget test \n");
  fprintf(stderr,"          hget /log/i2015/10/logfile.2015.1028.035631.2015.1029.000208 .\n");
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
  char *hpssfile;
  char *destination;
  int l;

  program = argv[0];    /*save program name for error messages*/

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

  hpssfile = *argv++;
  argc--;
    
  if (argc == 0) {
    fprintf(stderr,"%s: local destination is not specified\n",program);
    printUsage(1);
  }

  destination = *argv++;
  argc--;

  if (argc > 0) {
    fprintf(stderr,"%s: too many arguments on command line\n",program);
    printUsage(1);
  }

  if (verbose) printf("%s: using a buffer size of %d bytes\n",program,buffersize);

  getFile(hpssfile,destination);

  exit(0);
}
