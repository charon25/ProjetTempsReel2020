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

/**
 * @author pkern01
 */
public class Server extends Thread {

    private final int DAYS_COUNT = 120;
    private final String AGENDA_PATH = "agenda.txt";
    private final String USERS_PATH = "users.txt";

    private Inet4Address ipAddress;
    private ServerSocket server;
    private ArrayList<SocketMessaging> connections;
    private ArrayList<Login> logins;
    private ServerUI ui;
    private Agenda agenda;
    private boolean connected;

    public Agenda getAgenda() {
        return agenda;
    }

    public Server(ServerUI ui) {
        this.ipAddress = INetAdressUtil.premiereAdresseNonLoopback();
        this.connections = new ArrayList<>();
        this.logins = new ArrayList<>();
        this.ui = ui;
        try {
            this.server = new ServerSocket(0, 1, ipAddress);
            this.connected = true;
            this.ui.receiveData("Création du serveur réussie !\nAdresse ip : " + server.getInetAddress() + "\nPort : " + server.getLocalPort() + "\n-----------------------------");
        } catch (IOException ex) {
            this.connected = false;
            this.ui.receiveData("[ERROR] Erreur lors de la création du serveur !");
        }
    }

    public int connectionCount() {
        return connections.size();
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


    @Override
    public void run() {
        loadUsers();
        loadAgenda();
        acceptConnections();
    }

    private void loadUsers() {
        try {
            File file = new File(USERS_PATH);
            if (file.isFile()) {
                FileReader reader = new FileReader(USERS_PATH);
                BufferedReader bufferedReader = new BufferedReader(reader);
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    logins.add(new Login(line.split(":")[0], line.split(":")[1], true));
                }
                reader.close();
            } else {
                file.createNewFile();
            }
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'ouverture du fichier utilisateur.");
        }
    }

    private void createUser(String username, String password) {
        logins.add(new Login(username, password, false));
        try {
            FileWriter fw = new FileWriter(USERS_PATH, true);
            fw.write(username + ":" + logins.get(logins.size() - 1).getHashedPasswordString() + "\n");
            fw.close();
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'écriture dans le fichier utilisateur.");
        }
    }

    private void loadAgenda() {
        try {
            File file = new File(AGENDA_PATH);
            if (file.isFile()) {
                FileReader reader = new FileReader(AGENDA_PATH);
                BufferedReader bufferedReader = new BufferedReader(reader);
                int lineCount = 0;
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    lineCount++;
                }
                reader.close();
                if (lineCount > 1) {
                    System.out.println("ici");
                    this.agenda = new Agenda(AGENDA_PATH);
                } else {
                    this.agenda = new Agenda(DAYS_COUNT);
                }
            } else {
                file.createNewFile();
                this.agenda = new Agenda(DAYS_COUNT);
            }
        } catch (IOException ex) {
            ui.receiveData("[ERROR] Erreur lors de l'ouverture du fichier utilisateur.");
        }
    }

    public void saveAgenda() {
        agenda.save(AGENDA_PATH);
    }

    private void acceptConnections() {
        while (connected) {
            try {
                Socket socket = server.accept();
                SocketMessaging connexion = new SocketMessaging(this, socket, logins);
                connexion.start();
                connections.add(connexion);
                ui.receiveData("Connexion établie, authentification...");
            } catch (IOException ex) {
                if (!ex.getMessage().equals("socket closed")) {
                    ui.receiveData("[ERROR] " + ex.getMessage());
                }
            }
        }
    }

    public void sendCommand(String message) {
        if (message.startsWith("::")) {
            try {
                message = message.substring(2);
                String[] args = message.split(" ");
                switch (args[0]) {
                    case "new-user":
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

    public void close() {
        try {
            connected = false;
            for (SocketMessaging connection : connections) {
                connection.close();
            }
            ui.receiveData("Serveur fermé.");
            server.close();
        } catch (IOException e) {
        }
    }

    public void sendStringToUI(String s) {
        ui.receiveData(s);
    }

}
