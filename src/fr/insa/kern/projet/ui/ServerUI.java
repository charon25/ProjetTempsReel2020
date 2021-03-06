package fr.insa.kern.projet.ui;

import fr.insa.kern.projet.Server;
import fr.insa.kern.projet.classifying.Classifier;
import fr.insa.kern.projet.classifying.EmbeddingClassifier;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;

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
    // Boutons pour sélectionner le type de classification
    private JLabel lblClassification;
    private ButtonGroup grpClassification;
    private JRadioButton rdLevenshtein;
    private JRadioButton rdKNN;
    private JRadioButton rdWeighting;
    // Boutons pour sélectionner le dictionnaire
    private JLabel lblDictionaries;
    private ButtonGroup grpDictionaries;
    private JRadioButton rdDic1;
    private JRadioButton rdDic2;

    final private Border border = BorderFactory.createLineBorder(Color.BLACK); // Permet d'avoir une bordure noire

    private Server server; // Instance du serveur associée
    private String dictionaryPath;

    // Instancie l'interface
    public ServerUI() {
        super("Projet Système Temps Réel - Serveur");
        dictionaryPath = EmbeddingClassifier.DIC2_PATH; // Par défaut, on choisit notre propre dictionnaire de mots vectorisés
        setupWindow();
        //On choisit le classification par k-NN par défaut
    }

    // Initialise les différents composants de la fenêtre
    private void setupWindow() {
        // FERMETURE DE LA FENETRE (on ferme la connexion et on enregistre l'agenda au passage)
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

        // CLASSIFICATION
        // LABEL
        lblClassification = new JLabel("Type de classification : ");
        lblClassification.setBounds(12, 150, 164, 30);
        add(lblClassification);
        // RADIOS BOUTONS
        // Levenshtein
        rdLevenshtein = new JRadioButton();
        rdLevenshtein.setBounds(10, 172, 164, 30);
        rdLevenshtein.setText("Distance de Levenshtein");
        rdLevenshtein.addActionListener(this);
        rdLevenshtein.setEnabled(false);
        add(rdLevenshtein);
        // Plus proche voisin
        rdKNN = new JRadioButton();
        rdKNN.setBounds(10, 195, 164, 30);
        rdKNN.setText("k-NN (Vectorisation)");
        rdKNN.setSelected(true);
        rdKNN.addActionListener(this);
        rdKNN.setEnabled(false);
        add(rdKNN);
        // Voisins pondérés
        rdWeighting = new JRadioButton();
        rdWeighting.setBounds(10, 218, 164, 30);
        rdWeighting.setText("Voisins pondérés");
        rdWeighting.setSelected(false);
        rdWeighting.addActionListener(this);
        rdWeighting.setEnabled(false);
        add(rdWeighting);
        // BUTTON GROUP
        grpClassification = new ButtonGroup();
        grpClassification.add(rdLevenshtein);
        grpClassification.add(rdKNN);
        grpClassification.add(rdWeighting);
        
        // DICTIONNAIRES
        // LABEL
        lblDictionaries = new JLabel("Dictionnaire à utiliser : ");
        lblDictionaries.setBounds(12, 240, 164, 30);
        add(lblDictionaries);
        // RADIOS BOUTONS
        // Dictionnaire 1 (prof)
        rdDic1 = new JRadioButton();
        rdDic1.setBounds(10, 262, 164, 30);
        rdDic1.setText("Dictionnaire M. Samet");
        rdDic1.addActionListener(this);
        add(rdDic1);
        // Dictionnaire 2 (nous)
        rdDic2 = new JRadioButton();
        rdDic2.setBounds(10, 285, 164, 30);
        rdDic2.setText("Dictionnaire personnel");
        rdDic2.setSelected(true);
        rdDic2.addActionListener(this);
        add(rdDic2);
        // BUTTON GROUP
        grpDictionaries = new ButtonGroup();
        grpDictionaries.add(rdDic1);
        grpDictionaries.add(rdDic2);

        // FENETRE
        setSize(562, 400);
        setLayout(null);
        setVisible(true);
        setResizable(false);
    }

    // Affiche les données reçues depuis l'instance du Server
    public void receiveData(String s) {
        taConsole.append(s + "\n");
        spConsole.getVerticalScrollBar().setValue(spConsole.getVerticalScrollBar().getMaximum());
    }

    @Override
    // Détecte un clic sur un bouton
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(btnRunServer)) { // Si c'est le bouton pour lancer le serveur, on le lance
            server = new Server(this, dictionaryPath);
            server.start(); // On démarre le serveur
            if (server.isConnected()) { // Si il est connecté, on affiche les informations sur la fenêtre
                lblServerInfo.setText("<html>Adresse : " + server.getInetAddress() + "<br>Port : " + server.getLocalPort() + "</html>");
                btnRunServer.setEnabled(false);
                btnStopServer.setEnabled(true);
                // Parcourt tous les boutons de sélection de méthode pour tous les activer
                for (Enumeration<AbstractButton> buttons = grpClassification.getElements() ; buttons.hasMoreElements();) {
                    AbstractButton button = buttons.nextElement();
                    button.setEnabled(true);
                }
                // Par défaut, on choisit d'utiliser le k-NN
                server.setClassificationMethod(Server.ClassificationMethod.KNN);
                
				// Parcourt tous les boutons de sélection de dictionnaire pour tous les désactiver
				for (Enumeration<AbstractButton> buttons = grpDictionaries.getElements(); buttons.hasMoreElements();) {
					AbstractButton button = buttons.nextElement();
					button.setEnabled(false);
				}
			}
        } else if (e.getSource().equals(btnStopServer)) { // Si c'est le bouton pour stopper le serveur, on l'arrête
            if (server.isConnected()) server.close();
            lblServerInfo.setText("Serveur éteint.");
            btnRunServer.setEnabled(true);
            btnStopServer.setEnabled(false);
            // Parcourt tous les boutons de sélection de méthode pour tous les désactiver
            for (Enumeration<AbstractButton> buttons = grpClassification.getElements() ; buttons.hasMoreElements();) {
                AbstractButton button = buttons.nextElement();
                button.setEnabled(false);
            }
            // Parcourt tous les boutons de sélection de dictionnaire pour tous les activer
			for (Enumeration<AbstractButton> buttons = grpDictionaries.getElements(); buttons.hasMoreElements();) {
				AbstractButton button = buttons.nextElement();
				button.setEnabled(true);
			}
        } else if (e.getSource().equals(rdLevenshtein)) { // Si c'est le bouton de sélection de la distance de Levenshtein, on l'indique au serveur
            server.setClassificationMethod(Server.ClassificationMethod.LEVENSHTEIN);
            taConsole.append("Méthode de classification choisie : distance de Levenshtein.\n");
        } else if (e.getSource().equals(rdKNN)) { // Si c'est le bouton de sélection du k-NN, on l'indique au serveur
            server.setClassificationMethod(Server.ClassificationMethod.KNN);
            taConsole.append("Méthode de classification choisie : k-NN.\n");
        } else if (e.getSource().equals(rdWeighting)) {
            server.setClassificationMethod(Server.ClassificationMethod.WEIGHTING);
            taConsole.append("Méthode de classification choisie : voisins pondérés\n");
        } else if (e.getSource().equals(rdDic1)) {
        	dictionaryPath = EmbeddingClassifier.DIC_PATH;
        	taConsole.append("Dictionnaire choisi : celui de M. Samet\n");
        } else if (e.getSource().equals(rdDic2)) {
        	dictionaryPath = EmbeddingClassifier.DIC2_PATH;
        	taConsole.append("Dictionnaire choisi : le notre\n");
        }
    }

    @Override
    // Détecte un appui sur une touche
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
