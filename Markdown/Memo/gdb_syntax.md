## 三、编译带调试信息的程序

要使用GDB调试，编译时需要添加 `-g` 选项：

```bash
# C程序
gcc -g program.c -o program

# C++程序
g++ -g program.cpp -o program

# 更完整的编译选项（推荐）
gcc -g -O0 -Wall program.c -o program
# -O0：关闭优化，便于调试
# -Wall：显示所有警告
# -g：包含调试符号信息
```

## 四、启动GDB

### 基本启动方式
```bash
# 方式1：直接启动
gdb ./program

# 方式2：启动后加载程序
gdb
(gdb) file ./program

# 方式3：带参数启动
gdb --args ./program arg1 arg2
```

## 五、基础调试命令

### 1. 运行程序
```bash
run                 # 运行程序
run arg1 arg2       # 带参数运行
start               # 开始执行并在main函数第一行暂停
```

### 2. 断点管理
```bash
break main          # 在main函数设置断点
break 10            # 在第10行设置断点
break file.c:15     # 在指定文件第15行设置断点
break function_name # 在指定函数设置断点

info breakpoints    # 查看所有断点
delete 1            # 删除1号断点
delete              # 删除所有断点
clear 10            # 删除第10行的断点

# 条件断点
break 20 if i==5    # 当i等于5时在第20行中断
```

### 3. 单步执行
```bash
next        # 执行下一行（不进入函数）
step        # 执行下一行（进入函数）
continue    # 继续运行直到下一个断点
finish      # 运行到当前函数返回
until       # 运行到指定行或循环结束
```

### 4. 查看代码
```bash
list        # 显示当前行附近代码
list 10     # 显示第10行附近代码
list main   # 显示main函数代码
list 1,20   # 显示1-20行代码
```

### 5. 查看变量和内存
```bash
print x         # 打印变量x的值
print *ptr      # 打印指针指向的值
print array[0]@10  # 打印数组前10个元素

display x       # 每次暂停时自动显示x的值
info display    # 查看所有自动显示
undisplay 1     # 取消1号自动显示

whatis variable # 查看变量类型
ptype variable  # 查看详细类型信息

x/10x &array    # 以十六进制查看数组内存
x/10c str       # 以字符形式查看字符串
```

### 6. 调用栈和帧
```bash
backtrace       # 查看调用栈（简写 bt）
frame 0         # 切换到0号栈帧
info frame      # 查看当前帧信息
info locals     # 查看当前帧局部变量
info args       # 查看当前帧参数
```

### 7. 观察点（监视变量变化）
```bash
watch x         # 当x被修改时中断
rwatch x        # 当x被读取时中断
awatch x        # 当x被读取或修改时中断
info watchpoints # 查看所有观察点
```

### 8. 线程调试
```bash
info threads    # 查看所有线程
thread 2        # 切换到2号线程
thread apply all bt  # 查看所有线程的调用栈
```

## 六、实际调试示例

### 示例程序（debug_demo.c）
```c
#include <stdio.h>

int factorial(int n) {
    if (n <= 1) {
        return 1;
    }
    return n * factorial(n - 1);
}

int main() {
    int i;
    int sum = 0;
    
    for (i = 1; i <= 5; i++) {
        int fact = factorial(i);
        sum += fact;
        printf("factorial(%d) = %d\n", i, fact);
    }
    
    printf("Sum of factorials = %d\n", sum);
    return 0;
}
```

### 调试过程
```bash
# 1. 编译
gcc -g -O0 debug_demo.c -o debug_demo

# 2. 启动GDB
gdb ./debug_demo

# 3. 在main函数设置断点
(gdb) break main

# 4. 运行程序
(gdb) run

# 5. 单步执行
(gdb) next    # 执行下一行
(gdb) step    # 进入factorial函数

# 6. 查看变量
(gdb) print i
(gdb) print sum

# 7. 设置条件断点
(gdb) break 8 if i==3

# 8. 继续执行
(gdb) continue

# 9. 查看调用栈
(gdb) backtrace

# 10. 退出调试
(gdb) quit
```

## 七、GDB实用技巧

### 1. 命令行参数传递
```bash
gdb ./program
(gdb) set args arg1 arg2
(gdb) run
```

### 2. 调试核心转储文件
```bash
gdb ./program core
# 或
gdb ./program -c core.dump
```

### 3. 调试运行中的进程
```bash
# 查看进程ID
ps aux | grep program_name

# 附加到进程
gdb -p pid
# 或
gdb
(gdb) attach pid
```

### 4. GDB批处理模式
```bash
# 创建命令文件
echo "break main" > gdb_commands
echo "run" >> gdb_commands
echo "backtrace" >> gdb_commands

# 使用命令文件
gdb -x gdb_commands ./program
```

### 5. 常用配置（~/.gdbinit）
```bash
# 显示行号
set listsize 30
# 美化打印STL容器（需要安装gdb-dashboard或插件）
# 设置历史记录保存
set history save on
```

## 八、常见问题解决

### 1. 调试时看不到源代码
- 确保编译时使用了 `-g` 选项
- 使用 `dir /path/to/source` 添加源码路径

### 2. 程序参数包含特殊字符
```bash
gdb ./program
(gdb) run arg1 "arg with spaces" arg3
```

### 3. 多文件项目调试
```bash
# 在特定文件设置断点
(gdb) break file.c:function_name
```

## 九、学习资源

1. **官方文档**：`info gdb` 或在线文档
2. **速查表**：创建GDB命令速查表
3. **实战练习**：尝试调试有问题的程序
4. **进阶主题**：学习Python脚本扩展GDB功能

## 十、下一步学习建议

1. 掌握基本命令后，练习调试段错误、内存泄漏
2. 学习使用GDB调试多线程程序
3. 了解GDB的TUI（文本用户界面）模式
4. 学习使用GDB Python API进行高级调试

通过以上步骤，你已掌握GDB的基础调试技能。建议通过实际调试练习巩固知识，遇到问题时多用 `help` 命令查看帮助。