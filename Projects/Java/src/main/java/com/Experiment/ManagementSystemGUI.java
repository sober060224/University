package com.Experiment;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;

// ==================== 1. 封装、继承、多态 ====================
// 基类：Person（封装）
class Person implements Serializable {
    private String id;
    private String name;
    private int age;

    public Person(String id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // Getter 和 Setter（封装）
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "ID: " + id + ", 姓名: " + name + ", 年龄: " + age;
    }
}

// 子类：Student（继承）
class Student extends Person {
    private String major;

    public Student(String id, String name, int age, String major) {
        super(id, name, age);
        this.major = major;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    @Override
    public String toString() {
        return super.toString() + ", 专业: " + major;
    }
}

// 子类：Teacher（继承）
class Teacher extends Person {
    private String department;

    public Teacher(String id, String name, int age, String department) {
        super(id, name, age);
        this.department = department;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    @Override
    public String toString() {
        return super.toString() + ", 部门: " + department;
    }
}

// ==================== 2. 管理类（多态、异常、输入输出） ====================
class ManagementSystem {
    private List<Person> personList;

    public ManagementSystem() {
        personList = new ArrayList<>();
    }

    // 添加人员（多态）
    public void addPerson(Person person) {
        personList.add(person);
    }

    // 删除人员
    public boolean removePerson(String id) {
        for (Person p : personList) {
            if (p.getId().equals(id)) {
                personList.remove(p);
                return true;
            }
        }
        return false;
    }

    // 修改人员信息
    public boolean updatePerson(String id, String name, int age) {
        for (Person p : personList) {
            if (p.getId().equals(id)) {
                p.setName(name);
                p.setAge(age);
                return true;
            }
        }
        return false;
    }

    // 显示所有人员
    public String listAll() {
        StringBuilder sb = new StringBuilder();
        for (Person p : personList) {
            sb.append(p.toString()).append("\n");
        }
        return sb.toString();
    }

    // 保存到文件（对象序列化）
    public void saveToFile(String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(personList);
        }
    }

    // 从文件加载（对象反序列化）
    @SuppressWarnings("unchecked")
    public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            personList = (List<Person>) ois.readObject();
        }
    }
}

// ==================== 3. 多线程任务（保存数据） ====================
class SaveTask implements Runnable {
    private ManagementSystem ms;
    private String filename;

    public SaveTask(ManagementSystem ms, String filename) {
        this.ms = ms;
        this.filename = filename;
    }

    @Override
    public void run() {
        try {
            ms.saveToFile(filename);
            System.out.println("数据已保存到 " + filename);
        } catch (IOException e) {
            System.err.println("保存失败：" + e.getMessage());
        }
    }
}

// ==================== 4. 网络服务端（Socket编程） ====================
class Server {
    private ManagementSystem ms;

    public Server(ManagementSystem ms) {
        this.ms = ms;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("服务器启动，监听端口 12345...");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(socket, ms)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// 客户端处理器
class ClientHandler implements Runnable {
    private Socket socket;
    private ManagementSystem ms;

    public ClientHandler(Socket socket, ManagementSystem ms) {
        this.socket = socket;
        this.ms = ms;
    }

    @Override
    public void run() {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            // 接收客户端发送的Person对象
            Person person = (Person) ois.readObject();
            ms.addPerson(person);
            oos.writeObject("添加成功：" + person);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}

// ==================== 5. GUI界面（Swing） ====================
public class ManagementSystemGUI extends JFrame {
    private ManagementSystem ms = new ManagementSystem();
    private JTextArea displayArea;

    public ManagementSystemGUI() {
        setTitle("综合管理系统");
        setSize(600, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 显示区域
        displayArea = new JTextArea();
        add(new JScrollPane(displayArea), BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("添加学生");
        JButton removeButton = new JButton("删除人员");
        JButton listButton = new JButton("显示所有");
        JButton saveButton = new JButton("保存到文件");
        JButton loadButton = new JButton("从文件加载");

        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(listButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 添加学生按钮事件
        addButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog(this, "输入ID:");
            String name = JOptionPane.showInputDialog("输入姓名:");
            String ageStr = JOptionPane.showInputDialog("输入年龄:");
            String major = JOptionPane.showInputDialog("输入专业:");
            try {
                int age = Integer.parseInt(ageStr);
                Student stu = new Student(id, name, age, major);
                ms.addPerson(stu);
                displayArea.setText("添加成功：" + stu + "\n");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(null, "年龄必须是数字！");
            }
        });

        // 删除人员按钮事件
        removeButton.addActionListener(e -> {
            String id = JOptionPane.showInputDialog("输入要删除的ID:");
            if (ms.removePerson(id)) {
                displayArea.setText("删除成功\n");
            } else {
                displayArea.setText("未找到该ID\n");
            }
        });

        // 显示所有人员按钮事件
        listButton.addActionListener(e -> {
            displayArea.setText(ms.listAll());
        });

        // 保存到文件按钮事件（使用多线程）
        saveButton.addActionListener(e -> {
            Thread saveThread = new Thread(new SaveTask(ms, "data.dat"));
            saveThread.start();
        });

        // 从文件加载按钮事件
        loadButton.addActionListener(e -> {
            try {
                ms.loadFromFile("data.dat");
                displayArea.setText("加载成功\n");
            } catch (IOException | ClassNotFoundException ex) {
                displayArea.setText("加载失败：" + ex.getMessage() + "\n");
            }
        });

        // 启动服务器线程（网络编程）
        new Thread(() -> new Server(ms).start()).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ManagementSystemGUI().setVisible(true);
        });
    }
}