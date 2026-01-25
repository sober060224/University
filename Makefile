# 定义变量（推荐做法，便于修改）
CC = gcc
TARGET = program
OBJS = main.o hello.o

# 默认规则：链接生成可执行文件
# 目标是 program，依赖是所有的 .o 文件
$(TARGET):  $(OBJS)
	$(CC)  $^ -o  $@

# 编译规则：将 .c 编译成 .o
# 这里使用了模式规则 '%'，代表任意名字
%.o: %.c
	$(CC) -c  $< -o  $@

# 清理规则：删除生成的文件
# .PHONY 声明这是一个“伪目标”，不是真实文件
.PHONY: clean
clean:
	rm -f  $(OBJS)  $(TARGET)