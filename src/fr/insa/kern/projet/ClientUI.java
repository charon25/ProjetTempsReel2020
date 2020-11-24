package fr.insa.kern.projet;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;

public class ClientUI extends JFrame implements ActionListener, KeyListener {

    private JLabel lblAddress;
    private JTextField tfAddress;
    private JLabel lblPort;
    private JTextField tfPort;
    private JLabel lblUsername;
    private JTextField tfUsername;
    private JLabel lblPassword;
    private JPasswordField tfPassword;
    private JButton btnConnect;
    private JButton btnDisconnect;
    private JTextArea taConsole;
    private JScrollPane spConsole;
    private JTextField tfInput;
    private JLabel lblInput;

    final private Border border = BorderFactory.createLineBorder(Color.BLACK);

    private Client client;

    public ClientUI() {
        super("Projet Systèmes Temps Réel - Client");
        setupWindow();
    }

    private void setupWindow() {
        //FERMETURE DE LA FENETRE
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };
        addWindowListener(l);

        //LABEL ADRESSE IP
        lblAddress = new JLabel("Adresse IP :");
        lblAddress.setBounds(12, 12, 100, 13);
        add(lblAddress);
        //INPUT ADRESSE IP
        tfAddress = new JTextField("192.168.56.1");
        tfAddress.setBounds(12, 28, 164, 20);
        add(tfAddress);

        //LABEL PORT
        lblPort = new JLabel("Port :");
        lblPort.setBounds(12, 60, 100, 13);
        add(lblPort);
        //INPUT PORT
        tfPort = new JTextField();
        tfPort.setBounds(12, 76, 164, 20);
        add(tfPort);

        //LABEL USERNAME
        lblUsername = new JLabel("Nom d'utilisateur :");
        lblUsername.setBounds(12, 108, 150, 13);
        add(lblUsername);
        //INPUT USERNAME
        tfUsername = new JTextField();
        tfUsername.setBounds(12, 124, 164, 20);
        add(tfUsername);

        //LABEL PASSWORD
        lblPassword = new JLabel("Mot de passe :");
        lblPassword.setBounds(12, 156, 150, 13);
        add(lblPassword);
        //INPUT PASSWORD
        tfPassword = new JPasswordField();
        tfPassword.setBounds(12, 172, 164, 20);
        tfPassword.addKeyListener(this);
        add(tfPassword);

        //BOUTON CONNEXION
        btnConnect = new JButton("Connexion");
        btnConnect.setBounds(12, 198, 164, 38);
        btnConnect.addActionListener(this);
        add(btnConnect);

        //BOUTON DECONNEXION
        btnDisconnect = new JButton("Déconnexion");
        btnDisconnect.setBounds(12, 242, 164, 38);
        btnDisconnect.addActionListener(this);
        btnDisconnect.setEnabled(false);
        add(btnDisconnect);

        //CONSOLE
        taConsole = new JTextArea();
        taConsole.setEditable(false);
        taConsole.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        taConsole.setWrapStyleWord(true);
        taConsole.setLineWrap(true);
        spConsole = new JScrollPane(taConsole);
        spConsole.setBounds(182, 12, 458, 411);
        spConsole.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum())); //Auto scroll
        add(spConsole);

        //CHAMP D'ENTREE
        tfInput = new JTextField();
        tfInput.setBounds(182, 429, 458, 20);
        tfInput.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        tfInput.addKeyListener(this);
        add(tfInput);

        //LABEL INPUT
        lblInput = new JLabel("Commande : ");
        lblInput.setBounds(100, 432, 100, 13);
        add(lblInput);

        //FENETRE
        setSize(662, 500);
        setLayout(null);
        setVisible(true);
        setResizable(false);
    }

    public void receiveData(String s) {
        taConsole.append(s + "\n");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnConnect)) {
            connect();
        } else if (e.getSource().equals(btnDisconnect)) {
            disconnect();
        }
    }

    public void connect() {
        try {
            client = new Client(tfAddress.getText(), Integer.parseInt(tfPort.getText()), tfUsername.getText(), new String(tfPassword.getPassword()), this);
            client.connect();
            if (client.isConnected()) {
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                taConsole.append("-----------------------------\nConnexion établie avec le serveur !\n");
            }
        } catch (ConnectException ex) {
            taConsole.append("[ERROR] Erreur de connexion au serveur, vérifiez le port et l'adresse IP.\n");
        } catch (UnknownHostException ex) {
            taConsole.append("[ERROR] Adresse IP inconnue.\n");
        } catch (IOException ex) {
            taConsole.append("[ERROR] Erreur lors de l'ouverture des flux.\n");
        }
        tfPassword.setText("");
    }

    public void disconnect() {
        client.sendMessageAndGetResponse("::close");
        client.disconnect();
        taConsole.append("Déconnecté du serveur.\n");
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
    }

    public void clientDisconnected() {
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (e.getSource().equals(tfInput) && e.getKeyChar() == 10 && tfInput.getText().length() > 0) {
            if (client.isConnected()) {
                taConsole.append("************************************************************\n");
                taConsole.append("--> " + tfInput.getText() + "\n");
                taConsole.append("<-- " + client.sendMessageAndGetResponse(tfInput.getText()) + "\n");
                tfInput.setText("");
            }
        }
        if (e.getSource().equals(tfPassword) && e.getKeyChar() == 10 && btnConnect.isEnabled()) {
            connect();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }

    @Override
    public void keyReleased(KeyEvent e) {

    }


    public static void main(String[] args) {
        new ClientUI();
    }
}
