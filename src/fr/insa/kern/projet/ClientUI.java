package fr.insa.kern.projet;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

// Interface utilisateur du client
public class ClientUI extends JFrame implements ActionListener, KeyListener {

    // Adresse IP
    private JLabel lblAddress;
    private JTextField tfAddress;
    // Port
    private JLabel lblPort;
    private JTextField tfPort;
    // Nom d'utilisateur
    private JLabel lblUsername;
    private JTextField tfUsername;
    // Mot de passe
    private JLabel lblPassword;
    private JPasswordField tfPassword;
    // Boutons de connexion et déconnexion
    private JButton btnConnect;
    private JButton btnDisconnect;
    // Console où s'affiche les messages issus de la connexion
    private JTextArea taConsole;
    private JScrollPane spConsole;
    // Entrée de texte de l'utilisateur
    private JTextField tfInput;
    private JLabel lblInput;

    final private Border border = BorderFactory.createLineBorder(Color.BLACK); // Permet d'avoir une bordure noire

    private Client client; // Instance du client associée

    // Instancie l'interface
    public ClientUI() {
        super("Projet Systèmes Temps Réel - Client");
        setupWindow();
    }

    // Initialise les différents composants de la fenêtre
    private void setupWindow() {
        // FERMETURE DE LA FENETRE
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        };
        addWindowListener(l);

        // LABEL ADRESSE IP
        lblAddress = new JLabel("Adresse IP :");
        lblAddress.setBounds(12, 12, 100, 13);
        add(lblAddress);
        // INPUT ADRESSE IP
        tfAddress = new JTextField("192.168.0.10");
        tfAddress.setBounds(12, 28, 164, 20);
        add(tfAddress);

        // LABEL PORT
        lblPort = new JLabel("Port :");
        lblPort.setBounds(12, 60, 100, 13);
        add(lblPort);
        // INPUT PORT
        tfPort = new JTextField();
        tfPort.setBounds(12, 76, 164, 20);
        add(tfPort);

        // LABEL USERNAME
        lblUsername = new JLabel("Nom d'utilisateur :");
        lblUsername.setBounds(12, 108, 150, 13);
        add(lblUsername);
        // INPUT USERNAME
        tfUsername = new JTextField();
        tfUsername.setBounds(12, 124, 164, 20);
        add(tfUsername);

        // LABEL PASSWORD
        lblPassword = new JLabel("Mot de passe :");
        lblPassword.setBounds(12, 156, 150, 13);
        add(lblPassword);
        // INPUT PASSWORD
        tfPassword = new JPasswordField();
        tfPassword.setBounds(12, 172, 164, 20);
        tfPassword.addKeyListener(this);
        add(tfPassword);

        // BOUTON CONNEXION
        btnConnect = new JButton("Connexion");
        btnConnect.setBounds(12, 198, 164, 38);
        btnConnect.addActionListener(this);
        add(btnConnect);

        // BOUTON DECONNEXION
        btnDisconnect = new JButton("Déconnexion");
        btnDisconnect.setBounds(12, 242, 164, 38);
        btnDisconnect.addActionListener(this);
        btnDisconnect.setEnabled(false);
        add(btnDisconnect);

        // CONSOLE
        taConsole = new JTextArea();
        taConsole.setEditable(false);
        taConsole.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        taConsole.setWrapStyleWord(true);
        taConsole.setLineWrap(true);
        spConsole = new JScrollPane(taConsole);
        spConsole.setBounds(182, 12, 458, 411);
        spConsole.getVerticalScrollBar().addAdjustmentListener(e -> e.getAdjustable().setValue(e.getAdjustable().getMaximum())); // Auto scroll
        add(spConsole);

        // CHAMP D'ENTREE
        tfInput = new JTextField();
        tfInput.setBounds(182, 429, 458, 20);
        tfInput.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(1, 1, 1, 1)));
        tfInput.addKeyListener(this);
        add(tfInput);

        // LABEL INPUT
        lblInput = new JLabel("Commande : ");
        lblInput.setBounds(100, 432, 100, 13);
        add(lblInput);

        // FENETRE
        setSize(662, 500);
        setLayout(null);
        setVisible(true);
        setResizable(false);
    }

    // Affiche les données reçues depuis l'instance du Client
    public void receiveData(String s) {
        taConsole.append(s + "\n");
    }

    @Override
    // Détecte lorsqu'on clic sur un des boutons
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnConnect)) { // Si c'est le bouton de connexion
            connect();
        } else if (e.getSource().equals(btnDisconnect)) { // Si c'est celui de déconnexion
            disconnect();
        }
    }

    // Connecte le client
    public void connect() {
        try {
            // Instancie le client à l'aide de l'adresse IP, du port, du nom d'utilisateur et du mot de passe
            client = new Client(tfAddress.getText(), Integer.parseInt(tfPort.getText()), tfUsername.getText(), new String(tfPassword.getPassword()), this);
            client.connect(); // On connecte le client
            if (client.isConnected()) { // Si ça a marché, on l'indique à l'utilisateur
                btnConnect.setEnabled(false);
                btnDisconnect.setEnabled(true);
                taConsole.append("-----------------------------\nConnexion établie avec le serveur !\n");
            }
        // Gère les différentes erreurs
        } catch (ConnectException ex) {
            taConsole.append("[ERROR] Erreur de connexion au serveur, vérifiez le port et l'adresse IP.\n");
        } catch (UnknownHostException ex) {
            taConsole.append("[ERROR] Adresse IP inconnue.\n");
        } catch (IOException ex) {
            taConsole.append("[ERROR] Erreur lors de l'ouverture des flux.\n");
        } catch (NoSuchAlgorithmException ex) {}
        tfPassword.setText("");
    }

    // Déconnecte le client
    public void disconnect() {
        client.sendMessageAndGetResponse("::close"); // On envoie une commande de déconnexion
        client.disconnect();
        taConsole.append("Déconnecté du serveur.\n");
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
    }

    // Indique à l'interface qu'il faut changer l'état des boutons car le client a été déconnecté
    public void clientDisconnected() {
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
    }

    @Override
    // Détecte l'appui de touches du clavier
    public void keyTyped(KeyEvent e) {
        // Si on est sur l'entrée de texte, qu'on appuie sur Entrée, qu'on a écrit des caractères et que le client est connecté
        // Alors on envoie le message au serveur
        if (e.getSource().equals(tfInput) && e.getKeyChar() == 10 && tfInput.getText().length() > 0) {
            if (client.isConnected()) {
                // Mise en forme sur la console du client
                taConsole.append("************************************************************\n");
                taConsole.append("--> " + tfInput.getText() + "\n");
                taConsole.append("<-- " + client.sendMessageAndGetResponse(tfInput.getText()) + "\n"); // On envoie le message et on affiche la réponse
                tfInput.setText("");
            }
        }
        // Si on est sur le mot de passe, qu'on appuie sur Entrée et qu'on peut se connecté, alors on se connecte
        if (e.getSource().equals(tfPassword) && e.getKeyChar() == 10 && btnConnect.isEnabled()) {
            connect();
        }
    }

    // Méthodes nécessaires lorsqu'on veut pouvoir détecter des appuis sur des touches
    @Override
    public void keyPressed(KeyEvent e) {}
    @Override
    public void keyReleased(KeyEvent e) {}

    // Génère une fenêtre
    public static void main(String[] args) {
        new ClientUI();
    }
}
