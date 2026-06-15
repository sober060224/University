package com.GUI;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class demo {
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        // frame.setLayout(new GridLayout());
        frame.setLayout(new FlowLayout());
        JLabel label = new JLabel("输入姓名：");
        JTextField field = new JTextField(10);
        JButton button = new JButton("提交");

        frame.add(label);
        frame.add(field);
        frame.add(button);

        button.addActionListener(e -> {
            String name = field.getText();
            JOptionPane.showMessageDialog(null, "你好" + name);
        });
        frame.setVisible(true);
    }
}
