package fr.insa.kern.projet;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;

// Interface utilisateur du serveur
public class ServerUI extends JFrame implements ActionListener, KeyListener {

    // Boutons pour lancer et stopper le serveur
    private JButton btnRunServer;
    private JButton btnStopServer;
    // Infos du serveur (adresse IP et port)
    private JLabel lblServerInfo;
    // Console d'affichage des informations
    private JTextArea taConsole;
    private JScrollPane spConsole;
    // Entrée depuis le serveur
    private JTextField tfInput;
    private JLabel lblInput;

    final private Border border = BorderFactory.createLineBorder(Color.BLACK); // Permet d'avoir une bordure noire

    private Server server; // Instance du serveur associée

    // Instancie l'interface
    public ServerUI() {
        super("Projet Système Temps Réel - Serveur");
        setupWindow();
    }

    // Initialise les différents composants de la fenêtre
    private void setupWindow() {
        // FERMETURE DE LA FENETRE
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (server != null) server.close();
                System.exit(0);
            }
        };
        addWindowListener(l);

        // BOUTON LANCER SERVEUR
        btnRunServer = new JButton("Lancer le serveur");
        btnRunServer.setBounds(12, 12, 164, 38);
        btnRunServer.addActionListener(this);
        add(btnRunServer);

        // BOUTON STOPPER SERVEUR
        btnStopServer = new JButton("Arrêter le serveur");
        btnStopServer.setBounds(12, 56, 164, 38);
        btnStopServer.addActionListener(this);
        btnStopServer.setEnabled(false);
        add(btnStopServer);

        // LABEL
        lblServerInfo = new JLabel("Serveur éteint.");
        lblServerInfo.setBounds(12, 110, 164, 30);
        add(lblServerInfo);

        // CONSOLE
        taConsole = new JTextArea();
        taConsole.setEditable(false);
        taConsole.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        taConsole.setWrapStyleWord(true);
        taConsole.setLineWrap(true);
        spConsole = new JScrollPane(taConsole);
        spConsole.setBounds(182, 12, 358, 311);
        spConsole.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum())); // Auto scroll
        add(spConsole);

        // CHAMP D'ENTREE
        tfInput = new JTextField();
        tfInput.setBounds(182, 329, 358, 20);
        tfInput.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        tfInput.addKeyListener(this);
        add(tfInput);

        // LABEL INPUT
        lblInput = new JLabel("Commande : ");
        lblInput.setBounds(100, 332, 100, 13);
        add(lblInput);

        // FENETRE
        setSize(562, 400);
        setLayout(null);
        setVisible(true);
        setResizable(false);
    }

    // Affiche les données reçues depuis l'instance du Server
    public void receiveData(String s) {
        taConsole.append(s + "\n");
    }

    @Override
    // Détecte un clic sur un bouton
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnRunServer)) { // Si c'est le bouton pour lancer le serveur, on le lance
            server = new Server(this);
            server.start(); // On démarre le serveur
            if (server.isConnected()) { // Si il est connecté, on affiche les informations sur la fenêtre
                lblServerInfo.setText("<html>Adresse : " + server.getInetAddress() + "<br>Port : " + server.getLocalPort() + "</html>");
                btnRunServer.setEnabled(false);
                btnStopServer.setEnabled(true);
            }
        } else if (e.getSource().equals(btnStopServer)) { // Si c'est le bouton pour stopper le serveur, on l'arrête
            if (server.isConnected()) server.close();
            lblServerInfo.setText("Serveur éteint.");
            btnRunServer.setEnabled(true);
            btnStopServer.setEnabled(false);
        }
    }

    @Override
    // Détecte un appui sur le clavier
    public void keyTyped(KeyEvent e) {
        // Si il s'agit de l'entrée de texte, qu'on appuie sur Entrée et que le serveur est connecté
        // Alors on envoie le texte sous forme d'une commande
        if (e.getSource().equals(tfInput) && e.getKeyChar() == 10 && server.isConnected()) {
            server.sendCommand("::" + tfInput.getText());
            tfInput.setText("");
        }
    }

    // Méthodes nécessaires lorsqu'on veut pouvoir détecter des appuis sur des touches
    @Override
    public void keyPressed(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}

    // Génère la fenêtre
    public static void main(String[] args) {
        new ServerUI();
    }
}
