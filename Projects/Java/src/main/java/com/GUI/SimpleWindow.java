package com.GUI;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class SimpleWindow {
    public static void main(String[] args) {
        JFrame frame = new JFrame("考试专用窗口");
        frame.setSize(400, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new FlowLayout());

        JLabel label = new JLabel("请输入内容");
        JTextField textField = new JTextField(20);
        JButton button = new JButton("点击提交");

        frame.add(label);
        frame.add(textField);
        frame.add(button);

        textField.addActionListener(e -> {
            String input = textField.getText();
            JOptionPane.showMessageDialog(frame, "输入了：" + input);
            textField.setText("");
        });
        button.addActionListener(e -> {
            String input = textField.getText();
            JOptionPane.showMessageDialog(frame, "输入了：" + input);
            textField.setText("");
        });

        frame.setVisible(true);
    }
}
