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
 *
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
        } catch (IOException ex) {
            server.sendStringToUI("[ERROR] Erreur lors de l'ouverture des flux.");
        }
    }
    
    @Override
    public void run() {
        while (readMessage()){}
    }

    public boolean readMessage() {
        if (connected) {
            try {
                String line = in.readLine();
                if (line == null) return false;
                if (line.startsWith("::")) {
                    String response = readCommand(line);
                    switch (response) {
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
                            sendMessage(response);
                            return true;
                    }
                } else {
                    server.sendStringToUI("[" + name + "] " + line);
                    sendMessage(line);
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
        } catch (IOException ex) {}
    }

}
