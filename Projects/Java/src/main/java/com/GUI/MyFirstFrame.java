package com.GUI;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class MyFirstFrame extends JFrame {
    public MyFirstFrame() {
        setTitle("My First Window");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JTextField field = new JTextField(10);
        JLabel label = new JLabel("label");
        JButton button1 = new JButton("MyButton1");
        JButton button2 = new JButton("MyButton2");
        JButton button3 = new JButton("MyButton3");

        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "button1 have been clicked!");
            }
        });

        button2.addActionListener(e -> {
            JOptionPane.showMessageDialog(null, "Lambda");
        });

        add(label, BorderLayout.NORTH);
        add(field, BorderLayout.CENTER);
        add(button1, BorderLayout.NORTH);
        add(button2, BorderLayout.CENTER);
        add(button3, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MyFirstFrame());
    }
}
