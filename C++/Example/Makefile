CC = gcc
CFLAGS = -g -Wall -c
LDFLAGS = -g -Wall
TARGET = hello
TARGET_OBJECTS = hello.o

$(TARGET) : $(TARGET_OBJECTS)
	$(CC) $(LDFLAGS) -o $(TARGET) $<

%.o : %.c
	$(CC) $(CFLAGS) $<

clean:
	rm -f *.o $(TARGET) $(TARGET_OBJECTS)