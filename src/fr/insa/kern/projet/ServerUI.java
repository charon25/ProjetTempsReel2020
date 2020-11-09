package fr.insa.kern.projet;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

public class ServerUI extends JFrame implements ActionListener, KeyListener {

    private JButton btnRunServer;
    private JButton btnStopServer;
    private JLabel lblServerInfo;
    private JTextArea taConsole;
    private JScrollPane spConsole;
    private JTextField tfInput;
    private JLabel lblInput;

    final private Border border = BorderFactory.createLineBorder(Color.BLACK);

    private Server server;

    public ServerUI() {
        super("Projet Système Temps Réel - Serveur");
        setupWindow();
    }
    private void setupWindow() {
        //FERMETURE DE LA FENETRE
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e){
                if (server != null) server.close();
                System.exit(0);
            }
        };
        addWindowListener(l);

        //BOUTON LANCER SERVEUR
        btnRunServer = new JButton("Lancer le serveur");
        btnRunServer.setBounds(12, 12, 164, 38);
        btnRunServer.addActionListener(this);
        add(btnRunServer);

        //BOUTON STOPPER SERVEUR
        btnStopServer = new JButton("Arrêter le serveur");
        btnStopServer.setBounds(12, 56, 164, 38);
        btnStopServer.addActionListener(this);
        btnStopServer.setEnabled(false);
        add(btnStopServer);

        //LABEL
        lblServerInfo = new JLabel("Serveur éteint.");
        lblServerInfo.setBounds(12, 110, 164,30);
        add(lblServerInfo);

        //CONSOLE
        taConsole = new JTextArea();
        taConsole.setEditable(false);
        taConsole.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        taConsole.setWrapStyleWord(true);
        taConsole.setLineWrap(true);
        spConsole = new JScrollPane(taConsole);
        spConsole.setBounds(182, 12, 358, 311);
        add(spConsole);
        add(taConsole);

        //CHAMP D'ENTREE
        tfInput = new JTextField();
        tfInput.setBounds(182, 329, 358, 20);
        tfInput.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        tfInput.addKeyListener(this);
        add(tfInput);

        //LABEL INPUT
        lblInput = new JLabel("Commande : ");
        lblInput.setBounds(100, 332, 100, 13);
        add(lblInput);

        //FENETRE
        setSize(562, 400);
        setLayout(null);
        setVisible(true);
    }

    public void receiveData(String s) {
        taConsole.append(s + "\n");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnRunServer)) {
            server = new Server(this);
            server.start();
            if (server.isConnected()) {
                lblServerInfo.setText("<html>Adresse : " + server.getInetAddress() + "<br>Port : " + server.getLocalPort() + "</html>");
                btnStopServer.setEnabled(true);
            }
        } else if (e.getSource().equals(btnStopServer)) {
            if (server.isConnected()) server.close();
            lblServerInfo.setText("Serveur éteint.");
            btnStopServer.setEnabled(false);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getSource().equals(tfInput) && e.getKeyChar() == 10) {
            server.sendCommand("::" + tfInput.getText());
            tfInput.setText("");
        }
    }
    @Override
    public void keyPressed(KeyEvent e) {

    }
    @Override
    public void keyReleased(KeyEvent e) {

    }





    public static void main(String[] args) {
        new ServerUI();
    }
}
