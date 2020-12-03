/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insa.kern.projet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Function;

// Classe qui gère la partie serveur du ChatBot
public class Server extends Thread {

    public enum ClassificationMethod {LEVENSHTEIN, KNN, WEIGHTING};

    private final int DAYS_COUNT = 120; // Nombre de jour de l'agenda par défaut
    private final String AGENDA_PATH = "agenda.txt"; // Chemin de sauvegarde de l'agenda
    private final String USERS_PATH = "users.txt"; // Chemin de sauvegarde des utilisateurs

    private Inet4Address ipAddress; // Adresse IP
    private ServerSocket server; // Socket contenant le serveur
    private ArrayList<SocketMessaging> connections; // Liste des connexions actives
    private ArrayList<Login> logins; // Liste de tous les logins
    private ServerUI ui; // Interface utilisateur associée
    private Agenda agenda; // Agenda associé
    private boolean connected; // Indique si le serveur est connecté

    private Function<String, Classifier.MessageType> getMessageType;
    private EmbeddingClassifier embeddingClassifier;

    // ACCESSEUR
    public Agenda getAgenda() {
        return agenda;
    }
    public ArrayList<SocketMessaging> getConnections() {
        return connections;
    }
    public java.net.InetAddress getInetAddress() {
        return server.getInetAddress();
    }
    public int getLocalPort() {
        return server.getLocalPort();
    }
    public boolean isConnected() {
        return connected;
    }
    public ArrayList<Login> getLogins() {
        return logins;
    }
    public Function<String, Classifier.MessageType> getGetMessageTypeFunction() {
        return getMessageType;
    }

    // Instancie le serveur avec l'interface graphique
    public Server(ServerUI ui) {
        this.ipAddress = INetAdressUtil.premiereAdresseNonLoopback(); // On récupère l'adresse IP locale
        // On instancie les listes
        this.connections = new ArrayList<>();
        this.logins = new ArrayList<>();
        this.ui = ui;
        // On initialise le classifier par vectorisation avec les chemins du dictionnaire et des phrases de référence du k-NN
        this.embeddingClassifier = new EmbeddingClassifier(EmbeddingClassifier.DIC_PATH, EmbeddingClassifier.KNN_REFERENCES_PATH);
        try {
            // On essaye de créer un serveur, et si c'est bon on l'indique à l'interface
            this.server = new ServerSocket(0, 1, ipAddress);
            this.connected = true;
            this.ui.receiveData("Création du serveur réussie !\nAdresse ip : " + server.getInetAddress() + "\nPort : " + server.getLocalPort() + "\n-----------------------------");
        } catch (IOException ex) { // Sinon on l'indique également
            this.connected = false;
            this.ui.receiveData("[ERROR] Erreur lors de la création du serveur !");
        }
    }

    // Indique combien de connexions sont établies
    public int connectionCount() {
        return connections.size();
    }

    @Override
    // Méthode héritée de la classe Thread dans laquelle on va charger les utilisateurs, l'agenda et ensuite écouter les connexions entrantes
    public void run() {
        loadUsers();
        loadAgenda();
        Agenda.Day days[] = agenda.days;
        for (Agenda.Slot s : days[31].getSlots()) {
            System.out.println(s);
        }
        acceptConnections();
    }

    // Charge les utilisateurs contenus dans le fichier USERS_PATH
    // Il contient un login par ligne, sous la forme <nom d'utilisateur>:<mot de passe hashé>
    private void loadUsers() {
        try {
            File file = new File(USERS_PATH);
            // On vérifie d'abord si le fichier existe
            if (file.isFile()) { // Si oui, on le lit ligne par ligne
                FileReader reader = new FileReader(USERS_PATH);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    // Pour chaque ligne, on crée un nouveau login, et on indique qu'il est déjà hashé
                    logins.add(new Login(line.split(":")[0], line.split(":")[1], true));
                }
                reader.close();
            } else {// Sinon on crée le fichier
                file.createNewFile();
            }
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'ouverture du fichier utilisateur.");
        }
    }

    // Crée un nouvel login à partir d'un nom d'utilisateur et d'un mot de passe non hashé
    private void createUser(String username, String password) {
        // On crée le login
        logins.add(new Login(username, password, false));
        try {
            // On essaye ensuite de l'ajouter à la fin du fichier contenant les logins
            FileWriter fw = new FileWriter(USERS_PATH, true);
            fw.write(username + ":" + logins.get(logins.size() - 1).getHashedPasswordString() + "\n");
            fw.close();
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'écriture dans le fichier utilisateur.");
        }
    }

    // Charge l'agenda
    private void loadAgenda() {
        try {
            File file = new File(AGENDA_PATH);
            // On vérifie tout d'abord si le fichier contenant l'agenda existe
            if (file.isFile()) {
                // Si oui, on compte combien il contient de ligne
                FileReader reader = new FileReader(AGENDA_PATH);
                BufferedReader bufferedReader = new BufferedReader(reader);
                int lineCount = 0;
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lineCount++;
                }
                reader.close();
                if (lineCount > 1) { // Si le nombre de ligne est plus grand que 1, on instancie l'agenda à partir du chemin
                    this.agenda = new Agenda(AGENDA_PATH);
                } else { // Sinon c'est que le fichier est vide et on instancie l'agenda à partir de 0
                    this.agenda = new Agenda(DAYS_COUNT);
                }
            } else { // Si le fichier n'existe pas, on le crée et on instancie l'agenda à partir de 0
                file.createNewFile();
                this.agenda = new Agenda(DAYS_COUNT);
            }
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'ouverture du fichier utilisateur.");
        }
    }

    // Sauvegarde l'agenda
    public void saveAgenda() {
        agenda.save(AGENDA_PATH);
    }

    // Accèpte les connexions. Cette fonction tourne en boucle tant que le serveur est connecté
    private void acceptConnections() {
        while (connected) {
            try {
                // On attend une connexion
                Socket socket = server.accept();
                // Lorsqu'on la reçoit, on crée une nouvelle connexion qu'on ajoute à la liste et qu'on démarre
                SocketMessaging connexion = new SocketMessaging(this, socket);
                connexion.start();
                connections.add(connexion);
                ui.receiveData("Connexion établie, authentification...");
            } catch (IOException ex) {
                if (!ex.getMessage().equals("socket closed")) { // Cette vérification permet de savoir que l'exception n'est pas la fermeture du serveur
                    ui.receiveData("[ERROR] " + ex.getMessage());
                }
            }
        }
    }

    // Envoie une commande au serveur, utilisée seulement pour créer un nouvel utilisateur
    public void sendCommand(String message) {
        if (message.startsWith("::")) { // Si le message commence par ::, c'est bien une commande
            try {
                message = message.substring(2); // On retire ::
                String[] args = message.split(" "); // On découpe selon les espaces
                switch (args[0]) { // Selon l'instruction de la commande, on effectue différentes actions
                    // Commande pour créer un nouveau login : ::new-user <nom d'utilisateur> <mot de passe>
                    case "new-user": // Si la commande est "new-user", on crée un nouvel utilisateur avec les deux autres arguments
                        createUser(args[1], args[2]);
                        ui.receiveData("Utilisateur \"" + args[1] + "\" correctement ajouté !");
                        break;
                    default:
                        ui.receiveData("Commande inconnue.");
                }
            } catch (ArrayIndexOutOfBoundsException ex) {
                ui.receiveData("Erreur dans la syntaxe de la commande.");
            }
        }
    }

    public void setClassificationMethod(ClassificationMethod method) {
        switch (method) {
            case LEVENSHTEIN:
                getMessageType = LevenshteinClassifier::getMessageType;
                break;
            case KNN:
                getMessageType = embeddingClassifier::getMessageTypeByClosestNeighbor;
                break;
            case WEIGHTING:
                getMessageType = embeddingClassifier::getMessageTypeByWeighting;
                break;
        }
    }

    // Ferme le serveur
    public void close() {
        try {
            connected = false; // On indique qu'il est déconnecté
            for (SocketMessaging connection : connections) { // On ferme toutes les connexions en cours
                connection.close();
            }
            ui.receiveData("Serveur fermé.");
            server.close();
        } catch (IOException e) {}
    }

    // Envoie du texte à afficher sur l'interface utilisateur
    public void sendStringToUI(String s) {
        ui.receiveData(s);
    }

}
