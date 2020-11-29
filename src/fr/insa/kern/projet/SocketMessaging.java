/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.insa.kern.projet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

// Classe qui gère une connexion client/serveur
public class SocketMessaging extends Thread {

    final private int MAX_ERRORS = 10; // Nombre maximal d'erreurs avant de fermer la connexion

    private Socket socket; // Socket associé
    // Flux de lecture et d'écriture
    private BufferedReader in;
    private PrintWriter out;
    private String name; // Nom de la connexion
    private Server server; // Serveur associé
    private boolean connected; // Indique si la connexion est établie
    private int ioErrorsCount; // Nombre d'erreurs

    // METHODE SANS MACHINE LEARNING
    private String[] reservationDateTime; // Heure et date de réservation
    private boolean wasReservation; // Indique si le message précédent était un message de réservation
    // ##################################################

    // ACCESSEURS
    public boolean isConnected() {
        return connected;
    }
    public String getClientName() {
        return name;
    }

    // Instancie une connexion avec le serveur et le socket
    public SocketMessaging(Server server, Socket socket) {
        try {
            // On initialise toutes les variables
            this.server = server;
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            this.name = "";
            this.connected = true;
            this.ioErrorsCount = 0;
            this.reservationDateTime = null;
            this.wasReservation = false;
        } catch (IOException ex) {
            server.sendStringToUI("[ERROR] Erreur lors de l'ouverture des flux.");
        }
    }

    @Override
    // Méthode exécutée lorsque le Thread se lance, qui va lire les messages jusqu'à ce que ça retourne False
    public void run() {
        while (readMessage());
    }

    // Lis et répond aux messages du client
    // Renvoie True si on doit continuer la connexion, False sinon
    public boolean readMessage() {
        if (connected) { // Si on est connecté
            try {
                // On lit le message du client
                String line = in.readLine();
                if (line == null) return false; // Si il est vide, on arrête la connexion
                // Si le message commence par ::, c'est une commande et on la gère comme telle
                if (line.startsWith("::")) {
                    String result = readCommand(line); // On récupère l'état de la commande
                    switch (result) { // Selon le résultat on répond différents messages au Client et on ferme ou non la connexion
                        case "null": // Indique qu'il faut fermer la connexion
                            return false;
                        case "connect:true": // L'authentification a réussie, donc on peut continuer
                            sendMessage("Connexion réussie !");
                            return true;
                        case "connect:false": // Mauvaise authentification, donc on ferme la connexion
                            sendMessage("bad-auth");
                            close();
                            return false;
                        default: // Sinon on renvoie juste la réponse
                            sendMessage(result);
                            return true;
                    }
                } else { // Si ce n'est pas une commande
                    // A CHANGER SELON LA METHODE########################
                    // On récupère la réponse du Classifier
                    Object[] response = Classifier.getResponse(name, line, server.getAgenda(), reservationDateTime, wasReservation);
                    wasReservation = (response[0] == Classifier.MessageType.RESERVATION); // On vérifie si le dernier message était une demande de réservation
                    if (wasReservation && Boolean.parseBoolean(response[2].toString())) { // Si oui, on récupère les informations de date et heure contenue dans la réponse
                        reservationDateTime = new String[2];
                        reservationDateTime[0] = Agenda.getYearOfDayMonth(Integer.parseInt(response[4].toString()), Integer.parseInt(response[5].toString())) + "-" + response[5].toString() + "-" + response[4].toString();
                        reservationDateTime[1] = response[3].toString();
                    } else { // Sinon, on supprime les dernière infos enregistrées
                        reservationDateTime = null;
                    }
                    // Si la dernière réponse est une confirmation valide, alors on enregistre l'agenda (qui vient d'être modifié)
                    if (response[0] == Classifier.MessageType.CONFIRMATION && Boolean.parseBoolean(response[2].toString())) {
                        server.saveAgenda();
                    }
                    // Dans tous les cas on répond ce qui a été renvoyé par le Classifier
                    sendMessage(response[1].toString());
                    // ##################################################
                    // On affiche sur la console du serveur le type de message et la réponse
                    server.sendStringToUI("[" + name + "] " + "[" + response[0] + "] " + line);
                    server.sendStringToUI("Answer : " + response[1].toString());
                }
            } catch (IOException ex) {
                // Si il y a eu une erreur, on compte combien on en a eu, et on ferme si on dépasse le maximum autorisé
                ioErrorsCount++;
                if (ioErrorsCount >= MAX_ERRORS) {
                    close();
                    return false;
                }
            }
        }
        return true;
    }

    // Envoie un message au Client
    private void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    // Interprète une commande
    public String readCommand(String message) {
        message = message.substring(2); // On enlève :: au début
        String command = message.split("=")[0]; // On récupère l'instruction contenue avant le = (qu'il y en ait un ou pas)
        String argument = "";
        if (message.contains("=")) { // Si il y en a un, on récupère l'argument associé
            argument = message.split("=")[1];
        }
        switch (command) { // Selon l'instruction
            case "username": // Enregistre le nom d'utilisateur contenu dans argument, et ne renvoie pas de message
                this.name = argument;
                return "";
            case "password": // Récupère le mot de passe hashé, et compare si le couple nom d'utilisateur/mot de passe existe en parcourant la liste enregistrée
                Login login = new Login(this.name, argument, true);
                for (int i = 0; i < server.getLogins().size(); i++) {
                    if (server.getLogins().get(i).isSameAs(login)) { // Si elle existe, on se connecte et on indique que la connexion a été réussie
                        server.sendStringToUI("Authentification de \"" + name + "\" réussie !");
                        return "connect:true";
                    }
                }
                return "connect:false"; // Sinon on indique qu'il y a eu un problème
            case "close": // Ferme la connexion
                close();
                return "null";
            case "count": // Renvoie le nombre de connexion active
                return Integer.toString(this.server.connectionCount());
            default: // Si on en a reconnue aucune, on l'indique au Client
                return "Commande inconnue.";
        }
    }

    // Ferme la connexion
    public void close() {
        try {
            server.sendStringToUI("Connexion avec \"" + name + "\" fermée.");
            connected = false;
            socket.close();
            server.getConnections().remove(this);
        } catch (IOException ex) {}
    }

}
