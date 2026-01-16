package com.GUI;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class SimpleGUI extends JFrame implements ActionListener {
    private JTextField inputField;
    private JButton clickButton;
    private JLabel infoLabel;

    public SimpleGUI() {
        setTitle("My first GUI");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        inputField = new JTextField(15);
        clickButton = new JButton("click me");
        infoLabel = new JLabel("here show info");

        add(inputField);
        add(clickButton);
        add(infoLabel);

        clickButton.addActionListener(this);

        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String text = inputField.getText();
        infoLabel.setText("你输入了：" + text);

        inputField.setText("");
    }

    public static void main(String[] args) {
        new SimpleGUI();
    }
}