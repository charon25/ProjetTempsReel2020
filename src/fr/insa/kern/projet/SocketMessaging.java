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

/**
 * @author pkern01
 */
public class SocketMessaging extends Thread {

    final private int MAX_ERRORS = 10;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private Server server;
    private boolean connected;
    private ArrayList<Login> logins;
    private int ioErrorsCount;
    //A CHANGER SELON LA METHODE########################
    private String[] reservationDateTime;
    private boolean wasReservation;
    //##################################################

    public boolean isConnected() {
        return connected;
    }

    public String getClientName() {
        return name;
    }

    public SocketMessaging(Server server, Socket socket, ArrayList<Login> logins) {
        try {
            this.server = server;
            this.socket = socket;
            this.logins = logins;
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
    public void run() {
        while (readMessage()) {
        }
    }

    public boolean readMessage() {
        if (connected) {
            try {
                String line = in.readLine();
                System.out.println("input : " + line);
                if (line == null) return false;
                if (line.startsWith("::")) {
                    String result = readCommand(line);
                    switch (result) {
                        case "null":
                            return false;
                        case "connect:true":
                            sendMessage("Connexion réussie !");
                            return true;
                        case "connect:false":
                            sendMessage("bad-auth");
                            close();
                            return false;
                        default:
                            sendMessage(result);
                            return true;
                    }
                } else {
                    System.out.println("was reservation : " + wasReservation);
                    //A CHANGER SELON LA METHODE########################
                    Object[] response = Classifier.getResponse(name, line, server.getAgenda(), reservationDateTime, wasReservation);
                    wasReservation = (response[0] == Classifier.MessageType.RESERVATION);// && Boolean.parseBoolean(response[2].toString()); //On vérifie si le dernier message était une demande de réservation et qu'on a pu extraire les infos
                    if (wasReservation && Boolean.parseBoolean(response[2].toString())) { //Si oui
                        reservationDateTime = new String[2];
                        //TODO
                        reservationDateTime[0] = Agenda.getYearOfDayMonth(Integer.parseInt(response[4].toString()), Integer.parseInt(response[5].toString())) + "-" + response[5].toString() + "-" + response[4].toString();
                        reservationDateTime[1] = response[3].toString();
                    } else {
                        reservationDateTime = null;
                    }
                    if (response[0] == Classifier.MessageType.CONFIRMATION && Boolean.parseBoolean(response[2].toString())) {
                        server.saveAgenda();
                    }
                    sendMessage(response[1].toString());
                    //##################################################
                    server.sendStringToUI("[" + name + "] " + "[" + response[0] + "] " + line);
                    server.sendStringToUI("Answer : " + response[1].toString());
                }
            } catch (IOException ex) {
                ioErrorsCount++;
                if (ioErrorsCount >= MAX_ERRORS) {
                    close();
                    return false;
                }
            }
        }
        return true;
    }

    private void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    public String readCommand(String message) {
        message = message.substring(2);
        String command = message.split("=")[0];
        String argument = "";
        if (message.contains("=")) {
            argument = message.split("=")[1];
        }
        switch (command) {
            case "username":
                this.name = argument;
                return "";
            case "password":
                Login login = new Login(this.name, argument, false);
                for (int i = 0; i < logins.size(); i++) {
                    if (logins.get(i).isSameAs(login)) {
                        server.sendStringToUI("Authentification de \"" + name + "\" réussie !");
                        return "connect:true";
                    }
                }
                return "connect:false";
            case "close":
                close();
                return "null";
            case "count":
                return Integer.toString(this.server.connectionCount());
            default:
                return "Commande inconnue.";
        }
    }

    public void close() {
        try {
            server.sendStringToUI("Connexion avec \"" + name + "\" fermée.");
            connected = false;
            socket.close();
            server.getConnections().remove(this);
        } catch (IOException ex) {
        }
    }

}
