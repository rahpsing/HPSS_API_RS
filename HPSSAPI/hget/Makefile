LOCAL_ROOT    = /opt/hpss
ROOT_SUBPATH  = .

include $(LOCAL_ROOT)/Makefile.macros

COMPFLAGS      = -g

#
# Common
#
                                                                                
INCLUDE_PATH	= -I. -I$(LOCAL_INCLUDE) -I$(DB_INSTALL_PATH)/include
CFLAGS		= $(INCLUDE_PATH) $(COMPFLAGS) $(MACHINE_FLAGS)

SRCS		= hget.c

OBJS		=  $(SRCS:.c=.o) $(CS_SRCS:.c=.o)

.c.o:;		@echo "Compiling $<"...
		@$(CC) $(CFLAGS) -c $<

LIBS		= -L$(BUILD_LIB) -lhpsstools -lhpss -lhpsscs

LDFLAGS         = $(COMPFLAGS) $(RUNFLAGS)

PROGRAMS	= hget

all:		$(CS_OBJS) $(PROGRAMS)

clean:;		/bin/rm -f $(OBJS) $(PROGRAMS)

hget:		hget.o 
		@echo "Linking $@"...
		@$(CC) $@.o -o $@ $(CFLAGS) $(LDFLAGS) -W1,-rpath=/opt/hpss/lib $(LIBS)

install:	hget
		cp hget /usr/local/bin
		chmod 755 /usr/local/bin/hget

#===============================================================================
# Include common rules
#===============================================================================

include $(LOCAL_ROOT)/Makefile.rules

# DO NOT DELETE THIS LINE -- make depend depends on it.

