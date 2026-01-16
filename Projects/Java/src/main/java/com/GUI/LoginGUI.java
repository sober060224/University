package com.GUI;

import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginGUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginGUI() {
        setTitle("登录界面");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 2));

        add(new JLabel("用户名："));
        usernameField = new JTextField();
        add(usernameField);

        add(new Label("密码："));
        passwordField = new JPasswordField();
        add(passwordField);

        JButton loginButton = new JButton("登录");
        add(loginButton);

        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                if ("admin".equals(username) && "123456".equals(password))
                    JOptionPane.showMessageDialog(null, "登陆成功");
                else
                    JOptionPane.showMessageDialog(null, "用户名或密码错误！");
            }
        });

        setVisible(true);
    }

    public static void main(String[] args) {
        new LoginGUI();
    }
}